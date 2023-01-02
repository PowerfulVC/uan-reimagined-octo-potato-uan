package uan.mod.billing

import com.android.billingclient.api.*
import uan.mod.billing.BillingEvent

interface BillingListener {
    /**
     * @param event one of resolved [BillingEvent]s
     * @param message as reported by [BillingResult.getDebugMessage]
     * @param responseCode as reported by [BillingResult.getResponseCode] as [BillingClient.BillingResponseCode].
     */
    fun onBillingEvent(event: BillingEvent, message: String?, responseCode: Int?)
}