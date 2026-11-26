package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SfxType
import com.example.audio.SoundEffects
import com.example.data.CloudRepository
import com.example.data.GameCatalog
import com.example.data.ResourceCost
import com.example.data.SaveRepository
import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

data class UiModalState(
    val activeTab: MainTab = MainTab.CAMP,
    val heroesSubTab: Int = 0,
    val selectedBuildingId: String? = null,
    val highlightedBuildingId: String? = null,
    val showFurnaceModal: Boolean = false,
    val showSurvivorsModal: Boolean = false,
    val showOrdersModal: Boolean = false,
    val showQuestsModal: Boolean = false,
    val showSettingsModal: Boolean = false,
    val showLeaderboardModal: Boolean = false,
    val showShopModal: Boolean = false,
    val showProfileModal: Boolean = false,
    val showChatModal: Boolean = false,
    val chatInitialTab: Int = 0,
    val showAllianceModal: Boolean = false,
    val selectedWorldNodeId: String? = null,
    val offlineSummary: OfflineGainSummary? = null,
    val lastGachaResult: GachaPullResult? = null,
    val battleSession: BattleSession? = null,
    val resourceFlyEvents: List<ResourceFlyEvent> = emptyList(),
    val syncStatus: CloudSyncStatus = CloudSyncStatus.IDLE,
    val chiefName: String = "Pharaoh Chief",
    val bannerMessageEn: String? = null,
    val bannerMessageAr: String? = null
)

class GameViewModel(
    private val repository: SaveRepository,
    private val cloudRepository: CloudRepository,
    val currentUserId: String,
    initialDisplayName: String
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameCatalog.initialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _uiState = MutableStateFlow(
        UiModalState(chiefName = initialDisplayName.trim().take(40).ifBlank { "Pharaoh Chief" })
    )
    val uiState: StateFlow<UiModalState> = _uiState.asStateFlow()

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> = cloudRepository.observeLeaderboard()
        .map<List<LeaderboardEntry>, UiState<List<LeaderboardEntry>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w("GameViewModel", "Error observing leaderboard", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load leaderboard"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val worldCitadelsState: StateFlow<List<WorldCitadel>> = cloudRepository.observeWorldCitadels()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w("GameViewModel", "Error observing world citadels", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val alliancesState: StateFlow<List<AllianceInfo>> = cloudRepository.observeAlliances()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w("GameViewModel", "Error observing alliances", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val worldChatState: StateFlow<List<ChatMessage>> = cloudRepository.observeWorldChat()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w("GameViewModel", "Error observing world chat", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allianceChatState: StateFlow<List<ChatMessage>> = _gameState
        .map { it.allianceId }
        .distinctUntilChanged()
        .flatMapLatest { allianceId ->
            if (allianceId.isBlank()) {
                flowOf(emptyList())
            } else {
                cloudRepository.observeAllianceChat(allianceId)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w("GameViewModel", "Error observing alliance chat", firestoreError ?: error)
                        emit(emptyList())
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private var tickCount = 0
    private var battleJob: Job? = null
    private var damageIdCounter = 1L
    private var effectIdCounter = 1L
    private var flyEventIdCounter = 1L
    private val cloudSyncMutex = Mutex()
    @Volatile
    private var isLocalLoaded = false

    init {
        viewModelScope.launch {
            val (hasLocal, localLoaded) = withContext(Dispatchers.IO) {
                repository.hasLocalSave() to repository.load()
            }
            val (withOffline, summary) = repository.calculateOfflineGains(localLoaded)
            _gameState.value = withOffline
            SoundEffects.enabled = withOffline.soundEnabled
            isLocalLoaded = true
            if (summary != null) {
                _uiState.update { it.copy(offlineSummary = summary) }
            }
            startSimulationLoop()
            bootstrapFromCloud(localLoaded, hasLocal)
        }
    }

    private suspend fun bootstrapFromCloud(localLoaded: GameState, hasLocal: Boolean) {
        _uiState.update { it.copy(syncStatus = CloudSyncStatus.SYNCING) }

        val remoteName = cloudRepository.loadChiefName(currentUserId).getOrNull()
        if (!remoteName.isNullOrBlank()) {
            _uiState.update { it.copy(chiefName = remoteName) }
        }

        val cloudResult = cloudRepository.loadCloudSave(currentUserId)
        val cloudState = cloudResult.getOrNull()

        if (cloudState != null && (!hasLocal || cloudState.lastSavedEpochMs > localLoaded.lastSavedEpochMs + 2000L)) {
            val (cloudWithOffline, cloudSummary) = repository.calculateOfflineGains(cloudState)
            _gameState.value = cloudWithOffline
            SoundEffects.enabled = cloudWithOffline.soundEnabled
            withContext(Dispatchers.IO) {
                repository.save(cloudWithOffline)
            }
            if (cloudSummary != null) {
                _uiState.update { it.copy(offlineSummary = cloudSummary) }
            }
        }

        syncCloudInternal(publishLeaderboard = true)
    }

    private fun startSimulationLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                tickOneSecond()
                tickCount++
                if (tickCount % 10 == 0) {
                    val snap = _gameState.value
                    withContext(Dispatchers.IO) { repository.save(snap) }
                }
                if (tickCount % 20 == 0) {
                    syncCloudNow(silent = true)
                }
            }
        }
    }

    fun saveNow(syncCloud: Boolean = true) {
        if (!isLocalLoaded) return
        val snap = _gameState.value
        viewModelScope.launch(Dispatchers.IO) {
            repository.save(snap)
        }
        if (syncCloud) {
            syncCloudNow(silent = true)
        }
    }

    fun syncCloudNow(silent: Boolean = false) {
        viewModelScope.launch {
            val ok = syncCloudInternal(publishLeaderboard = true)
            if (!silent) {
                if (ok) {
                    showBanner("Cloud progress & leaderboard synced!", "تمت مزامنة السحابة ولوحة الصدارة!")
                } else {
                    showBanner("Cloud sync failed. Local save preserved.", "تعذرت المزامنة السحابية. تم الحفظ محلياً.")
                }
            }
        }
    }

    private suspend fun syncCloudInternal(publishLeaderboard: Boolean): Boolean = cloudSyncMutex.withLock {
        _uiState.update { it.copy(syncStatus = CloudSyncStatus.SYNCING) }
        val snap = _gameState.value.copy(lastSavedEpochMs = System.currentTimeMillis())
        val power = GameCatalog.computeTotalPower(snap)
        val devScore = GameCatalog.computeDevelopmentScore(snap)
        val chiefName = _uiState.value.chiefName
        val saveRes = cloudRepository.saveCloudState(currentUserId, snap, power)
        val lbRes = if (publishLeaderboard) {
            cloudRepository.upsertLeaderboardEntry(currentUserId, chiefName, snap, power, devScore)
        } else {
            Result.success(Unit)
        }
        if (publishLeaderboard) {
            cloudRepository.upsertWorldCitadel(currentUserId, chiefName, snap, power)
        }
        val ok = saveRes.isSuccess && lbRes.isSuccess
        _uiState.update { it.copy(syncStatus = if (ok) CloudSyncStatus.SYNCED else CloudSyncStatus.ERROR) }
        ok
    }

    private fun showBanner(en: String, ar: String) {
        _uiState.update { it.copy(bannerMessageEn = en, bannerMessageAr = ar) }
    }

    fun clearBanner() {
        _uiState.update { it.copy(bannerMessageEn = null, bannerMessageAr = null) }
    }

    private fun emitFlyEvents(newEvents: List<ResourceFlyEvent>) {
        if (newEvents.isEmpty()) return
        val now = System.currentTimeMillis()
        _uiState.update { ui ->
            val active = ui.resourceFlyEvents.filter { now - it.createdAtMs < 1100L }
            ui.copy(resourceFlyEvents = (active + newEvents).takeLast(14))
        }
    }

    private fun emitCostEvents(cost: ResourceCost, targetX: Float, targetY: Float) {
        val list = mutableListOf<ResourceFlyEvent>()
        if (cost.wood > 0) list += ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.WOOD, targetX, targetY, cost.wood, isConsumption = true)
        if (cost.coal > 0) list += ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.COAL, targetX, targetY, cost.coal, isConsumption = true)
        if (cost.iron > 0) list += ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.IRON, targetX, targetY, cost.iron, isConsumption = true)
        if (cost.meat > 0) list += ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.MEAT, targetX, targetY, cost.meat, isConsumption = true)
        emitFlyEvents(list)
    }

    // --- Navigation & Modals ---
    fun selectTab(tab: MainTab) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(activeTab = tab, highlightedBuildingId = null) }
    }

    fun selectHeroesSubTab(idx: Int) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(heroesSubTab = idx) }
    }

    fun openBuilding(id: String?) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(selectedBuildingId = id, highlightedBuildingId = null) }
    }

    fun setFurnaceModal(open: Boolean) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(showFurnaceModal = open, highlightedBuildingId = null) }
    }

    fun setSurvivorsModal(open: Boolean) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(showSurvivorsModal = open) }
    }

    fun setOrdersModal(open: Boolean) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(showOrdersModal = open) }
    }

    fun setQuestsModal(open: Boolean) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(showQuestsModal = open) }
    }

    fun setSettingsModal(open: Boolean) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(showSettingsModal = open) }
    }

    fun setLeaderboardModal(open: Boolean) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(showLeaderboardModal = open) }
        if (open) syncCloudNow(silent = true)
    }

    fun setShopModal(open: Boolean) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(showShopModal = open) }
    }

    fun setProfileModal(open: Boolean) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(showProfileModal = open) }
    }

    fun setChatModal(open: Boolean, initialTab: Int = 0) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(showChatModal = open, chatInitialTab = initialTab) }
    }

    fun setAllianceModal(open: Boolean) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(showAllianceModal = open) }
    }

    fun updateChiefName(newName: String) {
        val snap = _gameState.value
        updateCommanderProfile(newName, snap.avatarHeroId, snap.frameStyle)
    }

    fun updateCommanderProfile(newName: String, avatarHeroId: String, frameStyle: String) {
        val clean = newName.trim().take(40)
        if (clean.isEmpty()) return
        SoundEffects.play(SfxType.REWARD)
        _uiState.update { it.copy(chiefName = clean) }
        _gameState.update { s ->
            s.copy(
                avatarHeroId = avatarHeroId.ifBlank { s.avatarHeroId },
                frameStyle = frameStyle.ifBlank { s.frameStyle }
            )
        }
        saveNow(syncCloud = true)
        showBanner("Commander profile saved!", "تم حفظ ملف القائد الملكي!")
    }

    // --- Royal Shop ---
    fun claimDailyRoyalGift() {
        val state = _gameState.value
        val now = System.currentTimeMillis()
        val cooldownMs = 24L * 3600L * 1000L
        if (now - state.dailyGiftLastEpochMs < cooldownMs) {
            showBanner("Daily gift already claimed today!", "تم استلام الهدية اليومية بالفعل!")
            return
        }
        SoundEffects.play(SfxType.GACHA)
        emitFlyEvents(
            listOf(
                ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.GEMS, 0.50f, 0.35f, 120)
            )
        )
        _gameState.update { s ->
            s.copy(
                resources = s.resources.copy(
                    gems = s.resources.gems + 120,
                    goldKeys = s.resources.goldKeys + 1,
                    speedupsMin = s.resources.speedupsMin + 10,
                    heroXp = s.resources.heroXp + 350
                ),
                dailyGiftLastEpochMs = now
            )
        }
        saveNow(syncCloud = true)
        showBanner("Royal Gift claimed: +120 Gems & +1 Gold Key!", "تم استلام الهدية الملكية: +120 جوهرة ومفتاح ذهبي!")
    }

    fun buyShopItem(itemId: String) {
        val item = GameCatalog.shopCatalog.firstOrNull { it.id == itemId } ?: return
        val state = _gameState.value
        if (state.resources.gems < item.gemCost) {
            showBanner("Not enough Gems!", "الجواهر غير كافية!")
            return
        }
        SoundEffects.play(SfxType.REWARD)
        val flies = mutableListOf<ResourceFlyEvent>()
        if (item.meatGain > 0) flies += ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.MEAT, 0.4f, 0.5f, item.meatGain)
        if (item.woodGain > 0) flies += ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.WOOD, 0.5f, 0.5f, item.woodGain)
        if (item.coalGain > 0) flies += ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.COAL, 0.6f, 0.5f, item.coalGain)
        if (item.ironGain > 0) flies += ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.IRON, 0.5f, 0.45f, item.ironGain)
        emitFlyEvents(flies)

        _gameState.update { s ->
            s.copy(
                resources = s.resources.copy(
                    gems = s.resources.gems - item.gemCost,
                    meat = s.resources.meat + item.meatGain,
                    wood = s.resources.wood + item.woodGain,
                    coal = s.resources.coal + item.coalGain,
                    iron = s.resources.iron + item.ironGain,
                    speedupsMin = s.resources.speedupsMin + item.speedupMinGain,
                    silverKeys = s.resources.silverKeys + item.silverKeyGain,
                    goldKeys = s.resources.goldKeys + item.goldKeyGain,
                    heroXp = s.resources.heroXp + item.heroXpGain
                ),
                shopPurchasesCount = s.shopPurchasesCount + 1
            )
        }
        saveNow(syncCloud = true)
        showBanner("Purchased ${item.titleEn}!", "تم شراء ${item.titleAr} بنجاح!")
    }

    // --- Alliances & Live Chat ---
    fun createAlliance(name: String, tag: String, description: String, bannerColorHex: Long) {
        val cleanName = name.trim().take(32)
        val cleanTag = tag.trim().uppercase().take(5)
        if (cleanName.length < 2 || cleanTag.length < 2) {
            showBanner("Name & Tag need 2+ characters.", "الاسم والوسم يجب أن يكونا حرفين على الأقل.")
            return
        }
        val state = _gameState.value
        val power = GameCatalog.computeTotalPower(state)
        val chiefName = _uiState.value.chiefName
        viewModelScope.launch {
            val res = cloudRepository.createAlliance(
                userId = currentUserId,
                leaderName = chiefName,
                name = cleanName,
                tag = cleanTag,
                description = description,
                bannerColorHex = bannerColorHex,
                playerPower = power
            )
            res.onSuccess { info ->
                SoundEffects.play(SfxType.VICTORY)
                _gameState.update { it.copy(allianceId = info.allianceId, allianceTag = info.tag) }
                saveNow(syncCloud = true)
                showBanner("Alliance [${info.tag}] founded!", "تم تأسيس تحالف [${info.tag}] بنجاح!")
            }.onFailure {
                showBanner("Could not create alliance.", "تعذر تأسيس التحالف حالياً.")
            }
        }
    }

    fun joinAlliance(alliance: AllianceInfo) {
        val state = _gameState.value
        if (state.allianceId == alliance.allianceId) return
        val power = GameCatalog.computeTotalPower(state)
        viewModelScope.launch {
            val currentAlliance = alliancesState.value.firstOrNull { it.allianceId == state.allianceId }
            if (currentAlliance != null) {
                cloudRepository.leaveAlliance(currentUserId, currentAlliance, power)
            }
            val res = cloudRepository.joinAlliance(currentUserId, alliance, power)
            res.onSuccess {
                SoundEffects.play(SfxType.REWARD)
                _gameState.update { it.copy(allianceId = alliance.allianceId, allianceTag = alliance.tag) }
                saveNow(syncCloud = true)
                showBanner("Joined [${alliance.tag}] ${alliance.name}!", "انضممت إلى تحالف [${alliance.tag}]!")
            }.onFailure {
                showBanner("Could not join alliance.", "تعذر الانضمام للتحالف.")
            }
        }
    }

    fun leaveCurrentAlliance() {
        val state = _gameState.value
        if (state.allianceId.isBlank()) return
        val alliance = alliancesState.value.firstOrNull { it.allianceId == state.allianceId }
        val power = GameCatalog.computeTotalPower(state)
        viewModelScope.launch {
            if (alliance != null) {
                val res = cloudRepository.leaveAlliance(currentUserId, alliance, power)
                if (res.isFailure) {
                    showBanner("Leader cannot leave while members remain.", "لا يمكن للقائد المغادرة مع وجود أعضاء آخرين.")
                    return@launch
                }
            }
            SoundEffects.play(SfxType.TAP)
            _gameState.update { it.copy(allianceId = "", allianceTag = "") }
            saveNow(syncCloud = true)
            showBanner("Left alliance.", "تمت مغادرة التحالف.")
        }
    }

    fun sendWorldChat(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val snap = _gameState.value
        val chiefName = _uiState.value.chiefName
        SoundEffects.play(SfxType.TAP)
        viewModelScope.launch {
            val res = cloudRepository.sendWorldChat(
                userId = currentUserId,
                senderName = chiefName,
                avatarHeroId = snap.avatarHeroId,
                allianceTag = snap.allianceTag,
                text = clean
            )
            if (res.isFailure) {
                showBanner("Failed to send message.", "تعذر إرسال الرسالة.")
            }
        }
    }

    fun sendAllianceChat(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val snap = _gameState.value
        val alliance = alliancesState.value.firstOrNull { it.allianceId == snap.allianceId } ?: run {
            showBanner("Join an alliance first!", "انضم إلى تحالف أولاً!")
            return
        }
        val chiefName = _uiState.value.chiefName
        SoundEffects.play(SfxType.TAP)
        viewModelScope.launch {
            val res = cloudRepository.sendAllianceChat(
                alliance = alliance,
                userId = currentUserId,
                senderName = chiefName,
                avatarHeroId = snap.avatarHeroId,
                text = clean
            )
            if (res.isFailure) {
                showBanner("Failed to send alliance message.", "تعذر إرسال رسالة التحالف.")
            }
        }
    }

    fun dispatchCitadelExpedition(citadelId: String, npc: NpcCitadelInfo?, cloudCitadel: WorldCitadel?) {
        val state = _gameState.value
        if (state.marches.size >= GameCatalog.maxMarchSlots(state)) {
            showBanner("Max march slots reached! Upgrade Watchtower.", "بلغت الحد الأقصى للمسيرات! قم بترقية برج المراقبة.")
            return
        }
        val marchSec = 12
        SoundEffects.play(SfxType.UPGRADE)
        _gameState.update { s ->
            s.copy(
                marches = s.marches + MarchTask(
                    id = "m_${System.currentTimeMillis()}",
                    nodeId = citadelId,
                    nodeType = if (npc != null && npc.furnaceLevel >= 5) WorldNodeType.YETI_BOSS else WorldNodeType.FROST_BEAST,
                    nodeLevel = npc?.furnaceLevel ?: (cloudCitadel?.furnaceLevel?.toInt() ?: 2),
                    heroIds = s.formationHeroIds.take(3),
                    infantrySent = (s.troops.infantry / 2).coerceAtLeast(10),
                    lancersSent = (s.troops.lancers / 2).coerceAtLeast(5),
                    marksmenSent = (s.troops.marksmen / 2).coerceAtLeast(5),
                    remainingSec = marchSec,
                    totalSec = marchSec,
                    returning = false
                )
            )
        }
        val targetName = npc?.let { if (state.language == AppLang.AR) it.nameAr else it.nameEn }
            ?: cloudCitadel?.chiefName
            ?: "Citadel"
        showBanner("Expedition marching toward $targetName!", "انطلقت الحملة نحو $targetName!")
    }

    fun goToChapterMission(missionId: String) {
        val def = GameCatalog.chapterMissions.firstOrNull { it.id == missionId } ?: return
        SoundEffects.play(SfxType.TAP)
        when (def.targetFocus) {
            "furnace" -> _uiState.update {
                it.copy(
                    activeTab = MainTab.CAMP,
                    showQuestsModal = false,
                    highlightedBuildingId = "furnace",
                    showFurnaceModal = true
                )
            }
            "explore" -> _uiState.update {
                it.copy(
                    activeTab = MainTab.HEROES,
                    heroesSubTab = 2,
                    showQuestsModal = false
                )
            }
            "world" -> _uiState.update {
                it.copy(
                    activeTab = MainTab.WORLD,
                    showQuestsModal = false
                )
            }
            "tech" -> _uiState.update {
                it.copy(
                    activeTab = MainTab.TECH,
                    showQuestsModal = false
                )
            }
            else -> _uiState.update {
                it.copy(
                    activeTab = MainTab.CAMP,
                    showQuestsModal = false,
                    highlightedBuildingId = def.targetFocus,
                    selectedBuildingId = def.targetFocus
                )
            }
        }
    }

    fun selectWorldNode(nodeId: String?) {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { it.copy(selectedWorldNodeId = nodeId) }
    }

    fun dismissOfflineSummary() {
        SoundEffects.play(SfxType.REWARD)
        _uiState.update { it.copy(offlineSummary = null) }
    }

    fun dismissGachaResult() {
        _uiState.update { it.copy(lastGachaResult = null) }
    }

    fun toggleLanguage() {
        SoundEffects.play(SfxType.TAP)
        _gameState.update { s ->
            val next = if (s.language == AppLang.AR) AppLang.EN else AppLang.AR
            s.copy(language = next)
        }
        saveNow()
    }

    fun toggleSound() {
        _gameState.update { s ->
            val next = !s.soundEnabled
            SoundEffects.enabled = next
            if (next) SoundEffects.play(SfxType.TAP)
            s.copy(soundEnabled = next)
        }
        saveNow()
    }

    // --- Simulation Tick (1s) ---
    private fun tickOneSecond() {
        var triggerBlizzardSound = false
        var triggerUpgradeSound = false

        _gameState.update { state ->
            // 1. Advance Day Time (12 real seconds = 1 game hour -> 288s full day, or 12 min per sec)
            val newDayMinutes = state.dayTimeMinutes + 12
            val wrappedMinutes = newDayMinutes % 1440
            val nextDay = state.dayNumber + (if (newDayMinutes >= 1440) 1 else 0)

            // 2. Blizzard Countdown & Storm Lifecycle
            val blizzard = if (state.blizzard.isActive) {
                val rem = state.blizzard.activeRemainingSec - 1
                if (rem <= 0) {
                    BlizzardState(isActive = false, countdownSec = 105, activeRemainingSec = 0, stormCount = state.blizzard.stormCount + 1)
                } else {
                    state.blizzard.copy(activeRemainingSec = rem)
                }
            } else {
                val rem = state.blizzard.countdownSec - 1
                if (rem <= 0) {
                    triggerBlizzardSound = true
                    BlizzardState(isActive = true, countdownSec = 0, activeRemainingSec = 40, stormCount = state.blizzard.stormCount)
                } else {
                    state.blizzard.copy(countdownSec = rem)
                }
            }

            // 3. Resources per second
            val ratesPerMin = GameCatalog.computeRatesPerMinute(state)
            var meat = (state.resources.meat + ratesPerMin.meat / 60.0).coerceAtLeast(0.0)
            val wood = (state.resources.wood + ratesPerMin.wood / 60.0).coerceAtLeast(0.0)
            var coal = (state.resources.coal + ratesPerMin.coal / 60.0).coerceAtLeast(0.0)
            val iron = (state.resources.iron + ratesPerMin.iron / 60.0).coerceAtLeast(0.0)
            var meals = state.resources.meals

            // Auto-turn off Overdrive if coal hits 0
            var furnace = state.furnace
            if (coal <= 0.1 && furnace.overdrive) {
                furnace = furnace.copy(overdrive = false)
            }
            if (furnace.upgradeRemainingSec > 0) {
                val rem = furnace.upgradeRemainingSec - 1
                furnace = if (rem <= 0) {
                    triggerUpgradeSound = true
                    furnace.copy(level = (furnace.level + 1).coerceAtMost(10), upgradeRemainingSec = 0, upgradeTotalSec = 0)
                } else {
                    furnace.copy(upgradeRemainingSec = rem)
                }
            }

            // Cookhouse converts raw meat -> warm meals
            val cookhouse = state.buildings.firstOrNull { it.type == BuildingType.COOKHOUSE }
            if (cookhouse != null && cookhouse.level > 0 && meat >= 1.5) {
                val cookRate = (0.35 + cookhouse.assignedWorkers * 0.3 + cookhouse.level * 0.12)
                val meatUsed = cookRate.coerceAtMost(meat)
                meat -= meatUsed
                meals += meatUsed * (1.0 + (cookhouse.mealQuality - 1) * 0.25)
            }

            // 4. Buildings Construction & Upgrade Timers
            var completedUpgrades = 0
            val updatedBuildings = state.buildings.map { b ->
                if (b.upgradeRemainingSec > 0) {
                    val rem = b.upgradeRemainingSec - 1
                    if (rem <= 0) {
                        completedUpgrades++
                        triggerUpgradeSound = true
                        val autoWorkers = if (b.level == 0) GameCatalog.maxWorkersFor(b.copy(level = 1)).coerceAtMost(1) else b.assignedWorkers
                        b.copy(
                            level = (b.level + 1).coerceAtMost(10),
                            assignedWorkers = autoWorkers,
                            upgradeRemainingSec = 0,
                            upgradeTotalSec = 0
                        )
                    } else {
                        b.copy(upgradeRemainingSec = rem)
                    }
                } else b
            }

            // 5. Research Timer
            var techLevels = state.techLevels
            var techsCompleted = 0
            val activeResearch = state.activeResearch?.let { ar ->
                val rem = ar.remainingSec - 1
                if (rem <= 0) {
                    techsCompleted++
                    triggerUpgradeSound = true
                    techLevels = techLevels.map { tp ->
                        if (tp.id == ar.techId) tp.copy(level = (tp.level + 1).coerceAtMost(5)) else tp
                    }
                    null
                } else ar.copy(remainingSec = rem)
            }

            // 6. Troop Training Timer
            var troops = state.troops
            var trainedNow = 0
            val trainingQueue = state.trainingQueue?.let { tq ->
                val rem = tq.remainingSec - 1
                if (rem <= 0) {
                    trainedNow = tq.count
                    triggerUpgradeSound = true
                    troops = when (tq.troopType) {
                        TroopType.INFANTRY -> troops.copy(infantry = troops.infantry + tq.count)
                        TroopType.LANCER -> troops.copy(lancers = troops.lancers + tq.count)
                        TroopType.MARKSMAN -> troops.copy(marksmen = troops.marksmen + tq.count)
                    }
                    null
                } else tq.copy(remainingSec = rem)
            }

            // 7. Chief Orders Timers
            val updatedOrders = state.chiefOrders.map { o ->
                o.copy(
                    activeRemainingSec = (o.activeRemainingSec - 1).coerceAtLeast(0),
                    cooldownRemainingSec = (o.cooldownRemainingSec - 1).coerceAtLeast(0)
                )
            }
            val mobActive = updatedOrders.any { it.type == ChiefOrderType.URGENT_MOBILIZATION && it.activeRemainingSec > 0 }
            val doubleRations = updatedOrders.any { it.type == ChiefOrderType.DOUBLE_RATIONS && it.activeRemainingSec > 0 }
            val festivities = updatedOrders.any { it.type == ChiefOrderType.FESTIVITIES && it.activeRemainingSec > 0 }

            // 8. World Map Marches
            var extraMeat = 0.0
            var extraWood = 0.0
            var extraCoal = 0.0
            var extraIron = 0.0
            var extraXp = 0
            var extraSilverKeys = 0
            var huntsDone = 0
            val updatedMarches = state.marches.mapNotNull { m ->
                val rem = m.remainingSec - 1
                if (rem <= 0) {
                    if (!m.returning) {
                        // Arrived at target -> start returning
                        m.copy(returning = true, remainingSec = m.totalSec)
                    } else {
                        // Returned home -> collect loot!
                        huntsDone++
                        triggerUpgradeSound = true
                        when (m.nodeType) {
                            WorldNodeType.POLAR_BEAR -> {
                                extraMeat += 140 * m.nodeLevel
                                extraXp += 80 * m.nodeLevel
                                if (Random.nextFloat() < 0.55f) extraSilverKeys++
                            }
                            WorldNodeType.FROST_BEAST -> {
                                extraMeat += 180 * m.nodeLevel
                                extraCoal += 120 * m.nodeLevel
                                extraXp += 130 * m.nodeLevel
                                extraSilverKeys++
                            }
                            WorldNodeType.YETI_BOSS -> {
                                extraMeat += 360
                                extraIron += 240
                                extraXp += 420
                                extraSilverKeys += 2
                            }
                            WorldNodeType.COAL_VEIN -> extraCoal += 220 * m.nodeLevel
                            WorldNodeType.ANCIENT_TIMBER -> extraWood += 240 * m.nodeLevel
                            WorldNodeType.IRON_DEPOSIT -> extraIron += 180 * m.nodeLevel
                        }
                        null
                    }
                } else {
                    m.copy(remainingSec = rem)
                }
            }

            // 9. Survivors Daily Schedule, Warmth, Hunger, Fatigue, Clinic Treatment & Movement
            val campTemp = GameCatalog.computeCampTempC(state)
            val shelterPos = updatedBuildings.firstOrNull { it.type == BuildingType.SHELTER }
            val cookPos = updatedBuildings.firstOrNull { it.type == BuildingType.COOKHOUSE }
            val clinicPos = updatedBuildings.firstOrNull { it.type == BuildingType.CLINIC }
            val clinicHealMult = 1.0f + 0.25f * GameCatalog.techLevel(state, TechId.MEDICAL_HERBS)
            val clinicActive = (clinicPos?.level ?: 0) > 0

            val hour = wrappedMinutes / 60
            val isSleepHour = hour >= 22 || hour < 6
            val isMealHour = hour in 7..8 || hour in 12..13 || hour in 19..20

            val updatedSurvivors = state.survivors.map { surv ->
                if (surv.healthState == SurvivorHealthState.DEAD) return@map surv

                val assigned = updatedBuildings.firstOrNull { it.id == surv.assignedBuildingId && it.level > 0 }
                val heaterBonus = (assigned?.heaterLevel ?: 1) * 2.5f

                // Decide target activity
                val nextActivity = when {
                    surv.healthState == SurvivorHealthState.SICK && clinicActive -> SurvivorActivity.TREATING
                    isSleepHour || surv.fatigue > 85f -> SurvivorActivity.SLEEPING
                    (isMealHour || surv.hunger > 72f) && meals >= 1.0 -> SurvivorActivity.EATING
                    assigned != null -> SurvivorActivity.WORKING
                    else -> SurvivorActivity.IDLE
                }

                // Target coordinates
                val (tx, ty) = when (nextActivity) {
                    SurvivorActivity.TREATING -> (clinicPos?.gridX ?: 0.25f) to (clinicPos?.gridY ?: 0.6f)
                    SurvivorActivity.SLEEPING -> (shelterPos?.gridX ?: 0.25f) to (shelterPos?.gridY ?: 0.36f)
                    SurvivorActivity.EATING -> (cookPos?.gridX ?: 0.74f) to (cookPos?.gridY ?: 0.36f)
                    SurvivorActivity.WORKING -> (assigned?.gridX ?: 0.5f) to (assigned?.gridY ?: 0.48f)
                    SurvivorActivity.IDLE -> 0.50f to 0.53f
                }

                // Move survivor toward target smoothly
                val dx = tx - surv.posX
                val dy = ty - surv.posY
                val step = 0.045f
                val nx = if (abs(dx) <= step) tx else surv.posX + (if (dx > 0) step else -step)
                val ny = if (abs(dy) <= step) ty else surv.posY + (if (dy > 0) step else -step)

                // Stats simulation
                val targetWarmth = ((campTemp + 45f) + heaterBonus + (if (nextActivity == SurvivorActivity.SLEEPING) 14f else 0f) + (if (festivities) 12f else 0f))
                    .coerceIn(12f, 100f)
                val newWarmth = (surv.warmth + (targetWarmth - surv.warmth) * 0.10f).coerceIn(0f, 100f)

                var newHunger = surv.hunger
                if (nextActivity == SurvivorActivity.EATING && meals >= 1.0) {
                    meals -= if (doubleRations) 1.6 else 0.85
                    newHunger = (newHunger - 18f).coerceAtLeast(0f)
                } else {
                    newHunger = (newHunger + (if (doubleRations) 0.1f else 0.55f)).coerceAtMost(100f)
                }

                val newFatigue = when (nextActivity) {
                    SurvivorActivity.SLEEPING -> (surv.fatigue - 5.5f).coerceAtLeast(0f)
                    SurvivorActivity.WORKING -> (surv.fatigue + (if (mobActive) 1.45f else 0.65f)).coerceAtMost(100f)
                    else -> (surv.fatigue - 1.2f).coerceAtLeast(0f)
                }

                var newHealth = surv.health
                var newHealthState = surv.healthState

                if (newWarmth < 30f || newHunger > 85f) {
                    newHealth = (newHealth - 1.3f).coerceAtLeast(0f)
                    if (newHealth < 65f && newHealthState == SurvivorHealthState.HEALTHY) {
                        newHealthState = SurvivorHealthState.SICK
                    }
                } else if (nextActivity == SurvivorActivity.TREATING) {
                    newHealth = (newHealth + 3.4f * clinicHealMult).coerceAtMost(100f)
                    if (newHealth >= 88f) newHealthState = SurvivorHealthState.HEALTHY
                } else if (newHealthState == SurvivorHealthState.HEALTHY) {
                    newHealth = (newHealth + 0.45f).coerceAtMost(100f)
                }

                if (newHealth <= 0f) {
                    newHealthState = SurvivorHealthState.DEAD
                }

                val moraleDrift = ((newWarmth - 50f) * 0.03f) +
                    (if (newHunger > 70f) -0.8f else 0.25f) +
                    (if (festivities) 1.5f else 0f) +
                    (if (mobActive) -0.45f else 0f)
                val newHappiness = (surv.happiness + moraleDrift).coerceIn(10f, 100f)

                surv.copy(
                    health = newHealth,
                    warmth = newWarmth,
                    hunger = newHunger,
                    fatigue = newFatigue,
                    happiness = newHappiness,
                    activity = nextActivity,
                    healthState = newHealthState,
                    posX = nx,
                    posY = ny,
                    targetX = tx,
                    targetY = ty
                )
            }

            // Refresh world nodes periodically if fewer than 6
            val worldNodes = if (state.worldNodes.size < 6) GameCatalog.generateWorldNodes() else state.worldNodes

            state.copy(
                dayTimeMinutes = wrappedMinutes,
                dayNumber = nextDay,
                blizzard = blizzard,
                furnace = furnace,
                resources = state.resources.copy(
                    meat = meat + extraMeat,
                    wood = wood + extraWood,
                    coal = coal + extraCoal,
                    iron = iron + extraIron,
                    meals = meals.coerceAtLeast(0.0),
                    heroXp = state.resources.heroXp + extraXp,
                    silverKeys = state.resources.silverKeys + extraSilverKeys
                ),
                buildings = updatedBuildings,
                survivors = updatedSurvivors,
                chiefOrders = updatedOrders,
                techLevels = techLevels,
                activeResearch = activeResearch,
                troops = troops,
                trainingQueue = trainingQueue,
                marches = updatedMarches,
                worldNodes = worldNodes,
                quests = state.quests.copy(
                    upgradesCompleted = state.quests.upgradesCompleted + completedUpgrades,
                    techsResearched = state.quests.techsResearched + techsCompleted,
                    huntsCompleted = state.quests.huntsCompleted + huntsDone,
                    troopsTrained = state.quests.troopsTrained + trainedNow
                )
            )
        }

        if (triggerBlizzardSound) SoundEffects.play(SfxType.BLIZZARD)
        else if (triggerUpgradeSound) SoundEffects.play(SfxType.UPGRADE)

        // Prune expired fly events and emit real-time economy collection/consumption animations
        val now = System.currentTimeMillis()
        val snap = _gameState.value
        val newEconomyEvents = mutableListOf<ResourceFlyEvent>()
        if (tickCount % 3 == 0 && _uiState.value.activeTab == MainTab.CAMP) {
            val rates = GameCatalog.computeRatesPerMinute(snap)
            val producers = snap.buildings.filter {
                it.level > 0 && (it.type == BuildingType.COAL_MINE || it.type == BuildingType.SAWMILL ||
                    it.type == BuildingType.HUNTERS_HUT || it.type == BuildingType.IRON_MINE)
            }
            if (producers.isNotEmpty()) {
                val chosen = producers[(tickCount / 3) % producers.size]
                val (resType, amt) = when (chosen.type) {
                    BuildingType.COAL_MINE -> FlyResourceType.COAL to (rates.coal.coerceAtLeast(3.0) / 12.0).roundToInt().coerceAtLeast(1)
                    BuildingType.SAWMILL -> FlyResourceType.WOOD to (rates.wood / 12.0).roundToInt().coerceAtLeast(1)
                    BuildingType.HUNTERS_HUT -> FlyResourceType.MEAT to (rates.meat / 12.0).roundToInt().coerceAtLeast(1)
                    else -> FlyResourceType.IRON to (rates.iron / 12.0).roundToInt().coerceAtLeast(1)
                }
                newEconomyEvents += ResourceFlyEvent(
                    id = flyEventIdCounter++,
                    type = resType,
                    originX = chosen.gridX,
                    originY = 0.14f + chosen.gridY * 0.72f,
                    amount = amt,
                    isConsumption = false,
                    createdAtMs = now
                )
            }
            if (snap.resources.coal > 1.0) {
                val burnAmt = (GameCatalog.computeCoalBurnPerMinute(snap) / 15.0).roundToInt().coerceAtLeast(1)
                newEconomyEvents += ResourceFlyEvent(
                    id = flyEventIdCounter++,
                    type = FlyResourceType.COAL,
                    originX = 0.50f,
                    originY = 0.50f,
                    amount = burnAmt,
                    isConsumption = true,
                    createdAtMs = now
                )
            }
        }
        _uiState.update { ui ->
            val kept = ui.resourceFlyEvents.filter { now - it.createdAtMs < 1100L }
            if (kept.size == ui.resourceFlyEvents.size && newEconomyEvents.isEmpty()) ui
            else ui.copy(resourceFlyEvents = (kept + newEconomyEvents).takeLast(14))
        }
    }

    // --- Furnace Actions ---
    fun toggleFurnaceOverdrive() {
        val state = _gameState.value
        if (!state.furnace.overdrive && state.resources.coal < 10) {
            showBanner("Need at least 10 Coal for Overdrive!", "تحتاج 10 فحم على الأقل لأقصى طاقة!")
            return
        }
        SoundEffects.play(SfxType.OVERDRIVE)
        _gameState.update { s ->
            s.copy(furnace = s.furnace.copy(overdrive = !s.furnace.overdrive))
        }
    }

    fun upgradeFurnace() {
        val state = _gameState.value
        if (state.furnace.level >= 10 || state.furnace.upgradeRemainingSec > 0) return
        val cost = GameCatalog.furnaceUpgradeCost(state.furnace.level)
        if (!canAfford(state.resources, cost)) {
            showBanner("Insufficient resources!", "الموارد غير كافية!")
            return
        }
        val speedReduction = 1.0 - 0.12 * GameCatalog.techLevel(state, TechId.RAPID_CONSTRUCTION)
        val dur = (cost.durationSec * speedReduction).roundToInt().coerceAtLeast(5)
        SoundEffects.play(SfxType.UPGRADE)
        emitCostEvents(cost, 0.50f, 0.50f)
        _gameState.update { s ->
            s.copy(
                resources = deductCost(s.resources, cost),
                furnace = s.furnace.copy(upgradeRemainingSec = dur, upgradeTotalSec = dur)
            )
        }
    }

    fun speedupFurnace() {
        val state = _gameState.value
        if (state.furnace.upgradeRemainingSec <= 0 || state.resources.speedupsMin <= 0) return
        SoundEffects.play(SfxType.UPGRADE)
        _gameState.update { s ->
            s.copy(
                resources = s.resources.copy(speedupsMin = s.resources.speedupsMin - 1),
                furnace = s.furnace.copy(
                    level = (s.furnace.level + 1).coerceAtMost(10),
                    upgradeRemainingSec = 0,
                    upgradeTotalSec = 0
                ),
                quests = s.quests.copy(upgradesCompleted = s.quests.upgradesCompleted + 1)
            )
        }
    }

    // --- Building Actions ---
    fun upgradeOrBuildSlot(buildingId: String) {
        val state = _gameState.value
        val slot = state.buildings.firstOrNull { it.id == buildingId } ?: return
        if (slot.level >= 10 || slot.upgradeRemainingSec > 0) return
        if (slot.level >= state.furnace.level && slot.level > 0) {
            showBanner("Upgrade Central Furnace first!", "قم بترقية الفرن المركزي أولاً!")
            return
        }
        val cost = GameCatalog.buildingUpgradeCost(slot.type, slot.level)
        if (!canAfford(state.resources, cost)) {
            showBanner("Insufficient resources!", "الموارد غير كافية!")
            return
        }
        val speedReduction = 1.0 - 0.12 * GameCatalog.techLevel(state, TechId.RAPID_CONSTRUCTION)
        val dur = (cost.durationSec * speedReduction).roundToInt().coerceAtLeast(4)
        SoundEffects.play(SfxType.UPGRADE)
        emitCostEvents(cost, slot.gridX, 0.14f + slot.gridY * 0.72f)
        _gameState.update { s ->
            s.copy(
                resources = deductCost(s.resources, cost),
                buildings = s.buildings.map {
                    if (it.id == buildingId) it.copy(upgradeRemainingSec = dur, upgradeTotalSec = dur) else it
                }
            )
        }
    }

    fun speedupBuilding(buildingId: String) {
        val state = _gameState.value
        val slot = state.buildings.firstOrNull { it.id == buildingId } ?: return
        if (slot.upgradeRemainingSec <= 0 || state.resources.speedupsMin <= 0) return
        SoundEffects.play(SfxType.UPGRADE)
        _gameState.update { s ->
            s.copy(
                resources = s.resources.copy(speedupsMin = s.resources.speedupsMin - 1),
                buildings = s.buildings.map {
                    if (it.id == buildingId) {
                        val nextLv = (it.level + 1).coerceAtMost(10)
                        val autoW = if (it.level == 0) GameCatalog.maxWorkersFor(it.copy(level = 1)).coerceAtMost(1) else it.assignedWorkers
                        it.copy(level = nextLv, assignedWorkers = autoW, upgradeRemainingSec = 0, upgradeTotalSec = 0)
                    } else it
                },
                quests = s.quests.copy(upgradesCompleted = s.quests.upgradesCompleted + 1)
            )
        }
    }

    fun adjustWorkers(buildingId: String, delta: Int) {
        val state = _gameState.value
        val slot = state.buildings.firstOrNull { it.id == buildingId } ?: return
        val maxW = GameCatalog.maxWorkersFor(slot)
        val nextW = (slot.assignedWorkers + delta).coerceIn(0, maxW)
        if (nextW == slot.assignedWorkers) return
        SoundEffects.play(SfxType.TAP)
        _gameState.update { s ->
            val updatedBuildings = s.buildings.map {
                if (it.id == buildingId) it.copy(assignedWorkers = nextW) else it
            }
            // Reassign a survivor visually
            val updatedSurvivors = s.survivors.toMutableList()
            if (delta > 0) {
                val idx = updatedSurvivors.indexOfFirst { it.assignedBuildingId == null || it.assignedBuildingId != buildingId }
                if (idx >= 0) updatedSurvivors[idx] = updatedSurvivors[idx].copy(assignedBuildingId = buildingId)
            } else {
                val idx = updatedSurvivors.indexOfLast { it.assignedBuildingId == buildingId }
                if (idx >= 0) updatedSurvivors[idx] = updatedSurvivors[idx].copy(assignedBuildingId = null)
            }
            s.copy(buildings = updatedBuildings, survivors = updatedSurvivors)
        }
    }

    fun upgradeBuildingEquipment(buildingId: String, isHeater: Boolean) {
        val state = _gameState.value
        val slot = state.buildings.firstOrNull { it.id == buildingId } ?: return
        val currentLv = if (isHeater) slot.heaterLevel else slot.toolLevel
        if (currentLv >= 5) return
        val cost = GameCatalog.gearUpgradeCost(currentLv)
        if (!canAfford(state.resources, cost)) {
            showBanner("Insufficient resources!", "الموارد غير كافية!")
            return
        }
        SoundEffects.play(SfxType.UPGRADE)
        _gameState.update { s ->
            s.copy(
                resources = deductCost(s.resources, cost),
                buildings = s.buildings.map {
                    if (it.id == buildingId) {
                        if (isHeater) it.copy(heaterLevel = currentLv + 1) else it.copy(toolLevel = currentLv + 1)
                    } else it
                }
            )
        }
    }

    fun setCookhouseMealQuality(quality: Int) {
        SoundEffects.play(SfxType.TAP)
        _gameState.update { s ->
            s.copy(buildings = s.buildings.map {
                if (it.type == BuildingType.COOKHOUSE) it.copy(mealQuality = quality.coerceIn(1, 3)) else it
            })
        }
    }

    // --- Survivors & Chief Orders ---
    fun recruitSurvivor() {
        val state = _gameState.value
        val aliveCount = state.survivors.count { it.healthState != SurvivorHealthState.DEAD }
        val cap = GameCatalog.maxSurvivorCapacity(state)
        if (aliveCount >= cap) {
            showBanner("Shelters are full! Upgrade Shelters.", "الملاجئ ممتلئة! قم بترقية الملاجئ.")
            return
        }
        if (state.resources.meat < 60) {
            showBanner("Need 60 Meat to attract a survivor.", "تحتاج 60 لحم لجذب ناجٍ جديد.")
            return
        }
        val names = listOf(
            "Khufu" to "خوفو", "Nour" to "نور", "Sami" to "سامي",
            "Amina" to "أمينة", "Fares" to "فارس", "Salma" to "سلمى"
        )
        val pick = names[state.survivors.size % names.size]
        SoundEffects.play(SfxType.REWARD)
        _gameState.update { s ->
            s.copy(
                resources = s.resources.copy(meat = s.resources.meat - 60),
                survivors = s.survivors + Survivor(
                    id = "s_${System.currentTimeMillis()}",
                    nameEn = pick.first,
                    nameAr = pick.second,
                    posX = 0.5f,
                    posY = 0.55f
                )
            )
        }
    }

    fun treatAllSickSurvivors() {
        val state = _gameState.value
        val sickCount = state.survivors.count { it.healthState == SurvivorHealthState.SICK }
        if (sickCount == 0) return
        if (state.resources.gems < 15 && state.resources.meat < 40) {
            showBanner("Need 40 Meat for emergency medicine!", "تحتاج 40 لحم لعلاج المرضى!")
            return
        }
        SoundEffects.play(SfxType.REWARD)
        _gameState.update { s ->
            s.copy(
                resources = s.resources.copy(meat = (s.resources.meat - 40).coerceAtLeast(0.0)),
                survivors = s.survivors.map {
                    if (it.healthState == SurvivorHealthState.SICK)
                        it.copy(health = 95f, warmth = 80f, healthState = SurvivorHealthState.HEALTHY)
                    else it
                }
            )
        }
    }

    fun enactChiefOrder(type: ChiefOrderType) {
        val state = _gameState.value
        val order = state.chiefOrders.firstOrNull { it.type == type } ?: return
        if (order.cooldownRemainingSec > 0 || order.activeRemainingSec > 0) return

        SoundEffects.play(SfxType.OVERDRIVE)
        _gameState.update { s ->
            val updatedOrders = s.chiefOrders.map {
                if (it.type == type) it.copy(activeRemainingSec = 35, cooldownRemainingSec = 85) else it
            }
            val updatedSurvivors = when (type) {
                ChiefOrderType.URGENT_MOBILIZATION -> s.survivors.map {
                    it.copy(happiness = (it.happiness - 10f).coerceAtLeast(10f), fatigue = (it.fatigue + 12f).coerceAtMost(100f))
                }
                ChiefOrderType.DOUBLE_RATIONS -> s.survivors.map {
                    it.copy(hunger = 0f, happiness = (it.happiness + 20f).coerceAtMost(100f))
                }
                ChiefOrderType.FESTIVITIES -> s.survivors.map {
                    it.copy(happiness = (it.happiness + 30f).coerceAtMost(100f), warmth = (it.warmth + 15f).coerceAtMost(100f))
                }
            }
            s.copy(
                chiefOrders = updatedOrders,
                survivors = updatedSurvivors,
                quests = s.quests.copy(ordersEnacted = s.quests.ordersEnacted + 1)
            )
        }
    }

    // --- Barracks Troop Training ---
    fun trainTroops(type: TroopType, count: Int) {
        val state = _gameState.value
        if (state.trainingQueue != null) {
            showBanner("Training already in progress!", "التدريب جاري بالفعل!")
            return
        }
        val meatCost = count * 8
        val woodCost = count * 6
        val ironCost = if (type == TroopType.INFANTRY) count * 2 else count * 4
        if (state.resources.meat < meatCost || state.resources.wood < woodCost || state.resources.iron < ironCost) {
            showBanner("Insufficient resources to train troops!", "الموارد غير كافية لتدريب القوات!")
            return
        }
        val dur = (count / 2).coerceIn(6, 30)
        SoundEffects.play(SfxType.UPGRADE)
        _gameState.update { s ->
            s.copy(
                resources = s.resources.copy(
                    meat = s.resources.meat - meatCost,
                    wood = s.resources.wood - woodCost,
                    iron = s.resources.iron - ironCost
                ),
                trainingQueue = TroopTrainingQueue(type, count, dur, dur)
            )
        }
    }

    fun speedupTraining() {
        val state = _gameState.value
        val tq = state.trainingQueue ?: return
        if (state.resources.speedupsMin <= 0) return
        SoundEffects.play(SfxType.UPGRADE)
        _gameState.update { s ->
            val troops = when (tq.troopType) {
                TroopType.INFANTRY -> s.troops.copy(infantry = s.troops.infantry + tq.count)
                TroopType.LANCER -> s.troops.copy(lancers = s.troops.lancers + tq.count)
                TroopType.MARKSMAN -> s.troops.copy(marksmen = s.troops.marksmen + tq.count)
            }
            s.copy(
                resources = s.resources.copy(speedupsMin = s.resources.speedupsMin - 1),
                troops = troops,
                trainingQueue = null,
                quests = s.quests.copy(troopsTrained = s.quests.troopsTrained + tq.count)
            )
        }
    }

    // --- Research Lab Tech Tree ---
    fun startResearch(techId: TechId) {
        val state = _gameState.value
        val labLevel = state.buildings.firstOrNull { it.type == BuildingType.RESEARCH_LAB }?.level ?: 0
        if (labLevel <= 0) {
            showBanner("Build Research Lab first!", "قم ببناء مختبر الأبحاث أولاً!")
            return
        }
        if (state.activeResearch != null) {
            showBanner("Another research is already running!", "يوجد بحث قيد التنفيذ حالياً!")
            return
        }
        val currentLv = GameCatalog.techLevel(state, techId)
        if (currentLv >= 5) return
        val cost = GameCatalog.techResearchCost(techId, currentLv)
        if (!canAfford(state.resources, cost)) {
            showBanner("Insufficient resources!", "الموارد غير كافية!")
            return
        }
        val speedReduction = 1.0 - 0.12 * GameCatalog.techLevel(state, TechId.RAPID_CONSTRUCTION)
        val dur = (cost.durationSec * speedReduction).roundToInt().coerceAtLeast(5)
        SoundEffects.play(SfxType.UPGRADE)
        _gameState.update { s ->
            s.copy(
                resources = deductCost(s.resources, cost),
                activeResearch = ActiveResearch(techId, dur, dur)
            )
        }
    }

    fun speedupResearch() {
        val state = _gameState.value
        val ar = state.activeResearch ?: return
        if (state.resources.speedupsMin <= 0) return
        SoundEffects.play(SfxType.UPGRADE)
        _gameState.update { s ->
            s.copy(
                resources = s.resources.copy(speedupsMin = s.resources.speedupsMin - 1),
                techLevels = s.techLevels.map {
                    if (it.id == ar.techId) it.copy(level = (it.level + 1).coerceAtMost(5)) else it
                },
                activeResearch = null,
                quests = s.quests.copy(techsResearched = s.quests.techsResearched + 1)
            )
        }
    }

    // --- Heroes & Gacha ---
    fun levelUpHero(heroId: String) {
        val state = _gameState.value
        val hero = state.heroes.firstOrNull { it.id == heroId && it.unlocked } ?: return
        val xpCost = 80 + hero.level * 45
        if (state.resources.heroXp < xpCost) {
            showBanner("Need $xpCost Hero XP!", "تحتاج $xpCost خبرة أبطال!")
            return
        }
        SoundEffects.play(SfxType.UPGRADE)
        _gameState.update { s ->
            s.copy(
                resources = s.resources.copy(heroXp = s.resources.heroXp - xpCost),
                heroes = s.heroes.map { if (it.id == heroId) it.copy(level = it.level + 1) else it }
            )
        }
    }

    fun promoteHeroStar(heroId: String) {
        val state = _gameState.value
        val hero = state.heroes.firstOrNull { it.id == heroId && it.unlocked } ?: return
        if (hero.stars >= 5) return
        val shardCost = 5 + hero.stars * 5
        if (hero.shards < shardCost) {
            showBanner("Need $shardCost Hero Shards!", "تحتاج $shardCost شظية للبطل!")
            return
        }
        SoundEffects.play(SfxType.VICTORY)
        _gameState.update { s ->
            s.copy(
                heroes = s.heroes.map {
                    if (it.id == heroId) it.copy(stars = it.stars + 1, shards = it.shards - shardCost) else it
                }
            )
        }
    }

    fun upgradeHeroGear(heroId: String) {
        val state = _gameState.value
        val hero = state.heroes.firstOrNull { it.id == heroId && it.unlocked } ?: return
        if (hero.gearLevel >= 10) return
        val ironCost = 45 * hero.gearLevel
        if (state.resources.iron < ironCost) {
            showBanner("Need $ironCost Iron!", "تحتاج $ironCost حديد!")
            return
        }
        SoundEffects.play(SfxType.UPGRADE)
        _gameState.update { s ->
            s.copy(
                resources = s.resources.copy(iron = s.resources.iron - ironCost),
                heroes = s.heroes.map { if (it.id == heroId) it.copy(gearLevel = it.gearLevel + 1) else it }
            )
        }
    }

    fun toggleHeroInFormation(heroId: String) {
        val state = _gameState.value
        if (state.heroes.none { it.id == heroId && it.unlocked }) return
        SoundEffects.play(SfxType.TAP)
        _gameState.update { s ->
            val current = s.formationHeroIds.toMutableList()
            if (current.contains(heroId)) {
                if (current.size > 1) current.remove(heroId)
            } else if (current.size < 5) {
                current.add(heroId)
            }
            s.copy(formationHeroIds = current)
        }
    }

    fun pullGacha(isGoldChest: Boolean) {
        val state = _gameState.value
        if (isGoldChest && state.resources.goldKeys <= 0 && state.resources.gems < 180) {
            showBanner("Need 1 Gold Key or 180 Gems!", "تحتاج مفتاح ذهبي أو 180 جوهرة!")
            return
        }
        if (!isGoldChest && state.resources.silverKeys <= 0 && state.resources.gems < 80) {
            showBanner("Need 1 Silver Key or 80 Gems!", "تحتاج مفتاح فضي أو 80 جوهرة!")
            return
        }

        val pool = state.heroes
        val picked = if (isGoldChest) {
            val roll = Random.nextFloat()
            val targetRarity = when {
                roll < 0.38f -> HeroRarity.MYTHIC
                roll < 0.82f -> HeroRarity.EPIC
                else -> HeroRarity.RARE
            }
            pool.filter { it.rarity == targetRarity }.random()
        } else {
            val roll = Random.nextFloat()
            val targetRarity = when {
                roll < 0.10f -> HeroRarity.MYTHIC
                roll < 0.45f -> HeroRarity.EPIC
                else -> HeroRarity.RARE
            }
            pool.filter { it.rarity == targetRarity }.random()
        }

        val wasUnlocked = picked.unlocked
        val shardsGained = if (wasUnlocked) (if (isGoldChest) 8 else 4) else 0
        SoundEffects.play(SfxType.GACHA)

        _gameState.update { s ->
            val newRes = if (isGoldChest) {
                if (s.resources.goldKeys > 0) s.resources.copy(goldKeys = s.resources.goldKeys - 1)
                else s.resources.copy(gems = s.resources.gems - 180)
            } else {
                if (s.resources.silverKeys > 0) s.resources.copy(silverKeys = s.resources.silverKeys - 1)
                else s.resources.copy(gems = s.resources.gems - 80)
            }
            val newForm = if (!wasUnlocked && s.formationHeroIds.size < 5) s.formationHeroIds + picked.id else s.formationHeroIds
            s.copy(
                resources = newRes,
                heroes = s.heroes.map {
                    if (it.id == picked.id) {
                        if (!it.unlocked) it.copy(unlocked = true) else it.copy(shards = it.shards + shardsGained)
                    } else it
                },
                formationHeroIds = newForm,
                quests = s.quests.copy(gachaPullsCount = s.quests.gachaPullsCount + 1)
            )
        }

        _uiState.update {
            it.copy(
                lastGachaResult = GachaPullResult(
                    heroId = picked.id,
                    heroNameEn = picked.nameEn,
                    heroNameAr = picked.nameAr,
                    rarity = picked.rarity,
                    isNewUnlock = !wasUnlocked,
                    shardsGained = shardsGained,
                    accentColorHex = picked.accentColorHex
                )
            )
        }
    }

    // --- Exploration PvE Auto-Battler ---
    fun startExplorationBattle() {
        val state = _gameState.value
        val stage = state.explorationStage
        val allyHeroes = state.formationHeroIds.mapNotNull { id ->
            state.heroes.firstOrNull { it.id == id && it.unlocked }
        }.take(5)
        if (allyHeroes.isEmpty()) return

        val allyUnits = allyHeroes.mapIndexed { idx, h ->
            val maxHp = GameCatalog.heroMaxHp(h, state)
            BattleUnit(
                id = h.id,
                nameEn = h.nameEn,
                nameAr = h.nameAr,
                isAlly = true,
                isFrontline = h.heroClass == HeroClass.INFANTRY || idx < 2,
                heroClass = h.heroClass,
                rarity = h.rarity,
                maxHp = maxHp,
                hp = maxHp,
                attack = GameCatalog.heroAttack(h, state),
                defense = GameCatalog.heroDefense(h, state),
                energy = 25,
                accentColorHex = h.accentColorHex,
                skillNameEn = h.skillNameEn,
                skillNameAr = h.skillNameAr
            )
        }

        val scale = 1.0 + (stage - 1) * 0.28
        val enemyUnits = listOf(
            BattleUnit(
                id = "e_front_1", nameEn = "Frost Raider", nameAr = "مغير الصقيع",
                isAlly = false, isFrontline = true, heroClass = HeroClass.INFANTRY, rarity = HeroRarity.RARE,
                maxHp = (340 * scale).roundToInt(), hp = (340 * scale).roundToInt(),
                attack = (48 * scale).roundToInt(), defense = (14 * scale).roundToInt(),
                energy = 0, accentColorHex = 0xFFF87171, skillNameEn = "Khopesh Cleave", skillNameAr = "ضربة الفأس"
            ),
            BattleUnit(
                id = "e_front_2", nameEn = "Nile Frost Croc", nameAr = "تمساح النيل المتجمد",
                isAlly = false, isFrontline = true, heroClass = HeroClass.INFANTRY, rarity = HeroRarity.EPIC,
                maxHp = (410 * scale).roundToInt(), hp = (410 * scale).roundToInt(),
                attack = (54 * scale).roundToInt(), defense = (18 * scale).roundToInt(),
                energy = 0, accentColorHex = 0xFFFB923C, skillNameEn = "Glacial Jaw", skillNameAr = "فك الجليد"
            ),
            BattleUnit(
                id = "e_back_1", nameEn = "Sand-Frost Archer", nameAr = "رامي الخماسين",
                isAlly = false, isFrontline = false, heroClass = HeroClass.MARKSMAN, rarity = HeroRarity.RARE,
                maxHp = (230 * scale).roundToInt(), hp = (230 * scale).roundToInt(),
                attack = (62 * scale).roundToInt(), defense = (10 * scale).roundToInt(),
                energy = 0, accentColorHex = 0xFF94A3B8, skillNameEn = "Frost Volley", skillNameAr = "وابل الجليد"
            )
        ) + if (stage % 3 == 0) {
            listOf(
                BattleUnit(
                    id = "e_boss", nameEn = "Cryo Sphinx", nameAr = "أبو الهول الجليدي",
                    isAlly = false, isFrontline = false, heroClass = HeroClass.LANCER, rarity = HeroRarity.MYTHIC,
                    maxHp = (520 * scale).roundToInt(), hp = (520 * scale).roundToInt(),
                    attack = (72 * scale).roundToInt(), defense = (20 * scale).roundToInt(),
                    energy = 0, accentColorHex = 0xFFC084FC, skillNameEn = "Khamaseen Roar", skillNameAr = "زئير الخماسين"
                )
            )
        } else emptyList()

        val session = BattleSession(
            stage = stage,
            enemyTitleEn = "Stage $stage: Frozen Nile Dunes",
            enemyTitleAr = "المرحلة $stage: صحراء النيل المتجمدة",
            units = allyUnits + enemyUnits,
            autoSkill = true,
            rewardGems = 45 + stage * 12,
            rewardXp = 110 + stage * 45,
            rewardMeat = 90 + stage * 30,
            rewardKey = stage % 2 == 0
        )

        _uiState.update { it.copy(battleSession = session) }
        launchBattleLoop()
    }

    fun toggleBattleAutoSkill() {
        SoundEffects.play(SfxType.TAP)
        _uiState.update { ui ->
            ui.copy(battleSession = ui.battleSession?.copy(autoSkill = !ui.battleSession.autoSkill))
        }
    }

    fun triggerManualHeroSkill(unitId: String) {
        val current = _uiState.value.battleSession ?: return
        if (current.outcome != BattleOutcome.ONGOING) return
        val caster = current.units.firstOrNull { it.id == unitId && it.isAlly && it.hp > 0 && it.energy >= 100 } ?: return
        _uiState.update { it.copy(battleSession = applyUltimateSkill(current, caster.id)) }
    }

    private fun launchBattleLoop() {
        battleJob?.cancel()
        battleJob = viewModelScope.launch {
            while (isActive) {
                delay(450L)
                val session = _uiState.value.battleSession ?: break
                if (session.outcome != BattleOutcome.ONGOING) break

                val next = stepBattleTick(session)
                _uiState.update { it.copy(battleSession = next) }
                if (next.outcome != BattleOutcome.ONGOING) {
                    if (next.outcome == BattleOutcome.VICTORY) SoundEffects.play(SfxType.VICTORY)
                    break
                }
            }
        }
    }

    private fun stepBattleTick(session: BattleSession): BattleSession {
        var units = session.units.toMutableList()
        val floating = mutableListOf<FloatingDamage>()
        val visualEffects = mutableListOf<BattleVisualEffect>()
        val lunging = mutableSetOf<String>()
        val hitTargets = mutableSetOf<String>()
        var logEn = session.combatLogEn
        var logAr = session.combatLogAr

        val actingUnits = units.filter { it.hp > 0 }.shuffled()
        for (actorSnapshot in actingUnits) {
            val actor = units.firstOrNull { it.id == actorSnapshot.id && it.hp > 0 } ?: continue
            val targets = units.filter { it.isAlly != actor.isAlly && it.hp > 0 }
            if (targets.isEmpty()) break

            // Auto ultimate if ready
            if (actor.energy >= 100 && (!actor.isAlly || session.autoSkill)) {
                val updatedSession = applyUltimateSkill(
                    session.copy(units = units),
                    actor.id
                )
                units = updatedSession.units.toMutableList()
                floating.addAll(updatedSession.floatingDamages)
                visualEffects.addAll(updatedSession.activeEffects)
                lunging.addAll(updatedSession.lungingUnitIds)
                hitTargets.addAll(updatedSession.hitUnitIds)
                logEn = updatedSession.combatLogEn
                logAr = updatedSession.combatLogAr
                continue
            }

            // Basic attack prefers frontline
            val target = targets.filter { it.isFrontline }.minByOrNull { it.hp } ?: targets.minByOrNull { it.hp }!!
            val isCrit = Random.nextFloat() < 0.2f
            val raw = (actor.attack * (if (isCrit) 1.55 else 1.0) - target.defense * 0.45).roundToInt().coerceAtLeast(12)
            val idxT = units.indexOfFirst { it.id == target.id }
            units[idxT] = units[idxT].copy(
                hp = (units[idxT].hp - raw).coerceAtLeast(0),
                energy = (units[idxT].energy + 16).coerceAtMost(100)
            )
            val idxA = units.indexOfFirst { it.id == actor.id }
            units[idxA] = units[idxA].copy(energy = (units[idxA].energy + 25).coerceAtMost(100))

            lunging.add(actor.id)
            hitTargets.add(target.id)
            val actionType = when (actor.heroClass) {
                HeroClass.MARKSMAN -> BattleActionType.ARROW
                HeroClass.LANCER -> BattleActionType.LANCE_CHARGE
                HeroClass.INFANTRY -> BattleActionType.SLASH
            }
            visualEffects.add(
                BattleVisualEffect(
                    id = effectIdCounter++,
                    attackerId = actor.id,
                    targetId = target.id,
                    actionType = actionType,
                    colorHex = actor.accentColorHex,
                    isCrit = isCrit
                )
            )
            floating.add(FloatingDamage(damageIdCounter++, target.id, raw, isCrit))
            SoundEffects.play(SfxType.BATTLE_HIT)
        }

        val alliesAlive = units.any { it.isAlly && it.hp > 0 }
        val enemiesAlive = units.any { !it.isAlly && it.hp > 0 }
        val outcome = when {
            !enemiesAlive -> BattleOutcome.VICTORY
            !alliesAlive -> BattleOutcome.DEFEAT
            else -> BattleOutcome.ONGOING
        }

        return session.copy(
            units = units,
            outcome = outcome,
            floatingDamages = floating.takeLast(8),
            activeEffects = visualEffects.takeLast(6),
            lungingUnitIds = lunging,
            hitUnitIds = hitTargets,
            tickSeq = session.tickSeq + 1,
            combatLogEn = logEn,
            combatLogAr = logAr,
            activeFlashTextEn = null,
            activeFlashTextAr = null
        )
    }

    private fun applyUltimateSkill(session: BattleSession, casterId: String): BattleSession {
        val units = session.units.toMutableList()
        val casterIdx = units.indexOfFirst { it.id == casterId }
        if (casterIdx < 0) return session
        val caster = units[casterIdx]
        val floating = mutableListOf<FloatingDamage>()
        val effects = mutableListOf<BattleVisualEffect>()
        val hits = mutableSetOf<String>()

        SoundEffects.play(SfxType.SKILL)
        units[casterIdx] = caster.copy(energy = 0)

        if (caster.id == "h_patrick") {
            for (i in units.indices) {
                if (units[i].isAlly && units[i].hp > 0) {
                    val heal = (caster.attack * 1.45).roundToInt()
                    units[i] = units[i].copy(hp = (units[i].hp + heal).coerceAtMost(units[i].maxHp))
                    floating.add(FloatingDamage(damageIdCounter++, units[i].id, heal, isCrit = false, isHeal = true))
                    effects.add(BattleVisualEffect(effectIdCounter++, caster.id, units[i].id, BattleActionType.HEAL_RAY, caster.accentColorHex))
                }
            }
        } else if (caster.heroClass == HeroClass.LANCER || caster.id == "h_natalia" || caster.id == "h_flint") {
            for (i in units.indices) {
                if (units[i].isAlly != caster.isAlly && units[i].hp > 0) {
                    val dmg = (caster.attack * 1.75 - units[i].defense * 0.3).roundToInt().coerceAtLeast(25)
                    units[i] = units[i].copy(hp = (units[i].hp - dmg).coerceAtLeast(0))
                    floating.add(FloatingDamage(damageIdCounter++, units[i].id, dmg, isCrit = true))
                    effects.add(BattleVisualEffect(effectIdCounter++, caster.id, units[i].id, BattleActionType.ULTIMATE_NOVA, caster.accentColorHex, isCrit = true))
                    hits.add(units[i].id)
                }
            }
        } else {
            val target = units.filter { it.isAlly != caster.isAlly && it.hp > 0 }.minByOrNull { it.hp }
            if (target != null) {
                val idx = units.indexOfFirst { it.id == target.id }
                val dmg = (caster.attack * 2.6 - target.defense * 0.2).roundToInt().coerceAtLeast(35)
                units[idx] = units[idx].copy(hp = (units[idx].hp - dmg).coerceAtLeast(0))
                floating.add(FloatingDamage(damageIdCounter++, target.id, dmg, isCrit = true))
                effects.add(BattleVisualEffect(effectIdCounter++, caster.id, target.id, BattleActionType.ULTIMATE_NOVA, caster.accentColorHex, isCrit = true))
                hits.add(target.id)
            }
        }

        val enemiesAlive = units.any { !it.isAlly && it.hp > 0 }
        val outcome = if (!enemiesAlive) BattleOutcome.VICTORY else session.outcome

        return session.copy(
            units = units,
            outcome = outcome,
            floatingDamages = floating,
            activeEffects = effects,
            lungingUnitIds = setOf(caster.id),
            hitUnitIds = hits,
            tickSeq = session.tickSeq + 1,
            combatLogEn = "${caster.nameEn} used ${caster.skillNameEn}!",
            combatLogAr = "أطلق ${caster.nameAr} مهارة ${caster.skillNameAr}!",
            activeFlashTextEn = "${caster.nameEn.uppercase()}: ${caster.skillNameEn}!",
            activeFlashTextAr = "${caster.nameAr}: ${caster.skillNameAr}!",
            activeFlashColorHex = caster.accentColorHex
        )
    }

    fun finishBattle(claimVictory: Boolean) {
        battleJob?.cancel()
        val session = _uiState.value.battleSession
        if (claimVictory && session != null && session.outcome == BattleOutcome.VICTORY) {
            SoundEffects.play(SfxType.REWARD)
            emitFlyEvents(
                listOf(
                    ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.GEMS, 0.45f, 0.65f, session.rewardGems),
                    ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.MEAT, 0.55f, 0.68f, session.rewardMeat)
                )
            )
            _gameState.update { s ->
                s.copy(
                    explorationStage = s.explorationStage + 1,
                    resources = s.resources.copy(
                        gems = s.resources.gems + session.rewardGems,
                        heroXp = s.resources.heroXp + session.rewardXp,
                        meat = s.resources.meat + session.rewardMeat,
                        silverKeys = s.resources.silverKeys + (if (session.rewardKey) 1 else 0)
                    ),
                    quests = s.quests.copy(battlesWon = s.quests.battlesWon + 1)
                )
            }
            saveNow()
        }
        _uiState.update { it.copy(battleSession = null) }
    }

    // --- World Map Marches ---
    fun dispatchMarch(nodeId: String) {
        val state = _gameState.value
        if (state.marches.size >= GameCatalog.maxMarchSlots(state)) {
            showBanner("Max march slots reached! Upgrade Watchtower.", "بلغت الحد الأقصى للمسيرات! قم بترقية برج المراقبة.")
            return
        }
        val node = state.worldNodes.firstOrNull { it.id == nodeId } ?: return
        val totalPower = GameCatalog.computeTotalPower(state)
        if (totalPower < node.powerReq * 0.45 && (node.type == WorldNodeType.POLAR_BEAR || node.type == WorldNodeType.FROST_BEAST || node.type == WorldNodeType.YETI_BOSS)) {
            showBanner("Power too low! Upgrade heroes & troops.", "القوة منخفضة! طور الأبطال والقوات.")
            return
        }
        val marchSec = 10 + node.level * 2
        SoundEffects.play(SfxType.UPGRADE)
        _gameState.update { s ->
            s.copy(
                marches = s.marches + MarchTask(
                    id = "m_${System.currentTimeMillis()}",
                    nodeId = node.id,
                    nodeType = node.type,
                    nodeLevel = node.level,
                    heroIds = s.formationHeroIds.take(3),
                    infantrySent = (s.troops.infantry / 2).coerceAtLeast(10),
                    lancersSent = (s.troops.lancers / 2).coerceAtLeast(5),
                    marksmenSent = (s.troops.marksmen / 2).coerceAtLeast(5),
                    remainingSec = marchSec,
                    totalSec = marchSec,
                    returning = false
                )
            )
        }
        _uiState.update { it.copy(selectedWorldNodeId = null) }
    }

    // --- Quests ---
    fun claimChapterMission(missionId: String) {
        val state = _gameState.value
        val def = GameCatalog.chapterMissions.firstOrNull { it.id == missionId } ?: return
        if (state.quests.claimedChapterMissions.contains(missionId)) return
        val (cur, req) = def.progress(state)
        if (cur < req) return

        SoundEffects.play(SfxType.REWARD)
        emitFlyEvents(
            listOf(
                ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.GEMS, 0.25f, 0.78f, def.rewardGems),
                ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.MEAT, 0.30f, 0.80f, def.rewardMeat),
                ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.WOOD, 0.35f, 0.82f, def.rewardWood),
                ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.COAL, 0.40f, 0.80f, def.rewardCoal)
            )
        )
        _gameState.update { s ->
            val newClaimed = s.quests.claimedChapterMissions + missionId
            s.copy(
                resources = s.resources.copy(
                    gems = s.resources.gems + def.rewardGems,
                    meat = s.resources.meat + def.rewardMeat,
                    wood = s.resources.wood + def.rewardWood,
                    coal = s.resources.coal + def.rewardCoal,
                    silverKeys = s.resources.silverKeys + def.rewardKeys
                ),
                quests = s.quests.copy(claimedChapterMissions = newClaimed)
            )
        }
        saveNow()
    }

    fun claimChapterChest(chapter: Int) {
        val state = _gameState.value
        if (state.quests.claimedChapterChests.contains(chapter)) return
        val chapterDefs = GameCatalog.chapterMissions.filter { it.chapter == chapter }
        if (chapterDefs.isNotEmpty() && !chapterDefs.all { state.quests.claimedChapterMissions.contains(it.id) }) return

        val reward = GameCatalog.chapterChestReward(chapter)
        SoundEffects.play(SfxType.GACHA)
        emitFlyEvents(
            listOf(ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.GEMS, 0.32f, 0.76f, reward.gems))
        )
        _gameState.update { s ->
            s.copy(
                resources = s.resources.copy(
                    gems = s.resources.gems + reward.gems,
                    goldKeys = s.resources.goldKeys + reward.goldKeys,
                    speedupsMin = s.resources.speedupsMin + reward.speedupsMin,
                    heroXp = s.resources.heroXp + reward.heroXp
                ),
                quests = s.quests.copy(
                    chapter = if (s.quests.chapter == chapter) chapter + 1 else s.quests.chapter,
                    claimedChapterChests = s.quests.claimedChapterChests + chapter
                )
            )
        }
        saveNow()
    }

    fun claimDailyQuest(questId: String) {
        val state = _gameState.value
        val def = GameCatalog.dailyQuests.firstOrNull { it.id == questId } ?: return
        if (state.quests.claimedDailyIds.contains(questId)) return
        val (cur, req) = def.progress(state)
        if (cur < req) return

        SoundEffects.play(SfxType.REWARD)
        emitFlyEvents(
            listOf(ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.GEMS, 0.50f, 0.55f, def.rewardGems))
        )
        _gameState.update { s ->
            s.copy(
                resources = s.resources.copy(
                    gems = s.resources.gems + def.rewardGems,
                    heroXp = s.resources.heroXp + def.rewardXp
                ),
                quests = s.quests.copy(claimedDailyIds = s.quests.claimedDailyIds + questId)
            )
        }
        saveNow()
    }

    fun claimMilestoneChest(pointsTarget: Int) {
        val state = _gameState.value
        if (state.quests.claimedMilestonePoints.contains(pointsTarget)) return
        val earnedPoints = GameCatalog.dailyQuests
            .filter { state.quests.claimedDailyIds.contains(it.id) }
            .sumOf { it.points }
        if (earnedPoints < pointsTarget) return

        SoundEffects.play(SfxType.GACHA)
        emitFlyEvents(
            listOf(ResourceFlyEvent(flyEventIdCounter++, FlyResourceType.GEMS, 0.50f, 0.40f, pointsTarget * 2))
        )
        _gameState.update { s ->
            s.copy(
                resources = s.resources.copy(
                    gems = s.resources.gems + pointsTarget * 2,
                    goldKeys = s.resources.goldKeys + (if (pointsTarget >= 100) 1 else 0),
                    silverKeys = s.resources.silverKeys + (if (pointsTarget < 100) 1 else 0),
                    speedupsMin = s.resources.speedupsMin + 5
                ),
                quests = s.quests.copy(claimedMilestonePoints = s.quests.claimedMilestonePoints + pointsTarget)
            )
        }
        saveNow()
    }

    // --- Backup JSON Export/Import ---
    fun exportSaveJson(): String = repository.exportJson(_gameState.value)

    fun importSaveJson(json: String): Boolean {
        val imported = repository.importJson(json) ?: return false
        SoundEffects.enabled = imported.soundEnabled
        SoundEffects.play(SfxType.REWARD)
        _gameState.value = imported
        syncCloudNow(silent = true)
        return true
    }

    fun resetGame() {
        SoundEffects.play(SfxType.TAP)
        val fresh = repository.reset()
        SoundEffects.enabled = fresh.soundEnabled
        _gameState.value = fresh
        _uiState.update { UiModalState(chiefName = it.chiefName) }
        syncCloudNow(silent = true)
    }

    private fun canAfford(res: ResourcePack, cost: ResourceCost): Boolean =
        res.meat >= cost.meat && res.wood >= cost.wood && res.coal >= cost.coal && res.iron >= cost.iron

    private fun deductCost(res: ResourcePack, cost: ResourceCost): ResourcePack = res.copy(
        meat = (res.meat - cost.meat).coerceAtLeast(0.0),
        wood = (res.wood - cost.wood).coerceAtLeast(0.0),
        coal = (res.coal - cost.coal).coerceAtLeast(0.0),
        iron = (res.iron - cost.iron).coerceAtLeast(0.0)
    )

    override fun onCleared() {
        battleJob?.cancel()
        repository.save(_gameState.value)
        super.onCleared()
    }
}
