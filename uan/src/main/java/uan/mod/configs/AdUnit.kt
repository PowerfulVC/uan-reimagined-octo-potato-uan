package uan.mod.configs

import com.google.gson.annotations.SerializedName

data class AdUnit(
    @SerializedName("admob")
    val admob: Boolean = true,
    @SerializedName("app")
    val app : String = "",
    @SerializedName("interstitial")
    val interstitial : String = "",
    @SerializedName("native")
    val native : String = "",
    @SerializedName("open")
    val open : String = "",
    @SerializedName("rewarded")
    val rewarded : String = "",
    @SerializedName("banner")
    val banner : String = "",
)