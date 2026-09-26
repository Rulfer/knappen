package com.bardsplayground.knappen

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.telephony.TelephonyManager
import java.util.Locale

/**
 * Knappen's own language rule (instead of Android's normal "follow the phone language"):
 * Norwegian (Bokmål, `values-nb`) if the phone language is Norwegian (nb / nn / no) OR the phone is in Norway
 * (Norwegian SIM or mobile network, or the phone's region is set to Norway). English otherwise.
 *
 * Android does not apply this automatically. Every place that resolves strings must go through [localized]:
 * activities in `attachBaseContext`, and widget / notification code that only has a receiver context.
 */
object AppLanguage {
    private val NORWEGIAN_LANGUAGES = setOf("nb", "nn", "no")
    private const val NORWAY = "no"

    private val NORWEGIAN: Locale = Locale.forLanguageTag("nb-NO")
    private val ENGLISH: Locale = Locale.ENGLISH

    fun isNorwegian(context: Context): Boolean {
        // The phone's own setting, not this app's (possibly already overridden) resources.
        val phoneLocale = Resources.getSystem().configuration.locales[0]
        // Must use the application context: in an activity's attachBaseContext the activity itself is not
        // attached yet, and creating TelephonyManager from it crashes (NPE in ContextWrapper).
        val telephony = context.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        // Country of the mobile network / SIM. Needs no permission; empty on Wi-Fi-only devices.
        val (networkCountry, simCountry) = try {
            telephony?.networkCountryIso to telephony?.simCountryIso
        } catch (e: RuntimeException) {
            null to null
        }
        return shouldUseNorwegian(phoneLocale.language, phoneLocale.country, networkCountry, simCountry)
    }

    /** The rule itself, kept free of Android calls so it can be unit tested. */
    fun shouldUseNorwegian(
        phoneLanguage: String,
        phoneRegion: String,
        networkCountry: String?,
        simCountry: String?
    ): Boolean {
        if (phoneLanguage.lowercase() in NORWEGIAN_LANGUAGES) return true
        return listOf(phoneRegion, networkCountry, simCountry).any { it.equals(NORWAY, ignoreCase = true) }
    }

    fun locale(context: Context): Locale = if (isNorwegian(context)) NORWEGIAN else ENGLISH

    /** A context whose resources use Knappen's language. Use it for every `getString` and layout inflation. */
    fun localized(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale(context))
        return context.createConfigurationContext(config)
    }
}
