package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GameCatalog
import com.example.localization.LocalAppStrings
import com.example.model.*
import com.example.ui.art.FramedHeroAvatar
import com.example.ui.art.HeroPortraitArt
import com.example.ui.art.ResIconType
import com.example.ui.art.ResourceVectorIcon
import com.example.ui.theme.*

@Composable
fun LiveChatTickerBar(
    worldMessages: List<ChatMessage>,
    allianceMessages: List<ChatMessage>,
    allianceTag: String,
    onOpenChat: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val str = LocalAppStrings.current
    val latest = worldMessages.lastOrNull() ?: allianceMessages.lastOrNull()
    val preview = if (latest != null) {
        val tagPrefix = if (latest.allianceTag.isNotBlank()) "[${latest.allianceTag}] " else ""
        "$tagPrefix${latest.senderName}: ${latest.text}"
    } else {
        str.chatEmpty
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 46.dp)
            .background(
                Brush.horizontalGradient(listOf(SlateAbyss, SlateSurface, SlateAbyss))
            )
            .border(1.dp, NileTurquoise.copy(alpha = 0.35f))
            .clickable { onOpenChat(0) }
            .testTag("live_chat_ticker")
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(EmeraldSafe)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(NileTurquoise.copy(alpha = 0.18f))
                .border(1.dp, NileTurquoise.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = str.chatWorldTab,
                fontSize = 10.sp,
                color = IceGlow,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = preview,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 12.sp,
            color = FrostWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (allianceTag.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(WarmGold.copy(alpha = 0.18f))
                    .border(1.dp, WarmGold.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .clickable { onOpenChat(1) }
                    .testTag("ticker_alliance_pill")
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text("[$allianceTag]", fontSize = 10.sp, color = WarmGold, fontWeight = FontWeight.Bold)
            }
        }
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = str.chatWorldTab,
            tint = WarmGold,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun WorldAndAllianceChatModal(
    state: GameState,
    initialTab: Int,
    worldMessages: List<ChatMessage>,
    allianceMessages: List<ChatMessage>,
    onSendWorld: (String) -> Unit,
    onSendAlliance: (String) -> Unit,
    onOpenAllianceHub: () -> Unit,
    onDismiss: () -> Unit
) {
    val str = LocalAppStrings.current
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var draft by remember { mutableStateOf("") }
    val messages = if (activeTab == 0) worldMessages else allianceMessages

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .heightIn(min = 440.dp, max = 640.dp),
            shape = RoundedCornerShape(22.dp),
            color = SlateAbyss,
            border = androidx.compose.foundation.BorderStroke(1.8.dp, NileTurquoise.copy(alpha = 0.8f))
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Forum, contentDescription = null, tint = NileTurquoise)
                        Text(
                            text = if (activeTab == 0) str.chatWorldTab else str.chatAllianceTab,
                            style = MaterialTheme.typography.titleMedium,
                            color = WarmGold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = str.close, tint = MistText)
                    }
                }

                TabRow(selectedTabIndex = activeTab, containerColor = SlateSurface, contentColor = WarmGold) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.testTag("chat_tab_world"),
                        text = { Text(str.chatWorldTab, maxLines = 1) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier.testTag("chat_tab_alliance"),
                        text = {
                            val label = if (state.allianceTag.isNotBlank()) "${str.chatAllianceTab} [${state.allianceTag}]" else str.chatAllianceTab
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    )
                }

                if (activeTab == 1 && state.allianceId.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = WarmGold, modifier = Modifier.size(44.dp))
                            Text(str.chatJoinAllianceHint, color = FrostWhite, style = MaterialTheme.typography.bodyLarge)
                            Button(
                                onClick = {
                                    onDismiss()
                                    onOpenAllianceHub()
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
                            ) {
                                Text(str.allianceTitle, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (messages.isEmpty()) {
                            item {
                                Text(
                                    text = str.chatEmpty,
                                    color = MistText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            items(messages, key = { it.id.ifBlank { "${it.senderUid}_${it.sentEpochMs}" } }) { msg ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        HeroPortraitArt(
                                            heroId = msg.avatarHeroId.ifBlank { "h_sergey" },
                                            heroClass = HeroClass.INFANTRY,
                                            rarity = HeroRarity.MYTHIC,
                                            accentColor = WarmGold,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .border(1.2.dp, WarmGold, CircleShape)
                                        )
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (msg.allianceTag.isNotBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(5.dp))
                                                            .background(WarmGold.copy(alpha = 0.20f))
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("[${msg.allianceTag}]", fontSize = 9.sp, color = WarmGold, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Text(
                                                    text = msg.senderName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = IceGlow,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.weight(1f))
                                                Text(
                                                    text = formatRelativeTime(msg.sentEpochMs),
                                                    fontSize = 10.sp,
                                                    color = MistText
                                                )
                                            }
                                            Text(
                                                text = msg.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = FrostWhite
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateSurface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { if (it.length <= 240) draft = it },
                            placeholder = { Text(str.chatInputHint, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )
                        FilledIconButton(
                            onClick = {
                                val text = draft.trim()
                                if (text.isNotEmpty()) {
                                    if (activeTab == 0) onSendWorld(text) else onSendAlliance(text)
                                    draft = ""
                                }
                            },
                            enabled = draft.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_send_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = WarmGold,
                                contentColor = SlateAbyss
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = str.chatSend)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommanderProfileModal(
    state: GameState,
    chiefName: String,
    onSaveProfile: (String, String, String) -> Unit,
    onOpenAllianceModal: () -> Unit,
    onDismiss: () -> Unit
) {
    val str = LocalAppStrings.current
    var editedName by remember(chiefName) { mutableStateOf(chiefName) }
    var selectedAvatar by remember(state.avatarHeroId) { mutableStateOf(state.avatarHeroId) }
    var selectedFrame by remember(state.frameStyle) { mutableStateOf(state.frameStyle) }

    val totalPower = GameCatalog.computeTotalPower(state)
    val devScore = GameCatalog.computeDevelopmentScore(state)
    val totalTroops = state.troops.infantry + state.troops.lancers + state.troops.marksmen
    val frameOptions = listOf(
        "ROYAL_GOLD" to if (str.isRtl) "ذهب ملكي" else "Royal Gold",
        "NILE_TURQUOISE" to if (str.isRtl) "فيروز النيل" else "Nile Turquoise",
        "MYTHIC_SOLAR" to if (str.isRtl) "الشمس الأسطورية" else "Solar Mythic",
        "LAPIS_IMPERIAL" to if (str.isRtl) "لازوردي إمبراطوري" else "Lapis Imperial"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .heightIn(max = 660.dp),
            shape = RoundedCornerShape(22.dp),
            color = SlateAbyss,
            border = androidx.compose.foundation.BorderStroke(1.8.dp, WarmGold.copy(alpha = 0.85f))
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = WarmGold)
                        Text(str.profileTitle, style = MaterialTheme.typography.titleMedium, color = WarmGold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = str.close, tint = MistText)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Commander Banner Preview
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = androidx.compose.foundation.BorderStroke(1.4.dp, WarmGold.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FramedHeroAvatar(
                                heroId = selectedAvatar,
                                frameStyle = selectedFrame,
                                size = 72.dp
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (state.allianceTag.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(WarmGold.copy(alpha = 0.20f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("[${state.allianceTag}]", fontSize = 11.sp, color = WarmGold, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(
                                        text = editedName.ifBlank { chiefName },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = FrostWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "⚔ ${str.power}: $totalPower  •  ${str.furnace} ${str.levelShort}.${state.furnace.level}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IceGlow
                                )
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onOpenAllianceModal()
                                    },
                                    modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = if (state.allianceTag.isNotBlank()) "${str.allianceTitle} [${state.allianceTag}]" else str.allianceTitle,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Name Input
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { if (it.length <= 40) editedName = it },
                        label = { Text(str.chiefNameLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    // 8 Royal King/Queen 3D Portraits Selector
                    Text(str.profileSelectAvatar, style = MaterialTheme.typography.titleSmall, color = WarmGold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.heroes, key = { it.id }) { hero ->
                            val isSelected = selectedAvatar == hero.id
                            Column(
                                modifier = Modifier
                                    .width(74.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SlateElevated else SlateCard)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) WarmGold else SlateBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedAvatar = hero.id }
                                    .testTag("profile_avatar_${hero.id}")
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                HeroPortraitArt(
                                    heroId = hero.id,
                                    heroClass = hero.heroClass,
                                    rarity = hero.rarity,
                                    accentColor = Color(hero.accentColorHex),
                                    modifier = Modifier.size(48.dp).clip(CircleShape)
                                )
                                Text(
                                    text = if (str.isRtl) hero.nameAr else hero.nameEn,
                                    fontSize = 9.sp,
                                    color = if (isSelected) WarmGold else FrostWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Frame Selector
                    Text(str.profileSelectFrame, style = MaterialTheme.typography.titleSmall, color = NileTurquoise)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(frameOptions, key = { it.first }) { (key, label) ->
                            FilterChip(
                                selected = selectedFrame == key,
                                onClick = { selectedFrame = key },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Career Stats
                    Text(str.profileStats, style = MaterialTheme.typography.titleSmall, color = IceGlow)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ProfileStatRow(str.power, formatCompact(totalPower), WarmGold)
                            ProfileStatRow(str.developmentScoreLabel, formatCompact(devScore), NileTurquoise)
                            ProfileStatRow("${str.furnace} / ${str.stage}", "${str.levelShort}.${state.furnace.level}  •  ${str.stage} ${state.explorationStage}", IceCyan)
                            ProfileStatRow(str.trainTroops, "$totalTroops", EmeraldSafe)
                        }
                    }

                    Button(
                        onClick = {
                            onSaveProfile(editedName, selectedAvatar, selectedFrame)
                            onDismiss()
                        },
                        enabled = editedName.trim().isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("profile_save_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(str.saveName, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MistText)
        Text(value, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RoyalShopModal(
    state: GameState,
    onClaimDailyGift: () -> Unit,
    onBuyItem: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val str = LocalAppStrings.current
    var category by remember { mutableStateOf(ShopCategory.SPEEDUPS_KEYS) }
    val now = remember(state.dailyGiftLastEpochMs) { System.currentTimeMillis() }
    val cooldownMs = 24L * 3600L * 1000L
    val giftReady = now - state.dailyGiftLastEpochMs >= cooldownMs
    val items = remember(category) { GameCatalog.shopCatalog.filter { it.category == category } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .heightIn(max = 660.dp),
            shape = RoundedCornerShape(22.dp),
            color = SlateAbyss,
            border = androidx.compose.foundation.BorderStroke(1.8.dp, WarmGold.copy(alpha = 0.85f))
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = WarmGold)
                        Text(str.shopTitle, style = MaterialTheme.typography.titleMedium, color = WarmGold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SlateCard)
                                .border(1.dp, IceCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ResourceVectorIcon(ResIconType.GEMS, size = 15.dp)
                            Text("${state.resources.gems}", style = MaterialTheme.typography.labelMedium, color = FrostWhite)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = str.close, tint = MistText)
                        }
                    }
                }

                // Daily Free Royal Gift Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF23190B)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (giftReady) WarmGold else SlateBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(str.shopDailyFree, style = MaterialTheme.typography.titleSmall, color = WarmGold)
                            Text(
                                text = "+120 💎  •  +1 🔑  •  +10m ⚡  •  +350 XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = FrostWhite
                            )
                        }
                        Button(
                            onClick = onClaimDailyGift,
                            enabled = giftReady,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .testTag("shop_daily_gift_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (giftReady) EmeraldSafe else SlateElevated,
                                contentColor = if (giftReady) SlateAbyss else MistText
                            )
                        ) {
                            Text(if (giftReady) str.shopDailyClaim else str.claimed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                TabRow(selectedTabIndex = category.ordinal, containerColor = SlateSurface, contentColor = WarmGold) {
                    Tab(
                        selected = category == ShopCategory.SPEEDUPS_KEYS,
                        onClick = { category = ShopCategory.SPEEDUPS_KEYS },
                        text = { Text(str.shopTabSpeedups, maxLines = 1, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = category == ShopCategory.RESOURCES,
                        onClick = { category = ShopCategory.RESOURCES },
                        text = { Text(str.shopTabResources, maxLines = 1, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = category == ShopCategory.BUNDLES,
                        onClick = { category = ShopCategory.BUNDLES },
                        text = { Text(str.shopTabBundles, maxLines = 1, fontSize = 11.sp) }
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        val accent = Color(item.badgeColorHex)
                        val canAfford = state.resources.gems >= item.gemCost
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, accent.copy(alpha = 0.65f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(accent.copy(alpha = 0.18f))
                                        .border(1.dp, accent, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (item.category) {
                                            ShopCategory.SPEEDUPS_KEYS -> Icons.Default.VpnKey
                                            ShopCategory.RESOURCES -> Icons.Default.Inventory2
                                            ShopCategory.BUNDLES -> Icons.Default.CardGiftcard
                                        },
                                        contentDescription = null,
                                        tint = accent
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = if (str.isRtl) item.titleAr else item.titleEn,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = FrostWhite
                                    )
                                    Text(
                                        text = if (str.isRtl) item.descAr else item.descEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 12.sp,
                                        color = MistText
                                    )
                                }
                                Button(
                                    onClick = { onBuyItem(item.id) },
                                    enabled = canAfford,
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 46.dp)
                                        .testTag("shop_buy_${item.id}"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canAfford) WarmGold else SlateElevated,
                                        contentColor = if (canAfford) SlateAbyss else MistText
                                    )
                                ) {
                                    Text("${item.gemCost} 💎", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
fun AllianceHubModal(
    state: GameState,
    alliances: List<AllianceInfo>,
    onCreateAlliance: (String, String, String, Long) -> Unit,
    onJoinAlliance: (AllianceInfo) -> Unit,
    onLeaveAlliance: () -> Unit,
    onOpenAllianceChat: () -> Unit,
    onDismiss: () -> Unit
) {
    val str = LocalAppStrings.current
    val myAlliance = alliances.firstOrNull { it.allianceId == state.allianceId }
    var nameInput by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    val bannerSwatches = listOf(0xFFF6C453L, 0xFF2EC4B6L, 0xFFC084FCL, 0xFFF97316L)
    var selectedBanner by remember { mutableLongStateOf(bannerSwatches.first()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .heightIn(max = 660.dp),
            shape = RoundedCornerShape(22.dp),
            color = SlateAbyss,
            border = androidx.compose.foundation.BorderStroke(1.8.dp, NileTurquoise.copy(alpha = 0.85f))
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = NileTurquoise)
                        Text(str.allianceTitle, style = MaterialTheme.typography.titleMedium, color = WarmGold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = str.close, tint = MistText)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (myAlliance != null) {
                        item {
                            val accent = Color(myAlliance.bannerColorHex)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = androidx.compose.foundation.BorderStroke(1.8.dp, accent)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(accent.copy(alpha = 0.22f))
                                                .border(1.dp, accent, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("[${myAlliance.tag}]", color = accent, fontWeight = FontWeight.Bold)
                                        }
                                        Text(myAlliance.name, style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                                    }
                                    if (myAlliance.description.isNotBlank()) {
                                        Text(myAlliance.description, style = MaterialTheme.typography.bodyMedium, color = MistText)
                                    }
                                    Text(
                                        text = "${str.allianceMembers}: ${myAlliance.memberCount}  •  ⚔ ${formatCompact(myAlliance.totalPower.toInt())}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = IceGlow
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onOpenAllianceChat()
                                            },
                                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = NileTurquoise, contentColor = SlateAbyss)
                                        ) {
                                            Text(str.chatAllianceTab, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = onLeaveAlliance,
                                            modifier = Modifier.defaultMinSize(minHeight = 44.dp).testTag("alliance_leave_button")
                                        ) {
                                            Text(str.allianceLeave, color = CrimsonAlert)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = androidx.compose.foundation.BorderStroke(1.4.dp, WarmGold.copy(alpha = 0.65f))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(str.allianceCreate, style = MaterialTheme.typography.titleSmall, color = WarmGold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = nameInput,
                                            onValueChange = { if (it.length <= 32) nameInput = it },
                                            label = { Text(str.allianceNameLabel, fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("alliance_name_input")
                                        )
                                        OutlinedTextField(
                                            value = tagInput,
                                            onValueChange = { if (it.length <= 5) tagInput = it.uppercase() },
                                            label = { Text(str.allianceTagLabel, fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.width(105.dp).testTag("alliance_tag_input")
                                        )
                                    }
                                    OutlinedTextField(
                                        value = descInput,
                                        onValueChange = { if (it.length <= 120) descInput = it },
                                        label = { Text(str.allianceDescLabel, fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        bannerSwatches.forEach { hex ->
                                            val c = Color(hex)
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(c)
                                                    .border(
                                                        width = if (selectedBanner == hex) 2.5.dp else 1.dp,
                                                        color = if (selectedBanner == hex) FrostWhite else SlateBorder,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { selectedBanner = hex }
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Button(
                                            onClick = { onCreateAlliance(nameInput, tagInput, descInput, selectedBanner) },
                                            enabled = nameInput.trim().length >= 2 && tagInput.trim().length >= 2,
                                            modifier = Modifier.defaultMinSize(minHeight = 44.dp).testTag("alliance_create_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
                                        ) {
                                            Text(str.allianceCreate, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (alliances.isEmpty()) {
                        item {
                            Text(str.allianceNoAlliance, color = MistText, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        items(alliances, key = { it.allianceId.ifBlank { it.id } }) { alliance ->
                            val isMember = alliance.allianceId == state.allianceId
                            val accent = Color(alliance.bannerColorHex)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isMember) accent else SlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(accent.copy(alpha = 0.22f))
                                            .border(1.dp, accent, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text("[${alliance.tag}]", color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(alliance.name, style = MaterialTheme.typography.titleSmall, color = FrostWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            text = "${str.allianceMembers}: ${alliance.memberCount}  •  ⚔ ${formatCompact(alliance.totalPower.toInt())}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = IceGlow
                                        )
                                    }
                                    if (!isMember) {
                                        Button(
                                            onClick = { onJoinAlliance(alliance) },
                                            modifier = Modifier.defaultMinSize(minHeight = 42.dp).testTag("alliance_join_${alliance.allianceId}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = NileTurquoise, contentColor = SlateAbyss)
                                        ) {
                                            Text(str.allianceJoin, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
}

private fun formatRelativeTime(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val diffSec = ((System.currentTimeMillis() - epochMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "now"
        diffSec < 3600L -> "${diffSec / 60L}m"
        diffSec < 86400L -> "${diffSec / 3600L}h"
        else -> "${diffSec / 86400L}d"
    }
}
