package com.firestorm.llmessage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageTemplateCatalogTest {

    @Test
    fun sizeMatchesExpectedTotal() {
        // 18 deprecated + all active entries
        assertEquals(18, MessageTemplateCatalog.deprecatedNames.size)
        assertTrue(MessageTemplateCatalog.activeNames.isNotEmpty())
        assertEquals(
            MessageTemplateCatalog.activeNames.size + MessageTemplateCatalog.deprecatedNames.size,
            MessageTemplateCatalog.size
        )
    }

    @Test
    fun allNamesEqualsActivePlusDeprecated() {
        assertEquals(
            MessageTemplateCatalog.activeNames + MessageTemplateCatalog.deprecatedNames,
            MessageTemplateCatalog.allNames
        )
    }

    @Test
    fun activeAndDeprecatedNamesAreDisjoint() {
        assertTrue(
            MessageTemplateCatalog.activeNames.intersect(MessageTemplateCatalog.deprecatedNames).isEmpty()
        )
    }

    @Test
    fun getByNameReturnsCorrectTemplate() {
        val tmpl = MessageTemplateCatalog["AgentUpdate"]
        assertNotNull(tmpl)
        assertEquals("AgentUpdate", tmpl.name)
        assertEquals(Deprecation.NOT_DEPRECATED, tmpl.deprecation)
    }

    @Test
    fun getByNameReturnsNullForUnknown() {
        assertNull(MessageTemplateCatalog["NotARealMessage"])
    }

    @Test
    fun containsReturnsTrueForKnownMessage() {
        assertTrue(MessageTemplateCatalog.contains("ChatFromSimulator"))
        assertTrue(MessageTemplateCatalog.contains("ImprovedTerseObjectUpdate"))
        assertTrue(MessageTemplateCatalog.contains("UseCircuitCode"))
    }

    @Test
    fun containsReturnsFalseForUnknownMessage() {
        assertTrue(!MessageTemplateCatalog.contains("BogusMessage"))
        assertTrue(!MessageTemplateCatalog.contains(""))
    }

    @Test
    fun deprecatedNamesHaveCorrectDeprecationStatus() {
        for (name in MessageTemplateCatalog.deprecatedNames) {
            val tmpl = MessageTemplateCatalog[name]
            assertNotNull(tmpl, "Missing template for deprecated name: $name")
            assertTrue(
                tmpl.deprecation != Deprecation.NOT_DEPRECATED,
                "$name should be deprecated but has deprecation=${tmpl.deprecation}"
            )
        }
    }

    @Test
    fun activeNamesHaveNotDeprecatedStatus() {
        for (name in MessageTemplateCatalog.activeNames) {
            val tmpl = MessageTemplateCatalog[name]
            assertNotNull(tmpl, "Missing template for active name: $name")
            assertEquals(
                Deprecation.NOT_DEPRECATED, tmpl.deprecation,
                "$name should not be deprecated"
            )
        }
    }

    @Test
    fun spotCheckKnownDeprecatedMessages() {
        val deprecated = MessageTemplateCatalog.deprecatedNames
        assertTrue("AgentDropGroup" in deprecated)
        assertTrue("ViewerStats" in deprecated)
        assertTrue("ObjectPosition" in deprecated)
        assertTrue("ScriptRunningReply" in deprecated)
    }

    @Test
    fun spotCheckKnownActiveMessages() {
        val active = MessageTemplateCatalog.activeNames
        assertTrue("StartPingCheck" in active)
        assertTrue("CompletePingCheck" in active)
        assertTrue("AgentUpdate" in active)
        assertTrue("LogoutRequest" in active)
        assertTrue("ImprovedInstantMessage" in active)
        assertTrue("RegionHandshake" in active)
        assertTrue("TeleportFinish" in active)
    }
}
