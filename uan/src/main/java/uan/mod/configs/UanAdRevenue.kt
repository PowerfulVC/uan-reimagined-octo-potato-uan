package uan.mod.configs

import uan.mod.models.AdType

/**
 * Impression-level ad revenue. [adSourceName] is the network that served the impression
 * (e.g. "AdMob Network", "AppLovin"), which is what mediation reporting needs.
 */
data class UanAdRevenue(
    val adType: AdType,
    val adUnitId: String,
    val valueMicros: Long,
    val currencyCode: String,
    val precisionType: Int,
    val adSourceName: String?,
    val adSourceId: String?,
)
