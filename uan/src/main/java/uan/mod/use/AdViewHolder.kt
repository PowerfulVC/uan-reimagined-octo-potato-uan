package uan.mod.use

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uan.mod.Ad
import uan.mod.AdNew
import uan.mod.R
import uan.mod.configs.UaNativeAd

class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val adHolder: FrameLayout = view.findViewById(R.id.nativeAdHolder)

    fun bind(nativeADUnit: String, nativeADConfig: UaNativeAd) {
        Ad.showNative(adHolder, nativeADUnit, nativeADConfig)
    }

    fun bindNew(
        nativeADUnit: String,
        nativeADConfig: UaNativeAd,
        scope: CoroutineScope,
        uan: AdNew,
        activity: Activity
    ) {
        scope.launch {
            withContext(Dispatchers.Main) {
                uan.showAdInFrame(activity, adHolder)
            }
        }
        Ad.showNative(adHolder, nativeADUnit, nativeADConfig)
    }
}
