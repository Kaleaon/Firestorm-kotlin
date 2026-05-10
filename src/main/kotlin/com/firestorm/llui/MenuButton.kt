package com.firestorm.llui

open class ToggleableMenu(name: String) : MenuGL(name) {
    private var buttonRect: Any? = null
    private val visibilityChangeCallbacks: MutableList<(Boolean, Boolean) -> Unit> = mutableListOf()

    fun setButtonRect(rect: Any?) { buttonRect = rect }

    fun toggleVisibility(): Boolean {
        val newVisible = !visible
        setVisible(newVisible)
        return newVisible
    }

    override fun setVisible(vis: Boolean) {
        val wasVisible = visible
        super.setVisible(vis)
        if (vis != wasVisible) {
            visibilityChangeCallbacks.forEach { it(vis, false) }
        }
    }

    fun setVisibilityChangeCallback(cb: (Boolean, Boolean) -> Unit) {
        visibilityChangeCallbacks.add(cb)
    }
}

enum class MenuPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

open class MenuButton(name: String) : Button(name) {
    private var menuHandle: ToggleableMenu? = null
    private var isMenuShown: Boolean = false
    private var menuPosition: MenuPosition = MenuPosition.BOTTOM_LEFT
    private var menuX: Int = 0
    private var menuY: Int = 0
    private var ownMenu: Boolean = false

    var rectLeft: Int = 0
    var rectRight: Int = 0
    var rectTop: Int = 0
    var rectBottom: Int = 0

    val mouseDownCallbacksMenu: MutableList<(Int, Int, UInt) -> Unit> = mutableListOf()

    fun addMouseDownCallback(cb: (Int, Int, UInt) -> Unit) { mouseDownCallbacksMenu.add(cb) }

    override fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        super.handleMouseDown(x, y, mask)
        toggleMenu()
        return true
    }

    override fun handleKeyHere(key: Int, mask: UInt): Boolean {
        val menu = getMenu() ?: return false
        if (key == KEY_RETURN && mask == 0u) {
            toggleMenu()
            return true
        }
        if (menu.isOpen() && key == KEY_ESCAPE && mask == 0u) {
            menu.setVisible(false)
            return true
        }
        return false
    }

    fun hideMenu() { getMenu()?.setVisible(false) }

    fun getMenu(): ToggleableMenu? = menuHandle

    fun setMenu(menuFilename: String, position: MenuPosition = MenuPosition.TOP_LEFT) {
        if (menuFilename.isEmpty()) return
        TODO("APR: load ToggleableMenu from file '$menuFilename'")
    }

    fun setMenu(menu: ToggleableMenu, position: MenuPosition = MenuPosition.TOP_LEFT, takeOwnership: Boolean = false) {
        cleanup()
        menuHandle = menu
        menuPosition = position
        ownMenu = takeOwnership
        menu.setVisibilityChangeCallback { visibility, closedByButtonClick ->
            onMenuVisibilityChange(visibility, closedByButtonClick)
        }
    }

    fun setMenuPosition(position: MenuPosition) { menuPosition = position }

    fun toggleMenu() {
        val menu = getMenu() ?: return

        menu.setButtonRect(this)

        if (!menu.toggleVisibility() && isMenuShown) {
            setForcePressedState(false)
            isMenuShown = false
        } else {
            menu.buildDrawLabels()
            menu.arrangeAndClear()
            menu.updateParent(MenuGL.sMenuContainer)

            updateMenuOrigin()

            MenuGL.showPopup(null, menu, menuX, menuY)

            setForcePressedState(true)
            isMenuShown = true
        }
    }

    fun updateMenuOrigin() {
        val menu = getMenu() ?: return

        when (menuPosition) {
            MenuPosition.TOP_LEFT -> {
                menuX = rectLeft
                menuY = rectTop + menu.getMenuHeight()
            }
            MenuPosition.TOP_RIGHT -> {
                menuX = rectRight - menu.getMenuWidth()
                menuY = rectTop + menu.getMenuHeight()
            }
            MenuPosition.BOTTOM_LEFT -> {
                menuX = rectLeft
                menuY = rectBottom
            }
            MenuPosition.BOTTOM_RIGHT -> {
                menuX = rectRight - menu.getMenuWidth()
                menuY = rectBottom
            }
        }
    }

    fun onMenuVisibilityChange(newVisibility: Boolean, closedByButtonClick: Boolean) {
        if (!newVisibility && !closedByButtonClick && isMenuShown) {
            setForcePressedState(false)
            isMenuShown = false
        }
    }

    private fun cleanup() {
        if (ownMenu) {
            menuHandle = null
        }
    }

    companion object {
        private const val KEY_RETURN: Int = 0x0D
        private const val KEY_ESCAPE: Int = 0x1B
    }
}

private fun MenuGL.getMenuHeight(): Int { TODO("GPU: get menu height") }
private fun MenuGL.getMenuWidth(): Int { TODO("GPU: get menu width") }
