package uan.mod.helper

import android.app.Application
import android.util.Log
import uan.mod.callbacks.OnReInit
import uan.mod.configs.AdUnit
import uan.mod.models.AdType
import uan.mod.net.UnitsRequest

class AdUnitsHelper(private val app: Application, private val onReInit: OnReInit) {
    private var adUnit: AdUnit? = null

    fun setDefaultAdUnits(adUnit: AdUnit) {
        if (adUnit.app.isNotEmpty() && adUnit.interstitial.isNotEmpty()) {
            this.adUnit = adUnit
        } else {
            Log.e("UAN_ERROR", "Failed to setup default ad units.")
        }
    }

    fun setSynchronizedAdUnits(adUnit: AdUnit) {
        if (adUnit.app.isNotEmpty() && adUnit.interstitial.isNotEmpty()) {
            this.adUnit = adUnit
        } else if (this.adUnit == null) {
            Log.e("UAN_ERROR", "AD Units is null.")
            Log.e("UAN_ERROR", "==================")
            Log.e("UAN_ERROR", "Default ad units not specified")
            Log.e("UAN_ERROR", "==================")
        } else {
            Log.e("UAN_ERROR", "Server returned incorrect ad units. Using defaults. ")
        }
    }

    fun providerIsAdmob(): Boolean = adUnit?.admob == true

    fun getAdUnit(adType: AdType): String {
        when (adType) {
            AdType.BANNER -> {
                return adUnit?.banner ?: ""
            }
            AdType.INTERSTITIAL -> {
                return adUnit?.interstitial ?: ""
            }
            AdType.NATIVE -> {
                return adUnit?.native ?: ""
            }
            AdType.OPEN -> {
                return adUnit?.open ?: ""
            }
            AdType.REWARD -> {
                return adUnit?.rewarded ?: ""
            }
        }
    }

    fun verifyAdUnits(adType: AdType, onCorrect: (isCorrect: Boolean) -> Unit) {
        when (adType) {
            AdType.BANNER -> {
                onCorrect.invoke(!adUnit?.banner.isNullOrEmpty())
            }
            AdType.REWARD -> {
                onCorrect.invoke(!adUnit?.rewarded.isNullOrEmpty())
            }
            AdType.INTERSTITIAL -> {
                onCorrect.invoke(!adUnit?.interstitial.isNullOrEmpty())
            }
            AdType.NATIVE -> {
                Log.e("UAN", "Native ad unit ${adUnit?.native}")
                if (adUnit?.native.isNullOrEmpty()) {
                    reloadAdUnits()
                }
                onCorrect.invoke(!adUnit?.native.isNullOrEmpty())
            }
            AdType.OPEN -> {
                onCorrect.invoke(!adUnit?.open.isNullOrEmpty())
            }
        }
    }

    private fun reloadAdUnits() {
        Log.e("UAN", "Reload ad units")
        UnitsRequest.retry {
            if (it != null) {
                setSynchronizedAdUnits(it)
                initAd(null)
            }
        }
    }

    fun initAd(action: (() -> Unit)?) {
        Log.d("UAN", "UAN INITING ADS ${System.currentTimeMillis()}")
        adUnit?.let { it1 -> AdInitializer.initAds(app, it1, onReInit) }
        action?.invoke()
    }

}
