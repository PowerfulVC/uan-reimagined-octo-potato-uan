package uan.mod

import android.app.Activity
import android.app.Application
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.gson.Gson
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.*
import uan.mod.callbacks.OnReInit
import uan.mod.configs.AdUnit
import uan.mod.configs.NativeAdKeys
import uan.mod.helper.*
import uan.mod.models.AdType
import uan.mod.net.UnitsRequest
import uan.mod.use.AppOpenManager
import java.lang.Exception


class AdImpl(private val app: Application) : Ad, OnReInit {
    private var premiumUser: Boolean = false
    override var adUnitsHelper: AdUnitsHelper? = null
    override var unitsRequest: UnitsRequest? = null
    private val loadHelper = LoadHelper(app)
    override val frameAds = FrameAds()
    private val adScope = CoroutineScope(Dispatchers.Main + Job())
    private val mainScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mInter: InterstitialAd? = null
    private var mReward: RewardedInterstitialAd? = null
    private var mNativeAd: NativeAd? = null
    private var adView: AdView? = null
    private var globalCallback: OnReInit = object : OnReInit {
        override fun onAdReInit() {
            Log.d("UAN-RQ", "UAN On Ad Re Initialized")
            setupOpenAds(app)
        }
    }

    private var appOpenManager: AppOpenManager? = null


    init {
        unitsRequest = UnitsRequest()
        adUnitsHelper = AdUnitsHelper(app, this, unitsRequest ?: UnitsRequest())
    }

    override suspend fun setupDefaultAdUnits(strJson: String) {
        if (strJson.isNotEmpty()) {
            try {
                val units = Gson().fromJson(strJson, AdUnit::class.java)
                adUnitsHelper?.setDefaultAdUnits(units)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("UAN_ERROR", "Failed to set default ad units  ${e.message}")
            }
        } else {
            Log.e("UAN_ERROR", "Failed to set default ad units. JSON Is empty")
        }
    }


    override suspend fun init(
        projectId: String, premiumUser: Boolean
    ): Job {
        this@AdImpl.premiumUser = premiumUser
        unitsRequest?.setupUnitsUrl(projectId)

        return mainScope.launch {
            initInternal().await()
        }

    }

    private suspend fun initInternal(): CompletableDeferred<Boolean> {
        val completableDeferred = CompletableDeferred<Boolean>()
        unitsRequest?.request { adUnit ->
            if (adUnit != null) {
                Log.d("UAN", "UAN REQUESTED AD UNITS ${System.currentTimeMillis()}")
                adUnitsHelper?.setSynchronizedAdUnits(adUnit)
            }
            adUnitsHelper?.initAd {
                completableDeferred.complete(true)
            }
        }
        return completableDeferred
    }

    override fun setupOpenAds(application: Application) {
        adScope.launch(Dispatchers.Main) {
            if (!this@AdImpl.premiumUser) {
                if (adUnitsHelper != null) {
                    if (appOpenManager == null) {
                        appOpenManager =
                            AppOpenManager(application, adUnitsHelper!!)
                        application.unregisterActivityLifecycleCallbacks(appOpenManager)
                        application.registerActivityLifecycleCallbacks(appOpenManager)
                        ProcessLifecycleOwner.get().lifecycle.removeObserver(appOpenManager!!)
                        ProcessLifecycleOwner.get().lifecycle.addObserver(appOpenManager!!)
                        Log.d("Info", "Open ads initialization")
                    }
                }
            }
        }
    }

    override fun setOpenAdsRestriction(restrictionUnit: (activity: Activity?) -> Boolean) {
        appOpenManager?.openAdsRestricted = restrictionUnit
        Log.d("Info", "Set restrictions to open ads")
    }

    override suspend fun showSplashInter(activity: Activity, onAdClosed: () -> Unit) {
        if (premiumUser) {
            delay(2000)
            onAdClosed.invoke()
            return
        }
        adUnitsHelper?.verifyAdUnits(AdType.INTERSTITIAL) {
            if (it) {
                if (adUnitsHelper?.providerIsAdmob() == true) {
                    adScope.launch(Dispatchers.Main) {
                        val scopeTimeout = CoroutineScope(Dispatchers.Main + SupervisorJob())
                        scopeTimeout.launch {
                            delay(7000)
                            onAdClosed.invoke()
                            scopeTimeout.cancel()
                            Log.d("UAN", "JOB CANCEL TIMEOUT")
                        }
                        scopeTimeout.launch {
                            loadHelper.loadInter(
                                adUnitsHelper?.getAdUnit(AdType.INTERSTITIAL).toString()
                            ) { inter ->
                                if (inter == null) {
                                    Log.d("UAN", "SPLASH INTER IS NULL")
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
                                Log.d("UAN", "JOB CANCEL LOADED INTER")
                                scopeTimeout.cancel()
                            }

                        }
                    }
                } else {
                    adScope.launch {
                        delay(5000)
                        UnityAds.show(activity,
                            adUnitsHelper?.getAdUnit(AdType.INTERSTITIAL),
                            UnityAdsShowOptions(),
                            object : IUnityAdsShowListener {
                                override fun onUnityAdsShowFailure(
                                    placementId: String?,
                                    error: UnityAds.UnityAdsShowError?,
                                    message: String?
                                ) {
                                    onAdClosed.invoke()
                                    loadUnityInter()
                                }

                                override fun onUnityAdsShowStart(placementId: String?) {}
                                override fun onUnityAdsShowClick(placementId: String?) {}
                                override fun onUnityAdsShowComplete(
                                    placementId: String?,
                                    state: UnityAds.UnityAdsShowCompletionState?
                                ) {
                                    loadUnityInter()
                                    onAdClosed.invoke()
                                }

                            })
                    }
                }
            } else {
                Log.e("UAN", "AD UNIT INTER INCORRECT")
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
        adUnitsHelper?.verifyAdUnits(AdType.INTERSTITIAL) {
            if (it) {
                if (adUnitsHelper?.providerIsAdmob() == true) {
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
                } else {
                    UnityAds.show(activity,
                        adUnitsHelper?.getAdUnit(AdType.INTERSTITIAL),
                        UnityAdsShowOptions(),
                        object : IUnityAdsShowListener {
                            override fun onUnityAdsShowFailure(
                                placementId: String?,
                                error: UnityAds.UnityAdsShowError?,
                                message: String?
                            ) {
                                onAdClosed.invoke()
                                loadUnityInter()
                            }

                            override fun onUnityAdsShowStart(placementId: String?) {}
                            override fun onUnityAdsShowClick(placementId: String?) {}
                            override fun onUnityAdsShowComplete(
                                placementId: String?, state: UnityAds.UnityAdsShowCompletionState?
                            ) {
                                loadUnityInter()
                                onAdClosed.invoke()
                            }

                        })
                }
            } else {
                onAdClosed.invoke()
            }
        }
    }

    override suspend fun showReward(
        activity: Activity, onRewardClosed: (rewarded: Boolean) -> Unit
    ) {
        if (premiumUser) {
            onRewardClosed.invoke(true)
            return
        }
        adUnitsHelper?.verifyAdUnits(AdType.REWARD) {
            if (it) {
                if (adUnitsHelper?.providerIsAdmob() == true) {
                    var rewarded = false
                    if (mReward != null) {
                        mReward?.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent()
                                mReward?.fullScreenContentCallback = null
                                mReward = null
                                onRewardClosed.invoke(rewarded)
                                loadRewardInter()
                            }

                            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                super.onAdFailedToShowFullScreenContent(p0)
                                mReward?.fullScreenContentCallback = null
                                mReward = null
                                loadRewardInter()
                            }
                        }
                        mReward?.show(activity) {
                            rewarded = true
                        }
                    } else {
                        onRewardClosed.invoke(false)
                    }
                } else {
                    UnityAds.show(activity,
                        adUnitsHelper?.getAdUnit(AdType.REWARD),
                        UnityAdsShowOptions(),
                        object : IUnityAdsShowListener {
                            override fun onUnityAdsShowFailure(
                                placementId: String?,
                                error: UnityAds.UnityAdsShowError?,
                                message: String?
                            ) {
                                onRewardClosed.invoke(false)
                                loadUnityReward()
                            }

                            override fun onUnityAdsShowStart(placementId: String?) {}
                            override fun onUnityAdsShowClick(placementId: String?) {}
                            override fun onUnityAdsShowComplete(
                                placementId: String?, state: UnityAds.UnityAdsShowCompletionState?
                            ) {
                                loadUnityReward()
                                onRewardClosed.invoke(state?.equals(UnityAds.UnityAdsShowCompletionState.COMPLETED) == true)
                            }

                        })
                }
            } else {
                onRewardClosed.invoke(false)
            }
        }
    }

    override fun showAdInFrame(
        activity: Activity,
        frameLayout: FrameLayout,
        style: NativeAdKeys?
    ) {
        if (premiumUser) {
            frameLayout.visibility = GONE
            return
        }

        val styleSmall = style ?: NativeAdKeys.SMALL_ONE
        val styleBig = style ?: NativeAdKeys.BIG_ONE

        adUnitsHelper?.verifyAdUnits(AdType.NATIVE) {
            if (it) {
                if (adUnitsHelper?.providerIsAdmob() == true) {
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
                                        Log.e(
                                            "UAN",
                                            "Showing big native ad. Preloaded :${mNativeAd != null}"
                                        )
                                        if (mNativeAd == null) {
                                            mNativeAd = loadNativeAdRuntime().await()
                                        }
                                        Log.e(
                                            "UAN",
                                            "Showing big native ad. Load status : ${mNativeAd != null}"
                                        )
                                        frameAds.showNativeAd(
                                            frameLayout,
                                            styleBig,
                                            mNativeAd
                                        )
                                        mNativeAd = null
                                        loadNativeAd()
                                    }
                                }

                                heightDp >= 150 -> {
                                    adScope.launch {
                                        if (mNativeAd == null) {
                                            mNativeAd = loadNativeAdRuntime().await()
                                        }

                                        frameAds.showNativeAd(frameLayout, styleSmall, mNativeAd)
                                        mNativeAd = null
                                        loadNativeAd()
                                    }
                                }

                                height >= 50 -> {
                                    showBanner(activity, frameLayout)
                                }
                            }
                        }
                    })
                } else {
                    Log.e("UAN", "Admob is selected. disabling native ad")
                    showBanner(activity, frameLayout)
                }
            } else {
                Log.e("UAN", "Incorrect native ad units")
                frameLayout.visibility = GONE
            }
        }
    }

    override fun onAdReInit() {
        loadHelper.setIsAdmob(adUnitsHelper?.providerIsAdmob() == true)
        globalCallback.onAdReInit()
        if (adUnitsHelper?.providerIsAdmob() == true) {
            loadInter()
            loadNativeAd()
            loadRewardInter()
        } else {
            loadUnityInter()
            loadUnityReward()
        }
    }

    private fun loadInter() {
        adScope.launch(Dispatchers.Main) {
            loadHelper.loadInter(adUnitsHelper?.getAdUnit(AdType.INTERSTITIAL).toString()) {
                Log.d("Info", "First inter loaded")
                if (mInter == null) mInter = it
            }
        }
    }

    private fun loadUnityInter() {
        adScope.launch(Dispatchers.Main) {
            loadHelper.loadUnityInter(adUnitsHelper?.getAdUnit(AdType.INTERSTITIAL).toString()) {

            }
        }
    }


    private fun loadUnityReward() {
        adScope.launch(Dispatchers.Main) {
            loadHelper.loadUnityReward(adUnitsHelper?.getAdUnit(AdType.REWARD).toString()) {

            }
        }
    }

    private fun loadRewardInter() {
        adScope.launch(Dispatchers.Main) {
            loadHelper.loadReward(adUnitsHelper?.getAdUnit(AdType.REWARD).toString()) {
                if (mReward == null) mReward = it
            }
        }
    }

    private fun loadNativeAd() {
        loadHelper.loadNativeAd(adUnitsHelper?.getAdUnit(AdType.NATIVE).toString()) {
            if (mNativeAd == null) mNativeAd = it
        }
    }

    override suspend fun destroyNativeAd() {
        mNativeAd?.destroy()
        mNativeAd = null
    }

    private fun loadNativeAdRuntime(): CompletableDeferred<NativeAd?> {
        val runtimeLoad = CompletableDeferred<NativeAd?>()
        val native =
            AdLoader.Builder(app, adUnitsHelper?.getAdUnit(AdType.NATIVE).toString()).forNativeAd {
                runtimeLoad.complete(it)
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    runtimeLoad.complete(null)
                }
            }).build()

        native.loadAd(AdRequest.Builder().build())
        return runtimeLoad
    }

    fun showBanner(activity: Activity, bannerView: FrameLayout) {
        if (premiumUser) {
            bannerView.visibility = GONE
            return
        }
        adUnitsHelper?.verifyAdUnits(AdType.BANNER) {
            if (it) {
                if (adUnitsHelper?.providerIsAdmob() == true) {
                    adView = AdView(activity)
                    adView?.adUnitId = adUnitsHelper?.getAdUnit(AdType.BANNER).toString()
                    try {
                        bannerView.addView(adView)
                        loadBanner(activity)
                        bannerView.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        e.printStackTrace()
                        bannerView.visibility = GONE
                    }
                } else {
                    val bottomBanner = BannerView(
                        activity, adUnitsHelper?.getAdUnit(AdType.BANNER), UnityBannerSize(320, 50)
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
        val adRequest = AdRequest.Builder().build()
        val adSize: AdSize = SizeUtils.getAdSize(activity)
        adView?.setAdSize(adSize)
        adView?.loadAd(adRequest)
    }

}
