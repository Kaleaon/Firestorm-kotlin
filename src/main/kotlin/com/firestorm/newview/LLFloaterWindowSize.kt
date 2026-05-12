package com.firestorm.newview

fun extractWindowSizeFromString(instr: String): Pair<UInt, UInt>? {
    val match = Regex("([0-9]+)[^0-9]+([0-9]+)").find(instr) ?: return null
    val width = match.groupValues[1].toUIntOrNull() ?: return null
    val height = match.groupValues[2].toUIntOrNull() ?: return null
    return Pair(width, height)
}

class LLFloaterWindowSize private constructor(key: LLSD) : LLFloater(key) {

    override fun postBuild(): Boolean {
        center()
        initWindowSizeControls()
        getChild<LLUICtrl>("set_btn").setCommitCallback { onClickSet() }
        getChild<LLUICtrl>("cancel_btn").setCommitCallback { onClickCancel() }
        setDefaultBtn("set_btn")
        return true
    }

    fun initWindowSizeControls() {
        val ctrlWindowSize = getChild<LLComboBox>("window_size_combo")

        val height = gViewerWindow.getWindowHeightRaw()
        val width = gViewerWindow.getWindowWidthRaw()

        for (i in 0 until ctrlWindowSize.getItemCount()) {
            ctrlWindowSize.setCurrentByIndex(i)
            val resolution = ctrlWindowSize.getValue().asString()
            val size = extractWindowSizeFromString(resolution)
            if (size != null && size.first == width && size.second == height) {
                return
            }
        }

        // Current window size is not in the preset list; add it so the user sees what is active.
        val resolutionLabel = getString("resolution_format")
            .replace("[RES_X]", width.toString())
            .replace("[RES_Y]", height.toString())
        ctrlWindowSize.add(resolutionLabel, AddPosition.ADD_TOP)
        ctrlWindowSize.setCurrentByIndex(0)
    }

    fun onClickSet() {
        val ctrlWindowSize = getChild<LLComboBox>("window_size_combo")
        val resolution = ctrlWindowSize.getValue().asString()
        val size = extractWindowSizeFromString(resolution)
        if (size != null) {
            val (width, height) = size
            gSavedSettings.setS32("WindowWidth", width.toInt())
            gSavedSettings.setS32("WindowHeight", height.toInt())
            LLViewerWindow.movieSize(width, height)
        }
        closeFloater()
    }

    fun onClickCancel() {
        closeFloater()
    }
}
