package com.example.data

import com.example.model.AllianceInfo
import com.example.model.ChatMessage
import com.example.model.GameState
import com.example.model.LeaderboardEntry
import com.example.model.WorldCitadel
import com.example.model.gameStateFromFirestoreMap
import com.example.model.toFirestoreCreateMap
import com.example.model.toFirestoreUpdateMap
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        cont.cancel()
    }
}

class CloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun loadCloudSave(userId: String): Result<GameState?> {
        val docPath = "users/$userId/saves/main"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
            val snapshot = db.collection("users")
                .document(userId)
                .collection("saves")
                .document("main")
                .get()
                .awaitTask()
            val parsed = if (snapshot.exists()) {
                snapshot.data?.let { gameStateFromFirestoreMap(it) }
            } else {
                null
            }
            Result.success(parsed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveCloudState(userId: String, state: GameState, totalPower: Int): Result<Unit> {
        val docPath = "users/$userId/saves/main"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val docRef = db.collection("users")
                .document(userId)
                .collection("saves")
                .document("main")
            val existing = docRef.get().awaitTask()
            if (existing.exists()) {
                docRef.update(state.toFirestoreUpdateMap(totalPower)).awaitTask()
            } else {
                docRef.set(state.toFirestoreCreateMap(userId, totalPower)).awaitTask()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun loadChiefName(userId: String): Result<String?> {
        val docPath = "leaderboard/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
            val snap = db.collection("leaderboard")
                .document(userId)
                .get()
                .awaitTask()
            Result.success(snap.getString("chiefName"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertLeaderboardEntry(
        userId: String,
        chiefName: String,
        state: GameState,
        totalPower: Int,
        developmentScore: Int
    ): Result<Unit> {
        val docPath = "leaderboard/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val cleanName = chiefName.trim().take(40).ifBlank { "Pharaoh Chief" }
            val docRef = db.collection("leaderboard").document(userId)
            val existing = docRef.get().awaitTask()
            val clampedPower = totalPower.toLong().coerceIn(0L, 100_000_000L)
            val clampedDev = developmentScore.toLong().coerceIn(0L, 100_000_000L)
            val clampedFurnace = state.furnace.level.toLong().coerceIn(1L, 10L)
            val clampedChapter = state.quests.chapter.toLong().coerceIn(1L, 50L)
            val clampedStage = state.explorationStage.toLong().coerceIn(1L, 1000L)
            val cleanAvatar = state.avatarHeroId.take(40).ifBlank { "h_sergey" }
            val cleanTag = state.allianceTag.take(10)

            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "chiefName" to cleanName,
                        "avatarHeroId" to cleanAvatar,
                        "allianceTag" to cleanTag,
                        "totalPower" to clampedPower,
                        "developmentScore" to clampedDev,
                        "furnaceLevel" to clampedFurnace,
                        "chapter" to clampedChapter,
                        "explorationStage" to clampedStage,
                        "language" to state.language.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitTask()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "chiefName" to cleanName,
                        "avatarHeroId" to cleanAvatar,
                        "allianceTag" to cleanTag,
                        "totalPower" to clampedPower,
                        "developmentScore" to clampedDev,
                        "furnaceLevel" to clampedFurnace,
                        "chapter" to clampedChapter,
                        "explorationStage" to clampedStage,
                        "language" to state.language.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitTask()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertWorldCitadel(
        userId: String,
        chiefName: String,
        state: GameState,
        totalPower: Int
    ): Result<Unit> {
        val docPath = "world_citadels/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val cleanName = chiefName.trim().take(40).ifBlank { "Pharaoh Chief" }
            val docRef = db.collection("world_citadels").document(userId)
            val existing = docRef.get().awaitTask()
            val (mx, my) = GameCatalog.citadelCoordsFor(userId)
            val cleanAvatar = state.avatarHeroId.take(40).ifBlank { "h_sergey" }
            val cleanFrame = state.frameStyle.take(40).ifBlank { "ROYAL_GOLD" }
            val cleanAllianceId = state.allianceId.take(64)
            val cleanAllianceTag = state.allianceTag.take(10)
            val clampedFurnace = state.furnace.level.toLong().coerceIn(1L, 10L)
            val clampedPower = totalPower.toLong().coerceIn(0L, 100_000_000L)

            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "chiefName" to cleanName,
                        "avatarHeroId" to cleanAvatar,
                        "frameStyle" to cleanFrame,
                        "allianceId" to cleanAllianceId,
                        "allianceTag" to cleanAllianceTag,
                        "furnaceLevel" to clampedFurnace,
                        "totalPower" to clampedPower,
                        "mapX" to mx.toDouble(),
                        "mapY" to my.toDouble(),
                        "shieldActive" to state.blizzard.isActive,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitTask()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "chiefName" to cleanName,
                        "avatarHeroId" to cleanAvatar,
                        "frameStyle" to cleanFrame,
                        "allianceId" to cleanAllianceId,
                        "allianceTag" to cleanAllianceTag,
                        "furnaceLevel" to clampedFurnace,
                        "totalPower" to clampedPower,
                        "mapX" to mx.toDouble(),
                        "mapY" to my.toDouble(),
                        "shieldActive" to state.blizzard.isActive,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitTask()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            close(IllegalStateException("User must be signed in before observing leaderboard."))
            return@callbackFlow
        }
        val query = db.collection("leaderboard")
            .whereGreaterThanOrEqualTo("totalPower", 0)
            .orderBy("totalPower", Query.Direction.DESCENDING)
            .limit(100)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, "leaderboard")
                }
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val entries = snapshot.toObjects(
                    LeaderboardEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                trySend(entries)
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeWorldCitadels(): Flow<List<WorldCitadel>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            close(IllegalStateException("User must be signed in before observing world citadels."))
            return@callbackFlow
        }
        val query = db.collection("world_citadels")
            .whereGreaterThanOrEqualTo("totalPower", 0)
            .orderBy("totalPower", Query.Direction.DESCENDING)
            .limit(40)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, "world_citadels")
                }
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.toObjects(
                    WorldCitadel::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                trySend(items)
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeAlliances(): Flow<List<AllianceInfo>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            close(IllegalStateException("User must be signed in before observing alliances."))
            return@callbackFlow
        }
        val query = db.collection("alliances")
            .whereGreaterThanOrEqualTo("totalPower", 0)
            .orderBy("totalPower", Query.Direction.DESCENDING)
            .limit(30)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, "alliances")
                }
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.toObjects(
                    AllianceInfo::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                trySend(items)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun createAlliance(
        userId: String,
        leaderName: String,
        name: String,
        tag: String,
        description: String,
        bannerColorHex: Long,
        playerPower: Int
    ): Result<AllianceInfo> {
        val cleanName = name.trim().take(32)
        val cleanTag = tag.trim().uppercase().replace(Regex("[^A-Z0-9\u0621-\u064A]"), "").take(5)
        if (cleanName.length < 2 || cleanTag.length < 2) {
            return Result.failure(IllegalArgumentException("Alliance name and tag must be at least 2 characters."))
        }
        val safeLeader = leaderName.trim().take(40).ifBlank { "Pharaoh Chief" }
        val cleanDesc = description.trim().take(140)
        val sanitizedUid = userId.replace(Regex("[^a-zA-Z0-9_-]"), "").take(12)
        val allianceId = "al_${System.currentTimeMillis()}_$sanitizedUid"
        val docPath = "alliances/$allianceId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing create." }
            val powerLong = playerPower.toLong().coerceAtLeast(0L)
            val payload = mapOf(
                "allianceId" to allianceId,
                "name" to cleanName,
                "tag" to cleanTag,
                "description" to cleanDesc,
                "bannerColorHex" to bannerColorHex,
                "leaderUid" to userId,
                "leaderName" to safeLeader,
                "memberUids" to listOf(userId),
                "memberCount" to 1L,
                "totalPower" to powerLong,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("alliances").document(allianceId).set(payload).awaitTask()
            Result.success(
                AllianceInfo(
                    id = allianceId,
                    allianceId = allianceId,
                    name = cleanName,
                    tag = cleanTag,
                    description = cleanDesc,
                    bannerColorHex = bannerColorHex,
                    leaderUid = userId,
                    leaderName = safeLeader,
                    memberUids = listOf(userId),
                    memberCount = 1L,
                    totalPower = powerLong
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinAlliance(userId: String, alliance: AllianceInfo, playerPower: Int): Result<Unit> {
        val docPath = "alliances/${alliance.allianceId}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed." }
            if (userId in alliance.memberUids) return Result.success(Unit)
            val updatedMembers = alliance.memberUids + userId
            db.collection("alliances").document(alliance.allianceId).update(
                mapOf(
                    "memberUids" to updatedMembers,
                    "memberCount" to updatedMembers.size.toLong(),
                    "totalPower" to (alliance.totalPower + playerPower.toLong().coerceAtLeast(0L)),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitTask()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun leaveAlliance(userId: String, alliance: AllianceInfo, playerPower: Int): Result<Unit> {
        val docPath = "alliances/${alliance.allianceId}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed." }
            if (alliance.leaderUid == userId) {
                if (alliance.memberUids.size <= 1) {
                    db.collection("alliances").document(alliance.allianceId).delete().awaitTask()
                    return Result.success(Unit)
                }
                return Result.failure(IllegalStateException("Leader cannot leave while other members remain."))
            }
            val updatedMembers = alliance.memberUids.filterNot { it == userId }
            db.collection("alliances").document(alliance.allianceId).update(
                mapOf(
                    "memberUids" to updatedMembers,
                    "memberCount" to updatedMembers.size.toLong(),
                    "totalPower" to (alliance.totalPower - playerPower.toLong().coerceAtLeast(0L)).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitTask()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeWorldChat(): Flow<List<ChatMessage>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            close(IllegalStateException("User must be signed in before observing world chat."))
            return@callbackFlow
        }
        val query = db.collection("world_chat")
            .whereGreaterThanOrEqualTo("sentEpochMs", 0)
            .orderBy("sentEpochMs", Query.Direction.DESCENDING)
            .limit(40)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, "world_chat")
                }
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.sentEpochMs }
                trySend(items)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun sendWorldChat(
        userId: String,
        senderName: String,
        avatarHeroId: String,
        allianceTag: String,
        text: String
    ): Result<Unit> {
        val cleanText = text.trim().take(280)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Empty message"))
        val sanitizedUid = userId.replace(Regex("[^a-zA-Z0-9_-]"), "").take(12)
        val msgId = "w_${System.currentTimeMillis()}_$sanitizedUid"
        val docPath = "world_chat/$msgId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed." }
            db.collection("world_chat").document(msgId).set(
                mapOf(
                    "senderUid" to userId,
                    "senderName" to senderName.trim().take(40).ifBlank { "Pharaoh Chief" },
                    "avatarHeroId" to avatarHeroId.take(40).ifBlank { "h_sergey" },
                    "allianceTag" to allianceTag.take(10),
                    "text" to cleanText,
                    "sentEpochMs" to System.currentTimeMillis().coerceAtLeast(1L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).awaitTask()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    fun observeAllianceChat(allianceId: String): Flow<List<ChatMessage>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || allianceId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val query = db.collection("alliances")
            .document(allianceId)
            .collection("messages")
            .whereArrayContains("memberUids", uid)
            .limit(60)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, "alliances/$allianceId/messages")
                }
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.sentEpochMs }
                trySend(items)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun sendAllianceChat(
        alliance: AllianceInfo,
        userId: String,
        senderName: String,
        avatarHeroId: String,
        text: String
    ): Result<Unit> {
        val cleanText = text.trim().take(280)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Empty message"))
        val sanitizedUid = userId.replace(Regex("[^a-zA-Z0-9_-]"), "").take(12)
        val msgId = "a_${System.currentTimeMillis()}_$sanitizedUid"
        val docPath = "alliances/${alliance.allianceId}/messages/$msgId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed." }
            val members = if (userId in alliance.memberUids) alliance.memberUids else (alliance.memberUids + userId)
            db.collection("alliances")
                .document(alliance.allianceId)
                .collection("messages")
                .document(msgId)
                .set(
                    mapOf(
                        "senderUid" to userId,
                        "senderName" to senderName.trim().take(40).ifBlank { "Pharaoh Chief" },
                        "avatarHeroId" to avatarHeroId.take(40).ifBlank { "h_sergey" },
                        "allianceTag" to alliance.tag.take(10),
                        "text" to cleanText,
                        "sentEpochMs" to System.currentTimeMillis().coerceAtLeast(1L),
                        "memberUids" to members.take(50),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).awaitTask()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}

