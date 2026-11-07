package com.firestorm.llxml

import kotlin.test.Test
import kotlin.test.assertEquals

class ControlGroupTest {
    @Test
    fun loadFromXmlParsesTupleValuesFromChildNodes() {
        val group = ControlGroup("settings")
        group.declareVec3("CameraOffset", com.firestorm.llmath.Vector3(), "camera offset")

        val root = XmlNode("llsd")
        val cameraOffset = XmlNode("CameraOffset")
        cameraOffset.addChild(XmlNode("Type").also { it.value = "Vector3" })
        cameraOffset.addChild(XmlNode("Value").also { valueNode ->
            valueNode.addChild(XmlNode("x").also { it.value = "1.5" })
            valueNode.addChild(XmlNode("y").also { it.value = "2.5" })
            valueNode.addChild(XmlNode("z").also { it.value = "3.5" })
        })
        root.addChild(cameraOffset)

        val loaded = group.loadFromXml(root)
        assertEquals(1, loaded)

        val parsed = group.getVector3("CameraOffset")
        assertEquals(1.5f, parsed.x)
        assertEquals(2.5f, parsed.y)
        assertEquals(3.5f, parsed.z)
    }
}
