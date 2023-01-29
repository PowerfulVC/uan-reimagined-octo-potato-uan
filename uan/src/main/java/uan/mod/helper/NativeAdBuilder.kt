package uan.mod.helper

import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import uan.mod.configs.NativeAdKeys
import uan.mod.configs.UaNativeAd
import uan.mod.models.NativeAdConfig

class NativeAdBuilder {
    @LayoutRes
    private var mainLayout: Int = 0

    @IdRes
    private var nativeAdViewId: Int = 0

    @IdRes
    private var mediaViewId: Int = 0

    @IdRes
    private var headlineViewId: Int = 0

    @IdRes
    private var bodyViewId: Int = 0

    @IdRes
    private var callToActionViewId: Int = 0

    @IdRes
    private var advertiserViewId: Int = 0

    @IdRes
    private var iconViewId: Int = 0

    private var uanNativeAd: UaNativeAd? = null


    fun style(uaNativeAd: UaNativeAd?): NativeAdBuilder {
        this.uanNativeAd = uaNativeAd
        return this
    }

    fun layout(@LayoutRes res: Int): NativeAdBuilder {
        this.mainLayout = res
        return this
    }

    fun coreView(@IdRes res: Int): NativeAdBuilder {
        this.nativeAdViewId = res
        return this
    }

    fun mediaView(@IdRes res: Int): NativeAdBuilder {
        this.mediaViewId = res
        return this
    }

    fun headline(@IdRes res: Int): NativeAdBuilder {
        this.headlineViewId = res
        return this
    }

    fun body(@IdRes res: Int): NativeAdBuilder {
        this.bodyViewId = res
        return this
    }

    fun button(@IdRes res: Int): NativeAdBuilder {
        this.callToActionViewId = res
        return this
    }

    fun icon(@IdRes res: Int): NativeAdBuilder {
        this.iconViewId = res
        return this
    }

    fun advertiser(@IdRes res: Int): NativeAdBuilder {
        this.advertiserViewId = res
        return this
    }


    fun build(key: NativeAdKeys): NativeAdConfig {
        return NativeAdConfig(
            key,
            this.mainLayout,
            this.nativeAdViewId,
            this.mediaViewId,
            this.headlineViewId,
            this.bodyViewId,
            this.callToActionViewId,
            this.advertiserViewId,
            this.iconViewId,
            this.uanNativeAd
        )
    }

}