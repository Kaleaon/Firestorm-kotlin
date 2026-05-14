package com.firestorm.newview

var gHUDView: LLHUDView? = null

class LLHUDView(rect: LLRect) : LLPanel() {

    init {
        System.err.println("LLHUDView: init not yet implemented")
        setShape(rect, true)
    }

    override fun draw() {
        System.err.println("LLHUDView: draw not yet implemented")
        super.draw()
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (false) {
            return true
        }
        return super.handleMouseDown(x, y, mask)
    }
}
