package com.kmi.loader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmi.loader.model.ModuleUi
import com.kmi.loader.model.Segment
import com.kmi.loader.ui.KColors
import com.kmi.loader.ui.ModuleCard
import com.kmi.loader.ui.SearchField
import com.kmi.loader.ui.Segmented
import com.kmi.loader.ui.SystemModuleCard
import com.kmi.loader.ui.bouncyClick

@Composable
fun ModulesScreen(
    custom: List<ModuleUi>,
    system: List<ModuleUi>,
    segment: Segment,
    expertMode: Boolean,
    dangerLock: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onSegment: (Segment) -> Unit,
    onModuleClick: (String) -> Unit,
    onLoad: (String) -> Unit,
    onUnload: (String) -> Unit,
    onAutoStart: (String) -> Unit,
    onLog: (String) -> Unit,
    onInstall: () -> Unit,
) {
    val source = if (segment == Segment.Custom) custom else system
    val filtered = if (query.isBlank()) {
        source
    } else {
        source.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.desc.contains(query, ignoreCase = true) ||
                it.vermagic.contains(query, ignoreCase = true) ||
                it.tierLabel.contains(query, ignoreCase = true)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("模块", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = KColors.Ink)
                Spacer(Modifier.weight(1f))
                Text("${custom.size + system.size} 个", fontSize = 11.sp, color = KColors.Ink3)
            }

            SearchField(
                value = query,
                onValueChange = onQuery,
                placeholder = "搜索模块名、路径或 vermagic",
                modifier = Modifier.padding(horizontal = 14.dp),
            )

            Spacer(Modifier.height(12.dp))

            Segmented(
                labels = listOf("自定义 ${custom.size}", "系统内置 ${system.size}"),
                selected = if (segment == Segment.Custom) 0 else 1,
                onSelect = { onSegment(if (it == 0) Segment.Custom else Segment.System) },
                modifier = Modifier.padding(horizontal = 14.dp),
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("没有匹配的模块", fontSize = 12.sp, color = KColors.Ink3)
                        }
                    }
                }
                if (segment == Segment.Custom) {
                    items(filtered, key = { it.id }) { module ->
                        ModuleCard(
                            module = module,
                            onClick = { onModuleClick(module.id) },
                            onLoad = { onLoad(module.id) },
                            onUnload = { onUnload(module.id) },
                            onAutoStart = { onAutoStart(module.id) },
                            onLog = { onLog(module.id) },
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { module ->
                        SystemModuleCard(
                            module = module,
                            onClick = { onModuleClick(module.id) },
                            expertMode = expertMode,
                            dangerLock = dangerLock,
                            onUnload = { onUnload(module.id) },
                            onLoad = { onLoad(module.id) },
                        )
                    }
                }
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 4.dp)
                .shadow(10.dp, RoundedCornerShape(15.dp), spotColor = Color(0x553A56E0))
                .clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(listOf(KColors.Primary, KColors.PrimaryLight)))
                .bouncyClick(onClick = onInstall)
                .height(46.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("安装", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}
