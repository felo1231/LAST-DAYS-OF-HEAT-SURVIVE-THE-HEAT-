package com.example.ui.art

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BuildingType
import com.example.model.HeroClass
import com.example.model.HeroRarity
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private object DrawableBitmapCache {
    val cache = LruCache<String, ImageBitmap>(24)
}

@Composable
fun AsyncDrawableImage(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    maxDimPx: Int = 720,
    placeholderColor: Color = SlateAbyss
) {
    val context = LocalContext.current
    val cacheKey = remember(resId, maxDimPx) { "$resId@$maxDimPx" }
    var bitmap by remember(cacheKey) {
        mutableStateOf(DrawableBitmapCache.cache.get(cacheKey))
    }

    LaunchedEffect(cacheKey) {
        if (bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                DrawableBitmapCache.cache.get(cacheKey) ?: runCatching {
                    val res = context.applicationContext.resources
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeResource(res, resId, bounds)
                    var sample = 1
                    val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
                    while (maxSide / sample > maxDimPx) {
                        sample *= 2
                    }
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = sample.coerceAtLeast(1)
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    BitmapFactory.decodeResource(res, resId, opts)?.asImageBitmap()?.also {
                        DrawableBitmapCache.cache.put(cacheKey, it)
                    }
                }.getOrNull()
            }
            if (loaded != null) bitmap = loaded
        }
    }

    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(placeholderColor))
    }
}

enum class ResIconType { MEAT, WOOD, COAL, IRON, GEMS, MEALS, SPEEDUP, POWER }

@Composable
fun ResourceVectorIcon(
    type: ResIconType,
    size: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        when (type) {
            ResIconType.MEAT -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFCA5A5), Color(0xFFDC2626), Color(0xFF7F1D1D)),
                        center = Offset(w * 0.42f, h * 0.45f),
                        radius = w * 0.42f
                    ),
                    radius = w * 0.34f,
                    center = Offset(w * 0.44f, h * 0.54f)
                )
                drawLine(
                    color = PapyrusCream,
                    start = Offset(w * 0.62f, h * 0.36f),
                    end = Offset(w * 0.85f, h * 0.16f),
                    strokeWidth = w * 0.16f,
                    cap = StrokeCap.Round
                )
                drawCircle(PapyrusCream, radius = w * 0.09f, center = Offset(w * 0.85f, h * 0.16f))
                drawOval(
                    color = Color.White.copy(alpha = 0.55f),
                    topLeft = Offset(w * 0.24f, h * 0.34f),
                    size = Size(w * 0.22f, h * 0.12f)
                )
            }
            ResIconType.WOOD -> {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFF78350F))),
                    topLeft = Offset(w * 0.12f, h * 0.46f),
                    size = Size(w * 0.76f, h * 0.36f),
                    cornerRadius = CornerRadius(w * 0.14f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFD97706), Color(0xFF92400E))),
                    topLeft = Offset(w * 0.22f, h * 0.20f),
                    size = Size(w * 0.60f, h * 0.32f),
                    cornerRadius = CornerRadius(w * 0.12f)
                )
                drawRoundRect(
                    color = FrostWhite.copy(alpha = 0.9f),
                    topLeft = Offset(w * 0.24f, h * 0.18f),
                    size = Size(w * 0.56f, h * 0.10f),
                    cornerRadius = CornerRadius(w * 0.05f)
                )
                drawCircle(PapyrusCream, radius = w * 0.13f, center = Offset(w * 0.76f, h * 0.64f))
                drawCircle(Color(0xFF92400E), radius = w * 0.06f, center = Offset(w * 0.76f, h * 0.64f))
            }
            ResIconType.COAL -> {
                val path = Path().apply {
                    moveTo(w * 0.50f, h * 0.10f)
                    lineTo(w * 0.86f, h * 0.34f)
                    lineTo(w * 0.78f, h * 0.82f)
                    lineTo(w * 0.24f, h * 0.86f)
                    lineTo(w * 0.12f, h * 0.42f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF0F172A)))
                )
                drawPath(path = path, color = Color(0xFF64748B), style = Stroke(width = w * 0.05f))
                drawLine(FurnaceOrange, Offset(w * 0.34f, h * 0.66f), Offset(w * 0.60f, h * 0.42f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                drawLine(WarmGold, Offset(w * 0.50f, h * 0.52f), Offset(w * 0.68f, h * 0.64f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
            }
            ResIconType.IRON -> {
                val bar = Path().apply {
                    moveTo(w * 0.22f, h * 0.30f)
                    lineTo(w * 0.78f, h * 0.30f)
                    lineTo(w * 0.90f, h * 0.74f)
                    lineTo(w * 0.10f, h * 0.74f)
                    close()
                }
                drawPath(
                    path = bar,
                    brush = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF475569)))
                )
                drawLine(Color.White.copy(alpha = 0.85f), Offset(w * 0.25f, h * 0.34f), Offset(w * 0.75f, h * 0.34f), strokeWidth = w * 0.07f)
            }
            ResIconType.GEMS -> {
                val gem = Path().apply {
                    moveTo(w * 0.28f, h * 0.16f)
                    lineTo(w * 0.72f, h * 0.16f)
                    lineTo(w * 0.92f, h * 0.42f)
                    lineTo(w * 0.50f, h * 0.90f)
                    lineTo(w * 0.08f, h * 0.42f)
                    close()
                }
                drawPath(
                    path = gem,
                    brush = Brush.linearGradient(listOf(NileTurquoise, IceCyan, Color(0xFF1D4ED8)))
                )
                drawLine(Color.White.copy(alpha = 0.85f), Offset(w * 0.12f, h * 0.42f), Offset(w * 0.88f, h * 0.42f), strokeWidth = w * 0.05f)
                drawLine(WarmGold.copy(alpha = 0.75f), Offset(w * 0.50f, h * 0.16f), Offset(w * 0.50f, h * 0.88f), strokeWidth = w * 0.04f)
            }
            ResIconType.MEALS -> {
                drawArc(
                    brush = Brush.verticalGradient(listOf(WarmGold, FurnaceOrange)),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.12f, h * 0.24f),
                    size = Size(w * 0.76f, h * 0.64f)
                )
                drawLine(FrostWhite.copy(alpha = 0.75f), Offset(w * 0.35f, h * 0.38f), Offset(w * 0.38f, h * 0.18f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
                drawLine(FrostWhite.copy(alpha = 0.75f), Offset(w * 0.62f, h * 0.38f), Offset(w * 0.65f, h * 0.18f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
            }
            ResIconType.SPEEDUP -> {
                val bolt = Path().apply {
                    moveTo(w * 0.56f, h * 0.08f)
                    lineTo(w * 0.20f, h * 0.54f)
                    lineTo(w * 0.48f, h * 0.54f)
                    lineTo(w * 0.40f, h * 0.92f)
                    lineTo(w * 0.80f, h * 0.44f)
                    lineTo(w * 0.52f, h * 0.44f)
                    close()
                }
                drawPath(bolt, brush = Brush.verticalGradient(listOf(PapyrusCream, WarmGold)))
            }
            ResIconType.POWER -> {
                rotate(degrees = -35f) {
                    drawRoundRect(
                        color = Color(0xFFE2E8F0),
                        topLeft = Offset(w * 0.44f, h * 0.06f),
                        size = Size(w * 0.12f, h * 0.82f),
                        cornerRadius = CornerRadius(w * 0.04f)
                    )
                    drawRoundRect(
                        color = WarmGold,
                        topLeft = Offset(w * 0.28f, h * 0.66f),
                        size = Size(w * 0.44f, h * 0.10f),
                        cornerRadius = CornerRadius(w * 0.04f)
                    )
                }
                val shield = Path().apply {
                    moveTo(w * 0.22f, h * 0.20f)
                    lineTo(w * 0.78f, h * 0.20f)
                    lineTo(w * 0.74f, h * 0.64f)
                    lineTo(w * 0.50f, h * 0.88f)
                    lineTo(w * 0.26f, h * 0.64f)
                    close()
                }
                drawPath(
                    path = shield,
                    brush = Brush.verticalGradient(listOf(WarmGold, FurnaceOrange))
                )
                drawPath(path = shield, color = PapyrusCream, style = Stroke(width = w * 0.06f))
            }
        }
    }
}

@Composable
fun FurnaceVectorIllustration(
    level: Int,
    overdrive: Boolean,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "obelisk3D")
    val flicker by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(460), RepeatMode.Reverse),
        label = "solarFlicker"
    )
    val smokeRise by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "steamRise"
    )
    val sunSpin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "sunSpin"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Volumetric Radial Glow & Rotating Solar Rays
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.5f, h * 0.5f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        if (overdrive) FurnaceOrange.copy(alpha = 0.55f * flicker) else WarmGold.copy(alpha = 0.34f * flicker),
                        NileTurquoise.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = center
            )

            rotate(degrees = sunSpin, pivot = center) {
                for (deg in 0 until 360 step 30) {
                    val rad = deg * PI / 180.0
                    val sx = center.x + cos(rad).toFloat() * w * 0.38f
                    val sy = center.y + sin(rad).toFloat() * h * 0.38f
                    val ex = center.x + cos(rad).toFloat() * w * (if (overdrive) 0.49f else 0.45f)
                    val ey = center.y + sin(rad).toFloat() * h * (if (overdrive) 0.49f else 0.45f)
                    drawLine(
                        color = if (overdrive) FurnaceOrange.copy(alpha = 0.85f) else WarmGold.copy(alpha = 0.65f),
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = w * 0.024f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Custom 3D Solar Obelisk Render framed with beveled gold & turquoise glow
        AsyncDrawableImage(
            resId = R.drawable.img_3d_solar_obelisk,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            maxDimPx = 256,
            placeholderColor = SlateCard,
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .clip(CircleShape)
                .border(
                    width = if (overdrive) 2.5.dp else 1.8.dp,
                    brush = Brush.linearGradient(
                        if (overdrive) listOf(PapyrusCream, FurnaceOrange, WarmGold)
                        else listOf(WarmGold, NileTurquoise, WarmGold)
                    ),
                    shape = CircleShape
                )
        )

        // Animated Steam Puffs & Level Aura Ring Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            for (i in 0..2) {
                val phase = (smokeRise + i * 0.33f) % 1f
                val puffY = h * (0.34f - phase * 0.25f)
                val puffAlpha = (1f - phase) * (if (overdrive) 0.58f else 0.34f)
                drawCircle(
                    color = FrostWhite.copy(alpha = puffAlpha),
                    radius = w * (0.025f + phase * 0.042f),
                    center = Offset(w * 0.24f - phase * 0.06f * w, puffY)
                )
                drawCircle(
                    color = FrostWhite.copy(alpha = puffAlpha),
                    radius = w * (0.025f + phase * 0.042f),
                    center = Offset(w * 0.76f + phase * 0.06f * w, puffY)
                )
            }
            if (level >= 3) {
                drawCircle(
                    color = NileTurquoise.copy(alpha = 0.35f * flicker),
                    radius = w * 0.44f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = 1.6f)
                )
            }
        }
    }
}

@Composable
fun CityBuildingVectorIllustration(
    type: BuildingType,
    built: Boolean,
    smokePhase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        if (!built) {
            // 3D Isometric Foundation Plot with Corner Stakes
            val plot = Path().apply {
                moveTo(w * 0.50f, h * 0.28f)
                lineTo(w * 0.88f, h * 0.52f)
                lineTo(w * 0.50f, h * 0.78f)
                lineTo(w * 0.12f, h * 0.52f)
                close()
            }
            drawPath(plot, color = SlateElevated.copy(alpha = 0.55f))
            drawPath(plot, color = WarmGold.copy(alpha = 0.65f), style = Stroke(width = 2.2f))
            drawLine(NileTurquoise, Offset(w * 0.50f, h * 0.40f), Offset(w * 0.50f, h * 0.64f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(NileTurquoise, Offset(w * 0.36f, h * 0.52f), Offset(w * 0.64f, h * 0.52f), strokeWidth = 3f, cap = StrokeCap.Round)
            return@Canvas
        }

        // Ground Ambient Occlusion & Directional 3D Drop Shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.46f),
            topLeft = Offset(w * 0.12f, h * 0.74f),
            size = Size(w * 0.80f, h * 0.20f)
        )

        // Isometric 3D Stone Pedestal Base
        val padTop = Path().apply {
            moveTo(w * 0.50f, h * 0.58f)
            lineTo(w * 0.90f, h * 0.75f)
            lineTo(w * 0.50f, h * 0.92f)
            lineTo(w * 0.10f, h * 0.75f)
            close()
        }
        val padLeftRim = Path().apply {
            moveTo(w * 0.10f, h * 0.75f)
            lineTo(w * 0.50f, h * 0.92f)
            lineTo(w * 0.50f, h * 0.96f)
            lineTo(w * 0.10f, h * 0.79f)
            close()
        }
        val padRightRim = Path().apply {
            moveTo(w * 0.50f, h * 0.92f)
            lineTo(w * 0.90f, h * 0.75f)
            lineTo(w * 0.90f, h * 0.79f)
            lineTo(w * 0.50f, h * 0.96f)
            close()
        }
        drawPath(padTop, brush = Brush.linearGradient(listOf(SlateElevated, SlateCard)))
        drawPath(padLeftRim, color = SlateBorder)
        drawPath(padRightRim, color = SlateAbyss)

        // Colors per building type (Sunlit Left Wall, Shaded Right Wall, 3D Roof Accent)
        val (leftWallTop, leftWallBot, rightWall, roofPrimary) = when (type) {
            BuildingType.COAL_MINE -> Quad4(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFFF59E0B))
            BuildingType.SAWMILL -> Quad4(Color(0xFFD97706), Color(0xFF92400E), Color(0xFF5A2709), NileTurquoise)
            BuildingType.HUNTERS_HUT -> Quad4(Color(0xFFEA580C), Color(0xFF9A3412), Color(0xFF5D1D08), PapyrusCream)
            BuildingType.IRON_MINE -> Quad4(Color(0xFF64748B), Color(0xFF334155), Color(0xFF1E293B), FurnaceAmber)
            BuildingType.SHELTER -> Quad4(Color(0xFFE6A84C), Color(0xFFB4701E), Color(0xFF704210), WarmGold)
            BuildingType.COOKHOUSE -> Quad4(Color(0xFFF59E0B), Color(0xFFB45309), Color(0xFF78350F), FurnaceOrange)
            BuildingType.CLINIC -> Quad4(Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFF64748B), NileTurquoise)
            BuildingType.RESEARCH_LAB -> Quad4(Color(0xFF2563EB), Color(0xFF1E3A8A), Color(0xFF172554), IceCyan)
            BuildingType.BARRACKS -> Quad4(Color(0xFF7C3AED), Color(0xFF4C1D95), Color(0xFF2E1065), WarmGold)
            BuildingType.WATCHTOWER -> Quad4(Color(0xFF38BDF8), Color(0xFF1E3A8A), Color(0xFF0F172A), WarmGold)
        }

        // Brass Chimney & Animated Rising Steam for heated facilities
        if (type == BuildingType.COAL_MINE || type == BuildingType.IRON_MINE || type == BuildingType.COOKHOUSE ||
            type == BuildingType.CLINIC || type == BuildingType.SHELTER
        ) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(WarmGold, Color(0xFF78350F))),
                topLeft = Offset(w * 0.66f, h * 0.14f),
                size = Size(w * 0.10f, h * 0.26f),
                cornerRadius = CornerRadius(w * 0.02f)
            )
            val py = h * (0.12f - smokePhase * 0.11f)
            drawCircle(
                color = FrostWhite.copy(alpha = (1f - smokePhase) * 0.65f),
                radius = w * (0.045f + smokePhase * 0.045f),
                center = Offset(w * 0.71f, py)
            )
        }

        if (type == BuildingType.WATCHTOWER) {
            // Tall 3D Isometric Obelisk Beacon Tower
            val leftFace = Path().apply {
                moveTo(w * 0.50f, h * 0.12f)
                lineTo(w * 0.34f, h * 0.72f)
                lineTo(w * 0.50f, h * 0.84f)
                close()
            }
            val rightFace = Path().apply {
                moveTo(w * 0.50f, h * 0.12f)
                lineTo(w * 0.66f, h * 0.72f)
                lineTo(w * 0.50f, h * 0.84f)
                close()
            }
            drawPath(leftFace, brush = Brush.verticalGradient(listOf(leftWallTop, leftWallBot)))
            drawPath(rightFace, color = rightWall)
            drawCircle(
                brush = Brush.radialGradient(listOf(FrostWhite, IceCyan, Color.Transparent)),
                radius = w * 0.14f,
                center = Offset(w * 0.50f, h * 0.14f)
            )
            drawCircle(WarmGold, radius = w * 0.055f, center = Offset(w * 0.50f, h * 0.14f))
            drawLine(FrostWhite.copy(alpha = 0.8f), Offset(w * 0.50f, h * 0.16f), Offset(w * 0.50f, h * 0.83f), strokeWidth = 1.6f)
            return@Canvas
        }

        // Main 3D Isometric Building Volume: Sunlit Left Face + Shaded Right Face
        val roofY = if (type == BuildingType.BARRACKS || type == BuildingType.RESEARCH_LAB) h * 0.31f else h * 0.36f
        val leftFace = Path().apply {
            moveTo(w * 0.18f, roofY + h * 0.11f)
            lineTo(w * 0.50f, roofY + h * 0.22f)
            lineTo(w * 0.50f, h * 0.84f)
            lineTo(w * 0.18f, h * 0.71f)
            close()
        }
        val rightFace = Path().apply {
            moveTo(w * 0.50f, roofY + h * 0.22f)
            lineTo(w * 0.82f, roofY + h * 0.11f)
            lineTo(w * 0.82f, h * 0.71f)
            lineTo(w * 0.50f, h * 0.84f)
            close()
        }
        drawPath(leftFace, brush = Brush.verticalGradient(listOf(leftWallTop, leftWallBot)))
        drawPath(rightFace, color = rightWall)

        // 3D Isometric Roof Cap + Snow Frost Edge
        val roofSlab = Path().apply {
            moveTo(w * 0.50f, roofY - h * 0.08f)
            lineTo(w * 0.86f, roofY + h * 0.09f)
            lineTo(w * 0.50f, roofY + h * 0.23f)
            lineTo(w * 0.14f, roofY + h * 0.09f)
            close()
        }
        drawPath(roofSlab, brush = Brush.linearGradient(listOf(roofPrimary, roofPrimary.copy(alpha = 0.72f))))
        drawPath(roofSlab, color = FrostWhite.copy(alpha = 0.82f), style = Stroke(width = w * 0.03f))

        // Distinct 3D Architectural Signatures per Facility
        when (type) {
            BuildingType.RESEARCH_LAB -> {
                // 3D Astrolabe Dome & Golden Pyramid Finial
                drawArc(
                    brush = Brush.radialGradient(listOf(FrostWhite, IceCyan, Color(0xFF1D4ED8))),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.30f, h * 0.10f),
                    size = Size(w * 0.40f, h * 0.34f)
                )
                drawCircle(WarmGold, radius = w * 0.045f, center = Offset(w * 0.50f, h * 0.10f))
            }
            BuildingType.SAWMILL -> {
                // 3D Stacked Timber Logs + Spinning Bronze Sawblade
                drawCircle(WarmGold, radius = w * 0.11f, center = Offset(w * 0.32f, h * 0.62f))
                drawCircle(SlateAbyss, radius = w * 0.04f, center = Offset(w * 0.32f, h * 0.62f))
                drawCircle(Color(0xFF78350F), radius = w * 0.06f, center = Offset(w * 0.66f, h * 0.68f))
                drawCircle(PapyrusCream, radius = w * 0.035f, center = Offset(w * 0.66f, h * 0.68f))
            }
            BuildingType.COAL_MINE, BuildingType.IRON_MINE -> {
                // Glowing 3D Smelter / Mine Archway
                val arch = Path().apply {
                    moveTo(w * 0.26f, h * 0.74f)
                    lineTo(w * 0.26f, h * 0.55f)
                    quadraticTo(w * 0.35f, h * 0.48f, w * 0.44f, h * 0.60f)
                    lineTo(w * 0.44f, h * 0.80f)
                    close()
                }
                drawPath(arch, brush = Brush.verticalGradient(listOf(WarmGold, FurnaceOrange, SlateAbyss)))
            }
            BuildingType.CLINIC -> {
                // Glowing 3D Ankh Health Emblem on Left Temple Wall
                drawCircle(NileTurquoise, radius = w * 0.055f, center = Offset(w * 0.34f, h * 0.56f), style = Stroke(width = w * 0.03f))
                drawLine(NileTurquoise, Offset(w * 0.34f, h * 0.61f), Offset(w * 0.34f, h * 0.72f), strokeWidth = w * 0.03f)
                drawLine(NileTurquoise, Offset(w * 0.28f, h * 0.65f), Offset(w * 0.40f, h * 0.65f), strokeWidth = w * 0.03f)
            }
            BuildingType.BARRACKS -> {
                // Golden War Shield Crest & Pyramid Battlements
                val crest = Path().apply {
                    moveTo(w * 0.26f, h * 0.50f)
                    lineTo(w * 0.42f, h * 0.55f)
                    lineTo(w * 0.40f, h * 0.69f)
                    lineTo(w * 0.34f, h * 0.74f)
                    lineTo(w * 0.26f, h * 0.64f)
                    close()
                }
                drawPath(crest, brush = Brush.verticalGradient(listOf(PapyrusCream, WarmGold)))
            }
            else -> {
                // Pyramidion Finial on Shelter / Cookhouse / Hunter's Hut
                val apex = Path().apply {
                    moveTo(w * 0.50f, h * 0.14f)
                    lineTo(w * 0.65f, roofY + h * 0.07f)
                    lineTo(w * 0.50f, roofY + h * 0.13f)
                    lineTo(w * 0.35f, roofY + h * 0.07f)
                    close()
                }
                drawPath(apex, brush = Brush.verticalGradient(listOf(PapyrusCream, WarmGold)))
            }
        }

        // Specular 3D Corner Edge Highlight + Warm Illuminated Windows on Both Planes
        drawLine(FrostWhite.copy(alpha = 0.55f), Offset(w * 0.50f, roofY + h * 0.23f), Offset(w * 0.50f, h * 0.83f), strokeWidth = 1.5f)
        if (type != BuildingType.COAL_MINE && type != BuildingType.IRON_MINE && type != BuildingType.CLINIC) {
            drawRoundRect(
                color = WarmGold,
                topLeft = Offset(w * 0.27f, h * 0.58f),
                size = Size(w * 0.11f, h * 0.11f),
                cornerRadius = CornerRadius(w * 0.02f)
            )
        }
        drawRoundRect(
            color = FurnaceAmber.copy(alpha = 0.88f),
            topLeft = Offset(w * 0.61f, h * 0.57f),
            size = Size(w * 0.11f, h * 0.12f),
            cornerRadius = CornerRadius(w * 0.02f)
        )
    }
}

private data class Quad4(val a: Color, val b: Color, val c: Color, val d: Color)

@Composable
fun HeroPortraitArt(
    heroId: String,
    heroClass: HeroClass,
    rarity: HeroRarity,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "hero3DGlow")
    val glowPulse by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glowPulse"
    )

    // Use bespoke 3D rendered King & Queen portraits for all 8 Pharaonic heroes
    val rasterResId = when (heroId) {
        "h_sergey" -> R.drawable.img_3d_hero_horus
        "h_molly" -> R.drawable.img_3d_hero_amunet
        "h_bahiti" -> R.drawable.img_3d_hero_ahmose
        "h_patrick" -> R.drawable.img_3d_hero_djoser
        "h_zinman" -> R.drawable.img_3d_hero_khufu
        "h_natalia" -> R.drawable.img_3d_hero_hatshepsut
        "h_flint" -> R.drawable.img_3d_hero_thutmose
        "h_jasser" -> R.drawable.img_3d_hero_cleopatra
        else -> null
    }

    if (rasterResId != null) {
        Box(modifier = modifier) {
            AsyncDrawableImage(
                resId = rasterResId,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                maxDimPx = 256,
                placeholderColor = SlateCard,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle 3D specular rim & rarity aura overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent,
                            SlateAbyss.copy(alpha = 0.45f)
                        )
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accentColor.copy(alpha = 0.25f * glowPulse), Color.Transparent)
                    ),
                    radius = size.maxDimension * 0.6f,
                    center = Offset(size.width * 0.5f, size.height * 0.3f)
                )
            }
        }
        return
    }

    // Multi-layered 3D shaded vector portraits for remaining Pharaonic heroes
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val bgColors = when (rarity) {
            HeroRarity.MYTHIC -> listOf(Color(0xFF92400E), Color(0xFF4C1D95), SlateAbyss)
            HeroRarity.EPIC -> listOf(Color(0xFF1D4ED8), SlateSurface, SlateAbyss)
            HeroRarity.RARE -> listOf(Color(0xFF0F766E), SlateSurface, SlateAbyss)
        }
        drawRect(brush = Brush.radialGradient(bgColors, center = Offset(w * 0.45f, h * 0.35f), radius = w * 0.78f))

        // 3D Back Rim Halo
        drawCircle(
            brush = Brush.radialGradient(listOf(accentColor.copy(alpha = 0.36f * glowPulse), Color.Transparent)),
            radius = w * 0.44f,
            center = Offset(w * 0.50f, h * 0.42f)
        )

        drawHeroWeaponSilhouette(heroClass, accentColor, w, h)

        val nemesColor = when (heroId) {
            "h_flint" -> Color(0xFFB45309)
            "h_natalia" -> Color(0xFF831843)
            "h_patrick" -> Color(0xFF0F766E)
            "h_zinman" -> Color(0xFF9A3412)
            else -> Color(0xFF1E3A8A)
        }
        val nemes = Path().apply {
            moveTo(w * 0.20f, h * 0.34f)
            quadraticTo(w * 0.50f, h * 0.08f, w * 0.80f, h * 0.34f)
            lineTo(w * 0.86f, h * 0.74f)
            lineTo(w * 0.14f, h * 0.74f)
            close()
        }
        drawPath(
            nemes,
            brush = Brush.linearGradient(
                listOf(WarmGold, nemesColor, Color(0xFF091224)),
                start = Offset(w * 0.20f, h * 0.15f),
                end = Offset(w * 0.85f, h * 0.75f)
            )
        )
        drawLine(WarmGold.copy(alpha = 0.78f), Offset(w * 0.22f, h * 0.44f), Offset(w * 0.78f, h * 0.44f), strokeWidth = w * 0.028f)
        drawLine(WarmGold.copy(alpha = 0.78f), Offset(w * 0.18f, h * 0.56f), Offset(w * 0.82f, h * 0.56f), strokeWidth = w * 0.028f)

        // 3D Armored Torso & Fur Mantle with Shadowing
        val furColor = if (heroId == "h_natalia") PapyrusCream else Color(0xFFE2E8F0)
        val torso = Path().apply {
            moveTo(w * 0.12f, h * 0.99f)
            quadraticTo(w * 0.18f, h * 0.62f, w * 0.50f, h * 0.60f)
            quadraticTo(w * 0.82f, h * 0.62f, w * 0.88f, h * 0.99f)
            close()
        }
        drawPath(
            path = torso,
            brush = Brush.linearGradient(
                listOf(nemesColor, SlateSurface, SlateAbyss),
                start = Offset(w * 0.25f, h * 0.62f),
                end = Offset(w * 0.85f, h * 0.98f)
            )
        )
        drawArc(
            brush = Brush.horizontalGradient(listOf(WarmGold, NileTurquoise, PapyrusCream, WarmGold)),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.23f, h * 0.54f),
            size = Size(w * 0.54f, h * 0.28f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(furColor, Color(0xFF94A3B8)), center = Offset(w * 0.20f, h * 0.72f), radius = w * 0.11f),
            radius = w * 0.095f,
            center = Offset(w * 0.22f, h * 0.74f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(furColor, Color(0xFF94A3B8)), center = Offset(w * 0.76f, h * 0.72f), radius = w * 0.11f),
            radius = w * 0.095f,
            center = Offset(w * 0.78f, h * 0.74f)
        )

        // 3D Spherical Shaded Face
        drawOval(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFFEDD5), Color(0xFFE5A472), Color(0xFF8C4A22)),
                center = Offset(w * 0.45f, h * 0.39f),
                radius = w * 0.26f
            ),
            topLeft = Offset(w * 0.31f, h * 0.25f),
            size = Size(w * 0.38f, h * 0.38f)
        )

        // Beveled 3D Crown & Uraeus Jewel
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(PapyrusCream, WarmGold, FurnaceOrange)),
            topLeft = Offset(w * 0.30f, h * 0.23f),
            size = Size(w * 0.40f, h * 0.075f),
            cornerRadius = CornerRadius(w * 0.025f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White, if (heroId == "h_flint") FurnaceOrange else NileTurquoise),
                center = Offset(w * 0.49f, h * 0.22f),
                radius = w * 0.05f
            ),
            radius = w * 0.046f,
            center = Offset(w * 0.50f, h * 0.23f)
        )

        // Luminous Eyes
        val eyeColor = if (heroId == "h_flint") WarmGold else IceGlow
        drawCircle(eyeColor.copy(alpha = glowPulse), radius = w * 0.034f, center = Offset(w * 0.41f, h * 0.43f))
        drawCircle(eyeColor.copy(alpha = glowPulse), radius = w * 0.034f, center = Offset(w * 0.59f, h * 0.43f))
        drawCircle(Color.White, radius = w * 0.013f, center = Offset(w * 0.415f, h * 0.425f))
        drawCircle(Color.White, radius = w * 0.013f, center = Offset(w * 0.595f, h * 0.425f))

        // 3D Frost Silk Scarf
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(accentColor, nemesColor, SlateAbyss)),
            topLeft = Offset(w * 0.33f, h * 0.51f),
            size = Size(w * 0.34f, h * 0.11f),
            cornerRadius = CornerRadius(w * 0.045f)
        )
    }
}

private fun DrawScope.drawHeroWeaponSilhouette(
    heroClass: HeroClass,
    accent: Color,
    w: Float,
    h: Float
) {
    when (heroClass) {
        HeroClass.INFANTRY -> {
            val shield = Path().apply {
                moveTo(w * 0.06f, h * 0.24f)
                lineTo(w * 0.28f, h * 0.20f)
                lineTo(w * 0.26f, h * 0.62f)
                lineTo(w * 0.14f, h * 0.76f)
                lineTo(w * 0.05f, h * 0.58f)
                close()
            }
            drawPath(shield, brush = Brush.verticalGradient(listOf(WarmGold.copy(alpha = 0.55f), FurnaceOrange.copy(alpha = 0.28f))))
            drawPath(shield, color = WarmGold, style = Stroke(width = 2.8f))
        }
        HeroClass.LANCER -> {
            drawLine(
                color = WarmGold,
                start = Offset(w * 0.14f, h * 0.90f),
                end = Offset(w * 0.88f, h * 0.12f),
                strokeWidth = w * 0.048f,
                cap = StrokeCap.Round
            )
            val tip = Path().apply {
                moveTo(w * 0.94f, h * 0.06f)
                lineTo(w * 0.78f, h * 0.14f)
                lineTo(w * 0.86f, h * 0.24f)
                close()
            }
            drawPath(tip, color = accent)
        }
        HeroClass.MARKSMAN -> {
            drawArc(
                color = WarmGold,
                startAngle = 210f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(w * 0.66f, h * 0.16f),
                size = Size(w * 0.28f, h * 0.36f),
                style = Stroke(width = 3.8f)
            )
            drawLine(
                color = accent,
                start = Offset(w * 0.68f, h * 0.70f),
                end = Offset(w * 0.92f, h * 0.18f),
                strokeWidth = w * 0.048f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun FramedHeroAvatar(
    heroId: String,
    frameStyle: String,
    size: Dp = 44.dp,
    accentColor: Color = WarmGold,
    modifier: Modifier = Modifier
) {
    val frameColors = when (frameStyle) {
        "NILE_TURQUOISE" -> listOf(IceGlow, NileTurquoise, Color(0xFF0F766E), IceCyan)
        "MYTHIC_SOLAR" -> listOf(PapyrusCream, FurnaceOrange, MythicPurple, WarmGold)
        "LAPIS_IMPERIAL" -> listOf(IceCyan, Color(0xFF2563EB), WarmGold, SlateElevated)
        else -> listOf(PapyrusCream, WarmGold, FurnaceAmber, WarmGold)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(SlateCard)
            .border(2.2.dp, Brush.sweepGradient(frameColors), CircleShape)
            .padding(2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        HeroPortraitArt(
            heroId = heroId,
            heroClass = HeroClass.INFANTRY,
            rarity = HeroRarity.MYTHIC,
            accentColor = accentColor,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@Composable
fun EnemyMonsterPortrait(
    unitId: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val isBoss = unitId == "e_boss"
        val isCroc = unitId == "e_front_2"
        val isArcher = unitId == "e_back_1"

        drawRect(
            brush = Brush.radialGradient(
                listOf(
                    accentColor.copy(alpha = if (isBoss) 0.48f else 0.30f),
                    SlateSurface,
                    SlateAbyss
                ),
                center = Offset(w * 0.5f, h * 0.42f),
                radius = w * 0.72f
            )
        )

        when {
            isBoss -> {
                // Cryo Sphinx: Golden/Violet Nemes + Glowing Crown + Roaring Fangs
                val headdress = Path().apply {
                    moveTo(w * 0.14f, h * 0.28f)
                    quadraticTo(w * 0.50f, h * 0.05f, w * 0.86f, h * 0.28f)
                    lineTo(w * 0.90f, h * 0.86f)
                    lineTo(w * 0.10f, h * 0.86f)
                    close()
                }
                drawPath(headdress, brush = Brush.verticalGradient(listOf(MythicPurple, Color(0xFF3B0764), SlateAbyss)))
                drawCircle(
                    brush = Brush.radialGradient(listOf(IceGlow, MythicPurple, SlateAbyss)),
                    radius = w * 0.26f,
                    center = Offset(w * 0.50f, h * 0.46f)
                )
                drawLine(WarmGold, Offset(w * 0.36f, h * 0.22f), Offset(w * 0.64f, h * 0.22f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
                drawCircle(IceCyan, radius = w * 0.055f, center = Offset(w * 0.40f, h * 0.43f))
                drawCircle(IceCyan, radius = w * 0.055f, center = Offset(w * 0.60f, h * 0.43f))
                drawArc(WarmGold, 15f, 150f, false, Offset(w * 0.36f, h * 0.52f), Size(w * 0.28f, h * 0.16f), style = Stroke(w * 0.045f))
            }
            isCroc -> {
                // Nile Frost Croc: Armored Turquoise-Ice Scales & Amber Slit Eyes
                val snout = Path().apply {
                    moveTo(w * 0.18f, h * 0.52f)
                    quadraticTo(w * 0.50f, h * 0.14f, w * 0.82f, h * 0.52f)
                    lineTo(w * 0.72f, h * 0.82f)
                    lineTo(w * 0.28f, h * 0.82f)
                    close()
                }
                drawPath(snout, brush = Brush.verticalGradient(listOf(NileTurquoise, Color(0xFF0F766E), SlateAbyss)))
                drawCircle(FurnaceAmber, radius = w * 0.06f, center = Offset(w * 0.37f, h * 0.42f))
                drawCircle(FurnaceAmber, radius = w * 0.06f, center = Offset(w * 0.63f, h * 0.42f))
                drawLine(FrostWhite, Offset(w * 0.33f, h * 0.66f), Offset(w * 0.38f, h * 0.76f), strokeWidth = w * 0.04f)
                drawLine(FrostWhite, Offset(w * 0.50f, h * 0.68f), Offset(w * 0.50f, h * 0.78f), strokeWidth = w * 0.04f)
                drawLine(FrostWhite, Offset(w * 0.67f, h * 0.66f), Offset(w * 0.62f, h * 0.76f), strokeWidth = w * 0.04f)
            }
            isArcher -> {
                // Sand-Frost Archer: Hooded Dune Mask + Ice Bow Arc
                drawArc(
                    color = IceCyan,
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w * 0.64f, h * 0.16f),
                    size = Size(w * 0.28f, h * 0.55f),
                    style = Stroke(width = w * 0.055f)
                )
                drawCircle(
                    brush = Brush.verticalGradient(listOf(Color(0xFF475569), SlateAbyss)),
                    radius = w * 0.27f,
                    center = Offset(w * 0.46f, h * 0.48f)
                )
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(w * 0.24f, h * 0.46f),
                    size = Size(w * 0.44f, h * 0.22f),
                    cornerRadius = CornerRadius(w * 0.08f)
                )
                drawCircle(IceGlow, radius = w * 0.045f, center = Offset(w * 0.38f, h * 0.40f))
                drawCircle(IceGlow, radius = w * 0.045f, center = Offset(w * 0.54f, h * 0.40f))
            }
            else -> {
                // Frost Khopesh Raider: Horned Desert Helm & Crimson Visor
                drawCircle(
                    brush = Brush.verticalGradient(listOf(Color(0xFF991B1B), SlateAbyss)),
                    radius = w * 0.28f,
                    center = Offset(w * 0.50f, h * 0.50f)
                )
                drawLine(WarmGold, Offset(w * 0.22f, h * 0.34f), Offset(w * 0.14f, h * 0.14f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
                drawLine(WarmGold, Offset(w * 0.78f, h * 0.34f), Offset(w * 0.86f, h * 0.14f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
                drawCircle(FurnaceAmber, radius = w * 0.05f, center = Offset(w * 0.40f, h * 0.48f))
                drawCircle(FurnaceAmber, radius = w * 0.05f, center = Offset(w * 0.60f, h * 0.48f))
            }
        }
    }
}

