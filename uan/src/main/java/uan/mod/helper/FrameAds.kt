package uan.mod.helper

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.cardview.widget.CardView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import uan.mod.R
import uan.mod.configs.NativeAdKeys
import uan.mod.models.NativeAdConfig


class FrameAds {
    private var adStyles = mutableListOf<NativeAdConfig>()

    fun putAdStyles(list: MutableList<NativeAdConfig>) {
        this.adStyles.clear()
        this.adStyles.addAll(list)
    }

    /**
     * Renders [nativeAd] into [holder]. The holder owns the ad from then on: the previously shown
     * ad is destroyed, and so is [nativeAd] if it cannot be shown. Returns true when shown.
     */
    fun showNativeAd(holder: FrameLayout, styleKey: NativeAdKeys, nativeAd: NativeAd?): Boolean {
        if (nativeAd == null) return false
        val config = adStyles.firstOrNull { it.key == styleKey } ?: adStyles.firstOrNull()
        if (config == null) {
            Log.e("Info", "No styles specified.")
            nativeAd.destroy()
            return false
        }
        return try {
            val inflatedView = LayoutInflater.from(holder.context)
                .inflate(config.mainLayout, holder, false)

            setupStyle(inflatedView, config)
            mapNativeAd(nativeAd, inflatedView, config)
            releaseNativeAd(holder)
            holder.removeAllViews()
            holder.addView(inflatedView)
            holder.setTag(R.id.uan_native_ad_tag, nativeAd)
            true
        } catch (e: Exception) {
            UanLog.e("Failed to render native ad", e)
            nativeAd.destroy()
            holder.visibility = GONE
            false
        }
    }

    /** Destroys the native ad currently shown in [holder], if any. */
    fun releaseNativeAd(holder: FrameLayout) {
        (holder.getTag(R.id.uan_native_ad_tag) as? NativeAd)?.destroy()
        holder.setTag(R.id.uan_native_ad_tag, null)
    }

    private fun View.findViewByIdSafe(@IdRes id: Int): View? {
        return if (id != 0) {
            findViewById(id)
        } else {
            null
        }
    }

    private fun mapNativeAd(adFromGoogle: NativeAd, view: View, config: NativeAdConfig) {
        val myAdView: NativeAdView =
            view.findViewById(config.nativeAdViewId)

        val mediaView: MediaView? =
            myAdView.findViewByIdSafe(config.mediaViewId) as? MediaView

        myAdView.mediaView = mediaView
        myAdView.headlineView = myAdView.findViewByIdSafe(config.headlineViewId) as? TextView
        myAdView.bodyView = myAdView.findViewByIdSafe(config.bodyViewId) as? TextView
        myAdView.callToActionView = myAdView.findViewByIdSafe(config.callToActionViewId) as? TextView
        myAdView.advertiserView = myAdView.findViewByIdSafe(config.advertiserViewId) as? TextView
        myAdView.iconView = myAdView.findViewByIdSafe(config.iconViewId) as? ImageView

        if (config.advertiserViewId != 0)
            mediaView?.setImageScaleType(ImageView.ScaleType.CENTER_CROP)

        (myAdView.headlineView as? TextView)?.text = adFromGoogle.headline
        if (adFromGoogle.body == null) {
            myAdView.bodyView?.visibility = GONE
        } else {
            (myAdView.bodyView as? TextView)?.text = adFromGoogle.body
        }
        if (adFromGoogle.callToAction == null) {
            myAdView.callToActionView?.visibility = GONE
        } else {
            (myAdView.callToActionView as? TextView)?.text = adFromGoogle.callToAction
        }
        if (adFromGoogle.icon == null) {
            myAdView.iconView?.visibility = GONE
        } else {
            (myAdView.iconView as? ImageView)?.setImageDrawable(adFromGoogle.icon?.drawable)
        }
        if (adFromGoogle.advertiser == null) {
            myAdView.advertiserView?.visibility = GONE
        } else {
            (myAdView.advertiserView as? TextView)?.text = adFromGoogle.advertiser
        }
        myAdView.setNativeAd(adFromGoogle)
    }

    private fun setupStyle(view: View, config: NativeAdConfig) {
        val style = config.uanNativeAd ?: return

        style.adBodyHex?.let { hex ->
            val color = Color.parseColor(hex)
            if (view is CardView) view.setCardBackgroundColor(color) else view.setBackgroundColor(color)
        }

        val headline = view.findViewByIdSafe(config.headlineViewId) as? TextView
        val body = view.findViewByIdSafe(config.bodyViewId) as? TextView
        val advertiser = view.findViewByIdSafe(config.advertiserViewId) as? TextView
        val button = view.findViewByIdSafe(config.callToActionViewId) as? TextView

        style.font?.let { font ->
            listOf(headline, body, advertiser, button).forEach { it?.typeface = font }
        }

        style.textColorHex?.let { hex ->
            val color = Color.parseColor(hex)
            headline?.setTextColor(color)
            body?.setTextColor(color)
        }

        style.btnHex?.let { button?.backgroundTintList = ColorStateList.valueOf(Color.parseColor(it)) }
        style.btnTextHex?.let { button?.setTextColor(Color.parseColor(it)) }
    }


}
