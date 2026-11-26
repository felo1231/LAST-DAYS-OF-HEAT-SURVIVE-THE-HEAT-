package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.GameCatalog
import com.example.localization.LocalAppStrings
import com.example.model.*
import com.example.ui.art.AsyncDrawableImage
import com.example.ui.art.EnemyMonsterPortrait
import com.example.ui.art.HeroPortraitArt
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HeroesScreen(
    state: GameState,
    subTab: Int,
    onSelectSubTab: (Int) -> Unit,
    lastGachaResult: GachaPullResult?,
    battleSession: BattleSession?,
    onLevelUpHero: (String) -> Unit,
    onPromoteStar: (String) -> Unit,
    onUpgradeGear: (String) -> Unit,
    onToggleFormation: (String) -> Unit,
    onPullGacha: (Boolean) -> Unit,
    onDismissGacha: () -> Unit,
    onStartBattle: () -> Unit,
    onToggleAutoSkill: () -> Unit,
    onTriggerSkill: (String) -> Unit,
    onFinishBattle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val str = LocalAppStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateAbyss)
    ) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = SlateSurface,
            contentColor = IceCyan
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { onSelectSubTab(0) },
                modifier = Modifier.testTag("heroes_subtab_roster"),
                text = { Text(str.rosterSubTab, maxLines = 1) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { onSelectSubTab(1) },
                modifier = Modifier.testTag("heroes_subtab_recruit"),
                text = { Text(str.recruitSubTab, maxLines = 1) }
            )
            Tab(
                selected = subTab == 2,
                onClick = { onSelectSubTab(2) },
                modifier = Modifier.testTag("heroes_subtab_explore"),
                text = { Text(str.exploreSubTab, maxLines = 1) }
            )
        }

        when (subTab) {
            0 -> HeroRosterList(
                state = state,
                onLevelUpHero = onLevelUpHero,
                onPromoteStar = onPromoteStar,
                onUpgradeGear = onUpgradeGear,
                onToggleFormation = onToggleFormation
            )
            1 -> HeroGachaView(
                state = state,
                onPullGacha = onPullGacha
            )
            2 -> ExplorationPrepView(
                state = state,
                onToggleFormation = onToggleFormation,
                onStartBattle = onStartBattle
            )
        }
    }

    if (lastGachaResult != null) {
        GachaRevealDialog(result = lastGachaResult, onDismiss = onDismissGacha)
    }

    if (battleSession != null) {
        ExplorationBattleModal(
            session = battleSession,
            onToggleAuto = onToggleAutoSkill,
            onTriggerSkill = onTriggerSkill,
            onFinish = onFinishBattle
        )
    }
}

@Composable
private fun HeroRosterList(
    state: GameState,
    onLevelUpHero: (String) -> Unit,
    onPromoteStar: (String) -> Unit,
    onUpgradeGear: (String) -> Unit,
    onToggleFormation: (String) -> Unit
) {
    val str = LocalAppStrings.current
    val infinite = rememberInfiniteTransition(label = "mythicBorder")
    val shimmer by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "shimmer"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateSurface)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⭐ ${str.heroXp}: ${state.resources.heroXp}", style = MaterialTheme.typography.titleSmall, color = IceGlow)
            Text(
                "${str.formation}: ${state.formationHeroIds.size}/5",
                style = MaterialTheme.typography.labelMedium,
                color = WarmGold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = androidx.compose.foundation.BorderStroke(1.4.dp, WarmGold.copy(alpha = 0.65f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(92.dp)) {
                        AsyncDrawableImage(
                            resId = R.drawable.img_3d_nile_tundra,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            maxDimPx = 640,
                            placeholderColor = SlateCard,
                            modifier = Modifier.fillMaxSize().alpha(0.32f)
                        )
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncDrawableImage(
                                resId = R.drawable.img_3d_hero_horus,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                maxDimPx = 256,
                                placeholderColor = SlateElevated,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.8.dp, WarmGold, RoundedCornerShape(12.dp))
                            )
                            AsyncDrawableImage(
                                resId = R.drawable.img_3d_hero_amunet,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                maxDimPx = 256,
                                placeholderColor = SlateElevated,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.8.dp, MythicPurple, RoundedCornerShape(12.dp))
                            )
                            AsyncDrawableImage(
                                resId = R.drawable.img_3d_hero_thutmose,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                maxDimPx = 256,
                                placeholderColor = SlateElevated,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.8.dp, FurnaceOrange, RoundedCornerShape(12.dp))
                            )
                            AsyncDrawableImage(
                                resId = R.drawable.img_3d_hero_cleopatra,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                maxDimPx = 256,
                                placeholderColor = SlateElevated,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.8.dp, NileTurquoise, RoundedCornerShape(12.dp))
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = str.rosterSubTab,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = WarmGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${state.heroes.count { it.unlocked }}/${state.heroes.size} • ${str.formation}: ${state.formationHeroIds.size}/5",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IceGlow,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            items(state.heroes, key = { it.id }) { hero ->
                val accent = Color(hero.accentColorHex)
                val inFormation = state.formationHeroIds.contains(hero.id)
                val frameBrush = when (hero.rarity) {
                    HeroRarity.MYTHIC -> Brush.linearGradient(
                        listOf(
                            WarmGold.copy(alpha = shimmer),
                            FurnaceOrange,
                            MythicPurple.copy(alpha = shimmer)
                        )
                    )
                    HeroRarity.EPIC -> Brush.linearGradient(listOf(EpicViolet, Color(0xFF7C3AED), IceCyan))
                    HeroRarity.RARE -> Brush.linearGradient(listOf(RareBlue, IceCyan))
                }
                val rarityColor = when (hero.rarity) {
                    HeroRarity.MYTHIC -> FurnaceOrange
                    HeroRarity.EPIC -> EpicViolet
                    HeroRarity.RARE -> RareBlue
                }
                val xpCost = 80 + hero.level * 45
                val shardCost = 5 + hero.stars * 5
                val gearIronCost = 45 * hero.gearLevel

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (hero.unlocked) SlateCard else SlateSurface.copy(alpha = 0.65f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (hero.rarity == HeroRarity.MYTHIC) 2.2.dp else 1.5.dp,
                        brush = frameBrush
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Authentic Character Portrait Card with Rarity Frame, Level Tag, Class Emblem & Gold Stars
                            Box(
                                modifier = Modifier
                                    .width(82.dp)
                                    .height(98.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(2.5.dp, frameBrush, RoundedCornerShape(14.dp))
                            ) {
                                HeroPortraitArt(
                                    heroId = hero.id,
                                    heroClass = hero.heroClass,
                                    rarity = hero.rarity,
                                    accentColor = accent,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Top-Start Level Tag
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SlateAbyss.copy(alpha = 0.85f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (hero.unlocked) "${str.levelShort}.${hero.level}" else "🔒",
                                        fontSize = 9.sp,
                                        color = WarmGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Top-End Class Emblem Badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(SlateAbyss.copy(alpha = 0.85f))
                                        .border(1.dp, rarityColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (hero.heroClass) {
                                            HeroClass.INFANTRY -> Icons.Default.Shield
                                            HeroClass.LANCER -> Icons.Default.FlashOn
                                            HeroClass.MARKSMAN -> Icons.Default.GpsFixed
                                        },
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }

                                // Bottom Gold Stars Strip
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(SlateAbyss.copy(alpha = 0.78f))
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "★".repeat(hero.stars) + "☆".repeat(5 - hero.stars),
                                        fontSize = 9.sp,
                                        color = WarmGold
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (str.isRtl) hero.nameAr else hero.nameEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = FrostWhite
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(rarityColor.copy(alpha = 0.22f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = str.rarityLabel(hero.rarity),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = rarityColor
                                        )
                                    }
                                }
                                Text(
                                    text = "${str.classLabel(hero.heroClass)} • ${if (str.isRtl) hero.titleAr else hero.titleEn}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MistText
                                )
                                if (hero.unlocked) {
                                    Text(
                                        text = "⚔ ${GameCatalog.heroAttack(hero, state)}   ❤ ${GameCatalog.heroMaxHp(hero, state)}   🛡 ${GameCatalog.heroDefense(hero, state)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldSafe
                                    )
                                } else {
                                    Text(
                                        text = "🔒 ${hero.shards} shards",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MistText
                                    )
                                }
                            }

                            if (hero.unlocked) {
                                FilterChip(
                                    selected = inFormation,
                                    onClick = { onToggleFormation(hero.id) },
                                    label = { Text(if (inFormation) "✓" else str.selectHero, fontSize = 11.sp) }
                                )
                            }
                        }

                        Text(
                            text = "⚡ ${if (str.isRtl) hero.skillNameAr else hero.skillNameEn}: ${if (str.isRtl) hero.skillDescAr else hero.skillDescEn}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IceGlow
                        )

                        if (hero.unlocked) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onLevelUpHero(hero.id) },
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text("${str.levelUp}\n($xpCost XP)", fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { onPromoteStar(hero.id) },
                                    enabled = hero.stars < 5,
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text("${str.promoteStar}\n(${hero.shards}/$shardCost)", fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { onUpgradeGear(hero.id) },
                                    enabled = hero.gearLevel < 10,
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text("${str.upgradeGear} ${hero.gearLevel}\n($gearIronCost ⚙)", fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
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
private fun HeroGachaView(
    state: GameState,
    onPullGacha: (Boolean) -> Unit
) {
    val str = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AsyncDrawableImage(
                resId = R.drawable.img_gacha_frost_chest,
                contentDescription = str.recruitSubTab,
                contentScale = ContentScale.Crop,
                maxDimPx = 420,
                placeholderColor = SlateCard,
                modifier = Modifier
                    .size(190.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, WarmGold.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text("🗝 ${str.silverKey}: ${state.resources.silverKeys}", color = IceGlow) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("🔑 ${str.goldKey}: ${state.resources.goldKeys}", color = WarmGold) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onPullGacha(false) },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp)
                        .testTag("pull_silver_chest"),
                    colors = ButtonDefaults.buttonColors(containerColor = IceCyan, contentColor = SlateAbyss)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(str.pullSilver, fontWeight = FontWeight.Bold)
                        Text(
                            if (state.resources.silverKeys > 0) "1x ${str.silverKey}" else "80 💎",
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = { onPullGacha(true) },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp)
                        .testTag("pull_gold_chest"),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(str.pullGold, fontWeight = FontWeight.Bold)
                        Text(
                            if (state.resources.goldKeys > 0) "1x ${str.goldKey}" else "180 💎",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorationPrepView(
    state: GameState,
    onToggleFormation: (String) -> Unit,
    onStartBattle: () -> Unit
) {
    val str = LocalAppStrings.current
    val deployed = state.formationHeroIds.mapNotNull { id -> state.heroes.firstOrNull { it.id == id && it.unlocked } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, IceCyan.copy(alpha = 0.60f))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncDrawableImage(
                    resId = R.drawable.img_3d_nile_tundra,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    maxDimPx = 640,
                    placeholderColor = SlateCard,
                    modifier = Modifier.matchParentSize().alpha(0.26f)
                )
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${str.exploreSubTab} — ${str.stage} ${state.explorationStage}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = WarmGold
                    )
                    Text(
                        text = str.deployUpTo5,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FrostWhite
                    )
                    Text(
                        text = "🎁 +${45 + state.explorationStage * 12} 💎 • +${110 + state.explorationStage * 45} XP • +${90 + state.explorationStage * 30} ${str.meat}",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldSafe
                    )
                }
            }
        }

        Text("${str.formation} (${deployed.size}/5)", style = MaterialTheme.typography.titleSmall, color = FrostWhite)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.heroes.filter { it.unlocked }, key = { it.id }) { hero ->
                val selected = state.formationHeroIds.contains(hero.id)
                val accent = Color(hero.accentColorHex)
                Card(
                    modifier = Modifier
                        .width(104.dp)
                        .clickable { onToggleFormation(hero.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) SlateElevated else SlateCard
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.8.dp,
                        if (selected) WarmGold else SlateBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        HeroPortraitArt(
                            heroId = hero.id,
                            heroClass = hero.heroClass,
                            rarity = hero.rarity,
                            accentColor = accent,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Text(
                            text = if (str.isRtl) hero.nameAr else hero.nameEn,
                            style = MaterialTheme.typography.labelMedium,
                            color = FrostWhite,
                            maxLines = 1
                        )
                        Text(
                            text = if (hero.heroClass == HeroClass.INFANTRY) str.frontline else str.backline,
                            fontSize = 10.sp,
                            color = if (selected) WarmGold else MistText
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onStartBattle,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .testTag("start_exploration_battle_button"),
            colors = ButtonDefaults.buttonColors(containerColor = FurnaceOrange, contentColor = SlateAbyss)
        ) {
            Icon(Icons.Default.SportsKabaddi, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("${str.startBattle} (${str.stage} ${state.explorationStage})", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun GachaRevealDialog(result: GachaPullResult, onDismiss: () -> Unit) {
    val str = LocalAppStrings.current
    val accent = Color(result.accentColorHex)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Text(
                text = if (str.isRtl) result.heroNameAr else result.heroNameEn,
                color = WarmGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroPortraitArt(
                    heroId = result.heroId,
                    heroClass = HeroClass.INFANTRY,
                    rarity = result.rarity,
                    accentColor = accent,
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(2.5.dp, WarmGold, RoundedCornerShape(18.dp))
                )
                Text(str.rarityLabel(result.rarity), color = accent, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (result.isNewUnlock) "✨ Unlocked New Hero!" else "+${result.shardsGained} Hero Shards",
                    color = EmeraldSafe,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
            ) {
                Text(str.collect, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ExplorationBattleModal(
    session: BattleSession,
    onToggleAuto: () -> Unit,
    onTriggerSkill: (String) -> Unit,
    onFinish: (Boolean) -> Unit
) {
    val str = LocalAppStrings.current
    val allies = session.units.filter { it.isAlly }
    val enemies = session.units.filter { !it.isAlly }

    val infinite = rememberInfiniteTransition(label = "arenaIdle")
    val bobPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "bobPhase"
    )
    val sandDrift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Restart),
        label = "sandDrift"
    )

    val effectAnim = remember { Animatable(1f) }
    LaunchedEffect(session.tickSeq) {
        effectAnim.snapTo(0f)
        effectAnim.animateTo(1f, animationSpec = tween(390, easing = FastOutSlowInEasing))
    }

    var arenaCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val unitCenters = remember { mutableStateMapOf<String, Offset>() }

    Dialog(
        onDismissRequest = { if (session.outcome != BattleOutcome.ONGOING) onFinish(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = RoundedCornerShape(22.dp),
            color = SlateAbyss,
            border = androidx.compose.foundation.BorderStroke(2.dp, WarmGold.copy(alpha = 0.72f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Frozen Nile desert battleground backdrop
                AsyncDrawableImage(
                    resId = R.drawable.img_3d_nile_tundra,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    maxDimPx = 640,
                    placeholderColor = SlateAbyss,
                    modifier = Modifier.fillMaxSize().alpha(0.32f)
                )

                // Atmospheric desert sunset + frost sandstorm gradient & dune silhouettes
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                SlateAbyss.copy(alpha = 0.85f),
                                Color(0xFF1A2542).copy(alpha = 0.42f),
                                Color(0xFF2B1C10).copy(alpha = 0.58f),
                                SlateAbyss.copy(alpha = 0.92f)
                            )
                        )
                    )
                    // Distant Giza Pyramid silhouette
                    val pyr = Path().apply {
                        moveTo(w * 0.65f, h * 0.46f)
                        lineTo(w * 0.86f, h * 0.30f)
                        lineTo(w * 1.02f, h * 0.46f)
                        close()
                    }
                    drawPath(pyr, color = WarmGold.copy(alpha = 0.14f))

                    // Drifting frost-sand motes across battlefield
                    for (i in 0..14) {
                        val px = ((sandDrift + i * 0.13f) % 1f) * w
                        val py = h * (0.18f + (i % 7) * 0.10f) + sin(bobPhase + i) * 6f
                        drawCircle(
                            color = if (i % 2 == 0) WarmGold.copy(alpha = 0.32f) else IceCyan.copy(alpha = 0.30f),
                            radius = 2.6f + (i % 3),
                            center = Offset(px, py)
                        )
                    }
                }

                // Dramatic Ultimate Skill Screen Flash Overlay
                val flashText = if (str.isRtl) session.activeFlashTextAr else session.activeFlashTextEn
                if (flashText != null && session.outcome == BattleOutcome.ONGOING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(session.activeFlashColorHex).copy(alpha = 0.22f * (1f - effectAnim.value * 0.5f)))
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${str.battleWaveLabel} ${session.stage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NileTurquoise,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (str.isRtl) session.enemyTitleAr else session.enemyTitleEn,
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = session.autoSkill,
                                onClick = onToggleAuto,
                                modifier = Modifier.testTag("battle_auto_toggle"),
                                label = { Text(str.autoSkill, fontSize = 11.sp) }
                            )
                            IconButton(
                                onClick = { onFinish(false) },
                                modifier = Modifier.testTag("battle_close_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = str.close, tint = MistText)
                            }
                        }
                    }

                    // Dramatic Ultimate Banner
                    AnimatedVisibility(visible = flashText != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color(session.activeFlashColorHex).copy(alpha = 0.88f), Color.Transparent)
                                    )
                                )
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚡ ${flashText.orEmpty()} ⚡",
                                style = MaterialTheme.typography.titleMedium,
                                color = FrostWhite,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Live Animated 2D Desert Battlefield Arena
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.2.dp, SlateBorder.copy(alpha = 0.85f), RoundedCornerShape(18.dp))
                            .onGloballyPositioned { arenaCoords = it }
                            .padding(horizontal = 6.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Enemy Formation Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Top
                            ) {
                                enemies.forEachIndexed { idx, u ->
                                    val dmg = session.floatingDamages.lastOrNull { it.targetUnitId == u.id }
                                    BattleUnitPortraitCard(
                                        unit = u,
                                        damage = dmg,
                                        bobOffsetDp = if (u.hp > 0) sin(bobPhase + idx * 1.2f) * 3f else 0f,
                                        isLunging = u.id in session.lungingUnitIds,
                                        isHit = u.id in session.hitUnitIds,
                                        actionProgress = effectAnim.value,
                                        onPositioned = { coords ->
                                            val root = arenaCoords
                                            if (root != null && coords.isAttached && root.isAttached) {
                                                val rel = coords.positionInRoot() - root.positionInRoot()
                                                unitCenters[u.id] = Offset(
                                                    rel.x + coords.size.width * 0.5f,
                                                    rel.y + coords.size.height * 0.5f
                                                )
                                            }
                                        },
                                        onClick = {}
                                    )
                                }
                            }

                            // Central Desert Sand Clash Line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.84f)
                                    .height(2.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, WarmGold.copy(alpha = 0.45f), NileTurquoise.copy(alpha = 0.45f), Color.Transparent)
                                        )
                                    )
                            )

                            // Ally Pharaonic Kings & Queens Formation Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                allies.forEachIndexed { idx, u ->
                                    val dmg = session.floatingDamages.lastOrNull { it.targetUnitId == u.id }
                                    BattleUnitPortraitCard(
                                        unit = u,
                                        damage = dmg,
                                        bobOffsetDp = if (u.hp > 0) sin(bobPhase + idx * 1.5f + 0.7f) * 3.5f else 0f,
                                        isLunging = u.id in session.lungingUnitIds,
                                        isHit = u.id in session.hitUnitIds,
                                        actionProgress = effectAnim.value,
                                        onPositioned = { coords ->
                                            val root = arenaCoords
                                            if (root != null && coords.isAttached && root.isAttached) {
                                                val rel = coords.positionInRoot() - root.positionInRoot()
                                                unitCenters[u.id] = Offset(
                                                    rel.x + coords.size.width * 0.5f,
                                                    rel.y + coords.size.height * 0.5f
                                                )
                                            }
                                        },
                                        onClick = { if (u.energy >= 100 && u.hp > 0) onTriggerSkill(u.id) }
                                    )
                                }
                            }
                        }

                        // Real-time Attack Trajectory, Slash, Lance & Ultimate Nova Canvas Overlay
                        if (session.outcome == BattleOutcome.ONGOING) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val t = effectAnim.value
                                session.activeEffects.forEach { fx ->
                                    val from = unitCenters[fx.attackerId] ?: return@forEach
                                    val to = unitCenters[fx.targetId] ?: return@forEach
                                    val fxColor = Color(fx.colorHex)
                                    val current = Offset(
                                        from.x + (to.x - from.x) * t,
                                        from.y + (to.y - from.y) * t
                                    )
                                    val fade = (1f - t * 0.65f).coerceIn(0f, 1f)

                                    when (fx.actionType) {
                                        BattleActionType.ARROW -> {
                                            val trailStart = Offset(
                                                from.x + (to.x - from.x) * (t - 0.25f).coerceAtLeast(0f),
                                                from.y + (to.y - from.y) * (t - 0.25f).coerceAtLeast(0f)
                                            )
                                            drawLine(
                                                brush = Brush.linearGradient(listOf(Color.Transparent, fxColor, FrostWhite)),
                                                start = trailStart,
                                                end = current,
                                                strokeWidth = if (fx.isCrit) 7f else 4.5f,
                                                cap = StrokeCap.Round
                                            )
                                            drawCircle(FrostWhite, radius = 6f * fade, center = current)
                                        }
                                        BattleActionType.LANCE_CHARGE -> {
                                            drawLine(
                                                color = NileTurquoise.copy(alpha = 0.72f * fade),
                                                start = from,
                                                end = current,
                                                strokeWidth = 6f,
                                                cap = StrokeCap.Round
                                            )
                                            drawCircle(
                                                color = WarmGold.copy(alpha = fade),
                                                radius = 12f * (1f - t * 0.4f),
                                                center = current
                                            )
                                        }
                                        BattleActionType.SLASH -> {
                                            val impactR = 26f + 22f * t
                                            drawArc(
                                                color = fxColor.copy(alpha = fade),
                                                startAngle = -140f + 90f * t,
                                                sweepAngle = 120f,
                                                useCenter = false,
                                                topLeft = Offset(to.x - impactR, to.y - impactR),
                                                size = Size(impactR * 2f, impactR * 2f),
                                                style = Stroke(width = if (fx.isCrit) 8f else 5.5f, cap = StrokeCap.Round)
                                            )
                                            drawLine(
                                                color = FrostWhite.copy(alpha = fade),
                                                start = Offset(to.x - 20f, to.y - 16f),
                                                end = Offset(to.x + 20f, to.y + 16f),
                                                strokeWidth = 4f,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                        BattleActionType.HEAL_RAY -> {
                                            drawLine(
                                                brush = Brush.linearGradient(listOf(EmeraldSafe.copy(alpha = 0.2f), EmeraldSafe, WarmGold)),
                                                start = from,
                                                end = to,
                                                strokeWidth = 5f * fade,
                                                cap = StrokeCap.Round
                                            )
                                            drawCircle(
                                                color = EmeraldSafe.copy(alpha = 0.75f * fade),
                                                radius = 16f + 24f * t,
                                                center = to,
                                                style = Stroke(width = 4f)
                                            )
                                        }
                                        BattleActionType.ULTIMATE_NOVA -> {
                                            val novaR = 18f + 56f * t
                                            drawCircle(
                                                color = fxColor.copy(alpha = 0.85f * fade),
                                                radius = novaR,
                                                center = to,
                                                style = Stroke(width = 6f)
                                            )
                                            drawCircle(
                                                color = WarmGold.copy(alpha = 0.7f * fade),
                                                radius = novaR * 0.6f,
                                                center = to
                                            )
                                            for (deg in 0 until 360 step 45) {
                                                val rad = deg * PI / 180.0
                                                drawLine(
                                                    color = FrostWhite.copy(alpha = fade),
                                                    start = Offset(to.x + cos(rad).toFloat() * 8f, to.y + sin(rad).toFloat() * 8f),
                                                    end = Offset(to.x + cos(rad).toFloat() * novaR, to.y + sin(rad).toFloat() * novaR),
                                                    strokeWidth = 3f,
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Combat Log Ticker
                    val logLine = if (str.isRtl) session.combatLogAr else session.combatLogEn
                    if (logLine.isNotBlank() && session.outcome == BattleOutcome.ONGOING) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateSurface.copy(alpha = 0.88f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = logLine,
                                style = MaterialTheme.typography.labelSmall,
                                color = IceGlow,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Outcome Banner / Skill Hint
                    AnimatedVisibility(visible = session.outcome != BattleOutcome.ONGOING) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                if (session.outcome == BattleOutcome.VICTORY) EmeraldSafe else CrimsonAlert
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (session.outcome == BattleOutcome.VICTORY) str.victory else str.defeat,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (session.outcome == BattleOutcome.VICTORY) EmeraldSafe else CrimsonAlert
                                )
                                if (session.outcome == BattleOutcome.VICTORY) {
                                    Text(
                                        text = "+${session.rewardGems} 💎  +${session.rewardXp} XP  +${session.rewardMeat} ${str.meat}" +
                                            (if (session.rewardKey) "  +1 🗝" else ""),
                                        color = WarmGold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Button(
                                    onClick = { onFinish(session.outcome == BattleOutcome.VICTORY) },
                                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("battle_finish_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (session.outcome == BattleOutcome.VICTORY) EmeraldSafe else IceCyan,
                                        contentColor = SlateAbyss
                                    )
                                ) {
                                    Text(
                                        if (session.outcome == BattleOutcome.VICTORY) str.claimNext else str.retry,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (session.outcome == BattleOutcome.ONGOING) {
                        Text(
                            text = if (session.autoSkill) "⚡ ${str.autoSkill}" else str.battleTapUltimate,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (session.autoSkill) MistText else WarmGold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleUnitPortraitCard(
    unit: BattleUnit,
    damage: FloatingDamage?,
    bobOffsetDp: Float,
    isLunging: Boolean,
    isHit: Boolean,
    actionProgress: Float,
    onPositioned: (LayoutCoordinates) -> Unit,
    onClick: () -> Unit
) {
    val str = LocalAppStrings.current
    val hpFraction by animateFloatAsState(
        targetValue = (unit.hp.toFloat() / unit.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(240),
        label = "hpAnim"
    )
    val energyReady = unit.isAlly && unit.energy >= 100 && unit.hp > 0
    val accent = Color(unit.accentColorHex)

    // Lunge forward cleanly toward center on attack, recoil horizontally when hit
    val lungeSign = if (unit.isAlly) -1f else 1f
    val lungeCurve = if (isLunging) sin(actionProgress * PI).toFloat() * 18f * lungeSign else 0f
    val hitShake = if (isHit && actionProgress < 0.7f) sin(actionProgress * 28f) * 6f else 0f
    val cardAlpha = if (unit.hp > 0) 1f else 0.35f

    Column(
        modifier = Modifier
            .width(66.dp)
            .onGloballyPositioned(onPositioned)
            .graphicsLayer {
                translationY = (bobOffsetDp + lungeCurve).dp.toPx()
                translationX = hitShake.dp.toPx()
                alpha = cardAlpha
                if (energyReady) {
                    scaleX = 1.06f
                    scaleY = 1.06f
                }
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isHit && actionProgress < 0.45f) CrimsonAlert.copy(alpha = 0.45f)
                else if (unit.hp > 0) SlateCard.copy(alpha = 0.94f)
                else SlateSurface.copy(alpha = 0.4f)
            )
            .border(
                width = if (energyReady) 2.4.dp else 1.3.dp,
                color = if (energyReady) WarmGold else if (unit.hp > 0) accent.copy(alpha = 0.78f) else SlateBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = energyReady, onClick = onClick)
            .testTag("battle_unit_${unit.id}")
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (damage != null) {
            Text(
                text = if (damage.isHeal) "+${damage.amount}" else if (damage.isCrit) "⚡-${damage.amount}" else "-${damage.amount}",
                fontSize = if (damage.isCrit) 10.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (damage.isHeal) EmeraldSafe else if (damage.isCrit) WarmGold else CrimsonAlert,
                maxLines = 1,
                modifier = Modifier.graphicsLayer {
                    translationY = -(actionProgress * 8f).dp.toPx()
                }
            )
        } else if (energyReady) {
            Text(
                text = "⚡ ULT",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WarmGold,
                maxLines = 1
            )
        } else {
            Spacer(Modifier.height(13.dp))
        }

        if (unit.isAlly) {
            HeroPortraitArt(
                heroId = unit.id,
                heroClass = unit.heroClass,
                rarity = unit.rarity,
                accentColor = accent,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(9.dp))
            )
        } else {
            EnemyMonsterPortrait(
                unitId = unit.id,
                accentColor = accent,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(9.dp))
            )
        }

        Text(
            text = if (str.isRtl) unit.nameAr else unit.nameEn,
            fontSize = 8.5.sp,
            color = if (unit.hp > 0) FrostWhite else MistText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        LinearProgressIndicator(
            progress = { hpFraction },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = if (unit.isAlly) EmeraldSafe else CrimsonAlert,
            trackColor = SlateAbyss
        )
        LinearProgressIndicator(
            progress = { (unit.energy / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(3.5.dp).clip(RoundedCornerShape(2.dp)),
            color = WarmGold,
            trackColor = SlateAbyss
        )
    }
}

