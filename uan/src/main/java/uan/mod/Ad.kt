package uan.mod

import android.app.Activity
import android.app.Application
import android.content.ContentValues
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.View
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
import com.google.gson.Gson
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import javax.security.auth.callback.Callback

class Ad(activity: Application) {
    private val act = activity
    private var adUnit: AdUnit? = null
    private var initInProgress = false
    private var lastUsedProjectId = ""
    private var mInterstitialAd: InterstitialAd? = null
    private var adView: AdView? = null

    var nativeAdResourceId =

    fun initialize(projectId: String, action: (success: Boolean) -> Unit) {
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
                UnityAds.initialize(act, adUnit!!.app, false, true)
            }
        }
    }

    private fun loadAd() {
        if (adUnit == null) return
        if (adUnit!!.admob)
            loadAdmobInter()
        else
            loadUnityAdInter()
    }

    private fun loadUnityAdInter() {
        if (adUnit == null) {
            return
        }
        UnityAds.load(adUnit!!.interstitial, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {}

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(3000)
                    loadUnityAdInter()
                }
            }
        })
    }

    private fun loadAdmobInter() {
        if (adUnit == null)
            return
        GlobalScope.launch(Dispatchers.Main) {
            if (adUnit!!.app.isNotEmpty())
                if (mInterstitialAd == null)
                    InterstitialAd.load(act, adUnit!!.interstitial, AdRequest.Builder().build(),
                        object : InterstitialAdLoadCallback() {
                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                mInterstitialAd = null
                                CoroutineScope(Dispatchers.IO).launch {
                                    delay(3000)
                                    if (mInterstitialAd == null)
                                        loadAdmobInter()
                                }
                            }

                            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                                mInterstitialAd = interstitialAd
                            }
                        })
        }
    }

    fun showInter(activity: Activity, onAdClosed: () -> Unit) {
        if (adUnit == null && !initInProgress) {
            initialize(lastUsedProjectId) {
                if (it) {
                    showRealInter(onAdClosed, activity)
                } else {
                    onAdClosed.invoke()
                }
            }
        } else {
            showRealInter(onAdClosed, activity)
        }
    }

    private fun showRealInter(onAdClosed: () -> Unit, activity: Activity) {
        if (adUnit?.admob == true) {
            showAdmobInter(onAdClosed, activity)
        } else {
            showUnityAdInter(onAdClosed, activity)
        }
    }

    private fun showAdmobInter(onAdClosed: () -> Unit, activity: Activity) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        onAdClosed.invoke()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                        Log.d(ContentValues.TAG, "Ad failed to show.")
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
        if (adUnit == null) return
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
        val adRequest = AdRequest.Builder()
            .build()
        val adSize: AdSize = getAdSize(activity)
        adView?.adSize = adSize
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

    fun showNative(frameLayout: FrameLayout, unifiedNativeAdView: CardView) {
        if (adUnit == null) return
        if (adUnit!!.app.isNotEmpty()) {
            try {
                val adLoader = AdLoader.Builder(frameLayout.context, adUnit!!.native)
                    .forNativeAd { nativeAd: NativeAd ->
                        try {
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