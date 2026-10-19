package uan.mod.helper

import android.app.Application
import android.content.pm.PackageManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.AdapterStatus
import com.google.android.gms.ads.initialization.InitializationStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uan.mod.callbacks.OnReInit
import uan.mod.configs.AdUnit
import java.util.concurrent.atomic.AtomicBoolean

internal object AdInitializer {
    private const val APP_ID_KEY = "com.google.android.gms.ads.APPLICATION_ID"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val started = AtomicBoolean(false)
    private val completion = CompletableDeferred<InitializationStatus>()

    val isStarted: Boolean get() = started.get()

    @Volatile
    var status: InitializationStatus? = null
        private set

    /**
     * Initializes MobileAds (and every mediation adapter) once per process. Later calls only
     * refresh ad units through [onReInit].
     */
    fun initAds(app: Application, adUnit: AdUnit, onReInit: OnReInit) {
        verifyAppId(app, adUnit.app)
        if (!started.compareAndSet(false, true)) {
            if (completion.isCompleted) onReInit.onAdReInit()
            return
        }
        MobileAds.setRequestConfiguration(UanRuntime.requestConfiguration())
        // Google recommends initializing off the main thread; the callback arrives on main.
        scope.launch {
            MobileAds.initialize(app) { initStatus ->
                status = initStatus
                logAdapterStatuses(initStatus)
                completion.complete(initStatus)
                UanLog.d("UAN initialized")
                onReInit.onAdReInit()
            }
        }
    }

    /** Suspends until MobileAds and all mediation adapters have finished initializing. */
    suspend fun awaitInitialized(): InitializationStatus = completion.await()

    private fun logAdapterStatuses(status: InitializationStatus) {
        status.adapterStatusMap.forEach { (adapter, adapterStatus) ->
            if (adapterStatus.initializationState == AdapterStatus.State.READY) {
                UanLog.d("Adapter $adapter ready (${adapterStatus.latency}ms)")
            } else {
                UanLog.w("Adapter $adapter not ready: ${adapterStatus.description}")
            }
        }
    }

    private fun verifyAppId(app: Application, expected: String) {
        val manifestId = try {
            app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
                .metaData?.getString(APP_ID_KEY)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        if (manifestId.isNullOrEmpty()) {
            UanLog.e("AdMob app ID missing. Set manifestPlaceholders = [admobAppId: \"...\"] in your app module.")
        } else if (expected.isNotEmpty() && expected != manifestId) {
            UanLog.e("AdMob app ID mismatch: manifest has $manifestId but ad units belong to $expected. Ads may not fill.")
        }
    }
}
