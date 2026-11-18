package com.example.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.example.BuildConfig
import com.example.data.model.PlanType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * WatchPilot v2 Edition Google Play Billing 7.x Gateway.
 */
object EditionBillingProvider {
    fun createGateway(
        context: Context,
        scope: CoroutineScope,
        callbacks: PlayBillingCallbacks
    ): PlayBillingGateway = PlayBillingClientGateway(context.applicationContext, scope, callbacks)
}

private class PlayBillingClientGateway(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val callbacks: PlayBillingCallbacks
) : PlayBillingGateway {

    companion object {
        private const val TAG = "PlayBillingGateway"
    }

    private val productDetailsCache = mutableMapOf<String, ProductDetails>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    scope.launch(Dispatchers.IO) {
                        handlePurchases(purchases)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                callbacks.onStatusMessage("Checkout canceled.")
            }
            else -> {
                callbacks.onStatusMessage(
                    "Google Play Billing: ${billingResult.debugMessage.ifBlank { "Unable to complete purchase." }}"
                )
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    override fun connect() {
        if (billingClient.isReady) {
            scope.launch(Dispatchers.IO) {
                refreshCatalogAndPurchases()
            }
            return
        }
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    val connected = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    callbacks.onConnectionChanged(connected)
                    if (connected) {
                        scope.launch(Dispatchers.IO) {
                            refreshCatalogAndPurchases()
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    callbacks.onConnectionChanged(false)
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Google Play Billing unavailable in this environment: ${e.message}")
        }
    }

    private suspend fun refreshCatalogAndPurchases() {
        try {
            val subsQuery = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(MonetizationManager.SKU_PRO_MONTHLY, MonetizationManager.SKU_PRO_ANNUAL).map { productId ->
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    }
                )
                .build()
            val subsResult = billingClient.queryProductDetails(subsQuery)
            subsResult.productDetailsList?.forEach { details ->
                productDetailsCache[details.productId] = details
            }

            val inAppQuery = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(MonetizationManager.SKU_PRO_LIFETIME)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()
            val inAppResult = billingClient.queryProductDetails(inAppQuery)
            inAppResult.productDetailsList?.forEach { details ->
                productDetailsCache[details.productId] = details
            }

            val monthlyFormatted = productDetailsCache[MonetizationManager.SKU_PRO_MONTHLY]
                ?.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                ?.let { "$it / mo" }

            val annualFormatted = productDetailsCache[MonetizationManager.SKU_PRO_ANNUAL]
                ?.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                ?.let { "$it / yr" }

            val lifetimeFormatted = productDetailsCache[MonetizationManager.SKU_PRO_LIFETIME]
                ?.oneTimePurchaseOfferDetails?.formattedPrice
                ?.let { "$it once" }

            callbacks.onPricesUpdated(
                PlayBillingCatalogPrices(
                    monthlyLabel = monthlyFormatted,
                    annualLabel = annualFormatted,
                    lifetimeLabel = lifetimeFormatted
                )
            )

            restorePurchasesInternal(silent = true, currentPlanType = PlanType.FREE)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query Play Billing catalog: ${e.message}")
        }
    }

    override fun launchPurchaseFlow(activity: Activity, productId: String) {
        val details = productDetailsCache[productId]
        if (!billingClient.isReady || details == null) {
            val message = if (BuildConfig.DEBUG) {
                "Play Store SKU '$productId' is not yet active on this build. Use Sandbox Preview below to test WatchPilot Pro immediately, or publish '$productId' in Play Console."
            } else {
                "Google Play billing is currently unavailable. Please ensure you're signed in to Google Play and try again."
            }
            callbacks.onStatusMessage(message)
            connect()
            return
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken.isNullOrBlank()) {
                callbacks.onStatusMessage("No active subscription offer found for $productId.")
                return
            }
            productParamsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun restorePurchases(silent: Boolean, currentPlanType: PlanType) {
        scope.launch(Dispatchers.IO) {
            if (!billingClient.isReady) {
                connect()
                if (!silent) {
                    callbacks.onStatusMessage("Connecting to Google Play to restore purchases...")
                }
                return@launch
            }
            restorePurchasesInternal(silent = silent, currentPlanType = currentPlanType)
        }
    }

    private suspend fun restorePurchasesInternal(silent: Boolean, currentPlanType: PlanType) {
        try {
            val subsPurchases = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
            ).purchasesList

            val inAppPurchases = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
            ).purchasesList

            val allActive = (subsPurchases + inAppPurchases).filter {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }

            if (allActive.isNotEmpty()) {
                handlePurchases(allActive)
                if (!silent) {
                    callbacks.onStatusMessage("WatchPilot Pro restored successfully!")
                }
            } else if (!silent) {
                callbacks.onNoActivePurchasesOnRestore(currentPlanType)
            }
        } catch (e: Exception) {
            if (!silent) {
                callbacks.onStatusMessage("Restore failed: ${e.localizedMessage ?: "Play Store error"}")
            }
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        var resolvedPlan: PlanType? = null
        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams)
            }
            val products = purchase.products
            resolvedPlan = when {
                MonetizationManager.SKU_PRO_LIFETIME in products -> PlanType.PRO_LIFETIME
                MonetizationManager.SKU_PRO_ANNUAL in products -> PlanType.PRO_ANNUAL
                MonetizationManager.SKU_PRO_MONTHLY in products -> PlanType.PRO_MONTHLY
                else -> PlanType.PRO_ANNUAL
            }
        }

        if (resolvedPlan != null) {
            callbacks.onPlansUnlocked(resolvedPlan, "${resolvedPlan.displayLabel} unlocked!")
        }
    }

    override fun destroy() {
        runCatching { billingClient.endConnection() }
    }
}
