package com.example.data.billing

import android.app.Activity
import android.content.Context
import com.example.data.model.PlanType
import kotlinx.coroutines.CoroutineScope

/**
 * WatchPilot v1 Edition Billing Delegate.
 * Keeps Google Play Billing dependencies and BILLING permission completely out of v1 builds.
 */
object EditionBillingProvider {
    fun createGateway(
        context: Context,
        scope: CoroutineScope,
        callbacks: PlayBillingCallbacks
    ): PlayBillingGateway = NoOpPlayBillingGateway()
}

private class NoOpPlayBillingGateway : PlayBillingGateway {
    override fun connect() = Unit
    override fun launchPurchaseFlow(activity: Activity, productId: String) = Unit
    override fun restorePurchases(silent: Boolean, currentPlanType: PlanType) = Unit
    override fun destroy() = Unit
}
