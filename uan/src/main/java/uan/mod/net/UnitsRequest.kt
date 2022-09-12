package uan.mod.net

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import uan.mod.configs.AdUnit
import java.lang.Exception
import javax.security.auth.callback.Callback

class UnitsRequest {
    private var retries = 0
    private var url: String = ""


    fun setupUnitsUrl(unitsUrl: String) {
        this.url = unitsUrl
    }

    fun request(result: (adUnit: AdUnit?) -> Unit) {
        if (retries > 3) {
            result.invoke(null)
            return
        }
        if (url.contains("https://")) {
            val request = Request.Builder()
                .url(url)
                .build()
            OkHttpClient().newCall(request).enqueue(object : Callback, okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    e.printStackTrace()
                    result.invoke(null)
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            val units = Gson().fromJson(response.body?.string(), AdUnit::class.java)
                            result.invoke(units)
                            retries = 0
                        } catch (e: Exception) {
                            result.invoke(null)
                        }

                    } else {
                        result.invoke(null)
                    }
                }
            })
        }
    }

    fun retry(result: (adUnit: AdUnit?) -> Unit) {
        if (url.contains("https://")) {
            retries += 1
            request(result)
        }
    }
}