package uan.mod.helper

import android.util.Log

internal object UanLog {
    private const val TAG = "UAN"

    @Volatile
    var debug = false

    fun d(message: String) {
        if (debug) Log.d(TAG, message)
    }

    fun w(message: String) = Log.w(TAG, message)

    fun e(message: String, error: Throwable? = null) = Log.e(TAG, message, error)
}
