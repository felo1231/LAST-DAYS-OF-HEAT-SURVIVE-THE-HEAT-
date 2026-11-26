package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GameCatalog
import com.example.localization.LocalAppStrings
import com.example.model.*
import com.example.ui.art.ResIconType
import com.example.ui.art.ResourceVectorIcon
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun TechScreen(
    state: GameState,
    onStartResearch: (TechId) -> Unit,
    onSpeedupResearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val str = LocalAppStrings.current
    var branchIdx by remember { mutableIntStateOf(0) }
    val branch = TechBranch.entries[branchIdx]
    val labLevel = state.buildings.firstOrNull { it.type == BuildingType.RESEARCH_LAB }?.level ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateAbyss)
    ) {
        TabRow(selectedTabIndex = branchIdx, containerColor = SlateSurface, contentColor = IceCyan) {
            Tab(selected = branchIdx == 0, onClick = { branchIdx = 0 }, text = { Text(str.techEconomy) })
            Tab(selected = branchIdx == 1, onClick = { branchIdx = 1 }, text = { Text(str.techGrowth) })
            Tab(selected = branchIdx == 2, onClick = { branchIdx = 2 }, text = { Text(str.techBattle) })
        }

        if (labLevel <= 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CrimsonAlert.copy(alpha = 0.16f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠ ${str.buildingName(BuildingType.RESEARCH_LAB)} (Lv.1+)",
                    color = CrimsonAlert,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        state.activeResearch?.let { ar ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, IceCyan)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${str.researching}: ${str.techName(ar.techId)}", color = IceGlow, style = MaterialTheme.typography.titleSmall)
                        LinearProgressIndicator(
                            progress = { if (ar.totalSec > 0) 1f - ar.remainingSec.toFloat() / ar.totalSec else 0.5f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = IceCyan,
                            trackColor = SlateAbyss
                        )
                        Text("${ar.remainingSec}s", style = MaterialTheme.typography.labelSmall, color = MistText)
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = onSpeedupResearch,
                        enabled = state.resources.speedupsMin > 0,
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
                    ) {
                        Text("⚡ ${str.speedupBtn}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        val metas = GameCatalog.techMetas.filter { it.branch == branch }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(metas, key = { it.id.name }) { meta ->
                val curLv = GameCatalog.techLevel(state, meta.id)
                val cost = GameCatalog.techResearchCost(meta.id, curLv)
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (branch) {
                                TechBranch.ECONOMY -> Icons.Default.Inventory2
                                TechBranch.GROWTH -> Icons.Default.Engineering
                                TechBranch.BATTLE -> Icons.Default.MilitaryTech
                            },
                            contentDescription = null,
                            tint = when (branch) {
                                TechBranch.ECONOMY -> WarmGold
                                TechBranch.GROWTH -> EmeraldSafe
                                TechBranch.BATTLE -> CrimsonAlert
                            },
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "${str.techName(meta.id)}  (${str.levelShort}.$curLv/${meta.maxLevel})",
                                style = MaterialTheme.typography.titleSmall,
                                color = FrostWhite
                            )
                            Text(str.techDesc(meta.id), style = MaterialTheme.typography.bodyMedium, color = MistText)
                            if (curLv < meta.maxLevel) {
                                Text(
                                    text = "🪵 ${cost.wood}  🔥 ${cost.coal}  ⚙ ${cost.iron}  •  ⏱ ${cost.durationSec}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IceGlow
                                )
                            }
                        }
                        if (curLv < meta.maxLevel) {
                            Button(
                                onClick = { onStartResearch(meta.id) },
                                enabled = labLevel > 0 && state.activeResearch == null,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("research_${meta.id.name}"),
                                colors = ButtonDefaults.buttonColors(containerColor = IceCyan, contentColor = SlateAbyss)
                            ) {
                                Text(str.research, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("MAX", style = MaterialTheme.typography.labelSmall, color = EmeraldSafe)
                        }
                    }
                }
            }
        }
    }
}

private data class BackpackEntry(
    val id: String,
    val title: String,
    val count: String,
    val iconType: ResIconType?,
    val tint: Color,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

@Composable
fun BackpackScreen(
    state: GameState,
    onOpenGachaTab: () -> Unit,
    onOpenQuestsModal: () -> Unit,
    onOpenShopModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val str = LocalAppStrings.current
    val items = listOf(
        BackpackEntry("bp_silver", str.silverKey, "x${state.resources.silverKeys}", null, IceGlow, str.recruitSubTab, onOpenGachaTab),
        BackpackEntry("bp_gold", str.goldKey, "x${state.resources.goldKeys}", null, WarmGold, str.recruitSubTab, onOpenGachaTab),
        BackpackEntry("bp_speedup", str.speedups, "${state.resources.speedupsMin}m", ResIconType.SPEEDUP, IceCyan),
        BackpackEntry("bp_xp", str.heroXp, "${state.resources.heroXp}", null, EmeraldSafe),
        BackpackEntry("bp_gems", str.gems, "${state.resources.gems}", ResIconType.GEMS, MythicPurple),
        BackpackEntry("bp_meals", str.meals, "${state.resources.meals.roundToInt()}", ResIconType.MEALS, WarmGold),
        BackpackEntry("bp_meat", str.meat, "${state.resources.meat.roundToInt()}", ResIconType.MEAT, CrimsonAlert),
        BackpackEntry("bp_wood", str.wood, "${state.resources.wood.roundToInt()}", ResIconType.WOOD, EmeraldSafe),
        BackpackEntry("bp_coal", str.coal, "${state.resources.coal.roundToInt()}", ResIconType.COAL, FurnaceAmber),
        BackpackEntry("bp_iron", str.iron, "${state.resources.iron.roundToInt()}", ResIconType.IRON, IceGlow)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateAbyss)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(str.backpackTitle, style = MaterialTheme.typography.headlineMedium, color = WarmGold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onOpenShopModal,
                    modifier = Modifier.defaultMinSize(minHeight = 42.dp).testTag("backpack_shop_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(str.shopTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                FilledTonalButton(onClick = onOpenQuestsModal) {
                    Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(str.tabQuests, fontSize = 11.sp)
                }
            }
        }

        // Garrison Summary Strip
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, IceCyan.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("🛡 ${str.infantry}: ${state.troops.infantry}", style = MaterialTheme.typography.labelMedium, color = FrostWhite)
                Text("🔱 ${str.lancers}: ${state.troops.lancers}", style = MaterialTheme.typography.labelMedium, color = IceGlow)
                Text("🏹 ${str.marksmen}: ${state.troops.marksmen}", style = MaterialTheme.typography.labelMedium, color = WarmGold)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items, key = { it.id }) { entry ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, entry.tint.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (entry.iconType != null) {
                            ResourceVectorIcon(entry.iconType, size = 32.dp)
                        } else {
                            Icon(
                                imageVector = if (entry.id.contains("xp")) Icons.Default.AutoAwesome else Icons.Default.VpnKey,
                                contentDescription = entry.title,
                                tint = entry.tint,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(entry.title, style = MaterialTheme.typography.labelMedium, color = MistText, maxLines = 1)
                        Text(entry.count, style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                        if (entry.actionLabel != null && entry.onAction != null) {
                            Button(
                                onClick = entry.onAction,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 36.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = entry.tint, contentColor = SlateAbyss)
                            ) {
                                Text(entry.actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterProgressAndQuestsModal(
    state: GameState,
    onDismiss: () -> Unit,
    onGoToMission: (String) -> Unit,
    onClaimChapter: (String) -> Unit,
    onClaimChapterChest: (Int) -> Unit,
    onClaimDaily: (String) -> Unit,
    onClaimMilestone: (Int) -> Unit
) {
    val str = LocalAppStrings.current
    var tab by remember { mutableIntStateOf(0) }
    val chapterDefs = GameCatalog.chapterMissions.filter { it.chapter == state.quests.chapter }
    val allMissionsClaimed = chapterDefs.isNotEmpty() && chapterDefs.all { state.quests.claimedChapterMissions.contains(it.id) }
    val chestClaimed = state.quests.claimedChapterChests.contains(state.quests.chapter)
    val chestReward = GameCatalog.chapterChestReward(state.quests.chapter)
    val earnedDailyPts = GameCatalog.dailyQuests
        .filter { state.quests.claimedDailyIds.contains(it.id) }
        .sumOf { it.points }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(14.dp).heightIn(max = 620.dp),
            shape = RoundedCornerShape(22.dp),
            color = SlateAbyss,
            border = androidx.compose.foundation.BorderStroke(1.8.dp, WarmGold.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateSurface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${str.chapterProgress} — ${str.chapter} ${state.quests.chapter}", style = MaterialTheme.typography.titleMedium, color = WarmGold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = str.close, tint = MistText) }
                }

                TabRow(selectedTabIndex = tab, containerColor = SlateSurface, contentColor = WarmGold) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(str.chapterMissions) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(str.dailyQuests) })
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (tab == 0) {
                        items(chapterDefs, key = { it.id }) { m ->
                            val claimed = state.quests.claimedChapterMissions.contains(m.id)
                            val (cur, req) = m.progress(state)
                            val ready = !claimed && cur >= req
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (ready) EmeraldSafe else SlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(if (str.isRtl) m.titleAr else m.titleEn, color = FrostWhite, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "$cur/$req  •  +${m.rewardGems} 💎 +${m.rewardMeat} ${str.meat} +${m.rewardWood} ${str.wood}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = IceGlow
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (ready) onClaimChapter(m.id) else if (!claimed) onGoToMission(m.id)
                                        },
                                        enabled = !claimed,
                                        modifier = Modifier.defaultMinSize(minHeight = 42.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (claimed) SlateElevated else if (ready) EmeraldSafe else WarmGold,
                                            contentColor = if (claimed) MistText else SlateAbyss
                                        )
                                    ) {
                                        Text(
                                            text = if (claimed) str.claimed else if (ready) str.claim else str.goAction,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Grand Chapter Chest Card at bottom of Chapter
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F170B)),
                                border = androidx.compose.foundation.BorderStroke(1.8.dp, WarmGold)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = WarmGold, modifier = Modifier.size(32.dp))
                                        Column {
                                            Text(str.grandChest, style = MaterialTheme.typography.titleSmall, color = WarmGold)
                                            Text(
                                                text = "+${chestReward.gems} 💎 • +${chestReward.goldKeys} 🔑 • +${chestReward.speedupsMin}m ⚡ • +${chestReward.heroXp} XP",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = FrostWhite
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { onClaimChapterChest(state.quests.chapter) },
                                        enabled = allMissionsClaimed && !chestClaimed,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (allMissionsClaimed && !chestClaimed) EmeraldSafe else SlateElevated,
                                            contentColor = if (allMissionsClaimed && !chestClaimed) SlateAbyss else MistText
                                        )
                                    ) {
                                        Text(if (chestClaimed) str.claimed else str.claimChest, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = androidx.compose.foundation.BorderStroke(1.dp, IceCyan.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${str.dailyQuests}: $earnedDailyPts / 100", color = WarmGold, style = MaterialTheme.typography.titleSmall)
                                    LinearProgressIndicator(
                                        progress = { (earnedDailyPts / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp)),
                                        color = WarmGold,
                                        trackColor = SlateAbyss
                                    )
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        listOf(25, 50, 100).forEach { target ->
                                            val claimed = state.quests.claimedMilestonePoints.contains(target)
                                            val ready = !claimed && earnedDailyPts >= target
                                            OutlinedButton(
                                                onClick = { onClaimMilestone(target) },
                                                enabled = ready,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (ready) WarmGold else SlateBorder)
                                            ) {
                                                Text(
                                                    text = if (claimed) "✓ $target" else "🎁 $target",
                                                    color = if (ready) WarmGold else MistText,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        items(GameCatalog.dailyQuests, key = { it.id }) { dq ->
                            val claimed = state.quests.claimedDailyIds.contains(dq.id)
                            val (cur, req) = dq.progress(state)
                            val ready = !claimed && cur >= req
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (ready) EmeraldSafe else SlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(if (str.isRtl) dq.titleAr else dq.titleEn, color = FrostWhite, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "$cur/$req  •  +${dq.points} pts • +${dq.rewardGems} 💎 +${dq.rewardXp} XP",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WarmGold
                                        )
                                    }
                                    Button(
                                        onClick = { onClaimDaily(dq.id) },
                                        enabled = ready,
                                        modifier = Modifier.defaultMinSize(minHeight = 42.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (ready) EmeraldSafe else SlateElevated,
                                            contentColor = if (ready) SlateAbyss else MistText
                                        )
                                    ) {
                                        Text(if (claimed) str.claimed else if (ready) str.claim else "$cur/$req", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
