package com.firestorm.newview

import com.firestorm.ui.LLFloater
import com.firestorm.ui.LLUICtrl
import com.firestorm.ui.LLColorSwatchCtrl
import com.firestorm.ui.LLScrollListCtrl
import com.firestorm.ui.LLScrollListItem
import com.firestorm.ui.LLSpinCtrl
import com.firestorm.ui.LLTextBox
import com.firestorm.control.LLUIColorTable
import com.firestorm.llsd.LLSD

class LLFloaterSettingsColor(key: LLSD) : LLFloater(key) {

    private var mSettingList: LLScrollListCtrl? = null

    protected var mDefaultButton: LLUICtrl? = null
    protected var mSettingNameText: LLTextBox? = null
    protected var mAlphaSpinner: LLSpinCtrl? = null
    protected var mColorSwatch: LLColorSwatchCtrl? = null

    protected var mSearchFilter: String = ""

    override fun postBuild(): Boolean {
        enableResizeCtrls(true, false, true)

        mAlphaSpinner = getChild("alpha_spinner")
        mColorSwatch = getChild("color_swatch")
        mDefaultButton = getChild("default_btn")
        mSettingNameText = getChild("color_name_txt")

        getChild<LLUICtrl>("filter_input").setCommitCallback { _, value ->
            setSearchFilter(value.asString())
        }

        mSettingList = getChild<LLScrollListCtrl>("setting_list").also { list ->
            list.setCommitOnSelectionChange(true)
            list.setCommitCallback { onSettingSelect() }
        }

        updateList()

        gSavedSettings.getControl("ColorSettingsHideDefault")
            ?.getCommitSignal()
            ?.add { updateList(skipSelection = false) }

        return super.postBuild()
    }

    override fun draw() {
        val firstSelected: LLScrollListItem? = mSettingList?.getFirstSelected()
        if (firstSelected != null) {
            val cell = firstSelected.getColumn(1)
            if (cell != null) {
                updateControl(cell.getValue().asString())
            }
        }
        super.draw()
    }

    fun onCommitSettings() {
        val firstSelected: LLScrollListItem = mSettingList?.getFirstSelected() ?: return
        val cell = firstSelected.getColumn(1) ?: return
        val colorName = cell.getValue().asString()
        if (colorName.isEmpty()) return

        val col3 = LLColor3()
        col3.setValue(mColorSwatch!!.getValue())
        val col4 = LLColor4(col3, mAlphaSpinner!!.getValue().asReal().toFloat())
        LLUIColorTable.instance().setColor(colorName, col4)
        updateDefaultColumn(colorName)
    }

    fun onClickDefault() {
        val firstSelected: LLScrollListItem = mSettingList?.getFirstSelected() ?: return
        val cell = firstSelected.getColumn(1) ?: return
        val name = cell.getValue().asString()
        LLUIColorTable.instance().resetToDefault(name)
        updateDefaultColumn(name)
        updateControl(name)
    }

    fun updateControl(colorName: String) {
        hideUIControls()
        if (!isSettingHidden(colorName)) {
            mDefaultButton?.setVisible(true)
            mSettingNameText?.setVisible(true)
            mSettingNameText?.setText(colorName)
            mSettingNameText?.setToolTip(colorName)

            val clr: LLColor4 = LLUIColorTable.instance().getColor(colorName)
            mColorSwatch?.setVisible(true)
            if (clr != LLColor4(mColorSwatch!!.getValue())) {
                mColorSwatch?.setOriginal(clr)
            }

            mAlphaSpinner?.setVisible(true)
            mAlphaSpinner?.setLabel("Alpha")
            if (mAlphaSpinner?.hasFocus() == false) {
                mAlphaSpinner?.setPrecision(3)
                mAlphaSpinner?.setMinValue(0.0)
                mAlphaSpinner?.setMaxValue(1.0f)
                mAlphaSpinner?.setValue(clr.mV[VALPHA])
            }
        }
    }

    fun matchesSearchFilter(settingName: String): Boolean {
        if (mSearchFilter.isEmpty()) return true
        return settingName.toLowerCase().contains(mSearchFilter)
    }

    fun isSettingHidden(colorName: String): Boolean {
        val hideDefault: Boolean = gSavedSettings.getBOOL("ColorSettingsHideDefault")
        return hideDefault && LLUIColorTable.instance().isDefault(colorName)
    }

    private fun updateList(skipSelection: Boolean = false) {
        var lastSelected = ""
        val item: LLScrollListItem? = mSettingList?.getFirstSelected()
        if (item != null) {
            val cell = item.getColumn(1)
            if (cell != null) {
                lastSelected = cell.getValue().asString()
            }
        }

        mSettingList?.deleteAllItems()

        val baseColors = LLUIColorTable.instance().getLoadedColors()
        for ((name, _) in baseColors) {
            if (matchesSearchFilter(name) && !isSettingHidden(name)) {
                val row = LLSD()
                row["columns"][0]["column"] = "changed_color"
                row["columns"][0]["value"] = if (LLUIColorTable.instance().isDefault(name)) "" else "*"
                row["columns"][1]["column"] = "color"
                row["columns"][1]["value"] = name

                val addedItem = mSettingList?.addElement(row, ADD_BOTTOM, null)
                if (!mSearchFilter.isEmpty() && lastSelected == name && !skipSelection) {
                    val lowerName = name.toLowerCase()
                    if (lowerName.startsWith(mSearchFilter)) {
                        addedItem?.setSelected(true)
                    }
                }
            }
        }

        for ((name, _) in LLUIColorTable.instance().getUserColors()) {
            if (!baseColors.containsKey(name) && matchesSearchFilter(name) && !isSettingHidden(name)) {
                val row = LLSD()
                row["columns"][0]["column"] = "changed_color"
                row["columns"][0]["value"] = if (LLUIColorTable.instance().isDefault(name)) "" else "*"
                row["columns"][1]["column"] = "color"
                row["columns"][1]["value"] = name

                val addedItem = mSettingList?.addElement(row, ADD_BOTTOM, null)
                if (!mSearchFilter.isEmpty() && lastSelected == name && !skipSelection) {
                    val lowerName = name.toLowerCase()
                    if (lowerName.startsWith(mSearchFilter)) {
                        addedItem?.setSelected(true)
                    }
                }
            }
        }

        mSettingList?.updateSort()

        if (mSettingList?.isEmpty() == false) {
            if (mSettingList?.hasSelectedItem() == true) {
                mSettingList?.scrollToShowSelected()
            } else if (!mSearchFilter.isEmpty() && !skipSelection) {
                if (mSettingList?.selectItemByPrefix(mSearchFilter, false, 1) == false) {
                    mSettingList?.selectFirstItem()
                }
                mSettingList?.scrollToShowSelected()
            }
        } else {
            val row = LLSD()
            row["columns"][0]["column"] = "changed_color"
            row["columns"][0]["value"] = ""
            row["columns"][1]["column"] = "color"
            row["columns"][1]["value"] = "No matching colors."
            mSettingList?.addElement(row)
            hideUIControls()
        }
    }

    private fun onSettingSelect() {
        val firstSelected: LLScrollListItem = mSettingList?.getFirstSelected() ?: return
        val cell = firstSelected.getColumn(1) ?: return
        updateControl(cell.getValue().asString())
    }

    private fun setSearchFilter(filter: String) {
        if (mSearchFilter == filter) return
        mSearchFilter = filter.toLowerCase()
        updateList()
    }

    private fun updateDefaultColumn(colorName: String) {
        if (isSettingHidden(colorName)) {
            hideUIControls()
            updateList(skipSelection = true)
            return
        }
        val item: LLScrollListItem = mSettingList?.getFirstSelected() ?: return
        val cell = item.getColumn(0) ?: return
        val isDefault = if (LLUIColorTable.instance().isDefault(colorName)) "" else "*"
        cell.setValue(isDefault)
    }

    private fun hideUIControls() {
        mColorSwatch?.setVisible(false)
        mAlphaSpinner?.setVisible(false)
        mDefaultButton?.setVisible(false)
        mSettingNameText?.setVisible(false)
    }
}
