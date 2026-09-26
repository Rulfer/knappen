package com.bardsplayground.knappen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {

    private fun norwegian(language: String, region: String = "", network: String? = "", sim: String? = "") =
        AppLanguage.shouldUseNorwegian(language, region, network, sim)

    @Test
    fun norwegianPhoneLanguage_anywhere() {
        assertTrue(norwegian("nb", "NO"))
        assertTrue(norwegian("nn", "NO"))
        assertTrue(norwegian("no"))
        assertTrue(norwegian("nb", "SE", network = "se", sim = "se")) // Norwegian abroad
    }

    @Test
    fun otherLanguage_inNorway() {
        assertTrue(norwegian("en", "US", network = "no")) // tourist on a Norwegian network
        assertTrue(norwegian("en", "US", network = "", sim = "no")) // Norwegian SIM, no network (e.g. flight mode)
        assertTrue(norwegian("en", "NO")) // English with region Norway
        assertTrue(norwegian("de", "DE", network = "NO")) // case-insensitive
    }

    @Test
    fun otherLanguage_outsideNorway_isEnglish() {
        assertFalse(norwegian("en", "US", network = "us", sim = "us"))
        assertFalse(norwegian("sv", "SE", network = "se", sim = "se"))
        assertFalse(norwegian("en", "GB", network = null, sim = null)) // Wi-Fi-only tablet
    }
}
