package com.kmi.loader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmi.loader.model.DeviceInfo
import com.kmi.loader.model.ModuleUi
import com.kmi.loader.model.ModuleOrigin
import com.kmi.loader.model.Tier
import com.kmi.loader.ui.InfoRow
import com.kmi.loader.ui.KCard
import com.kmi.loader.ui.KColors
import com.kmi.loader.ui.KType
import com.kmi.loader.ui.PillButton
import com.kmi.loader.ui.PillStyle
import com.kmi.loader.ui.SectionTitle
import com.kmi.loader.ui.Tag
import com.kmi.loader.ui.TagKind
import com.kmi.loader.ui.TopBar

private data class CheckRowData(val level: Int, val title: String, val detail: String)

@Composable
fun ModuleDetailScreen(
    module: ModuleUi,
    dev: DeviceInfo,
    expertMode: Boolean,
    dangerLock: Boolean,
    onBack: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onForceLoad: () -> Unit,
) {
    val isSystem = module.origin == ModuleOrigin.System

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = if (isSystem) "系统模块" else "模块详情",
            onBack = onBack,
            trailing = {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MoreVert, null, tint = KColors.Ink2, modifier = Modifier.size(18.dp))
                }
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
        ) {
            KCard(padH = 18.dp, padV = 16.dp) {
                Text(
                    module.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = KColors.Ink,
                    fontFamily = KType.Mono,
                )
                Row(
                    Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (module.loaded) {
                        Tag("运行中", TagKind.Blue)
                    } else {
                        when (module.tier) {
                            Tier.Green -> Tag(module.tierLabel, TagKind.Green)
                            Tier.Amber -> Tag(module.tierLabel, TagKind.Amber)
                            Tier.Red -> Tag(module.tierLabel, TagKind.Red)
                        }
                    }
                    if (!isSystem && module.tier == Tier.Amber && expertMode) {
                        Tag("专家模式", TagKind.Red)
                    }
                    if (isSystem && module.bootCritical) {
                        Tag("启动关键", TagKind.Amber)
                    }
                    if (isSystem && module.bootListed && !module.bootCritical) {
                        Tag("清单内 · 未加载", TagKind.Gray)
                    }
                }
            }

            if (!isSystem) {
                SectionTitle("兼容性比对")
                KCard {
                    InfoRow("模块 vermagic", module.vermagic, mono = true)
                    InfoRow("设备内核", dev.kernel, mono = true)
                    InfoRow(
                        "设备 KMI",
                        dev.kmi,
                        mono = true,
                        valueColor = if (module.tier == Tier.Red) KColors.RedText else if (module.tier == Tier.Amber) KColors.AmberText else KColors.PrimaryDeep,
                    )
                    InfoRow(
                        "页大小",
                        if (module.moduleAlignKb <= 0) "未知 / ${dev.pageSize} KB"
                        else "${module.moduleAlignKb} KB / ${dev.pageSize} KB",
                        mono = true,
                        valueColor = when {
                            module.moduleAlignKb <= 0 -> KColors.Ink2
                            module.moduleAlignKb != dev.pageSize -> KColors.RedText
                            else -> KColors.Ink
                        },
                        divider = false,
                    )
                }

                SectionTitle("加载前检查")
                KCard(padV = 8.dp) {
                    checksFor(module, dev).forEachIndexed { index, row ->
                        CheckRow(row)
                        if (index != checksFor(module, dev).lastIndex) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(KColors.Line))
                        }
                    }
                }

                SectionTitle("文件信息")
                KCard {
                    InfoRow("路径", module.path.ifEmpty { "—" }, mono = true)
                    InfoRow("大小", module.sizeText, mono = true)
                    InfoRow("架构", module.arch, mono = true)
                    InfoRow("依赖", module.depends.ifEmpty { "无" }, mono = true, divider = false)
                }
            } else {
                SectionTitle("运行状态")
                KCard {
                    InfoRow("引用计数", "refcnt ${module.refcnt}", mono = true)
                    InfoRow("持有者", module.holders.ifEmpty { "无" }, mono = true)
                    InfoRow(
                        "加载阶段",
                        when {
                            module.loaded && module.bootCritical -> "开机加载（modules.load）"
                            module.bootListed -> "未加载（在 modules.load 清单内，本次启动被跳过）"
                            module.loaded -> "运行时加载"
                            else -> "未加载"
                        },
                        mono = true,
                    )
                    InfoRow(
                        "可卸载",
                        when {
                            !module.loaded -> "未加载，无需卸载"
                            module.refcnt > 0 -> "否（被 ${module.refcnt} 处引用）"
                            else -> "是（refcnt 0）"
                        },
                        mono = true,
                        valueColor = when {
                            !module.loaded -> KColors.Ink2
                            module.refcnt > 0 -> KColors.RedText
                            else -> KColors.GreenText
                        },
                    )
                    InfoRow("来源", module.path.substringBeforeLast('/', "—").ifEmpty { "—" }, mono = true, divider = false)
                }

                SectionTitle("模块信息")
                KCard {
                    InfoRow("vermagic", module.vermagic, mono = true)
                    InfoRow("大小", module.sizeText, mono = true)
                    InfoRow("可卸载", if (module.builtin) "否（内置于内核）" else "是", divider = false)
                }
            }

            Text(
                noteText(module, expertMode, dangerLock),
                fontSize = 10.8.sp,
                lineHeight = 16.sp,
                color = KColors.Ink3,
                modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 12.dp),
            )
        }

        BottomActions(
            module = module,
            expertMode = expertMode,
            dangerLock = dangerLock,
            onBack = onBack,
            onLoad = onLoad,
            onUnload = onUnload,
            onForceLoad = onForceLoad,
        )
    }
}

@Composable
private fun BottomActions(
    module: ModuleUi,
    expertMode: Boolean,
    dangerLock: Boolean,
    onBack: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onForceLoad: () -> Unit,
) {
    val isSystem = module.origin == ModuleOrigin.System

    Column(
        Modifier
            .fillMaxWidth()
            .background(KColors.Card),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(KColors.Line))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BarButton("返回", modifier = Modifier.weight(1f), onClick = onBack)

            when {
                isSystem -> when {
                    module.loaded && module.refcnt > 0 ->
                        BarButton("被占用 · 不可卸载", modifier = Modifier.weight(1.7f), enabled = false, onClick = {})
                    module.loaded && module.bootCritical && dangerLock ->
                        BarButton("卸载模块", modifier = Modifier.weight(1.7f), onClick = onUnload)
                    module.loaded && !expertMode ->
                        BarButton("需专家模式才能卸载", modifier = Modifier.weight(1.7f), enabled = false, onClick = {})
                    module.loaded ->
                        BarButton("卸载模块", modifier = Modifier.weight(1.7f), onClick = onUnload)
                    module.tier == Tier.Green ->
                        BarButton("加载模块", modifier = Modifier.weight(1.7f), primary = true, onClick = onLoad)
                    module.tier == Tier.Amber && expertMode ->
                        BarButton("强制加载（专家）", modifier = Modifier.weight(1.7f), primary = true, onClick = onForceLoad)
                    module.tier == Tier.Amber ->
                        BarButton("需专家模式", modifier = Modifier.weight(1.7f), enabled = false, onClick = {})
                    else ->
                        BarButton("红档禁止加载", modifier = Modifier.weight(1.7f), enabled = false, onClick = {})
                }
                module.tier == Tier.Green && module.loaded ->
                    BarButton("卸载模块", modifier = Modifier.weight(1.7f), onClick = onUnload)
                module.tier == Tier.Green ->
                    BarButton("加载模块", modifier = Modifier.weight(1.7f), primary = true, onClick = onLoad)
                module.tier == Tier.Amber && expertMode ->
                    BarButton("强制加载（专家）", modifier = Modifier.weight(1.7f), primary = true, onClick = onForceLoad)
                module.tier == Tier.Amber ->
                    BarButton("需专家模式", modifier = Modifier.weight(1.7f), enabled = false, onClick = {})
                else ->
                    BarButton("红档禁止加载", modifier = Modifier.weight(1.7f), enabled = false, onClick = {})
            }
        }
    }
}

/** 详情页底部大按钮：52dp 高、16dp 圆角，避免被拉成贴底的细胶囊。 */
@Composable
private fun BarButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                when {
                    !enabled -> Modifier.background(KColors.Track)
                    primary -> Modifier.background(
                        Brush.linearGradient(listOf(KColors.Primary, KColors.PrimaryLight)),
                    )
                    else -> Modifier.background(Color(0xFFF1F0F7))
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                !enabled -> KColors.Ink3
                primary -> Color.White
                else -> KColors.Ink
            },
        )
    }
}

@Composable
private fun CheckRow(row: CheckRowData) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    when (row.level) {
                        0 -> KColors.GreenBg
                        1 -> KColors.AmberBg
                        else -> KColors.RedBg
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (row.level) {
                0 -> Icon(Icons.Default.Check, null, tint = KColors.Green, modifier = Modifier.size(11.dp))
                else -> Icon(
                    if (row.level == 1) Icons.Default.Warning else Icons.Default.Close,
                    null,
                    tint = if (row.level == 1) KColors.Amber else KColors.Red,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(row.title, fontSize = 12.sp, color = KColors.Ink, fontWeight = FontWeight.Medium)
            if (row.detail.isNotEmpty()) {
                Text(row.detail, fontSize = 10.2.sp, lineHeight = 15.sp, color = KColors.Ink3, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

private fun checksFor(module: ModuleUi, dev: DeviceInfo): List<CheckRowData> {
    val list = mutableListOf<CheckRowData>()

    // 架构：来自 vermagic 末尾的架构令牌
    if (module.arch.isNotEmpty() && module.arch != "—") {
        list += CheckRowData(0, "架构 ${module.arch}", "vermagic 中的架构令牌")
    } else {
        list += CheckRowData(1, "架构未识别", "vermagic 里没有架构令牌")
    }

    // KMI / 内核版本：用真实判定理由
    when (module.tier) {
        Tier.Green -> list += CheckRowData(0, "内核基线一致", module.reason)
        Tier.Amber -> list += CheckRowData(1, "存在兼容风险", module.reason)
        Tier.Red -> list += CheckRowData(2, "不可加载", module.reason)
    }

    // 页大小
    when {
        module.moduleAlignKb <= 0 -> list += CheckRowData(1, "页大小未知", "模块没有可读的 PT_LOAD 对齐信息")
        dev.pageSize <= 0 -> list += CheckRowData(1, "设备页大小未知", "getconf 没取到，按 4 KB 兜底判定")
        module.moduleAlignKb != dev.pageSize ->
            list += CheckRowData(2, "页大小不符", "模块按 ${module.moduleAlignKb} KB 对齐，设备为 ${dev.pageSize} KB")
        else -> list += CheckRowData(0, "页大小一致", "${dev.pageSize} KB")
    }

    // 依赖
    list += if (module.depends.isEmpty()) {
        CheckRowData(0, "无其他依赖", "depends 为空")
    } else {
        CheckRowData(1, "依赖（${module.depends}）", "依赖模块必须在同一批次先加载")
    }

    // 签名：这里只声明我们不校验，不做结论
    list += CheckRowData(1, "未校验签名", "预检不验证书链，最终以内核 CONFIG_MODULE_SIG 为准")

    return list
}

private fun noteText(module: ModuleUi, expertMode: Boolean, dangerLock: Boolean): String {
    if (module.origin == ModuleOrigin.System) {
        return when {
            module.loaded && module.refcnt > 0 ->
                "正在内核中运行，被 ${module.refcnt} 处引用，rmmod 会返回 EBUSY。这是内核层面的拒绝，专家模式也不会改变。"
            module.loaded && module.bootCritical && dangerLock ->
                "它在厂商 modules.load 列表里，卸载会弹确认框提示风险；危险模块锁只是把这条提示加强，不会再拦着不让卸。"
            module.loaded && !expertMode ->
                "普通系统模块。卸载默认不给入口，开启专家模式后才会出现「卸载」按钮。"
            module.loaded ->
                "专家模式已开启且当前无引用，可以 rmmod；若厂商开机脚本会重新拉起，重启后仍会回来。"
            else ->
                "来自系统分区但当前未加载，能否加载由 vermagic / 页大小 / KMI 三档判定，与是否内置无关。"
        }
    }
    return when {
        module.tier == Tier.Red -> "红档永远不提供加载入口，开启专家模式也不会放开。"
        module.tier == Tier.Amber && expertMode -> "黄档在专家模式下允许强制加载一次，失败会写入日志，可回到这里查看原因。"
        module.tier == Tier.Amber -> "黄档默认不提供加载入口，需要到「高级设置」开启专家模式后才能尝试强制加载。"
        else -> "通过预检不等于一定能加载，内核的 vermagic 与符号 CRC 才是最终判据，失败会在这里给出具体 errno。"
    }
}
