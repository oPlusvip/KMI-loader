package com.kmi.loader.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmi.loader.core.Repo
import com.kmi.loader.model.DeviceInfo
import com.kmi.loader.model.DirUi
import com.kmi.loader.model.Dirs
import com.kmi.loader.model.ModuleUi
import com.kmi.loader.model.RootTab
import com.kmi.loader.model.Segment
import com.kmi.loader.model.Tier
import com.kmi.loader.ui.screens.AdvancedSettingsScreen
import com.kmi.loader.ui.screens.HomeScreen
import com.kmi.loader.ui.screens.ModuleDetailScreen
import com.kmi.loader.ui.screens.ModulesScreen
import com.kmi.loader.ui.screens.SettingsScreen
import com.kmi.loader.ui.screens.SystemDirsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface Dest {
    data class Tab(val tab: RootTab) : Dest
    data object Advanced : Dest
    data object SystemDirs : Dest
    data class Detail(val id: String) : Dest
}

@Composable
fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dest by remember { mutableStateOf<Dest>(Dest.Tab(RootTab.Home)) }

    var dev by remember { mutableStateOf(DeviceInfo.loading()) }
    val custom = remember { mutableStateListOf<ModuleUi>() }
    val system = remember { mutableStateListOf<ModuleUi>() }
    var dirs by remember { mutableStateOf(Dirs.defaults()) }
    var scanning by remember { mutableStateOf(true) }
    var scanStage by remember { mutableStateOf("正在读取内核信息…") }
    var toast by remember { mutableStateOf<String?>(null) }

    var segment by remember { mutableStateOf(Segment.Custom) }
    var query by remember { mutableStateOf("") }
    var expert by remember { mutableStateOf(false) }
    var bootAuto by remember { mutableStateOf(false) }
    var dangerLock by remember { mutableStateOf(true) }

    var showExpertDialog by remember { mutableStateOf(false) }
    var showInstall by remember { mutableStateOf(false) }
    var pendingUninstall by remember { mutableStateOf<String?>(null) }
    var importThenLoad by remember { mutableStateOf(false) }

    fun applyList(mods: List<ModuleUi>) {
        val sys = mods.filter { it.builtin }
        val cus = mods.filter { !it.builtin }
        system.clear()
        system.addAll(sys)
        custom.clear()
        custom.addAll(cus)
        dev = dev.copy(systemCount = sys.size, customCount = cus.size)
    }

    fun rescan() {
        scope.launch {
            try {
                scanning = true
                scanStage = "正在读取内核信息…"
                val d = Repo.device()
                scanStage = "正在扫描模块目录…"
                val (mods, newDirs) = Repo.scan(dirs, d, context.cacheDir)
                dev = d.copy(
                    systemCount = mods.count { it.builtin },
                    customCount = mods.count { !it.builtin },
                )
                applyList(mods)
                dirs = newDirs
                val auto = Repo.autoStartPaths()
                for (i in custom.indices) custom[i] = custom[i].copy(autoStart = auto.contains(custom[i].path))
                bootAuto = auto.isNotEmpty()
            } catch (t: Throwable) {
                toast = "读取失败：${t.message ?: "未知错误"}"
            } finally {
                scanning = false
            }
        }
    }

    fun syncLoaded() {
        scope.launch {
            val lm = Repo.loadedModules()
            for (i in custom.indices) {
                val key = custom[i].moduleName
                custom[i] = custom[i].copy(loaded = lm.containsKey(key), refcnt = lm[key] ?: 0)
            }
            for (i in system.indices) {
                val key = system[i].moduleName
                system[i] = system[i].copy(loaded = lm.containsKey(key), refcnt = lm[key] ?: 0)
            }
        }
    }

    fun doLoad(id: String) {
        val m = custom.firstOrNull { it.id == id } ?: system.firstOrNull { it.id == id } ?: return
        if (m.tier == Tier.Red) {
            toast = "红档模块不提供加载"
            return
        }
        if (m.tier == Tier.Amber && !expert) {
            toast = "黄档需要在专家模式里强制加载"
            return
        }
        scope.launch {
            toast = "正在加载 ${m.name}…"
            val r = Repo.load(m)
            toast = r.message
            syncLoaded()
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                toast = "正在解析…"
                val (module, msg) = Repo.import(context, uri, dirs, dev)
                if (module != null) {
                    custom.add(module)
                    dev = dev.copy(customCount = custom.size)
                    toast = msg
                    if (importThenLoad) {
                        importThenLoad = false
                        doLoad(module.id)
                    }
                } else {
                    toast = msg
                }
            }
        }
    }

    LaunchedEffect(Unit) { rescan() }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(3200)
            toast = null
        }
    }

    fun updateAny(id: String, block: (ModuleUi) -> ModuleUi) {
        val i = custom.indexOfFirst { it.id == id }
        if (i >= 0) {
            custom[i] = block(custom[i])
            return
        }
        val j = system.indexOfFirst { it.id == id }
        if (j >= 0) system[j] = block(system[j])
    }

    fun findModule(id: String): ModuleUi? =
        custom.firstOrNull { it.id == id } ?: system.firstOrNull { it.id == id }

    BackHandler(enabled = dest !is Dest.Tab) {
        dest = when (dest) {
            Dest.Advanced, Dest.SystemDirs -> Dest.Tab(RootTab.Settings)
            is Dest.Detail -> Dest.Tab(RootTab.Modules)
            else -> dest
        }
    }

    val currentTab = when (val d = dest) {
        is Dest.Tab -> d.tab
        Dest.Advanced, Dest.SystemDirs -> RootTab.Settings
        is Dest.Detail -> RootTab.Modules
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(KColors.Background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            AnimatedContent(
                targetState = dest,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(260)) + slideInHorizontally { it / 12 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally { -it / 16 })
                },
                label = "screen",
            ) { d ->
                when (d) {
                    is Dest.Tab -> when (d.tab) {
                        RootTab.Home -> HomeScreen(
                            dev = dev,
                            customCount = custom.size,
                            systemCount = system.size,
                            bootAuto = bootAuto,
                            autoList = custom.filter { it.autoStart },
                            onOpenModules = { seg ->
                                segment = seg
                                dest = Dest.Tab(RootTab.Modules)
                            },
                            onOpenSettings = { dest = Dest.Tab(RootTab.Settings) },
                        )

                        RootTab.Modules -> ModulesScreen(
                            custom = custom,
                            system = system,
                            segment = segment,
                            expertMode = expert,
                            dangerLock = dangerLock,
                            query = query,
                            onQuery = { query = it },
                            onSegment = { segment = it },
                            onModuleClick = { dest = Dest.Detail(it) },
                            onLoad = { id -> doLoad(id) },
                            onUnload = { id -> pendingUninstall = id },
                            onAutoStart = { id ->
                                updateAny(id) { it.copy(autoStart = !it.autoStart) }
                                scope.launch {
                                    val r = Repo.syncAutoStart(custom + system)
                                    toast = r.message
                                    bootAuto = custom.any { it.autoStart } || system.any { it.autoStart }
                                }
                            },
                            onLog = {
                                scope.launch {
                                    val m = findModule(it)
                                    if (m != null) toast = Repo.loadOrLog(m)
                                }
                            },
                            onInstall = { showInstall = true },
                        )

                        RootTab.Settings -> SettingsScreen(
                            dev = dev,
                            dirCount = dirs.size,
                            enabledAdvanced = listOf(expert, bootAuto, dangerLock).count { it },
                            onAdvanced = { dest = Dest.Advanced },
                            onSystemDirs = { dest = Dest.SystemDirs },
                        )

                        RootTab.Placeholder -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("预留位", fontSize = 12.sp, color = KColors.Ink3, fontWeight = FontWeight.Medium)
                        }
                    }

                    Dest.Advanced -> AdvancedSettingsScreen(
                        expertMode = expert,
                        bootAuto = bootAuto,
                        dangerLock = dangerLock,
                        onBack = { dest = Dest.Tab(RootTab.Settings) },
                        onExpertMode = { value ->
                            if (value && !expert) {
                                showExpertDialog = true
                            } else {
                                expert = value
                            }
                        },
                        onBootAuto = { value ->
                            bootAuto = value
                            if (!value) {
                                for (i in custom.indices) custom[i] = custom[i].copy(autoStart = false)
                                for (i in system.indices) system[i] = system[i].copy(autoStart = false)
                                scope.launch {
                                    toast = Repo.syncAutoStart(emptyList()).message
                                }
                            }
                        },
                        onDangerLock = { dangerLock = it },
                        onReset = {
                            expert = false
                            bootAuto = false
                            dangerLock = true
                            for (i in custom.indices) custom[i] = custom[i].copy(autoStart = false)
                        },
                    )

                    Dest.SystemDirs -> SystemDirsScreen(
                        dirs = dirs,
                        onBack = { dest = Dest.Tab(RootTab.Settings) },
                        onToggle = { path, enabled ->
                            dirs = dirs.map { if (it.path == path) it.copy(enabled = enabled) else it }
                            rescan()
                        },
                        onAddDir = { path ->
                            if (dirs.none { it.path == path }) {
                                dirs = dirs + DirUi(path, "用户添加", 0, true)
                                rescan()
                            }
                        },
                    )

                    is Dest.Detail -> {
                        val module = findModule(d.id)
                        if (module == null) {
                            dest = Dest.Tab(RootTab.Modules)
                        } else {
                            ModuleDetailScreen(
                                module = module,
                                dev = dev,
                                expertMode = expert,
                                dangerLock = dangerLock,
                                onBack = { dest = Dest.Tab(RootTab.Modules) },
                                onLoad = { doLoad(module.id) },
                                onUnload = { pendingUninstall = module.id },
                                onForceLoad = { doLoad(module.id) },
                            )
                        }
                    }
                }
            }

            if (dest !is Dest.Detail) {
                BottomNav(
                    selected = currentTab,
                    badgeCount = custom.count { !it.loaded && it.tier == Tier.Green },
                    onSelect = { dest = Dest.Tab(it) },
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        }

        toast?.let { msg ->
            Text(
                msg,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
                    .fillMaxWidth(0.9f)
                    .background(KColors.Ink.copy(alpha = 0.92f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color.White,
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
            )
        }

        if (scanning && custom.isEmpty() && system.isEmpty()) {
            Text(
                scanStage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp),
                color = KColors.Ink3,
                fontSize = 11.sp,
            )
        }
    }

    if (showExpertDialog) {
        ExpertModeDialog(
            onDismiss = { showExpertDialog = false },
            onConfirm = {
                expert = true
                showExpertDialog = false
            },
        )
    }

    if (showInstall) {
        InstallDialog(
            onDismiss = { showInstall = false },
            onImport = {
                showInstall = false
                importThenLoad = false
                picker.launch(arrayOf("*/*"))
            },
            onLoadNow = {
                showInstall = false
                importThenLoad = true
                picker.launch(arrayOf("*/*"))
            },
        )
    }

    pendingUninstall?.let { id ->
        val module = findModule(id)
        if (module == null) {
            pendingUninstall = null
        } else {
            UninstallConfirmDialog(
                name = module.name,
                bootCritical = module.bootCritical,
                onDismiss = { pendingUninstall = null },
                onConfirm = {
                    scope.launch {
                        val r = Repo.unload(module)
                        toast = r.message
                        syncLoaded()
                    }
                    pendingUninstall = null
                },
            )
        }
    }
}
