package uan.mod.models

import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import uan.mod.configs.NativeAdKeys
import uan.mod.configs.UaNativeAd

data class NativeAdConfig(
    var key: NativeAdKeys,

    @param:LayoutRes
    var mainLayout: Int = 0,

    @param:IdRes
    var nativeAdViewId: Int = 0,

    @param:IdRes
    var mediaViewId: Int = 0,

    @param:IdRes
    var headlineViewId: Int = 0,

    @param:IdRes
    var bodyViewId: Int = 0,

    @param:IdRes
    var callToActionViewId: Int = 0,

    @param:IdRes
    var advertiserViewId: Int = 0,

    @param:IdRes
    var iconViewId: Int = 0,

    var uanNativeAd: UaNativeAd?
)