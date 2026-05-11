package com.firestorm.newview

import java.util.UUID

// Stub UI types matching the C++ hierarchy.
open class LLView
open class LLContextMenu : LLView() {
    open fun die(): Unit = TODO("GPU: LLContextMenu.die")
    open fun hide(): Unit = TODO("GPU: LLContextMenu.hide")
    open fun show(x: Int, y: Int): Unit = TODO("GPU: LLContextMenu.show")
    open fun getHandle(): LLHandle<LLContextMenu> = TODO("GPU: LLContextMenu.getHandle")
}

class LLHandle<T> {
    private var dead = false
    private var ref: T? = null

    fun get(): T? = if (dead) null else ref
    fun isDead(): Boolean = dead
    fun markDead() { dead = true }

    companion object {
        fun <T> of(value: T): LLHandle<T> {
            val h = LLHandle<T>()
            h.ref = value
            return h
        }
    }
}

/**
 * Context menu for single or multiple list items.
 *
 * Subclasses must implement [createMenu].
 *
 * Typical use: `myContextMenu.show(parentView, selectedIds, x, y)`
 */
abstract class LLListContextMenu {

    protected val mUUIDs: MutableList<UUID> = mutableListOf()
    protected var mMenuHandle: LLHandle<LLContextMenu> = LLHandle()

    open fun show(spawningView: LLView, uuids: List<UUID>, x: Int, y: Int) {
        val existing = mMenuHandle.get()
        if (existing != null) {
            existing.die()
            mMenuHandle.markDead()
            mUUIDs.clear()
        }

        if (uuids.isEmpty()) return

        mUUIDs.addAll(uuids)

        val menu = createMenu()
        if (menu == null) {
            return
        }

        mMenuHandle = menu.getHandle()
        menu.show(x, y)
        TODO("GPU: LLMenuGL.showPopup(spawningView, menu, x, y)")
    }

    open fun hide() {
        mMenuHandle.get()?.hide()
    }

    protected abstract fun createMenu(): LLContextMenu?

    companion object {
        fun createFromFile(filename: String): LLContextMenu =
            TODO("APR: use JVM equivalent for LLUICtrlFactory::createFromFile")

        fun handleMultiple(functor: (UUID) -> Unit, ids: List<UUID>) {
            ids.forEach { functor(it) }
        }
    }
}
