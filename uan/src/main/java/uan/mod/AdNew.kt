package uan.mod

import android.app.Activity
import android.widget.FrameLayout
import uan.mod.callbacks.OnReInit
import uan.mod.helper.AdUnitsHelper
import uan.mod.helper.FrameAds
import uan.mod.net.UnitsRequest

interface AdNew {
    val frameAds : FrameAds
    val adUnitsHelper : AdUnitsHelper?
    suspend fun showSplashInter(activity: Activity, onAdClosed: () -> Unit)
    suspend fun setupGlobalInitListener(callback : OnReInit)
    suspend fun showInter(activity: Activity, onAdClosed: () -> Unit)
    suspend fun showReward(activity: Activity, onRewardClosed: (rewarded: Boolean) -> Unit)
    suspend fun showAdInFrame(activity: Activity, frameLayout: FrameLayout)
    suspend fun setupDefaultAdUnits(strJson: String)
    suspend fun destroyNativeAd()
    suspend fun init(
        projectId: String,
        action: () -> Unit,
        premiumUser: Boolean = false
    )

    var unitsRequest: UnitsRequest?
}