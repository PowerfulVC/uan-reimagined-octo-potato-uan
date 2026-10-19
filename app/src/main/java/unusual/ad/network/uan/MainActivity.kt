package unusual.ad.network.uan

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uan.mod.Ad
import uan.mod.AdImpl
import uan.mod.configs.AdUnit
import uan.mod.configs.NativeAdKeys
import uan.mod.configs.UanConfig
import uan.mod.helper.NativeAdBuilder

class MainActivity : AppCompatActivity() {

    // In a real app, hold a single Ad instance in your Application class.
    private val ad: Ad by lazy { AdImpl(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(top = bars.top, bottom = bars.bottom, left = bars.left, right = bars.right)
            insets
        }

        ad.config = UanConfig(
            debugLogging = BuildConfig.DEBUG,
            onPaidEvent = { Log.d("Revenue", "$it") },
        )
        ad.frameAds.putAdStyles(
            mutableListOf(
                NativeAdBuilder()
                    .layout(uan.mod.R.layout.ad_layoujt)
                    .coreView(uan.mod.R.id.unifiedNativeAdView)
                    .mediaView(uan.mod.R.id.ad_media)
                    .headline(uan.mod.R.id.ad_headline)
                    .body(uan.mod.R.id.ad_body)
                    .button(uan.mod.R.id.ad_call_to_action)
                    .icon(uan.mod.R.id.ad_icon)
                    .build(NativeAdKeys.BIG_ONE)
            )
        )

        lifecycleScope.launch {
            ad.initWithConsent(this@MainActivity, TEST_UNITS).join()
            findViewById<View>(R.id.privacyOptions).visibility =
                if (ad.consent.isPrivacyOptionsRequired) View.VISIBLE else View.GONE
            ad.showAdInFrame(this@MainActivity, findViewById<FrameLayout>(R.id.nativeFrame), null)
            ad.showBanner(this@MainActivity, findViewById(R.id.bannerFrame))
        }

        findViewById<Button>(R.id.showInter).setOnClickListener {
            lifecycleScope.launch { ad.showInter(this@MainActivity) { Log.d("Sample", "Interstitial closed") } }
        }
        findViewById<Button>(R.id.showReward).setOnClickListener {
            lifecycleScope.launch { ad.showReward(this@MainActivity) { Log.d("Sample", "Rewarded: $it") } }
        }
        findViewById<Button>(R.id.privacyOptions).setOnClickListener {
            ad.consent.showPrivacyOptionsForm(this)
        }
        findViewById<Button>(R.id.adInspector).setOnClickListener {
            ad.openAdInspector(this)
        }
    }

    private companion object {
        // Google's public test ad units.
        val TEST_UNITS = AdUnit(
            admob = true,
            app = "ca-app-pub-3940256099942544~3347511713",
            interstitial = "ca-app-pub-3940256099942544/1033173712",
            native = "ca-app-pub-3940256099942544/2247696110",
            open = "ca-app-pub-3940256099942544/9257395921",
            rewarded = "ca-app-pub-3940256099942544/5354046379",
            banner = "ca-app-pub-3940256099942544/9214589741",
        )
    }
}
