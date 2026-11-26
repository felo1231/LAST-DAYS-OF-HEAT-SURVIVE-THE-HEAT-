package com.example.model

import androidx.annotation.Keep
import com.example.data.GameCatalog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class CloudSyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    ERROR
}

enum class LeaderboardCategory {
    POWER,
    DEVELOPMENT,
    EXPLORATION
}

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val chiefName: String = "",
    val avatarHeroId: String = "h_sergey",
    val allianceTag: String = "",
    val totalPower: Long = 0L,
    val developmentScore: Long = 0L,
    val furnaceLevel: Long = 1L,
    val chapter: Long = 1L,
    val explorationStage: Long = 1L,
    val language: String = "AR",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun effectiveDevelopmentScore(): Long =
        if (developmentScore > 0L) developmentScore else furnaceLevel * 500L + chapter * 200L
}

@Keep
@IgnoreExtraProperties
data class WorldCitadel(
    @DocumentId val id: String = "",
    val userId: String = "",
    val chiefName: String = "",
    val avatarHeroId: String = "h_sergey",
    val frameStyle: String = "ROYAL_GOLD",
    val allianceId: String = "",
    val allianceTag: String = "",
    val furnaceLevel: Long = 1L,
    val totalPower: Long = 0L,
    val mapX: Double = 0.5,
    val mapY: Double = 0.5,
    val shieldActive: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AllianceInfo(
    @DocumentId val id: String = "",
    val allianceId: String = "",
    val name: String = "",
    val tag: String = "",
    val description: String = "",
    val bannerColorHex: Long = 0xFFF6C453L,
    val leaderUid: String = "",
    val leaderName: String = "",
    val memberUids: List<String> = emptyList(),
    val memberCount: Long = 1L,
    val totalPower: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val avatarHeroId: String = "h_sergey",
    val allianceTag: String = "",
    val text: String = "",
    val sentEpochMs: Long = 0L,
    val memberUids: List<String> = emptyList(),
    val createdAt: Timestamp? = null
)

private inline fun <reified T : Enum<T>> safeEnum(raw: Any?, default: T): T {
    val name = raw as? String ?: return default
    return enumValues<T>().firstOrNull { it.name == name } ?: default
}

private fun ResourcePack.toFirestoreMap(): Map<String, Any> = mapOf(
    "meat" to meat,
    "wood" to wood,
    "coal" to coal,
    "iron" to iron,
    "gems" to gems,
    "meals" to meals,
    "speedupsMin" to speedupsMin,
    "silverKeys" to silverKeys,
    "goldKeys" to goldKeys,
    "heroXp" to heroXp
)

private fun BuildingSlot.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "type" to type.name,
    "gridX" to gridX.toDouble(),
    "gridY" to gridY.toDouble(),
    "level" to level,
    "assignedWorkers" to assignedWorkers,
    "heaterLevel" to heaterLevel,
    "toolLevel" to toolLevel,
    "mealQuality" to mealQuality,
    "upgradeRemainingSec" to upgradeRemainingSec,
    "upgradeTotalSec" to upgradeTotalSec
)

private fun Survivor.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "nameEn" to nameEn,
    "nameAr" to nameAr,
    "health" to health.toDouble(),
    "happiness" to happiness.toDouble(),
    "warmth" to warmth.toDouble(),
    "hunger" to hunger.toDouble(),
    "fatigue" to fatigue.toDouble(),
    "activity" to activity.name,
    "healthState" to healthState.name,
    "assignedBuildingId" to (assignedBuildingId ?: ""),
    "posX" to posX.toDouble(),
    "posY" to posY.toDouble(),
    "targetX" to targetX.toDouble(),
    "targetY" to targetY.toDouble()
)

private fun ChiefOrderState.toFirestoreMap(): Map<String, Any> = mapOf(
    "type" to type.name,
    "activeRemainingSec" to activeRemainingSec,
    "cooldownRemainingSec" to cooldownRemainingSec
)

private fun TechProgress.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id.name,
    "level" to level
)

private fun TroopCounts.toFirestoreMap(): Map<String, Any> = mapOf(
    "infantry" to infantry,
    "lancers" to lancers,
    "marksmen" to marksmen
)

private fun HeroInstance.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "nameEn" to nameEn,
    "nameAr" to nameAr,
    "titleEn" to titleEn,
    "titleAr" to titleAr,
    "heroClass" to heroClass.name,
    "rarity" to rarity.name,
    "unlocked" to unlocked,
    "level" to level,
    "stars" to stars,
    "shards" to shards,
    "gearLevel" to gearLevel,
    "skillNameEn" to skillNameEn,
    "skillNameAr" to skillNameAr,
    "skillDescEn" to skillDescEn,
    "skillDescAr" to skillDescAr,
    "accentColorHex" to accentColorHex
)

private fun QuestState.toFirestoreMap(): Map<String, Any> = mapOf(
    "chapter" to chapter,
    "claimedChapterMissions" to claimedChapterMissions,
    "claimedChapterChests" to claimedChapterChests,
    "claimedDailyIds" to claimedDailyIds,
    "claimedMilestonePoints" to claimedMilestonePoints,
    "upgradesCompleted" to upgradesCompleted,
    "techsResearched" to techsResearched,
    "gachaPullsCount" to gachaPullsCount,
    "huntsCompleted" to huntsCompleted,
    "battlesWon" to battlesWon,
    "troopsTrained" to troopsTrained,
    "ordersEnacted" to ordersEnacted
)

private fun BlizzardState.toFirestoreMap(): Map<String, Any> = mapOf(
    "isActive" to isActive,
    "countdownSec" to countdownSec,
    "activeRemainingSec" to activeRemainingSec,
    "stormCount" to stormCount
)

private fun WorldNode.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "type" to type.name,
    "level" to level,
    "posX" to posX.toDouble(),
    "posY" to posY.toDouble(),
    "powerReq" to powerReq,
    "remainingAmount" to remainingAmount
)

private fun MarchTask.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "nodeId" to nodeId,
    "nodeType" to nodeType.name,
    "nodeLevel" to nodeLevel,
    "heroIds" to heroIds,
    "infantrySent" to infantrySent,
    "lancersSent" to lancersSent,
    "marksmenSent" to marksmenSent,
    "remainingSec" to remainingSec,
    "totalSec" to totalSec,
    "returning" to returning
)

private fun ActiveResearch.toFirestoreMap(): Map<String, Any> = mapOf(
    "techId" to techId.name,
    "remainingSec" to remainingSec,
    "totalSec" to totalSec
)

private fun TroopTrainingQueue.toFirestoreMap(): Map<String, Any> = mapOf(
    "troopType" to troopType.name,
    "count" to count,
    "remainingSec" to remainingSec,
    "totalSec" to totalSec
)

@Suppress("UNCHECKED_CAST")
fun GameState.toFirestoreCreateMap(userId: String, totalPower: Int): Map<String, Any> {
    val raw = mutableMapOf<String, Any?>(
        "userId" to userId,
        "language" to language.name,
        "soundEnabled" to soundEnabled,
        "totalPower" to totalPower.coerceIn(0, 100_000_000),
        "furnaceLevel" to furnace.level.coerceIn(1, 10),
        "furnaceOverdrive" to furnace.overdrive,
        "chapter" to quests.chapter.coerceIn(1, 50),
        "explorationStage" to explorationStage.coerceIn(1, 1000),
        "dayNumber" to dayNumber.coerceIn(1, 1_000_000),
        "dayTimeMinutes" to dayTimeMinutes.coerceIn(0, 1439),
        "lastSavedEpochMs" to lastSavedEpochMs.coerceAtLeast(0L),
        "avatarHeroId" to avatarHeroId.take(40).ifBlank { "h_sergey" },
        "frameStyle" to frameStyle.take(40).ifBlank { "ROYAL_GOLD" },
        "allianceId" to allianceId.take(64),
        "allianceTag" to allianceTag.take(10),
        "dailyGiftLastEpochMs" to dailyGiftLastEpochMs.coerceAtLeast(0L),
        "shopPurchasesCount" to shopPurchasesCount.coerceIn(0, 100_000),
        "resources" to resources.toFirestoreMap(),
        "buildings" to buildings.take(20).map { it.toFirestoreMap() },
        "survivors" to survivors.take(60).map { it.toFirestoreMap() },
        "chiefOrders" to chiefOrders.take(10).map { it.toFirestoreMap() },
        "techLevels" to techLevels.take(20).map { it.toFirestoreMap() },
        "troops" to troops.toFirestoreMap(),
        "heroes" to heroes.take(30).map { it.toFirestoreMap() },
        "formationHeroIds" to formationHeroIds.take(5),
        "quests" to quests.toFirestoreMap(),
        "blizzard" to blizzard.toFirestoreMap(),
        "worldNodes" to worldNodes.take(30).map { it.toFirestoreMap() },
        "marches" to marches.take(10).map { it.toFirestoreMap() },
        "activeResearch" to activeResearch?.toFirestoreMap(),
        "trainingQueue" to trainingQueue?.toFirestoreMap(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    return raw.filterValues { it != null } as Map<String, Any>
}

fun GameState.toFirestoreUpdateMap(totalPower: Int): Map<String, Any> = mapOf(
    "language" to language.name,
    "soundEnabled" to soundEnabled,
    "totalPower" to totalPower.coerceIn(0, 100_000_000),
    "furnaceLevel" to furnace.level.coerceIn(1, 10),
    "furnaceOverdrive" to furnace.overdrive,
    "chapter" to quests.chapter.coerceIn(1, 50),
    "explorationStage" to explorationStage.coerceIn(1, 1000),
    "dayNumber" to dayNumber.coerceIn(1, 1_000_000),
    "dayTimeMinutes" to dayTimeMinutes.coerceIn(0, 1439),
    "lastSavedEpochMs" to lastSavedEpochMs.coerceAtLeast(0L),
    "avatarHeroId" to avatarHeroId.take(40).ifBlank { "h_sergey" },
    "frameStyle" to frameStyle.take(40).ifBlank { "ROYAL_GOLD" },
    "allianceId" to allianceId.take(64),
    "allianceTag" to allianceTag.take(10),
    "dailyGiftLastEpochMs" to dailyGiftLastEpochMs.coerceAtLeast(0L),
    "shopPurchasesCount" to shopPurchasesCount.coerceIn(0, 100_000),
    "resources" to resources.toFirestoreMap(),
    "buildings" to buildings.take(20).map { it.toFirestoreMap() },
    "survivors" to survivors.take(60).map { it.toFirestoreMap() },
    "chiefOrders" to chiefOrders.take(10).map { it.toFirestoreMap() },
    "techLevels" to techLevels.take(20).map { it.toFirestoreMap() },
    "troops" to troops.toFirestoreMap(),
    "heroes" to heroes.take(30).map { it.toFirestoreMap() },
    "formationHeroIds" to formationHeroIds.take(5),
    "quests" to quests.toFirestoreMap(),
    "blizzard" to blizzard.toFirestoreMap(),
    "worldNodes" to worldNodes.take(30).map { it.toFirestoreMap() },
    "marches" to marches.take(10).map { it.toFirestoreMap() },
    "activeResearch" to (activeResearch?.toFirestoreMap() ?: FieldValue.delete()),
    "trainingQueue" to (trainingQueue?.toFirestoreMap() ?: FieldValue.delete()),
    "updatedAt" to FieldValue.serverTimestamp()
)

@Suppress("UNCHECKED_CAST")
fun gameStateFromFirestoreMap(data: Map<String, Any?>): GameState? {
    val fallback = GameCatalog.initialState()
    val resMap = data["resources"] as? Map<String, Any?> ?: return null
    val buildingsRaw = data["buildings"] as? List<Map<String, Any?>> ?: return null
    val heroesRaw = data["heroes"] as? List<Map<String, Any?>> ?: return null
    if (buildingsRaw.isEmpty() || heroesRaw.isEmpty()) return null

    val resources = ResourcePack(
        meat = (resMap["meat"] as? Number)?.toDouble() ?: fallback.resources.meat,
        wood = (resMap["wood"] as? Number)?.toDouble() ?: fallback.resources.wood,
        coal = (resMap["coal"] as? Number)?.toDouble() ?: fallback.resources.coal,
        iron = (resMap["iron"] as? Number)?.toDouble() ?: fallback.resources.iron,
        gems = (resMap["gems"] as? Number)?.toInt() ?: fallback.resources.gems,
        meals = (resMap["meals"] as? Number)?.toDouble() ?: fallback.resources.meals,
        speedupsMin = (resMap["speedupsMin"] as? Number)?.toInt() ?: fallback.resources.speedupsMin,
        silverKeys = (resMap["silverKeys"] as? Number)?.toInt() ?: fallback.resources.silverKeys,
        goldKeys = (resMap["goldKeys"] as? Number)?.toInt() ?: fallback.resources.goldKeys,
        heroXp = (resMap["heroXp"] as? Number)?.toInt() ?: fallback.resources.heroXp
    )

    val buildings = buildingsRaw.mapNotNull { b ->
        val id = b["id"] as? String ?: return@mapNotNull null
        BuildingSlot(
            id = id,
            type = safeEnum(b["type"], BuildingType.SHELTER),
            gridX = (b["gridX"] as? Number)?.toFloat() ?: 0.5f,
            gridY = (b["gridY"] as? Number)?.toFloat() ?: 0.5f,
            level = (b["level"] as? Number)?.toInt() ?: 0,
            assignedWorkers = (b["assignedWorkers"] as? Number)?.toInt() ?: 0,
            heaterLevel = (b["heaterLevel"] as? Number)?.toInt() ?: 1,
            toolLevel = (b["toolLevel"] as? Number)?.toInt() ?: 1,
            mealQuality = (b["mealQuality"] as? Number)?.toInt() ?: 1,
            upgradeRemainingSec = (b["upgradeRemainingSec"] as? Number)?.toInt() ?: 0,
            upgradeTotalSec = (b["upgradeTotalSec"] as? Number)?.toInt() ?: 0
        )
    }

    val survivorsRaw = data["survivors"] as? List<Map<String, Any?>> ?: emptyList()
    val survivors = survivorsRaw.mapNotNull { s ->
        val id = s["id"] as? String ?: return@mapNotNull null
        val assignedRaw = s["assignedBuildingId"] as? String
        Survivor(
            id = id,
            nameEn = s["nameEn"] as? String ?: "Citizen",
            nameAr = s["nameAr"] as? String ?: "مواطن",
            health = (s["health"] as? Number)?.toFloat() ?: 100f,
            happiness = (s["happiness"] as? Number)?.toFloat() ?: 82f,
            warmth = (s["warmth"] as? Number)?.toFloat() ?: 84f,
            hunger = (s["hunger"] as? Number)?.toFloat() ?: 18f,
            fatigue = (s["fatigue"] as? Number)?.toFloat() ?: 15f,
            activity = safeEnum(s["activity"], SurvivorActivity.WORKING),
            healthState = safeEnum(s["healthState"], SurvivorHealthState.HEALTHY),
            assignedBuildingId = assignedRaw?.takeIf { it.isNotBlank() },
            posX = (s["posX"] as? Number)?.toFloat() ?: 0.5f,
            posY = (s["posY"] as? Number)?.toFloat() ?: 0.5f,
            targetX = (s["targetX"] as? Number)?.toFloat() ?: 0.5f,
            targetY = (s["targetY"] as? Number)?.toFloat() ?: 0.5f
        )
    }.ifEmpty { fallback.survivors }

    val ordersRaw = data["chiefOrders"] as? List<Map<String, Any?>> ?: emptyList()
    val chiefOrders = ordersRaw.map { o ->
        ChiefOrderState(
            type = safeEnum(o["type"], ChiefOrderType.URGENT_MOBILIZATION),
            activeRemainingSec = (o["activeRemainingSec"] as? Number)?.toInt() ?: 0,
            cooldownRemainingSec = (o["cooldownRemainingSec"] as? Number)?.toInt() ?: 0
        )
    }.ifEmpty { fallback.chiefOrders }

    val techRaw = data["techLevels"] as? List<Map<String, Any?>> ?: emptyList()
    val techLevels = techRaw.map { t ->
        TechProgress(
            id = safeEnum(t["id"], TechId.EFFICIENT_MINING),
            level = (t["level"] as? Number)?.toInt() ?: 0
        )
    }.ifEmpty { fallback.techLevels }

    val troopsMap = data["troops"] as? Map<String, Any?>
    val troops = if (troopsMap != null) {
        TroopCounts(
            infantry = (troopsMap["infantry"] as? Number)?.toInt() ?: 35,
            lancers = (troopsMap["lancers"] as? Number)?.toInt() ?: 25,
            marksmen = (troopsMap["marksmen"] as? Number)?.toInt() ?: 25
        )
    } else fallback.troops

    val heroes = heroesRaw.mapNotNull { h ->
        val id = h["id"] as? String ?: return@mapNotNull null
        HeroInstance(
            id = id,
            nameEn = h["nameEn"] as? String ?: "Hero",
            nameAr = h["nameAr"] as? String ?: "بطل",
            titleEn = h["titleEn"] as? String ?: "",
            titleAr = h["titleAr"] as? String ?: "",
            heroClass = safeEnum(h["heroClass"], HeroClass.INFANTRY),
            rarity = safeEnum(h["rarity"], HeroRarity.RARE),
            unlocked = h["unlocked"] as? Boolean ?: false,
            level = (h["level"] as? Number)?.toInt() ?: 1,
            stars = (h["stars"] as? Number)?.toInt() ?: 1,
            shards = (h["shards"] as? Number)?.toInt() ?: 0,
            gearLevel = (h["gearLevel"] as? Number)?.toInt() ?: 1,
            skillNameEn = h["skillNameEn"] as? String ?: "",
            skillNameAr = h["skillNameAr"] as? String ?: "",
            skillDescEn = h["skillDescEn"] as? String ?: "",
            skillDescAr = h["skillDescAr"] as? String ?: "",
            accentColorHex = (h["accentColorHex"] as? Number)?.toLong() ?: 0xFF38BDF8L
        )
    }

    val formation = (data["formationHeroIds"] as? List<*>)?.mapNotNull { it as? String }?.take(5)
        ?: fallback.formationHeroIds

    val qMap = data["quests"] as? Map<String, Any?>
    val quests = if (qMap != null) {
        QuestState(
            chapter = (qMap["chapter"] as? Number)?.toInt() ?: 1,
            claimedChapterMissions = (qMap["claimedChapterMissions"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            claimedChapterChests = (qMap["claimedChapterChests"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
            claimedDailyIds = (qMap["claimedDailyIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            claimedMilestonePoints = (qMap["claimedMilestonePoints"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
            upgradesCompleted = (qMap["upgradesCompleted"] as? Number)?.toInt() ?: 0,
            techsResearched = (qMap["techsResearched"] as? Number)?.toInt() ?: 0,
            gachaPullsCount = (qMap["gachaPullsCount"] as? Number)?.toInt() ?: 0,
            huntsCompleted = (qMap["huntsCompleted"] as? Number)?.toInt() ?: 0,
            battlesWon = (qMap["battlesWon"] as? Number)?.toInt() ?: 0,
            troopsTrained = (qMap["troopsTrained"] as? Number)?.toInt() ?: 0,
            ordersEnacted = (qMap["ordersEnacted"] as? Number)?.toInt() ?: 0
        )
    } else QuestState()

    val bMap = data["blizzard"] as? Map<String, Any?>
    val blizzard = if (bMap != null) {
        BlizzardState(
            isActive = bMap["isActive"] as? Boolean ?: false,
            countdownSec = (bMap["countdownSec"] as? Number)?.toInt() ?: 95,
            activeRemainingSec = (bMap["activeRemainingSec"] as? Number)?.toInt() ?: 0,
            stormCount = (bMap["stormCount"] as? Number)?.toInt() ?: 0
        )
    } else BlizzardState()

    val nodesRaw = data["worldNodes"] as? List<Map<String, Any?>> ?: emptyList()
    val worldNodes = nodesRaw.mapNotNull { n ->
        val id = n["id"] as? String ?: return@mapNotNull null
        WorldNode(
            id = id,
            type = safeEnum(n["type"], WorldNodeType.COAL_VEIN),
            level = (n["level"] as? Number)?.toInt() ?: 1,
            posX = (n["posX"] as? Number)?.toFloat() ?: 0.5f,
            posY = (n["posY"] as? Number)?.toFloat() ?: 0.5f,
            powerReq = (n["powerReq"] as? Number)?.toInt() ?: 200,
            remainingAmount = (n["remainingAmount"] as? Number)?.toInt() ?: 100
        )
    }.ifEmpty { fallback.worldNodes }

    val marchesRaw = data["marches"] as? List<Map<String, Any?>> ?: emptyList()
    val marches = marchesRaw.mapNotNull { m ->
        val id = m["id"] as? String ?: return@mapNotNull null
        MarchTask(
            id = id,
            nodeId = m["nodeId"] as? String ?: "",
            nodeType = safeEnum(m["nodeType"], WorldNodeType.COAL_VEIN),
            nodeLevel = (m["nodeLevel"] as? Number)?.toInt() ?: 1,
            heroIds = (m["heroIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            infantrySent = (m["infantrySent"] as? Number)?.toInt() ?: 10,
            lancersSent = (m["lancersSent"] as? Number)?.toInt() ?: 5,
            marksmenSent = (m["marksmenSent"] as? Number)?.toInt() ?: 5,
            remainingSec = (m["remainingSec"] as? Number)?.toInt() ?: 10,
            totalSec = (m["totalSec"] as? Number)?.toInt() ?: 10,
            returning = m["returning"] as? Boolean ?: false
        )
    }

    val arMap = data["activeResearch"] as? Map<String, Any?>
    val activeResearch = arMap?.let {
        ActiveResearch(
            techId = safeEnum(it["techId"], TechId.EFFICIENT_MINING),
            remainingSec = (it["remainingSec"] as? Number)?.toInt() ?: 10,
            totalSec = (it["totalSec"] as? Number)?.toInt() ?: 10
        )
    }

    val tqMap = data["trainingQueue"] as? Map<String, Any?>
    val trainingQueue = tqMap?.let {
        TroopTrainingQueue(
            troopType = safeEnum(it["troopType"], TroopType.INFANTRY),
            count = (it["count"] as? Number)?.toInt() ?: 10,
            remainingSec = (it["remainingSec"] as? Number)?.toInt() ?: 10,
            totalSec = (it["totalSec"] as? Number)?.toInt() ?: 10
        )
    }

    return GameState(
        language = safeEnum(data["language"], AppLang.AR),
        soundEnabled = data["soundEnabled"] as? Boolean ?: true,
        resources = resources,
        furnace = FurnaceState(
            level = (data["furnaceLevel"] as? Number)?.toInt()?.coerceIn(1, 10) ?: 1,
            overdrive = data["furnaceOverdrive"] as? Boolean ?: false
        ),
        buildings = buildings,
        survivors = survivors,
        chiefOrders = chiefOrders,
        techLevels = techLevels,
        activeResearch = activeResearch,
        troops = troops,
        trainingQueue = trainingQueue,
        heroes = GameCatalog.normalizeHeroesForRoyalNames(heroes),
        formationHeroIds = formation,
        explorationStage = (data["explorationStage"] as? Number)?.toInt()?.coerceIn(1, 1000) ?: 1,
        worldNodes = worldNodes,
        marches = marches,
        blizzard = blizzard,
        quests = quests,
        dayTimeMinutes = (data["dayTimeMinutes"] as? Number)?.toInt()?.coerceIn(0, 1439) ?: 480,
        dayNumber = (data["dayNumber"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 1,
        lastSavedEpochMs = (data["lastSavedEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        avatarHeroId = (data["avatarHeroId"] as? String)?.take(40)?.ifBlank { "h_sergey" } ?: "h_sergey",
        frameStyle = (data["frameStyle"] as? String)?.take(40)?.ifBlank { "ROYAL_GOLD" } ?: "ROYAL_GOLD",
        allianceId = (data["allianceId"] as? String)?.take(64) ?: "",
        allianceTag = (data["allianceTag"] as? String)?.take(10) ?: "",
        dailyGiftLastEpochMs = (data["dailyGiftLastEpochMs"] as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L,
        shopPurchasesCount = (data["shopPurchasesCount"] as? Number)?.toInt()?.coerceIn(0, 100_000) ?: 0
    )
}
