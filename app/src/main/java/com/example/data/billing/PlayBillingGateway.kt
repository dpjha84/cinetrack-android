package com.example.data.billing

import android.app.Activity
import android.content.Context
import com.example.data.model.PlanType
import kotlinx.coroutines.CoroutineScope

data class PlayBillingCatalogPrices(
    val monthlyLabel: String? = null,
    val annualLabel: String? = null,
    val lifetimeLabel: String? = null
)

interface PlayBillingCallbacks {
    fun onConnectionChanged(connected: Boolean)
    fun onPricesUpdated(prices: PlayBillingCatalogPrices)
    fun onPlansUnlocked(planType: PlanType, statusMessage: String)
    fun onNoActivePurchasesOnRestore(currentPlanType: PlanType)
    fun onStatusMessage(message: String)
}

interface PlayBillingGateway {
    fun connect()
    fun launchPurchaseFlow(activity: Activity, productId: String)
    fun restorePurchases(silent: Boolean, currentPlanType: PlanType)
    fun destroy()
}
