package uan.mod.use

import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import uan.mod.Ad
import uan.mod.R
import uan.mod.configs.UaNativeAd

class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val adHolder: FrameLayout = view.findViewById(R.id.nativeAdHolder)

    fun bind(nativeADUnit: String, nativeADConfig: UaNativeAd) {
        Ad.showNative(adHolder, nativeADUnit, nativeADConfig)
    }
}