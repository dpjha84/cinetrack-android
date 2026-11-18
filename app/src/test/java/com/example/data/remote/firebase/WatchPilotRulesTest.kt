package com.example.data.remote.firebase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.MatchCandidateCard
import com.example.data.model.MonetizationState
import com.example.data.model.PlanType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WatchPilotRulesTest : FirestoreEmulatorTestBase() {

    private fun randomRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    @Test
    fun matchRoom_hostCreates_guestJoins_andMutualSwipeMatches() = runBlocking {
        val aliceUid = signInTestUser("alice_wp@test.com")
        val repo = MatchRoomRepository(firestore, auth)
        val roomCode = randomRoomCode()
        val candidate = MatchCandidateCard(
            id = "tmdb_m_693134",
            title = "Dune: Part Two",
            releaseYear = 2024,
            mediaType = "MOVIE",
            genres = "Sci-Fi, Adventure",
            rating = 8.4f,
            posterUrl = "https://image.tmdb.org/t/p/w500/dune2.jpg",
            providersLabel = "Max, Prime Video"
        )

        val created = withTimeout(5000L) {
            repo.createRoom(
                hostName = "Alice",
                regionCode = "US",
                selectedProviders = listOf("Max", "Prime Video"),
                candidates = listOf(candidate),
                roomCodeOverride = roomCode
            ).getOrThrow()
        }
        assertEquals(roomCode, created.roomCode)
        assertEquals(aliceUid, created.hostUid)

        // Alice swipes right
        withTimeout(5000L) {
            repo.recordRightSwipe(roomCode, candidate).getOrThrow()
        }

        // Bob signs in, joins, and swipes right -> triggers mutual MATCHED state!
        val bobUid = signInTestUser("bob_wp@test.com")
        val joined = withTimeout(5000L) {
            repo.joinRoom(roomCode).getOrThrow()
        }
        assertTrue(bobUid in joined.participantUids)

        val matched = withTimeout(5000L) {
            repo.recordRightSwipe(roomCode, candidate).getOrThrow()
        }
        assertEquals("MATCHED", matched.status)
        assertEquals("tmdb_m_693134", matched.matchedMediaId)

        // Snapshot listener emits live state
        val observed = withTimeout(3000L) {
            repo.observeRoom(roomCode).first { it?.status == "MATCHED" }
        }
        assertNotNull(observed)
        assertEquals("Dune: Part Two", observed?.matchedTitle)
    }

    @Test
    fun matchRoom_unauthenticatedObserver_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = MatchRoomRepository(firestore, auth)
        val code = randomRoomCode()

        var caught: Throwable? = null
        try {
            withTimeout(3000L) {
                repo.observeRoom(code).first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull("Expected PERMISSION_DENIED exception for unauthenticated caller", caught)
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected wrapped FirebaseFirestoreException, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun monetizationEntitlement_ownerSyncsAndCrossUserReadDenied() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val aliceUid = signInTestUser("alice_pro@test.com")
        val syncManager = FirestoreSyncManager(context, firestore)

        withTimeout(5000L) {
            syncManager.uploadMonetizationEntitlement(
                aliceUid,
                MonetizationState(
                    isPro = true,
                    planType = PlanType.PRO_ANNUAL,
                    subscribedProviders = setOf("Netflix", "Prime Video"),
                    streamOnMyAppsOnly = true
                )
            )
        }

        val pulled = withTimeout(5000L) {
            syncManager.pullCloudData(aliceUid)
        }
        assertEquals(true, pulled?.entitlement?.isPro)
        assertEquals("PRO_ANNUAL", pulled?.entitlement?.planType)

        // Bob cannot read Alice's monetization subcollection directly
        signInTestUser("bob_snooper@test.com")
        var crossUserError: FirebaseFirestoreException? = null
        try {
            withTimeout(5000L) {
                firestore.collection("users").document(aliceUid)
                    .collection("monetization").document("entitlement")
                    .get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            crossUserError = e
        }
        assertNotNull(crossUserError)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, crossUserError?.code)
    }
}
