//package uan.mod.billing
//
//import android.app.Activity
//
//interface Bill {
//    fun launchPurchaseFlow(activity: Activity, sku: String)
//    fun putSkus(sku: List<String>)
//    fun getSkus(): List<String>
//    fun putSkusSubs(sku: List<String>)
//    fun getSkusSubs(): List<String>
//    suspend fun getPriceList(onPriceReceived: (list: List<String>) -> Unit)
//    suspend fun getPriceListSubs(onPriceReceived: (list: List<String>) -> Unit)
//    fun getPurchases(onPurchasesInfo: () -> Unit): List<String>
//    fun init()
//    fun destroyBilling()
//}