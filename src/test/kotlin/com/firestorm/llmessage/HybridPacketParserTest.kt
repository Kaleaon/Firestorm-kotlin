package com.firestorm.llmessage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HybridPacketParserTest {

    @Test
    fun testStaticFastPathCatalogLookup() {
        val tmpl = MessageTemplateCatalog["AgentUpdate"]
        assertNotNull(tmpl, "Fast-path lookup for AgentUpdate should succeed")
        assertEquals("AgentUpdate", tmpl.name)
    }

    @Test
    fun testDynamicTemplateParserAndRegistration() {
        val templateText = """
            {
                DynamicNewWireMsg Low 777 NotTrusted Zerocoded
                {
                    PayloadBlock Single
                    {   Val1   U32 }
                    {   Val2   LLUUID }
                }
            }
        """.trimIndent()

        MessageTemplateCatalog.loadFromText(templateText)

        val tmpl = MessageTemplateCatalog["DynamicNewWireMsg"]
        assertNotNull(tmpl, "Dynamically parsed template should be in catalog")
        assertEquals("DynamicNewWireMsg", tmpl.name)
        assertEquals(777u, tmpl.messageNumber)
        assertEquals(Encoding.ZEROCODED, tmpl.encoding)

        val block = tmpl.getBlock("PayloadBlock")
        assertNotNull(block, "PayloadBlock should exist")
        assertEquals(2, block.variables.size)
        assertEquals("Val1", block.variables[0].name)
        assertEquals(VarType.U32, block.variables[0].type)
        assertEquals("Val2", block.variables[1].name)
        assertEquals(VarType.LLUUID, block.variables[1].type)
    }

    @Test
    fun testThreadSafeDynamicCacheCapAt1000() {
        for (i in 1..1200) {
            val tmpl = MessageTemplate(
                name = "DynMsg_$i",
                frequency = Frequency.LOW,
                messageNumber = i.toUInt()
            )
            MessageTemplateCatalog.addTemplate(tmpl)
        }

        assertTrue(
            MessageTemplateCatalog.cacheSize <= 1000,
            "Dynamic cache size must be capped at 1000 active entries"
        )
    }
}
