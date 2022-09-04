package uan.mod

import android.app.Activity
import android.app.Application
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.gson.Gson
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.*
import uan.mod.callbacks.OnReInit
import uan.mod.configs.AdUnit
import uan.mod.helper.*
import uan.mod.models.AdType
import uan.mod.net.UnitsRequest
import java.lang.Exception

class AdNewImpl(private val app: Application) : AdNew, OnReInit {
    private var premiumUser: Boolean = false
    private val adUnitsHelper = AdUnitsHelper(app, this)
    private val loadHelper = LoadHelper(app)
    override val frameAds = FrameAds()
    private val adScope = CoroutineScope(Dispatchers.Main + Job())

    private var mInter: InterstitialAd? = null
    private var mReward: RewardedInterstitialAd? = null
    private var mNativeAd: NativeAd? = null
    private var adView: AdView? = null

    override suspend fun setupDefaultAdUnits(strJson: String) {
        if (strJson.isNotEmpty()) {
            try {
                val units = Gson().fromJson(strJson, AdUnit::class.java)
                adUnitsHelper.setDefaultAdUnits(units)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("UAN_ERROR", "Failed to set default ad units  ${e.message}")
            }
        } else {
            Log.e("UAN_ERROR", "Failed to set default ad units. JSON Is empty")
        }
    }


    override suspend fun init(
        projectId: String,
        action: () -> Unit,
        premiumUser: Boolean
    ) {
        this.premiumUser = premiumUser
        UnitsRequest.request(projectId) { adUnit ->
            if (adUnit != null) {
                adUnitsHelper.setSynchronizedAdUnits(adUnit)
            }
            adUnitsHelper.initAd(action)
        }

    }

    override suspend fun showSplashInter(activity: Activity, onAdClosed: () -> Unit) {
        if (premiumUser) {
            delay(2000)
            onAdClosed.invoke()
            return
        }
        adUnitsHelper.verifyAdUnits(AdType.INTERSTITIAL) {
            if (it) {
                adScope.launch(Dispatchers.Main) {
                    val job = Job()
                    launch(job) {
                        delay(7000)
                        onAdClosed.invoke()
                        job.cancel()
                    }
                    launch(job) {
                        loadHelper.loadInter(adUnitsHelper.getAdUnit(AdType.INTERSTITIAL)) { inter ->
                            if (inter == null) {
                                adScope.launch {
                                    delay(3000)
                                    onAdClosed.invoke()
                                }
                            } else {
                                adScope.launch {
                                    inter.fullScreenContentCallback =
                                        object : FullScreenContentCallback() {
                                            override fun onAdDismissedFullScreenContent() {
                                                super.onAdDismissedFullScreenContent()
                                                inter.fullScreenContentCallback = null
                                                onAdClosed.invoke()
                                            }

                                            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                                super.onAdFailedToShowFullScreenContent(p0)
                                                inter.fullScreenContentCallback = null
                                            }
                                        }
                                    inter.show(activity)
                                }
                            }
                            job.cancel()
                        }
                    }
                }
            } else {
                adScope.launch {
                    delay(2000)
                    onAdClosed.invoke()
                }
            }
        }
    }

    override suspend fun showInter(activity: Activity, onAdClosed: () -> Unit) {
        if (premiumUser) {
            onAdClosed.invoke()
            return
        }
        adUnitsHelper.verifyAdUnits(AdType.INTERSTITIAL) {
            if (it) {
                if (adUnitsHelper.providerIsAdmob()) {
                    if (mInter != null) {
                        mInter?.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent()
                                mInter?.fullScreenContentCallback = null
                                mInter = null
                                onAdClosed.invoke()
                                loadInter()
                            }

                            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                super.onAdFailedToShowFullScreenContent(p0)
                                mInter?.fullScreenContentCallback = null
                                mInter = null
                                loadInter()
                            }
                        }
                        mInter?.show(activity)
                    } else {
                        onAdClosed.invoke()
                        loadInter()
                    }
                }
            } else {
                onAdClosed.invoke()
            }
        }
    }

    override suspend fun showReward(
        activity: Activity,
        onRewardClosed: (rewarded: Boolean) -> Unit
    ) {
        if (premiumUser) {
            onRewardClosed.invoke(true)
            return
        }
        adUnitsHelper.verifyAdUnits(AdType.REWARD) {
            if (it) {
                if (adUnitsHelper.providerIsAdmob()) {
                    var rewarded = false
                    if (mReward != null) {
                        mReward?.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent()
                                mReward = null
                                mReward?.fullScreenContentCallback = null
                                onRewardClosed.invoke(rewarded)
                                loadRewardInter()
                            }

                            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                super.onAdFailedToShowFullScreenContent(p0)
                                mReward = null
                                mReward?.fullScreenContentCallback = null
                                loadRewardInter()
                            }
                        }
                        mReward?.show(activity) {
                            rewarded = true
                        }
                    } else {
                        onRewardClosed.invoke(false)
                    }
                }
            } else {
                onRewardClosed.invoke(false)
            }
        }
    }

    override suspend fun showAdInFrame(activity: Activity, frameLayout: FrameLayout) {
        if (premiumUser) {
            frameLayout.visibility = GONE
            return
        }
        adUnitsHelper.verifyAdUnits(AdType.NATIVE) {
            if (it) {
                if (adUnitsHelper.providerIsAdmob()) {
                    val viewTreeObserver = frameLayout.viewTreeObserver
                    viewTreeObserver.addOnGlobalLayoutListener(object :
                        ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            frameLayout.viewTreeObserver.removeGlobalOnLayoutListener(this);
                            val height = frameLayout.height
                            val heightDp = SizeUtils.pxToDp(app, height)

                            when {
                                heightDp >= 270 -> {
                                    adScope.launch {
                                        if (mNativeAd == null) {
                                            mNativeAd = loadNativeAdRuntime().await()
                                        }

                                        frameAds.showNative(frameLayout, false, mNativeAd)
                                        mNativeAd = null
                                    }
                                }
                                heightDp >= 150 -> {
                                    adScope.launch {
                                        if (mNativeAd == null) {
                                            mNativeAd = loadNativeAdRuntime().await()
                                        }

                                        frameAds.showNative(frameLayout, true, mNativeAd)
                                        mNativeAd = null
                                    }
                                }
                                height >= 50 -> {
                                    showBanner(activity, frameLayout)
                                }
                            }
                        }
                    })
                } else {
                    frameLayout.visibility = GONE
                }
            } else {
                frameLayout.visibility = GONE
            }
        }
    }

    override fun onAdReInit() {
        loadHelper.setIsAdmob(adUnitsHelper.providerIsAdmob())
        if (adUnitsHelper.providerIsAdmob()) {
            loadInter()
            loadNativeAd()
            loadRewardInter()
        } else {
            TODO("PRELOAD ALL AD TYPES FOR UNITY")
        }
    }

    private fun loadInter() {
        adScope.launch(Dispatchers.Main) {
            loadHelper.loadInter(adUnitsHelper.getAdUnit(AdType.INTERSTITIAL)) {
                if (mInter == null)
                    mInter = it
            }
        }
    }

    private fun loadRewardInter() {
        adScope.launch(Dispatchers.Main) {
            loadHelper.loadReward(adUnitsHelper.getAdUnit(AdType.REWARD)) {
                if (mReward == null)
                    mReward = it
            }
        }
    }

    private fun loadNativeAd() {
        loadHelper.loadNativeAd(adUnitsHelper.getAdUnit(AdType.NATIVE)) {
            if (mNativeAd == null)
                mNativeAd = it
        }
    }

    override suspend fun destroyNativeAd() {
        mNativeAd?.destroy()
        mNativeAd = null
    }

    private fun loadNativeAdRuntime(): CompletableDeferred<NativeAd?> {
        val runtimeLoad = CompletableDeferred<NativeAd?>()
        val native = AdLoader.Builder(app, adUnitsHelper.getAdUnit(AdType.NATIVE))
            .forNativeAd {
                runtimeLoad.complete(it)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    runtimeLoad.complete(null)
                }
            })
            .build()

        native.loadAd(AdRequest.Builder().build())
        return runtimeLoad
    }

    fun showBanner(activity: Activity, bannerView: FrameLayout) {
        if (premiumUser) {
            bannerView.visibility = GONE
            return
        }
        adUnitsHelper.verifyAdUnits(AdType.NATIVE) {
            if (it) {
                if (adUnitsHelper.providerIsAdmob()) {
                    adView = AdView(activity)
                    adView?.adUnitId = adUnitsHelper.getAdUnit(AdType.BANNER)
                    try {
                        bannerView.addView(adView)
                        loadBanner(activity)
                        bannerView.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        e.printStackTrace()
                        bannerView.visibility = GONE
                    }
                } else {
                    val bottomBanner =
                        BannerView(
                            activity,
                            adUnitsHelper.getAdUnit(AdType.BANNER),
                            UnityBannerSize(320, 50)
                        )
                    bottomBanner.listener = null
                    bottomBanner.load()
                    bannerView.addView(bottomBanner)
                }
            } else {
                bannerView.visibility = GONE
            }

        }

    }

    private fun loadBanner(activity: Activity) {
        if (premiumUser) {
            return
        }
        val adRequest = AdRequest.Builder()
            .build()
        val adSize: AdSize = SizeUtils.getAdSize(activity)
        adView?.setAdSize(adSize)
        adView?.loadAd(adRequest)
    }

}
