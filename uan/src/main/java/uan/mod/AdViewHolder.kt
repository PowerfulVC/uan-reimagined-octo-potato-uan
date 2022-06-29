package uan.mod

import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView

class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val adHolder: FrameLayout = view.findViewById(R.id.nativeAdHolder)

    fun bind(nativeADUnit: String) {
        Ad.showNative(adHolder, nativeADUnit)
    }
}