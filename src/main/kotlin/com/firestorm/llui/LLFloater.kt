package com.firestorm.llui

import com.firestorm.llmath.Rect

open class Floater(
    name: String,
    rect: Rect = Rect()
) : Panel(name, rect) {

    var title: String = ""
    var resizable: Boolean = true
    var minimizable: Boolean = true
    var closeable: Boolean = true
    var isMinimized: Boolean = false

    fun open_() {
        visible = true
        isMinimized = false
        FloaterRegistry.floaters[name] = this
    }

    fun close(app_quitting: Boolean = false) {
        if (!onClose(app_quitting)) return
        visible = false
        FloaterRegistry.floaters.remove(name)
    }

    fun minimize() {
        if (!minimizable) return
        isMinimized = true
    }

    fun restore() {
        isMinimized = false
        visible = true
    }

    open fun onClose(app_quitting: Boolean): Boolean = true

    override fun draw() {
        if (!visible || isMinimized) return
        // stub: draw title bar with close/minimize/restore buttons, then panel content
        super.draw()
    }

    object FloaterRegistry {
        val floaters: MutableMap<String, Floater> = mutableMapOf()

        fun show(name: String): Floater? {
            val f = floaters[name]
            f?.open_()
            return f
        }

        fun hide(name: String) {
            floaters[name]?.close()
        }

        fun toggle(name: String) {
            val f = floaters[name] ?: return
            if (f.visible) f.close() else f.open_()
        }
    }
}
