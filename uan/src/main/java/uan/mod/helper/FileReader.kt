package uan.mod.helper

import android.content.Context
import java.io.IOException

object FileReader {
    fun readFromAssets(context: Context, filename: String): String {
        return try {
            context.assets.open(filename).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            UanLog.e("Failed to read asset $filename", e)
            ""
        }
    }
}
