package com.kmi.loader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmi.loader.model.DirUi
import com.kmi.loader.ui.KCard
import com.kmi.loader.ui.AddDirDialog
import com.kmi.loader.ui.KColors
import com.kmi.loader.ui.KType
import com.kmi.loader.ui.SectionTitle
import com.kmi.loader.ui.SwitchPill
import com.kmi.loader.ui.TopBar

@Composable
fun SystemDirsScreen(
    dirs: List<DirUi>,
    onBack: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onAddDir: (String) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopBar("系统模块设置", onBack)

        Column(Modifier.padding(horizontal = 14.dp)) {
            SectionTitle("扫描目录")
            KCard(padV = 2.dp) {
                dirs.forEachIndexed { index, dir ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(dir.path, fontSize = 12.5.sp, fontFamily = KType.Mono, color = KColors.Ink)
                            Text(
                                "${dir.sub} · ${dir.count} 个",
                                fontSize = 10.2.sp,
                                color = KColors.Ink3,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        SwitchPill(dir.enabled, { onToggle(dir.path, it) })
                    }
                    if (index != dirs.lastIndex) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(KColors.Line))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            KCard(padV = 2.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showAdd = true }
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("＋ 添加目录", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = KColors.PrimaryDeep)
                }
            }

            Text(
                "修改目录后需要重新扫描才生效。扫描只读取文件信息，不会加载任何模块。",
                fontSize = 10.5.sp,
                lineHeight = 16.sp,
                color = KColors.Ink3,
                modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 20.dp),
            )
        }
    }

    if (showAdd) {
        AddDirDialog(
            onDismiss = { showAdd = false },
            onAdd = {
                onAddDir(it)
                showAdd = false
            },
        )
    }
}
