package uan.mod.helper

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception

object FileReader {
    fun readFromAssets(context: Context, filename: String): String {
        try {
            var string: String? = ""
            val stringBuilder = StringBuilder()
            val `is`: InputStream = context.assets.open(filename)
            val reader = BufferedReader(InputStreamReader(`is`))
            while (true) {
                try {
                    if (reader.readLine().also { string = it } == null) break
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                stringBuilder.append(string).append("\n")
                return stringBuilder.toString()
            }
            `is`.close()
            return ""
        } catch (e: Exception) {
            return ""
        }
    }
}