# KMI Loader

Android 上的 **KMI 内核模块加载器**：不依赖任何模块管理器，自己探测设备内核、判定 .ko 能不能加载，并完成加载/卸载。

Kotlin + Jetpack Compose（全自绘 UI），minSdk 26 / targetSdk 35，需 root。

## 它做什么

- **真实探测**：`uname -r`、KMI 基线、GKI 代际、页大小（getconf → smaps 回退）、SELinux、root 管理器（Magisk / KernelSU / APatch）、insmod 路径、`CONFIG_MODULE_SIG`、`module_layout` 是否存在
- **扫描与解析**：扫 `/vendor/lib/modules`、`/system/lib/modules`、`/odm` 等目录，解析 ELF 的 `.modinfo`（name / vermagic / depends / license）与 PT_LOAD 对齐；app 读不到的 `vendor` 文件走 root `dd` 或 `cp` 到 cache 再解析
- **三档预检**：绿（可加载）/ 黄（仅专家模式可强制）/ 红（不放行），判定依据是架构、页对齐、KMI 段、vermagic flags、非 GKI 内核的降级规则
- **加载 / 卸载**：`insmod` / `rmmod`，失败时抓 `dmesg` 里的真实原因；`refcnt > 0` 硬锁，其余走确认弹窗
- **开机自启**：写入 `/data/adb/modules/kmi_loader_autostart/`，下次开机生效
- **系统模块区分**：`modules.load` 清单内 + 已加载 = 启动关键；清单内但未加载 = 清单内·未加载

## 判定逻辑（核心）

内核 `same_magic()` **只比较 vermagic 第一个空格之后的部分**（如 `SMP preempt mod_unload modversions aarch64`），不比较 release 号，再配合符号 CRC 校验。所以“构建号不同”不等于加载失败——实测 `5.4.254-qgki-g05455f9c3e28` 的备份模块能加载进 `5.4.254-...-gaa34c30c0417-dirty` 的内核。

因此预检里“构建号不同”只判黄并说明取决于符号 CRC，不判红。

## 构建

```bash
export JAVA_HOME=/path/to/jdk17
./gradlew :app:assembleDebug
```

当前版本 **v0.4.3**，包名 `com.kmi.loader`（占位，未定稿）。
