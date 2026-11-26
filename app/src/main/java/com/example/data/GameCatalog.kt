package com.example.data

import com.example.model.*
import kotlin.math.pow
import kotlin.math.roundToInt

data class ResourceCost(
    val meat: Int = 0,
    val wood: Int = 0,
    val coal: Int = 0,
    val iron: Int = 0,
    val durationSec: Int = 10
)

data class TechMeta(
    val id: TechId,
    val branch: TechBranch,
    val maxLevel: Int = 5
)

data class ChapterMissionDef(
    val id: String,
    val chapter: Int,
    val titleEn: String,
    val titleAr: String,
    val rewardGems: Int,
    val rewardMeat: Int,
    val rewardWood: Int,
    val rewardCoal: Int,
    val rewardKeys: Int = 0,
    val targetFocus: String = "b_coal",
    val progress: (GameState) -> Pair<Int, Int>
)

data class ChapterChestReward(
    val chapter: Int,
    val gems: Int,
    val goldKeys: Int,
    val speedupsMin: Int,
    val heroXp: Int
)

data class DailyQuestDef(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val points: Int,
    val rewardGems: Int,
    val rewardXp: Int,
    val progress: (GameState) -> Pair<Int, Int>
)

object GameCatalog {

    val techMetas = listOf(
        TechMeta(TechId.EFFICIENT_MINING, TechBranch.ECONOMY),
        TechMeta(TechId.TIMBER_REFINING, TechBranch.ECONOMY),
        TechMeta(TechId.GOURMET_RATIONS, TechBranch.ECONOMY),
        TechMeta(TechId.INSULATED_WALLS, TechBranch.GROWTH),
        TechMeta(TechId.RAPID_CONSTRUCTION, TechBranch.GROWTH),
        TechMeta(TechId.MEDICAL_HERBS, TechBranch.GROWTH),
        TechMeta(TechId.STEEL_FORGING, TechBranch.BATTLE),
        TechMeta(TechId.HERO_TACTICS, TechBranch.BATTLE),
        TechMeta(TechId.FROST_RESISTANCE, TechBranch.BATTLE)
    )

    fun initialState(): GameState {
        val buildings = listOf(
            // Inner ring around Solar Obelisk (0.5f, 0.48f)
            BuildingSlot("b_shelter_1", BuildingType.SHELTER, 0.25f, 0.36f, level = 1),
            BuildingSlot("b_cookhouse", BuildingType.COOKHOUSE, 0.74f, 0.36f, level = 1, assignedWorkers = 1),
            BuildingSlot("b_clinic", BuildingType.CLINIC, 0.23f, 0.61f, level = 1, assignedWorkers = 1),
            BuildingSlot("b_lab", BuildingType.RESEARCH_LAB, 0.76f, 0.61f, level = 0),
            // Outer production & military ring
            BuildingSlot("b_coal", BuildingType.COAL_MINE, 0.16f, 0.18f, level = 1, assignedWorkers = 2),
            BuildingSlot("b_sawmill", BuildingType.SAWMILL, 0.50f, 0.15f, level = 1, assignedWorkers = 2),
            BuildingSlot("b_hunter", BuildingType.HUNTERS_HUT, 0.84f, 0.18f, level = 1, assignedWorkers = 2),
            BuildingSlot("b_iron", BuildingType.IRON_MINE, 0.16f, 0.82f, level = 0),
            BuildingSlot("b_barracks", BuildingType.BARRACKS, 0.50f, 0.83f, level = 1),
            BuildingSlot("b_watchtower", BuildingType.WATCHTOWER, 0.84f, 0.82f, level = 1)
        )

        val survivors = listOf(
            Survivor("s1", "Mina", "مينا", assignedBuildingId = "b_coal", posX = 0.20f, posY = 0.22f, targetX = 0.16f, targetY = 0.18f),
            Survivor("s2", "Nefertiti", "نفرتيتي", assignedBuildingId = "b_coal", posX = 0.18f, posY = 0.25f, targetX = 0.16f, targetY = 0.18f),
            Survivor("s3", "Tarek", "طارق", assignedBuildingId = "b_sawmill", posX = 0.48f, posY = 0.20f, targetX = 0.50f, targetY = 0.15f),
            Survivor("s4", "Hatshepsut", "حتشبسوت", assignedBuildingId = "b_sawmill", posX = 0.52f, posY = 0.20f, targetX = 0.50f, targetY = 0.15f),
            Survivor("s5", "Omar", "عمر", assignedBuildingId = "b_hunter", posX = 0.80f, posY = 0.22f, targetX = 0.84f, targetY = 0.18f),
            Survivor("s6", "Layla", "ليلى", assignedBuildingId = "b_hunter", posX = 0.82f, posY = 0.24f, targetX = 0.84f, targetY = 0.18f),
            Survivor("s7", "Zayd", "زيد", assignedBuildingId = "b_cookhouse", posX = 0.72f, posY = 0.38f, targetX = 0.74f, targetY = 0.36f),
            Survivor("s8", "Hana", "هناء", assignedBuildingId = "b_clinic", posX = 0.27f, posY = 0.59f, targetX = 0.23f, targetY = 0.61f)
        )

        val orders = ChiefOrderType.entries.map { ChiefOrderState(type = it) }
        val techs = TechId.entries.map { TechProgress(id = it, level = 0) }
        val heroes = initialHeroes()

        return GameState(
            language = AppLang.AR,
            soundEnabled = true,
            resources = ResourcePack(),
            furnace = FurnaceState(level = 1, overdrive = false),
            buildings = buildings,
            survivors = survivors,
            chiefOrders = orders,
            techLevels = techs,
            troops = TroopCounts(infantry = 35, lancers = 25, marksmen = 25),
            heroes = heroes,
            formationHeroIds = listOf("h_sergey", "h_bahiti", "h_molly"),
            explorationStage = 1,
            worldNodes = generateWorldNodes(),
            blizzard = BlizzardState(isActive = false, countdownSec = 95),
            quests = QuestState(chapter = 1)
        )
    }

    fun initialHeroes(): List<HeroInstance> = listOf(
        HeroInstance(
            id = "h_sergey",
            nameEn = "King Ramses II", nameAr = "الملك رمسيس الثاني",
            titleEn = "Pharaoh of Kadesh", titleAr = "فرعون قادش المظفر",
            heroClass = HeroClass.INFANTRY, rarity = HeroRarity.EPIC,
            unlocked = true, level = 5, stars = 2, shards = 4, gearLevel = 2,
            skillNameEn = "Chariot Aegis of Kadesh", skillNameAr = "درع قادش الملكي",
            skillDescEn = "Raises golden sun-frost shields and slams the enemy vanguard.",
            skillDescAr = "يرفع دروع الشمس المقدسة للحلفاء ويضرب طليعة العدو.",
            accentColorHex = 0xFF38BDF8
        ),
        HeroInstance(
            id = "h_bahiti",
            nameEn = "King Ahmose I", nameAr = "الملك أحمس الأول",
            titleEn = "Liberator of Upper Egypt", titleAr = "محرر صعيد مصر",
            heroClass = HeroClass.MARKSMAN, rarity = HeroRarity.EPIC,
            unlocked = true, level = 5, stars = 1, shards = 3, gearLevel = 2,
            skillNameEn = "Piercing Sun Arrow", skillNameAr = "سهم الشمس المحرر",
            skillDescEn = "Fires a high-crit solar-frost arrow at the weakest enemy.",
            skillDescAr = "يطلق سهماً شمسياً بضرر حرج على أضعف عدو.",
            accentColorHex = 0xFF60A5FA
        ),
        HeroInstance(
            id = "h_molly",
            nameEn = "Queen Nefertiti", nameAr = "الملكة نفرتيتي",
            titleEn = "Empress of the Frost Nile", titleAr = "إمبراطورة النيل المتجمد",
            heroClass = HeroClass.LANCER, rarity = HeroRarity.MYTHIC,
            unlocked = true, level = 6, stars = 2, shards = 2, gearLevel = 2,
            skillNameEn = "Nile Frost Tempest", skillNameAr = "إعصار النيل المتجمد",
            skillDescEn = "AoE turquoise frost whirlwind sweeping across all enemies.",
            skillDescAr = "إعصار فيروزي شامل يكتسح جميع الأعداء في الساحة.",
            accentColorHex = 0xFFC084FC
        ),
        HeroInstance(
            id = "h_patrick",
            nameEn = "King Djoser", nameAr = "الملك زوسر",
            titleEn = "Architect of Saqqara", titleAr = "باني هرم سقارة",
            heroClass = HeroClass.INFANTRY, rarity = HeroRarity.RARE,
            unlocked = false, level = 1, stars = 1, shards = 2, gearLevel = 1,
            skillNameEn = "Saqqara Healing Elixir", skillNameAr = "إكسير سقارة الملكي",
            skillDescEn = "Heals all allied kings and guards with warm temple herbs.",
            skillDescAr = "يعالج جميع الحلفاء بأعشاب المعبد الدافئة.",
            accentColorHex = 0xFF2DD4BF
        ),
        HeroInstance(
            id = "h_zinman",
            nameEn = "King Khufu", nameAr = "الملك خوفو",
            titleEn = "Sovereign of the Great Pyramid", titleAr = "صاحب الهرم الأكبر",
            heroClass = HeroClass.MARKSMAN, rarity = HeroRarity.EPIC,
            unlocked = false, level = 1, stars = 1, shards = 0, gearLevel = 1,
            skillNameEn = "Giza Solar Barrage", skillNameAr = "وابل الجيزة النحاسي",
            skillDescEn = "Launches clockwork brass bolts across enemy backlines.",
            skillDescAr = "يطلق قذائف نحاسية متفجرة على الصف الخلفي للعدو.",
            accentColorHex = 0xFFFB923C
        ),
        HeroInstance(
            id = "h_natalia",
            nameEn = "Queen Hatshepsut", nameAr = "الملكة حتشبسوت",
            titleEn = "Solar Queen of Karnak", titleAr = "ملكة الكرنك الذهبية",
            heroClass = HeroClass.INFANTRY, rarity = HeroRarity.MYTHIC,
            unlocked = false, level = 1, stars = 1, shards = 0, gearLevel = 1,
            skillNameEn = "Karnak Golden Cleave", skillNameAr = "صولجان الكرنك الحارق",
            skillDescEn = "Massive AoE royal cleave that shatters armor across the arena.",
            skillDescAr = "ضربة ملكية هائلة تحطم دروع جميع الأعداء.",
            accentColorHex = 0xFFF472B6
        ),
        HeroInstance(
            id = "h_flint",
            nameEn = "King Thutmose III", nameAr = "الملك تحتمس الثالث",
            titleEn = "Master of Desert Campaigns", titleAr = "فاتح الصحراء الأعظم",
            heroClass = HeroClass.INFANTRY, rarity = HeroRarity.MYTHIC,
            unlocked = false, level = 1, stars = 1, shards = 0, gearLevel = 1,
            skillNameEn = "Megiddo Solar Nova", skillNameAr = "انفجار شمس مجدو",
            skillDescEn = "Unleashes scorching Obelisk flame across the entire desert.",
            skillDescAr = "يطلق نيران مسلّة الشمس الحارقة عبر ساحة المعركة.",
            accentColorHex = 0xFFF59E0B
        ),
        HeroInstance(
            id = "h_jasser",
            nameEn = "Queen Cleopatra VII", nameAr = "الملكة كليوباترا",
            titleEn = "Star of Alexandria", titleAr = "نجمة الإسكندرية",
            heroClass = HeroClass.MARKSMAN, rarity = HeroRarity.RARE,
            unlocked = false, level = 1, stars = 1, shards = 4, gearLevel = 1,
            skillNameEn = "Alexandria Twin-Volley", skillNameAr = "رشقة الإسكندرية المزدوجة",
            skillDescEn = "Precision twin-shot targeting the highest-threat foe.",
            skillDescAr = "طلقة مزدوجة دقيقة ضد أخطر الأعداء.",
            accentColorHex = 0xFF38BDF8
        )
    )

    fun normalizeHeroesForRoyalNames(savedHeroes: List<HeroInstance>): List<HeroInstance> {
        val canonicalById = initialHeroes().associateBy { it.id }
        val updated = savedHeroes.map { hero ->
            val ref = canonicalById[hero.id] ?: return@map hero
            hero.copy(
                nameEn = ref.nameEn,
                nameAr = ref.nameAr,
                titleEn = ref.titleEn,
                titleAr = ref.titleAr,
                heroClass = ref.heroClass,
                rarity = ref.rarity,
                skillNameEn = ref.skillNameEn,
                skillNameAr = ref.skillNameAr,
                skillDescEn = ref.skillDescEn,
                skillDescAr = ref.skillDescAr,
                accentColorHex = ref.accentColorHex
            )
        }
        val existingIds = updated.map { it.id }.toSet()
        val missing = canonicalById.values.filter { it.id !in existingIds }
        return updated + missing
    }

    val shopCatalog: List<ShopItemDef> = listOf(
        ShopItemDef(
            id = "shop_speedup_pack",
            category = ShopCategory.SPEEDUPS_KEYS,
            titleEn = "Pharaoh's 15m Speedup Pack", titleAr = "حزمة تسريع 15 دقيقة",
            descEn = "+15m instant Construction & Research speedups",
            descAr = "+15 دقيقة تسريع فوري للبناء والأبحاث",
            gemCost = 90,
            speedupMinGain = 15,
            badgeColorHex = 0xFF4CC9F0
        ),
        ShopItemDef(
            id = "shop_silver_keys_3",
            category = ShopCategory.SPEEDUPS_KEYS,
            titleEn = "3x Silver Temple Keys", titleAr = "3 مفاتيح معبد فضية",
            descEn = "Unlock royal hero chests & shards",
            descAr = "لفتح صناديق تجنيد الأبطال الفضية",
            gemCost = 160,
            silverKeyGain = 3,
            badgeColorHex = 0xFFBAE6FD
        ),
        ShopItemDef(
            id = "shop_gold_keys_2",
            category = ShopCategory.SPEEDUPS_KEYS,
            titleEn = "2x Golden Sun Keys", titleAr = "مفتاحان ذهبيان ملكيان",
            descEn = "High-rate Mythic King & Queen summons",
            descAr = "فرصة عالية لاستدعاء الملوك والملكات الأساطير",
            gemCost = 290,
            goldKeyGain = 2,
            badgeColorHex = 0xFFF6C453
        ),
        ShopItemDef(
            id = "shop_xp_tome",
            category = ShopCategory.RESOURCES,
            titleEn = "Royal Scribe XP Tome", titleAr = "مخطوطة خبرة الملوك",
            descEn = "+900 Hero XP to level up your Kings & Queens",
            descAr = "+900 نقطة خبرة لترقية أبطالك فوراً",
            gemCost = 120,
            heroXpGain = 900,
            badgeColorHex = 0xFF10B981
        ),
        ShopItemDef(
            id = "shop_coal_crate",
            category = ShopCategory.RESOURCES,
            titleEn = "Hathor Coal Supply Crate", titleAr = "صندوق فحم حتحور",
            descEn = "+650 Coal to sustain Solar Obelisk Overdrive",
            descAr = "+650 فحم لتغذية مسلّة الشمس خلال العواصف",
            gemCost = 85,
            coalGain = 650,
            badgeColorHex = 0xFFF97316
        ),
        ShopItemDef(
            id = "shop_wood_iron_crate",
            category = ShopCategory.RESOURCES,
            titleEn = "Nile Quarry Material Crate", titleAr = "صندوق أخشاب وحديد النيل",
            descEn = "+700 Wood & +450 Nubian Iron",
            descAr = "+700 خشب و+450 حديد لتطوير القلعة",
            gemCost = 115,
            woodGain = 700,
            ironGain = 450,
            badgeColorHex = 0xFF2EC4B6
        ),
        ShopItemDef(
            id = "shop_meat_feast",
            category = ShopCategory.RESOURCES,
            titleEn = "Royal Delta Feast Crate", titleAr = "صندوق مؤونة الدلتا",
            descEn = "+800 Meat for troops & citizen feasts",
            descAr = "+800 لحم لتدريب الحرس وإطعام الرعايا",
            gemCost = 80,
            meatGain = 800,
            badgeColorHex = 0xFFEF4444
        ),
        ShopItemDef(
            id = "shop_bundle_warlord",
            category = ShopCategory.BUNDLES,
            titleEn = "Conqueror of Kadesh Bundle", titleAr = "باقة فاتح قادش الكبرى",
            descEn = "+2 Gold Keys, +1200 Hero XP, +20m Speedups, +500 Iron",
            descAr = "+2 مفتاح ذهبي، +1200 خبرة، +20د تسريع، +500 حديد",
            gemCost = 420,
            goldKeyGain = 2,
            heroXpGain = 1200,
            speedupMinGain = 20,
            ironGain = 500,
            badgeColorHex = 0xFFC084FC
        ),
        ShopItemDef(
            id = "shop_bundle_obelisk",
            category = ShopCategory.BUNDLES,
            titleEn = "Solar Obelisk Survival Pack", titleAr = "باقة بقاء مسلّة الشمس",
            descEn = "+1000 Wood, +900 Coal, +900 Meat, +10m Speedups",
            descAr = "+1000 خشب، +900 فحم، +900 لحم، +10د تسريع",
            gemCost = 220,
            woodGain = 1000,
            coalGain = 900,
            meatGain = 900,
            speedupMinGain = 10,
            badgeColorHex = 0xFFF6C453
        )
    )

    val npcCitadels: List<NpcCitadelInfo> = listOf(
        NpcCitadelInfo(
            id = "npc_kmt",
            nameEn = "Memphis Bastion", nameAr = "حصن منف الملكي",
            allianceTag = "KMT",
            furnaceLevel = 4, power = 3200,
            posX = 0.24f, posY = 0.36f,
            bannerColorHex = 0xFFF6C453,
            rewardCoal = 220, rewardWood = 260, rewardMeat = 180, rewardXp = 180
        ),
        NpcCitadelInfo(
            id = "npc_anb",
            nameEn = "Anubis Watch", nameAr = "معقل أنوبيس",
            allianceTag = "ANB",
            furnaceLevel = 5, power = 5400,
            posX = 0.77f, posY = 0.34f,
            bannerColorHex = 0xFFC084FC,
            rewardCoal = 320, rewardWood = 340, rewardMeat = 260, rewardXp = 260
        ),
        NpcCitadelInfo(
            id = "npc_sob",
            nameEn = "Sobek Citadel", nameAr = "قلعة سوبك النيلية",
            allianceTag = "SOB",
            furnaceLevel = 3, power = 2400,
            posX = 0.28f, posY = 0.76f,
            bannerColorHex = 0xFF2EC4B6,
            rewardCoal = 180, rewardWood = 210, rewardMeat = 220, rewardXp = 150
        )
    )

    fun citadelCoordsFor(userId: String): Pair<Float, Float> {
        val slots = listOf(
            0.73f to 0.62f,
            0.16f to 0.50f,
            0.84f to 0.48f,
            0.38f to 0.24f,
            0.64f to 0.26f,
            0.39f to 0.80f,
            0.66f to 0.78f,
            0.16f to 0.16f
        )
        val idx = (userId.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) % slots.size
        return slots[idx]
    }

    fun generateWorldNodes(): List<WorldNode> = listOf(
        WorldNode("wn_bear_1", WorldNodeType.POLAR_BEAR, 1, 0.22f, 0.22f, 450, 1),
        WorldNode("wn_bear_2", WorldNodeType.POLAR_BEAR, 3, 0.79f, 0.20f, 950, 1),
        WorldNode("wn_beast_1", WorldNodeType.FROST_BEAST, 5, 0.17f, 0.66f, 1650, 1),
        WorldNode("wn_beast_2", WorldNodeType.FROST_BEAST, 7, 0.81f, 0.72f, 2600, 1),
        WorldNode("wn_yeti", WorldNodeType.YETI_BOSS, 10, 0.50f, 0.15f, 4200, 1),
        WorldNode("wn_coal", WorldNodeType.COAL_VEIN, 2, 0.34f, 0.50f, 200, 320),
        WorldNode("wn_wood", WorldNodeType.ANCIENT_TIMBER, 2, 0.69f, 0.48f, 200, 360),
        WorldNode("wn_iron", WorldNodeType.IRON_DEPOSIT, 4, 0.50f, 0.82f, 400, 220)
    )

    fun furnaceUpgradeCost(currentLevel: Int): ResourceCost {
        val mult = 1.55.pow(currentLevel - 1)
        return ResourceCost(
            wood = (160 * mult).roundToInt(),
            coal = (110 * mult).roundToInt(),
            iron = if (currentLevel >= 2) (75 * mult).roundToInt() else 0,
            durationSec = 18 + currentLevel * 12
        )
    }

    fun buildingUpgradeCost(type: BuildingType, currentLevel: Int): ResourceCost {
        val baseWood = when (type) {
            BuildingType.SHELTER -> 75
            BuildingType.COAL_MINE -> 80
            BuildingType.SAWMILL -> 70
            BuildingType.HUNTERS_HUT -> 75
            BuildingType.IRON_MINE -> 110
            BuildingType.COOKHOUSE -> 85
            BuildingType.CLINIC -> 95
            BuildingType.RESEARCH_LAB -> 125
            BuildingType.BARRACKS -> 115
            BuildingType.WATCHTOWER -> 100
        }
        val mult = 1.48.pow(currentLevel)
        return ResourceCost(
            wood = (baseWood * mult).roundToInt(),
            coal = ((baseWood * 0.45) * mult).roundToInt(),
            iron = if (currentLevel >= 2 || type == BuildingType.IRON_MINE || type == BuildingType.RESEARCH_LAB)
                ((baseWood * 0.35) * mult).roundToInt() else 0,
            durationSec = 10 + currentLevel * 7
        )
    }

    fun gearUpgradeCost(currentGearLevel: Int): ResourceCost {
        val mult = 1.4.pow(currentGearLevel - 1)
        return ResourceCost(
            wood = (65 * mult).roundToInt(),
            iron = (35 * mult).roundToInt(),
            coal = (40 * mult).roundToInt()
        )
    }

    fun techResearchCost(id: TechId, currentLevel: Int): ResourceCost {
        val mult = 1.55.pow(currentLevel)
        val baseIron = if (techMetas.first { it.id == id }.branch == TechBranch.BATTLE) 90 else 55
        return ResourceCost(
            wood = (95 * mult).roundToInt(),
            coal = (75 * mult).roundToInt(),
            iron = (baseIron * mult).roundToInt(),
            durationSec = 14 + currentLevel * 10
        )
    }

    fun maxWorkersFor(slot: BuildingSlot): Int = when (slot.type) {
        BuildingType.COAL_MINE, BuildingType.SAWMILL, BuildingType.HUNTERS_HUT, BuildingType.IRON_MINE ->
            if (slot.level == 0) 0 else 2 + slot.level / 2
        BuildingType.COOKHOUSE, BuildingType.CLINIC ->
            if (slot.level == 0) 0 else 1 + slot.level / 3
        else -> 0
    }

    fun maxSurvivorCapacity(state: GameState): Int {
        val shelterLevels = state.buildings.filter { it.type == BuildingType.SHELTER }.sumOf { it.level }
        return 6 + shelterLevels * 3
    }

    fun maxMarchSlots(state: GameState): Int {
        val wtLevel = state.buildings.firstOrNull { it.type == BuildingType.WATCHTOWER }?.level ?: 1
        return 1 + (wtLevel / 2)
    }

    fun techLevel(state: GameState, id: TechId): Int =
        state.techLevels.firstOrNull { it.id == id }?.level ?: 0

    fun computeRatesPerMinute(state: GameState): ResourcePack {
        val miningBonus = 1.0 + 0.15 * techLevel(state, TechId.EFFICIENT_MINING)
        val timberBonus = 1.0 + 0.15 * techLevel(state, TechId.TIMBER_REFINING)
        val rationBonus = 1.0 + 0.15 * techLevel(state, TechId.GOURMET_RATIONS)
        val mobActive = state.chiefOrders.any { it.type == ChiefOrderType.URGENT_MOBILIZATION && it.activeRemainingSec > 0 }
        val orderMult = if (mobActive) 1.45 else 1.0

        val avgMorale = state.survivors.filter { it.healthState == SurvivorHealthState.HEALTHY }
            .map { it.happiness }.average().takeIf { !it.isNaN() } ?: 75.0
        val moraleMult = (0.75 + (avgMorale / 200.0)).coerceIn(0.6, 1.25)

        var coalProd = 0.0
        var woodProd = 0.0
        var meatProd = 0.0
        var ironProd = 0.0

        for (b in state.buildings) {
            if (b.level <= 0) continue
            val workerFactor = (0.4 + b.assignedWorkers * 0.55)
            val gearMult = 1.0 + (b.toolLevel - 1) * 0.18
            val lvlMult = 1.0 + (b.level - 1) * 0.35
            val base = 18.0 * lvlMult * workerFactor * gearMult * orderMult * moraleMult
            when (b.type) {
                BuildingType.COAL_MINE -> coalProd += base * miningBonus
                BuildingType.SAWMILL -> woodProd += base * 1.15 * timberBonus
                BuildingType.HUNTERS_HUT -> meatProd += base * 1.1 * rationBonus
                BuildingType.IRON_MINE -> ironProd += base * 0.72 * miningBonus
                else -> {}
            }
        }

        val coalBurn = computeCoalBurnPerMinute(state)
        return ResourcePack(
            meat = meatProd,
            wood = woodProd,
            coal = coalProd - coalBurn,
            iron = ironProd
        )
    }

    fun computeCoalBurnPerMinute(state: GameState): Double {
        val insulation = 1.0 - (0.06 * techLevel(state, TechId.INSULATED_WALLS)).coerceAtMost(0.35)
        val baseBurn = (8.0 + state.furnace.level * 3.5) * insulation
        val overdriveMult = if (state.furnace.overdrive) 2.25 else 1.0
        val blizzardMult = if (state.blizzard.isActive) 1.55 else 1.0
        return baseBurn * overdriveMult * blizzardMult
    }

    fun computeAmbientTempC(state: GameState): Int {
        val dayFrac = state.dayTimeMinutes / 1440f
        val nightDrop = if (dayFrac < 0.25f || dayFrac > 0.80f) -8 else 0
        val base = -20 + nightDrop - (state.blizzard.stormCount * 2).coerceAtMost(10)
        return if (state.blizzard.isActive) base - 42 else base
    }

    fun computeCampTempC(state: GameState): Int {
        val ambient = computeAmbientTempC(state)
        val furnaceHeat = 14 + state.furnace.level * 5 + (if (state.furnace.overdrive) 24 else 0)
        val shelterHeat = (state.buildings.filter { it.type == BuildingType.SHELTER }.sumOf { it.level }) * 2
        val techHeat = techLevel(state, TechId.INSULATED_WALLS) * 3
        val coalPenalty = if (state.resources.coal <= 0.5) -18 else 0
        return (ambient + furnaceHeat + shelterHeat + techHeat + coalPenalty).coerceAtMost(26)
    }

    fun heroAttack(hero: HeroInstance, state: GameState): Int {
        val rarityMult = when (hero.rarity) {
            HeroRarity.RARE -> 1.0
            HeroRarity.EPIC -> 1.25
            HeroRarity.MYTHIC -> 1.55
        }
        val techMult = 1.0 + 0.14 * techLevel(state, TechId.STEEL_FORGING)
        return ((42 + hero.level * 14 + hero.stars * 28 + hero.gearLevel * 18) * rarityMult * techMult).roundToInt()
    }

    fun heroMaxHp(hero: HeroInstance, state: GameState): Int {
        val classMult = when (hero.heroClass) {
            HeroClass.INFANTRY -> 1.45
            HeroClass.LANCER -> 1.1
            HeroClass.MARKSMAN -> 0.88
        }
        val rarityMult = when (hero.rarity) {
            HeroRarity.RARE -> 1.0
            HeroRarity.EPIC -> 1.25
            HeroRarity.MYTHIC -> 1.52
        }
        val techMult = 1.0 + 0.14 * techLevel(state, TechId.HERO_TACTICS)
        return ((260 + hero.level * 62 + hero.stars * 120 + hero.gearLevel * 75) * classMult * rarityMult * techMult).roundToInt()
    }

    fun heroDefense(hero: HeroInstance, state: GameState): Int {
        val techMult = 1.0 + 0.14 * techLevel(state, TechId.HERO_TACTICS)
        return ((14 + hero.level * 4 + hero.stars * 8 + hero.gearLevel * 6) * techMult).roundToInt()
    }

    fun heroPower(hero: HeroInstance, state: GameState): Int =
        heroMaxHp(hero, state) / 2 + heroAttack(hero, state) * 4 + heroDefense(hero, state) * 3

    fun computeTotalPower(state: GameState): Int {
        val furnacePower = state.furnace.level * 450
        val buildingPower = state.buildings.sumOf { it.level * 180 + (it.heaterLevel + it.toolLevel - 2) * 55 }
        val troopPower = state.troops.infantry * 12 + state.troops.lancers * 15 + state.troops.marksmen * 16
        val heroPower = state.heroes.filter { it.unlocked }.sumOf { heroPower(it, state) }
        val techPower = state.techLevels.sumOf { it.level * 220 }
        val frostMult = 1.0 + 0.12 * techLevel(state, TechId.FROST_RESISTANCE)
        return ((furnacePower + buildingPower + troopPower + heroPower + techPower) * frostMult).roundToInt()
    }

    fun computeDevelopmentScore(state: GameState): Int {
        val furnaceScore = state.furnace.level * 500
        val buildingScore = state.buildings.sumOf { it.level * 120 + (it.heaterLevel + it.toolLevel - 2) * 45 }
        val techScore = state.techLevels.sumOf { it.level * 100 }
        return (furnaceScore + buildingScore + techScore).coerceIn(0, 100_000_000)
    }

    fun chapterChestReward(chapter: Int): ChapterChestReward = ChapterChestReward(
        chapter = chapter,
        gems = 180 + chapter * 90,
        goldKeys = 1 + (if (chapter >= 3) 1 else 0),
        speedupsMin = 6 + chapter * 3,
        heroXp = 220 + chapter * 140
    )

    val chapterMissions = listOf(
        ChapterMissionDef(
            id = "c1_coal", chapter = 1,
            titleEn = "Upgrade Hathor Coal Mine to Lv.2", titleAr = "ترقية منجم فحم حتحور للمستوى 2",
            rewardGems = 80, rewardMeat = 120, rewardWood = 120, rewardCoal = 100,
            targetFocus = "b_coal"
        ) { s -> (s.buildings.firstOrNull { it.type == BuildingType.COAL_MINE }?.level ?: 0).coerceAtMost(2) to 2 },
        ChapterMissionDef(
            id = "c1_furnace", chapter = 1,
            titleEn = "Upgrade Solar Obelisk to Lv.2", titleAr = "ترقية مسلّة الشمس للمستوى 2",
            rewardGems = 120, rewardMeat = 150, rewardWood = 150, rewardCoal = 140, rewardKeys = 1,
            targetFocus = "furnace"
        ) { s -> s.furnace.level.coerceAtMost(2) to 2 },
        ChapterMissionDef(
            id = "c1_explore", chapter = 1,
            titleEn = "Clear Desert Exploration Stage 2", titleAr = "اجتياز معركة الصحراء مرحلة 2",
            rewardGems = 100, rewardMeat = 140, rewardWood = 100, rewardCoal = 100, rewardKeys = 1,
            targetFocus = "explore"
        ) { s -> (s.explorationStage - 1).coerceAtMost(2) to 2 },

        ChapterMissionDef(
            id = "c2_lab", chapter = 2,
            titleEn = "Construct Thoth Observatory (Lv.1)", titleAr = "تشييد مرصد تحوت للأبحاث (مستوى 1)",
            rewardGems = 120, rewardMeat = 160, rewardWood = 200, rewardCoal = 150,
            targetFocus = "b_lab"
        ) { s -> (s.buildings.firstOrNull { it.type == BuildingType.RESEARCH_LAB }?.level ?: 0).coerceAtMost(1) to 1 },
        ChapterMissionDef(
            id = "c2_iron", chapter = 2,
            titleEn = "Construct Pharaonic Iron Foundry (Lv.1)", titleAr = "تشييد مسبك الحديد الفرعوني (مستوى 1)",
            rewardGems = 130, rewardMeat = 180, rewardWood = 220, rewardCoal = 160,
            targetFocus = "b_iron"
        ) { s -> (s.buildings.firstOrNull { it.type == BuildingType.IRON_MINE }?.level ?: 0).coerceAtMost(1) to 1 },
        ChapterMissionDef(
            id = "c2_hunt", chapter = 2,
            titleEn = "Complete 1 Nile Map Expedition", titleAr = "إتمام حملة صيد أو جمع على خريطة النيل",
            rewardGems = 150, rewardMeat = 200, rewardWood = 200, rewardCoal = 180, rewardKeys = 1,
            targetFocus = "world"
        ) { s -> s.quests.huntsCompleted.coerceAtMost(1) to 1 },

        ChapterMissionDef(
            id = "c3_furnace3", chapter = 3,
            titleEn = "Upgrade Solar Obelisk to Lv.3", titleAr = "ترقية مسلّة الشمس للمستوى 3",
            rewardGems = 200, rewardMeat = 250, rewardWood = 300, rewardCoal = 250, rewardKeys = 2,
            targetFocus = "furnace"
        ) { s -> s.furnace.level.coerceAtMost(3) to 3 },
        ChapterMissionDef(
            id = "c3_tech", chapter = 3,
            titleEn = "Research 2 Observatory Wisdoms", titleAr = "إتمام بحثين في مرصد تحوت",
            rewardGems = 160, rewardMeat = 220, rewardWood = 240, rewardCoal = 220,
            targetFocus = "tech"
        ) { s -> s.quests.techsResearched.coerceAtMost(2) to 2 },
        ChapterMissionDef(
            id = "c3_explore5", chapter = 3,
            titleEn = "Clear Desert Exploration Stage 5", titleAr = "اجتياز معركة الصحراء مرحلة 5",
            rewardGems = 220, rewardMeat = 260, rewardWood = 260, rewardCoal = 240, rewardKeys = 2,
            targetFocus = "explore"
        ) { s -> (s.explorationStage - 1).coerceAtMost(5) to 5 }
    )

    val dailyQuests = listOf(
        DailyQuestDef(
            id = "dq_upgrades",
            titleEn = "Complete 2 Temple Upgrades", titleAr = "إتمام ترقيتين للمباني والمعابد",
            points = 25, rewardGems = 40, rewardXp = 120
        ) { s -> s.quests.upgradesCompleted.coerceAtMost(2) to 2 },
        DailyQuestDef(
            id = "dq_gacha",
            titleEn = "Recruit 1 Hero from Temple Chests", titleAr = "تجنيد بطل من صناديق المعبد",
            points = 25, rewardGems = 50, rewardXp = 150
        ) { s -> s.quests.gachaPullsCount.coerceAtMost(1) to 1 },
        DailyQuestDef(
            id = "dq_battle",
            titleEn = "Win 2 Exploration Battles", titleAr = "الفوز في معركتي استكشاف",
            points = 25, rewardGems = 50, rewardXp = 180
        ) { s -> s.quests.battlesWon.coerceAtMost(2) to 2 },
        DailyQuestDef(
            id = "dq_hunt",
            titleEn = "Complete 2 Nile Expeditions", titleAr = "إتمام حملتين في خريطة النيل",
            points = 25, rewardGems = 60, rewardXp = 160
        ) { s -> s.quests.huntsCompleted.coerceAtMost(2) to 2 }
    )
}
