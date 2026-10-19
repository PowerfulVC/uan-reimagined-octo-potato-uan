package uan.mod.helper

import android.app.Activity
import android.app.Application
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * GDPR / US-state privacy consent through Google's UMP SDK. AdMob and Google-certified
 * mediation partners read the resulting IAB TCF / GPP strings automatically.
 */
class ConsentManager internal constructor(app: Application) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(app)

    @Volatile
    private var requestedThisSession = false

    /** True when ads may be requested, based on consent from this or a previous session. */
    val canRequestAds: Boolean get() = consentInformation.canRequestAds()

    /** Show a "Privacy settings" entry in your app only when this is true. */
    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Ads are allowed when consent permits them, or when the host app never runs the consent
     * flow (legacy integrations, apps without EEA/UK/US-state traffic).
     */
    internal val adsAllowed: Boolean get() = !requestedThisSession || canRequestAds

    /** Updates consent status and shows the consent form if required. Call on every launch. */
    fun gather(activity: Activity, onComplete: (canRequestAds: Boolean) -> Unit) {
        requestedThisSession = true
        val params = ConsentRequestParameters.Builder().apply {
            val geography = UanRuntime.config.consentDebugGeography
            if (geography != null) {
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(geography)
                        .apply { UanRuntime.config.testDeviceIds.forEach(::addTestDeviceHashedId) }
                        .build()
                )
            }
        }.build()

        consentInformation.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                if (formError != null) UanLog.w("Consent form error: ${formError.message}")
                onComplete(canRequestAds)
            }
        }, { requestError ->
            UanLog.w("Consent info update failed: ${requestError.message}")
            onComplete(canRequestAds)
        })
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismissed: (() -> Unit)? = null) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) UanLog.w("Privacy options form error: ${formError.message}")
            onDismissed?.invoke()
        }
    }
}
