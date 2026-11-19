package com.kmi.loader.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmi.loader.model.DeviceInfo
import com.kmi.loader.ui.InfoRow
import com.kmi.loader.ui.KCard
import com.kmi.loader.ui.KColors
import com.kmi.loader.ui.NavRow
import com.kmi.loader.ui.SectionTitle

@Composable
fun SettingsScreen(
    dev: DeviceInfo,
    dirCount: Int,
    enabledAdvanced: Int,
    onAdvanced: () -> Unit,
    onSystemDirs: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
    ) {
        Text(
            "设置",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = KColors.Ink,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 14.dp),
        )

        SectionTitle("加载与安全")
        KCard {
            NavRow(
                title = "高级设置",
                subtitle = "专家模式 · 开机自启 · 危险模块锁",
                value = "$enabledAdvanced 项已开",
                onClick = onAdvanced,
            )
            NavRow(
                title = "系统模块设置",
                subtitle = "扫描目录 · $dirCount 个",
                value = "",
                onClick = onSystemDirs,
            )
        }

        SectionTitle("外观")
        KCard {
            InfoRow("主题", "跟随系统", divider = true)
            InfoRow("语言", "简体中文", divider = false)
        }

        SectionTitle("关于")
        KCard {
            InfoRow("当前 KMI", dev.kmi, mono = true)
            InfoRow("版本", appVersionName(), mono = true)
            InfoRow("导出日志", "›", divider = false)
        }

        Text(
            "所有判断都在本机完成，不上传任何设备信息。",
            fontSize = 10.5.sp,
            color = KColors.Ink3,
            modifier = Modifier.padding(start = 6.dp, top = 14.dp),
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun appVersionName(): String {
    val ctx = LocalContext.current
    return androidx.compose.runtime.remember {
        runCatching {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }
}
