package com.kmi.loader.core

import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** 统一走 shell 执行；root 命令统一 su -c。所有调用都有硬超时。 */
object Shell {

    data class Res(val code: Int, val out: String, val err: String) {
        val ok: Boolean get() = code == 0
    }

    private const val DEFAULT_TIMEOUT = 8000L

    /** root 是否可用：只探一次，之后复用 */
    private val rootAvailable: Boolean by lazy {
        root("id", 6000).out.contains("uid=0")
    }

    fun hasRoot(): Boolean = rootAvailable

    /** 关键：读取输出放在独立线程，主线程只负责超时判定，避免进程卡死时无限等待 */
    fun exec(cmd: String, timeoutMs: Long = DEFAULT_TIMEOUT): Res {
        return try {
            val p = ProcessBuilder("/system/bin/sh", "-c", cmd).start()
            val outRef = AtomicReference("")
            val errRef = AtomicReference("")

            val tOut = Thread {
                runCatching { p.inputStream.bufferedReader().use { outRef.set(it.readText()) } }
            }.apply { isDaemon = true; name = "sh-out" }

            val tErr = Thread {
                runCatching { p.errorStream.bufferedReader().use { errRef.set(it.readText()) } }
            }.apply { isDaemon = true; name = "sh-err" }

            tOut.start()
            tErr.start()

            val done = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!done) {
                p.destroyForcibly()
                p.waitFor(600, TimeUnit.MILLISECONDS)
            }
            tOut.join(1200)
            tErr.join(1200)

            if (done) Res(p.exitValue(), outRef.get(), errRef.get())
            else Res(-1, outRef.get(), "timeout after ${timeoutMs}ms")
        } catch (t: Throwable) {
            Res(-1, "", t.message ?: "exec failed")
        }
    }

    private fun q(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    fun root(cmd: String, timeoutMs: Long = DEFAULT_TIMEOUT): Res = exec("su -c ${q(cmd)}", timeoutMs)

    fun rootManager(): String {
        return when {
            File("/data/adb/ksu").exists() || File("/data/adb/ksud").exists() -> "KernelSU"
            File("/data/adb/ap").exists() || File("/data/adb/apd").exists() -> "APatch"
            File("/data/adb/magisk").exists() -> "Magisk"
            hasRoot() -> "Root（未识别管理器）"
            else -> "无 root"
        }
    }

    /** 读文本：先本地，失败再 root，最终都有超时 */
    fun readText(path: String): String {
        val f = File(path)
        if (f.exists() && f.canRead()) {
            runCatching { return f.readText() }
        }
        return root("cat ${q(path)}").out
    }

    /** 读字节区间：本地直读优先；不可读时退回 root dd（结果去空白后再解码） */
    fun readRange(path: String, offset: Long, length: Int): ByteArray? {
        val f = File(path)
        if (f.exists() && f.canRead()) {
            runCatching {
                java.io.RandomAccessFile(f, "r").use { raf ->
                    raf.seek(offset)
                    val buf = ByteArray(length)
                    var read = 0
                    while (read < length) {
                        val n = raf.read(buf, read, length - read)
                        if (n < 0) break
                        read += n
                    }
                    if (read == 0) return null
                    return if (read == length) buf else buf.copyOf(read)
                }
            }
        }
        val r = root("dd if=${q(path)} bs=1 skip=$offset count=$length 2>/dev/null | base64", 15000)
        val b64 = r.out.replace(Regex("""\s"""), "")
        if (b64.isEmpty()) return null
        return runCatching { android.util.Base64.decode(b64, android.util.Base64.DEFAULT) }.getOrNull()
    }
}
