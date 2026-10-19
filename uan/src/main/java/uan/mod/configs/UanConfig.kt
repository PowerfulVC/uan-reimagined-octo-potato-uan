package uan.mod.configs

import android.os.Bundle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationExtrasReceiver

/**
 * Optional runtime configuration. Assign to [uan.mod.Ad.config] before calling init.
 *
 * @param testDeviceIds hashed device IDs that receive test ads (also used for UMP debug settings).
 * @param networkExtras per-adapter extras for AdMob mediation, keyed by the adapter's extras
 *   receiver class (e.g. `AppLovinAdapter::class.java`); attached to every ad request.
 * @param configureRequest last-chance hook to customize every [AdRequest].
 * @param configureRequestConfiguration hook to customize the global [RequestConfiguration]
 *   (content rating, age-restricted treatment, ...). Applied before MobileAds is initialized.
 * @param consentDebugGeography `ConsentDebugSettings.DebugGeography` value to force a consent
 *   geography in debug builds. Leave null in production.
 * @param onPaidEvent impression-level revenue callback, fired for every ad format including
 *   impressions served by mediated networks.
 * @param splashTimeoutMillis max time [uan.mod.Ad.showSplashInter] waits for an ad.
 * @param debugLogging verbose logs, including the mediation waterfall on load failures.
 */
data class UanConfig(
    val testDeviceIds: List<String> = emptyList(),
    val networkExtras: Map<Class<out MediationExtrasReceiver>, Bundle> = emptyMap(),
    val configureRequest: ((AdRequest.Builder) -> Unit)? = null,
    val configureRequestConfiguration: ((RequestConfiguration.Builder) -> Unit)? = null,
    val consentDebugGeography: Int? = null,
    val onPaidEvent: ((UanAdRevenue) -> Unit)? = null,
    val splashTimeoutMillis: Long = 7000L,
    val debugLogging: Boolean = false,
)
