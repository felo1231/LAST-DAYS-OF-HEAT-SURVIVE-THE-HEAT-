package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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
import com.example.ui.art.FramedHeroAvatar
import com.example.ui.art.FurnaceVectorIllustration
import com.example.ui.art.ResIconType
import com.example.ui.art.ResourceVectorIcon
import com.example.ui.components.formatCompact
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun WorldMapScreen(
    state: GameState,
    currentUserId: String,
    worldCitadels: List<WorldCitadel>,
    alliances: List<AllianceInfo>,
    selectedNodeId: String?,
    onSelectNode: (String?) -> Unit,
    onDispatchMarch: (String) -> Unit,
    onOpenAllianceModal: () -> Unit,
    onDispatchCitadelExpedition: (String, NpcCitadelInfo?, WorldCitadel?) -> Unit,
    modifier: Modifier = Modifier
) {
    val str = LocalAppStrings.current
    val maxMarches = GameCatalog.maxMarchSlots(state)
    val selectedNode = state.worldNodes.firstOrNull { it.id == selectedNodeId }
    var selectedNpcCitadel by remember { mutableStateOf<NpcCitadelInfo?>(null) }
    var selectedCloudCitadel by remember { mutableStateOf<WorldCitadel?>(null) }
    var scoutReportText by remember { mutableStateOf<String?>(null) }

    val otherCitadels = remember(worldCitadels, currentUserId) {
        worldCitadels.filter { it.userId != currentUserId }.take(12)
    }

    val infinite = rememberInfiniteTransition(label = "worldAnim")
    val roamPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "roamPhase"
    )
    val dashOffset by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "dashOffset"
    )
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "wavePhase"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateAbyss)
    ) {
        // Header strip with Alliance Hub CTA & March Counter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateSurface)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = NileTurquoise)
                Text(
                    text = str.worldMapTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (state.allianceTag.isNotBlank()) WarmGold.copy(alpha = 0.16f) else SlateCard)
                        .border(1.2.dp, WarmGold.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                        .clickable { onOpenAllianceModal() }
                        .testTag("world_alliance_button")
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Groups, contentDescription = str.allianceTitle, tint = WarmGold, modifier = Modifier.size(15.dp))
                    Text(
                        text = if (state.allianceTag.isNotBlank()) "[${state.allianceTag}]" else str.allianceTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlateCard)
                        .border(1.dp, IceCyan.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${str.activeMarches}: ${state.marches.size}/$maxMarches",
                        style = MaterialTheme.typography.labelSmall,
                        color = IceGlow
                    )
                }
            }
        }

        // Authentic 3D Living Nile Tundra Strategy Map
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()

            AsyncDrawableImage(
                resId = R.drawable.img_3d_nile_tundra,
                contentDescription = str.worldMapTitle,
                contentScale = ContentScale.Crop,
                maxDimPx = 720,
                placeholderColor = SlateAbyss,
                modifier = Modifier.fillMaxSize().alpha(0.68f)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                SlateAbyss.copy(alpha = 0.25f),
                                Color.Transparent,
                                SlateAbyss.copy(alpha = 0.44f)
                            )
                        )
                    )
            )

            // Floating Living Nile & Alliance Realm Showcase Pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateAbyss.copy(alpha = 0.80f))
                    .border(1.dp, NileTurquoise.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${str.nileShowcaseBadge} • ${otherCitadels.size + GameCatalog.npcCitadels.size} 🏰 • ${alliances.size} 🛡",
                    style = MaterialTheme.typography.labelSmall,
                    color = IceGlow
                )
            }

            // Living Nile River, Animated Ice Floes, Swaying Palms, Giza Pyramids & Dotted March Trails
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = size.width
                val ch = size.height
                val cityCenter = Offset(cw * 0.50f, ch * 0.52f)

                // Subtle Snowy Isometric Diamond Grid
                val step = cw / 7f
                for (i in -6..12) {
                    drawLine(
                        color = WarmGold.copy(alpha = 0.10f),
                        start = Offset(i * step, 0f),
                        end = Offset(i * step + ch * 0.55f, ch),
                        strokeWidth = 1.2f
                    )
                    drawLine(
                        color = IceGlow.copy(alpha = 0.10f),
                        start = Offset(i * step, 0f),
                        end = Offset(i * step - ch * 0.55f, ch),
                        strokeWidth = 1.2f
                    )
                }

                // Living Winding Frozen Nile River with Animated Flowing Waves & Drifting Ice Floes
                val nilePath = Path().apply {
                    moveTo(cw * 0.14f, 0f)
                    cubicTo(cw * 0.08f, ch * 0.30f, cw * 0.34f, ch * 0.44f, cw * 0.25f, ch * 0.65f)
                    cubicTo(cw * 0.18f, ch * 0.82f, cw * 0.42f, ch * 0.90f, cw * 0.38f, ch)
                }
                drawPath(
                    path = nilePath,
                    color = NileTurquoise.copy(alpha = 0.28f),
                    style = Stroke(width = cw * 0.090f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = nilePath,
                    color = IceGlow.copy(alpha = 0.42f),
                    style = Stroke(
                        width = cw * 0.028f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 26f), -wavePhase * 120f)
                    )
                )

                // Drifting Ice Floes floating southward along the Nile
                for (i in 0..3) {
                    val t = (wavePhase + i * 0.25f) % 1f
                    val fy = t * ch
                    val fx = cw * (0.14f + sin(t * PI * 2.1).toFloat() * 0.11f + t * 0.22f)
                    val floe = Path().apply {
                        moveTo(fx, fy - 7f)
                        lineTo(fx + 9f, fy + 4f)
                        lineTo(fx - 8f, fy + 5f)
                        close()
                    }
                    drawPath(floe, color = FrostWhite.copy(alpha = 0.65f))
                }

                // Snow-Dusted Giza Pyramids on the Horizon
                drawFrozenPyramid(Offset(cw * 0.80f, ch * 0.46f), cw * 0.10f)
                drawFrozenPyramid(Offset(cw * 0.90f, ch * 0.52f), cw * 0.075f)
                drawFrozenPyramid(Offset(cw * 0.12f, ch * 0.46f), cw * 0.08f)

                // Swaying Frozen Oasis Palms across the Nile banks
                val palmSpots = listOf(
                    0.09f to 0.14f, 0.15f to 0.34f, 0.36f to 0.14f, 0.64f to 0.15f,
                    0.88f to 0.16f, 0.90f to 0.36f, 0.10f to 0.85f, 0.33f to 0.88f,
                    0.67f to 0.88f, 0.89f to 0.84f
                )
                palmSpots.forEachIndexed { idx, (tx, ty) ->
                    val sway = sin(roamPhase + idx * 0.8f) * 3.5f
                    drawSnowPalmTree(Offset(cw * tx, ch * ty), cw * 0.036f, sway)
                }

                // Animated Dotted Glowing March Troop Lines toward Resource Nodes, NPC Bastions & Player Citadels
                state.marches.forEach { march ->
                    val targetNode = state.worldNodes.firstOrNull { it.id == march.nodeId }
                    val targetNpc = GameCatalog.npcCitadels.firstOrNull { it.id == march.nodeId }
                    val targetCloud = otherCitadels.firstOrNull { it.userId == march.nodeId }
                    val targetPt = when {
                        targetNode != null -> Offset(cw * targetNode.posX, ch * targetNode.posY)
                        targetNpc != null -> Offset(cw * targetNpc.posX, ch * targetNpc.posY)
                        targetCloud != null -> Offset(cw * targetCloud.mapX.toFloat(), ch * targetCloud.mapY.toFloat())
                        else -> null
                    }
                    if (targetPt != null) {
                        val lineColor = if (march.returning) EmeraldSafe else WarmGold
                        drawLine(
                            color = lineColor.copy(alpha = 0.88f),
                            start = cityCenter,
                            end = targetPt,
                            strokeWidth = 4.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), -dashOffset)
                        )
                        val progress = if (march.totalSec > 0) 1f - (march.remainingSec.toFloat() / march.totalSec) else 0.5f
                        val squadPos = if (march.returning) {
                            Offset(
                                targetPt.x + (cityCenter.x - targetPt.x) * progress,
                                targetPt.y + (cityCenter.y - targetPt.y) * progress
                            )
                        } else {
                            Offset(
                                cityCenter.x + (targetPt.x - cityCenter.x) * progress,
                                cityCenter.y + (targetPt.y - cityCenter.y) * progress
                            )
                        }
                        drawCircle(color = lineColor.copy(alpha = 0.28f), radius = 19f, center = squadPos)
                        drawCircle(color = SlateAbyss, radius = 12f, center = squadPos)
                        drawCircle(color = lineColor, radius = 8f, center = squadPos)
                    }
                }
            }

            // Home City Citadel Marker with Alliance Banner & Furnace Level Crown
            Column(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (w * 0.50f).roundToInt() - 48.dp.toPx().roundToInt(),
                            (h * 0.52f).roundToInt() - 44.dp.toPx().roundToInt()
                        )
                    }
                    .width(96.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0E1B2B).copy(alpha = 0.92f))
                        .border(2.2.dp, WarmGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    FurnaceVectorIllustration(
                        level = state.furnace.level,
                        overdrive = state.furnace.overdrive,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateAbyss.copy(alpha = 0.90f))
                        .border(1.dp, WarmGold.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    val tagPrefix = if (state.allianceTag.isNotBlank()) "[${state.allianceTag}] " else ""
                    Text(
                        text = "$tagPrefix${str.levelShort}.${state.furnace.level}",
                        fontSize = 10.sp,
                        color = WarmGold,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Rival NPC Alliance Strongholds on Nile Banks
            GameCatalog.npcCitadels.forEach { npc ->
                val bannerColor = Color(npc.bannerColorHex)
                CitadelMarkerCard(
                    tag = npc.allianceTag,
                    title = if (str.isRtl) npc.nameAr else npc.nameEn,
                    furnaceLevel = npc.furnaceLevel,
                    power = npc.power,
                    accent = bannerColor,
                    avatarHeroId = null,
                    frameStyle = "ROYAL_GOLD",
                    normX = npc.posX,
                    normY = npc.posY,
                    mapWidthPx = w,
                    mapHeightPx = h,
                    testTag = "npc_citadel_${npc.id}",
                    onClick = {
                        scoutReportText = null
                        selectedNpcCitadel = npc
                    }
                )
            }

            // Live Cloud Player Citadels synced via Firestore
            otherCitadels.forEach { citadel ->
                val coordX = citadel.mapX.toFloat().coerceIn(0.10f, 0.90f)
                val coordY = citadel.mapY.toFloat().coerceIn(0.14f, 0.86f)
                CitadelMarkerCard(
                    tag = citadel.allianceTag.ifBlank { "KMT" },
                    title = citadel.chiefName.ifBlank { "Pharaoh" },
                    furnaceLevel = citadel.furnaceLevel.toInt(),
                    power = citadel.totalPower.toInt(),
                    accent = NileTurquoise,
                    avatarHeroId = citadel.avatarHeroId,
                    frameStyle = citadel.frameStyle,
                    normX = coordX,
                    normY = coordY,
                    mapWidthPx = w,
                    mapHeightPx = h,
                    testTag = "cloud_citadel_${citadel.userId}",
                    onClick = {
                        scoutReportText = null
                        selectedCloudCitadel = citadel
                    }
                )
            }

            // Roaming Animated Beasts & Glowing Rarity-Bordered Resource Mines
            state.worldNodes.forEachIndexed { idx, node ->
                val isBeast = node.type == WorldNodeType.POLAR_BEAR ||
                    node.type == WorldNodeType.FROST_BEAST ||
                    node.type == WorldNodeType.YETI_BOSS

                val bobX = if (isBeast) (sin(roamPhase + idx * 1.3f) * 6f).roundToInt() else 0
                val bobY = if (isBeast) (sin(roamPhase * 1.4f + idx) * 4f).roundToInt() else 0

                val rarityBorderColor = when (node.type) {
                    WorldNodeType.YETI_BOSS -> MythicPurple
                    WorldNodeType.FROST_BEAST -> CrimsonAlert
                    WorldNodeType.POLAR_BEAR -> FurnaceOrange
                    WorldNodeType.IRON_DEPOSIT -> EpicViolet
                    WorldNodeType.COAL_VEIN -> RareBlue
                    WorldNodeType.ANCIENT_TIMBER -> EmeraldSafe
                }

                Column(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (w * node.posX).roundToInt() - 46.dp.toPx().roundToInt() + bobX,
                                (h * node.posY).roundToInt() - 34.dp.toPx().roundToInt() + bobY
                            )
                        }
                        .width(92.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(SlateCard.copy(alpha = 0.94f), SlateAbyss.copy(alpha = 0.96f))
                            )
                        )
                        .border(
                            width = if (selectedNodeId == node.id) 2.5.dp else 1.5.dp,
                            color = if (selectedNodeId == node.id) FrostWhite else rarityBorderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectNode(node.id) }
                        .testTag("world_node_${node.id}")
                        .padding(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        when (node.type) {
                            WorldNodeType.COAL_VEIN -> ResourceVectorIcon(ResIconType.COAL, size = 16.dp)
                            WorldNodeType.ANCIENT_TIMBER -> ResourceVectorIcon(ResIconType.WOOD, size = 16.dp)
                            WorldNodeType.IRON_DEPOSIT -> ResourceVectorIcon(ResIconType.IRON, size = 16.dp)
                            else -> WorldBeastVectorBadge(node.type, modifier = Modifier.size(18.dp))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(rarityBorderColor.copy(alpha = 0.25f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("${str.levelShort}.${node.level}", style = MaterialTheme.typography.labelSmall, color = WarmGold, fontSize = 9.sp)
                        }
                    }

                    Text(
                        text = str.nodeName(node.type),
                        fontSize = 8.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = FrostWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isBeast) {
                        Text(text = "⚔ ${node.powerReq}", fontSize = 8.5.sp, color = MistText)
                    }
                }
            }
        }

        // Active Marches Strip with Live Timers
        Surface(color = SlateSurface, tonalElevation = 4.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.marches.isEmpty()) {
                    Text(str.noMarches, style = MaterialTheme.typography.bodyMedium, color = MistText)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.marches, key = { it.id }) { m ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, if (m.returning) EmeraldSafe else WarmGold)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        if (m.returning) Icons.AutoMirrored.Filled.KeyboardReturn else Icons.AutoMirrored.Filled.DirectionsRun,
                                        contentDescription = null,
                                        tint = if (m.returning) EmeraldSafe else WarmGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "${str.nodeName(m.nodeType)} (${str.levelShort}.${m.nodeLevel})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = FrostWhite
                                        )
                                        Text(
                                            text = "${if (m.returning) str.returning else str.marching}: ${m.remainingSec}s",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (m.returning) EmeraldSafe else WarmGold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedNode != null) {
        WorldNodeDialog(
            node = selectedNode,
            state = state,
            onDismiss = { onSelectNode(null) },
            onDispatch = { onDispatchMarch(selectedNode.id) }
        )
    }

    val activeNpc = selectedNpcCitadel
    val activeCloud = selectedCloudCitadel
    if (activeNpc != null || activeCloud != null) {
        val playerPower = GameCatalog.computeTotalPower(state)
        val cTitle = activeNpc?.let { if (str.isRtl) it.nameAr else it.nameEn } ?: activeCloud!!.chiefName
        val cTag = activeNpc?.allianceTag ?: activeCloud!!.allianceTag
        val cFurnace = activeNpc?.furnaceLevel ?: activeCloud!!.furnaceLevel.toInt()
        val cPower = activeNpc?.power ?: activeCloud!!.totalPower.toInt()
        val cAvatar = activeCloud?.avatarHeroId
        val cFrame = activeCloud?.frameStyle ?: "ROYAL_GOLD"

        AlertDialog(
            onDismissRequest = {
                selectedNpcCitadel = null
                selectedCloudCitadel = null
            },
            containerColor = SlateCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (cAvatar != null) {
                        FramedHeroAvatar(heroId = cAvatar, frameStyle = cFrame, size = 44.dp)
                    } else {
                        Icon(Icons.Default.Castle, contentDescription = null, tint = WarmGold, modifier = Modifier.size(34.dp))
                    }
                    Column {
                        if (cTag.isNotBlank()) {
                            Text("[$cTag]", fontSize = 11.sp, color = NileTurquoise, fontWeight = FontWeight.Bold)
                        }
                        Text(cTitle, color = WarmGold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${str.furnace}: ${str.levelShort}.$cFurnace  •  ⚔ ${str.power}: $cPower", color = IceGlow)
                    Text("${str.power}: $playerPower vs $cPower", color = FrostWhite, style = MaterialTheme.typography.bodyMedium)
                    if (activeNpc != null) {
                        Text(
                            text = "🎁 +${activeNpc.rewardCoal} ${str.coal} • +${activeNpc.rewardWood} ${str.wood} • +${activeNpc.rewardXp} XP",
                            color = WarmGold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (scoutReportText != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateSurface)
                                .border(1.dp, NileTurquoise.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Text(scoutReportText!!, color = EmeraldSafe, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val ratio = if (cPower > 0) (playerPower * 100 / cPower) else 100
                            scoutReportText = if (str.isRtl) {
                                "تقرير الكشافة: نسبة التفوق $ratio% — الحامية تعتمد على مسلّة مستوى $cFurnace."
                            } else {
                                "Scout Report: Power ratio $ratio% — Garrison anchored by Obelisk Lv.$cFurnace."
                            }
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp).testTag("citadel_scout_button")
                    ) {
                        Text(str.citadelScout, color = NileTurquoise)
                    }
                    Button(
                        onClick = {
                            val targetId = activeNpc?.id ?: activeCloud!!.userId
                            onDispatchCitadelExpedition(targetId, activeNpc, activeCloud)
                            selectedNpcCitadel = null
                            selectedCloudCitadel = null
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp).testTag("citadel_raid_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
                    ) {
                        Text(str.citadelRaid, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedNpcCitadel = null
                    selectedCloudCitadel = null
                }) {
                    Text(str.close, color = MistText)
                }
            }
        )
    }
}

@Composable
private fun CitadelMarkerCard(
    tag: String,
    title: String,
    furnaceLevel: Int,
    power: Int,
    accent: Color,
    avatarHeroId: String?,
    frameStyle: String,
    normX: Float,
    normY: Float,
    mapWidthPx: Float,
    mapHeightPx: Float,
    testTag: String,
    onClick: () -> Unit
) {
    val str = LocalAppStrings.current
    Column(
        modifier = Modifier
            .offset {
                IntOffset(
                    (mapWidthPx * normX).roundToInt() - 48.dp.toPx().roundToInt(),
                    (mapHeightPx * normY).roundToInt() - 38.dp.toPx().roundToInt()
                )
            }
            .width(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SlateAbyss.copy(alpha = 0.90f))
            .border(1.5.dp, accent.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (avatarHeroId != null) {
                FramedHeroAvatar(heroId = avatarHeroId, frameStyle = frameStyle, size = 24.dp)
            } else {
                Icon(Icons.Default.Castle, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            if (tag.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(accent.copy(alpha = 0.22f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("[$tag]", fontSize = 8.5.sp, color = accent, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            text = title,
            fontSize = 9.sp,
            color = FrostWhite,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${str.levelShort}.$furnaceLevel • ⚔ ${formatCompact(power)}",
            fontSize = 8.sp,
            color = IceGlow,
            maxLines = 1
        )
    }
}

private fun DrawScope.drawFrozenPyramid(baseCenter: Offset, s: Float) {
    val leftFace = Path().apply {
        moveTo(baseCenter.x, baseCenter.y - s * 1.15f)
        lineTo(baseCenter.x - s * 1.1f, baseCenter.y)
        lineTo(baseCenter.x, baseCenter.y + s * 0.25f)
        close()
    }
    val rightFace = Path().apply {
        moveTo(baseCenter.x, baseCenter.y - s * 1.15f)
        lineTo(baseCenter.x + s * 1.1f, baseCenter.y)
        lineTo(baseCenter.x, baseCenter.y + s * 0.25f)
        close()
    }
    drawPath(leftFace, Color(0xFF92400E).copy(alpha = 0.72f))
    drawPath(rightFace, Color(0xFF451A03).copy(alpha = 0.78f))
    val snowCap = Path().apply {
        moveTo(baseCenter.x, baseCenter.y - s * 1.15f)
        lineTo(baseCenter.x - s * 0.38f, baseCenter.y - s * 0.72f)
        lineTo(baseCenter.x, baseCenter.y - s * 0.62f)
        lineTo(baseCenter.x + s * 0.38f, baseCenter.y - s * 0.72f)
        close()
    }
    drawPath(snowCap, FrostWhite.copy(alpha = 0.85f))
}

private fun DrawScope.drawSnowPalmTree(base: Offset, s: Float, swayX: Float = 0f) {
    val crown = Offset(base.x + s * 0.12f + swayX, base.y - s * 1.15f)
    drawLine(Color(0xFF78350F), base, crown, strokeWidth = s * 0.22f)
    val frondOffsets = listOf(
        Offset(-s * 0.75f + swayX * 0.4f, -s * 0.15f),
        Offset(s * 0.75f + swayX * 0.4f, -s * 0.15f),
        Offset(-s * 0.55f + swayX * 0.3f, -s * 0.55f),
        Offset(s * 0.55f + swayX * 0.3f, -s * 0.55f)
    )
    frondOffsets.forEach { d ->
        drawLine(Color(0xFF0F766E), crown, crown + d, strokeWidth = s * 0.20f, cap = StrokeCap.Round)
        drawLine(FrostWhite.copy(alpha = 0.78f), crown, crown + d * 0.85f, strokeWidth = s * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
private fun WorldBeastVectorBadge(type: WorldNodeType, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val fur = when (type) {
            WorldNodeType.YETI_BOSS -> Color(0xFFDDD6FE)
            WorldNodeType.FROST_BEAST -> Color(0xFF94A3B8)
            else -> FrostWhite
        }
        val eye = if (type == WorldNodeType.YETI_BOSS) MythicPurple else CrimsonAlert
        drawCircle(fur, radius = w * 0.38f, center = Offset(w * 0.50f, h * 0.55f))
        drawCircle(fur, radius = w * 0.14f, center = Offset(w * 0.24f, h * 0.26f))
        drawCircle(fur, radius = w * 0.14f, center = Offset(w * 0.76f, h * 0.26f))
        drawCircle(eye, radius = w * 0.07f, center = Offset(w * 0.38f, h * 0.50f))
        drawCircle(eye, radius = w * 0.07f, center = Offset(w * 0.62f, h * 0.50f))
    }
}

@Composable
private fun WorldNodeDialog(
    node: WorldNode,
    state: GameState,
    onDismiss: () -> Unit,
    onDispatch: () -> Unit
) {
    val str = LocalAppStrings.current
    val playerPower = GameCatalog.computeTotalPower(state)
    val rewardText = when (node.type) {
        WorldNodeType.POLAR_BEAR -> "+${140 * node.level} ${str.meat}, +${80 * node.level} XP, 🔑"
        WorldNodeType.FROST_BEAST -> "+${180 * node.level} ${str.meat}, +${120 * node.level} ${str.coal}, 🔑"
        WorldNodeType.YETI_BOSS -> "+360 ${str.meat}, +240 ${str.iron}, +420 XP, 2x 🔑"
        WorldNodeType.COAL_VEIN -> "+${220 * node.level} ${str.coal}"
        WorldNodeType.ANCIENT_TIMBER -> "+${240 * node.level} ${str.wood}"
        WorldNodeType.IRON_DEPOSIT -> "+${180 * node.level} ${str.iron}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Text("${str.nodeName(node.type)} (${str.levelShort}.${node.level})", color = FrostWhite)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${str.power}: $playerPower / ${node.powerReq}", color = IceGlow, style = MaterialTheme.typography.bodyMedium)
                Text("🎁 $rewardText", color = WarmGold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "⚔ ${str.infantry}: ${(state.troops.infantry / 2).coerceAtLeast(10)} • ${str.lancers}: ${(state.troops.lancers / 2).coerceAtLeast(5)} • ${str.marksmen}: ${(state.troops.marksmen / 2).coerceAtLeast(5)}",
                    color = MistText,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDispatch,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("dispatch_march_button"),
                colors = ButtonDefaults.buttonColors(containerColor = IceCyan, contentColor = SlateAbyss)
            ) {
                Text(str.dispatchMarch, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(str.close, color = MistText)
            }
        }
    )
}
