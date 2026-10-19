package uan.mod.net

import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import uan.mod.configs.AdUnit
import uan.mod.helper.UanLog
import java.io.IOException
import java.util.concurrent.TimeUnit

class UnitsRequest {
    private var retries = 0
    private var url: String = ""

    fun setupUnitsUrl(unitsUrl: String) {
        this.url = unitsUrl
    }

    /** Fetches ad units over HTTPS. [result] is invoked once, on a background thread. */
    fun request(result: (adUnit: AdUnit?) -> Unit) {
        if (retries > 3 || !url.startsWith("https://")) {
            result.invoke(null)
            return
        }
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                UanLog.w("Ad units request failed: ${e.message}")
                result.invoke(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val units = response.use {
                    if (!it.isSuccessful) return@use null
                    try {
                        Gson().fromJson(it.body.string(), AdUnit::class.java)
                    } catch (e: Exception) {
                        UanLog.w("Ad units response is invalid: ${e.message}")
                        null
                    }
                }
                if (units != null) retries = 0
                result.invoke(units)
            }
        })
    }

    fun retry(result: (adUnit: AdUnit?) -> Unit) {
        retries += 1
        request(result)
    }

    private companion object {
        val client: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .build()
        }
    }
}
