package com.example.model

import com.squareup.moshi.JsonClass

enum class AppLang { AR, EN }

enum class MainTab { CAMP, WORLD, HEROES, BACKPACK, TECH }

@JsonClass(generateAdapter = true)
data class ResourcePack(
    val meat: Double = 320.0,
    val wood: Double = 420.0,
    val coal: Double = 280.0,
    val iron: Double = 160.0,
    val gems: Int = 500,
    val meals: Double = 45.0,
    val speedupsMin: Int = 25,
    val silverKeys: Int = 4,
    val goldKeys: Int = 2,
    val heroXp: Int = 650
)

@JsonClass(generateAdapter = true)
data class FurnaceState(
    val level: Int = 1,
    val overdrive: Boolean = false,
    val upgradeRemainingSec: Int = 0,
    val upgradeTotalSec: Int = 0
)

enum class BuildingType {
    COAL_MINE,
    SAWMILL,
    HUNTERS_HUT,
    IRON_MINE,
    SHELTER,
    COOKHOUSE,
    CLINIC,
    RESEARCH_LAB,
    BARRACKS,
    WATCHTOWER
}

@JsonClass(generateAdapter = true)
data class BuildingSlot(
    val id: String,
    val type: BuildingType,
    val gridX: Float, // normalized 0..1 on camp map
    val gridY: Float, // normalized 0..1 on camp map
    val level: Int = 0, // 0 = plot unbuilt, 1..10 = built
    val assignedWorkers: Int = 0,
    val heaterLevel: Int = 1, // 1..5
    val toolLevel: Int = 1, // 1..5
    val mealQuality: Int = 1, // 1..3 for Cookhouse
    val upgradeRemainingSec: Int = 0,
    val upgradeTotalSec: Int = 0
)

enum class SurvivorActivity {
    WORKING,
    EATING,
    SLEEPING,
    TREATING,
    IDLE
}

enum class SurvivorHealthState {
    HEALTHY,
    SICK,
    DEAD
}

@JsonClass(generateAdapter = true)
data class Survivor(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val health: Float = 100f,
    val happiness: Float = 82f,
    val warmth: Float = 84f,
    val hunger: Float = 18f, // 0 = full, 100 = starving
    val fatigue: Float = 15f, // 0 = rested, 100 = exhausted
    val activity: SurvivorActivity = SurvivorActivity.WORKING,
    val healthState: SurvivorHealthState = SurvivorHealthState.HEALTHY,
    val assignedBuildingId: String? = null,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val targetX: Float = 0.5f,
    val targetY: Float = 0.5f
)

enum class ChiefOrderType {
    URGENT_MOBILIZATION,
    DOUBLE_RATIONS,
    FESTIVITIES
}

@JsonClass(generateAdapter = true)
data class ChiefOrderState(
    val type: ChiefOrderType,
    val activeRemainingSec: Int = 0,
    val cooldownRemainingSec: Int = 0
)

enum class TechBranch { ECONOMY, GROWTH, BATTLE }

enum class TechId {
    EFFICIENT_MINING,
    TIMBER_REFINING,
    GOURMET_RATIONS,
    INSULATED_WALLS,
    RAPID_CONSTRUCTION,
    MEDICAL_HERBS,
    STEEL_FORGING,
    HERO_TACTICS,
    FROST_RESISTANCE
}

@JsonClass(generateAdapter = true)
data class TechProgress(
    val id: TechId,
    val level: Int = 0
)

@JsonClass(generateAdapter = true)
data class ActiveResearch(
    val techId: TechId,
    val remainingSec: Int,
    val totalSec: Int
)

enum class TroopType { INFANTRY, LANCER, MARKSMAN }

@JsonClass(generateAdapter = true)
data class TroopCounts(
    val infantry: Int = 30,
    val lancers: Int = 20,
    val marksmen: Int = 20
)

@JsonClass(generateAdapter = true)
data class TroopTrainingQueue(
    val troopType: TroopType,
    val count: Int,
    val remainingSec: Int,
    val totalSec: Int
)

enum class HeroClass { INFANTRY, LANCER, MARKSMAN }

enum class HeroRarity { RARE, EPIC, MYTHIC }

@JsonClass(generateAdapter = true)
data class HeroInstance(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val titleEn: String,
    val titleAr: String,
    val heroClass: HeroClass,
    val rarity: HeroRarity,
    val unlocked: Boolean,
    val level: Int = 1,
    val stars: Int = 1,
    val shards: Int = 0,
    val gearLevel: Int = 1,
    val skillNameEn: String,
    val skillNameAr: String,
    val skillDescEn: String,
    val skillDescAr: String,
    val accentColorHex: Long
)

enum class WorldNodeType {
    POLAR_BEAR,
    FROST_BEAST,
    YETI_BOSS,
    COAL_VEIN,
    ANCIENT_TIMBER,
    IRON_DEPOSIT
}

@JsonClass(generateAdapter = true)
data class WorldNode(
    val id: String,
    val type: WorldNodeType,
    val level: Int,
    val posX: Float,
    val posY: Float,
    val powerReq: Int,
    val remainingAmount: Int
)

@JsonClass(generateAdapter = true)
data class MarchTask(
    val id: String,
    val nodeId: String,
    val nodeType: WorldNodeType,
    val nodeLevel: Int,
    val heroIds: List<String>,
    val infantrySent: Int,
    val lancersSent: Int,
    val marksmenSent: Int,
    val remainingSec: Int,
    val totalSec: Int,
    val returning: Boolean = false
)

@JsonClass(generateAdapter = true)
data class BlizzardState(
    val isActive: Boolean = false,
    val countdownSec: Int = 90,
    val activeRemainingSec: Int = 0,
    val stormCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class QuestState(
    val chapter: Int = 1,
    val claimedChapterMissions: List<String> = emptyList(),
    val claimedChapterChests: List<Int> = emptyList(),
    val claimedDailyIds: List<String> = emptyList(),
    val claimedMilestonePoints: List<Int> = emptyList(),
    val upgradesCompleted: Int = 0,
    val techsResearched: Int = 0,
    val gachaPullsCount: Int = 0,
    val huntsCompleted: Int = 0,
    val battlesWon: Int = 0,
    val troopsTrained: Int = 0,
    val ordersEnacted: Int = 0
)

@JsonClass(generateAdapter = true)
data class GameState(
    val language: AppLang = AppLang.AR,
    val soundEnabled: Boolean = true,
    val resources: ResourcePack = ResourcePack(),
    val furnace: FurnaceState = FurnaceState(),
    val buildings: List<BuildingSlot> = emptyList(),
    val survivors: List<Survivor> = emptyList(),
    val chiefOrders: List<ChiefOrderState> = emptyList(),
    val techLevels: List<TechProgress> = emptyList(),
    val activeResearch: ActiveResearch? = null,
    val troops: TroopCounts = TroopCounts(),
    val trainingQueue: TroopTrainingQueue? = null,
    val heroes: List<HeroInstance> = emptyList(),
    val formationHeroIds: List<String> = emptyList(),
    val explorationStage: Int = 1,
    val worldNodes: List<WorldNode> = emptyList(),
    val marches: List<MarchTask> = emptyList(),
    val blizzard: BlizzardState = BlizzardState(),
    val quests: QuestState = QuestState(),
    val dayTimeMinutes: Int = 480, // starts 08:00 AM
    val dayNumber: Int = 1,
    val lastSavedEpochMs: Long = System.currentTimeMillis(),
    val avatarHeroId: String = "h_sergey",
    val frameStyle: String = "ROYAL_GOLD",
    val allianceId: String = "",
    val allianceTag: String = "",
    val dailyGiftLastEpochMs: Long = 0L,
    val shopPurchasesCount: Int = 0
)

data class OfflineGainSummary(
    val elapsedSeconds: Long,
    val meatGained: Int,
    val woodGained: Int,
    val coalGained: Int,
    val ironGained: Int,
    val xpGained: Int
)

data class GachaPullResult(
    val heroId: String,
    val heroNameEn: String,
    val heroNameAr: String,
    val rarity: HeroRarity,
    val isNewUnlock: Boolean,
    val shardsGained: Int,
    val accentColorHex: Long
)

// Live Exploration Battle Arena Models
data class BattleUnit(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val isAlly: Boolean,
    val isFrontline: Boolean,
    val heroClass: HeroClass,
    val rarity: HeroRarity,
    val maxHp: Int,
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val energy: Int, // 0..100
    val accentColorHex: Long,
    val skillNameEn: String,
    val skillNameAr: String
)

data class FloatingDamage(
    val id: Long,
    val targetUnitId: String,
    val amount: Int,
    val isCrit: Boolean,
    val isHeal: Boolean = false
)

enum class BattleActionType { SLASH, ARROW, LANCE_CHARGE, HEAL_RAY, ULTIMATE_NOVA }

data class BattleVisualEffect(
    val id: Long,
    val attackerId: String,
    val targetId: String,
    val actionType: BattleActionType,
    val colorHex: Long,
    val isCrit: Boolean = false
)

enum class BattleOutcome { ONGOING, VICTORY, DEFEAT }

data class BattleSession(
    val stage: Int,
    val enemyTitleEn: String,
    val enemyTitleAr: String,
    val units: List<BattleUnit>,
    val autoSkill: Boolean = true,
    val outcome: BattleOutcome = BattleOutcome.ONGOING,
    val floatingDamages: List<FloatingDamage> = emptyList(),
    val activeEffects: List<BattleVisualEffect> = emptyList(),
    val lungingUnitIds: Set<String> = emptySet(),
    val hitUnitIds: Set<String> = emptySet(),
    val tickSeq: Int = 0,
    val combatLogEn: String = "",
    val combatLogAr: String = "",
    val activeFlashTextEn: String? = null,
    val activeFlashTextAr: String? = null,
    val activeFlashColorHex: Long = 0xFF38BDF8,
    val rewardGems: Int = 0,
    val rewardXp: Int = 0,
    val rewardMeat: Int = 0,
    val rewardKey: Boolean = false
)

enum class ShopCategory { SPEEDUPS_KEYS, RESOURCES, BUNDLES }

data class ShopItemDef(
    val id: String,
    val category: ShopCategory,
    val titleEn: String,
    val titleAr: String,
    val descEn: String,
    val descAr: String,
    val gemCost: Int,
    val meatGain: Int = 0,
    val woodGain: Int = 0,
    val coalGain: Int = 0,
    val ironGain: Int = 0,
    val speedupMinGain: Int = 0,
    val silverKeyGain: Int = 0,
    val goldKeyGain: Int = 0,
    val heroXpGain: Int = 0,
    val badgeColorHex: Long = 0xFFF6C453
)

data class NpcCitadelInfo(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val allianceTag: String,
    val furnaceLevel: Int,
    val power: Int,
    val posX: Float,
    val posY: Float,
    val bannerColorHex: Long,
    val rewardCoal: Int,
    val rewardWood: Int,
    val rewardMeat: Int,
    val rewardXp: Int
)

enum class FlyResourceType { MEAT, WOOD, COAL, IRON, GEMS }

data class ResourceFlyEvent(
    val id: Long,
    val type: FlyResourceType,
    val originX: Float,
    val originY: Float,
    val amount: Int,
    val isConsumption: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)


