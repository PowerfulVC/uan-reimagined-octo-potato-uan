package uan.mod.use

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.appopen.AppOpenAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uan.mod.helper.AdUnitsHelper
import uan.mod.helper.Cached
import uan.mod.helper.FullScreenAdState
import uan.mod.helper.LoadHelper
import uan.mod.helper.UanLog
import uan.mod.models.AdType
import java.lang.ref.WeakReference

class AppOpenManager(
    private val app: Application,
    private val unit: AdUnitsHelper
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val loadHelper = LoadHelper(app)
    private var appOpenAd: Cached<AppOpenAd>? = null
    private var isLoading = false
    private var currentActivity: WeakReference<Activity>? = null
    var allowOpenAd = true

    /** Set by AdImpl: false for premium users or when consent does not allow ads. */
    internal var adsAllowed: () -> Boolean = { true }

    private val isAdAvailable: Boolean
        get() = appOpenAd?.isFresh(AD_EXPIRY_MILLIS) == true

    var openAdsRestricted: (activity: Activity?) -> Boolean = { false }

    fun fetchAd() {
        if (isLoading || isAdAvailable || !adsAllowed() || !unit.isValid(AdType.OPEN)) {
            return
        }
        isLoading = true
        scope.launch {
            val ad = loadHelper.loadAppOpen(unit.getAdUnit(AdType.OPEN)).adOrNull()
            if (ad != null) appOpenAd = Cached(ad)
            isLoading = false
        }
    }

    private fun showAdIfAvailable() {
        Log.d("UAN", "Allow open ad : $allowOpenAd")
        if (!allowOpenAd || !adsAllowed() || FullScreenAdState.isShowing) return
        if (FullScreenAdState.consumeAppOpenSuppression()) return
        val activity = currentActivity?.get() ?: return
        if (activity::class.java.simpleName == "SplashActivity" || openAdsRestricted(activity)) return
        if (!FullScreenAdState.canShowOn(activity)) return

        val ad = appOpenAd?.takeIf { isAdAvailable }?.ad
        appOpenAd = null
        if (ad == null) {
            fetchAd()
            return
        }
        val onFinished = { fetchAd() }
        ad.fullScreenContentCallback = FullScreenAdState.callback("App open ad", onFinished)
        UanLog.d("Show open ad")
        FullScreenAdState.showSafely("App open ad", onFinished) { ad.show(activity) }
    }

    /** Drops the cached ad, e.g. when the user becomes premium. */
    internal fun clear() {
        appOpenAd = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) = track(activity)

    override fun onActivityResumed(activity: Activity) = track(activity)

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity?.get() === activity) currentActivity = null
    }

    // Ad SDK activities (AdMob or mediated networks) must never become the host for an app open ad.
    private fun track(activity: Activity) {
        if (FullScreenAdState.isShowing || activity.javaClass.name.startsWith("com.google.android.gms.ads")) return
        currentActivity = WeakReference(activity)
    }

    /** App returned to the foreground. */
    override fun onStart(owner: LifecycleOwner) {
        showAdIfAvailable()
    }

    private companion object {
        // App open ads expire after four hours.
        const val AD_EXPIRY_MILLIS = 4 * 60 * 60 * 1000L
    }
}
