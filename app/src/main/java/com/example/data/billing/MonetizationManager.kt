package com.example.data.billing

import android.app.Activity
import android.content.Context
import com.example.BuildConfig
import com.example.data.model.MonetizationState
import com.example.data.model.PlanType
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
        private const val KEY_SUBSCRIBED_PROVIDERS = "subscribed_providers"
        private const val KEY_STREAM_ON_MY_APPS = "stream_on_my_apps_only"
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
                if (BuildConfig.IS_V1_EDITION) return
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
                    if (curr.planType == PlanType.PRO_SANDBOX) {
                        curr.copy(billingStatusMessage = "Sandbox Preview Pro is currently active.")
                    } else {
                        curr.copy(billingStatusMessage = "No active Google Play purchases found on this Google account.")
                    }
                }
            }

            override fun onStatusMessage(message: String) {
                _state.update { it.copy(billingStatusMessage = message) }
            }
        }
    )

    init {
        if (!BuildConfig.IS_V1_EDITION) {
            connectToGooglePlayBilling()
        }
    }

    fun connectToGooglePlayBilling() {
        if (BuildConfig.IS_V1_EDITION) return
        billingGateway.connect()
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        if (BuildConfig.IS_V1_EDITION) return
        billingGateway.launchPurchaseFlow(activity, productId)
    }

    fun restorePurchases() {
        if (BuildConfig.IS_V1_EDITION) return
        billingGateway.restorePurchases(
            silent = false,
            currentPlanType = _state.value.planType
        )
    }

    fun activateSandboxPro(plan: PlanType = PlanType.PRO_SANDBOX) {
        if (BuildConfig.IS_V1_EDITION || !BuildConfig.DEBUG) return
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
                billingStatusMessage = "Switched back to WatchPilot Free tier."
            )
        }
    }

    fun clearBillingMessage() {
        _state.update { it.copy(billingStatusMessage = null) }
    }

    fun recordSnapScanUsed() {
        if (_state.value.isPro) return
        updateAndPersist(syncCloud = false) { curr ->
            curr.copy(snapScansUsed = curr.snapScansUsed + 1)
        }
    }

    fun recordConciergeUsed() {
        if (_state.value.isPro) return
        updateAndPersist(syncCloud = false) { curr ->
            curr.copy(aiConciergeUsed = curr.aiConciergeUsed + 1)
        }
    }

    fun recordMatchRoomHosted() {
        if (_state.value.isPro) return
        updateAndPersist(syncCloud = false) { curr ->
            curr.copy(matchRoomsHosted = curr.matchRoomsHosted + 1)
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

    fun mergeCloudEntitlement(
        cloudIsPro: Boolean,
        cloudPlanType: String,
        cloudProvidersCsv: String,
        cloudStreamOnMyAppsOnly: Boolean
    ) {
        val parsedPlan = runCatching { PlanType.valueOf(cloudPlanType) }.getOrDefault(PlanType.FREE)
        val cloudProviders = cloudProvidersCsv.split(",")
            .map { OttMoneySaverCalculator.canonicalizeProvider(it) }
            .filter { it.isNotBlank() }
            .toSet()

        updateAndPersist(syncCloud = false) { curr ->
            val mergedPro = !BuildConfig.IS_V1_EDITION && (curr.isPro || cloudIsPro)
            val mergedPlan = when {
                !mergedPro -> PlanType.FREE
                curr.isPro && curr.planType != PlanType.FREE -> curr.planType
                cloudIsPro -> parsedPlan
                else -> PlanType.FREE
            }
            val mergedProviders = if (cloudProviders.isNotEmpty()) (curr.subscribedProviders + cloudProviders) else curr.subscribedProviders
            curr.copy(
                isPro = mergedPro,
                planType = mergedPlan,
                subscribedProviders = mergedProviders,
                streamOnMyAppsOnly = !BuildConfig.IS_V1_EDITION && (curr.streamOnMyAppsOnly || (cloudStreamOnMyAppsOnly && mergedProviders.isNotEmpty()))
            )
        }
    }

    fun resetOnSignOut() {
        prefs.edit().clear().commit()
        _state.value = MonetizationState()
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
            .putString(KEY_SUBSCRIBED_PROVIDERS, updated.subscribedProviders.joinToString(","))
            .putBoolean(KEY_STREAM_ON_MY_APPS, updated.streamOnMyAppsOnly)
            .apply()

        if (syncCloud && onSyncEntitlementToCloud != null && !BuildConfig.IS_V1_EDITION) {
            scope.launch(Dispatchers.IO) {
                onSyncEntitlementToCloud.invoke(updated)
            }
        }
    }

    private fun loadPersistedState(): MonetizationState {
        if (BuildConfig.IS_V1_EDITION) {
            return MonetizationState()
        }
        val isPro = prefs.getBoolean(KEY_IS_PRO, false)
        val planStr = prefs.getString(KEY_PLAN_TYPE, PlanType.FREE.name) ?: PlanType.FREE.name
        val planType = runCatching { PlanType.valueOf(planStr) }.getOrDefault(PlanType.FREE)
        val providersCsv = prefs.getString(KEY_SUBSCRIBED_PROVIDERS, "").orEmpty()
        val providers = providersCsv.split(",")
            .map { OttMoneySaverCalculator.canonicalizeProvider(it) }
            .filter { it.isNotBlank() }
            .toSet()

        return MonetizationState(
            isPro = isPro,
            planType = if (isPro && planType == PlanType.FREE) PlanType.PRO_SANDBOX else planType,
            snapScansUsed = prefs.getInt(KEY_SNAP_SCANS_USED, 0),
            aiConciergeUsed = prefs.getInt(KEY_CONCIERGE_USED, 0),
            matchRoomsHosted = prefs.getInt(KEY_MATCH_ROOMS_HOSTED, 0),
            subscribedProviders = providers,
            streamOnMyAppsOnly = prefs.getBoolean(KEY_STREAM_ON_MY_APPS, false) && providers.isNotEmpty()
        )
    }
}
