package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameCatalog
import com.example.localization.LocalAppStrings
import com.example.model.*
import com.example.ui.art.AsyncDrawableImage
import com.example.ui.art.CityBuildingVectorIllustration
import com.example.ui.art.FurnaceVectorIllustration
import com.example.ui.theme.*
import kotlin.math.roundToInt
import kotlin.random.Random

private data class SnowFlake(val x: Float, val y: Float, val speed: Float, val size: Float, val layer: Int)

@Composable
fun CampMapScreen(
    state: GameState,
    highlightedBuildingId: String?,
    onTapFurnace: () -> Unit,
    onToggleOverdrive: () -> Unit,
    onTapBuilding: (String) -> Unit,
    onOpenSurvivors: () -> Unit,
    onOpenOrders: () -> Unit,
    onGoToChapterMission: (String) -> Unit,
    onClaimChapterMission: (String) -> Unit,
    onClaimChapterChest: (Int) -> Unit,
    onOpenQuestsModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val str = LocalAppStrings.current
    val infinite = rememberInfiniteTransition(label = "campAnim")
    val pulse by infinite.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "furnacePulse"
    )
    val snowTick by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3800, easing = LinearEasing), RepeatMode.Restart),
        label = "snowTick"
    )
    val smokePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "smokePhase"
    )
    val snowflakes = remember {
        List(95) { i ->
            SnowFlake(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = 0.55f + Random.nextFloat() * 1.45f,
                size = 1.5f + Random.nextFloat() * 3.6f,
                layer = i % 3
            )
        }
    }

    val sickCount = state.survivors.count { it.healthState == SurvivorHealthState.SICK }
    val aliveCount = state.survivors.count { it.healthState != SurvivorHealthState.DEAD }
    val maxCap = GameCatalog.maxSurvivorCapacity(state)
    val campTemp = GameCatalog.computeCampTempC(state)

    val chapterMissions = GameCatalog.chapterMissions.filter { it.chapter == state.quests.chapter }
    val activeMission = chapterMissions.firstOrNull { !state.quests.claimedChapterMissions.contains(it.id) }
    val allChapterDone = chapterMissions.isNotEmpty() && activeMission == null &&
        !state.quests.claimedChapterChests.contains(state.quests.chapter)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SlateAbyss)
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // 3D Isometric Pharaonic Citadel & Nile Horizon Backdrop
        AsyncDrawableImage(
            resId = R.drawable.img_3d_pharaoh_citadel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            maxDimPx = 720,
            placeholderColor = SlateAbyss,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (state.blizzard.isActive) 0.26f else 0.42f)
        )

        // Snowy Cliffs, Warm Furnace Lighting, Paths, Survivors & Layered Snow Particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.50f, h * 0.48f)

            // Surrounding Snow-Capped Basalt & Pyramid Ridges along top corners
            val leftPyramid = Path().apply {
                moveTo(0f, 0f); lineTo(w * 0.22f, 0f); lineTo(w * 0.09f, h * 0.22f); lineTo(0f, h * 0.34f); close()
            }
            val rightPyramid = Path().apply {
                moveTo(w, 0f); lineTo(w * 0.78f, 0f); lineTo(w * 0.91f, h * 0.23f); lineTo(w, h * 0.36f); close()
            }
            drawPath(leftPyramid, Brush.verticalGradient(listOf(Color(0xFF3F2E1A), Color(0xFF0F172A).copy(alpha = 0.45f))))
            drawPath(rightPyramid, Brush.verticalGradient(listOf(Color(0xFF3F2E1A), Color(0xFF0F172A).copy(alpha = 0.45f))))

            // Warm Orange-Gold Lighting Radiating from Central Furnace onto surrounding snow
            val baseRadius = size.minDimension * (0.28f + state.furnace.level * 0.02f) *
                (if (state.furnace.overdrive) 1.35f else 1.0f) * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (state.furnace.overdrive) {
                        listOf(
                            FurnaceOrange.copy(alpha = 0.52f),
                            WarmGold.copy(alpha = 0.26f),
                            FurnaceAmber.copy(alpha = 0.09f),
                            Color.Transparent
                        )
                    } else {
                        listOf(
                            WarmGold.copy(alpha = 0.32f),
                            FurnaceOrange.copy(alpha = 0.16f),
                            IceCyan.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    },
                    center = center,
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = center
            )

            // Heat boundary dashed ring
            drawCircle(
                color = if (state.furnace.overdrive) WarmGold.copy(alpha = 0.78f) else IceGlow.copy(alpha = 0.48f),
                radius = baseRadius * 0.82f,
                center = center,
                style = Stroke(
                    width = 2.4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                )
            )

            // Snowy wooden pathways connecting buildings to the central furnace
            state.buildings.forEach { b ->
                val bPos = Offset(w * b.gridX, h * b.gridY)
                drawLine(
                    color = Color(0xFF334155).copy(alpha = 0.7f),
                    start = center,
                    end = bPos,
                    strokeWidth = 6f
                )
                drawLine(
                    color = FrostWhite.copy(alpha = 0.22f),
                    start = center,
                    end = bPos,
                    strokeWidth = 2f
                )
            }

            // Animated Survivors walking on the camp map
            state.survivors.forEach { s ->
                if (s.healthState == SurvivorHealthState.DEAD) return@forEach
                val sx = w * s.posX
                val sy = h * s.posY
                val dotColor = when {
                    s.healthState == SurvivorHealthState.SICK -> CrimsonAlert
                    s.activity == SurvivorActivity.WORKING -> EmeraldSafe
                    s.activity == SurvivorActivity.EATING -> WarmGold
                    s.activity == SurvivorActivity.SLEEPING -> IceGlow
                    else -> MistText
                }
                drawCircle(color = SlateAbyss, radius = 9.5f, center = Offset(sx, sy))
                drawCircle(color = dotColor, radius = 6.5f, center = Offset(sx, sy))
            }

            // Continuous Layered Falling Snow Particles
            val activeFlakes = if (state.blizzard.isActive) snowflakes.size else 55
            for (i in 0 until activeFlakes) {
                val f = snowflakes[i]
                val layerScale = 0.65f + f.layer * 0.35f
                val speed = (if (state.blizzard.isActive) f.speed * 2.3f else f.speed * 0.75f) * layerScale
                val windX = if (state.blizzard.isActive) 0.42f else 0.08f
                val px = ((f.x + snowTick * windX * speed) % 1f) * w
                val py = ((f.y + snowTick * speed) % 1f) * h
                drawCircle(
                    color = FrostWhite.copy(alpha = (if (state.blizzard.isActive) 0.85f else 0.45f) * (0.5f + f.layer * 0.22f)),
                    radius = f.size * layerScale,
                    center = Offset(px, py)
                )
            }

            // Icy Frost Vignette around screen edges when temp < 0°C or Blizzard active
            if (campTemp < 0 || state.blizzard.isActive) {
                val edgeAlpha = if (state.blizzard.isActive) 0.42f else ((-campTemp) / 80f).coerceIn(0.10f, 0.28f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, IceCyan.copy(alpha = edgeAlpha)),
                        center = center,
                        radius = size.maxDimension * 0.68f
                    )
                )
            }
        }

        // Blizzard Banner Alert
        AnimatedVisibility(
            visible = state.blizzard.isActive || state.blizzard.countdownSec <= 15,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (state.blizzard.isActive) CrimsonAlert.copy(alpha = 0.94f)
                        else FurnaceOrange.copy(alpha = 0.92f)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.AcUnit, contentDescription = null, tint = SlateAbyss, modifier = Modifier.size(16.dp))
                Text(
                    text = if (state.blizzard.isActive)
                        "${str.blizzardActive} (${state.blizzard.activeRemainingSec}s)"
                    else
                        "${str.blizzardWarning} (${state.blizzard.countdownSec}s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = SlateAbyss,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Detailed Custom Vector City Buildings
        state.buildings.forEach { slot ->
            val nodeWidth = 88.dp
            val nodeHeight = 82.dp
            val isHighlighted = highlightedBuildingId == slot.id
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (widthPx * slot.gridX).roundToInt() - (nodeWidth.toPx() / 2f).roundToInt(),
                            y = (heightPx * slot.gridY).roundToInt() - (nodeHeight.toPx() / 2f).roundToInt()
                        )
                    }
                    .width(nodeWidth)
                    .defaultMinSize(minHeight = nodeHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (slot.level > 0) SlateCard.copy(alpha = 0.88f)
                        else SlateSurface.copy(alpha = 0.68f)
                    )
                    .border(
                        width = if (isHighlighted || slot.upgradeRemainingSec > 0) 2.dp else 1.dp,
                        color = when {
                            isHighlighted -> WarmGold
                            slot.upgradeRemainingSec > 0 -> FurnaceAmber
                            slot.level > 0 -> IceCyan.copy(alpha = 0.45f)
                            else -> SlateBorder.copy(alpha = 0.55f)
                        },
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onTapBuilding(slot.id) }
                    .testTag("building_${slot.id}")
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        CityBuildingVectorIllustration(
                            type = slot.type,
                            built = slot.level > 0,
                            smokePhase = smokePhase,
                            modifier = Modifier.size(44.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (slot.level > 0) WarmGold else IceCyan)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (slot.level > 0) "${slot.level}" else "+",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateAbyss
                            )
                        }
                    }

                    Text(
                        text = str.buildingName(slot.type),
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (slot.level > 0) FrostWhite else MistText
                    )

                    if (slot.upgradeRemainingSec > 0) {
                        LinearProgressIndicator(
                            progress = {
                                if (slot.upgradeTotalSec > 0) 1f - (slot.upgradeRemainingSec.toFloat() / slot.upgradeTotalSec) else 0.5f
                            },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = FurnaceAmber,
                            trackColor = SlateAbyss
                        )
                    } else if (GameCatalog.maxWorkersFor(slot) > 0 && slot.level > 0) {
                        Text(
                            text = "👷 ${slot.assignedWorkers}/${GameCatalog.maxWorkersFor(slot)}",
                            fontSize = 8.sp,
                            color = EmeraldSafe
                        )
                    }
                }
            }
        }

        // DETAILED CENTRAL FURNACE HUB
        val furnaceWidth = 112.dp
        val isFurnaceHighlighted = highlightedBuildingId == "furnace"
        Column(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (widthPx * 0.50f).roundToInt() - (furnaceWidth.toPx() / 2f).roundToInt(),
                        y = (heightPx * 0.48f).roundToInt() - 56.dp.toPx().roundToInt()
                    )
                }
                .width(furnaceWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(SlateSurface.copy(alpha = 0.72f))
                    .border(
                        width = if (isFurnaceHighlighted) 3.dp else 2.dp,
                        color = if (isFurnaceHighlighted || state.furnace.overdrive) WarmGold else IceGlow.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
                    .clickable { onTapFurnace() }
                    .testTag("central_furnace_button"),
                contentAlignment = Alignment.Center
            ) {
                FurnaceVectorIllustration(
                    level = state.furnace.level,
                    overdrive = state.furnace.overdrive,
                    modifier = Modifier.size(78.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateAbyss.copy(alpha = 0.88f))
                        .border(1.dp, WarmGold, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${str.levelShort}.${state.furnace.level}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (state.furnace.overdrive) listOf(FurnaceOrange, WarmGold)
                            else listOf(Color(0xFF15253A), Color(0xFF0E1B2B))
                        )
                    )
                    .border(1.dp, if (state.furnace.overdrive) WarmGold else IceCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable { onToggleOverdrive() }
                    .testTag("quick_overdrive_button")
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (state.furnace.overdrive) "🔥 ${str.overdrive}" else str.normalPower,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.furnace.overdrive) SlateAbyss else IceGlow,
                    maxLines = 1
                )
            }
        }

        // Bottom-Start: Authentic Floating Chapter Mission Banner with "Go / اذهب" & "Claim / استلام"
        if (activeMission != null || allChapterDone) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .widthIn(min = 210.dp, max = 258.dp)
                    .clickable { onOpenQuestsModal() }
                    .testTag("chapter_mission_tracker"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1728).copy(alpha = 0.95f)),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (allChapterDone) WarmGold else IceCyan.copy(alpha = 0.65f)
                )
            ) {
                if (allChapterDone) {
                    val reward = GameCatalog.chapterChestReward(state.quests.chapter)
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = WarmGold, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${str.chapter} ${state.quests.chapter}", style = MaterialTheme.typography.labelSmall, color = WarmGold)
                            Text(str.grandChest, style = MaterialTheme.typography.bodyMedium, color = FrostWhite, maxLines = 1)
                        }
                        Button(
                            onClick = { onClaimChapterChest(state.quests.chapter) },
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp).testTag("claim_chapter_chest_banner"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
                        ) {
                            Text(str.claimChest, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (activeMission != null) {
                    val (cur, req) = activeMission.progress(state)
                    val ready = cur >= req
                    Row(
                        modifier = Modifier.padding(9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = WarmGold, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "${str.chapter} ${state.quests.chapter} • $cur/$req",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarmGold
                                )
                            }
                            Text(
                                text = if (str.isRtl) activeMission.titleAr else activeMission.titleEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = FrostWhite,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                if (ready) onClaimChapterMission(activeMission.id)
                                else onGoToChapterMission(activeMission.id)
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 38.dp).testTag("chapter_mission_action_btn"),
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (ready) EmeraldSafe else WarmGold,
                                contentColor = SlateAbyss
                            )
                        ) {
                            Text(
                                text = if (ready) str.claim else str.goAction,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Bottom-End: Survivors, Chief Orders & Quests Quick Pills
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            horizontalAlignment = Alignment.End
        ) {
            FilledTonalButton(
                onClick = onOpenQuestsModal,
                modifier = Modifier.defaultMinSize(minHeight = 42.dp).testTag("open_quests_modal_btn"),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = SlateCard, contentColor = IceGlow),
                border = androidx.compose.foundation.BorderStroke(1.dp, IceCyan.copy(alpha = 0.55f))
            ) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = str.tabQuests, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(str.tabQuests, style = MaterialTheme.typography.labelMedium)
            }

            FilledTonalButton(
                onClick = onOpenSurvivors,
                modifier = Modifier.defaultMinSize(minHeight = 44.dp).testTag("open_survivors_button"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (sickCount > 0) CrimsonAlert.copy(alpha = 0.28f) else SlateCard,
                    contentColor = FrostWhite
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (sickCount > 0) CrimsonAlert else IceCyan.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Groups, contentDescription = str.survivorsTitle, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "$aliveCount/$maxCap" + (if (sickCount > 0) " 🏥$sickCount" else ""),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            FilledTonalButton(
                onClick = onOpenOrders,
                modifier = Modifier.defaultMinSize(minHeight = 44.dp).testTag("open_orders_button"),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = SlateCard, contentColor = WarmGold),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarmGold.copy(alpha = 0.55f))
            ) {
                Icon(Icons.Default.Gavel, contentDescription = str.chiefOrders, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text(text = str.chiefOrders, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

fun buildingIcon(type: BuildingType): ImageVector = when (type) {
    BuildingType.COAL_MINE -> Icons.Default.LocalFireDepartment
    BuildingType.SAWMILL -> Icons.Default.Park
    BuildingType.HUNTERS_HUT -> Icons.Default.Restaurant
    BuildingType.IRON_MINE -> Icons.Default.Hardware
    BuildingType.SHELTER -> Icons.Default.Cottage
    BuildingType.COOKHOUSE -> Icons.Default.SoupKitchen
    BuildingType.CLINIC -> Icons.Default.LocalHospital
    BuildingType.RESEARCH_LAB -> Icons.Default.Science
    BuildingType.BARRACKS -> Icons.Default.Shield
    BuildingType.WATCHTOWER -> Icons.Default.Visibility
}

fun buildingAccent(type: BuildingType): Color = when (type) {
    BuildingType.COAL_MINE -> FurnaceAmber
    BuildingType.SAWMILL -> EmeraldSafe
    BuildingType.HUNTERS_HUT -> CrimsonAlert
    BuildingType.IRON_MINE -> IceGlow
    BuildingType.SHELTER -> WarmGold
    BuildingType.COOKHOUSE -> FurnaceOrange
    BuildingType.CLINIC -> EmeraldSafe
    BuildingType.RESEARCH_LAB -> IceCyan
    BuildingType.BARRACKS -> MythicPurple
    BuildingType.WATCHTOWER -> RareBlue
}
