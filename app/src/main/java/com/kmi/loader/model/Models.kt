package com.kmi.loader.model

enum class RootTab { Home, Modules, Settings, Placeholder }

enum class Segment { Custom, System }

/** 绿 / 黄 / 红三档。红档任何情况下都不放开。 */
enum class Tier { Green, Amber, Red }

enum class ModuleOrigin { Imported, Scanned, System }

/** 一条真实的内核模块记录（path 为唯一 id） */
data class ModuleUi(
    val id: String,
    val name: String,
    val version: String,
    val origin: ModuleOrigin,
    val sizeText: String,
    val desc: String,
    val tier: Tier,
    val tierLabel: String,
    val loaded: Boolean = false,
    val autoStart: Boolean = false,
    val hasLog: Boolean = false,
    val refcnt: Int = 0,
    val holders: String = "",
    val bootCritical: Boolean = false,
    val bootListed: Boolean = false,
    val builtin: Boolean = false,
    val vermagic: String = "",
    val arch: String = "aarch64",
    val moduleAlignKb: Int = 4,
    val depends: String = "",
    val path: String = "",
    val reason: String = "",
    val moduleName: String = "",
)

data class DirUi(
    val path: String,
    val sub: String,
    val count: Int = 0,
    val enabled: Boolean = true,
)

data class DeviceInfo(
    val model: String,
    val rom: String,
    val android: String,
    val kernel: String,
    val kmi: String,
    val gki: String,
    val supported: Boolean,
    val gen: Int,
    val pageSize: Int,
    val root: String,
    val selinux: String,
    val moduleLoad: String,
    val protectedSig: String,
    val systemCount: Int,
    val customCount: Int,
    val protectState: String,
    val loadAbility: String,
) {
    companion object {
        /** 读取完成前的占位，字段全部为空，不做任何编造 */
        fun loading(): DeviceInfo = DeviceInfo(
            model = "读取中…",
            rom = "—",
            android = "—",
            kernel = "—",
            kmi = "—",
            gki = "—",
            supported = false,
            gen = 0,
            pageSize = 0,
            root = "—",
            selinux = "—",
            moduleLoad = "—",
            protectedSig = "—",
            systemCount = 0,
            customCount = 0,
            protectState = "—",
            loadAbility = "—",
        )
    }
}

object Dirs {
    fun defaults(): List<DirUi> = listOf(
        DirUi("/vendor/lib/modules", "vendor_dlkm"),
        DirUi("/system/lib/modules", "system_dlkm"),
        DirUi("/odm/lib/modules", "odm_dlkm"),
        DirUi("/data/adb/modules", "Magisk / KernelSU"),
    )
}
