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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.kmi.loader.model.DeviceInfo
import com.kmi.loader.model.ModuleUi
import com.kmi.loader.model.Segment
import com.kmi.loader.ui.InfoRow
import com.kmi.loader.ui.KCard
import com.kmi.loader.ui.KColors
import com.kmi.loader.ui.SectionTitle
import com.kmi.loader.ui.StatCard

@Composable
fun HomeScreen(
    dev: DeviceInfo,
    customCount: Int,
    systemCount: Int,
    bootAuto: Boolean,
    autoList: List<ModuleUi>,
    onOpenModules: (Segment) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("KMI 加载器", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = KColors.Ink)
                Text("内核模块 · 加载与校验", fontSize = 11.sp, color = KColors.Ink3, modifier = Modifier.padding(top = 2.dp))
            }
            Text(
                appVersion(),
                fontSize = 10.5.sp,
                color = KColors.Ink3,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(KColors.Track)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        com.kmi.loader.ui.HeroCard(
            title = "GKI 内核基线",
            kmi = dev.kmi,
            chips = listOfNotNull(
                dev.gki,
                if (dev.gen > 0) "代数 ${dev.gen}" else null,
                if (dev.pageSize > 0) "${dev.pageSize} KB 页" else null,
            ),
        )

        if (!dev.supported && dev.kmi != "—") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(KColors.AmberBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "非 GKI 内核：${dev.kmi}，没有 KMI 代数。模块判定降级为 vermagic 直配，KMI 匹配暂不支持。",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = KColors.AmberText,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                number = systemCount.toString(),
                label = "系统内置模块",
                modifier = Modifier.weight(1f),
                onClick = { onOpenModules(Segment.System) },
            )
            StatCard(
                number = customCount.toString(),
                label = "自定义模块",
                modifier = Modifier.weight(1f),
                onClick = { onOpenModules(Segment.Custom) },
            )
        }

        SectionTitle("设备与内核")
        KCard {
            InfoRow("内核版本", dev.kernel, mono = true)
            InfoRow("KMI 基线", dev.kmi, mono = true, valueColor = KColors.PrimaryDeep)
            InfoRow(
                "GKI 代际",
                dev.gki,
                mono = true,
                valueColor = if (dev.supported) KColors.Ink else KColors.AmberText,
            )
            InfoRow("页大小", "${dev.pageSize} KB", mono = true)
            InfoRow("Android", dev.android, mono = true)
            InfoRow("Root 环境", dev.root)
            InfoRow("SELinux", dev.selinux, valueColor = KColors.GreenText, divider = false)
        }

        SectionTitle("加载能力")
        KCard {
            InfoRow("可加载模块", dev.loadAbility, mono = true, valueColor = KColors.GreenText)
            InfoRow("受保护符号", dev.protectedSig)
            InfoRow("模块签名", dev.moduleLoad)
            InfoRow("保护模块", dev.protectState, valueColor = KColors.GreenText, divider = false)
        }

        if (bootAuto) {
            SectionTitle("开机自启 · ${autoList.size} 个")
            KCard {
                if (autoList.isEmpty()) {
                    Text(
                        "还没有设置为开机自启的模块。在模块卡上点第一个圆钮即可加入。",
                        fontSize = 12.sp,
                       color = KColors.Ink3,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                } else {
                    autoList.forEachIndexed { index, module ->
                        InfoRow(
                            module.name,
                            if (module.loaded) "本次已加载" else "等待下次开机",
                            mono = true,
                            valueColor = if (module.loaded) KColors.GreenText else KColors.AmberText,
                            divider = index != autoList.lastIndex,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@androidx.compose.runtime.Composable
private fun appVersion(): String {
    val ctx = LocalContext.current
    return androidx.compose.runtime.remember {
        runCatching {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }
}
