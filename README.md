# UAN — AdMob wrapper

Interstitial, rewarded interstitial, app open, native and adaptive banner ads on top of the
Google Mobile Ads SDK 25.5.0, with UMP consent (GDPR / US states) and AdMob mediation support.

Requirements: minSdk 24, compileSdk 37 (Android 17), AGP 9.4+, JDK 17+.

## Setup

1. Provide your AdMob app ID in the **app** module (required, the build fails without it):

   ```groovy
   android.defaultConfig.manifestPlaceholders = [admobAppId: "ca-app-pub-xxxxxxxx~yyyyyyyy"]
   ```

2. Create one `AdImpl` in your `Application`, configure it, then initialize from your launch activity:

   ```kotlin
   ad.config = UanConfig(
       debugLogging = BuildConfig.DEBUG,
       onPaidEvent = { revenue -> analytics.logAdRevenue(revenue) },
   )
   lifecycleScope.launch {
       ad.initWithConsent(activity, adUnits).join()
       privacyButton.isVisible = ad.consent.isPrivacyOptionsRequired
   }
   ```

   `initWithConsent` runs the UMP consent flow on every launch and only initializes ads when the user's
   consent allows it. `init(adUnit)` still works for apps with no EEA/UK/US-state traffic.
   If `consent.isPrivacyOptionsRequired`, you must offer a "Privacy settings" entry that calls
   `ad.consent.showPrivacyOptionsForm(activity)`.

## AdMob mediation

1. Enable networks in your AdMob mediation groups.
2. Add the matching adapter for each network to the app module, e.g.
   `implementation 'com.google.ads.mediation:applovin:13.6.4.2'` (see `app/build.gradle`). Pangle and
   Mintegral also need their maven repos (see `settings.gradle`).
3. Nothing else is needed for most networks: `MobileAds.initialize` starts every adapter, and the
   consent strings written by UMP are read by Google-certified partners.
4. Network-specific options go in `UanConfig(networkExtras = mapOf(AdapterClass::class.java to bundle))`.

Debugging: `ad.adapterStatuses()` lists the initialization state of each adapter, `debugLogging = true`
logs the waterfall on every failed load, and `ad.openAdInspector(activity)` opens Ad Inspector
on test devices. `UanAdRevenue.adSourceName` names the network that served each impression.

## Behavior guarantees

- `showInter`, `showSplashInter` and `showReward` call their callback exactly once, on the main thread,
  including when the ad fails to show.
- Cached ads expire (1 h, or 4 h for app open ads) and are never shown stale.
- App open ads never show on top of another full-screen ad or right after the user returns from an ad click.
- Banner and native ads are paused, resumed and destroyed with the activity that shows them.
- Setting `premiumUser = true` releases all loaded ads.
