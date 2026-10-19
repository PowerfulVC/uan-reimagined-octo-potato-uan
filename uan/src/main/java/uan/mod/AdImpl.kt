package uan.mod

import android.app.Activity
import android.app.Application
import android.view.View
import android.view.View.GONE
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.AdapterStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import uan.mod.callbacks.OnReInit
import uan.mod.configs.AdUnit
import uan.mod.configs.NativeAdKeys
import uan.mod.configs.UanConfig
import uan.mod.helper.AdInitializer
import uan.mod.helper.AdUnitsHelper
import uan.mod.helper.Cached
import uan.mod.helper.ConsentManager
import uan.mod.helper.FrameAds
import uan.mod.helper.FullScreenAdState
import uan.mod.helper.LoadHelper
import uan.mod.helper.SizeUtils
import uan.mod.helper.UanLog
import uan.mod.helper.UanRuntime
import uan.mod.models.AdType
import uan.mod.use.AppOpenManager


class AdImpl(private val app: Application) : Ad, OnReInit {
    @Volatile
    private var premium = false
    override var premiumUser: Boolean
        get() = premium
        set(value) {
            premium = value
            if (value) adScope.launch { releaseAds() }
        }
    override var adUnitsHelper: AdUnitsHelper? = null
    override var config: UanConfig
        get() = UanRuntime.config
        set(value) {
            UanRuntime.config = value
        }
    override val consent = ConsentManager(app)
    private val loadHelper = LoadHelper(app)
    override val frameAds = FrameAds()
    private val adScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Cached ads and in-flight loads are only touched on the main thread.
    private var mInter: Cached<InterstitialAd>? = null
    private var mReward: Cached<RewardedInterstitialAd>? = null
    private var mNativeAd: Cached<NativeAd>? = null
    private var interJob: Job? = null
    private var rewardJob: Job? = null
    private var nativeJob: Job? = null

    private var globalCallback: OnReInit = object : OnReInit {
        override fun onAdReInit() {
            UanLog.d("UAN On Ad Re Initialized")
            setupOpenAds(app)
        }
    }

    override var appOpenManager: AppOpenManager? = null


    init {
        adUnitsHelper = AdUnitsHelper(app, this)
    }

    override suspend fun setupDefaultAdUnits(strJson: String) {
        if (strJson.isNotEmpty()) {
            try {
                val units = Gson().fromJson(strJson, AdUnit::class.java)
                adUnitsHelper?.setDefaultAdUnits(units)
            } catch (e: Exception) {
                UanLog.e("Failed to set default ad units  ${e.message}", e)
            }
        } else {
            UanLog.e("Failed to set default ad units. JSON Is empty")
        }
    }


    override suspend fun init(
        adUnit: AdUnit, premiumUser: Boolean
    ): Job {
        this@AdImpl.premiumUser = premiumUser
        return mainScope.launch {
            initInternal(adUnit).await()
        }
    }

    override suspend fun initWithConsent(
        activity: Activity, adUnit: AdUnit, premiumUser: Boolean
    ): Job {
        this@AdImpl.premiumUser = premiumUser
        adUnitsHelper?.setSynchronizedAdUnits(adUnit)
        return adScope.launch {
            // Consent from a previous session lets ads start loading while UMP refreshes.
            if (consent.canRequestAds) initInternal(adUnit).await()
            val canRequestAds = CompletableDeferred<Boolean>()
            consent.gather(activity) { canRequestAds.complete(it) }
            if (canRequestAds.await()) {
                if (!AdInitializer.isStarted) initInternal(adUnit).await()
            } else {
                UanLog.w("Consent does not allow ads; ads stay disabled.")
                releaseAds()
            }
        }
    }

    private fun initInternal(adUnit: AdUnit): CompletableDeferred<Boolean> {
        val completableDeferred = CompletableDeferred<Boolean>()
        adUnitsHelper?.setSynchronizedAdUnits(adUnit)
        adUnitsHelper?.initAd(adUnit) {
            completableDeferred.complete(true)
        }
        return completableDeferred
    }

    override suspend fun awaitInitialization() {
        AdInitializer.awaitInitialized()
    }

    override fun adapterStatuses(): Map<String, AdapterStatus> =
        AdInitializer.status?.adapterStatusMap.orEmpty()

    override fun openAdInspector(activity: Activity) {
        MobileAds.openAdInspector(activity) { error ->
            if (error != null) UanLog.w("Ad Inspector error: ${error.message}")
        }
    }

    override fun setupOpenAds(application: Application) {
        adScope.launch {
            val units = adUnitsHelper ?: return@launch
            if (premiumUser || appOpenManager != null) return@launch
            val manager = AppOpenManager(application, units)
            manager.adsAllowed = { canLoad(AdType.OPEN) }
            appOpenManager = manager
            application.registerActivityLifecycleCallbacks(manager)
            ProcessLifecycleOwner.get().lifecycle.addObserver(manager)
            manager.fetchAd()
            UanLog.d("Open ads initialization")
        }
    }

    override fun setOpenAdsRestriction(restrictionUnit: (activity: Activity?) -> Boolean) {
        appOpenManager?.openAdsRestricted = restrictionUnit
        UanLog.d("Set restrictions to open ads")
    }

    override suspend fun showSplashInter(activity: Activity, onAdClosed: () -> Unit) {
        adScope.launch {
            if (premiumUser) {
                delay(2000)
                onAdClosed.invoke()
                return@launch
            }
            if (!canLoad(AdType.INTERSTITIAL)) {
                UanLog.e("AD UNIT INTER INCORRECT or ads not allowed")
                delay(2000)
                onAdClosed.invoke()
                return@launch
            }
            val inter = takeFresh(mInter).also { mInter = null }
                ?: withTimeoutOrNull(config.splashTimeoutMillis) {
                    // With mediation, adapters should be ready before the first request.
                    if (AdInitializer.isStarted) AdInitializer.awaitInitialized()
                    loadHelper.loadInter(unit(AdType.INTERSTITIAL)).adOrNull()
                }
            if (inter == null || !FullScreenAdState.canShowOn(activity) || !activity.isInForeground()) {
                UanLog.d("Splash inter not shown")
                onAdClosed.invoke()
                loadInter()
                return@launch
            }
            showFullScreen("Splash interstitial", onFinished = {
                onAdClosed.invoke()
                loadInter()
            }) { callback ->
                inter.fullScreenContentCallback = callback
                inter.show(activity)
            }
        }
    }

    override suspend fun showInter(activity: Activity, onAdClosed: () -> Unit) = withContext(Dispatchers.Main) {
        if (premiumUser) {
            onAdClosed.invoke()
            return@withContext
        }
        val inter = takeFresh(mInter)
        if (inter == null || !FullScreenAdState.canShowOn(activity)) {
            onAdClosed.invoke()
            loadInter()
            return@withContext
        }
        mInter = null
        showFullScreen("Interstitial", onFinished = {
            onAdClosed.invoke()
            loadInter()
        }) { callback ->
            inter.fullScreenContentCallback = callback
            inter.show(activity)
        }
    }

    override suspend fun showReward(
        activity: Activity, onRewardClosed: (rewarded: Boolean) -> Unit
    ) = withContext(Dispatchers.Main) {
        if (premiumUser) {
            onRewardClosed.invoke(true)
            return@withContext
        }
        val reward = takeFresh(mReward)
        if (reward == null || !FullScreenAdState.canShowOn(activity)) {
            onRewardClosed.invoke(false)
            loadRewardInter()
            return@withContext
        }
        mReward = null
        var rewarded = false
        showFullScreen("Rewarded", onFinished = {
            onRewardClosed.invoke(rewarded)
            loadRewardInter()
        }) { callback ->
            reward.fullScreenContentCallback = callback
            reward.show(activity) { rewarded = true }
        }
    }

    private fun showFullScreen(
        name: String,
        onFinished: () -> Unit,
        show: (com.google.android.gms.ads.FullScreenContentCallback) -> Unit
    ) {
        val callback = FullScreenAdState.callback(name, onFinished)
        FullScreenAdState.showSafely(name, onFinished) { show(callback) }
    }

    override fun showAdInFrame(
        activity: AppCompatActivity,
        frameLayout: FrameLayout,
        style: NativeAdKeys?
    ) {
        if (premiumUser) {
            frameLayout.visibility = GONE
            return
        }

        val styleSmall = style ?: NativeAdKeys.SMALL_ONE
        val styleBig = style ?: NativeAdKeys.BIG_ONE

        if (!canLoad(AdType.NATIVE)) {
            UanLog.e("Incorrect native ad units or ads not allowed")
            frameLayout.visibility = GONE
            return
        }
        if (adUnitsHelper?.providerIsAdmob() != true) {
            UanLog.d("Native ads disabled for this provider, showing banner")
            showBanner(activity, frameLayout)
            return
        }
        bindToLifecycle(activity, frameLayout)
        activity.lifecycleScope.launch {
            val heightDp = SizeUtils.pxToDp(app, frameLayout.getHeightDeferred().await())
            UanLog.d("Native frame measured: ${heightDp}dp")
            when {
                heightDp >= 150 -> {
                    val key = if (heightDp >= 270) styleBig else styleSmall
                    val nativeAd = takeFresh(mNativeAd).also { mNativeAd = null }
                        ?: loadHelper.loadNativeAd(unit(AdType.NATIVE)).adOrNull()
                    frameAds.showNativeAd(frameLayout, key, nativeAd)
                    loadNativeAd()
                }

                heightDp >= 50 -> showBanner(activity, frameLayout, maxHeightDp = heightDp)

                else -> UanLog.w("Ad frame is too small (${heightDp}dp) to show an ad")
            }
        }
    }


    fun FrameLayout.getHeightDeferred(): CompletableDeferred<Int> {
        val completableDeferred = CompletableDeferred<Int>()
        this.doOnLayout {
            completableDeferred.complete(it.measuredHeight)
        }
        return completableDeferred
    }

    override fun onAdReInit() {
        globalCallback.onAdReInit()
        loadInter()
        loadNativeAd()
        loadRewardInter()
    }

    private fun unit(adType: AdType): String = adUnitsHelper?.getAdUnit(adType).orEmpty()

    private fun canLoad(adType: AdType): Boolean =
        !premiumUser && adUnitsHelper?.isValid(adType) == true && consent.adsAllowed

    private fun <T> takeFresh(cached: Cached<T>?): T? = cached?.takeIf { it.isFresh(AD_EXPIRY_MILLIS) }?.ad

    private fun Activity.isInForeground(): Boolean =
        (this as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: true

    private fun loadInter() {
        adScope.launch {
            if (!canLoad(AdType.INTERSTITIAL) || interJob?.isActive == true || takeFresh(mInter) != null) return@launch
            interJob = launch {
                loadHelper.withRetry { loadHelper.loadInter(unit(AdType.INTERSTITIAL)) }
                    ?.let { mInter = Cached(it) }
            }
        }
    }

    private fun loadRewardInter() {
        adScope.launch {
            if (!canLoad(AdType.REWARD) || rewardJob?.isActive == true || takeFresh(mReward) != null) return@launch
            rewardJob = launch {
                loadHelper.withRetry { loadHelper.loadReward(unit(AdType.REWARD)) }
                    ?.let { mReward = Cached(it) }
            }
        }
    }

    private fun loadNativeAd() {
        adScope.launch {
            if (!canLoad(AdType.NATIVE) || adUnitsHelper?.providerIsAdmob() != true) return@launch
            if (nativeJob?.isActive == true || takeFresh(mNativeAd) != null) return@launch
            mNativeAd?.ad?.destroy()
            mNativeAd = null
            nativeJob = launch {
                loadHelper.withRetry { loadHelper.loadNativeAd(unit(AdType.NATIVE)) }?.let { ad ->
                    if (premiumUser) ad.destroy() else mNativeAd = Cached(ad)
                }
            }
        }
    }

    override suspend fun destroyNativeAd() = withContext(Dispatchers.Main) {
        mNativeAd?.ad?.destroy()
        mNativeAd = null
    }

    private fun releaseAds() {
        interJob?.cancel()
        rewardJob?.cancel()
        nativeJob?.cancel()
        mInter = null
        mReward = null
        mNativeAd?.ad?.destroy()
        mNativeAd = null
        appOpenManager?.clear()
    }

    override fun showBanner(activity: Activity, bannerView: FrameLayout) =
        showBanner(activity, bannerView, maxHeightDp = null)

    private fun showBanner(activity: Activity, bannerView: FrameLayout, maxHeightDp: Int?) {
        if (!canLoad(AdType.BANNER)) {
            bannerView.visibility = GONE
            return
        }
        bindToLifecycle(activity, bannerView)
        releaseBanner(bannerView)
        val unitId = unit(AdType.BANNER)
        val adView = AdView(activity).apply {
            adUnitId = unitId
            onPaidEventListener = UanRuntime.paidListener(AdType.BANNER, unitId) { responseInfo }
            adListener = object : AdListener() {
                override fun onAdLoaded() = UanRuntime.logLoaded(AdType.BANNER, responseInfo)
                override fun onAdFailedToLoad(error: LoadAdError) = UanRuntime.logFailed(AdType.BANNER, error)
            }
        }
        try {
            bannerView.addView(adView)
            bannerView.setTag(R.id.uan_banner_tag, adView)
            bannerView.visibility = View.VISIBLE
            // Size the banner from the container so it fits edge-to-edge, split-screen and foldables.
            bannerView.doOnLayout {
                if (bannerView.getTag(R.id.uan_banner_tag) !== adView) return@doOnLayout
                adView.setAdSize(SizeUtils.getAdSize(activity, it.width, maxHeightDp))
                adView.loadAd(UanRuntime.newRequest())
            }
        } catch (e: Exception) {
            UanLog.e("Failed to show banner", e)
            releaseBanner(bannerView)
            bannerView.visibility = GONE
        }
    }

    private fun releaseBanner(container: FrameLayout) {
        val adView = container.getTag(R.id.uan_banner_tag) as? AdView ?: return
        container.setTag(R.id.uan_banner_tag, null)
        container.removeView(adView)
        adView.destroy()
    }

    /** Pauses, resumes and destroys the frame's banner / native ad with the activity. */
    private fun bindToLifecycle(activity: Activity, container: FrameLayout) {
        val owner = activity as? LifecycleOwner ?: return
        if (container.getTag(R.id.uan_lifecycle_tag) === owner) return
        container.setTag(R.id.uan_lifecycle_tag, owner)
        owner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                val banner = container.getTag(R.id.uan_banner_tag) as? AdView
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> banner?.pause()
                    Lifecycle.Event.ON_RESUME -> banner?.resume()
                    Lifecycle.Event.ON_DESTROY -> {
                        releaseBanner(container)
                        frameAds.releaseNativeAd(container)
                        container.setTag(R.id.uan_lifecycle_tag, null)
                        source.lifecycle.removeObserver(this)
                    }

                    else -> Unit
                }
            }
        })
    }

    private companion object {
        // Interstitial, rewarded and native ads should be shown within an hour of loading.
        const val AD_EXPIRY_MILLIS = 60 * 60 * 1000L
    }
}
