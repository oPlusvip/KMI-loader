package com.kmi.loader.core

import android.content.Context
import android.net.Uri
import android.os.Build
import com.kmi.loader.model.DeviceInfo
import com.kmi.loader.model.DirUi
import com.kmi.loader.model.ModuleOrigin
import com.kmi.loader.model.ModuleUi
import com.kmi.loader.model.Tier
import java.io.File
import java.util.concurrent.Executors
import kotlin.concurrent.thread

object Repo {

    data class OpResult(val ok: Boolean, val message: String)

    private val pageSizes = listOf(4096, 16384, 65536)

    suspend fun device(): DeviceInfo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val release = readKernelRelease()
        val arch = System.getProperty("os.arch")?.lowercase().orEmpty().ifEmpty { "aarch64" }
        val pageKb = detectPageSizeKb()
        val k = TierJudge.parseKernel(release, pageKb, arch)

        val gen = Regex("""android\d+-(\d+)""").find(k.androidTag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val (gki, supported) = TierJudge.gkiInfo(k.majorMinor, k.androidTag)
        val kmi = if (k.androidTag.isNotEmpty()) "${k.majorMinor}-${k.androidTag}" else k.majorMinor

        val rootManager = Shell.rootManager()
        val selinux = run {
            val raw = runCatching { java.io.File("/sys/fs/selinux/enforce").readText().trim() }.getOrDefault("")
            when (raw) {
                "1" -> "Enforcing"
                "0" -> "Permissive"
                else -> {
                    val s = Shell.exec("getenforce", 4000).out.trim()
                    if (s.isNotEmpty()) s else {
                        val r = Shell.root("cat /sys/fs/selinux/enforce", 6000).out.trim()
                        when (r) {
                            "1" -> "Enforcing"
                            "0" -> "Permissive"
                            else -> "未探测（SELinux 未启用或不可读）"
                        }
                    }
                }
            }
        }

        val insmodPath = listOf(
            "/system/bin/insmod",
            "/system/xbin/insmod",
            "/debug_ramdisk/insmod",
            "/data/adb/ksu/bin/insmod",
        ).firstOrNull { java.io.File(it).exists() }
        val loadAbility = when {
            rootManager == "无 root" -> "无 root，无法加载"
            insmodPath != null -> "insmod: $insmodPath"
            else -> "insmod 不在常见路径"
        }

        val sig = detectModuleSig()
        val protSym = detectProtectedSymbol()

        val guardDir = java.io.File("/data/adb/modules/kmi_loader_guard")
        val protectState = if (guardDir.exists()) "已启用" else "未启用"

        DeviceInfo(
            model = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            rom = Build.DISPLAY ?: "未知",
            android = "Android ${Build.VERSION.RELEASE}（API ${Build.VERSION.SDK_INT}）",
            kernel = release.ifEmpty { "未知" },
            kmi = kmi.ifEmpty { "未知" },
            gki = gki,
            supported = supported,
            gen = gen,
            pageSize = pageKb,
            root = rootManager,
            selinux = selinux,
            moduleLoad = sig,
            protectedSig = protSym,
            systemCount = 0,
            customCount = 0,
            protectState = protectState,
            loadAbility = loadAbility,
        )
    }

    /** 内核是否开了模块签名校验：读 /proc/config.gz，拿不到就明说未探测 */
    private fun detectModuleSig(): String {
        val out = Shell.root("zcat /proc/config.gz 2>/dev/null | grep -E '^CONFIG_MODULE_SIG(=|_FORCE)'", 10000).out
            .lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (out.isEmpty()) return "未探测（无 /proc/config.gz）"
        val forced = out.any { it.startsWith("CONFIG_MODULE_SIG_FORCE=y") }
        val on = out.any { it.startsWith("CONFIG_MODULE_SIG=y") }
        return when {
            forced -> "开启 · 强制签名"
            on -> "开启（非强制）"
            else -> "关闭"
        }
    }

    /** module_layout 是否在内核符号表里（GKI 的模块版本校验依赖它） */
    private fun detectProtectedSymbol(): String {
        val out = Shell.root("grep -c ' module_layout$' /proc/kallsyms 2>/dev/null", 10000).out.trim()
        val n = out.lineSequence().firstOrNull()?.trim()?.toIntOrNull()
        return when {
            n == null -> "未探测（kallsyms 不可读）"
            n > 0 -> "module_layout 存在"
            else -> "未找到 module_layout"
        }
    }

    private fun readKernelRelease(): String {
        val p = Shell.readText("/proc/sys/kernel/osrelease").trim()
        if (p.isNotEmpty()) return p
        val v = System.getProperty("os.version")?.trim().orEmpty()
        if (v.isNotEmpty()) return v
        return Shell.exec("uname -r").out.trim()
    }

    private fun detectPageSizeKb(): Int {
        for (cmd in listOf("getconf PAGESIZE", "getconf PAGE_SIZE")) {
            val out = Shell.exec(cmd, 4000).out.trim()
            val n = out.toIntOrNull()
            if (n != null && n >= 4096) return n / 1024
        }
        // 反推：/proc/self/smaps 的 "KernelPageSize" 字段
        val smaps = runCatching { java.io.File("/proc/self/smaps").readText() }.getOrDefault("")
        Regex("""KernelPageSize:\s*(\d+)\s*kB""").find(smaps)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        return 4
    }

    fun kernelInfo(dev: DeviceInfo): KernelInfo =
        TierJudge.parseKernel(dev.kernel, dev.pageSize, "aarch64")

    private var bootSetCache: Set<String>? = null

    /** 厂商 modules.load 里列出的开机模块（读不到就返回空，不猜） */
    private fun bootCriticalNames(): Set<String> {
        bootSetCache?.let { return it }
        val files = listOf(
            "/vendor/etc/modules.load",
            "/vendor/lib/modules/modules.load",
            "/system/etc/modules.load",
            "/odm/etc/modules.load",
        )
        val set = mutableSetOf<String>()
        files.forEach { f ->
            val text = Shell.readText(f)
            text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .forEach { line ->
                    val token = line.split(Regex("""\s+""")).firstOrNull().orEmpty()
                    if (token.isNotEmpty()) {
                        set.add(token)
                        set.add(token.removeSuffix(".ko").replace('-', '_'))
                    }
                }
        }
        val out = set.toSet()
        bootSetCache = out
        return out
    }

    /** /proc/modules 里的 name -> refcnt */
    fun loadedModules(): Map<String, Int> {
        val text = Shell.readText("/proc/modules")
        val map = mutableMapOf<String, Int>()
        text.lineSequence().forEach { line ->
            val parts = line.trim().split(Regex("""\s+"""))
            if (parts.size >= 3) {
                val name = parts[0].replace('-', '_')
                val ref = parts[2].toIntOrNull() ?: 0
                map[name] = ref
            }
        }
        return map
    }

    /** 扫描目录里的 .ko（root 优先，退回普通遍历） */
    private fun listKo(dirs: List<DirUi>): List<String> {
        val existing = dirs.map { it.path }.filter { java.io.File(it).exists() }
        if (existing.isEmpty()) return emptyList()

        if (Shell.hasRoot()) {
            val args = existing.joinToString(" ") { q(it) }
            val viaRoot = Shell.root("find $args -maxdepth 3 -type f -name '*.ko' 2>/dev/null", 15000)
            val lines = viaRoot.out.lines().map { it.trim() }.filter { it.endsWith(".ko") && !it.contains("Permission denied") }
            if (lines.isNotEmpty()) return lines.distinct()
        }

        return existing.flatMap { dir ->
            val f = File(dir)
            if (!f.canRead()) emptyList<String>()
            else f.walkTopDown().maxDepth(3)
                .filter { it.isFile && it.name.endsWith(".ko") }
                .map { it.absolutePath }
                .toList()
        }.distinct()
    }

    /** 一次 root stat 批量拿真实大小（app 读不到 /vendor 时 length() 全是 0） */
    private fun koSizes(paths: List<String>): Map<String, Long> {
        if (paths.isEmpty()) return emptyMap()
        if (!Shell.hasRoot()) {
            return paths.associateWith { p -> runCatching { File(p).length() }.getOrDefault(0L) }
        }
        val out = Shell.root("stat -c '%s|%n' " + paths.joinToString(" ") { q(it) }, 25000).out
        val map = mutableMapOf<String, Long>()
        out.lineSequence().forEach { line ->
            val idx = line.indexOf('|')
            if (idx > 0) {
                val size = line.substring(0, idx).trim().toLongOrNull()
                val path = line.substring(idx + 1).trim()
                if (size != null && path.isNotEmpty()) map[path] = size
            }
        }
        return map
    }

    private fun q(s: String) = "'" + s.replace("'", "'\\''") + "'"

    suspend fun scan(dirs: List<DirUi>, dev: DeviceInfo, cacheDir: File):
        Pair<List<ModuleUi>, List<DirUi>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val kernel = kernelInfo(dev)
            val loaded = loadedModules()
            val enabled = dirs.filter { it.enabled }
            val found = listKo(enabled)

            // 本地读不到的，一次性批量 cp 到 cache，避免每个文件一次 su
            val scratch = File(cacheDir, "kmiscan").apply { mkdirs() }
            val unreadable = found.filter { p -> runCatching { File(p).canRead() }.getOrDefault(false).not() }
                .take(80)
            val localCopy = mutableMapOf<String, String>()
            if (unreadable.isNotEmpty() && Shell.hasRoot()) {
                val cmds = unreadable.mapIndexed { i, p -> "cp -f $p '$scratch/$i.ko' 2>/dev/null" }
                    .joinToString("; ")
                Shell.root(cmds, 25000)
                unreadable.forEachIndexed { i, p ->
                    val copy = File(scratch, "$i.ko")
                    if (copy.exists() && copy.length() > 0) localCopy[p] = copy.absolutePath
                }
            }

            val sizes = koSizes(found)
            val bootNames = bootCriticalNames()
            val pool = Executors.newFixedThreadPool(2)
            val results = java.util.Collections.synchronizedList(mutableListOf<Pair<String, KoParser.Meta>>())
            found.forEach { original ->
                pool.execute {
                    runCatching {
                        val pathForParse = localCopy[original] ?: original
                        val name = File(original).name
                        results.add(original to KoParser.parse(pathForParse, name, sizes[original] ?: 0L))
                    }
                }
            }
            pool.shutdown()
            pool.awaitTermination(20, java.util.concurrent.TimeUnit.SECONDS)
            pool.shutdownNow()
            runCatching { scratch.deleteRecursively() }

            val modules = results.map { (original, meta) ->
                val (tier, reason) = TierJudge.judge(meta, kernel, dev.supported)
                val system = original.startsWith("/vendor") ||
                    original.startsWith("/system") ||
                    original.startsWith("/odm")
                val dirName = original.substringBeforeLast('/', "").ifEmpty { "/" }
                val key = meta.fileName.removeSuffix(".ko").replace('-', '_')
                val isLoaded = loaded.containsKey(meta.moduleName) || loaded.containsKey(key)
                ModuleUi(
                    id = original,
                    name = meta.fileName,
                    version = meta.license.ifEmpty { "—" },
                    origin = if (system) ModuleOrigin.System else ModuleOrigin.Scanned,
                    sizeText = humanSize(if (meta.sizeBytes > 0) meta.sizeBytes else sizes[original] ?: 0L),
                    desc = buildString {
                        append("来自 $dirName")
                        if (meta.depends.isNotEmpty()) append(" · depends: ${meta.depends}")
                    },
                    tier = tier,
                    tierLabel = if (isLoaded) "运行中" else when (tier) {
                        Tier.Green -> "可加载"
                        Tier.Amber -> "需专家"
                        Tier.Red -> "不可加载"
                    },
                    loaded = isLoaded,
                    refcnt = loaded[meta.moduleName] ?: 0,
                    bootListed = bootNames.contains(meta.fileName) || bootNames.contains(meta.moduleName),
                    bootCritical = isLoaded && (bootNames.contains(meta.fileName) || bootNames.contains(meta.moduleName)),
                    builtin = system,
                    vermagic = meta.vermagic,
                    arch = meta.arch.ifEmpty { "—" },
                    moduleAlignKb = if (meta.alignBytes >= 4096) (meta.alignBytes / 1024).toInt() else 0,
                    depends = meta.depends,
                    path = original,
                    reason = reason,
                    moduleName = meta.moduleName,
                )
            }.sortedWith(compareBy({ it.builtin }, { it.name.lowercase() }))

            val counts = found.groupBy { path ->
                enabled.firstOrNull { path.startsWith(it.path) }?.path ?: path.substringBeforeLast('/')
            }
            val newDirs = dirs.map { d -> d.copy(count = counts[d.path]?.size ?: 0) }

            modules to newDirs
        } catch (t: Throwable) {
            emptyList<ModuleUi>() to dirs
        }
    }

    private fun humanSize(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }

    suspend fun load(module: ModuleUi): OpResult =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (!Shell.hasRoot()) return@withContext OpResult(false, "没有 root 权限，su 未授权")
            val res = Shell.root("insmod ${q(module.path)}", 25000)
            if (res.ok) {
                OpResult(true, "已加载 ${module.moduleName}")
            } else {
                val err = res.err.trim().ifEmpty { res.out.trim() }
                val dmesg = Shell.root("dmesg | tail -n 20 2>/dev/null", 15000).out
                    .lines().filter { it.contains("module", true) || it.contains("insmod", true) }
                    .takeLast(4).joinToString("\n")
                OpResult(false, buildString {
                    append(err.ifEmpty { "insmod 返回 ${res.code}" })
                    if (dmesg.isNotEmpty()) append("\n").append(dmesg)
                })
            }
        }

    suspend fun unload(module: ModuleUi): OpResult =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (!Shell.hasRoot()) return@withContext OpResult(false, "没有 root 权限，su 未授权")
            val name = module.moduleName.ifEmpty { module.name.removeSuffix(".ko").replace('-', '_') }
            val res = Shell.root("rmmod $name", 20000)
            if (res.ok) OpResult(true, "已卸载 $name")
            else OpResult(false, res.err.trim().ifEmpty { "rmmod 失败（可能仍被占用）" })
        }

    private const val AUTO_DIR = "/data/adb/modules/kmi_loader_autostart"

    private fun q2(s: String) = "'" + s.replace("'", "'\\''") + "'"

    private fun writeRootFile(path: String, content: String, mode: String): Boolean {
        val b64 = android.util.Base64.encodeToString(content.toByteArray(), android.util.Base64.NO_WRAP)
        val dir = path.substringBeforeLast('/')
        val cmd = "mkdir -p ${q2(dir)} && echo $b64 | base64 -d > ${q2(path)} && chmod $mode ${q2(path)}"
        return Shell.root(cmd).ok
    }

    /** 读取现有自启脚本里被 insmod 的路径 */
    fun autoStartPaths(): List<String> {
        val script = Shell.readText("$AUTO_DIR/post-fs-data.sh")
        if (script.isEmpty()) return emptyList()
        return script.lines()
            .map { it.trim() }
            .filter { it.startsWith("insmod ") }
            .map { it.removePrefix("insmod ").trim().removeSurrounding("'").removeSurrounding("\"") }
    }

    /** 把 autoStart 标记写成 Magisk/KernelSU 模块；没有标记则删除模块 */
    suspend fun syncAutoStart(modules: List<ModuleUi>): OpResult =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (!Shell.hasRoot()) return@withContext OpResult(false, "没有 root 权限，无法写自启模块")
            val paths = modules.filter { it.autoStart }.map { it.path }
            if (paths.isEmpty()) {
                val r = Shell.root("rm -rf $AUTO_DIR")
                return@withContext OpResult(r.ok, if (r.ok) "已关闭开机自启" else "移除失败：${r.err.trim()}")
            }
            val script = buildString {
                appendLine("#!/system/bin/sh")
                appendLine("# generated by KMI Loader")
                appendLine("LOG=$AUTO_DIR/kmi.log")
                paths.forEach { p -> appendLine("insmod ${q2(p)} >> \$LOG 2>&1") }
            }
            val prop = buildString {
                appendLine("id=kmi_loader_autostart")
                appendLine("name=KMI Loader Autostart")
                appendLine("version=1.0")
                appendLine("versionCode=1")
                appendLine("author=KMI Loader")
                appendLine("description=开机自动加载 KMI Loader 中标记自启的内核模块")
            }
            val okScript = writeRootFile("$AUTO_DIR/post-fs-data.sh", script, "755")
            val okProp = writeRootFile("$AUTO_DIR/module.prop", prop, "644")
            if (okScript && okProp) {
                OpResult(true, "自启已生效：${paths.size} 个模块将在下次开机加载")
            } else {
                OpResult(false, "写入自启模块失败（检查 /data/adb/modules 权限）")
            }
        }

    /** 该模块相关的内核日志（dmesg 里 grep 模块名） */
    suspend fun loadOrLog(module: ModuleUi): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val name = module.moduleName.ifEmpty { module.name.removeSuffix(".ko") }
            val out = Shell.root("dmesg 2>/dev/null | grep -i -E '\\($name|insmod|module)' | tail -n 12", 20000).out
            out.trim().ifEmpty { "没有抓到与 $name 相关的 dmesg 行（可能 dmesg 受限或还没加载过）" }
        }

    /** 从 SAF 导入 .ko 到 app 私有目录并解析 */
    suspend fun import(context: Context, uri: Uri, dirs: List<DirUi>, dev: DeviceInfo):
        Pair<ModuleUi?, String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            } ?: "imported.ko"
            val target = File(context.filesDir, "imported").apply { mkdirs() }
            val out = File(target, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { o -> input.copyTo(o) }
            } ?: return@runCatching null to "无法读取该文件"

            val meta = KoParser.parse(out.absolutePath)
            val (tier, reason) = TierJudge.judge(meta, kernelInfo(dev), dev.supported)
            val module = ModuleUi(
                id = out.absolutePath,
                name = name,
                version = meta.license.ifEmpty { "—" },
                origin = ModuleOrigin.Imported,
                sizeText = humanSize(out.length()),
                desc = "用户导入 · ${reason}",
                tier = tier,
                tierLabel = when (tier) {
                    Tier.Green -> "可加载"
                    Tier.Amber -> "需专家"
                    Tier.Red -> "不可加载"
                },
                vermagic = meta.vermagic,
                arch = meta.arch.ifEmpty { "—" },
                moduleAlignKb = if (meta.alignBytes >= 4096) (meta.alignBytes / 1024).toInt() else 0,
                depends = meta.depends,
                path = out.absolutePath,
                reason = reason,
                moduleName = meta.moduleName,
            )
            module to "已导入并完成预检"
        }.getOrElse { t -> null to (t.message ?: "导入失败") }
    }
}
