package com.kmi.loader.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmi.loader.model.ModuleOrigin
import com.kmi.loader.model.ModuleUi
import com.kmi.loader.model.RootTab
import com.kmi.loader.model.Tier

@Composable
fun KCard(
    modifier: Modifier = Modifier,
    padH: Dp = 18.dp,
    padV: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x0D0F0B2A), spotColor = Color(0x0D0F0B2A))
            .background(KColors.Card, RoundedCornerShape(24.dp))
            .padding(horizontal = padH, vertical = padV),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 6.dp, top = 18.dp, bottom = 8.dp),
        fontSize = 11.5.sp,
        letterSpacing = 0.4.sp,
        color = KColors.Ink3,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    mono: Boolean = false,
    valueColor: Color = KColors.Ink,
    divider: Boolean = true,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 12.8.sp, color = KColors.Ink2)
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                fontSize = if (mono) 12.6.sp else 13.sp,
                lineHeight = 18.sp,
                color = valueColor,
                fontFamily = if (mono) KType.Mono else FontFamily.Default,
                fontWeight = if (mono) FontWeight.Medium else FontWeight.Normal,
            )
        }
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(KColors.Line))
    }
}

@Composable
fun StatCard(number: String, label: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x0D0F0B2A), spotColor = Color(0x0D0F0B2A))
            .background(KColors.Card, RoundedCornerShape(22.dp))
            .then(if (onClick != null) Modifier.bouncyClick(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(number, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = KColors.Ink, letterSpacing = (-0.5).sp)
        Text(label, fontSize = 11.5.sp, color = KColors.Ink2, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
fun HeroCard(
    title: String,
    kmi: String,
    chips: List<String>,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(152.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(KColors.PrimaryDeep, KColors.Primary, KColors.PrimaryLight))),
    ) {
        Box(
            Modifier
                .size(190.dp)
                .offset(x = 250.dp, y = (-56).dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape),
        )
        Box(
            Modifier
                .size(120.dp)
                .offset(x = 210.dp, y = 96.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape),
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(title, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.72f), letterSpacing = 0.6.sp)
            Text(
                kmi,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = KType.Mono,
                letterSpacing = (-0.6).sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chips.forEach { chip ->
                    Text(
                        chip,
                        fontSize = 10.5.sp,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("详情", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.8f))
            Text(" ›", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

enum class TagKind { Green, Amber, Red, Blue, Gray }

@Composable
fun Tag(text: String, kind: TagKind) {
    val (bg, fg) = when (kind) {
        TagKind.Green -> KColors.GreenBg to KColors.GreenText
        TagKind.Amber -> KColors.AmberBg to KColors.AmberText
        TagKind.Red -> KColors.RedBg to KColors.RedText
        TagKind.Blue -> KColors.PrimaryBg to KColors.PrimaryDeep
        TagKind.Gray -> KColors.Track to KColors.Ink2
    }
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

enum class PillStyle { Primary, Soft, Done, Ghost, Disabled }

/** 卡片内的小胶囊按钮。需要更正式的确认按钮时用 [DialogButton]。 */
@Composable
fun PillButton(
    text: String,
    style: PillStyle = PillStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val fg = when (style) {
        PillStyle.Primary -> Color.White
        PillStyle.Soft -> KColors.PrimaryDeep
        PillStyle.Done -> KColors.GreenText
        PillStyle.Ghost -> KColors.Ink
        PillStyle.Disabled -> KColors.Ink3
    }
    val bgModifier = when (style) {
        PillStyle.Primary -> Modifier.background(
            Brush.linearGradient(listOf(KColors.Primary, KColors.PrimaryLight)),
            RoundedCornerShape(999.dp),
        )
        PillStyle.Soft -> Modifier.background(KColors.PrimaryBg, RoundedCornerShape(999.dp))
        PillStyle.Done -> Modifier.background(KColors.GreenBg, RoundedCornerShape(999.dp))
        PillStyle.Ghost -> Modifier.background(Color.White, RoundedCornerShape(999.dp))
        PillStyle.Disabled -> Modifier.background(KColors.Track, RoundedCornerShape(999.dp))
    }
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.55f)
            .then(
                if (style == PillStyle.Primary) {
                    Modifier.shadow(6.dp, RoundedCornerShape(999.dp), spotColor = Color(0x333A56E0))
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(999.dp))
            .then(bgModifier)
            .bouncyClick(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

/** 弹窗、大动作按钮用：更高的圆角矩形，不会被拉成一条扁胶囊。 */
@Composable
fun DialogButton(
    text: String,
    primary: Boolean = true,
    danger: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.6f)
            .then(
                when {
                    danger -> Modifier.background(KColors.RedBg, RoundedCornerShape(15.dp))
                    primary -> Modifier
                        .shadow(8.dp, RoundedCornerShape(15.dp), spotColor = Color(0x333A56E0))
                        .background(
                            Brush.linearGradient(listOf(KColors.Primary, KColors.PrimaryLight)),
                            RoundedCornerShape(15.dp),
                        )
                    else -> Modifier.background(KColors.Track, RoundedCornerShape(15.dp))
                }
            )
            .bouncyClick(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                danger -> KColors.RedText
                primary -> Color.White
                else -> KColors.Ink2
            },
            maxLines = 1,
        )
    }
}

@Composable
fun CircleButton(
    icon: ImageVector,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        if (active) KColors.PrimaryBg else KColors.Track,
        tween(180),
        label = "circleBg",
    )
    val tint by animateColorAsState(
        when {
            !enabled -> KColors.Disabled
            active -> KColors.PrimaryDeep
            else -> KColors.Ink2
        },
        tween(180),
        label = "circleTint",
    )
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bg)
            .bouncyClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, modifier = Modifier.size(17.dp), tint = tint)
    }
}

@Composable
fun SwitchPill(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val travel by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "knob",
    )
    val track by animateColorAsState(
        when {
            !enabled -> KColors.Track
            checked -> KColors.Primary
            else -> Color(0xFFE6E4EF)
        },
        tween(220),
        label = "track",
    )
    val checkAlpha by animateFloatAsState(if (checked) 1f else 0f, tween(160), label = "check")

    Box(
        Modifier
            .width(50.dp)
            .height(30.dp)
            .clip(CircleShape)
            .background(track)
            .bouncyClick(enabled = enabled) { onChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = travel)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Check,
                null,
                tint = KColors.Primary,
                modifier = Modifier.size(11.dp).alpha(checkAlpha),
            )
        }
    }
}

@Composable
fun Segmented(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(46.dp),
    ) {
        val count = labels.size.coerceAtLeast(1)
        val itemWidth = (maxWidth - 8.dp) / count
        val offset by animateDpAsState(
            targetValue = itemWidth * selected,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "segThumb",
        )

        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(999.dp))
                .background(KColors.TrackDeep)
                .padding(4.dp),
        ) {
            Box(
                Modifier
                    .offset(x = offset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White),
            )
            Row(Modifier.fillMaxSize()) {
                labels.forEachIndexed { index, label ->
                    val isSelected = index == selected
                    val color by animateColorAsState(
                        if (isSelected) KColors.Ink else KColors.Ink2,
                        tween(200),
                        label = "segColor",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontSize = 12.5.sp,
                            color = color,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFEFEEF5))
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, null, tint = KColors.Ink3, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(9.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 12.5.sp, color = KColors.Ink),
            cursorBrush = SolidColor(KColors.Primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 12.5.sp, color = KColors.Ink3)
                    }
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.Close,
                null,
                tint = KColors.Ink3,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onValueChange("") },
            )
        }
    }
}

@Composable
fun BottomNav(
    selected: RootTab,
    badgeCount: Int,
    onSelect: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x1A000000), spotColor = Color(0x1A000000))
            .background(Color.White, RoundedCornerShape(28.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem(RootTab.Home, "主页", Icons.Default.Home, selected == RootTab.Home, 0, onSelect)
        NavItem(RootTab.Modules, "模块", Icons.Default.List, selected == RootTab.Modules, badgeCount, onSelect)
        NavItem(RootTab.Settings, "设置", Icons.Default.Settings, selected == RootTab.Settings, 0, onSelect)
        NavItem(RootTab.Placeholder, "预留", Icons.Default.Star, selected == RootTab.Placeholder, 0, onSelect)
    }
}

@Composable
private fun RowScope.NavItem(
    tab: RootTab,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badge: Int,
    onSelect: (RootTab) -> Unit,
) {
    val placeholder = tab == RootTab.Placeholder
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "navProgress",
    )
    Box(
        Modifier
            .weight(1f)
            .height(50.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        KColors.Primary.copy(alpha = progress),
                        KColors.PrimaryLight.copy(alpha = progress),
                    ),
                ),
                RoundedCornerShape(22.dp),
            )
            .bouncyClick(enabled = !placeholder) { onSelect(tab) },
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.alpha(1f - progress), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    null,
                    tint = if (placeholder) KColors.Disabled.copy(alpha = 0.6f) else KColors.Ink2,
                    modifier = Modifier.size(19.dp),
                )
                if (badge > 0) {
                    Box(
                        Modifier
                            .offset(x = 13.dp, y = (-11).dp)
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(KColors.Red),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(badge.toString(), fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Row(Modifier.alpha(progress), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .bouncyClick { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.ArrowBack, null, tint = KColors.Ink, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = KColors.Ink)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun NavRow(
    title: String,
    subtitle: String,
    value: String = "",
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KColors.Ink)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 11.2.sp, color = KColors.Ink3, modifier = Modifier.padding(top = 3.dp))
            }
        }
        if (value.isNotEmpty()) {
            Text(value, fontSize = 12.sp, color = KColors.Ink2, modifier = Modifier.padding(end = 6.dp))
        }
        Text("›", fontSize = 16.sp, color = KColors.Ink3)
    }
}

@Composable
fun ModuleCard(
    module: ModuleUi,
    onClick: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onAutoStart: () -> Unit,
    onLog: () -> Unit,
) {
    val canLoad = module.tier == Tier.Green
    KCard(
        modifier = Modifier.bouncyClick(onClick = onClick),
        padH = 16.dp,
        padV = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                module.name,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = KColors.Ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (canLoad) {
                if (module.loaded) {
                    PillButton("已加载", PillStyle.Done, icon = Icons.Default.Check, onClick = onUnload)
                } else {
                    PillButton("加载", PillStyle.Primary, icon = Icons.Default.PlayArrow, onClick = onLoad)
                }
            }
        }
        Row(
            Modifier.padding(top = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "${module.version} · ${originText(module)} · ${module.sizeText}",
                fontSize = 10.8.sp,
                color = KColors.Ink3,
            )
            if (!canLoad) {
                if (module.loaded) {
                    Tag("运行中", TagKind.Blue)
                } else {
                    Tag(module.tierLabel, if (module.tier == Tier.Amber) TagKind.Amber else TagKind.Red)
                }
            }
        }
        Text(
            module.desc,
            fontSize = 12.2.sp,
            lineHeight = 18.sp,
            color = KColors.Ink2,
            maxLines = 2,
            modifier = Modifier.padding(top = 9.dp),
        )
        Row(
            Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(
                Icons.Default.PlayArrow,
                enabled = canLoad,
                active = module.autoStart,
                onClick = onAutoStart,
            )
            Spacer(Modifier.width(8.dp))
            CircleButton(Icons.Default.List, enabled = module.hasLog, active = false, onClick = onLog)
            Spacer(Modifier.weight(1f))
            if (!canLoad && !module.loaded) {
                Text("无加载入口 · 查看原因", fontSize = 11.sp, color = KColors.Ink3)
                Text("›", fontSize = 12.sp, color = KColors.Ink3)
            } else if (module.loaded) {
                PillButton("卸载", PillStyle.Ghost, onClick = onUnload)
            }
        }
    }
}

@Composable
fun SystemModuleCard(
    module: ModuleUi,
    onClick: () -> Unit,
    expertMode: Boolean,
    dangerLock: Boolean,
    onUnload: () -> Unit,
    onLoad: () -> Unit,
) {
    KCard(
        modifier = Modifier.bouncyClick(onClick = onClick),
        padH = 16.dp,
        padV = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                module.name,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = KColors.Ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (module.loaded) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(KColors.GreenBg)
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(KColors.Green))
                        Spacer(Modifier.width(5.dp))
                        Text("运行中", fontSize = 10.5.sp, color = KColors.GreenText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Text(
            "refcnt ${module.refcnt} · ${module.sizeText} · ${when {
                module.bootCritical -> "开机加载（modules.load）"
                module.bootListed -> "在 modules.load 清单，本次未加载"
                else -> "非开机列表"
            }}",
            fontSize = 10.8.sp,
            color = KColors.Ink3,
            modifier = Modifier.padding(top = 5.dp),
        )
        Text(
            module.desc,
            fontSize = 12.2.sp,
            lineHeight = 18.sp,
            color = KColors.Ink2,
            maxLines = 2,
            modifier = Modifier.padding(top = 9.dp),
        )
        Row(
            Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            when {
                // 内核不放行：专家模式也不解
                module.loaded && module.refcnt > 0 ->
                    LockedChip("被占用 · 不可卸", KColors.Track, KColors.Ink3)
                module.loaded && module.bootCritical && dangerLock ->
                    PillButton("卸载", PillStyle.Ghost, onClick = onUnload)
                // 普通系统模块：默认不给卸载入口，专家模式才放
                module.loaded && !expertMode ->
                    LockedChip("需专家 · 才能卸载", KColors.AmberBg, KColors.AmberText)
                module.loaded -> PillButton("卸载", PillStyle.Ghost, onClick = onUnload)
                // 未加载的系统模块：按档位决定能不能加载
                !module.loaded && module.tier == Tier.Green ->
                    PillButton("加载", PillStyle.Primary, onClick = onLoad)
                !module.loaded && module.tier == Tier.Amber && expertMode ->
                    LockedChip("专家 · 可强加", KColors.AmberBg, KColors.AmberText)
                !module.loaded && module.tier == Tier.Amber ->
                    LockedChip("需专家 · 才能加载", KColors.AmberBg, KColors.AmberText)
                else -> LockedChip("红档 · 禁止", KColors.Track, KColors.Ink3)
            }
        }
    }
}

@Composable
private fun LockedChip(text: String, bg: Color, fg: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Lock, null, tint = fg, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, fontSize = 10.5.sp, color = fg)
    }
}

private fun originText(module: ModuleUi): String = when (module.origin) {
    ModuleOrigin.Imported -> "导入"
    ModuleOrigin.Scanned -> "扫描"
    ModuleOrigin.System -> "系统"
}

/** 按下缩一下再回弹，替换掉默认的硬切。 */
@Composable
fun Modifier.bouncyClick(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}
