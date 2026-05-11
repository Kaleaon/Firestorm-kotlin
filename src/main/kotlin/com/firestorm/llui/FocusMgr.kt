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

    open fun onFocusReceivedInternal() = onFocusReceived()
    open fun onFocusLostInternal() = onFocusLost()

    abstract fun getFocusParent(): FocusableElement?
    open fun isFocusRoot(): Boolean = false
    open fun hasAccelerators(): Boolean = false
    open fun hasAncestor(ancestor: FocusableElement): Boolean {
        var p = getFocusParent()
        while (p != null) {
            if (p === ancestor) return true
            p = p.getFocusParent()
        }
        return false
    }
}

abstract class MouseHandler {
    abstract fun getName(): String
    abstract fun onMouseCaptureLost()
}

class FocusMgr {
    private var lockedView: FocusableElement? = null
    private var mouseCaptor: MouseHandler? = null
    private var keyboardFocus: FocusableElement? = null
    private var lastKeyboardFocus: FocusableElement? = null
    private var defaultKeyboardFocus: FocusableElement? = null
    private var keystrokesOnly: Boolean = false
    private var topCtrl: FocusableElement? = null
    private var appHasFocus: Boolean = true

    private var focusFlashStartTime: Long = System.nanoTime()

    private val cachedKeyboardFocusList: ArrayDeque<FocusableElement> = ArrayDeque()
    private val focusHistory: MutableMap<FocusableElement, FocusableElement?> = mutableMapOf()

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

    fun childHasMouseCapture(parent: FocusableElement): Boolean {
        var captor = mouseCaptor as? FocusableElement
        while (captor != null) {
            if (captor === parent) return true
            captor = captor.getFocusParent()
        }
        return false
    }

    fun setKeyboardFocus(newFocus: FocusableElement?, lock: Boolean = false, keystrokesOnly: Boolean = false) {
        this.keystrokesOnly = keystrokesOnly

        val locked = lockedView
        if (locked != null && newFocus !== locked) {
            if (newFocus == null || !newFocus.hasAncestor(locked)) {
                return
            }
        }

        if (newFocus !== keyboardFocus) {
            lastKeyboardFocus = keyboardFocus
            keyboardFocus = newFocus

            val oldBranch = cachedKeyboardFocusList.toList()
            val newBranch = mutableListOf<FocusableElement>()
            var v = newFocus
            while (v != null) {
                newBranch.add(v)
                v = v.getFocusParent()
            }

            val (trimmedNew, trimmedOld) = pruneCommonAncestors(newBranch, oldBranch)

            for (old in trimmedOld) {
                cachedKeyboardFocusList.removeFirst()
                old.onFocusLostInternal()
            }

            for (nw in trimmedNew.asReversed()) {
                cachedKeyboardFocusList.addFirst(nw)
                nw.onFocusReceivedInternal()
            }

            if (defaultKeyboardFocus != null && keyboardFocus == null) {
                defaultKeyboardFocus?.setFocus(true)
            }

            var subtree = newFocus
            var walker = newFocus
            while (walker != null) {
                if (walker.isFocusRoot()) subtree = walker
                walker = walker.getFocusParent()
            }
            if (subtree != null) {
                focusHistory[subtree] = newFocus
            }
        }

        if (lock) lockFocus()
    }

    fun getKeyboardFocus(): FocusableElement? = keyboardFocus

    fun getLastKeyboardFocus(): FocusableElement? = lastKeyboardFocus

    fun childHasKeyboardFocus(parent: FocusableElement): Boolean {
        var focus = keyboardFocus
        while (focus != null) {
            if (focus === parent) return true
            focus = focus.getFocusParent()
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

    fun getFocusColor(): Color4 {
        TODO("GPU: look up FocusColor from UI color table, lerp toward white by getFocusFlashAmt(), apply alpha dim if !appHasFocus")
    }

    fun triggerFocusFlash() {
        focusFlashStartTime = System.nanoTime()
    }

    fun getAppHasFocus(): Boolean = appHasFocus

    fun setAppHasFocus(focus: Boolean) {
        if (!appHasFocus && focus) triggerFocusFlash()
        appHasFocus = focus
    }

    fun getLastFocusForGroup(subtreeRoot: FocusableElement): FocusableElement? =
        focusHistory[subtreeRoot]

    fun clearLastFocusForGroup(subtreeRoot: FocusableElement) {
        focusHistory.remove(subtreeRoot)
    }

    fun setDefaultKeyboardFocus(defaultFocus: FocusableElement?) {
        defaultKeyboardFocus = defaultFocus
    }

    fun getDefaultKeyboardFocus(): FocusableElement? = defaultKeyboardFocus

    fun setTopCtrl(newTop: FocusableElement?) {
        val oldTop = topCtrl
        if (newTop !== oldTop) {
            topCtrl = newTop
            oldTop?.onTopLost()
        }
    }

    fun getTopCtrl(): FocusableElement? = topCtrl

    fun removeTopCtrlWithoutCallback(topView: FocusableElement) {
        if (topCtrl === topView) topCtrl = null
    }

    fun childIsTopCtrl(parent: FocusableElement): Boolean {
        var top = topCtrl
        while (top != null) {
            if (top === parent) return true
            top = top.getFocusParent()
        }
        return false
    }

    fun releaseFocusIfNeeded(view: FocusableElement) {
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
        lockedView = keyboardFocus
    }

    fun unlockFocus() {
        lockedView = null
    }

    fun focusLocked(): Boolean = lockedView != null

    fun keyboardFocusHasAccelerators(): Boolean {
        var focus = keyboardFocus
        while (focus != null) {
            if (focus.hasAccelerators()) return true
            focus = focus.getFocusParent()
        }
        return false
    }

    private fun pruneCommonAncestors(
        newBranch: MutableList<FocusableElement>,
        oldBranch: List<FocusableElement>,
    ): Pair<MutableList<FocusableElement>, List<FocusableElement>> {
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
