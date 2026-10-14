package com.firestorm.i18n

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LLTransTest {

    @Before
    fun setUp() {
        LLTrans.setLocale("en")
    }

    @Test
    fun testValidKeyLookup() {
        val result = LLTrans.getString("LoadingData")
        assertEquals("Loading data...", result)
    }

    @Test
    fun testLanguageSwitching() {
        LLTrans.setLocale("es")
        val spanishResult = LLTrans.getString("LoadingData")
        assertEquals("Cargando datos...", spanishResult)

        LLTrans.setLocale("en")
        val englishResult = LLTrans.getString("LoadingData")
        assertEquals("Loading data...", englishResult)
    }

    @Test
    fun testMissingKeyFallbackToEnglish() {
        LLTrans.loadTranslations("en", mapOf("EnglishOnlyKey" to "English Only Text"))
        LLTrans.setLocale("es")
        val result = LLTrans.getString("EnglishOnlyKey")
        assertEquals("English Only Text", result)
    }

    @Test
    fun testMissingKeyFallbackToKey() {
        val missingKey = "non.existent.translation.key"
        val result = LLTrans.getString(missingKey)
        assertEquals(missingKey, result)
        assertNotEquals("", result)
    }

    @Test
    fun testMapArgumentInterpolation() {
        LLTrans.loadTranslations("en", mapOf("WelcomeMsg" to "Welcome [USER] to [LOCATION]"))
        val args = mapOf("USER" to "Alice", "LOCATION" to "Main Grid")
        val result = LLTrans.getString("WelcomeMsg", args)
        assertEquals("Welcome Alice to Main Grid", result)
    }

    @Test
    fun testVarargArgumentFormatting() {
        LLTrans.loadTranslations("en", mapOf("CountMsg" to "User %s has %d unread messages"))
        val result = LLTrans.getString("CountMsg", "Bob", 4)
        assertEquals("User Bob has 4 unread messages", result)
    }

    @Test
    fun testLoadCustomTranslations() {
        LLTrans.loadTranslations("fr", mapOf("LoadingData" to "Chargement des données..."))
        LLTrans.setLocale("fr")
        val result = LLTrans.getString("LoadingData")
        assertEquals("Chargement des données...", result)
    }
}
