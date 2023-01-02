package uan.mod.helper

import android.app.Application
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.UnityAds
import kotlinx.coroutines.*

internal class LoadHelper(private val app: Application) {
    private var isAdmob: Boolean = true
    private var interReloadAttempts = 0
    private var rewardReloadAttempts = 0
    private var reloadScope = CoroutineScope(Dispatchers.Main + Job())

    fun setIsAdmob(admob: Boolean) {
        isAdmob = admob
    }

    fun loadInter(adUnit: String, interResult: (inter: InterstitialAd?) -> Unit) {
        InterstitialAd.load(
            app,
            adUnit,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    interResult.invoke(null)
                    reloadScope.launch {
                        delay(3000)
                        if (interReloadAttempts >= 3) {
                            return@launch
                        }
                        interReloadAttempts += 1
                        loadInter(adUnit, interResult)
                    }
                }

                override fun onAdLoaded(inter: InterstitialAd) {
                    super.onAdLoaded(inter)
                    interReloadAttempts = 0
                    interResult.invoke(inter)
                }
            })
    }

    fun loadReward(adUnit: String, rewardResult: (reward: RewardedInterstitialAd?) -> Unit) {
        RewardedInterstitialAd.load(
            app,
            adUnit,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(p0: RewardedInterstitialAd) {
                    super.onAdLoaded(p0)
                    rewardReloadAttempts = 0
                    rewardResult.invoke(p0)
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    rewardResult.invoke(null)
                    reloadScope.launch {
                        delay(2000)
                        if (rewardReloadAttempts >= 3) {
                            return@launch
                        }
                        rewardReloadAttempts += 1
                        loadReward(adUnit, rewardResult)
                    }

                }
            })
    }

    fun loadNativeAd(adUnit: String, nativeAdResult: (nativeAd: NativeAd?) -> Unit) {
        AdLoader.Builder(app, adUnit)
            .forNativeAd {
                nativeAdResult.invoke(it)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    nativeAdResult.invoke(null)
                }
            }).build().loadAd(AdRequest.Builder().build())
    }

    fun loadUnityInter(adUnit: String, interResult: () -> Unit){
        UnityAds.load(adUnit, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                interReloadAttempts = 0
                interResult.invoke()
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                reloadScope.launch {
                    delay(3000)
                    if (interReloadAttempts >= 3) {
                        return@launch
                    }
                    interReloadAttempts += 1
                    loadUnityInter(adUnit, interResult)
                }
            }
        })
    }

    fun loadUnityReward(adUnit: String, function: () -> Unit) {
        UnityAds.load(adUnit, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                rewardReloadAttempts = 0
                function.invoke()
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                reloadScope.launch {
                    delay(3000)
                    if (rewardReloadAttempts >= 3) {
                        return@launch
                    }
                    rewardReloadAttempts += 1
                    loadUnityReward(adUnit, function)
                }
            }
        })
    }
}