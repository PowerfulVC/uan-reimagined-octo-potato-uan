package uan.mod

import android.app.Activity
import android.app.Application
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.initialization.AdapterStatus
import kotlinx.coroutines.Job
import uan.mod.configs.AdUnit
import uan.mod.configs.NativeAdKeys
import uan.mod.configs.UanConfig
import uan.mod.helper.AdUnitsHelper
import uan.mod.helper.ConsentManager
import uan.mod.helper.FrameAds
import uan.mod.use.AppOpenManager

interface Ad {
    val frameAds: FrameAds
    val adUnitsHelper: AdUnitsHelper?
    var premiumUser: Boolean
    var appOpenManager: AppOpenManager?

    /** Test devices, mediation network extras, revenue callback, logging. Set before init. */
    var config: UanConfig

    /** GDPR / US-state privacy consent (Google UMP). */
    val consent: ConsentManager

    /** onAdClosed is always invoked exactly once, on the main thread. */
    suspend fun showSplashInter(activity: Activity, onAdClosed: () -> Unit)

    /** onAdClosed is always invoked exactly once, on the main thread. */
    suspend fun showInter(activity: Activity, onAdClosed: () -> Unit)

    /** onRewardClosed is always invoked exactly once, on the main thread. */
    suspend fun showReward(activity: Activity, onRewardClosed: (rewarded: Boolean) -> Unit)
    fun showAdInFrame(activity: AppCompatActivity, frameLayout: FrameLayout, style: NativeAdKeys?)
    fun showBanner(activity: Activity, bannerView: FrameLayout)
    suspend fun setupDefaultAdUnits(strJson: String)
    suspend fun destroyNativeAd()

    /**
     * Initializes ads without a consent flow. Prefer [initWithConsent] if you have users in the
     * EEA, UK, Switzerland or US states with privacy laws.
     */
    suspend fun init(
        adUnit: AdUnit, premiumUser: Boolean = false
    ): Job

    /**
     * Gathers consent through UMP (showing the form when required), then initializes MobileAds
     * and all mediation adapters only if ads may be requested. Call from your launch activity on
     * every app start.
     */
    suspend fun initWithConsent(
        activity: Activity, adUnit: AdUnit, premiumUser: Boolean = false
    ): Job

    /** Suspends until MobileAds and every mediation adapter finished initializing. */
    suspend fun awaitInitialization()

    /** Initialization state of each mediation adapter; empty until initialization completes. */
    fun adapterStatuses(): Map<String, AdapterStatus>

    /** Opens AdMob Ad Inspector to debug mediation in test devices. */
    fun openAdInspector(activity: Activity)

    fun setupOpenAds(application: Application)

    fun setOpenAdsRestriction(restrictionUnit: (activity: Activity?) -> Boolean)
}
