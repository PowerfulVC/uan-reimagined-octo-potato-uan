package uan.mod

import android.app.Activity
import android.app.Application
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Job
import uan.mod.callbacks.OnReInit
import uan.mod.configs.NativeAdKeys
import uan.mod.helper.AdUnitsHelper
import uan.mod.helper.FrameAds
import uan.mod.net.UnitsRequest

interface Ad {
    val frameAds: FrameAds
    val adUnitsHelper: AdUnitsHelper?
    var premiumUser: Boolean
    suspend fun showSplashInter(activity: Activity, onAdClosed: () -> Unit)
    suspend fun showInter(activity: Activity, onAdClosed: () -> Unit)
    suspend fun showReward(activity: Activity, onRewardClosed: (rewarded: Boolean) -> Unit)
    fun showAdInFrame(activity: AppCompatActivity, frameLayout: FrameLayout, style: NativeAdKeys?)
    suspend fun setupDefaultAdUnits(strJson: String)
    suspend fun destroyNativeAd()
    suspend fun init(
        projectId: String, premiumUser: Boolean = false
    ): Job

    fun setupOpenAds(application: Application)

    fun setOpenAdsRestriction(restrictionUnit: (activity: Activity?) -> Boolean)

    var unitsRequest: UnitsRequest?
}
