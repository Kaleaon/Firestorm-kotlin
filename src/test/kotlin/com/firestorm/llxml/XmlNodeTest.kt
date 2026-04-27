package com.firestorm.llxml

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class XmlNodeTest {
    @Test
    fun typedVectorAndColorParsingHandlesValidAndInvalidInput() {
        val node = XmlNode("Example")
        node.value = "1.0 2.0 3.0 4.0"

        val color = node.getValueAsColor4()
        assertEquals(1.0f, color?.r)
        assertEquals(2.0f, color?.g)
        assertEquals(3.0f, color?.b)
        assertEquals(4.0f, color?.a)

        val vector = XmlNode("Vector").also { it.value = "5 6 7" }.getValueAsVector3()
        assertEquals(5.0f, vector?.x)
        assertEquals(6.0f, vector?.y)
        assertEquals(7.0f, vector?.z)

        val invalidColor = XmlNode("BadColor").also { it.value = "red green blue alpha" }
        assertNull(invalidColor.getValueAsColor4())
    }

    @Test
    fun attributeTupleParsingReturnsNullForInvalidTuples() {
        val node = XmlNode("Shape")
        node.setAttribute("goodVec", "8 9 10")
        node.setAttribute("badVec", "8 9 nope")
        node.setAttribute("shortColor", "0.1 0.2 0.3")

        assertEquals(8.0f, node.getAttributeVector3("goodVec")?.x)
        assertNull(node.getAttributeVector3("badVec"))
        assertNull(node.getAttributeColor4("shortColor"))
    }
}
