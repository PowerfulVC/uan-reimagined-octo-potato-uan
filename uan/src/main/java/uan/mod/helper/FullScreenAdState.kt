package uan.mod.helper

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback

/** Coordinates full-screen formats so app open ads never stack on interstitials or rewarded. */
internal object FullScreenAdState {
    @Volatile
    var isShowing = false
        private set

    // Returning from an ad click must not trigger an app open ad.
    @Volatile
    private var suppressNextAppOpen = false

    fun consumeAppOpenSuppression(): Boolean {
        val suppressed = suppressNextAppOpen
        suppressNextAppOpen = false
        return suppressed
    }

    fun canShowOn(activity: Activity): Boolean =
        !isShowing && !activity.isFinishing && !activity.isDestroyed

    /** Callback that reports [onFinished] exactly once, whether the ad is dismissed or fails to show. */
    fun callback(name: String, onFinished: () -> Unit) = object : FullScreenContentCallback() {
        private var finished = false

        private fun finish() {
            if (finished) return
            finished = true
            isShowing = false
            onFinished()
        }

        override fun onAdShowedFullScreenContent() {
            isShowing = true
        }

        override fun onAdClicked() {
            suppressNextAppOpen = true
        }

        override fun onAdDismissedFullScreenContent() = finish()

        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            UanLog.w("$name failed to show: [${error.code}] ${error.message}")
            finish()
        }
    }

    /** Runs [show]; if it throws, reports the ad as finished so callers are never left waiting. */
    fun showSafely(name: String, onFinished: () -> Unit, show: () -> Unit) {
        try {
            show()
        } catch (e: Exception) {
            UanLog.e("$name show threw", e)
            isShowing = false
            onFinished()
        }
    }
}

/** A loaded ad with its load time; AdMob ads expire and must not be shown after [maxAgeMillis]. */
internal class Cached<T>(val ad: T, private val loadedAt: Long = android.os.SystemClock.elapsedRealtime()) {
    fun isFresh(maxAgeMillis: Long) = android.os.SystemClock.elapsedRealtime() - loadedAt < maxAgeMillis
}
