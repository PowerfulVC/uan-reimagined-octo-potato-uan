package uan.mod.configs

import android.graphics.Typeface
import android.util.Log

object UaNativeAd {
    var font: Typeface? = null
    var textColorHex: String? = null
    var adBodyHex: String? = null
    var btnHex: String? = null
    var btnTextHex: String? = null

    fun setupNativeAdView(
        font: Typeface,
        textColorHex: String,
        adBodyHex: String,
        btnHex: String,
        btnTextHex: String
    ) {
        this.font = font
        this.textColorHex = textColorHex
        this.adBodyHex = adBodyHex
        this.btnHex = btnHex
        this.btnTextHex = btnTextHex
        Log.d("UAN", "Native ad configured")
    }
}