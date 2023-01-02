package uan.mod.helper

import android.app.Activity
import android.app.Application
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.CompletableDeferred
import uan.mod.Ad
import uan.mod.R
import uan.mod.configs.UaNativeAd


class FrameAds {
    var nativeAdConfig: UaNativeAd = UaNativeAd()

    fun showNative(frameLayout: FrameLayout, small: Boolean, nativeAd: NativeAd?) {
        try {
            val unifiedNativeAdView = LayoutInflater.from(frameLayout.context)
                .inflate(
                    if (small) R.layout.ad_layout_small else R.layout.ad_layoujt,
                    null
                ) as CardView
            unifiedNativeAdView.setCardBackgroundColor(
                Color.parseColor(
                    nativeAdConfig?.adBodyHex
                )
            )
            unifiedNativeAdView.findViewById<TextView>(R.id.ad_call_to_action).typeface =
                nativeAdConfig.font
            unifiedNativeAdView.findViewById<TextView>(R.id.ad_body).typeface =
                nativeAdConfig.font
            unifiedNativeAdView.findViewById<TextView>(R.id.ad_headline).typeface =
                nativeAdConfig.font
            //set text color
            unifiedNativeAdView.findViewById<TextView>(R.id.ad_body)
                .setTextColor(Color.parseColor(nativeAdConfig.textColorHex))
            unifiedNativeAdView.findViewById<TextView>(R.id.ad_headline)
                .setTextColor(Color.parseColor(nativeAdConfig.textColorHex))
            //set btn color
            unifiedNativeAdView.findViewById<Button>(R.id.ad_call_to_action)?.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(nativeAdConfig.btnHex))
            unifiedNativeAdView.findViewById<Button>(R.id.ad_call_to_action)
                ?.setTextColor(Color.parseColor(nativeAdConfig.btnTextHex))
            mapUnifiedNativeAdToLayout(nativeAd!!, unifiedNativeAdView)
            frameLayout.removeAllViews()
            frameLayout.addView(unifiedNativeAdView)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            frameLayout.visibility = GONE
        }
    }

    private fun mapUnifiedNativeAdToLayout(adFromGoogle: NativeAd, card: CardView) {
        val myAdView: NativeAdView =
            card.findViewById(R.id.unifiedNativeAdView)
        val mediaView: MediaView =
            myAdView.findViewById(R.id.ad_media)
        myAdView.mediaView = mediaView
        myAdView.headlineView = myAdView.findViewById(R.id.ad_headline)
        myAdView.bodyView = myAdView.findViewById(R.id.ad_body)
        myAdView.callToActionView = myAdView.findViewById(R.id.ad_call_to_action)
        myAdView.iconView = myAdView.findViewById(R.id.ad_icon)
        (myAdView.headlineView as TextView).text = adFromGoogle.headline
        if (adFromGoogle.body == null) {
            myAdView.bodyView?.visibility = View.GONE
        } else {
            (myAdView.bodyView as TextView).text = adFromGoogle.body
        }
        if (adFromGoogle.callToAction == null) {
            myAdView.callToActionView?.visibility = View.GONE
        } else {
            (myAdView.callToActionView as Button).text = adFromGoogle.callToAction
        }
        if (adFromGoogle.icon == null) {
            myAdView.iconView?.visibility = View.GONE
        } else {
            (myAdView.iconView as ImageView).setImageDrawable(adFromGoogle.icon?.drawable)
        }
        myAdView.setNativeAd(adFromGoogle)
    }

}
