package uan.mod.helper

import android.app.Activity
import android.app.Application
import android.os.Build
import android.util.DisplayMetrics
import com.google.android.gms.ads.AdSize
import kotlin.math.roundToInt

object SizeUtils {
    fun pxToDp(app: Application, px: Int): Int {
        val displayMetrics: DisplayMetrics = app.resources.displayMetrics
        return (px / displayMetrics.density).roundToInt()
    }

    /**
     * Adaptive banner size for a container [containerWidthPx] wide (falls back to the window
     * width). With [maxHeightDp] the banner is an inline adaptive banner that fits a fixed-height
     * slot; otherwise it is a large anchored adaptive banner.
     */
    internal fun getAdSize(activity: Activity, containerWidthPx: Int = 0, maxHeightDp: Int? = null): AdSize {
        val density = activity.resources.displayMetrics.density
        val widthPx = if (containerWidthPx > 0) containerWidthPx else windowWidthPx(activity)
        val adWidth = (widthPx / density).toInt()
        return if (maxHeightDp != null) {
            AdSize.getInlineAdaptiveBannerAdSize(adWidth, maxHeightDp)
        } else {
            AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, adWidth)
        }
    }

    private fun windowWidthPx(activity: Activity): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.windowManager.currentWindowMetrics.bounds.width()
        } else {
            activity.resources.displayMetrics.widthPixels
        }

}
