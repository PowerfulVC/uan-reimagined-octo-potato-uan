package uan.mod

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenManager(private val app: Application, private val unit: String) :
    Application.ActivityLifecycleCallbacks,
    LifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var loadCallback: AppOpenAd.AppOpenAdLoadCallback? = null
    private var currentActivity: Activity? = null
    private var isShowingAd = false

    private val adRequest: AdRequest
        get() = AdRequest.Builder().build()

    val isAdAvailable: Boolean
        get() = appOpenAd != null

    init {
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun fetchAd() {
        Log.e("Info", "OpenLoadStart")
        if (isAdAvailable) {
            return
        }
        loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(p0: AppOpenAd) {
                super.onAdLoaded(p0)
                appOpenAd = p0

                Log.e("Info", "OnOpenAdLoaded")
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                Log.e("Info", "OpenFailedtoLoad:" + p0.responseInfo)

            }
        }
        val request = adRequest
        AppOpenAd.load(
            app,
            unit, request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback!!
        )
    }

    private fun showAdIfAvailable() {
        try {
            if ((currentActivity!!::class.java.simpleName == "SplashActivity"))
                return
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.e("UAN", "SHOWOPEN:" + (!isShowingAd) + "|" + isAdAvailable)
        if (isAdAvailable) {
            Log.d("tag", "will show ad ")
            val fullScreenContentCallback: FullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAd = null
                        isShowingAd = false
                        fetchAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(p0: AdError?) {
                    }

                    override fun onAdShowedFullScreenContent() {
                        isShowingAd = true
                    }
                }
            appOpenAd?.fullScreenContentCallback = fullScreenContentCallback
            appOpenAd!!.show(currentActivity)
        } else {
            Log.d("tag", "can't show ad ")
            fetchAd()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        showAdIfAvailable()
        Log.d("UAN", "onStart::::")
    }

}