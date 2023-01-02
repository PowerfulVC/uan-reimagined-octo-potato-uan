//package uan.mod.billing
//
//import android.app.Activity
//import android.content.Context
//import android.content.Intent
//import android.os.Handler
//import android.widget.Toast
//import com.android.billingclient.api.*
//
//class BillImpl(private val appContext: Context) : Bill, BillingListener {
//    private var billingCore: BillingHelper? = null
//    private var skusList: List<String> = emptyList()
//    private var skusListSub: List<String> = emptyList()
//
//    override fun launchPurchaseFlow(activity: Activity, sku: String) {
//        TODO("Not yet implemented")
//    }
//
//    override fun putSkus(sku: List<String>) {
//        this.skusList = sku
//    }
//
//    override fun getSkus(): List<String> = skusList
//
//    override fun putSkusSubs(sku: List<String>) {
//        this.skusListSub = sku
//    }
//
//    override fun getSkusSubs(): List<String> = skusListSub
//
//    override suspend fun getPriceList(onPriceReceived: (list: List<String>) -> Unit) {
//        val list = arrayListOf<String>()
//        skusList.forEach {
//            list.add(billingCore?.getSkuDetails(it)?.originalPrice.toString())
//        }
//        onPriceReceived.invoke(list)
//    }
//
//    override suspend fun getPriceListSubs(onPriceReceived: (list: List<String>) -> Unit) {
//        val list = arrayListOf<String>()
//        skusListSub.forEach {
//            list.add(billingCore?.getSkuDetails(it)?.originalPrice.toString())
//        }
//        onPriceReceived.invoke(list)
//    }
//
//    override fun getPurchases(onPurchasesInfo: () -> Unit): List<String> {
//        TODO("Not yet implemented")
//    }
//
//    override fun init() {
//        billingCore = BillingHelper(
//            context = appContext,
//            billingListener = this,
//            skuInAppPurchases = skusList,
//            skuSubscriptions = skusListSub,
//        )
//
//    }
//
//    override fun destroyBilling() {
//        billingCore?.endClientConnection()
//    }
//
//    override fun onBillingEvent(event: BillingEvent, message: String?, responseCode: Int?) {
//
//    }
//
//}