package uan.mod

import android.app.Activity
import android.app.Application
import android.widget.FrameLayout
import kotlinx.coroutines.Job
import uan.mod.callbacks.OnReInit
import uan.mod.configs.NativeAdKeys
import uan.mod.helper.AdUnitsHelper
import uan.mod.helper.FrameAds
import uan.mod.net.UnitsRequest

interface Ad {
    val frameAds: FrameAds
    val adUnitsHelper: AdUnitsHelper?
    suspend fun showSplashInter(activity: Activity, onAdClosed: () -> Unit)
    suspend fun showInter(activity: Activity, onAdClosed: () -> Unit)
    suspend fun showReward(activity: Activity, onRewardClosed: (rewarded: Boolean) -> Unit)
    fun showAdInFrame(activity: Activity, frameLayout: FrameLayout, style: NativeAdKeys?)
    suspend fun setupDefaultAdUnits(strJson: String)
    suspend fun destroyNativeAd()
    suspend fun init(
        projectId: String,
        action: () -> Unit,
        premiumUser: Boolean = false
    ): Job

    fun setupOpenAds(application: Application)

    fun setOpenAdsRestriction(restrictionUnit: (activity: Activity?) -> Boolean)

    var unitsRequest: UnitsRequest?
}