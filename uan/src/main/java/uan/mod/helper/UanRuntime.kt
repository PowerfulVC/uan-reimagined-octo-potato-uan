package uan.mod.helper

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.ResponseInfo
import uan.mod.configs.UanAdRevenue
import uan.mod.configs.UanConfig
import uan.mod.models.AdType

/** Process-wide ad request settings shared by every loader. */
internal object UanRuntime {
    @Volatile
    var config = UanConfig()
        set(value) {
            field = value
            UanLog.debug = value.debugLogging
        }

    fun newRequest(): AdRequest {
        val builder = AdRequest.Builder()
        config.networkExtras.forEach { (adapter, extras) ->
            builder.addNetworkExtrasBundle(adapter, extras)
        }
        config.configureRequest?.invoke(builder)
        return builder.build()
    }

    fun requestConfiguration(): RequestConfiguration {
        val builder = RequestConfiguration.Builder()
        if (config.testDeviceIds.isNotEmpty()) builder.setTestDeviceIds(config.testDeviceIds)
        config.configureRequestConfiguration?.invoke(builder)
        return builder.build()
    }

    fun paidListener(
        adType: AdType,
        adUnitId: String,
        responseInfo: () -> ResponseInfo?
    ) = OnPaidEventListener { value ->
        val callback = config.onPaidEvent ?: return@OnPaidEventListener
        val source = responseInfo()?.loadedAdapterResponseInfo
        try {
            callback(
                UanAdRevenue(
                    adType = adType,
                    adUnitId = adUnitId,
                    valueMicros = value.valueMicros,
                    currencyCode = value.currencyCode,
                    precisionType = value.precisionType,
                    adSourceName = source?.adSourceName,
                    adSourceId = source?.adSourceId,
                )
            )
        } catch (e: Exception) {
            UanLog.e("onPaidEvent callback threw", e)
        }
    }

    fun logLoaded(adType: AdType, responseInfo: ResponseInfo?) {
        UanLog.d("$adType loaded from ${responseInfo?.loadedAdapterResponseInfo?.adSourceName ?: "unknown source"}")
    }

    fun logFailed(adType: AdType, error: LoadAdError) {
        UanLog.w("$adType failed to load: [${error.code}] ${error.message}")
        if (UanLog.debug) {
            error.responseInfo?.adapterResponses?.forEach {
                UanLog.d("  ${it.adSourceName} (${it.latencyMillis}ms): ${it.adError?.message ?: "no error"}")
            }
        }
    }
}
