package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.GameCatalog
import com.example.localization.LocalAppStrings
import com.example.model.*
import com.example.ui.art.AsyncDrawableImage
import com.example.ui.art.CityBuildingVectorIllustration
import com.example.ui.art.FurnaceVectorIllustration
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun FurnaceModal(
    state: GameState,
    onDismiss: () -> Unit,
    onToggleOverdrive: () -> Unit,
    onUpgrade: () -> Unit,
    onSpeedup: () -> Unit
) {
    val str = LocalAppStrings.current
    val cost = GameCatalog.furnaceUpgradeCost(state.furnace.level)
    val campTemp = GameCatalog.computeCampTempC(state)
    val ambientTemp = GameCatalog.computeAmbientTempC(state)
    val coalBurn = GameCatalog.computeCoalBurnPerMinute(state).roundToInt()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SlateCard,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, FurnaceOrange.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Whatshot, contentDescription = null, tint = FurnaceOrange)
                        Text("${str.furnace} (${str.levelShort}.${state.furnace.level}/10)", style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = str.close, tint = MistText)
                    }
                }

                // 3D Solar Obelisk Visual Showcase
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(106.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateAbyss)
                        .border(1.2.dp, WarmGold.copy(alpha = 0.65f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncDrawableImage(
                        resId = R.drawable.img_3d_pharaoh_citadel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        maxDimPx = 512,
                        placeholderColor = SlateAbyss,
                        modifier = Modifier.fillMaxSize().alpha(0.32f)
                    )
                    FurnaceVectorIllustration(
                        level = state.furnace.level,
                        overdrive = state.furnace.overdrive,
                        modifier = Modifier.size(90.dp)
                    )
                }

                // Telemetry grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatBadge(str.campTemp, "${campTemp}°C", if (campTemp < 0) IceCyan else FurnaceAmber)
                    StatBadge(str.ambientTemp, "${ambientTemp}°C", MistText)
                    StatBadge(str.coalBurn, "-$coalBurn/m", CrimsonAlert)
                }

                // Overdrive toggle button
                Button(
                    onClick = onToggleOverdrive,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("modal_overdrive_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.furnace.overdrive) FurnaceOrange else SlateElevated,
                        contentColor = if (state.furnace.overdrive) SlateAbyss else FrostWhite
                    )
                ) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.furnace.overdrive) "${str.overdrive}: ON (2.25x 🔥)" else "${str.normalPower} → ${str.overdrive}",
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = SlateBorder)

                if (state.furnace.upgradeRemainingSec > 0) {
                    Text("${str.upgrade}: ${state.furnace.upgradeRemainingSec}s", color = WarmGold)
                    LinearProgressIndicator(
                        progress = {
                            if (state.furnace.upgradeTotalSec > 0) 1f - state.furnace.upgradeRemainingSec.toFloat() / state.furnace.upgradeTotalSec
                            else 0.5f
                        },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = FurnaceAmber,
                        trackColor = SlateAbyss
                    )
                    Button(
                        onClick = onSpeedup,
                        enabled = state.resources.speedupsMin > 0,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
                    ) {
                        Text("⚡ ${str.speedupBtn} (${state.resources.speedupsMin})", fontWeight = FontWeight.Bold)
                    }
                } else if (state.furnace.level < 10) {
                    Text(
                        text = "🪵 ${cost.wood}   🔥 ${cost.coal}   ⚙ ${cost.iron}   •   ⏱ ${cost.durationSec}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = IceGlow
                    )
                    Button(
                        onClick = onUpgrade,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("upgrade_furnace_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = IceCyan, contentColor = SlateAbyss)
                    ) {
                        Text("${str.upgrade} → ${str.levelShort}.${state.furnace.level + 1}", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("MAX LEVEL 10", color = EmeraldSafe, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BuildingDetailModal(
    slot: BuildingSlot,
    state: GameState,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    onSpeedup: () -> Unit,
    onAdjustWorkers: (Int) -> Unit,
    onUpgradeGear: (Boolean) -> Unit,
    onSetMealQuality: (Int) -> Unit,
    onTrainTroops: (TroopType, Int) -> Unit,
    onSpeedupTraining: () -> Unit
) {
    val str = LocalAppStrings.current
    val cost = GameCatalog.buildingUpgradeCost(slot.type, slot.level)
    val maxW = GameCatalog.maxWorkersFor(slot)
    val heaterCost = GameCatalog.gearUpgradeCost(slot.heaterLevel)
    val toolCost = GameCatalog.gearUpgradeCost(slot.toolLevel)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SlateCard,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, IceCyan.copy(alpha = 0.55f))
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp).heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(buildingIcon(slot.type), contentDescription = null, tint = buildingAccent(slot.type))
                            Text(
                                text = "${str.buildingName(slot.type)} (${if (slot.level == 0) str.construct else "${str.levelShort}.${slot.level}"})",
                                style = MaterialTheme.typography.titleMedium,
                                color = FrostWhite
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = str.close, tint = MistText)
                        }
                    }
                    Text(str.buildingDesc(slot.type), style = MaterialTheme.typography.bodyMedium, color = MistText)
                    Spacer(Modifier.height(8.dp))
                    // Clear 3D Isometric Building Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(listOf(SlateAbyss, SlateElevated))
                            )
                            .border(1.2.dp, buildingAccent(slot.type).copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    ) {
                        AsyncDrawableImage(
                            resId = R.drawable.img_3d_pharaoh_citadel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            maxDimPx = 512,
                            placeholderColor = SlateAbyss,
                            modifier = Modifier.fillMaxSize().alpha(0.30f)
                        )
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CityBuildingVectorIllustration(
                                type = slot.type,
                                built = slot.level > 0,
                                smokePhase = 0.3f,
                                modifier = Modifier.size(84.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = str.buildingShowcaseLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarmGold
                                )
                                Text(
                                    text = if (slot.level > 0) "${str.levelShort}.${slot.level}/10" else str.construct,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IceGlow
                                )
                            }
                        }
                    }
                }

                // Upgrade / Build section
                item {
                    if (slot.upgradeRemainingSec > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${str.upgrade}: ${slot.upgradeRemainingSec}s", color = WarmGold)
                            LinearProgressIndicator(
                                progress = {
                                    if (slot.upgradeTotalSec > 0) 1f - slot.upgradeRemainingSec.toFloat() / slot.upgradeTotalSec
                                    else 0.5f
                                },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = FurnaceAmber,
                                trackColor = SlateAbyss
                            )
                            Button(
                                onClick = onSpeedup,
                                enabled = state.resources.speedupsMin > 0,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
                            ) {
                                Text("⚡ ${str.speedupBtn}", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (slot.level < 10) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "🪵 ${cost.wood}   🔥 ${cost.coal}   ⚙ ${cost.iron}   •   ⏱ ${cost.durationSec}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = IceGlow
                            )
                            Button(
                                onClick = onUpgrade,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("upgrade_building_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = IceCyan, contentColor = SlateAbyss)
                            ) {
                                Text(
                                    text = if (slot.level == 0) str.construct else "${str.upgrade} → ${str.levelShort}.${slot.level + 1}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Workers management
                if (maxW > 0 && slot.level > 0) {
                    item {
                        HorizontalDivider(color = SlateBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${str.workers}: ${slot.assignedWorkers} / $maxW", color = FrostWhite, style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onAdjustWorkers(-1) }, enabled = slot.assignedWorkers > 0) { Text("-") }
                                OutlinedButton(onClick = { onAdjustWorkers(1) }, enabled = slot.assignedWorkers < maxW) { Text("+") }
                            }
                        }
                    }
                }

                // Building Equipment (Heater + Tools)
                if (slot.level > 0) {
                    item {
                        HorizontalDivider(color = SlateBorder)
                        Text(str.equipment, style = MaterialTheme.typography.titleSmall, color = WarmGold)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onUpgradeGear(true) },
                                enabled = slot.heaterLevel < 5,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🔥 ${str.heater} ${slot.heaterLevel}/5\n(${heaterCost.wood}🪵)", fontSize = 11.sp, lineHeight = 14.sp)
                            }
                            OutlinedButton(
                                onClick = { onUpgradeGear(false) },
                                enabled = slot.toolLevel < 5,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("⚒ ${str.tools} ${slot.toolLevel}/5\n(${toolCost.iron}⚙)", fontSize = 11.sp, lineHeight = 14.sp)
                            }
                        }
                    }
                }

                // Cookhouse Meal Quality
                if (slot.type == BuildingType.COOKHOUSE && slot.level > 0) {
                    item {
                        HorizontalDivider(color = SlateBorder)
                        Text(str.mealQuality, style = MaterialTheme.typography.titleSmall, color = WarmGold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3).forEach { q ->
                                FilterChip(
                                    selected = slot.mealQuality == q,
                                    onClick = { onSetMealQuality(q) },
                                    label = { Text("Tier $q") }
                                )
                            }
                        }
                    }
                }

                // Barracks Troop Training
                if (slot.type == BuildingType.BARRACKS && slot.level > 0) {
                    item {
                        HorizontalDivider(color = SlateBorder)
                        Text(str.trainTroops, style = MaterialTheme.typography.titleSmall, color = MythicPurple)
                        Text(
                            text = "🛡 ${str.infantry}: ${state.troops.infantry}  •  🔱 ${str.lancers}: ${state.troops.lancers}  •  🏹 ${str.marksmen}: ${state.troops.marksmen}",
                            style = MaterialTheme.typography.labelSmall,
                            color = IceGlow
                        )
                        Spacer(Modifier.height(6.dp))
                        val tq = state.trainingQueue
                        if (tq != null) {
                            Text("${tq.troopType.name} x${tq.count}: ${tq.remainingSec}s", color = WarmGold)
                            Button(
                                onClick = onSpeedupTraining,
                                enabled = state.resources.speedupsMin > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
                            ) {
                                Text("⚡ ${str.speedupBtn}")
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { onTrainTroops(TroopType.INFANTRY, 10) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                                    Text("+10 ${str.infantry}", fontSize = 10.sp)
                                }
                                Button(onClick = { onTrainTroops(TroopType.LANCER, 10) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                                    Text("+10 ${str.lancers}", fontSize = 10.sp)
                                }
                                Button(onClick = { onTrainTroops(TroopType.MARKSMAN, 10) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                                    Text("+10 ${str.marksmen}", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SurvivorsModal(
    state: GameState,
    onDismiss: () -> Unit,
    onRecruit: () -> Unit,
    onTreatAll: () -> Unit
) {
    val str = LocalAppStrings.current
    val cap = GameCatalog.maxSurvivorCapacity(state)
    val alive = state.survivors.count { it.healthState != SurvivorHealthState.DEAD }
    val sick = state.survivors.count { it.healthState == SurvivorHealthState.SICK }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SlateCard,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldSafe.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp).heightIn(max = 540.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${str.survivorsTitle} ($alive/$cap)", style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = str.close, tint = MistText) }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRecruit,
                        enabled = alive < cap,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IceCyan, contentColor = SlateAbyss)
                    ) {
                        Text("+1 (60 🥩)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (sick > 0) {
                        Button(
                            onClick = onTreatAll,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert, contentColor = SlateAbyss)
                        ) {
                            Text("${str.healAll} ($sick)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.survivors, key = { it.id }) { s ->
                        val statusColor = when (s.healthState) {
                            SurvivorHealthState.HEALTHY -> EmeraldSafe
                            SurvivorHealthState.SICK -> CrimsonAlert
                            SurvivorHealthState.DEAD -> MistText
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = SlateSurface)) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (str.isRtl) s.nameAr else s.nameEn, color = FrostWhite, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (s.healthState == SurvivorHealthState.SICK) str.sick
                                        else if (s.healthState == SurvivorHealthState.DEAD) str.dead
                                        else str.activityLabel(s.activity),
                                        color = statusColor,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text(
                                    text = "❤ ${s.health.roundToInt()}%  •  🔥 ${s.warmth.roundToInt()}%  •  😊 ${s.happiness.roundToInt()}%  •  🍽 ${100 - s.hunger.roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IceGlow
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChiefOrdersModal(
    state: GameState,
    onDismiss: () -> Unit,
    onEnact: (ChiefOrderType) -> Unit
) {
    val str = LocalAppStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SlateCard,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, WarmGold.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(str.chiefOrders, style = MaterialTheme.typography.titleMedium, color = WarmGold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = str.close, tint = MistText) }
                }

                state.chiefOrders.forEach { order ->
                    Card(colors = CardDefaults.cardColors(containerColor = SlateSurface)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(str.orderName(order.type), style = MaterialTheme.typography.titleSmall, color = FrostWhite)
                            Text(str.orderDesc(order.type), style = MaterialTheme.typography.bodyMedium, color = MistText)
                            Button(
                                onClick = { onEnact(order.type) },
                                enabled = order.activeRemainingSec == 0 && order.cooldownRemainingSec == 0,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
                            ) {
                                Text(
                                    text = when {
                                        order.activeRemainingSec > 0 -> "${str.active} (${order.activeRemainingSec}s)"
                                        order.cooldownRemainingSec > 0 -> "${str.cooldown} (${order.cooldownRemainingSec}s)"
                                        else -> str.enact
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsBackupModal(
    state: GameState,
    chiefName: String,
    syncStatus: CloudSyncStatus,
    onDismiss: () -> Unit,
    onSyncCloudNow: () -> Unit,
    onSignOut: () -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Boolean,
    onResetGame: () -> Unit
) {
    val str = LocalAppStrings.current
    val context = LocalContext.current
    var jsonText by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SlateCard,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, IceCyan.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(str.settingsBackup, style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = str.close, tint = MistText) }
                }

                // Cloud Sync Profile Strip
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarmGold.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(chiefName, style = MaterialTheme.typography.titleSmall, color = WarmGold)
                            Text(
                                text = when (syncStatus) {
                                    CloudSyncStatus.SYNCED -> str.cloudSynced
                                    CloudSyncStatus.SYNCING -> str.cloudSyncing
                                    CloudSyncStatus.ERROR -> str.cloudSyncError
                                    CloudSyncStatus.IDLE -> "Firebase"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (syncStatus) {
                                    CloudSyncStatus.SYNCED -> EmeraldSafe
                                    CloudSyncStatus.SYNCING -> WarmGold
                                    CloudSyncStatus.ERROR -> CrimsonAlert
                                    CloudSyncStatus.IDLE -> MistText
                                }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onSyncCloudNow,
                                enabled = syncStatus != CloudSyncStatus.SYNCING,
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp).testTag("sync_cloud_now_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe, contentColor = SlateAbyss)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(str.syncCloudNow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onSignOut()
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 44.dp).testTag("sign_out_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert)
                            ) {
                                Text(str.signOutLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val exported = onExportJson()
                        jsonText = exported
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("PharaohFrostholdSave", exported))
                        statusMsg = str.backupCopied
                    },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("export_json_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = IceCyan, contentColor = SlateAbyss)
                ) {
                    Text(str.exportJson, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    label = { Text(str.importJson) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val ok = onImportJson(jsonText)
                            statusMsg = if (ok) str.backupLoaded else str.backupInvalid
                        },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe, contentColor = SlateAbyss)
                    ) {
                        Text(str.pasteAndLoad, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            onResetGame()
                            onDismiss()
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert)
                    ) {
                        Text(str.resetGame)
                    }
                }

                statusMsg?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = WarmGold)
                }
            }
        }
    }
}

@Composable
fun OfflineSummaryDialog(summary: OfflineGainSummary, onCollect: () -> Unit) {
    val str = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onCollect,
        containerColor = SlateCard,
        title = { Text(str.offlineWelcome, color = WarmGold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(str.offlineDesc, color = MistText, style = MaterialTheme.typography.bodyMedium)
                Text("⏱ ${summary.elapsedSeconds / 60} min", color = IceGlow, style = MaterialTheme.typography.labelMedium)
                Text("🥩 +${summary.meatGained}  🪵 +${summary.woodGained}", color = FrostWhite)
                Text("🔥 +${summary.coalGained}  ⚙ +${summary.ironGained}  ⭐ +${summary.xpGained} XP", color = EmeraldSafe)
            }
        },
        confirmButton = {
            Button(
                onClick = onCollect,
                colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
            ) {
                Text(str.collect, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun StatBadge(label: String, value: String, tint: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SlateSurface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, color = MistText)
        Text(value, style = MaterialTheme.typography.titleSmall, color = tint)
    }
}
