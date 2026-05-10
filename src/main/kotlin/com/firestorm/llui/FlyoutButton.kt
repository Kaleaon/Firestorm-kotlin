package com.firestorm.llui

import com.firestorm.llmath.Rect

private const val FLYOUT_BUTTON_ARROW_WIDTH = 24

class FlyoutButton(
    name: String,
    label: String = "",
    rect: Rect = Rect()
) : ComboBox(name, rect) {

    private val actionButton: Button
    private var toggleState: Boolean = false

    init {
        allowTextEntry = false

        val actionRect = Rect(
            left = 0,
            bottom = 0,
            right = rect.width - FLYOUT_BUTTON_ARROW_WIDTH,
            top = rect.height
        )
        actionButton = Button(label, actionRect).also { btn ->
            btn.label = label
            btn.clickCallback = { onActionButtonClick() }
            addChild(btn)
        }
    }

    private fun onActionButtonClick() {
        // Deselect the dropdown list and commit, mirroring C++ mList->deselect() + onCommit().
        clear()
        onChange?.invoke(this)
    }

    fun setToggleState(state: Boolean) {
        toggleState = state
    }

    override fun draw() {
        if (!visible) return
        actionButton.isToggled = toggleState
        // Clear the combo label so only the action button text is shown.
        setSimple("")
        super.draw()
    }
}
