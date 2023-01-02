package uan.mod.use

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import uan.mod.Ad
import uan.mod.R
import uan.mod.configs.NativeAdKeys
import uan.mod.configs.UaNativeAd

class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val adHolder: FrameLayout = view.findViewById(R.id.nativeAdHolder)

    fun bind(activity: Activity, ad: Ad, key: NativeAdKeys) {
        ad.showAdInFrame(activity, adHolder, key)
    }
}