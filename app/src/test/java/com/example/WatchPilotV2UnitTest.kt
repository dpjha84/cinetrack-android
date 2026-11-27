package com.example

import com.example.data.billing.MonetizationManager
import com.example.data.billing.MonetizationQuotaPolicy
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.MonetizationState
import com.example.data.model.OttRecommendation
import com.example.data.model.ProFeatureQuotaType
import com.example.data.model.WatchAlertType
import com.example.data.model.WatchStatus
import com.example.util.CsvLibraryPorter
import com.example.util.OttMoneySaverCalculator
import com.example.util.RateAppEngagementPolicy
import com.example.util.RateAppPromptState
import com.example.util.WatchAlertSnapshot
import com.example.util.WatchAlertsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchPilotV2UnitTest {

    private fun sampleMedia(
        id: String,
        title: String,
        status: WatchStatus = WatchStatus.PLAN_TO_WATCH,
        providers: String = "US:Netflix,Max|IN:JioCinema",
        rating: Float = 0f,
        isFavorite: Boolean = false,
        mediaType: MediaType = MediaType.MOVIE,
        totalSeasons: Int = 0,
        totalEpisodes: Int = 1
    ): MediaEntity = MediaEntity(
        id = id,
        title = title,
        mediaType = mediaType.name,
        releaseYear = 2024,
        genres = "Sci-Fi, Thriller",
        posterUrl = "https://example.com/p.jpg",
        backdropUrl = "https://example.com/b.jpg",
        overview = "Sample overview",
        directorOrCreator = "Director",
        cast = "Actor",
        communityRating = 8.1f,
        runtimeMinutes = 120,
        totalSeasons = totalSeasons,
        totalEpisodes = totalEpisodes,
        status = status.name,
        userRating = rating,
        isFavorite = isFavorite,
        streamingProviders = providers
    )

    @Test
    fun csvPorter_parsesMoviebaseImdbLetterboxdAndRoundTripsWatchPilot() {
        val moviebaseCsv = """
            tmdb_id,title,year,media_type,status,rating
            693134,"Dune: Part Two",2024,movie,completed,9.0
            94997,"House of the Dragon",2022,tv,watching,8.0
        """.trimIndent()

        val mbBatch = CsvLibraryPorter.parseUniversalCsv(moviebaseCsv)
        assertEquals("Moviebase", mbBatch.formatName)
        assertEquals(2, mbBatch.rows.size)
        assertEquals("Dune: Part Two", mbBatch.rows[0].title)
        assertEquals(WatchStatus.COMPLETED, mbBatch.rows[0].status)
        assertEquals(4.5f, mbBatch.rows[0].userRatingOutOf5, 0.01f)
        assertEquals(MediaType.TV_SHOW, mbBatch.rows[1].mediaType)
        assertEquals(WatchStatus.WATCHING, mbBatch.rows[1].status)

        val imdbCsv = """
            Const,Your Rating,Date Rated,Title,Title Type,Year
            tt15239678,8,2024-03-05,Dune: Part Two,Movie,2024
        """.trimIndent()
        val imdbBatch = CsvLibraryPorter.parseUniversalCsv(imdbCsv)
        assertEquals("IMDb", imdbBatch.formatName)
        assertEquals(1, imdbBatch.rows.size)
        assertEquals(WatchStatus.COMPLETED, imdbBatch.rows[0].status)
        assertEquals(4.0f, imdbBatch.rows[0].userRatingOutOf5, 0.01f)

        val letterboxdCsv = """
            Date,Name,Year,Letterboxd URI,Rating,Watched Date
            2024-04-01,Oppenheimer,2023,https://boxd.it/xyz,4.5,2024-03-29
        """.trimIndent()
        val lbBatch = CsvLibraryPorter.parseUniversalCsv(letterboxdCsv)
        assertEquals("Letterboxd", lbBatch.formatName)
        assertEquals(1, lbBatch.rows.size)
        assertEquals(WatchStatus.COMPLETED, lbBatch.rows[0].status)
        assertEquals(4.5f, lbBatch.rows[0].userRatingOutOf5, 0.01f)

        val roundTripSource = listOf(
            sampleMedia("tmdb_m_101", "Dune: Part Two", WatchStatus.COMPLETED, rating = 5.0f, isFavorite = true),
            sampleMedia("tmdb_m_102", "Blade Runner 2099", WatchStatus.PLAN_TO_WATCH, rating = 0f, isFavorite = false)
        )
        val exportedCsv = CsvLibraryPorter.serializeToCsv(roundTripSource)
        val wpBatch = CsvLibraryPorter.parseUniversalCsv(exportedCsv)
        assertEquals("WatchPilot", wpBatch.formatName)
        assertEquals(2, wpBatch.rows.size)
        assertTrue(wpBatch.rows[0].isFavorite)
        assertEquals(WatchStatus.PLAN_TO_WATCH, wpBatch.rows[1].status)
    }

    @Test
    fun ottMoneySaver_detectsPauseAndSaveCandidatesAndMatchesApps() {
        val tracked = listOf(
            sampleMedia("m1", "Title 1", WatchStatus.PLAN_TO_WATCH, "US:Netflix,Max"),
            sampleMedia("m2", "Title 2", WatchStatus.WATCHING, "US:Netflix"),
            sampleMedia("m3", "Title 3", WatchStatus.PLAN_TO_WATCH, "US:Netflix"),
            sampleMedia("m4", "Title 4", WatchStatus.PLAN_TO_WATCH, "US:Prime Video")
        )

        val summary = OttMoneySaverCalculator.calculateAudit(
            trackedMedia = tracked,
            regionCode = "US",
            subscribedCanonicalNames = setOf("Netflix", "Prime Video", "Apple TV+")
        )

        assertEquals(3, summary.subscribedCount)
        assertEquals(4, summary.coveredWatchlistCount)

        val appleAudit = summary.items.first { it.providerName == "Apple TV+" }
        assertEquals(0, appleAudit.matchingTitlesCount)
        assertEquals(OttRecommendation.PAUSE_AND_SAVE, appleAudit.recommendation)

        val primeAudit = summary.items.first { it.providerName == "Prime Video" }
        assertEquals(1, primeAudit.matchingTitlesCount)
        assertEquals(OttRecommendation.CONSIDER_ROTATING, primeAudit.recommendation)

        val netflixAudit = summary.items.first { it.providerName == "Netflix" }
        assertEquals(3, netflixAudit.matchingTitlesCount)
        assertEquals(OttRecommendation.KEEP_ACTIVE, netflixAudit.recommendation)

        assertTrue(summary.potentialMonthlySavingsUsd >= 9.99f)
        assertTrue(OttMoneySaverCalculator.matchesAnySubscribedApp(tracked[0], "US", setOf("Netflix")))
        assertFalse(OttMoneySaverCalculator.matchesAnySubscribedApp(tracked[3], "US", setOf("Apple TV+")))
    }

    @Test
    fun mergeAppendOnlyById_preservesExistingOrderAndAppendsNewPagesStrictlyAtEnd() {
        val page1A = sampleMedia("tmdb_m_1", "Alpha", rating = 6.5f)
        val page1B = sampleMedia("tmdb_m_2", "Beta", rating = 6.0f)
        val page1C = sampleMedia("tmdb_m_3", "Gamma", rating = 5.5f)

        val orderedIds = mutableListOf<String>()
        val firstResult = com.example.ui.viewmodel.mergeAppendOnlyById(
            existingOrderedIds = orderedIds,
            candidates = listOf(page1A, page1B, page1C)
        ) { it.id }
        assertEquals(listOf("tmdb_m_1", "tmdb_m_2", "tmdb_m_3"), firstResult.map { it.id })

        // Simulate global sort returning higher-ranked page 2 items ahead of page 1 items
        val page2Top = sampleMedia("tmdb_m_99", "Top Hit", rating = 9.9f)
        val page2Mid = sampleMedia("tmdb_m_100", "Runner Up", rating = 8.8f)
        val sortedCombined = listOf(page2Top, page2Mid, page1A, page1B, page1C)

        val secondResult = com.example.ui.viewmodel.mergeAppendOnlyById(
            existingOrderedIds = orderedIds,
            candidates = sortedCombined
        ) { it.id }
        assertEquals(
            listOf("tmdb_m_1", "tmdb_m_2", "tmdb_m_3", "tmdb_m_99", "tmdb_m_100"),
            secondResult.map { it.id }
        )

        // Verify person list follows the exact same append-only rule
        val personIds = mutableListOf<String>()
        val p1 = com.example.data.model.TmdbSearchResultItem.Person(10L, "Actor A", null, "Acting", emptyList(), 50.0)
        val p2 = com.example.data.model.TmdbSearchResultItem.Person(20L, "Actor B", null, "Acting", emptyList(), 40.0)
        com.example.ui.viewmodel.mergeAppendOnlyById(personIds, listOf(p1, p2)) { it.id.toString() }

        val p3Higher = com.example.data.model.TmdbSearchResultItem.Person(30L, "Star C", null, "Acting", emptyList(), 99.0)
        val peopleSecond = com.example.ui.viewmodel.mergeAppendOnlyById(personIds, listOf(p3Higher, p1, p2)) {
            it.id.toString()
        }
        assertEquals(listOf(10L, 20L, 30L), peopleSecond.map { it.id })
    }

    @Test
    fun homeCatalogSectionFormattedTitle_reflectsMediaTypeFilter() {
        assertEquals(
            "Now Playing in United States",
            com.example.data.model.HomeCatalogSection.NOW_PLAYING.formattedTitle("United States", null)
        )
        assertEquals(
            "Now Playing Movies in United States",
            com.example.data.model.HomeCatalogSection.NOW_PLAYING.formattedTitle("United States", MediaType.MOVIE)
        )
        assertEquals(
            "Now Playing Shows in United States",
            com.example.data.model.HomeCatalogSection.NOW_PLAYING.formattedTitle("United States", MediaType.TV_SHOW)
        )
        assertEquals(
            "Popular Movies in India",
            com.example.data.model.HomeCatalogSection.POPULAR.formattedTitle("India", MediaType.MOVIE)
        )
        assertEquals(
            "Upcoming Shows in India",
            com.example.data.model.HomeCatalogSection.UPCOMING.formattedTitle("India", MediaType.TV_SHOW)
        )
    }

    @Test
    fun monetizationQuotaPolicy_resetsMonthlyAndEnforcesRewardedAdAntiAbuseCaps() {
        val exhaustedNov = MonetizationState(
            snapScansUsed = 3,
            aiConciergeUsed = 3,
            matchRoomsHosted = 1,
            aiRecapsUsed = 1,
            quotaYearMonth = "2026-11",
            rewardedTrialCredits = 1,
            rewardedAdsWatchedToday = 1,
            rewardedLastDayKey = "2026-11-30",
            rewardedAdsWatchedThisMonth = 3,
            rewardedMonthKey = "2026-11"
        )

        // Same day & month: cannot watch another rewarded ad
        assertFalse(exhaustedNov.canWatchRewardedTrialAd)
        val blockedSameDay = MonetizationQuotaPolicy.recordRewardedAdEarned(
            state = exhaustedNov,
            dayKey = "2026-11-30",
            monthKey = "2026-11"
        )
        assertEquals(1, blockedSameDay.rewardedTrialCredits)
        assertEquals(3, blockedSameDay.rewardedAdsWatchedThisMonth)

        // Calendar month rolls over -> all monthly free quotas & monthly rewarded counter reset; banked pass retained
        val rolledDec = MonetizationQuotaPolicy.normalizeForDate(
            state = exhaustedNov,
            dayKey = "2026-12-01",
            monthKey = "2026-12"
        )
        assertEquals(0, rolledDec.snapScansUsed)
        assertEquals(0, rolledDec.aiConciergeUsed)
        assertEquals(0, rolledDec.matchRoomsHosted)
        assertEquals(0, rolledDec.aiRecapsUsed)
        assertEquals(0, rolledDec.rewardedAdsWatchedToday)
        assertEquals(0, rolledDec.rewardedAdsWatchedThisMonth)
        assertEquals(1, rolledDec.rewardedTrialCredits)
        assertTrue(rolledDec.canWatchRewardedTrialAd)

        // Earn 1 ad on Dec 1 -> reaches MAX_BANKED_TRIAL_CREDITS (2) & daily cap (1/day)
        val afterDay1Ad = MonetizationQuotaPolicy.recordRewardedAdEarned(
            state = rolledDec,
            dayKey = "2026-12-01",
            monthKey = "2026-12"
        )
        assertEquals(2, afterDay1Ad.rewardedTrialCredits)
        assertEquals(1, afterDay1Ad.rewardedAdsWatchedToday)
        assertEquals(1, afterDay1Ad.rewardedAdsWatchedThisMonth)
        assertFalse(afterDay1Ad.canWatchRewardedTrialAd)

        // Even on Dec 2, cannot bank above MAX_BANKED_TRIAL_CREDITS (2)
        val nextDayNormalized = MonetizationQuotaPolicy.normalizeForDate(
            state = afterDay1Ad,
            dayKey = "2026-12-02",
            monthKey = "2026-12"
        )
        assertFalse(nextDayNormalized.canWatchRewardedTrialAd)

        // Consume AI Recap: uses free monthly quota first (1/1), keeping banked ad passes intact
        val afterFreeRecap = MonetizationQuotaPolicy.consumeFeatureUse(
            state = afterDay1Ad,
            feature = ProFeatureQuotaType.AI_RECAP,
            dayKey = "2026-12-02",
            monthKey = "2026-12"
        )
        assertEquals(1, afterFreeRecap.aiRecapsUsed)
        assertEquals(2, afterFreeRecap.rewardedTrialCredits)

        // Second AI Recap uses 1 banked rewarded trial pass
        val afterPassRecap = MonetizationQuotaPolicy.consumeFeatureUse(
            state = afterFreeRecap,
            feature = ProFeatureQuotaType.AI_RECAP,
            dayKey = "2026-12-02",
            monthKey = "2026-12"
        )
        assertEquals(1, afterPassRecap.rewardedTrialCredits)
        assertTrue(afterPassRecap.canWatchRewardedTrialAd) // bank dropped below 2 & Dec 2 daily count is 0

        // Pro users never consume monthly quotas or trial credits
        val proUser = afterFreeRecap.copy(isPro = true)
        val proAfterRecap = MonetizationQuotaPolicy.consumeFeatureUse(
            state = proUser,
            feature = ProFeatureQuotaType.AI_RECAP,
            dayKey = "2026-12-02",
            monthKey = "2026-12"
        )
        assertEquals(1, proAfterRecap.aiRecapsUsed)
        assertEquals(2, proAfterRecap.rewardedTrialCredits)
    }

    @Test
    fun watchAlertsManager_detectsStreamingArrivalAndNewSeasonWithoutDuplicates() {
        val movieOnNetflix = sampleMedia(
            id = "tmdb_m_701",
            title = "Arrival Movie",
            status = WatchStatus.PLAN_TO_WATCH,
            providers = "US:Netflix,Max"
        )
        val showNewSeason = sampleMedia(
            id = "tmdb_tv_802",
            title = "Severance",
            status = WatchStatus.WATCHING,
            providers = "US:Apple TV+",
            mediaType = MediaType.TV_SHOW,
            totalSeasons = 2,
            totalEpisodes = 19
        )

        val prevSnapshots = mapOf(
            "tmdb_m_701" to WatchAlertSnapshot(knownSeasons = 0, wasOnMyApps = false),
            "tmdb_tv_802" to WatchAlertSnapshot(knownSeasons = 1, wasOnMyApps = true)
        )

        val (alerts, updatedSnapshots) = WatchAlertsManager.detectWatchAlerts(
            trackedMedia = listOf(movieOnNetflix, showNewSeason),
            previousSnapshots = prevSnapshots,
            subscribedProviders = setOf("Netflix", "Apple TV+"),
            regionCode = "US",
            nowMs = 1_700_000_000_000L
        )

        assertEquals(2, alerts.size)
        val streamAlert = alerts.first { it.mediaId == "tmdb_m_701" }
        assertEquals(WatchAlertType.NOW_ON_MY_APPS, streamAlert.alertType)
        assertTrue(streamAlert.detailMessage.contains("Netflix"))

        val seasonAlert = alerts.first { it.mediaId == "tmdb_tv_802" }
        assertEquals(WatchAlertType.NEW_SEASON, seasonAlert.alertType)
        assertTrue(seasonAlert.detailMessage.contains("Season 2"))

        // Re-evaluating unchanged state must NOT fire duplicate alerts
        val (secondPassAlerts, _) = WatchAlertsManager.detectWatchAlerts(
            trackedMedia = listOf(movieOnNetflix, showNewSeason),
            previousSnapshots = updatedSnapshots,
            subscribedProviders = setOf("Netflix", "Apple TV+"),
            regionCode = "US",
            nowMs = 1_700_000_005_000L
        )
        assertTrue(secondPassAlerts.isEmpty())
    }

    @Test
    fun rateAppEngagementPolicy_triggersAtFiveWatchedAndRespectsSnoozeAndOptOut() {
        var state = RateAppPromptState()

        // First 4 watched movies/episodes should accumulate without triggering
        repeat(4) {
            val (next, shouldPrompt) = RateAppEngagementPolicy.recordWatched(
                state = state,
                delta = 1,
                alreadyPromptedThisSession = false
            )
            state = next
            assertFalse(shouldPrompt)
        }
        assertEquals(4, state.watchedCount)

        // 5th watched movie/episode triggers the dialog
        val (atFive, shouldPromptAtFive) = RateAppEngagementPolicy.recordWatched(
            state = state,
            delta = 1,
            alreadyPromptedThisSession = false
        )
        assertEquals(5, atFive.watchedCount)
        assertTrue(shouldPromptAtFive)

        // Same-session suppression prevents repeated prompts if dialog already opened
        val (_, suppressedSameSession) = RateAppEngagementPolicy.recordWatched(
            state = atFive,
            delta = 1,
            alreadyPromptedThisSession = true
        )
        assertFalse(suppressedSameSession)

        // Tapping Maybe Later snoozes by 10 watched items (5 + 10 = 15)
        val snoozedOnce = RateAppEngagementPolicy.onMaybeLater(atFive)
        assertEquals(1, snoozedOnce.dismissCount)
        assertEquals(15, snoozedOnce.nextTriggerThreshold)
        assertFalse(snoozedOnce.neverAskAgain)

        // Watching 9 more items (count 14) still does not re-trigger
        val (atFourteen, promptAtFourteen) = RateAppEngagementPolicy.recordWatched(
            state = snoozedOnce,
            delta = 9,
            alreadyPromptedThisSession = false
        )
        assertEquals(14, atFourteen.watchedCount)
        assertFalse(promptAtFourteen)

        // Watching 15th item triggers again
        val (atFifteen, promptAtFifteen) = RateAppEngagementPolicy.recordWatched(
            state = atFourteen,
            delta = 1,
            alreadyPromptedThisSession = false
        )
        assertEquals(15, atFifteen.watchedCount)
        assertTrue(promptAtFifteen)

        // Second Maybe Later permanently stops asking
        val snoozedTwice = RateAppEngagementPolicy.onMaybeLater(atFifteen)
        assertTrue(snoozedTwice.neverAskAgain)
        val (_, afterMaxSnooze) = RateAppEngagementPolicy.recordWatched(
            state = snoozedTwice,
            delta = 20,
            alreadyPromptedThisSession = false
        )
        assertFalse(afterMaxSnooze)

        // Rating now or selecting Never Ask Again permanently suppresses future triggers
        val rated = RateAppEngagementPolicy.onRateNow(atFive)
        assertTrue(rated.hasRatedApp)
        assertFalse(RateAppEngagementPolicy.recordWatched(rated, delta = 10).second)

        val optedOut = RateAppEngagementPolicy.onNeverAskAgain(atFive)
        assertTrue(optedOut.neverAskAgain)
        assertFalse(RateAppEngagementPolicy.recordWatched(optedOut, delta = 10).second)
    }

    @Test
    fun editionBillingProvider_resolvesHighestTierPlanPrecedence() {
        assertEquals(
            null,
            com.example.data.billing.resolveHighestTierPlan(emptyList())
        )
        assertEquals(
            com.example.data.model.PlanType.PRO_MONTHLY,
            com.example.data.billing.resolveHighestTierPlan(
                listOf(listOf(MonetizationManager.SKU_PRO_MONTHLY))
            )
        )
        assertEquals(
            com.example.data.model.PlanType.PRO_ANNUAL,
            com.example.data.billing.resolveHighestTierPlan(
                listOf(
                    listOf(MonetizationManager.SKU_PRO_MONTHLY),
                    listOf(MonetizationManager.SKU_PRO_ANNUAL)
                )
            )
        )
        assertEquals(
            com.example.data.model.PlanType.PRO_LIFETIME,
            com.example.data.billing.resolveHighestTierPlan(
                listOf(
                    listOf(MonetizationManager.SKU_PRO_MONTHLY),
                    listOf(MonetizationManager.SKU_PRO_LIFETIME),
                    listOf(MonetizationManager.SKU_PRO_ANNUAL)
                )
            )
        )
    }

    @Test
    fun libraryTabFiltering_filtersByTabCustomCollectionAndSubscribedAppsAndSorts() {
        val watchingNetflix = sampleMedia(
            id = "m1",
            title = "Zebra Show",
            status = WatchStatus.WATCHING,
            providers = "US:Netflix",
            rating = 4.0f
        ).copy(customWatchlist = "Weekend Binge")
        val planMax = sampleMedia(
            id = "m2",
            title = "Alpha Movie",
            status = WatchStatus.PLAN_TO_WATCH,
            providers = "US:Max",
            rating = 5.0f,
            isFavorite = true
        )
        val completedNetflix = sampleMedia(
            id = "m3",
            title = "Beta Film",
            status = WatchStatus.COMPLETED,
            providers = "US:Netflix",
            rating = 3.5f,
            isFavorite = true
        ).copy(customWatchlist = "Weekend Binge")

        val allItems = listOf(watchingNetflix, planMax, completedNetflix)
        val defaultMon = MonetizationState()

        assertEquals(
            listOf("m1"),
            com.example.ui.viewmodel.filterAndSortLibraryForTab(
                items = allItems,
                tab = com.example.ui.viewmodel.LibraryTab.WATCHING,
                sort = com.example.ui.viewmodel.SortOption.TITLE,
                collection = null,
                monState = defaultMon,
                regionCode = "US"
            ).map { it.id }
        )
        assertEquals(
            listOf("m2"),
            com.example.ui.viewmodel.filterAndSortLibraryForTab(
                items = allItems,
                tab = com.example.ui.viewmodel.LibraryTab.PLAN_TO_WATCH,
                sort = com.example.ui.viewmodel.SortOption.TITLE,
                collection = null,
                monState = defaultMon,
                regionCode = "US"
            ).map { it.id }
        )
        assertEquals(
            listOf("m3"),
            com.example.ui.viewmodel.filterAndSortLibraryForTab(
                items = allItems,
                tab = com.example.ui.viewmodel.LibraryTab.COMPLETED,
                sort = com.example.ui.viewmodel.SortOption.TITLE,
                collection = null,
                monState = defaultMon,
                regionCode = "US"
            ).map { it.id }
        )
        assertEquals(
            listOf("m2", "m3"),
            com.example.ui.viewmodel.filterAndSortLibraryForTab(
                items = allItems,
                tab = com.example.ui.viewmodel.LibraryTab.FAVORITES,
                sort = com.example.ui.viewmodel.SortOption.TITLE,
                collection = null,
                monState = defaultMon,
                regionCode = "US"
            ).map { it.id }
        )

        // Collection filter + Pro "On My Apps" filter together
        val myAppsMon = MonetizationState(
            streamOnMyAppsOnly = true,
            subscribedProviders = setOf("Netflix")
        )
        assertEquals(
            listOf("m3", "m1"),
            com.example.ui.viewmodel.filterAndSortLibraryForTab(
                items = allItems,
                tab = com.example.ui.viewmodel.LibraryTab.ALL_TRACKED,
                sort = com.example.ui.viewmodel.SortOption.TITLE,
                collection = "Weekend Binge",
                monState = myAppsMon,
                regionCode = "US"
            ).map { it.id }
        )
    }

    @Test
    fun shouldRevokePlayEntitlementOnEmptyQuery_revokesExpiredPlayPlansWhileKeepingDebugSandbox() {
        assertFalse(
            com.example.data.billing.shouldRevokePlayEntitlementOnEmptyQuery(
                state = MonetizationState(isPro = false, planType = com.example.data.model.PlanType.FREE),
                isDebugBuild = false
            )
        )
        assertTrue(
            com.example.data.billing.shouldRevokePlayEntitlementOnEmptyQuery(
                state = MonetizationState(isPro = true, planType = com.example.data.model.PlanType.PRO_MONTHLY),
                isDebugBuild = false
            )
        )
        assertTrue(
            com.example.data.billing.shouldRevokePlayEntitlementOnEmptyQuery(
                state = MonetizationState(isPro = true, planType = com.example.data.model.PlanType.PRO_ANNUAL),
                isDebugBuild = true
            )
        )
        assertTrue(
            com.example.data.billing.shouldRevokePlayEntitlementOnEmptyQuery(
                state = MonetizationState(isPro = true, planType = com.example.data.model.PlanType.PRO_LIFETIME),
                isDebugBuild = false
            )
        )
        assertFalse(
            com.example.data.billing.shouldRevokePlayEntitlementOnEmptyQuery(
                state = MonetizationState(isPro = true, planType = com.example.data.model.PlanType.PRO_SANDBOX),
                isDebugBuild = true
            )
        )
        assertTrue(
            com.example.data.billing.shouldRevokePlayEntitlementOnEmptyQuery(
                state = MonetizationState(isPro = true, planType = com.example.data.model.PlanType.PRO_SANDBOX),
                isDebugBuild = false
            )
        )
    }

    @Test
    fun analyticsTracker_logsPaywallBeginCheckoutAndProUnlockFunnelEvents() {
        val captured = mutableListOf<Pair<String, Map<String, String>>>()
        com.example.util.AnalyticsTracker.testEventObserver = { name, params ->
            captured.add(name to params)
        }
        try {
            com.example.util.AnalyticsTracker.logPaywallViewed("ott_money_saver")
            com.example.util.AnalyticsTracker.logBeginCheckout(MonetizationManager.SKU_PRO_ANNUAL)
            com.example.util.AnalyticsTracker.logProUnlocked(
                planTypeName = com.example.data.model.PlanType.PRO_ANNUAL.name,
                isRealPlayBilling = true
            )

            assertEquals(3, captured.size)
            assertEquals("paywall_viewed", captured[0].first)
            assertEquals("ott_money_saver", captured[0].second["trigger_reason"])

            assertEquals(com.google.firebase.analytics.FirebaseAnalytics.Event.BEGIN_CHECKOUT, captured[1].first)
            assertEquals(
                MonetizationManager.SKU_PRO_ANNUAL,
                captured[1].second[com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_ID]
            )

            assertEquals("pro_plan_unlocked", captured[2].first)
            assertEquals("PRO_ANNUAL", captured[2].second["plan_type"])
            assertEquals("google_play", captured[2].second["source"])
        } finally {
            com.example.util.AnalyticsTracker.testEventObserver = null
        }
    }

    @Test
    fun ottMoneySaver_localizesIndiaInrPricingAndMatchesPrimeJioHotstarSonyLiv() {
        val indiaServices = OttMoneySaverCalculator.servicesForRegion("IN")
        assertEquals("Prime Video", indiaServices[0].canonicalName)
        assertEquals("Disney+ / Hotstar", indiaServices[1].canonicalName)
        assertEquals("JioHotstar", OttMoneySaverCalculator.displayLabelForRegion(indiaServices[1], "IN"))
        assertTrue(indiaServices.take(6).any { it.canonicalName == "SonyLIV" })

        val trackedIndia = listOf(
            sampleMedia("in_1", "Mirzapur", WatchStatus.WATCHING, "IN:Amazon Prime Video"),
            sampleMedia("in_2", "Panchayat", WatchStatus.PLAN_TO_WATCH, "IN:Prime Video"),
            sampleMedia("in_3", "The Family Man", WatchStatus.PLAN_TO_WATCH, "IN:Amazon Prime Video"),
            sampleMedia("in_4", "Special Ops", WatchStatus.PLAN_TO_WATCH, "IN:JioHotstar")
        )

        val summaryIn = OttMoneySaverCalculator.calculateAudit(
            trackedMedia = trackedIndia,
            regionCode = "IN",
            subscribedCanonicalNames = setOf("Prime Video", "Disney+ / Hotstar", "SonyLIV")
        )

        assertEquals("IN", summaryIn.regionCode)
        assertEquals("₹", summaryIn.currencySymbol)
        assertEquals("₹847", summaryIn.estimatedMonthlySpendLabel)
        assertEquals("₹399", summaryIn.potentialMonthlySavingsLabel)
        assertEquals("₹399", summaryIn.potentialMonthlySavingsShortLabel)
        assertEquals(4, summaryIn.coveredWatchlistCount)

        val sonyAudit = summaryIn.items.first { it.providerName == "SonyLIV" }
        assertEquals(0, sonyAudit.matchingTitlesCount)
        assertEquals(OttRecommendation.PAUSE_AND_SAVE, sonyAudit.recommendation)
        assertEquals("₹399", sonyAudit.monthlyCostLabel)

        val hotstarAudit = summaryIn.items.first { it.providerName == "Disney+ / Hotstar" }
        assertEquals("JioHotstar", hotstarAudit.displayName)
        assertEquals(1, hotstarAudit.matchingTitlesCount)
        assertEquals(OttRecommendation.CONSIDER_ROTATING, hotstarAudit.recommendation)
        assertEquals("₹149", hotstarAudit.monthlyCostLabel)

        val primeAudit = summaryIn.items.first { it.providerName == "Prime Video" }
        assertEquals(3, primeAudit.matchingTitlesCount)
        assertEquals(OttRecommendation.KEEP_ACTIVE, primeAudit.recommendation)
        assertEquals("₹299", primeAudit.monthlyCostLabel)

        // Verify UK (GB) still formats decimal GBP properly
        val ukSummary = OttMoneySaverCalculator.calculateAudit(
            trackedMedia = trackedIndia,
            regionCode = "GB",
            subscribedCanonicalNames = setOf("Prime Video")
        )
        assertEquals("£", ukSummary.currencySymbol)
        assertEquals("£8.99", ukSummary.estimatedMonthlySpendLabel)
    }

    @Test
    fun safeFeaturesFirst_v510Configuration_disablesAiAndUses999AnnualFallback() {
        assertFalse(com.example.util.FeatureFlags.ENABLE_AI_FEATURES)
        assertEquals("$9.99 / yr", MonetizationState().annualPriceLabel)
    }

    @Test
    fun watchProviderLauncher_avoidsBrokenHotstarSearchDeepLinksAndUsesCleanTitle() {
        // JioHotstar on TV series "New Girl" (S1:E1 selected, no direct content URL)
        val hotstarConfig = com.example.util.WatchProviderLauncher.resolveProviderConfig(
            providerName = "JioHotstar",
            title = "New Girl",
            releaseYear = 2011,
            seasonNumber = 1,
            episodeNumber = 1,
            directContentUrl = null,
            regionCode = "IN"
        )
        assertEquals(listOf("in.startv.hotstar", "com.hotstar.hotstar"), hotstarConfig.packageNames)
        // Must NOT send unverified hotstar://search or /in/search?q=... into JioHotstar (causes "Something went wrong")
        assertTrue(hotstarConfig.verifiedInAppUris.isEmpty())
        assertEquals(
            "https://www.hotstar.com/in/explore?search_query=New%20Girl",
            hotstarConfig.webSearchUrls.first()
        )
        assertFalse(hotstarConfig.webSearchUrls.any { it.contains("Season") || it.contains("Episode") })

        // Netflix without direct URL uses clean App Link search (never "Season 1 Episode 1")
        val netflixSearch = com.example.util.WatchProviderLauncher.resolveProviderConfig(
            providerName = "Netflix",
            title = "New Girl",
            releaseYear = 2011,
            seasonNumber = 1,
            episodeNumber = 1,
            directContentUrl = null
        )
        assertEquals(listOf("https://www.netflix.com/search?q=New%20Girl"), netflixSearch.verifiedInAppUris)

        // Netflix with direct URL keeps targeted episode deep link first
        val netflixEpisode = com.example.util.WatchProviderLauncher.resolveProviderConfig(
            providerName = "Netflix",
            title = "New Girl",
            releaseYear = 2011,
            seasonNumber = 2,
            episodeNumber = 4,
            directContentUrl = "https://www.netflix.com/title/70196145"
        )
        assertEquals(
            "nflx://www.netflix.com/watch/70196145?episode=4&season=2",
            netflixEpisode.verifiedInAppUris.first()
        )

        // Prime Video uses verified HTTPS App Link app.primevideo.com with clean %20 title
        val primeConfig = com.example.util.WatchProviderLauncher.resolveProviderConfig(
            providerName = "Amazon Prime Video",
            title = "New Girl",
            releaseYear = 2011,
            seasonNumber = 1,
            episodeNumber = 1
        )
        assertEquals(
            listOf("https://app.primevideo.com/search?phrase=New%20Girl"),
            primeConfig.verifiedInAppUris
        )

        // JioCinema falls back to JioHotstar package if JioCinema isn't installed
        val jioConfig = com.example.util.WatchProviderLauncher.resolveProviderConfig(
            providerName = "JioCinema",
            title = "New Girl",
            regionCode = "IN"
        )
        assertEquals(listOf("com.jio.media.ondemand", "in.startv.hotstar"), jioConfig.packageNames)
        assertTrue(jioConfig.verifiedInAppUris.isEmpty())
    }

    @Test
    fun watchProviderLauncher_handlesAllGlobalCountriesRegionalAppsAndWorstCaseWebFallback() {
        // 1. Disney+ prioritizes Hotstar in India & SEA, Disney+ globally in US/EU
        val disneyIn = com.example.util.WatchProviderLauncher.resolveProviderConfig(
            providerName = "Disney Plus",
            title = "Shogun",
            regionCode = "IN"
        )
        assertEquals(
            listOf("in.startv.hotstar", "com.hotstar.hotstar", "com.disney.disneyplus"),
            disneyIn.packageNames
        )
        assertTrue(disneyIn.webSearchUrls.any { it.contains("hotstar.com/in/explore") })

        val disneyUs = com.example.util.WatchProviderLauncher.resolveProviderConfig(
            providerName = "Disney Plus",
            title = "Shogun",
            regionCode = "US"
        )
        assertEquals(listOf("com.disney.disneyplus", "in.startv.hotstar"), disneyUs.packageNames)
        assertTrue(disneyUs.webSearchUrls.any { it.contains("disneyplus.com") })

        // 2. Hotstar adapts web search domain path per country
        val hotstarId = com.example.util.WatchProviderLauncher.resolveProviderConfig(
            providerName = "Hotstar",
            title = "The Bear",
            regionCode = "ID"
        )
        assertEquals(
            "https://www.hotstar.com/id/explore?search_query=The%20Bear",
            hotstarId.webSearchUrls.first()
        )

        // 3. Amazon Channels route to Prime Video (not standalone Paramount+) with country Amazon domain
        val amazonChannelUk = com.example.util.WatchProviderLauncher.resolveProviderConfig(
            providerName = "Paramount+ Amazon Channel",
            title = "Yellowstone",
            regionCode = "GB"
        )
        assertEquals(
            listOf("com.amazon.avod.thirdpartyclient", "com.amazon.amazonvideo.livingroom"),
            amazonChannelUk.packageNames
        )
        assertTrue(amazonChannelUk.webSearchUrls.any { it.contains("amazon.co.uk/s?k=Yellowstone") })

        // 4. Verified regional OTT apps across GB, AU, CA, BR, MX, JP, KR, FR, DE, ES, IN
        assertEquals(
            listOf("bbc.iplayer.android"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("BBC iPlayer", "Doctor Who", regionCode = "GB").packageNames
        )
        assertEquals(
            listOf("air.ITVMobilePlayer"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("ITVX", "Broadchurch", regionCode = "GB").packageNames
        )
        assertEquals(
            listOf("au.com.stan.and"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("Stan", "Bump", regionCode = "AU").packageNames
        )
        assertEquals(
            listOf("au.com.streamotion.ares"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("Binge", "The Last of Us", regionCode = "AU").packageNames
        )
        assertEquals(
            listOf("ca.bellmedia.cravetv"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("Crave", "Letterkenny", regionCode = "CA").packageNames
        )
        assertEquals(
            listOf("com.globo.globotv"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("Globoplay", "Cidade Invisivel", regionCode = "BR").packageNames
        )
        assertEquals(
            listOf("com.univision.prendetv"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("ViX", "El Gallo de Oro", regionCode = "MX").packageNames
        )
        assertEquals(
            listOf("jp.unext.mediaplayer"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("U-NEXT", "Tokyo Vice", regionCode = "JP").packageNames
        )
        assertEquals(
            listOf("net.cj.cjhv.gs.tving"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("TVING", "Queen Woo", regionCode = "KR").packageNames
        )
        assertEquals(
            listOf("com.coupang.mobile.play"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("Coupang Play", "Anna", regionCode = "KR").packageNames
        )
        assertEquals(
            listOf("com.canalplus.canalplus"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("Canal+", "The Bureau", regionCode = "FR").packageNames
        )
        assertEquals(
            listOf("de.prosiebensat1digital.seventv"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("Joyn", "jerks.", regionCode = "DE").packageNames
        )
        assertEquals(
            listOf("es.plus.yomvi", "com.movistarplus.androidtv"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("Movistar Plus+", "La Mesias", regionCode = "ES").packageNames
        )
        assertEquals(
            listOf("com.suntv.sunnxt"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("Sun NXT", "Jailer", regionCode = "IN").packageNames
        )
        assertEquals(
            listOf("com.viewlift.hoichoi"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("Hoichoi", "Byomkesh", regionCode = "IN").packageNames
        )
        assertEquals(
            listOf("com.lionsgateplay.videoapp"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("Lionsgate Play", "John Wick", regionCode = "IN").packageNames
        )

        // Ensure "now" matching doesn't hijack unrelated providers containing "now"
        assertEquals(
            listOf("com.nowtv.uk"),
            com.example.util.WatchProviderLauncher.resolveProviderConfig("NOW", "Succession", regionCode = "GB").packageNames
        )
        assertTrue(
            com.example.util.WatchProviderLauncher.resolveProviderConfig(
                providerName = "SnowFlix Local",
                title = "Fargo",
                webLink = "https://www.themoviedb.org/tv/60622/watch?locale=NO",
                regionCode = "NO"
            ).packageNames.isEmpty()
        )

        // 5. Unknown / long-tail global provider routes cleanly to webLink then Google search
        val unknownProvider = com.example.util.WatchProviderLauncher.resolveProviderConfig(
            providerName = "RaiPlay",
            title = "Mare Fuori",
            releaseYear = 2020,
            webLink = "https://www.themoviedb.org/tv/108978/watch?locale=IT",
            directContentUrl = "customscheme://broken-deep-link",
            regionCode = "IT"
        )
        assertTrue(unknownProvider.packageNames.isEmpty())
        assertEquals(
            listOf(
                "https://www.themoviedb.org/tv/108978/watch?locale=IT",
                "https://www.google.com/search?q=watch+Mare+Fuori+2020+on+RaiPlay"
            ),
            unknownProvider.webSearchUrls
        )
        assertEquals(null, com.example.util.WatchProviderLauncher.safeHttpUrl("hotstar://search?q=x"))
        assertEquals(
            "https://www.google.com/search?q=watch+New+Girl+2011",
            com.example.util.WatchProviderLauncher.buildGoogleWatchSearchUrl("Streaming Provider", "New Girl", 2011)
        )
    }

    @Test
    fun searchImdbTop10Charts_containsExactTop10MoviesAndTvWithTmdbHydrationIds() {
        val topMovies = com.example.data.repository.MediaRepository.imdbTop10Movies()
        val topTv = com.example.data.repository.MediaRepository.imdbTop10TvShows()

        assertEquals(10, topMovies.size)
        assertEquals(10, topTv.size)

        // Exact IMDb Top 10 Movies #1..#10
        assertEquals("tmdb_m_278", topMovies[0].id)
        assertEquals("The Shawshank Redemption", topMovies[0].title)
        assertEquals(9.3f, topMovies[0].communityRating, 0.01f)
        assertEquals("The Godfather", topMovies[1].title)
        assertEquals("The Dark Knight", topMovies[2].title)
        assertEquals("The Godfather Part II", topMovies[3].title)
        assertEquals("The Good, the Bad and the Ugly", topMovies[9].title)
        assertTrue(topMovies.all { it.id.startsWith("tmdb_m_") && it.mediaType == MediaType.MOVIE.name })

        // Exact IMDb Top 10 TV Series #1..#10
        assertEquals("tmdb_tv_1396", topTv[0].id)
        assertEquals("Breaking Bad", topTv[0].title)
        assertEquals(9.5f, topTv[0].communityRating, 0.01f)
        assertEquals("Planet Earth II", topTv[1].title)
        assertEquals("Planet Earth", topTv[2].title)
        assertEquals("Band of Brothers", topTv[3].title)
        assertEquals("Chernobyl", topTv[4].title)
        assertEquals("Game of Thrones", topTv[9].title)
        assertTrue(topTv.all { it.id.startsWith("tmdb_tv_") && it.mediaType == MediaType.TV_SHOW.name })
    }

    @Test
    fun guestSaveLibraryBanner_visibleOnlyForUndismissedGuestUsers() {
        assertTrue(
            com.example.data.local.OnboardingPreferences.shouldShowGuestSaveBanner(
                isSignedIn = false,
                isDismissed = false
            )
        )
        assertFalse(
            com.example.data.local.OnboardingPreferences.shouldShowGuestSaveBanner(
                isSignedIn = false,
                isDismissed = true
            )
        )
        assertFalse(
            com.example.data.local.OnboardingPreferences.shouldShowGuestSaveBanner(
                isSignedIn = true,
                isDismissed = false
            )
        )
        assertFalse(
            com.example.data.local.OnboardingPreferences.shouldShowGuestSaveBanner(
                isSignedIn = true,
                isDismissed = true
            )
        )
    }

    @Test
    fun streamingDeepLinkResolver_parsesWikidataAndItunesAndMatchesRegionalProviders() {
        val wikidataJson = """
            {
              "results": {
                "bindings": [
                  {
                    "netflix": { "type": "literal", "value": "70196145" },
                    "hotstar": { "type": "literal", "value": "shows/new-girl/1541498" },
                    "prime": { "type": "literal", "value": "B07BYSTXMF" },
                    "disneySeries": { "type": "literal", "value": "2G2vQ2v5z" },
                    "huluSeries": { "type": "literal", "value": "new-girl-1234" },
                    "max": { "type": "literal", "value": "series/abcd-1234" },
                    "peacock": { "type": "literal", "value": "watch/asset/tv/new-girl/999" },
                    "crunchyroll": { "type": "literal", "value": "GR751KNZY" },
                    "bbc": { "type": "literal", "value": "b006q2x0" }
                  }
                ]
              }
            }
        """.trimIndent()

        val parsed = com.example.util.StreamingDeepLinkResolver.parseWikidataBindings(
            jsonBody = wikidataJson,
            title = "New Girl",
            isMovie = false,
            regionCode = "IN"
        )
        val resolved = com.example.util.ResolvedStreamingDeepLinks(parsed)

        assertEquals(
            "https://www.hotstar.com/in/shows/new-girl/1541498",
            com.example.util.StreamingDeepLinkResolver.matchDirectUrl(resolved, "JioHotstar", "IN")
        )
        assertEquals(
            "https://www.hotstar.com/in/shows/new-girl/1541498",
            com.example.util.StreamingDeepLinkResolver.matchDirectUrl(resolved, "Disney Plus", "IN")
        )
        assertEquals(
            "https://www.disneyplus.com/series/wp/2G2vQ2v5z",
            com.example.util.StreamingDeepLinkResolver.matchDirectUrl(resolved, "Disney Plus", "US")
        )
        assertEquals(
            "https://www.netflix.com/title/70196145",
            com.example.util.StreamingDeepLinkResolver.matchDirectUrl(resolved, "Netflix", "US")
        )
        assertEquals(
            "https://app.primevideo.com/detail?asin=B07BYSTXMF",
            com.example.util.StreamingDeepLinkResolver.matchDirectUrl(resolved, "Amazon Prime Video", "IN")
        )
        assertEquals(
            null,
            com.example.util.StreamingDeepLinkResolver.matchDirectUrl(resolved, "Paramount+ Amazon Channel", "GB")
        )
        assertEquals(
            "https://play.max.com/series/abcd-1234",
            com.example.util.StreamingDeepLinkResolver.matchDirectUrl(resolved, "Max", "US")
        )
        assertEquals(
            "https://www.hulu.com/series/new-girl-1234",
            com.example.util.StreamingDeepLinkResolver.matchDirectUrl(resolved, "Hulu", "US")
        )
        assertEquals(
            "https://www.crunchyroll.com/series/GR751KNZY",
            com.example.util.StreamingDeepLinkResolver.matchDirectUrl(resolved, "Crunchyroll", "US")
        )
        assertEquals(
            "https://www.bbc.co.uk/programmes/b006q2x0",
            com.example.util.StreamingDeepLinkResolver.matchDirectUrl(resolved, "BBC iPlayer", "GB")
        )

        // Numeric Hotstar ID normalizes cleanly to show slug URL
        assertEquals(
            "https://www.hotstar.com/in/shows/new-girl/1541498",
            com.example.util.StreamingDeepLinkResolver.normalizeHotstarUrl(
                raw = "1541498",
                title = "New Girl",
                isMovie = false,
                regionCode = "IN"
            )
        )

        // iTunes response matching for Apple TV
        val itunesJson = """
            {
              "resultCount": 2,
              "results": [
                {
                  "collectionName": "New Girl, Season 1",
                  "collectionViewUrl": "https://tv.apple.com/us/season/season-1/umc.cmc.123?showId=umc.cmc.456",
                  "releaseDate": "2011-09-20T07:00:00Z"
                },
                {
                  "collectionName": "Something Else",
                  "collectionViewUrl": "https://tv.apple.com/us/show/other/umc.cmc.999",
                  "releaseDate": "2020-01-01T00:00:00Z"
                }
              ]
            }
        """.trimIndent()
        assertEquals(
            "https://tv.apple.com/us/season/season-1/umc.cmc.123?showId=umc.cmc.456",
            com.example.util.StreamingDeepLinkResolver.parseItunesSearchResponse(
                jsonBody = itunesJson,
                title = "New Girl",
                isMovie = false,
                releaseYear = 2011
            )
        )

        // Prime Video ASIN direct URL expands to app detail, dp link, and amzn scheme before generic search
        val primeWithAsin = com.example.util.WatchProviderLauncher.resolveProviderConfig(
            providerName = "Amazon Prime Video",
            title = "New Girl",
            directContentUrl = "https://app.primevideo.com/detail?asin=B07BYSTXMF",
            regionCode = "IN"
        )
        assertEquals(
            listOf(
                "https://app.primevideo.com/detail?asin=B07BYSTXMF",
                "https://www.primevideo.com/dp/B07BYSTXMF",
                "amzn://video/detail/B07BYSTXMF",
                "https://app.primevideo.com/search?phrase=New%20Girl"
            ),
            primeWithAsin.verifiedInAppUris
        )

        // Title-targeted detection separates real search/deep links from bare homepages
        assertTrue(
            com.example.util.WatchProviderLauncher.isTitleTargetedWebUrl(
                url = "https://www.hotstar.com/in/explore?search_query=New%20Girl",
                safeDirectWeb = null,
                safeWebLink = null,
                uriTitle = "New%20Girl",
                formTitle = "New+Girl"
            )
        )
        assertFalse(
            com.example.util.WatchProviderLauncher.isTitleTargetedWebUrl(
                url = "https://www.hulu.com/",
                safeDirectWeb = null,
                safeWebLink = null,
                uriTitle = "New%20Girl",
                formTitle = "New+Girl"
            )
        )

        // Wikidata SPARQL includes IMDb ID UNION when available
        val unionSparql = com.example.util.StreamingDeepLinkResolver.buildWikidataSparqlQuery(
            tmdbId = 1420L,
            isMovie = false,
            imdbId = "tt1826940"
        )
        assertTrue(unionSparql.contains("""wdt:P4983 "1420""""))
        assertTrue(unionSparql.contains("""UNION { ?item wdt:P345 "tt1826940" . }"""))
    }
}
