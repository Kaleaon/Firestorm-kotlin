package com.firestorm.newview

abstract class VOInventoryListener {

    private var listenerVObject: ViewerObject? = null

    abstract fun inventoryChanged(
        obj: ViewerObject,
        inventory: MutableList<InventoryObject>?,
        serialNum: Int,
        userData: Any?,
    )

    fun removeVOInventoryListener() {
        listenerVObject?.removeInventoryListener(this)
        listenerVObject = null
    }

    // Clears our reference without telling the object; caller guarantees the object
    // is already cleaning its own listener list.
    fun clearVOInventoryListener() {
        listenerVObject = null
    }

    protected fun registerVOInventoryListener(obj: ViewerObject?, userData: Any?) {
        removeVOInventoryListener()
        if (obj != null) {
            listenerVObject = obj
            obj.registerInventoryListener(this, userData)
        }
    }

    protected fun requestVOInventory() {
        listenerVObject?.requestInventory()
    }
}
