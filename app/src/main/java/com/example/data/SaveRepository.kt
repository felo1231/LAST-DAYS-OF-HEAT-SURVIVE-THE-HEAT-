package com.example.data

import android.content.Context
import com.example.model.GameState
import com.example.model.OfflineGainSummary
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import kotlin.math.roundToInt

class SaveRepository(context: Context, userId: String? = null) {
    private val fileName = if (userId.isNullOrBlank()) {
        "frosthold_save_v1.json"
    } else {
        "frosthold_save_${userId}_v1.json"
    }
    private val saveFile = File(context.applicationContext.filesDir, fileName)

    companion object {
        private val moshi: Moshi by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
        }
        private val adapter by lazy { moshi.adapter(GameState::class.java) }
    }

    fun hasLocalSave(): Boolean = saveFile.exists()

    fun load(): GameState {
        if (!saveFile.exists()) return GameCatalog.initialState()
        return runCatching {
            val raw = saveFile.readText()
            val parsed = adapter.fromJson(raw) ?: return@runCatching GameCatalog.initialState()
            parsed.copy(heroes = GameCatalog.normalizeHeroesForRoyalNames(parsed.heroes))
        }.getOrElse {
            GameCatalog.initialState()
        }
    }

    fun save(state: GameState) {
        runCatching {
            val stamped = state.copy(lastSavedEpochMs = System.currentTimeMillis())
            saveFile.writeText(adapter.toJson(stamped))
        }
    }

    fun exportJson(state: GameState): String {
        val stamped = state.copy(lastSavedEpochMs = System.currentTimeMillis())
        return adapter.toJson(stamped)
    }

    fun importJson(json: String): GameState? {
        return runCatching {
            val parsed = adapter.fromJson(json.trim()) ?: return null
            if (parsed.buildings.isEmpty() || parsed.heroes.isEmpty()) return null
            val migrated = parsed.copy(heroes = GameCatalog.normalizeHeroesForRoyalNames(parsed.heroes))
            save(migrated)
            migrated
        }.getOrNull()
    }

    fun reset(): GameState {
        val fresh = GameCatalog.initialState()
        save(fresh)
        return fresh
    }

    fun calculateOfflineGains(state: GameState, nowMs: Long = System.currentTimeMillis()): Pair<GameState, OfflineGainSummary?> {
        val elapsedSec = ((nowMs - state.lastSavedEpochMs) / 1000L).coerceIn(0L, 12 * 3600L)
        if (elapsedSec < 30L) return state to null

        val minutes = elapsedSec / 60.0
        val rates = GameCatalog.computeRatesPerMinute(state)
        val meatGained = (rates.meat * minutes * 0.7).roundToInt().coerceAtLeast(0)
        val woodGained = (rates.wood * minutes * 0.7).roundToInt().coerceAtLeast(0)
        val coalGained = (rates.coal.coerceAtLeast(3.0) * minutes * 0.55).roundToInt().coerceAtLeast(0)
        val ironGained = (rates.iron * minutes * 0.7).roundToInt().coerceAtLeast(0)
        val xpGained = (minutes * 6.0).roundToInt().coerceAtLeast(5)

        val updated = state.copy(
            resources = state.resources.copy(
                meat = state.resources.meat + meatGained,
                wood = state.resources.wood + woodGained,
                coal = state.resources.coal + coalGained,
                iron = state.resources.iron + ironGained,
                heroXp = state.resources.heroXp + xpGained
            ),
            lastSavedEpochMs = nowMs
        )
        return updated to OfflineGainSummary(
            elapsedSeconds = elapsedSec,
            meatGained = meatGained,
            woodGained = woodGained,
            coalGained = coalGained,
            ironGained = ironGained,
            xpGained = xpGained
        )
    }
}
