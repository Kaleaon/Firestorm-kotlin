package com.firestorm.llui

import com.firestorm.llmath.Color4
import com.firestorm.llmath.Rect

open class Panel(
    name: String,
    rect: Rect = Rect()
) : View(name, rect) {

    var label: String = ""
    var backgroundVisible: Boolean = false
    var backgroundColor: UIColor = UIColor(Color4.TRANSPARENT)
    var tabStop: Boolean = true

    open fun notify(action: Map<String, Any?>): Int = 0
    open fun setValue(event: Map<String, Any?>) {}

    override fun draw() {
        if (!visible) return
        if (backgroundVisible) {
            // stub: fill rect with backgroundColor.get()
        }
        for (child in children) {
            child.draw()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : View> getChild(name: String): T? =
        getChildByName(name, recurse = true) as? T

    fun loadFromXML(filename: String): Boolean = false

    open fun postBuild(): Boolean = true
}
