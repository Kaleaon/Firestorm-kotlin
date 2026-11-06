package com.firestorm.newview

class LLFloaterInventorySettings private constructor(key: Any) : LLFloater(key) {

    override fun postBuild(): Boolean {
        getChild<LLButton>("ok_btn").setCommitCallback { closeFloater(false) }

        getChild<LLUICtrl>("favorites_color").setCommitCallback { updateColorSwatch() }

        val enableColor = gSavedSettings.getBool("InventoryFavoritesColorText")
        getChild<LLUICtrl>("favorites_swatch").setEnabled(enableColor)

        return true
    }

    private fun updateColorSwatch() {
        val value = getChild<LLUICtrl>("favorites_color").getValue() as? Boolean ?: false
        getChild<LLUICtrl>("favorites_swatch").setEnabled(value)
    }

    private fun applyUIColor(ctrl: LLUICtrl, param: String) {
        LLUIColorTable.instance().setColor(param, LLColor4(ctrl.getValue()))
    }

    private fun getUIColor(ctrl: LLUICtrl, param: String) {
        val colorSwatch = ctrl as LLColorSwatchCtrl
        colorSwatch.setOriginal(LLUIColorTable.instance().getColor(param))
    }
}
