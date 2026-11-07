package com.firestorm.newview

class LLFloaterScriptEdPrefs(val key: Any) : LLFloater(key) {

    private var mEditor: LLScriptEditor? = null

    init {
        mCommitCallbackRegistrar.add("ScriptPref.applyUIColor") { ctrl, param -> applyUIColor(ctrl, param) }
        mCommitCallbackRegistrar.add("ScriptPref.getUIColor") { ctrl, param -> getUIColor(ctrl, param) }
        mCommitCallbackRegistrar.add("NACL.SetPreprocInclude") { _, _ -> setPreprocInclude() }
    }

    override fun postBuild(): Boolean {
        mEditor = getChild<LLScriptEditor>("Script Preview")
        mEditor?.let {
            it.initKeywords()
            it.loadKeywords()
        }

        getChild<LLButton>("close_btn").setClickedCallback { closeFloater(false) }

        return true
    }

    private fun applyUIColor(ctrl: LLUICtrl, param: Any?) {
        val colorName = param?.toString() ?: return
        LLUIColorTable.instance().setColor(colorName, LLColor4(ctrl.getValue()))

        val floaters = LLFloaterReg.getFloaterList("preview_script")
        for (floater in floaters) {
            val cont = floater as? LLScriptEdContainer ?: continue
            cont.updateStyle()
        }
    }

    private fun getUIColor(ctrl: LLUICtrl, param: Any?) {
        val colorName = param?.toString() ?: return
        val colorSwatch = ctrl as? LLColorSwatchCtrl ?: return
        colorSwatch.setOriginal(LLUIColorTable.instance().getColor(colorName))
    }

    private fun setPreprocInclude() {
        val curName = gSavedSettings.getString("_NACL_PreProcHDDIncludeLocation")
        System.err.println("LLFloaterScriptedPrefs: setPreprocInclude not yet implemented")
    }

    private fun changePreprocIncludePath(filenames: MutableList<String>, proposedName: String) {
        val dirName = filenames.firstOrNull() ?: return
        if (dirName.isNotEmpty() && dirName != proposedName) {
            gSavedSettings.setString("_NACL_PreProcHDDIncludeLocation", dirName)
        }
    }
}
