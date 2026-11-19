package com.kmi.loader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
private fun DialogShell(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val visible = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(180)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = springStiffness()),
            ),
        ) {
            Box(Modifier.padding(horizontal = 24.dp)) {
                content()
            }
        }
    }
}

private fun springStiffness() = 520f

@Composable
fun ExpertModeDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var remain by remember { mutableStateOf(5) }

    LaunchedEffect(Unit) {
        while (remain > 0) {
            delay(1000L)
            remain -= 1
        }
    }

    DialogShell(onDismiss = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(KColors.Card)
                .padding(20.dp),
        ) {
            Text("开启专家模式？", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KColors.Ink)
            Text(
                "开启后，黄档模块的详情页会出现「强制加载（专家）」。",
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                color = KColors.Ink2,
                modifier = Modifier.padding(top = 6.dp),
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(KColors.RedBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    "红档模块仍然不会放开。",
                    fontSize = 11.8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KColors.RedText,
                )
                Text(
                    "强行加载可能触发内核崩溃、无法开机，严重时需要清数据恢复。",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = KColors.RedText,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }

            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(KColors.GreenBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Check, null, tint = KColors.Green, modifier = Modifier.size(10.dp))
                }
                Spacer(Modifier.width(7.dp))
                Text("我已了解上述风险，并自行承担后果", fontSize = 10.8.sp, color = KColors.Ink2)
            }

            Row(Modifier.padding(top = 18.dp)) {
                DialogButton(
                    "取消",
                    primary = false,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                Spacer(Modifier.width(10.dp))
                if (remain > 0) {
                    DialogButton(
                        "请稍候 ${remain}s",
                        primary = true,
                        enabled = false,
                        modifier = Modifier.weight(1.2f),
                        onClick = {},
                    )
                } else {
                    DialogButton(
                        "确认开启",
                        primary = true,
                        modifier = Modifier.weight(1.2f),
                        onClick = onConfirm,
                    )
                }
            }

            Text(
                "倒计时结束后按钮变为「确认开启」",
                fontSize = 9.8.sp,
                color = KColors.Ink3,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            )
        }
    }
}

@Composable
fun InstallDialog(onDismiss: () -> Unit, onImport: () -> Unit, onLoadNow: () -> Unit) {
    DialogShell(onDismiss = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(KColors.Card)
                .padding(18.dp),
        ) {
            Text("安装模块", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = KColors.Ink)
            Text(
                "导入只是把 .ko 放到扫描目录并登记，不会立刻加载。",
                fontSize = 11.sp,
                color = KColors.Ink3,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            DialogRow("从文件导入", "选择本地 .ko 文件，复制到自定义目录") {
                onImport()
            }
            Spacer(Modifier.height(8.dp))
            DialogRow("立即加载", "直接选择一个路径下的模块并执行加载") {
                onLoadNow()
            }
            Spacer(Modifier.height(14.dp))
            DialogButton("取消", primary = false, modifier = Modifier.fillMaxWidth(), onClick = onDismiss)
        }
    }
}

@Composable
private fun DialogRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KColors.Track)
            .bouncyClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = KColors.Ink)
        Text(subtitle, fontSize = 10.5.sp, color = KColors.Ink3, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun UninstallConfirmDialog(
    name: String,
    bootCritical: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    DialogShell(onDismiss = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(KColors.Card)
                .padding(20.dp),
        ) {
            Text("卸载 $name？", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = KColors.Ink)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KColors.AmberBg)
                    .padding(horizontal = 11.dp, vertical = 9.dp),
            ) {
                Icon(Icons.Default.Warning, null, tint = KColors.AmberText, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    if (bootCritical) {
                        "这是开机阶段加载的模块，卸载后可能触发依赖它的服务异常，下次开机才会恢复。"
                    } else {
                        "卸载会立即从内核移除该模块，正在使用它的进程可能报错。"
                    },
                    fontSize = 10.8.sp,
                    lineHeight = 16.sp,
                    color = KColors.AmberText,
                )
            }
            Row(Modifier.padding(top = 18.dp)) {
                DialogButton("取消", primary = false, modifier = Modifier.weight(1f), onClick = onDismiss)
                Spacer(Modifier.width(10.dp))
                DialogButton(
                    "确认卸载",
                    danger = true,
                    modifier = Modifier.weight(1.2f),
                    onClick = onConfirm,
                )
            }
        }
    }
}

@Composable
fun AddDirDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("/data/local/modules") }

    DialogShell(onDismiss = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(KColors.Card)
                .padding(20.dp),
        ) {
            Text("添加扫描目录", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = KColors.Ink)
            Text(
                "填写设备上存放 .ko 的绝对路径。",
                fontSize = 11.5.sp,
                color = KColors.Ink3,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(KColors.Track)
                    .padding(horizontal = 14.dp, vertical = 15.dp),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.5.sp, color = KColors.Ink, fontFamily = KType.Mono),
                    cursorBrush = SolidColor(KColors.Primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(Modifier.padding(top = 16.dp)) {
                DialogButton("取消", primary = false, modifier = Modifier.weight(1f), onClick = onDismiss)
                Spacer(Modifier.width(10.dp))
                DialogButton(
                    "添加",
                    primary = true,
                    enabled = text.trim().startsWith("/"),
                    modifier = Modifier.weight(1.2f),
                    onClick = { onAdd(text.trim()) },
                )
            }
        }
    }
}
