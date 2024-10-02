package uads.android.use

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import uads.android.help.AdUnitsHelper
import uads.android.models.AdType

internal class AppOpenManager(
    private val app: Application, private
    val unit: AdUnitsHelper,
    val premiumUser: Boolean
) :
    Application.ActivityLifecycleCallbacks,
    LifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var loadCallback: AppOpenAd.AppOpenAdLoadCallback? = null
    private var currentActivity: Activity? = null
    private var isShowingAd = false
    var allowOpenAd = true

    private val adRequest: AdRequest
        get() = AdRequest.Builder().build()

    private val isAdAvailable: Boolean
        get() = appOpenAd != null

    var condition: (activity: Activity?) -> Boolean = {
        Log.e("Info", "ITS DEFAULT CONDITION")
        false
    }

    private val lifecycleEventObserver = LifecycleEventObserver { source, event ->
        if (premiumUser) {
            return@LifecycleEventObserver
        }
        if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
            Log.e("APPOPENADS", "ON_RESUME: showOpen Ad")
            currentActivity?.let { showAdIfAvailable() }
        } else if (event == Lifecycle.Event.ON_PAUSE) {
            Log.e("APP", "paused")
        }
    }

    init {
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
    }

    fun fetchAd() {
        if (isAdAvailable) {
            return
        }
        if (premiumUser) {
            return
        }
        loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(p0: AppOpenAd) {
                super.onAdLoaded(p0)
                appOpenAd = p0
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
            }
        }
        val request = adRequest
        AppOpenAd.load(
            app,
            unit.getBlock(AdType.OPEN), request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback!!
        )
    }

    private fun showAdIfAvailable() {
        try {
            Log.d("UAN", "Allow open ad : ${allowOpenAd}")
            if (!this.allowOpenAd) {
                return
            }
            if (this.premiumUser) {
                return
            }
            if ((currentActivity!!::class.java.simpleName == "SplashActivity") || (currentActivity!!::class.java.simpleName == AdActivity::class.java.simpleName))
                return
            Log.e("Info", "IsAdAvailable:${isAdAvailable}")
            if (isAdAvailable) {
                val fullScreenContentCallback: FullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            appOpenAd = null
                            isShowingAd = false
                            fetchAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                            super.onAdFailedToShowFullScreenContent(p0)
                        }

                        override fun onAdShowedFullScreenContent() {
                            isShowingAd = true
                        }
                    }
                appOpenAd?.fullScreenContentCallback = fullScreenContentCallback
                Log.e("Info", "Current activity:${currentActivity != null}")
                currentActivity?.let {
                    if (!isShowingAd) {
                        Log.d("uads-OA", "Show open ads")
                        appOpenAd?.show(it)
                    }
                }
            } else {
                fetchAd()
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

//    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
//    fun onResume() {
//        if (premiumUser) {
//            return
//        }
//        showAdIfAvailable()
//    }
//
//    @OnLifecycleEvent(Lifecycle.Event.ON_START)
//    fun onStart() {
//        if (premiumUser) {
//            return
//        }
//        showAdIfAvailable()
//    }

}
