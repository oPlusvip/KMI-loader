package com.kmi.loader.core

import com.kmi.loader.model.Tier
import java.io.File

/** 从 .ko 的 ELF 结构里读 .modinfo（vermagic / name / depends / license）。 */
object KoParser {

    data class Meta(
        val path: String,
        val fileName: String,
        val moduleName: String,
        val vermagic: String,
        val depends: String,
        val license: String,
        val alignBytes: Long,
        val sizeBytes: Long,
        val arch: String,
        val error: String? = null,
    ) {
        val release: String get() = vermagic.substringBefore(' ').trim()
        val flags: List<String> get() = vermagic.split(' ').drop(1).filter { it.isNotBlank() }
        val hasModversions: Boolean get() = flags.contains("modversions")
    }

    fun parse(path: String, displayName: String = File(path).name, sizeHint: Long = 0L): Meta {
        val file = File(path)
        val localLen = runCatching { file.length() }.getOrDefault(0L)
        val size = if (localLen > 0) localLen else sizeHint
        val name0 = displayName.removeSuffix(".ko").replace('-', '_')

        val hdr = Shell.readRange(path, 0, 64)
            ?: return Meta(path, displayName, name0, "", "", "", 0, size, "", "无法读取文件头")

        if (hdr.size < 52) return Meta(path, displayName, name0, "", "", "", 0, size, "", "文件过小")

        val is64 = hdr[4].toInt() == 2
        val le = hdr[5].toInt() == 1
        if (!le) return Meta(path, displayName, name0, "", "", "", 0, size, "", "非小端 ELF")

        if (!is64) {
            return Meta(path, displayName, name0, "", "", "", 0, size, "", "32 位 ELF，暂不支持预检")
        }

        val shoff = leLong(hdr, 0x28)
        val shentsize = leShort(hdr, 0x3A).toLong()
        val shnum = leShort(hdr, 0x3C)
        val shstrndx = leShort(hdr, 0x3E)
        val phoff = leLong(hdr, 0x20)
        val phentsize = leShort(hdr, 0x36).toLong()
        val phnum = leShort(hdr, 0x38)

        if (shoff == 0L || shnum == 0 || shstrndx >= shnum) {
            return Meta(path, displayName, name0, "", "", "", 0, size, "", "ELF 无节表")
        }

        val shBlock = Shell.readRange(path, shoff, (shnum * shentsize).toInt().coerceAtMost(64 * 1024))
            ?: return Meta(path, displayName, name0, "", "", "", 0, size, "", "无法读取节表")

        fun shField(i: Int, fieldOff: Int, wide: Boolean): Long {
            val base = (i * shentsize).toInt()
            return if (wide) leLong(shBlock, base + fieldOff) else leInt(shBlock, base + fieldOff).toLong()
        }

        // 读 shstrtab 节名表
        val strTabOff = shField(shstrndx.toInt(), 24, true)
        val strTabSize = shField(shstrndx.toInt(), 32, true)
        val strTab = Shell.readRange(path, strTabOff, strTabSize.toInt().coerceAtMost(128 * 1024))
            ?: return Meta(path, displayName, name0, "", "", "", 0, size, "", "无法读取节名表")

        var modinfoOff = -1L
        var modinfoSize = 0L
        for (i in 0 until shnum) {
            val nameOff = shField(i, 0, false).toInt()
            val secName = readCString(strTab, nameOff) ?: continue
            if (secName == ".modinfo") {
                modinfoOff = shField(i, 24, true)
                modinfoSize = shField(i, 32, true)
            }
        }

        // 段对齐（PT_LOAD 的 p_align），用于页大小判断
        var align = 0L
        if (phoff > 0 && phnum > 0) {
            val phBlock = Shell.readRange(path, phoff, (phnum * phentsize).toInt().coerceAtMost(16 * 1024))
            if (phBlock != null) {
                for (i in 0 until phnum) {
                    val base = (i * phentsize).toInt()
                    if (base + 56 > phBlock.size) break
                    val type = leInt(phBlock, base)
                    if (type == 1) { // PT_LOAD
                        val a = leLong(phBlock, base + 0x30)
                        if (a > align) align = a
                    }
                }
            }
        }

        if (modinfoOff < 0) {
            return Meta(path, displayName, name0, "", "", "", align, size, "", "缺少 .modinfo 节（不是内核模块？）")
        }

        val raw = Shell.readRange(path, modinfoOff, modinfoSize.toInt().coerceAtMost(256 * 1024))
            ?: return Meta(path, displayName, name0, "", "", "", align, size, "", "无法读取 .modinfo")

        var name = ""
        var vermagic = ""
        var depends = ""
        var license = ""
        for (entry in raw.toString(Charsets.UTF_8).split('\u0000')) {
            when {
                entry.startsWith("name=") -> name = entry.removePrefix("name=")
                entry.startsWith("vermagic=") -> vermagic = entry.removePrefix("vermagic=")
                entry.startsWith("depends=") -> depends = entry.removePrefix("depends=")
                entry.startsWith("license=") -> license = entry.removePrefix("license=")
            }
        }

        val archToken = vermagic.split(' ').lastOrNull { it == "aarch64" || it == "arm" || it == "x86_64" || it == "riscv" }
            ?: ""

        val err = when {
            vermagic.isEmpty() -> "缺少 vermagic，无法预检"
            else -> null
        }

        return Meta(
            path = path,
            fileName = displayName,
            moduleName = name.ifEmpty { name0 },
            vermagic = vermagic,
            depends = depends,
            license = license,
            alignBytes = align,
            sizeBytes = size,
            arch = archToken,
            error = err,
        )
    }

    private fun readCString(buf: ByteArray, off: Int): String? {
        if (off < 0 || off >= buf.size) return null
        val sb = StringBuilder()
        var i = off
        while (i < buf.size && buf[i].toInt() != 0) {
            sb.append(buf[i].toInt().toChar())
            i++
        }
        return sb.toString()
    }

    private fun leShort(b: ByteArray, o: Int): Int {
        if (o + 2 > b.size) return 0
        return (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)
    }

    private fun leInt(b: ByteArray, o: Int): Int {
        if (o + 4 > b.size) return 0
        return (b[o].toInt() and 0xFF) or
            ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or
            ((b[o + 3].toInt() and 0xFF) shl 24)
    }

    private fun leLong(b: ByteArray, o: Int): Long {
        if (o + 8 > b.size) return 0
        return leInt(b, o).toLong() and 0xFFFFFFFFL or (leInt(b, o + 4).toLong() shl 32)
    }
}

/** 设备侧内核信息 */
data class KernelInfo(
    val release: String,
    val majorMinor: String,
    val androidTag: String,
    val buildId: String,
    val pageSizeKb: Int,
    val arch: String,
)

object TierJudge {

    fun parseKernel(release: String, pageSizeKb: Int, arch: String): KernelInfo {
        val mm = Regex("""^(\d+\.\d+)""").find(release)?.groupValues?.get(1) ?: ""
        val tag = Regex("""android(\d+)-(\d+)""").find(release)
            ?.let { "android${it.groupValues[1]}-${it.groupValues[2]}" } ?: ""
        val build = Regex("""-g([0-9a-fA-F]{6,})""").find(release)?.groupValues?.get(1) ?: ""
        return KernelInfo(release, mm, tag, build, pageSizeKb, arch)
    }

    /** 按内核主次版本 + KMI 段推 GKI 代际；返回 (标签, 是否支持 KMI 校验) */
    fun gkiInfo(majorMinor: String, androidTag: String): Pair<String, Boolean> {
        if (majorMinor.isEmpty()) return "未知" to false
        val major = majorMinor.substringBefore('.').toIntOrNull() ?: return "未知" to false
        if (major < 4) return "非 GKI" to false
        if (major == 4) return "非 GKI" to false

        val gen = when (majorMinor) {
            "5.4", "5.10" -> "GKI 1.0"
            "5.15", "6.1", "6.6", "6.12" -> "GKI 2.0"
            else -> if (major == 5) "GKI 1.0" else "GKI 2.0"
        }
        // 标准 GKI 的版本串里带 androidNN-N 段；没有就是自编译/厂商改动版本
        val standard = androidTag.isNotEmpty()
        return if (standard) gen to true else "$gen · 非官方构建" to true
    }

    /** 返回档位 + 判定依据（给用户看的真实理由） */
    fun judge(meta: KoParser.Meta, k: KernelInfo, kmiSupported: Boolean = true): Pair<Tier, String> {
        if (meta.error != null) {
            return when (meta.error) {
                "32 位 ELF，暂不支持预检" -> Tier.Red to "32 位模块，与 arm64 内核不匹配"
                "缺少 .modinfo 节（不是内核模块？）" -> Tier.Red to "不是合法的 .ko（缺 .modinfo）"
                else -> Tier.Amber to meta.error
            }
        }

        // 架构
        val devArch = if (k.arch.contains("aarch64")) "aarch64" else k.arch
        if (meta.arch.isNotEmpty() && devArch.isNotEmpty() && meta.arch != devArch) {
            return Tier.Red to "架构不匹配（模块 ${meta.arch}，设备 $devArch）"
        }

        // 页大小（模块 PT_LOAD 对齐小于设备页大小 → 映射必失败）
        if (meta.alignBytes > 0) {
            val devBytes = k.pageSizeKb * 1024L
            if (meta.alignBytes < devBytes && meta.alignBytes < 65536L) {
                return Tier.Red to "页大小不匹配（模块对齐 ${meta.alignBytes / 1024} KB，设备 ${k.pageSizeKb} KB）"
            }
        }

        // 内核版本族
        val koMM = Regex("""^(\d+\.\d+)""").find(meta.release)?.groupValues?.get(1) ?: ""
        if (koMM.isNotEmpty() && k.majorMinor.isNotEmpty() && koMM != k.majorMinor) {
            return Tier.Red to "内核版本不同族（模块 $koMM，设备 ${k.majorMinor}）"
        }

        // 非 GKI 内核：没有 KMI 概念，退回 vermagic 直配
        if (!kmiSupported) {
            return if (meta.release == k.release) {
                Tier.Green to "vermagic 与设备内核一致（非 GKI 内核，不做 KMI 匹配）"
            } else {
                Tier.Amber to "内核 release 不同（模块 ${meta.release} / 设备 ${k.release}），非 GKI 内核无 KMI 可比"
            }
        }

        // KMI 段（android版本 + 代数）
        val koTag = Regex("""android(\d+)-(\d+)""").find(meta.release)
            ?.let { "android${it.groupValues[1]}-${it.groupValues[2]}" } ?: ""
        if (koTag.isEmpty() || k.androidTag.isEmpty()) {
            return Tier.Amber to "KMI 段缺失，无法确认兼容性"
        }
        if (koTag != k.androidTag) {
            return Tier.Amber to "KMI 不同（模块 $koTag，设备 ${k.androidTag}）"
        }

        // 完整 release 比对
        if (meta.release != k.release) {
            val koBuild = Regex("""-g([0-9a-fA-F]{6,})""").find(meta.release)?.groupValues?.get(1) ?: ""
            val sameSub = koBuild.isNotEmpty() && k.buildId.isNotEmpty() && koBuild == k.buildId
            return if (sameSub) {
                Tier.Green to "构建号一致（$koBuild）"
            } else {
                Tier.Amber to "构建号不同（模块 ${meta.release} / 设备 ${k.release}），内核按 same_magic 只比 flags，能否加载取决于符号 CRC"
            }
        }

        return Tier.Green to "vermagic 与设备内核完全一致"
    }
}
