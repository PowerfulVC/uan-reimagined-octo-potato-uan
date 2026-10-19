package uan.mod.helper

import android.app.Application
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import uan.mod.models.AdType
import kotlin.coroutines.resume

/**
 * Suspending, single-shot ad loads. Each call resumes exactly once; retries are opt-in via
 * [withRetry] so one-off loads (splash) never receive late results.
 */
internal sealed interface LoadResult<out T> {
    class Loaded<T>(val ad: T) : LoadResult<T>
    class Failed(val error: LoadAdError?) : LoadResult<Nothing>

    fun adOrNull(): T? = (this as? Loaded<T>)?.ad
}

internal class LoadHelper(private val app: Application) {

    suspend fun loadInter(adUnit: String): LoadResult<InterstitialAd> = load(adUnit) { cont ->
        InterstitialAd.load(app, adUnit, UanRuntime.newRequest(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                UanRuntime.logLoaded(AdType.INTERSTITIAL, ad.responseInfo)
                ad.onPaidEventListener = UanRuntime.paidListener(AdType.INTERSTITIAL, adUnit) { ad.responseInfo }
                cont.resumeIfActive(ad)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                UanRuntime.logFailed(AdType.INTERSTITIAL, error)
                cont.failed(error)
            }
        })
    }

    suspend fun loadReward(adUnit: String): LoadResult<RewardedInterstitialAd> = load(adUnit) { cont ->
        RewardedInterstitialAd.load(app, adUnit, UanRuntime.newRequest(), object : RewardedInterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                UanRuntime.logLoaded(AdType.REWARD, ad.responseInfo)
                ad.onPaidEventListener = UanRuntime.paidListener(AdType.REWARD, adUnit) { ad.responseInfo }
                cont.resumeIfActive(ad)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                UanRuntime.logFailed(AdType.REWARD, error)
                cont.failed(error)
            }
        })
    }

    suspend fun loadAppOpen(adUnit: String): LoadResult<AppOpenAd> = load(adUnit) { cont ->
        AppOpenAd.load(app, adUnit, UanRuntime.newRequest(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                UanRuntime.logLoaded(AdType.OPEN, ad.responseInfo)
                ad.onPaidEventListener = UanRuntime.paidListener(AdType.OPEN, adUnit) { ad.responseInfo }
                cont.resumeIfActive(ad)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                UanRuntime.logFailed(AdType.OPEN, error)
                cont.failed(error)
            }
        })
    }

    suspend fun loadNativeAd(adUnit: String): LoadResult<NativeAd> = load(adUnit) { cont ->
        AdLoader.Builder(app, adUnit)
            .forNativeAd { ad ->
                UanRuntime.logLoaded(AdType.NATIVE, ad.responseInfo)
                ad.setOnPaidEventListener(UanRuntime.paidListener(AdType.NATIVE, adUnit) { ad.responseInfo })
                if (cont.isActive) {
                    // Destroy the ad if the caller was cancelled while the result was in flight.
                    cont.resume(LoadResult.Loaded(ad)) { _, _, _ -> ad.destroy() }
                } else {
                    ad.destroy()
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    UanRuntime.logFailed(AdType.NATIVE, error)
                    cont.failed(error)
                }
            })
            .build()
            .loadAd(UanRuntime.newRequest())
    }

    /**
     * Retries failed loads with exponential backoff (2s, 4s, 8s). Requests that can never succeed
     * (blank ad unit, invalid request, missing app ID) are not retried.
     */
    suspend fun <T> withRetry(maxRetries: Int = 3, load: suspend () -> LoadResult<T>): T? {
        var backoff = 2000L
        repeat(maxRetries + 1) { attempt ->
            val result = load()
            if (result is LoadResult.Loaded) return result.ad
            val code = (result as? LoadResult.Failed)?.error?.code
            if (code == null || code == AdRequest.ERROR_CODE_INVALID_REQUEST || code == AdRequest.ERROR_CODE_APP_ID_MISSING) {
                return null
            }
            if (attempt < maxRetries) {
                delay(backoff)
                backoff *= 2
            }
        }
        return null
    }

    private suspend fun <T> load(
        adUnit: String,
        start: (CancellableContinuation<LoadResult<T>>) -> Unit
    ): LoadResult<T> {
        if (adUnit.isBlank()) return LoadResult.Failed(null)
        return withContext(Dispatchers.Main) { suspendCancellableCoroutine(start) }
    }

    private fun <T> CancellableContinuation<LoadResult<T>>.resumeIfActive(ad: T) {
        if (isActive) resume(LoadResult.Loaded(ad))
    }

    private fun <T> CancellableContinuation<LoadResult<T>>.failed(error: LoadAdError) {
        if (isActive) resume(LoadResult.Failed(error))
    }
}
