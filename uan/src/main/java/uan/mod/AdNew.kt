package uan.mod

import android.app.Activity
import android.widget.FrameLayout

interface AdNew {
    suspend fun showSplashInter(activity: Activity, onAdClosed: () -> Unit)
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
}