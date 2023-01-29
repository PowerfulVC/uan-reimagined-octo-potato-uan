package uan.mod.helper

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.widget.Button
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
import uan.mod.configs.UaNativeAd
import uan.mod.models.NativeAdConfig
import java.lang.Exception


class FrameAds {
    private var adStyles = mutableListOf<NativeAdConfig>()

    fun putAdStyles(list: MutableList<NativeAdConfig>) {
        this.adStyles.clear()
        this.adStyles.addAll(list)
    }

    fun showNativeAd(holder: FrameLayout, styleKey: NativeAdKeys, nativeAd: NativeAd?) {
        try {
            val adConfig =
                adStyles.firstOrNull { it.key == styleKey } ?: adStyles.firstOrNull() ?: {
                    Log.e("Info", "No styles specified.")
                }

            val config = adConfig as? NativeAdConfig ?: return

            val inflatedView = LayoutInflater.from(holder.context)
                .inflate(
                    config.mainLayout,
                    null
                ) as CardView


            if (nativeAd != null) {
                setupStyle(inflatedView, config)
                mapNativeAd(nativeAd, inflatedView, config)
                holder.removeAllViews()
                holder.addView(inflatedView)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            holder.visibility = GONE
        }
    }

    private fun NativeAdView.findViewByIdSafe(@IdRes id: Int): View? {
        return if (id != 0) {
            findViewById(id)
        } else {
            null
        }
    }

    private fun mapNativeAd(adFromGoogle: NativeAd, view: CardView, config: NativeAdConfig) {
        val myAdView: NativeAdView =
            view.findViewById(config.nativeAdViewId)

        val mediaView: MediaView? =
            myAdView.findViewByIdSafe(config.mediaViewId) as? MediaView

        myAdView.mediaView = mediaView
        myAdView.headlineView = myAdView.findViewByIdSafe(config.headlineViewId) as? TextView
        myAdView.bodyView = myAdView.findViewByIdSafe(config.bodyViewId) as? TextView
        myAdView.callToActionView = myAdView.findViewByIdSafe(config.callToActionViewId) as? Button
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
            (myAdView.callToActionView as? Button)?.text = adFromGoogle.callToAction
        }
        if (adFromGoogle.icon == null) {
            myAdView.iconView?.visibility = GONE
        } else {
            (myAdView.iconView as? ImageView)?.setImageDrawable(adFromGoogle.icon?.drawable)
        }
        myAdView.setNativeAd(adFromGoogle)
    }

    private fun setupStyle(view: CardView, config: NativeAdConfig) {
        if (config.uanNativeAd == null) {
            return
        }
        if (config.uanNativeAd?.adBodyHex != null) {
            view.setCardBackgroundColor(
                Color.parseColor(
                    config.uanNativeAd!!.adBodyHex
                )
            )
        }

        val font = config.uanNativeAd?.font

        if (font != null) {
            view.findViewById<TextView>(config.callToActionViewId).typeface =
                font
            view.findViewById<TextView>(config.advertiserViewId)?.typeface =
                font
            view.findViewById<TextView>(config.bodyViewId).typeface =
                font
            view.findViewById<TextView>(config.headlineViewId).typeface =
                font
        }

        val textColor = config.uanNativeAd?.textColorHex

        if (textColor != null) {

            view.findViewById<TextView>(config.bodyViewId)
                .setTextColor(Color.parseColor(textColor))
            view.findViewById<TextView>(config.headlineViewId)
                .setTextColor(Color.parseColor(textColor))

        }

        if (config.uanNativeAd?.btnHex != null) {
            view.findViewById<Button>(config.callToActionViewId)?.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(config.uanNativeAd!!.btnHex))
        }
        if (config.uanNativeAd?.btnTextHex != null) {
            view.findViewById<Button>(config.callToActionViewId)
                ?.setTextColor(Color.parseColor(config.uanNativeAd!!.btnTextHex))
        }
    }


}
