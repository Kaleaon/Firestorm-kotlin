package com.firestorm.llui

import kotlin.math.roundToInt

private const val FOCUS_FADE_TIME = 0.3f

fun lerpFloat(a: Float, b: Float, t: Float): Float = a + t * (b - a)
fun clampRescale(value: Float, from0: Float, from1: Float, to0: Float, to1: Float): Float {
    val t = if (from1 == from0) 0f else ((value - from0) / (from1 - from0)).coerceIn(0f, 1f)
    return lerpFloat(to0, to1, t)
}

abstract class FocusableElement {
    var focusLostCallback: ((FocusableElement) -> Unit)? = null
    var focusReceivedCallback: ((FocusableElement) -> Unit)? = null
    var focusChangedCallback: ((FocusableElement) -> Unit)? = null
    var topLostCallback: ((FocusableElement) -> Unit)? = null

    open fun setFocus(b: Boolean) {}

    open fun hasFocus(): Boolean = gFocusMgr.getKeyboardFocus() === this

    open fun handleKey(key: Int, mask: Int, calledFromParent: Boolean): Boolean = false

    open fun handleKeyUp(key: Int, mask: Int, calledFromParent: Boolean): Boolean = false

    open fun handleUnicodeChar(uniChar: Char, calledFromParent: Boolean): Boolean = false

    open fun wantsKeyUpKeyDown(): Boolean = false

    open fun wantsReturnKey(): Boolean = false

    open fun onTopLost() {
        topLostCallback?.invoke(this)
    }

    protected open fun onFocusReceived() {
        focusReceivedCallback?.invoke(this)
        focusChangedCallback?.invoke(this)
    }

    protected open fun onFocusLost() {
        focusLostCallback?.invoke(this)
        focusChangedCallback?.invoke(this)
    }
}

abstract class View : FocusableElement() {
    abstract fun getParent(): View?
    abstract fun hasAncestor(ancestor: View): Boolean
    abstract fun isFocusRoot(): Boolean
    abstract fun hasAccelerators(): Boolean
    open fun onFocusReceivedPublic() = onFocusReceived()
    open fun onFocusLostPublic() = onFocusLost()
}

abstract class UiCtrl : View() {
    override fun onTopLost() {
        super.onTopLost()
    }
}

abstract class MouseHandler {
    abstract fun getName(): String
    abstract fun onMouseCaptureLost()
}

class FocusMgr {
    private var lockedView: UiCtrl? = null
    private var mouseCaptor: MouseHandler? = null
    private var keyboardFocus: FocusableElement? = null
    private var lastKeyboardFocus: FocusableElement? = null
    private var defaultKeyboardFocus: FocusableElement? = null
    private var keystrokesOnly: Boolean = false
    private var topCtrl: UiCtrl? = null
    private var appHasFocus: Boolean = true

    private var focusFlashStartTime: Long = System.nanoTime()

    private val cachedKeyboardFocusList: ArrayDeque<View> = ArrayDeque()
    private val focusHistory: MutableMap<View, View?> = mutableMapOf()

    fun setMouseCapture(newCaptor: MouseHandler?) {
        if (newCaptor !== mouseCaptor) {
            val oldCaptor = mouseCaptor
            mouseCaptor = newCaptor
            oldCaptor?.onMouseCaptureLost()
        }
    }

    fun getMouseCapture(): MouseHandler? = mouseCaptor

    fun removeMouseCaptureWithoutCallback(captor: MouseHandler) {
        if (mouseCaptor === captor) mouseCaptor = null
    }

    fun childHasMouseCapture(parent: View): Boolean {
        var captorView = mouseCaptor as? View
        while (captorView != null) {
            if (captorView === parent) return true
            captorView = captorView.getParent()
        }
        return false
    }

    fun setKeyboardFocus(newFocus: FocusableElement?, lock: Boolean = false, keystrokesOnly: Boolean = false) {
        this.keystrokesOnly = keystrokesOnly

        val locked = lockedView
        if (locked != null && newFocus !== locked) {
            val focusView = newFocus as? View
            if (focusView == null || !focusView.hasAncestor(locked)) {
                return
            }
        }

        if (newFocus !== keyboardFocus) {
            lastKeyboardFocus = keyboardFocus
            keyboardFocus = newFocus

            val oldFocusBranch = cachedKeyboardFocusList.toList()
            val newFocusBranch = mutableListOf<View>()
            var v = newFocus as? View
            while (v != null) {
                newFocusBranch.add(v)
                v = v.getParent()
            }

            val (trimmedNew, trimmedOld) = pruneCommonAncestors(newFocusBranch, oldFocusBranch)

            for (oldView in trimmedOld) {
                cachedKeyboardFocusList.removeFirst()
                oldView.onFocusLostPublic()
            }

            for (newView in trimmedNew.asReversed()) {
                cachedKeyboardFocusList.addFirst(newView)
                newView.onFocusReceivedPublic()
            }

            if (defaultKeyboardFocus != null && keyboardFocus == null) {
                defaultKeyboardFocus?.setFocus(true)
            }

            val focusView = newFocus as? View
            var subtree = focusView
            var walker = focusView
            while (walker != null) {
                if (walker.isFocusRoot()) subtree = walker
                walker = walker.getParent()
            }
            if (subtree != null) {
                focusHistory[subtree] = focusView
            }
        }

        if (lock) lockFocus()
    }

    fun getKeyboardFocus(): FocusableElement? = keyboardFocus

    fun getLastKeyboardFocus(): FocusableElement? = lastKeyboardFocus

    fun childHasKeyboardFocus(parent: View): Boolean {
        var focusView = keyboardFocus as? View
        while (focusView != null) {
            if (focusView === parent) return true
            focusView = focusView.getParent()
        }
        return false
    }

    fun removeKeyboardFocusWithoutCallback(focus: FocusableElement) {
        if (focus === lockedView) lockedView = null
        if (keyboardFocus === focus) keyboardFocus = null
    }

    fun getKeystrokesOnly(): Boolean = keystrokesOnly
    fun setKeystrokesOnly(v: Boolean) { keystrokesOnly = v }

    fun getFocusFlashAmt(): Float {
        val elapsed = (System.nanoTime() - focusFlashStartTime) / 1_000_000_000f
        return clampRescale(elapsed, 0f, FOCUS_FADE_TIME, 1f, 0f)
    }

    fun getFocusFlashWidth(): Int = lerpFloat(1f, 3f, getFocusFlashAmt()).roundToInt()

    fun getFocusColor(): FloatArray {
        TODO("GPU: look up FocusColor from UI color table and lerp toward white by getFocusFlashAmt()")
    }

    fun triggerFocusFlash() {
        focusFlashStartTime = System.nanoTime()
    }

    fun getAppHasFocus(): Boolean = appHasFocus

    fun setAppHasFocus(focus: Boolean) {
        if (!appHasFocus && focus) {
            triggerFocusFlash()
        }
        appHasFocus = focus
    }

    fun getLastFocusForGroup(subtreeRoot: View): View? = focusHistory[subtreeRoot]

    fun clearLastFocusForGroup(subtreeRoot: View) {
        focusHistory.remove(subtreeRoot)
    }

    fun setDefaultKeyboardFocus(defaultFocus: FocusableElement?) {
        defaultKeyboardFocus = defaultFocus
    }

    fun getDefaultKeyboardFocus(): FocusableElement? = defaultKeyboardFocus

    fun setTopCtrl(newTop: UiCtrl?) {
        val oldTop = topCtrl
        if (newTop !== oldTop) {
            topCtrl = newTop
            oldTop?.onTopLost()
        }
    }

    fun getTopCtrl(): UiCtrl? = topCtrl

    fun removeTopCtrlWithoutCallback(topView: UiCtrl) {
        if (topCtrl === topView) topCtrl = null
    }

    fun childIsTopCtrl(parent: View): Boolean {
        var topView = topCtrl as? View
        while (topView != null) {
            if (topView === parent) return true
            topView = topView.getParent()
        }
        return false
    }

    fun releaseFocusIfNeeded(view: View) {
        if (childHasMouseCapture(view)) setMouseCapture(null)

        if (childHasKeyboardFocus(view)) {
            if (view === lockedView) {
                lockedView = null
                setKeyboardFocus(null)
            } else {
                setKeyboardFocus(lockedView)
            }
        }
    }

    fun lockFocus() {
        lockedView = keyboardFocus as? UiCtrl
    }

    fun unlockFocus() {
        lockedView = null
    }

    fun focusLocked(): Boolean = lockedView != null

    fun keyboardFocusHasAccelerators(): Boolean {
        var focusView = keyboardFocus as? View
        while (focusView != null) {
            if (focusView.hasAccelerators()) return true
            focusView = focusView.getParent()
        }
        return false
    }

    private fun pruneCommonAncestors(
        newBranch: MutableList<View>,
        oldBranch: List<View>,
    ): Pair<MutableList<View>, List<View>> {
        val oldMutable = oldBranch.toMutableList()
        while (newBranch.isNotEmpty() && oldMutable.isNotEmpty() &&
            newBranch.last() === oldMutable.last()
        ) {
            newBranch.removeLast()
            oldMutable.removeLast()
        }
        return newBranch to oldMutable
    }
}

val gFocusMgr = FocusMgr()
