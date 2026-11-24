package com.example.data.billing

import android.app.Activity
import android.content.Context
import com.example.BuildConfig
import com.example.data.model.MonetizationState
import com.example.data.model.PlanType
import com.example.data.model.ProFeatureQuotaType
import com.example.util.AnalyticsTracker
import com.example.util.OttMoneySaverCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MonetizationManager(
    context: Context,
    private val scope: CoroutineScope,
    private val onSyncEntitlementToCloud: (suspend (MonetizationState) -> Unit)? = null
) {

    companion object {
        private const val PREFS_NAME = "watchpilot_monetization_prefs"

        const val SKU_PRO_MONTHLY = "pro_monthly"
        const val SKU_PRO_ANNUAL = "pro_annual"
        const val SKU_PRO_LIFETIME = "pro_lifetime"

        private const val KEY_IS_PRO = "is_pro"
        private const val KEY_PLAN_TYPE = "plan_type"
        private const val KEY_SNAP_SCANS_USED = "snap_scans_used"
        private const val KEY_CONCIERGE_USED = "concierge_used"
        private const val KEY_MATCH_ROOMS_HOSTED = "match_rooms_hosted"
        private const val KEY_AI_RECAPS_USED = "ai_recaps_used"
        private const val KEY_QUOTA_YEAR_MONTH = "quota_year_month"
        private const val KEY_REWARDED_CREDITS = "rewarded_trial_credits"
        private const val KEY_REWARDED_TODAY = "rewarded_ads_today"
        private const val KEY_REWARDED_DAY_KEY = "rewarded_last_day_key"
        private const val KEY_REWARDED_MONTH = "rewarded_ads_month"
        private const val KEY_REWARDED_MONTH_KEY = "rewarded_month_key"
        private const val KEY_SUBSCRIBED_PROVIDERS = "subscribed_providers"
        private const val KEY_STREAM_ON_MY_APPS = "stream_on_my_apps_only"
        private const val KEY_PRO_ALERTS_ENABLED = "pro_alerts_enabled"
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadPersistedState())
    val state: StateFlow<MonetizationState> = _state.asStateFlow()

    private val billingGateway: PlayBillingGateway = EditionBillingProvider.createGateway(
        context = appContext,
        scope = scope,
        callbacks = object : PlayBillingCallbacks {
            override fun onConnectionChanged(connected: Boolean) {
                _state.update { it.copy(isPlayBillingConnected = connected) }
            }

            override fun onPricesUpdated(prices: PlayBillingCatalogPrices) {
                _state.update { curr ->
                    curr.copy(
                        monthlyPriceLabel = prices.monthlyLabel ?: curr.monthlyPriceLabel,
                        annualPriceLabel = prices.annualLabel ?: curr.annualPriceLabel,
                        lifetimePriceLabel = prices.lifetimeLabel ?: curr.lifetimePriceLabel
                    )
                }
            }

            override fun onPlansUnlocked(planType: PlanType, statusMessage: String) {
                AnalyticsTracker.logProUnlocked(planType.name, isRealPlayBilling = true)
                updateAndPersist { curr ->
                    curr.copy(
                        isPro = true,
                        planType = planType,
                        billingStatusMessage = statusMessage
                    )
                }
            }

            override fun onNoActivePurchasesOnRestore(currentPlanType: PlanType) {
                _state.update { curr ->
                    if (curr.planType == PlanType.PRO_SANDBOX && BuildConfig.DEBUG) {
                        curr.copy(billingStatusMessage = "Sandbox Preview Pro is currently active.")
                    } else {
                        curr.copy(billingStatusMessage = "No active Google Play purchases found on this Google account.")
                    }
                }
            }

            override fun onPlayPurchasesVerifiedEmpty(currentPlanType: PlanType) {
                val curr = _state.value
                if (shouldRevokePlayEntitlementOnEmptyQuery(curr, BuildConfig.DEBUG)) {
                    updateAndPersist { state ->
                        state.copy(
                            isPro = false,
                            planType = PlanType.FREE,
                            streamOnMyAppsOnly = false,
                            proAlertsEnabled = false
                        )
                    }
                }
            }

            override fun onStatusMessage(message: String) {
                _state.update { it.copy(billingStatusMessage = message) }
            }
        }
    )

    init {
        connectToGooglePlayBilling()
    }

    fun refreshQuotasIfRolledOver() {
        val current = _state.value
        val normalized = MonetizationQuotaPolicy.normalizeForDate(current)
        if (normalized != current) {
            updateAndPersist(syncCloud = false) { normalized }
        }
    }

    fun connectToGooglePlayBilling() {
        billingGateway.connect()
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        billingGateway.launchPurchaseFlow(activity, productId)
    }

    fun restorePurchases() {
        billingGateway.restorePurchases(
            silent = false,
            currentPlanType = _state.value.planType
        )
    }

    fun activateSandboxPro(plan: PlanType = PlanType.PRO_SANDBOX) {
        if (!BuildConfig.DEBUG) return
        updateAndPersist { curr ->
            curr.copy(
                isPro = true,
                planType = plan,
                billingStatusMessage = "WatchPilot Pro (Sandbox Preview) activated — Ads removed & all Pro features unlocked!"
            )
        }
    }

    fun destroy() {
        billingGateway.destroy()
    }

    fun deactivateSandboxPro() {
        updateAndPersist { curr ->
            curr.copy(
                isPro = false,
                planType = PlanType.FREE,
                streamOnMyAppsOnly = false,
                proAlertsEnabled = false,
                billingStatusMessage = "Switched back to WatchPilot Free tier."
            )
        }
    }

    fun clearBillingMessage() {
        _state.update { it.copy(billingStatusMessage = null) }
    }

    fun grantRewardedTrialPass(): Boolean {
        val before = MonetizationQuotaPolicy.normalizeForDate(_state.value)
        if (!before.canWatchRewardedTrialAd) return false
        updateAndPersist(syncCloud = false) { curr ->
            MonetizationQuotaPolicy.recordRewardedAdEarned(curr).copy(
                billingStatusMessage = "1 Pro Trial Pass unlocked! Ready for your next AI session or Match Room."
            )
        }
        return true
    }

    fun recordSnapScanUsed() {
        updateAndPersist(syncCloud = false) { curr ->
            MonetizationQuotaPolicy.consumeFeatureUse(curr, ProFeatureQuotaType.SNAP_SCAN)
        }
    }

    fun recordConciergeUsed() {
        updateAndPersist(syncCloud = false) { curr ->
            MonetizationQuotaPolicy.consumeFeatureUse(curr, ProFeatureQuotaType.AI_CONCIERGE)
        }
    }

    fun recordMatchRoomHosted() {
        updateAndPersist(syncCloud = false) { curr ->
            MonetizationQuotaPolicy.consumeFeatureUse(curr, ProFeatureQuotaType.MATCH_ROOM_HOST)
        }
    }

    fun recordAiRecapUsed() {
        updateAndPersist(syncCloud = false) { curr ->
            MonetizationQuotaPolicy.consumeFeatureUse(curr, ProFeatureQuotaType.AI_RECAP)
        }
    }

    fun toggleSubscribedProvider(providerName: String) {
        val canonical = OttMoneySaverCalculator.canonicalizeProvider(providerName)
        if (canonical.isBlank()) return
        updateAndPersist { curr ->
            val updatedSet = curr.subscribedProviders.toMutableSet()
            if (canonical in updatedSet) updatedSet.remove(canonical) else updatedSet.add(canonical)
            curr.copy(
                subscribedProviders = updatedSet,
                streamOnMyAppsOnly = if (updatedSet.isEmpty()) false else curr.streamOnMyAppsOnly
            )
        }
    }

    fun setStreamOnMyAppsOnly(enabled: Boolean) {
        updateAndPersist { curr ->
            curr.copy(streamOnMyAppsOnly = enabled && curr.subscribedProviders.isNotEmpty())
        }
    }

    fun setProAlertsEnabled(enabled: Boolean) {
        updateAndPersist { curr ->
            curr.copy(proAlertsEnabled = curr.isPro && enabled)
        }
    }

    fun mergeCloudEntitlement(
        cloudIsPro: Boolean,
        cloudPlanType: String,
        cloudProvidersCsv: String,
        cloudStreamOnMyAppsOnly: Boolean
    ) {
        val parsedPlan = runCatching { PlanType.valueOf(cloudPlanType) }.getOrDefault(PlanType.FREE)
        val effectiveCloudPro = cloudIsPro && (BuildConfig.DEBUG || parsedPlan != PlanType.PRO_SANDBOX)
        val cloudProviders = cloudProvidersCsv.split(",")
            .map { OttMoneySaverCalculator.canonicalizeProvider(it) }
            .filter { it.isNotBlank() }
            .toSet()

        updateAndPersist(syncCloud = false) { curr ->
            val mergedPro = curr.isPro || effectiveCloudPro
            val mergedPlan = when {
                !mergedPro -> PlanType.FREE
                curr.isPro && curr.planType != PlanType.FREE -> curr.planType
                effectiveCloudPro -> parsedPlan
                else -> PlanType.FREE
            }
            val mergedProviders = if (cloudProviders.isNotEmpty()) (curr.subscribedProviders + cloudProviders) else curr.subscribedProviders
            curr.copy(
                isPro = mergedPro,
                planType = mergedPlan,
                subscribedProviders = mergedProviders,
                streamOnMyAppsOnly = curr.streamOnMyAppsOnly || (cloudStreamOnMyAppsOnly && mergedProviders.isNotEmpty())
            )
        }
    }

    fun resetOnSignOut() {
        prefs.edit().clear().commit()
        _state.value = MonetizationQuotaPolicy.normalizeForDate(MonetizationState())
    }

    private fun updateAndPersist(
        syncCloud: Boolean = true,
        transform: (MonetizationState) -> MonetizationState
    ) {
        val updated = transform(_state.value)
        _state.value = updated
        prefs.edit()
            .putBoolean(KEY_IS_PRO, updated.isPro)
            .putString(KEY_PLAN_TYPE, updated.planType.name)
            .putInt(KEY_SNAP_SCANS_USED, updated.snapScansUsed)
            .putInt(KEY_CONCIERGE_USED, updated.aiConciergeUsed)
            .putInt(KEY_MATCH_ROOMS_HOSTED, updated.matchRoomsHosted)
            .putInt(KEY_AI_RECAPS_USED, updated.aiRecapsUsed)
            .putString(KEY_QUOTA_YEAR_MONTH, updated.quotaYearMonth)
            .putInt(KEY_REWARDED_CREDITS, updated.rewardedTrialCredits)
            .putInt(KEY_REWARDED_TODAY, updated.rewardedAdsWatchedToday)
            .putString(KEY_REWARDED_DAY_KEY, updated.rewardedLastDayKey)
            .putInt(KEY_REWARDED_MONTH, updated.rewardedAdsWatchedThisMonth)
            .putString(KEY_REWARDED_MONTH_KEY, updated.rewardedMonthKey)
            .putString(KEY_SUBSCRIBED_PROVIDERS, updated.subscribedProviders.joinToString(","))
            .putBoolean(KEY_STREAM_ON_MY_APPS, updated.streamOnMyAppsOnly)
            .putBoolean(KEY_PRO_ALERTS_ENABLED, updated.proAlertsEnabled)
            .apply()

        if (syncCloud && onSyncEntitlementToCloud != null) {
            scope.launch(Dispatchers.IO) {
                onSyncEntitlementToCloud.invoke(updated)
            }
        }
    }

    private fun loadPersistedState(): MonetizationState {
        val isPro = prefs.getBoolean(KEY_IS_PRO, false)
        val planStr = prefs.getString(KEY_PLAN_TYPE, PlanType.FREE.name) ?: PlanType.FREE.name
        val planType = runCatching { PlanType.valueOf(planStr) }.getOrDefault(PlanType.FREE)
        val providersCsv = prefs.getString(KEY_SUBSCRIBED_PROVIDERS, "").orEmpty()
        val providers = providersCsv.split(",")
            .map { OttMoneySaverCalculator.canonicalizeProvider(it) }
            .filter { it.isNotBlank() }
            .toSet()

        val effectivePlanType = when {
            !isPro -> PlanType.FREE
            planType == PlanType.PRO_SANDBOX && !BuildConfig.DEBUG -> PlanType.FREE
            planType == PlanType.FREE -> if (BuildConfig.DEBUG) PlanType.PRO_SANDBOX else PlanType.FREE
            else -> planType
        }
        val effectiveIsPro = isPro && effectivePlanType != PlanType.FREE

        val raw = MonetizationState(
            isPro = effectiveIsPro,
            planType = effectivePlanType,
            snapScansUsed = prefs.getInt(KEY_SNAP_SCANS_USED, 0),
            aiConciergeUsed = prefs.getInt(KEY_CONCIERGE_USED, 0),
            matchRoomsHosted = prefs.getInt(KEY_MATCH_ROOMS_HOSTED, 0),
            aiRecapsUsed = prefs.getInt(KEY_AI_RECAPS_USED, 0),
            quotaYearMonth = prefs.getString(KEY_QUOTA_YEAR_MONTH, "").orEmpty(),
            rewardedTrialCredits = prefs.getInt(KEY_REWARDED_CREDITS, 0),
            rewardedAdsWatchedToday = prefs.getInt(KEY_REWARDED_TODAY, 0),
            rewardedLastDayKey = prefs.getString(KEY_REWARDED_DAY_KEY, "").orEmpty(),
            rewardedAdsWatchedThisMonth = prefs.getInt(KEY_REWARDED_MONTH, 0),
            rewardedMonthKey = prefs.getString(KEY_REWARDED_MONTH_KEY, "").orEmpty(),
            subscribedProviders = providers,
            streamOnMyAppsOnly = prefs.getBoolean(KEY_STREAM_ON_MY_APPS, false) && providers.isNotEmpty(),
            proAlertsEnabled = effectiveIsPro && prefs.getBoolean(KEY_PRO_ALERTS_ENABLED, false)
        )
        return MonetizationQuotaPolicy.normalizeForDate(raw)
    }
}

internal fun shouldRevokePlayEntitlementOnEmptyQuery(
    state: MonetizationState,
    isDebugBuild: Boolean
): Boolean {
    if (!state.isPro) return false
    if (isDebugBuild && state.planType == PlanType.PRO_SANDBOX) return false
    return true
}
