package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.CloudRepository
import com.example.data.SaveRepository
import com.example.localization.ArabicStrings
import com.example.localization.EnglishStrings
import com.example.localization.LocalAppStrings
import com.example.model.AppLang
import com.example.model.MainTab
import com.example.ui.components.*
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BackpackScreen
import com.example.ui.screens.ChapterProgressAndQuestsModal
import com.example.ui.screens.HeroesScreen
import com.example.ui.screens.TechScreen
import com.example.ui.screens.WorldMapScreen
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrostholdTheme {
                AppContent()
            }
        }
    }
}

@Composable
private fun AppContent(auth: FirebaseAuth = remember { FirebaseAuth.getInstance() }) {
    val appScope = rememberCoroutineScope()
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {}, authScope = appScope)
    } else {
        val uid = user.uid
        val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Pharaoh Chief"
        key(uid) {
            AuthenticatedGameHost(
                currentUserId = uid,
                initialDisplayName = displayName
            )
        }
    }
}

@Composable
private fun AuthenticatedGameHost(
    currentUserId: String,
    initialDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: GameViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val saveRepo = SaveRepository(app.applicationContext, currentUserId)
                val cloudRepo = CloudRepository(db)
                GameViewModel(
                    repository = saveRepo,
                    cloudRepository = cloudRepo,
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName
                )
            }
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.saveNow(syncCloud = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val worldCitadels by viewModel.worldCitadelsState.collectAsStateWithLifecycle()
    val alliances by viewModel.alliancesState.collectAsStateWithLifecycle()
    val worldChat by viewModel.worldChatState.collectAsStateWithLifecycle()
    val allianceChat by viewModel.allianceChatState.collectAsStateWithLifecycle()

    val strings = if (gameState.language == AppLang.AR) ArabicStrings else EnglishStrings
    val layoutDir = if (strings.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.bannerMessageEn, uiState.bannerMessageAr, gameState.language) {
        val msg = if (strings.isRtl) uiState.bannerMessageAr else uiState.bannerMessageEn
        if (msg != null) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearBanner()
        }
    }

    if (uiState.activeTab != MainTab.CAMP) {
        BackHandler { viewModel.selectTab(MainTab.CAMP) }
    }

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalLayoutDirection provides layoutDir
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateAbyss)
        ) {
            val isWideScreen = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = SlateAbyss,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopHudBar(
                        state = gameState,
                        syncStatus = uiState.syncStatus,
                        activeFlyEvents = uiState.resourceFlyEvents,
                        onToggleLang = viewModel::toggleLanguage,
                        onToggleSound = viewModel::toggleSound,
                        onOpenProfile = { viewModel.setProfileModal(true) },
                        onOpenShop = { viewModel.setShopModal(true) },
                        onOpenLeaderboard = { viewModel.setLeaderboardModal(true) },
                        onOpenSettings = { viewModel.setSettingsModal(true) }
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LiveChatTickerBar(
                                worldMessages = worldChat,
                                allianceMessages = allianceChat,
                                allianceTag = gameState.allianceTag,
                                onOpenChat = { tab -> viewModel.setChatModal(true, tab) }
                            )
                            FrostholdStylizedDock(
                                activeTab = uiState.activeTab,
                                onSelectTab = viewModel::selectTab
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        FrostholdNavRail(
                            activeTab = uiState.activeTab,
                            onSelectTab = viewModel::selectTab
                        )
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (uiState.activeTab) {
                                MainTab.CAMP -> CampMapScreen(
                                    state = gameState,
                                    highlightedBuildingId = uiState.highlightedBuildingId,
                                    onTapFurnace = { viewModel.setFurnaceModal(true) },
                                    onToggleOverdrive = viewModel::toggleFurnaceOverdrive,
                                    onTapBuilding = viewModel::openBuilding,
                                    onOpenSurvivors = { viewModel.setSurvivorsModal(true) },
                                    onOpenOrders = { viewModel.setOrdersModal(true) },
                                    onGoToChapterMission = viewModel::goToChapterMission,
                                    onClaimChapterMission = viewModel::claimChapterMission,
                                    onClaimChapterChest = viewModel::claimChapterChest,
                                    onOpenQuestsModal = { viewModel.setQuestsModal(true) }
                                )
                                MainTab.WORLD -> WorldMapScreen(
                                    state = gameState,
                                    currentUserId = currentUserId,
                                    worldCitadels = worldCitadels,
                                    alliances = alliances,
                                    selectedNodeId = uiState.selectedWorldNodeId,
                                    onSelectNode = viewModel::selectWorldNode,
                                    onDispatchMarch = viewModel::dispatchMarch,
                                    onOpenAllianceModal = { viewModel.setAllianceModal(true) },
                                    onDispatchCitadelExpedition = viewModel::dispatchCitadelExpedition
                                )
                                MainTab.HEROES -> HeroesScreen(
                                    state = gameState,
                                    subTab = uiState.heroesSubTab,
                                    onSelectSubTab = viewModel::selectHeroesSubTab,
                                    lastGachaResult = uiState.lastGachaResult,
                                    battleSession = uiState.battleSession,
                                    onLevelUpHero = viewModel::levelUpHero,
                                    onPromoteStar = viewModel::promoteHeroStar,
                                    onUpgradeGear = viewModel::upgradeHeroGear,
                                    onToggleFormation = viewModel::toggleHeroInFormation,
                                    onPullGacha = viewModel::pullGacha,
                                    onDismissGacha = viewModel::dismissGachaResult,
                                    onStartBattle = viewModel::startExplorationBattle,
                                    onToggleAutoSkill = viewModel::toggleBattleAutoSkill,
                                    onTriggerSkill = viewModel::triggerManualHeroSkill,
                                    onFinishBattle = viewModel::finishBattle
                                )
                                MainTab.BACKPACK -> BackpackScreen(
                                    state = gameState,
                                    onOpenGachaTab = {
                                        viewModel.selectHeroesSubTab(1)
                                        viewModel.selectTab(MainTab.HEROES)
                                    },
                                    onOpenQuestsModal = { viewModel.setQuestsModal(true) },
                                    onOpenShopModal = { viewModel.setShopModal(true) }
                                )
                                MainTab.TECH -> TechScreen(
                                    state = gameState,
                                    onStartResearch = viewModel::startResearch,
                                    onSpeedupResearch = viewModel::speedupResearch
                                )
                            }
                        }
                        if (isWideScreen) {
                            LiveChatTickerBar(
                                worldMessages = worldChat,
                                allianceMessages = allianceChat,
                                allianceTag = gameState.allianceTag,
                                onOpenChat = { tab -> viewModel.setChatModal(true, tab) }
                            )
                        }
                    }
                }
            }

            // Modals
            if (uiState.showFurnaceModal) {
                FurnaceModal(
                    state = gameState,
                    onDismiss = { viewModel.setFurnaceModal(false) },
                    onToggleOverdrive = viewModel::toggleFurnaceOverdrive,
                    onUpgrade = viewModel::upgradeFurnace,
                    onSpeedup = viewModel::speedupFurnace
                )
            }

            uiState.selectedBuildingId?.let { bId ->
                gameState.buildings.firstOrNull { it.id == bId }?.let { slot ->
                    BuildingDetailModal(
                        slot = slot,
                        state = gameState,
                        onDismiss = { viewModel.openBuilding(null) },
                        onUpgrade = { viewModel.upgradeOrBuildSlot(slot.id) },
                        onSpeedup = { viewModel.speedupBuilding(slot.id) },
                        onAdjustWorkers = { delta -> viewModel.adjustWorkers(slot.id, delta) },
                        onUpgradeGear = { isHeater -> viewModel.upgradeBuildingEquipment(slot.id, isHeater) },
                        onSetMealQuality = viewModel::setCookhouseMealQuality,
                        onTrainTroops = viewModel::trainTroops,
                        onSpeedupTraining = viewModel::speedupTraining
                    )
                }
            }

            if (uiState.showSurvivorsModal) {
                SurvivorsModal(
                    state = gameState,
                    onDismiss = { viewModel.setSurvivorsModal(false) },
                    onRecruit = viewModel::recruitSurvivor,
                    onTreatAll = viewModel::treatAllSickSurvivors
                )
            }

            if (uiState.showOrdersModal) {
                ChiefOrdersModal(
                    state = gameState,
                    onDismiss = { viewModel.setOrdersModal(false) },
                    onEnact = viewModel::enactChiefOrder
                )
            }

            if (uiState.showQuestsModal) {
                ChapterProgressAndQuestsModal(
                    state = gameState,
                    onDismiss = { viewModel.setQuestsModal(false) },
                    onGoToMission = viewModel::goToChapterMission,
                    onClaimChapter = viewModel::claimChapterMission,
                    onClaimChapterChest = viewModel::claimChapterChest,
                    onClaimDaily = viewModel::claimDailyQuest,
                    onClaimMilestone = viewModel::claimMilestoneChest
                )
            }

            if (uiState.showLeaderboardModal) {
                LeaderboardModal(
                    leaderboardState = leaderboardState,
                    currentUserId = currentUserId,
                    currentChiefName = uiState.chiefName,
                    onUpdateChiefName = viewModel::updateChiefName,
                    onDismiss = { viewModel.setLeaderboardModal(false) }
                )
            }

            if (uiState.showSettingsModal) {
                SettingsBackupModal(
                    state = gameState,
                    chiefName = uiState.chiefName,
                    syncStatus = uiState.syncStatus,
                    onDismiss = { viewModel.setSettingsModal(false) },
                    onSyncCloudNow = { viewModel.syncCloudNow(silent = false) },
                    onSignOut = {
                        viewModel.saveNow(syncCloud = true)
                        signOutUser(credentialManager, scope)
                    },
                    onExportJson = viewModel::exportSaveJson,
                    onImportJson = viewModel::importSaveJson,
                    onResetGame = viewModel::resetGame
                )
            }

            if (uiState.showShopModal) {
                RoyalShopModal(
                    state = gameState,
                    onClaimDailyGift = viewModel::claimDailyRoyalGift,
                    onBuyItem = viewModel::buyShopItem,
                    onDismiss = { viewModel.setShopModal(false) }
                )
            }

            if (uiState.showProfileModal) {
                CommanderProfileModal(
                    state = gameState,
                    chiefName = uiState.chiefName,
                    onSaveProfile = viewModel::updateCommanderProfile,
                    onOpenAllianceModal = { viewModel.setAllianceModal(true) },
                    onDismiss = { viewModel.setProfileModal(false) }
                )
            }

            if (uiState.showChatModal) {
                WorldAndAllianceChatModal(
                    state = gameState,
                    initialTab = uiState.chatInitialTab,
                    worldMessages = worldChat,
                    allianceMessages = allianceChat,
                    onSendWorld = viewModel::sendWorldChat,
                    onSendAlliance = viewModel::sendAllianceChat,
                    onOpenAllianceHub = { viewModel.setAllianceModal(true) },
                    onDismiss = { viewModel.setChatModal(false) }
                )
            }

            if (uiState.showAllianceModal) {
                AllianceHubModal(
                    state = gameState,
                    alliances = alliances,
                    onCreateAlliance = viewModel::createAlliance,
                    onJoinAlliance = viewModel::joinAlliance,
                    onLeaveAlliance = viewModel::leaveCurrentAlliance,
                    onOpenAllianceChat = { viewModel.setChatModal(true, 1) },
                    onDismiss = { viewModel.setAllianceModal(false) }
                )
            }

            uiState.offlineSummary?.let { summary ->
                OfflineSummaryDialog(
                    summary = summary,
                    onCollect = viewModel::dismissOfflineSummary
                )
            }

            // Real-time resource collection & consumption flight overlay
            ResourceFlyOverlay(events = uiState.resourceFlyEvents)
        }
    }
}

private data class DockItem(
    val tab: MainTab,
    val label: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
private fun FrostholdStylizedDock(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    val str = LocalAppStrings.current
    val items = listOf(
        DockItem(MainTab.CAMP, str.tabCamp, Icons.Default.Whatshot, FurnaceOrange),
        DockItem(MainTab.WORLD, str.tabWorld, Icons.Default.Public, IceCyan),
        DockItem(MainTab.HEROES, str.tabHeroes, Icons.Default.Groups, WarmGold),
        DockItem(MainTab.BACKPACK, str.tabBackpack, Icons.Default.Inventory2, EmeraldSafe),
        DockItem(MainTab.TECH, str.tabTech, Icons.Default.Science, MythicPurple)
    )

    Surface(
        color = SlateAbyss,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WarmGold.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = activeTab == item.tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp)
                        .shadow(if (selected) 8.dp else 2.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                if (selected) listOf(item.accent.copy(alpha = 0.94f), item.accent.copy(alpha = 0.68f))
                                else listOf(SlateElevated, SlateSurface)
                            )
                        )
                        .border(
                            width = if (selected) 1.6.dp else 1.dp,
                            color = if (selected) FrostWhite.copy(alpha = 0.85f) else SlateBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectTab(item.tab) }
                        .testTag("nav_${item.tab.name.lowercase()}")
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) SlateAbyss else item.accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = item.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) SlateAbyss else FrostWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrostholdNavRail(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    val str = LocalAppStrings.current
    NavigationRail(
        containerColor = SlateAbyss,
        contentColor = FrostWhite
    ) {
        val items = listOf(
            Triple(MainTab.CAMP, str.tabCamp, Icons.Default.Whatshot),
            Triple(MainTab.WORLD, str.tabWorld, Icons.Default.Public),
            Triple(MainTab.HEROES, str.tabHeroes, Icons.Default.Groups),
            Triple(MainTab.BACKPACK, str.tabBackpack, Icons.Default.Inventory2),
            Triple(MainTab.TECH, str.tabTech, Icons.Default.Science)
        )
        items.forEach { (tab, label, icon) ->
            NavigationRailItem(
                selected = activeTab == tab,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = SlateAbyss,
                    selectedTextColor = WarmGold,
                    indicatorColor = WarmGold,
                    unselectedIconColor = MistText,
                    unselectedTextColor = MistText
                )
            )
        }
    }
}
