package uan.mod.helper

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.ads.MobileAds
import uan.mod.callbacks.OnReInit
import uan.mod.configs.AdUnit

internal object AdInitializer {
    fun initAds(app: Application, adUnit: AdUnit, onReInit: OnReInit) {
        try {
            val applicationInfo: ApplicationInfo = app.packageManager.getApplicationInfo(
                app.packageName,
                PackageManager.GET_META_DATA
            )
            applicationInfo.metaData.putString(
                "com.google.android.gms.ads.APPLICATION_ID",
                adUnit.app
            )
            MobileAds.initialize(app) {
                onReInit.onAdReInit()
                Log.e("UAN", "UAN Initialized Successfully")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }
}