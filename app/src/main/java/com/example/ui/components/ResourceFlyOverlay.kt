package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FlyResourceType
import com.example.model.ResourceFlyEvent
import com.example.ui.art.ResIconType
import com.example.ui.art.ResourceVectorIcon
import com.example.ui.theme.*
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ResourceFlyOverlay(
    events: List<ResourceFlyEvent>,
    modifier: Modifier = Modifier
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        events.forEach { event ->
            key(event.id) {
                val progress = remember { Animatable(0f) }
                LaunchedEffect(event.id) {
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
                    )
                }

                val t = progress.value
                // Target X in the Top HUD resource bar
                val hudTargetFracX = when (event.type) {
                    FlyResourceType.MEAT -> 0.14f
                    FlyResourceType.WOOD -> 0.28f
                    FlyResourceType.COAL -> 0.43f
                    FlyResourceType.IRON -> 0.57f
                    FlyResourceType.GEMS -> 0.71f
                }.let { if (isRtl) 1f - it else it }

                val startX = widthPx * event.originX
                val startY = heightPx * event.originY

                val curX: Float
                val curY: Float
                val scale: Float
                val alpha: Float

                if (event.isConsumption) {
                    // Gently shrinks & fades downward/inward at consumption site
                    curX = startX + sin(t * 4f) * 14f
                    curY = startY + t * 28f
                    scale = (1.05f - t * 0.72f).coerceAtLeast(0.25f)
                    alpha = (1f - t * t).coerceIn(0f, 1f)
                } else {
                    // Arcs smoothly into the Top HUD while gently shrinking and fading
                    val endX = widthPx * hudTargetFracX
                    val endY = heightPx * 0.085f
                    val arcOffset = sin(t * Math.PI.toFloat()) * (-widthPx * 0.05f)
                    curX = startX + (endX - startX) * t + arcOffset
                    curY = startY + (endY - startY) * t
                    scale = (1.15f - t * 0.75f).coerceAtLeast(0.35f)
                    alpha = if (t < 0.65f) 1f else (1f - (t - 0.65f) / 0.35f).coerceIn(0f, 1f)
                }

                val resIcon = when (event.type) {
                    FlyResourceType.MEAT -> ResIconType.MEAT
                    FlyResourceType.WOOD -> ResIconType.WOOD
                    FlyResourceType.COAL -> ResIconType.COAL
                    FlyResourceType.IRON -> ResIconType.IRON
                    FlyResourceType.GEMS -> ResIconType.GEMS
                }
                val badgeColor = if (event.isConsumption) CrimsonAlert else EmeraldSafe

                Row(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = curX.roundToInt() - 26.dp.toPx().roundToInt(),
                                y = curY.roundToInt() - 14.dp.toPx().roundToInt()
                            )
                        }
                        .scale(scale)
                        .alpha(alpha)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0A1423).copy(alpha = 0.88f))
                        .border(1.dp, badgeColor.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ResourceVectorIcon(type = resIcon, size = 16.dp)
                    Text(
                        text = if (event.isConsumption) "-${event.amount}" else "+${event.amount}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }
        }
    }
}
