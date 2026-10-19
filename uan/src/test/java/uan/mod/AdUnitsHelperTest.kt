package uan.mod

import android.app.Application
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uan.mod.callbacks.OnReInit
import uan.mod.configs.AdUnit
import uan.mod.helper.AdUnitsHelper
import uan.mod.models.AdType

class AdUnitsHelperTest {
    private val helper = AdUnitsHelper(Application(), object : OnReInit {
        override fun onAdReInit() = Unit
    })

    private val defaults = AdUnit(app = "app~1", interstitial = "inter/1", banner = "banner/1")

    @Test
    fun `no units means nothing is valid`() {
        AdType.entries.forEach { assertFalse(helper.isValid(it)) }
        assertEquals("", helper.getAdUnit(AdType.INTERSTITIAL))
    }

    @Test
    fun `synchronized units replace defaults`() {
        helper.setDefaultAdUnits(defaults)
        helper.setSynchronizedAdUnits(defaults.copy(interstitial = "inter/2"))
        assertEquals("inter/2", helper.getAdUnit(AdType.INTERSTITIAL))
    }

    @Test
    fun `incomplete server units keep defaults`() {
        helper.setDefaultAdUnits(defaults)
        helper.setSynchronizedAdUnits(AdUnit(app = "app~1"))
        assertEquals("inter/1", helper.getAdUnit(AdType.INTERSTITIAL))
        assertTrue(helper.isValid(AdType.BANNER))
        assertFalse(helper.isValid(AdType.NATIVE))
    }

    @Test
    fun `units parse from json with missing fields`() {
        val units = Gson().fromJson(
            """{"admob":false,"app":"app~1","interstitial":"inter/1"}""",
            AdUnit::class.java
        )
        helper.setDefaultAdUnits(units)
        assertFalse(helper.providerIsAdmob())
        assertEquals("inter/1", helper.getAdUnit(AdType.INTERSTITIAL))
        assertEquals("", helper.getAdUnit(AdType.OPEN))
    }
}
