package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.localization.LocalAppStrings
import com.example.model.HeroClass
import com.example.model.HeroRarity
import com.example.model.LeaderboardCategory
import com.example.model.LeaderboardEntry
import com.example.model.UiState
import com.example.ui.art.HeroPortraitArt
import com.example.ui.theme.*

@Composable
fun LeaderboardModal(
    leaderboardState: UiState<List<LeaderboardEntry>>,
    currentUserId: String,
    currentChiefName: String,
    onUpdateChiefName: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val str = LocalAppStrings.current
    var editedName by remember(currentChiefName) { mutableStateOf(currentChiefName) }
    var selectedCategory by remember { mutableStateOf(LeaderboardCategory.POWER) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .heightIn(max = 650.dp),
            shape = RoundedCornerShape(22.dp),
            color = SlateAbyss,
            border = androidx.compose.foundation.BorderStroke(1.8.dp, WarmGold.copy(alpha = 0.80f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(SlateSurface, SlateCard, SlateSurface))
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = WarmGold)
                        Text(
                            text = str.leaderboardTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = WarmGold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = str.close, tint = MistText)
                    }
                }

                // Category Selector Tabs: Power | Development & Furnace | Exploration Stage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LeaderboardCategoryTab(
                        label = str.leaderboardTabPower,
                        icon = Icons.Default.Bolt,
                        selected = selectedCategory == LeaderboardCategory.POWER,
                        accent = WarmGold,
                        tag = "leaderboard_tab_power",
                        onClick = { selectedCategory = LeaderboardCategory.POWER },
                        modifier = Modifier.weight(1f)
                    )
                    LeaderboardCategoryTab(
                        label = str.leaderboardTabDevelopment,
                        icon = Icons.Default.Apartment,
                        selected = selectedCategory == LeaderboardCategory.DEVELOPMENT,
                        accent = NileTurquoise,
                        tag = "leaderboard_tab_development",
                        onClick = { selectedCategory = LeaderboardCategory.DEVELOPMENT },
                        modifier = Modifier.weight(1f)
                    )
                    LeaderboardCategoryTab(
                        label = str.leaderboardTabExploration,
                        icon = Icons.Default.Explore,
                        selected = selectedCategory == LeaderboardCategory.EXPLORATION,
                        accent = IceCyan,
                        tag = "leaderboard_tab_exploration",
                        onClick = { selectedCategory = LeaderboardCategory.EXPLORATION },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Editable Chief Alias bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateCard)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { if (it.length <= 40) editedName = it },
                        label = { Text(str.chiefNameLabel, fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chief_name_input")
                    )
                    Button(
                        onClick = { onUpdateChiefName(editedName) },
                        enabled = editedName.trim().isNotEmpty(),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("save_chief_name_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = SlateAbyss)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = str.saveName, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(str.saveName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = SlateBorder)

                when (leaderboardState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = WarmGold)
                        }
                    }
                    is UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CrimsonAlert.copy(alpha = 0.16f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonAlert)
                            ) {
                                Text(
                                    text = leaderboardState.message,
                                    color = CrimsonAlert,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                    is UiState.Success -> {
                        val sortedEntries = remember(leaderboardState.data, selectedCategory) {
                            when (selectedCategory) {
                                LeaderboardCategory.POWER -> leaderboardState.data.sortedWith(
                                    compareByDescending<LeaderboardEntry> { it.totalPower }
                                        .thenByDescending { it.effectiveDevelopmentScore() }
                                )
                                LeaderboardCategory.DEVELOPMENT -> leaderboardState.data.sortedWith(
                                    compareByDescending<LeaderboardEntry> { it.effectiveDevelopmentScore() }
                                        .thenByDescending { it.furnaceLevel }
                                        .thenByDescending { it.totalPower }
                                )
                                LeaderboardCategory.EXPLORATION -> leaderboardState.data.sortedWith(
                                    compareByDescending<LeaderboardEntry> { it.explorationStage }
                                        .thenByDescending { it.chapter }
                                        .thenByDescending { it.totalPower }
                                )
                            }
                        }

                        if (sortedEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(str.noLeaderboardYet, color = MistText, style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(sortedEntries, key = { idx, item -> item.userId.ifBlank { "row_$idx" } }) { index, entry ->
                                    val isMe = entry.userId == currentUserId
                                    val rank = index + 1
                                    val rankColor = when (rank) {
                                        1 -> WarmGold
                                        2 -> IceGlow
                                        3 -> FurnaceAmber
                                        else -> IceCyan
                                    }

                                    val metricValue = when (selectedCategory) {
                                        LeaderboardCategory.POWER -> formatCompact(entry.totalPower.toInt())
                                        LeaderboardCategory.DEVELOPMENT -> formatCompact(entry.effectiveDevelopmentScore().toInt())
                                        LeaderboardCategory.EXPLORATION -> "${str.stage} ${entry.explorationStage}"
                                    }
                                    val metricLabel = when (selectedCategory) {
                                        LeaderboardCategory.POWER -> str.power
                                        LeaderboardCategory.DEVELOPMENT -> str.developmentScoreLabel
                                        LeaderboardCategory.EXPLORATION -> "${str.chapter} ${entry.chapter}"
                                    }
                                    val metricColor = when (selectedCategory) {
                                        LeaderboardCategory.POWER -> WarmGold
                                        LeaderboardCategory.DEVELOPMENT -> NileTurquoise
                                        LeaderboardCategory.EXPLORATION -> IceCyan
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isMe) SlateElevated else SlateCard
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isMe || rank <= 3) 1.6.dp else 1.dp,
                                            color = if (isMe) WarmGold else if (rank <= 3) rankColor.copy(alpha = 0.65f) else SlateBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(rankColor.copy(alpha = 0.18f))
                                                    .border(1.5.dp, rankColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "#$rank",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = rankColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            HeroPortraitArt(
                                                heroId = entry.avatarHeroId.ifBlank { "h_sergey" },
                                                heroClass = HeroClass.INFANTRY,
                                                rarity = HeroRarity.MYTHIC,
                                                accentColor = rankColor,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .border(1.2.dp, rankColor, CircleShape)
                                            )

                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    if (entry.allianceTag.isNotBlank()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(5.dp))
                                                                .background(NileTurquoise.copy(alpha = 0.2f))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text("[${entry.allianceTag}]", fontSize = 9.sp, color = NileTurquoise, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    Text(
                                                        text = entry.chiefName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = FrostWhite,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (isMe) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(EmeraldSafe.copy(alpha = 0.2f))
                                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(str.youTag, fontSize = 9.sp, color = EmeraldSafe, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "${str.furnace} ${str.levelShort}.${entry.furnaceLevel}  •  ${str.chapter} ${entry.chapter}  •  ${str.stage} ${entry.explorationStage}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MistText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = metricValue,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = metricColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = metricLabel,
                                                    fontSize = 9.sp,
                                                    color = MistText
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
        }
    }
}

@Composable
private fun LeaderboardCategoryTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) accent.copy(alpha = 0.22f) else SlateCard
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else SlateBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag(tag)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accent else MistText,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent else MistText,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
