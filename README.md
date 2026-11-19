# KMI Loader（原型 v0.1.0）

Android 端的 KMI 内核模块加载器。当前版本只做 **UI 层 + 交互骨架**，数据全部是写死的演示值，
没有接 root、没有真正加载过 .ko。这样做的目的是先让界面和流程跑起来，你装上就能点。

## 装到手机

`KMI-Loader-v0.1.0-debug.apk` 直接装（需要允许安装未知来源）。
包名 `com.kmi.loader`，最低 Android 8.0（API 26），targetSdk 35。

## 编译

```bash
# 方式一：Android Studio 直接打开 KmiLoader/ 目录，等 Gradle sync 完点 Run
# 方式二：命令行（需要 JDK 17 + Android SDK 35）
cd KmiLoader
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` 没有随源码打包，Android Studio 会自动生成；命令行编译时自己写一行
`sdk.dir=/你的/sdk/路径`。

版本组合（已实际编译通过）：AGP 8.7.3 / Kotlin 2.1.0 / Gradle 8.9 / compileSdk 35 / Compose BOM 2024.12.01。

## 已经做出来的东西

四个主页面 + 两个二级页面 + 三个弹窗，全部可点：

- **主页**：KMI 身份卡、系统/自定义模块计数、设备与内核信息、加载能力、开机自启列表
- **模块页**：自定义 / 系统内置分段、模块卡片、加载/卸载/自启/日志按钮、右下「安装」
- **模块详情**：兼容性比对、加载前检查清单、文件信息、按档位变化的底部按钮
- **设置**：高级设置、系统模块设置两个入口
- **高级设置**：专家模式（5 秒倒计时确认弹窗）、开机自启、危险模块锁、重置
- **系统模块设置**：5 个默认扫描目录 + 每目录开关 + 添加目录弹窗
- **卸载**：二次确认弹窗，区分「启动关键模块」的文案

档位逻辑已经落到代码里：

| 档位 | 模块页 | 详情页底部 |
|---|---|---|
| 绿 | 有「加载」按钮 | 加载 / 卸载 |
| 黄 | 无加载入口，只有「查看原因」 | 专家模式开 → 强制加载；否则无入口 |
| 红 | 无加载入口 | 永远只有返回 |

系统模块锁：`builtin` → 灰锁不可卸；`bootCritical` 且专家模式关 → 黄锁；否则可卸。

## 还没做的（下一步）

1. **接真实数据**：`uname -r`、`/proc/version`、`getconf PAGE_SIZE`、`/proc/modules`、目录扫描
2. **接 root**：libsu 起 shell，`finit_module` / `init_module` 走 native，dmesg 解析失败原因
3. **真预检**：现在绿/黄/红是按演示字段判断的，要换成解析 .ko 的 vermagic、ELF 段对齐、符号依赖
4. **开机自启模块**：生成 Magisk/KSU 模块包并刷入，心跳文件 + boot_id 计数
5. **加载日志**：现在只有「有日志/无日志」两态，需要真实的逐次记录

演示数据集中在 `app/src/main/java/com/kmi/loader/model/Models.kt` 的 `DemoData`，
接真实数据时只换这一层，UI 不用动。
