package uan.mod

import android.app.Activity
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.ConnectivityManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.gson.Gson
import com.unity3d.ads.*
import com.unity3d.ads.UnityAds.UnityAdsInitializationError
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import uan.mod.configs.AdUnit
import uan.mod.configs.UaNativeAd
import javax.security.auth.callback.Callback
import kotlin.math.roundToInt


class Ad(activity: Application) : IUnityAdsInitializationListener {
    private val act = activity
    var adUnit: AdUnit? = null
    var nativeAdConfig: UaNativeAd = UaNativeAd()
    private var initInProgress = false
    private var lastUsedProjectId = ""
    private var mInterstitialAd: InterstitialAd? = null
    private var adView: AdView? = null
    private var rewardedAd: CompletableDeferred<RewardedInterstitialAd>? = CompletableDeferred()
    private var premiumUser = false
    private var loadsOnFail = 0

    fun initialize(
        projectId: String,
        action: (success: Boolean) -> Unit,
        premiumUser: Boolean = false
    ) {
        this.premiumUser = premiumUser
        lastUsedProjectId = projectId
        initInProgress = true
        val request = Request.Builder()
            .url(projectId)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback, okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                e.printStackTrace()
                initInProgress = false
                action.invoke(false)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                initInProgress = false
                if (response.isSuccessful) {
                    adUnit = Gson().fromJson(response.body?.string(), AdUnit::class.java)
                    initializedAd()
                    action.invoke(true)
                } else {
                    action.invoke(false)
                }
            }
        })
    }

    private fun initializedAd() {
        if (adUnit != null) {
            if (adUnit!!.admob) {
                try {
                    val applicationInfo: ApplicationInfo = act.packageManager.getApplicationInfo(
                        act.packageName,
                        PackageManager.GET_META_DATA
                    )
                    applicationInfo.metaData.putString(
                        "com.google.android.gms.ads.APPLICATION_ID",
                        adUnit!!.app
                    )
                    MobileAds.initialize(act) {
                        loadAd()
                        Log.e("UAN", "UAN Initialized Successfully")
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }
            } else {
                UnityAds.initialize(act, adUnit!!.app, false, this)
            }
        }
    }


    override fun onInitializationComplete() {
        loadAd()
    }

    override fun onInitializationFailed(error: UnityAdsInitializationError?, message: String?) {}

    private fun loadAd() {
        if (adUnit == null || premiumUser) return
        if (adUnit!!.admob) {
            loadAdmobInter()
            loadAdmobReward()
        } else
            loadUnityAdInter()
    }

    private fun loadUnityAdInter() {
        if (adUnit == null || premiumUser) {
            return
        }
        UnityAds.load(adUnit!!.interstitial, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                loadsOnFail = 0
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                if (loadsOnFail > 3)
                    return
                CoroutineScope(Dispatchers.IO).launch {
                    delay(3000)
                    loadsOnFail += 1
                    loadUnityAdInter()
                }
            }
        })
    }

    private fun loadAdmobInter() {
        if (adUnit == null || premiumUser)
            return
        GlobalScope.launch(Dispatchers.Main) {
            if (adUnit!!.app.isNotEmpty())
                if (mInterstitialAd == null)
                    InterstitialAd.load(act, adUnit!!.interstitial, AdRequest.Builder().build(),
                        object : InterstitialAdLoadCallback() {
                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                mInterstitialAd = null
                                if (loadsOnFail > 3)
                                    return
                                CoroutineScope(Dispatchers.IO).launch {
                                    delay(3000)
                                    if (mInterstitialAd == null) {
                                        loadsOnFail += 1
                                        loadAdmobInter()
                                    }
                                }
                            }

                            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                                mInterstitialAd = interstitialAd
                                loadsOnFail = 0
                            }
                        })
        }
    }

    suspend fun showAdmobReward(
        activity: Activity,
        action: (rewarded: Boolean) -> Unit
    ) {
        if (premiumUser) {
            action.invoke(true)
            return
        }
        var rewarded = false
        if (rewardedAd != null) {
            val rewAd = rewardedAd?.await()
            if (rewAd == null) {
                action.invoke(true)
                loadAdmobReward()
                return
            }
            rewAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    action.invoke(rewarded)
                    loadAdmobReward()
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    super.onAdFailedToShowFullScreenContent(p0)
                    action.invoke(rewarded)
                    loadAdmobReward()
                }
            }
            GlobalScope.launch(Dispatchers.Main) {
                rewAd.show(activity) {
                    rewarded = true
                }
            }
        } else {
            loadAdmobReward()
            action.invoke(false)
        }
    }

    fun loadAdmobReward() {
        if (adUnit == null || premiumUser) {
            return
        }
        if (adUnit?.admob == false)
            return
        rewardedAd = CompletableDeferred()
        GlobalScope.launch(Dispatchers.Main) {
            RewardedInterstitialAd.load(
                act,
                adUnit!!.rewarded,
                AdRequest.Builder().build(),
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(p0: RewardedInterstitialAd) {
                        super.onAdLoaded(p0)
                        rewardedAd?.complete(p0)
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        super.onAdFailedToLoad(p0)
                        rewardedAd = null
                        Log.e("UAN", "Admob reward ad failed to load:${p0.message}")
                    }
                })
        }
    }

    private fun userIsOnline(context: Context): Boolean {
        return try {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo?.isConnectedOrConnecting
                ?: false
        } catch (unused: Exception) {
            false
        }
    }

    fun loadAndShowAdmobReward(
        activity: Activity,
        onLoadStart: (loadStart: Boolean) -> Unit,
        onAdClosed: (rewarded: Boolean) -> Unit
    ) {
        if (adUnit == null || premiumUser) {
            onLoadStart.invoke(false)
            return
        }
        GlobalScope.launch(Dispatchers.Main) {
            if (adUnit!!.app.isNotEmpty()) {
                var rewardedInterstitialAd: RewardedInterstitialAd? = null
                var rewarded = false
                if (rewardedInterstitialAd == null) {
                    onLoadStart.invoke(true)
                    RewardedInterstitialAd.load(act, adUnit!!.rewarded,
                        AdRequest.Builder().build(), object : RewardedInterstitialAdLoadCallback() {
                            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                                rewardedInterstitialAd = ad
                                rewardedInterstitialAd?.fullScreenContentCallback =
                                    object : FullScreenContentCallback() {
                                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                            onAdClosed.invoke(false)
                                        }

                                        override fun onAdShowedFullScreenContent() {}

                                        override fun onAdDismissedFullScreenContent() {
                                            onAdClosed.invoke(rewarded)
                                        }
                                    }
                                rewardedInterstitialAd!!.show(
                                    activity
                                ) { rewarded = true }
                                Log.e("UAN", "Ad was loaded.")
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                Log.e("UAN", adError.message)
                                rewardedInterstitialAd = null
                                onAdClosed.invoke(false)
                            }
                        })
                } else {
                    onLoadStart.invoke(false)
                }
            } else {
                onLoadStart.invoke(false)
            }
        }
    }

    fun showInter(activity: Activity, onAdClosed: () -> Unit) {
        if (adUnit == null && !initInProgress) {
            initialize(lastUsedProjectId, {
                if (it) {
                    showRealInter(onAdClosed, activity)
                } else {
                    onAdClosed.invoke()
                }
            }, premiumUser)
        } else {
            showRealInter(onAdClosed, activity)
        }
    }

    private fun showRealInter(onAdClosed: () -> Unit, activity: Activity) {
        if (premiumUser) {
            onAdClosed.invoke()
            return
        }
        if (adUnit?.admob == true) {
            showAdmobInter(onAdClosed, activity)
        } else {
            showUnityAdInter(onAdClosed, activity)
        }
    }

    private fun showAdmobInter(onAdClosed: () -> Unit, activity: Activity) {
        if (premiumUser) {
            onAdClosed.invoke()
            return
        }
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        onAdClosed.invoke()
                    }

                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        super.onAdFailedToShowFullScreenContent(p0)
                        onAdClosed.invoke()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(ContentValues.TAG, "Ad showed fullscreen content.")
                        mInterstitialAd = null
                        loadAdmobInter()
                    }
                }
            mInterstitialAd?.show(activity)
        } else {
            onAdClosed.invoke()
        }
    }

    private fun showUnityAdInter(onAdClosed: () -> Unit, activity: Activity) {
        if (premiumUser) {
            onAdClosed.invoke()
            return
        }
        if (adUnit == null) {
            onAdClosed.invoke()
            return
        }
        UnityAds.show(activity, adUnit!!.interstitial, UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(
                    placementId: String?,
                    error: UnityAds.UnityAdsShowError?,
                    message: String?
                ) {
                    onAdClosed.invoke()
                }

                override fun onUnityAdsShowStart(placementId: String?) {}
                override fun onUnityAdsShowClick(placementId: String?) {}
                override fun onUnityAdsShowComplete(
                    placementId: String?,
                    state: UnityAds.UnityAdsShowCompletionState?
                ) {
                    loadUnityAdInter()
                    onAdClosed.invoke()
                }

            }
        )
    }

    fun showBanner(activity: Activity, bannerView: FrameLayout) {
        if (premiumUser) {
            return
        }
        if (adUnit == null) return
        if (adUnit!!.app.isNotEmpty()) {
            if (adUnit!!.admob) {
                adView = AdView(activity)
                adView?.adUnitId = adUnit!!.banner
                try {
                    bannerView.addView(adView)
                    loadBanner(activity)
                    bannerView.visibility = View.VISIBLE
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                val bottomBanner =
                    BannerView(activity, adUnit!!.banner, UnityBannerSize(320, 50))
                bottomBanner.listener = null
                bottomBanner.load()
                bannerView.addView(bottomBanner)
            }
        }
    }

    private fun loadBanner(activity: Activity) {
        if (premiumUser) {
            return
        }
        val adRequest = AdRequest.Builder()
            .build()
        val adSize: AdSize = getAdSize(activity)
        adView?.setAdSize(adSize)
        adView?.loadAd(adRequest)
    }

    private fun getAdSize(activity: Activity): AdSize {
        val display: Display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }


    fun showAdInFrame(
        activity: Activity,
        frameLayout: FrameLayout
    ) {
        if (premiumUser) {
            return
        }
        val viewTreeObserver = frameLayout.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                frameLayout.viewTreeObserver.removeGlobalOnLayoutListener(this);
                val height = frameLayout.height
                val heightDp = frameLayout.context.pxToDp(height)
                when {
                    heightDp >= 270 -> {
                        showNative(frameLayout)
                    }
                    heightDp >= 150 -> {
                        showNativeSmall(
                            frameLayout
                        )
                    }
                    height >= 50 -> {
                        showBanner(activity, frameLayout)
                    }
                }
            }
        })
    }

    fun Context.pxToDp(px: Int): Int {
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        return (px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
    }

    fun showNative(
        frameLayout: FrameLayout
    ) {
        if (adUnit == null || premiumUser) return
        if (nativeAdConfig.adBodyHex == null)
            return
        if (adUnit!!.app.isNotEmpty()) {
            try {
                val adLoader = AdLoader.Builder(frameLayout.context, adUnit!!.native)
                    .forNativeAd { nativeAd: NativeAd ->
                        try {
                            val unifiedNativeAdView = LayoutInflater.from(frameLayout.context)
                                .inflate(
                                    R.layout.ad_layoujt,
                                    null
                                ) as CardView
                            unifiedNativeAdView.setCardBackgroundColor(
                                Color.parseColor(
                                    nativeAdConfig.adBodyHex
                                )
                            )
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_call_to_action).typeface =
                                nativeAdConfig.font
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_body).typeface =
                                nativeAdConfig.font
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_headline).typeface =
                                nativeAdConfig.font
                            //set text color
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_body)
                                .setTextColor(Color.parseColor(nativeAdConfig.textColorHex))
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_headline)
                                .setTextColor(Color.parseColor(nativeAdConfig.textColorHex))
                            //set btn color
                            unifiedNativeAdView.findViewById<Button>(R.id.ad_call_to_action).backgroundTintList =
                                ColorStateList.valueOf(Color.parseColor(nativeAdConfig.btnHex))
                            unifiedNativeAdView.findViewById<Button>(R.id.ad_call_to_action)
                                .setTextColor(Color.parseColor(nativeAdConfig.btnTextHex))
                            mapUnifiedNativeAdToLayout(nativeAd, unifiedNativeAdView)
                            frameLayout.removeAllViews()
                            frameLayout.addView(unifiedNativeAdView)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            super.onAdFailedToLoad(loadAdError)
                            Log.d("AdInfo", "Native failed to load $loadAdError")
                        }

                        override fun onAdClosed() {}
                        override fun onAdOpened() {}
                        override fun onAdClicked() {}
                    })
                    .build()
                adLoader.loadAd(AdRequest.Builder().build())
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }


    fun showNativeSmall(
        frameLayout: FrameLayout
    ) {
        if (adUnit == null || premiumUser) return
        if (adUnit!!.app.isNotEmpty()) {
            try {
                val adLoader = AdLoader.Builder(frameLayout.context, adUnit!!.native)
                    .forNativeAd { nativeAd: NativeAd ->
                        try {
                            val unifiedNativeAdView = LayoutInflater.from(frameLayout.context)
                                .inflate(
                                    R.layout.ad_layout_small,
                                    null
                                ) as CardView
                            unifiedNativeAdView.setCardBackgroundColor(
                                Color.parseColor(
                                    nativeAdConfig.adBodyHex
                                )
                            )
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_call_to_action).typeface =
                                nativeAdConfig.font
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_body).typeface =
                                nativeAdConfig.font
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_headline).typeface =
                                nativeAdConfig.font
                            //set text color
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_body)
                                .setTextColor(Color.parseColor(nativeAdConfig.textColorHex))
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_headline)
                                .setTextColor(Color.parseColor(nativeAdConfig.textColorHex))
                            //set btn color
                            unifiedNativeAdView.findViewById<Button>(R.id.ad_call_to_action).backgroundTintList =
                                ColorStateList.valueOf(Color.parseColor(nativeAdConfig.btnHex))
                            unifiedNativeAdView.findViewById<Button>(R.id.ad_call_to_action)
                                .setTextColor(Color.parseColor(nativeAdConfig.btnTextHex))
                            mapUnifiedNativeAdToLayout(nativeAd, unifiedNativeAdView)
                            frameLayout.removeAllViews()
                            frameLayout.addView(unifiedNativeAdView)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            super.onAdFailedToLoad(loadAdError)
                            Log.d("AdInfo", "Native failed to load $loadAdError")
                        }

                        override fun onAdClosed() {}
                        override fun onAdOpened() {}
                        override fun onAdClicked() {}
                    })
                    .build()
                adLoader.loadAd(AdRequest.Builder().build())
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {

        fun showNative(
            frameLayout: FrameLayout, adUnit: String, nativeAdConfig: UaNativeAd
        ) {
            if (nativeAdConfig.adBodyHex == null)
                return
            try {
                val adLoader = AdLoader.Builder(frameLayout.context, adUnit)
                    .forNativeAd { nativeAd: NativeAd ->
                        try {
                            val unifiedNativeAdView = LayoutInflater.from(frameLayout.context)
                                .inflate(
                                    R.layout.ad_layoujt,
                                    null
                                ) as CardView
                            unifiedNativeAdView.setCardBackgroundColor(
                                Color.parseColor(
                                    nativeAdConfig.adBodyHex
                                )
                            )
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_call_to_action).typeface =
                                nativeAdConfig.font
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_body).typeface =
                                nativeAdConfig.font
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_headline).typeface =
                                nativeAdConfig.font
                            //set text color
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_body)
                                .setTextColor(Color.parseColor(nativeAdConfig.textColorHex))
                            unifiedNativeAdView.findViewById<TextView>(R.id.ad_headline)
                                .setTextColor(Color.parseColor(nativeAdConfig.textColorHex))
                            //set btn color
                            unifiedNativeAdView.findViewById<Button>(R.id.ad_call_to_action).backgroundTintList =
                                ColorStateList.valueOf(Color.parseColor(nativeAdConfig.btnHex))
                            unifiedNativeAdView.findViewById<Button>(R.id.ad_call_to_action)
                                .setTextColor(Color.parseColor(nativeAdConfig.btnTextHex))
                            mapUnifiedNativeAdToLayout(nativeAd, unifiedNativeAdView)
                            frameLayout.removeAllViews()
                            frameLayout.addView(unifiedNativeAdView)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            super.onAdFailedToLoad(loadAdError)
                            Log.d("AdInfo", "Native failed to load $loadAdError")
                        }

                        override fun onAdClosed() {}
                        override fun onAdOpened() {}
                        override fun onAdClicked() {}
                    })
                    .build()
                adLoader.loadAd(AdRequest.Builder().build())
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        private fun mapUnifiedNativeAdToLayout(adFromGoogle: NativeAd, card: CardView) {
            val myAdView: NativeAdView =
                card.findViewById(R.id.unifiedNativeAdView)
            val mediaView: MediaView =
                myAdView.findViewById(R.id.ad_media)
            myAdView.mediaView = mediaView
            myAdView.headlineView = myAdView.findViewById(R.id.ad_headline)
            myAdView.bodyView = myAdView.findViewById(R.id.ad_body)
            myAdView.callToActionView = myAdView.findViewById(R.id.ad_call_to_action)
            myAdView.iconView = myAdView.findViewById(R.id.ad_icon)
            (myAdView.headlineView as TextView).text = adFromGoogle.headline
            if (adFromGoogle.body == null) {
                myAdView.bodyView?.visibility = View.GONE
            } else {
                (myAdView.bodyView as TextView).text = adFromGoogle.body
            }
            if (adFromGoogle.callToAction == null) {
                myAdView.callToActionView?.visibility = View.GONE
            } else {
                (myAdView.callToActionView as Button).text = adFromGoogle.callToAction
            }
            if (adFromGoogle.icon == null) {
                myAdView.iconView?.visibility = View.GONE
            } else {
                (myAdView.iconView as ImageView).setImageDrawable(adFromGoogle.icon?.drawable)
            }
            myAdView.setNativeAd(adFromGoogle)
        }
    }

}
