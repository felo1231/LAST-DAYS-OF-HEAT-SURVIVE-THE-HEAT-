package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameCatalog
import com.example.localization.LocalAppStrings
import com.example.model.AppLang
import com.example.model.CloudSyncStatus
import com.example.model.FlyResourceType
import com.example.model.GameState
import com.example.model.ResourceFlyEvent
import com.example.ui.art.FramedHeroAvatar
import com.example.ui.art.ResIconType
import com.example.ui.art.ResourceVectorIcon
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun TopHudBar(
    state: GameState,
    syncStatus: CloudSyncStatus = CloudSyncStatus.IDLE,
    activeFlyEvents: List<ResourceFlyEvent> = emptyList(),
    onToggleLang: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val str = LocalAppStrings.current
    val rates = GameCatalog.computeRatesPerMinute(state)
    val campTemp = GameCatalog.computeCampTempC(state)
    val power = GameCatalog.computeTotalPower(state)

    val pulse = rememberInfiniteTransition(label = "hudPulse")
    val stormAlpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "stormAlpha"
    )
    val tempBg by animateColorAsState(
        targetValue = if (state.blizzard.isActive) CrimsonAlert.copy(alpha = 0.28f * stormAlpha)
        else if (campTemp < 0) Color(0xFF0C2340).copy(alpha = 0.85f)
        else Color(0xFF2B1810).copy(alpha = 0.85f),
        label = "tempBg"
    )

    val hours = state.dayTimeMinutes / 60
    val mins = state.dayTimeMinutes % 60
    val timeText = String.format("%02d:%02d", hours, mins)
    val stormSec = if (state.blizzard.isActive) state.blizzard.activeRemainingSec else state.blizzard.countdownSec
    val stormTimerText = String.format("%02d:%02d", stormSec / 60, stormSec % 60)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(SlateAbyss.copy(alpha = 0.98f), SlateSurface.copy(alpha = 0.95f))
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // Row 1: Commander Avatar + Golden Power Badge + Temp/Khamaseen + Shop & Utility Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(end = 4.dp)
            ) {
                // Clickable 3D Commander Avatar + Alliance Tag Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCard.copy(alpha = 0.9f))
                        .border(1.2.dp, WarmGold.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                        .clickable { onOpenProfile() }
                        .testTag("open_profile_button")
                        .padding(start = 2.dp, end = if (state.allianceTag.isNotBlank()) 6.dp else 2.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FramedHeroAvatar(
                        heroId = state.avatarHeroId,
                        frameStyle = state.frameStyle,
                        size = 32.dp
                    )
                    if (state.allianceTag.isNotBlank()) {
                        Text(
                            text = "[${state.allianceTag}]",
                            fontSize = 9.sp,
                            color = WarmGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF4A2E0B), Color(0xFF7C4E12), SlateCard)
                            )
                        )
                        .border(1.4.dp, WarmGold, RoundedCornerShape(14.dp))
                        .clickable { onOpenLeaderboard() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ResourceVectorIcon(ResIconType.POWER, size = 18.dp)
                    Column {
                        Text(
                            text = str.power,
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            color = PapyrusCream
                        )
                        Text(
                            text = formatCompact(power),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = WarmGold
                        )
                    }
                }

                // Temperature & Khamaseen Storm Timer Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(tempBg)
                        .border(
                            1.2.dp,
                            if (state.blizzard.isActive) CrimsonAlert else IceCyan.copy(alpha = 0.55f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = if (state.blizzard.isActive) Icons.Default.AcUnit else Icons.Default.Thermostat,
                        contentDescription = str.campTemp,
                        tint = if (state.blizzard.isActive) CrimsonAlert else IceGlow,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${campTemp}°C",
                        style = MaterialTheme.typography.labelSmall,
                        color = FrostWhite
                    )
                    Text(
                        text = if (state.blizzard.isActive) "❄$stormTimerText" else "⏱$stormTimerText",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.blizzard.isActive) CrimsonAlert else IceCyan
                    )
                }

                Text(
                    text = "D${state.dayNumber} $timeText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MistText,
                    maxLines = 1
                )
            }

            // Right utility buttons (Royal Shop, Leaderboard, AR/EN, Sound, Cloud Settings)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                IconButton(
                    onClick = onOpenShop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF12363A))
                        .border(1.2.dp, NileTurquoise, CircleShape)
                        .testTag("open_shop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = str.shopTitle,
                        tint = WarmGold,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = onOpenLeaderboard,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3A2710))
                        .border(1.2.dp, WarmGold.copy(alpha = 0.85f), CircleShape)
                        .testTag("open_leaderboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = str.leaderboardTitle,
                        tint = WarmGold,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlateCard)
                        .border(1.dp, IceCyan.copy(alpha = 0.50f), RoundedCornerShape(10.dp))
                        .clickable { onToggleLang() }
                        .testTag("toggle_lang_button")
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.language == AppLang.AR) "EN" else "عربي",
                        style = MaterialTheme.typography.labelSmall,
                        color = IceGlow,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SlateCard)
                        .border(1.dp, SlateBorder, CircleShape)
                        .testTag("toggle_sound_button")
                ) {
                    Icon(
                        imageVector = if (state.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Sound",
                        tint = if (state.soundEnabled) IceCyan else MistText,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SlateCard)
                        .border(
                            1.dp,
                            when (syncStatus) {
                                CloudSyncStatus.SYNCED -> EmeraldSafe
                                CloudSyncStatus.SYNCING -> WarmGold
                                CloudSyncStatus.ERROR -> CrimsonAlert
                                CloudSyncStatus.IDLE -> SlateBorder
                            },
                            CircleShape
                        )
                        .testTag("open_settings_button")
                ) {
                    Icon(
                        imageVector = when (syncStatus) {
                            CloudSyncStatus.SYNCED -> Icons.Default.CloudDone
                            CloudSyncStatus.SYNCING -> Icons.Default.CloudSync
                            CloudSyncStatus.ERROR -> Icons.Default.CloudOff
                            CloudSyncStatus.IDLE -> Icons.Default.Save
                        },
                        contentDescription = str.settingsBackup,
                        tint = when (syncStatus) {
                            CloudSyncStatus.SYNCED -> EmeraldSafe
                            CloudSyncStatus.SYNCING -> WarmGold
                            CloudSyncStatus.ERROR -> CrimsonAlert
                            CloudSyncStatus.IDLE -> FrostWhite
                        },
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // Row 2: Rounded Dark-Lapis Glassmorphic Resource Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val meatEv = activeFlyEvents.lastOrNull { it.type == FlyResourceType.MEAT }
            val woodEv = activeFlyEvents.lastOrNull { it.type == FlyResourceType.WOOD }
            val coalEv = activeFlyEvents.lastOrNull { it.type == FlyResourceType.COAL }
            val ironEv = activeFlyEvents.lastOrNull { it.type == FlyResourceType.IRON }
            val gemsEv = activeFlyEvents.lastOrNull { it.type == FlyResourceType.GEMS }

            GlassResourcePill(ResIconType.MEAT, str.meat, state.resources.meat.roundToInt(), rates.meat.roundToInt(), meatEv != null, meatEv?.isConsumption == true)
            GlassResourcePill(ResIconType.WOOD, str.wood, state.resources.wood.roundToInt(), rates.wood.roundToInt(), woodEv != null, woodEv?.isConsumption == true)
            GlassResourcePill(ResIconType.COAL, str.coal, state.resources.coal.roundToInt(), rates.coal.roundToInt(), coalEv != null, coalEv?.isConsumption == true)
            GlassResourcePill(ResIconType.IRON, str.iron, state.resources.iron.roundToInt(), rates.iron.roundToInt(), ironEv != null, ironEv?.isConsumption == true)
            GlassResourcePill(ResIconType.GEMS, str.gems, state.resources.gems, null, gemsEv != null, gemsEv?.isConsumption == true)
            GlassResourcePill(ResIconType.MEALS, str.meals, state.resources.meals.roundToInt(), null)
            GlassResourcePill(ResIconType.SPEEDUP, str.speedups, state.resources.speedupsMin, null)
        }
    }
}

@Composable
private fun GlassResourcePill(
    iconType: ResIconType,
    label: String,
    amount: Int,
    ratePerMin: Int?,
    isPulsing: Boolean = false,
    isConsuming: Boolean = false
) {
    val pillScale by animateFloatAsState(
        targetValue = if (isPulsing) 1.11f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pillPulse"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isPulsing && isConsuming -> CrimsonAlert
            isPulsing -> EmeraldSafe
            else -> WarmGold.copy(alpha = 0.28f)
        },
        label = "pillBorder"
    )

    Row(
        modifier = Modifier
            .scale(pillScale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF13243B).copy(alpha = 0.92f), Color(0xFF0A1423).copy(alpha = 0.95f))
                )
            )
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        ResourceVectorIcon(type = iconType, size = 16.dp)
        Text(
            text = formatCompact(amount),
            style = MaterialTheme.typography.labelSmall,
            color = FrostWhite
        )
        if (ratePerMin != null) {
            val sign = if (ratePerMin >= 0) "+" else ""
            Text(
                text = "$sign$ratePerMin/m",
                fontSize = 9.sp,
                fontFamily = JetBrainsMonoFamily,
                color = if (ratePerMin >= 0) EmeraldSafe else CrimsonAlert
            )
        }
    }
}

fun formatCompact(value: Int): String = when {
    value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000f)
    value >= 10_000 -> String.format("%.1fK", value / 1_000f)
    else -> value.toString()
}
