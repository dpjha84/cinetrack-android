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
}
