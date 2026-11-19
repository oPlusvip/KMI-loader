package com.kmi.loader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmi.loader.ui.KCard
import com.kmi.loader.ui.KColors
import com.kmi.loader.ui.NavRow
import com.kmi.loader.ui.SectionTitle
import com.kmi.loader.ui.SwitchPill
import com.kmi.loader.ui.TopBar

@Composable
fun AdvancedSettingsScreen(
    expertMode: Boolean,
    bootAuto: Boolean,
    dangerLock: Boolean,
    onBack: () -> Unit,
    onExpertMode: (Boolean) -> Unit,
    onBootAuto: (Boolean) -> Unit,
    onDangerLock: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopBar("高级设置", onBack)

        Column(Modifier.padding(horizontal = 14.dp)) {
            SectionTitle("加载与安全")
            KCard(padV = 2.dp) {
                SettingSwitchRow(
                    title = "专家模式",
                    subtitle = "黄档模块出现“强制加载”，红档永不放开",
                    checked = expertMode,
                    onChange = onExpertMode,
                )
                SettingDivider()
                SettingSwitchRow(
                    title = "开机自启",
                    subtitle = "开启后主页出现自启列表，改动下次开机生效",
                    checked = bootAuto,
                    onChange = onBootAuto,
                )
                SettingDivider()
                SettingSwitchRow(
                    title = "危险模块锁",
                    subtitle = "modules.load 内的模块默认禁止卸载",
                    checked = dangerLock,
                    onChange = onDangerLock,
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KColors.RedBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Default.Warning, null, tint = KColors.RedText, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "专家模式只跳过 App 层拦截，内核仍会执行自己的校验；强行加载可能导致无法开机，风险自负。",
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    color = KColors.RedText,
                )
            }

            Spacer(Modifier.height(14.dp))
            KCard {
                NavRow(
                    title = "重置高级设置",
                    subtitle = "关闭专家模式、清空自启列表",
                    onClick = onReset,
                )
            }

            Text(
                "危险模块锁关闭后，即便属启动关键模块也会显示卸载按钮，这一步同样需要二次确认。",
                fontSize = 10.5.sp,
                lineHeight = 16.sp,
                color = KColors.Ink3,
                modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KColors.Ink)
            Text(
                subtitle,
                fontSize = 10.8.sp,
                lineHeight = 15.sp,
                color = KColors.Ink3,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        SwitchPill(checked, onChange)
    }
}

@Composable
private fun SettingDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(KColors.Line))
}
