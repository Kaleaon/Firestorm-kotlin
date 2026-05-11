package com.firestorm.newview

var gHUDView: LLHUDView? = null

class LLHUDView(rect: LLRect) : LLPanel() {

    init {
        TODO("APR: use JVM equivalent - buildFromFile(\"panel_hud.xml\")")
        setShape(rect, true)
    }

    override fun draw() {
        TODO("APR: use JVM equivalent - LLTracker.drawHUDArrow()")
        super.draw()
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (TODO<Boolean>("APR: use JVM equivalent - LLTracker.handleMouseDown(x, y)")) {
            return true
        }
        return super.handleMouseDown(x, y, mask)
    }
}
