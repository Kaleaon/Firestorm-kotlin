package com.firestorm.newview

import com.firestorm.ui.LLFloater
import com.firestorm.ui.LLButton
import com.firestorm.ui.LLColorSwatchCtrl
import com.firestorm.ui.LLLineEditor
import com.firestorm.ui.LLRadioGroup
import com.firestorm.ui.LLScrollListCtrl
import com.firestorm.ui.LLSearchEditor
import com.firestorm.ui.LLSpinCtrl
import com.firestorm.ui.LLTextEditor
import com.firestorm.ui.LLUICtrl
import com.firestorm.control.LLControlVariable
import com.firestorm.control.eControlType
import com.firestorm.llsd.LLSD
import com.firestorm.math.LLColor3
import com.firestorm.math.LLColor4
import com.firestorm.math.LLQuaternion
import com.firestorm.math.LLRect
import com.firestorm.math.LLVector3
import com.firestorm.math.LLVector3d
import com.firestorm.floater.LLFloaterReg

open class LLFloaterSettingsDebug(key: LLSD) : LLFloater(key) {

    protected val mSettingsMap: MutableMap<String, LLControlVariable> = mutableMapOf()

    protected var mOldSearchTerm: String = "---"
    protected var mCurrentControlVariable: LLControlVariable? = null
    protected var mOldControlVariable: LLControlVariable? = null
    protected var mOldVisibility: Boolean = false

    protected var mSearchSettingsInput: LLSearchEditor? = null
    protected var mSettingsScrollList: LLScrollListCtrl? = null
    protected var mComment: LLTextEditor? = null
    protected var mSpinner1: LLSpinCtrl? = null
    protected var mSpinner2: LLSpinCtrl? = null
    protected var mSpinner3: LLSpinCtrl? = null
    protected var mSpinner4: LLSpinCtrl? = null
    protected var mColorSwatch: LLColorSwatchCtrl? = null
    protected var mValText: LLLineEditor? = null
    protected var mBooleanCombo: LLRadioGroup? = null
    protected var mCopyButton: LLButton? = null
    protected var mDefaultButton: LLButton? = null
    protected var mSanityButton: LLButton? = null

    override fun postBuild(): Boolean {
        mSearchSettingsInput = getChild("search_settings_input")
        mSettingsScrollList = getChild("settings_scroll_list")
        mComment = getChild("comment_text")
        mSpinner1 = getChild("val_spinner_1")
        mSpinner2 = getChild("val_spinner_2")
        mSpinner3 = getChild("val_spinner_3")
        mSpinner4 = getChild("val_spinner_4")
        mColorSwatch = getChild("val_color_swatch")
        mValText = getChild("val_text")
        mBooleanCombo = getChild("boolean_combo")
        mCopyButton = getChild("copy_btn")
        mDefaultButton = getChild("default_btn")
        mSanityButton = getChild("sanity_warning_btn")

        mSearchSettingsInput?.setFocus(true)
        mSearchSettingsInput?.setKeystrokeCallback { onUpdateFilter() }

        val key = getKey().asString()
        val applyFunctor = { name: String, control: LLControlVariable ->
            if (!control.isHiddenFromSettingsEditor()) {
                mSettingsMap[name] = control
            }
        }

        if (key == "all" || key == "base") {
            gSavedSettings.applyToAll(applyFunctor)
        }
        if (key == "all" || key == "account") {
            gSavedPerAccountSettings.applyToAll(applyFunctor)
        }

        gSavedSettings.getControl("DebugSettingsHideDefault")
            ?.getCommitSignal()
            ?.add { onUpdateFilter() }

        onUpdateFilter()
        mSettingsScrollList?.sortByColumnIndex(1, true)

        LLNotificationsUtil.add("DebugSettingsWarning")

        return true
    }

    open fun draw() {
        val current = mCurrentControlVariable
        if (current != null) {
            if (current.isHiddenFromSettingsEditor() != mOldVisibility) {
                updateControl()
            }
        }
        super.draw()
    }

    fun onUpdateFilter() {
        val hideDefault: Boolean = gSavedSettings.getBOOL("DebugSettingsHideDefault")
        val searchTerm: String = mSearchSettingsInput?.getValue()?.asString() ?: ""

        if (searchTerm == mOldSearchTerm) return
        mOldSearchTerm = searchTerm

        mSettingsScrollList?.deleteAllItems()

        for ((_, control) in mSettingsMap) {
            var addItem = false

            if (searchTerm.isEmpty()) {
                addItem = true
            } else {
                val itemValue = control.getName().toLowerCase()
                val itemComment = control.getComment().toLowerCase()
                val lowerSearch = searchTerm.toLowerCase()

                if (itemValue.contains(lowerSearch) || itemComment.contains(lowerSearch)) {
                    addItem = true
                }
            }

            if (addItem) {
                val settingName = control.getName()
                val isDefault = control.isDefault()

                if (hideDefault && isDefault) continue

                val item = LLSD()
                item["columns"][1]["column"] = "setting"
                item["columns"][1]["value"] = settingName
                if (!isDefault) {
                    item["columns"][0]["column"] = "changed_setting"
                    item["columns"][0]["value"] = "*"
                }
                mSettingsScrollList?.addElement(item, ADD_BOTTOM, control)
            }
        }

        if ((mSettingsScrollList?.getItemCount() ?: 0) > 0 && searchTerm.isNotEmpty()) {
            mSettingsScrollList?.sortByColumnIndex(1, true)
            mSettingsScrollList?.selectFirstItem()
        }

        onSettingSelect()
    }

    fun onSettingSelect() {
        mCurrentControlVariable = getControlVariable()

        if (mOldControlVariable !== mCurrentControlVariable) {
            mOldControlVariable?.getCommitSignal()?.remove { onSettingSelect() }
            mCurrentControlVariable?.getCommitSignal()?.add { onSettingSelect() }
            mOldControlVariable = mCurrentControlVariable
        }

        updateControl()
    }

    fun onCommitSettings() {
        val control = mCurrentControlVariable ?: return

        when (control.type()) {
            eControlType.TYPE_U32 -> control.set(mSpinner1!!.getValue())
            eControlType.TYPE_S32 -> control.set(mSpinner1!!.getValue())
            eControlType.TYPE_F32 -> control.set(LLSD(mSpinner1!!.getValue().asReal()))
            eControlType.TYPE_BOOLEAN -> control.set(mBooleanCombo!!.getValue())
            eControlType.TYPE_STRING -> control.set(LLSD(mValText!!.getValue().asString()))
            eControlType.TYPE_VEC3 -> {
                val vector = LLVector3()
                vector.mV[VX] = mSpinner1!!.getValue().asReal().toFloat()
                vector.mV[VY] = mSpinner2!!.getValue().asReal().toFloat()
                vector.mV[VZ] = mSpinner3!!.getValue().asReal().toFloat()
                control.set(vector.getValue())
            }
            eControlType.TYPE_VEC3D -> {
                val vectord = LLVector3d()
                vectord.mdV[VX] = mSpinner1!!.getValue().asReal()
                vectord.mdV[VY] = mSpinner2!!.getValue().asReal()
                vectord.mdV[VZ] = mSpinner3!!.getValue().asReal()
                control.set(vectord.getValue())
            }
            eControlType.TYPE_QUAT -> {
                val quat = LLQuaternion()
                quat.mQ[VX] = mSpinner1!!.getValue().asReal().toFloat()
                quat.mQ[VY] = mSpinner2!!.getValue().asReal().toFloat()
                quat.mQ[VZ] = mSpinner3!!.getValue().asReal().toFloat()
                quat.mQ[VS] = mSpinner4!!.getValue().asReal().toFloat()
                control.set(quat.getValue())
            }
            eControlType.TYPE_RECT -> {
                val rect = LLRect()
                rect.mLeft = mSpinner1!!.getValue().asInteger()
                rect.mRight = mSpinner2!!.getValue().asInteger()
                rect.mBottom = mSpinner3!!.getValue().asInteger()
                rect.mTop = mSpinner4!!.getValue().asInteger()
                control.set(rect.getValue())
            }
            eControlType.TYPE_COL4 -> {
                val col3 = LLColor3()
                col3.setValue(mColorSwatch!!.getValue())
                val col4 = LLColor4(col3, mSpinner4!!.getValue().asReal().toFloat())
                control.set(col4.getValue())
            }
            eControlType.TYPE_COL3 -> {
                control.set(mColorSwatch!!.getValue())
            }
            else -> Unit
        }

        if (!control.isSane()) {
            onSanityCheck()
        }
    }

    fun onClickDefault() {
        mCurrentControlVariable?.resetToDefault(true)
        updateControl()
    }

    fun onCopyToClipboard() {
        val control = mCurrentControlVariable ?: return
        getWindow()?.copyTextToClipboard(control.getName())
        LLNotificationsUtil.add("ControlNameCopiedToClipboard")
    }

    fun onSanityCheck() {
        SanityCheck.instance().onSanity(mCurrentControlVariable)
    }

    fun onClickSanityWarning() {
        SanityCheck.instance().onSanity(mCurrentControlVariable, true)
    }

    fun updateControl() {
        val s1 = mSpinner1
        val s2 = mSpinner2
        val s3 = mSpinner3
        val s4 = mSpinner4
        val cs = mColorSwatch
        if (s1 == null || s2 == null || s3 == null || s4 == null || cs == null) return

        s1.setVisible(false)
        s2.setVisible(false)
        s3.setVisible(false)
        s4.setVisible(false)
        cs.setVisible(false)
        mValText?.setVisible(false)
        mComment?.setText("")
        mBooleanCombo?.setEnabled(true)
        getChild<LLUICtrl>("TRUE").setEnabled(true)
        getChild<LLUICtrl>("FALSE").setEnabled(true)
        mDefaultButton?.setEnabled(false)
        mCopyButton?.setEnabled(false)
        mBooleanCombo?.setVisible(false)
        mSanityButton?.setVisible(false)

        val control = mCurrentControlVariable ?: return

        mOldVisibility = control.isHiddenFromSettingsEditor()
        val editable = !mOldVisibility
        s1.setEnabled(editable)
        s2.setEnabled(editable)
        s3.setEnabled(editable)
        s4.setEnabled(editable)
        cs.setEnabled(editable)
        mValText?.setEnabled(editable)
        mBooleanCombo?.setEnabled(editable)
        mDefaultButton?.setEnabled(editable)

        mCopyButton?.setEnabled(true)
        mSanityButton?.setVisible(!control.isSane())

        mComment?.setText("${control.getName()}: ${control.getComment()}")

        s1.setMaxValue(Float.MAX_VALUE)
        s2.setMaxValue(Float.MAX_VALUE)
        s3.setMaxValue(Float.MAX_VALUE)
        s4.setMaxValue(Float.MAX_VALUE)
        s1.setMinValue(-Float.MAX_VALUE)
        s2.setMinValue(-Float.MAX_VALUE)
        s3.setMinValue(-Float.MAX_VALUE)
        s4.setMinValue(-Float.MAX_VALUE)
        if (!s1.hasFocus()) s1.setIncrement(0.1f)
        if (!s2.hasFocus()) s2.setIncrement(0.1f)
        if (!s3.hasFocus()) s3.setIncrement(0.1f)
        if (!s4.hasFocus()) s4.setIncrement(0.1f)

        val sd = control.get()
        when (control.type()) {
            eControlType.TYPE_U32 -> {
                s1.setVisible(true)
                s1.setLabel("value")
                if (!s1.hasFocus()) {
                    s1.setValue(sd)
                    s1.setMinValue(UInt.MIN_VALUE.toFloat())
                    s1.setMaxValue(UInt.MAX_VALUE.toFloat())
                    s1.setIncrement(1.0f)
                    s1.setPrecision(0)
                }
            }
            eControlType.TYPE_S32 -> {
                s1.setVisible(true)
                s1.setLabel("value")
                if (!s1.hasFocus()) {
                    s1.setValue(sd)
                    s1.setMinValue(Int.MIN_VALUE.toFloat())
                    s1.setMaxValue(Int.MAX_VALUE.toFloat())
                    s1.setIncrement(1.0f)
                    s1.setPrecision(0)
                }
            }
            eControlType.TYPE_F32 -> {
                s1.setVisible(true)
                s1.setLabel("value")
                if (!s1.hasFocus()) {
                    s1.setPrecision(3)
                    s1.setValue(sd)
                }
            }
            eControlType.TYPE_BOOLEAN -> {
                mBooleanCombo?.setVisible(true)
                if (mBooleanCombo?.hasFocus() == false) {
                    mBooleanCombo?.setValue(if (sd.asBoolean()) LLSD("true") else LLSD(""))
                }
            }
            eControlType.TYPE_STRING -> {
                mValText?.setVisible(true)
                if (mValText?.hasFocus() == false) {
                    mValText?.setValue(sd)
                }
            }
            eControlType.TYPE_VEC3 -> {
                val v = LLVector3()
                v.setValue(sd)
                s1.setVisible(true); s1.setLabel("X")
                s2.setVisible(true); s2.setLabel("Y")
                s3.setVisible(true); s3.setLabel("Z")
                if (!s1.hasFocus()) { s1.setPrecision(3); s1.setValue(v[VX]) }
                if (!s2.hasFocus()) { s2.setPrecision(3); s2.setValue(v[VY]) }
                if (!s3.hasFocus()) { s3.setPrecision(3); s3.setValue(v[VZ]) }
            }
            eControlType.TYPE_VEC3D -> {
                val v = LLVector3d()
                v.setValue(sd)
                s1.setVisible(true); s1.setLabel("X")
                s2.setVisible(true); s2.setLabel("Y")
                s3.setVisible(true); s3.setLabel("Z")
                if (!s1.hasFocus()) { s1.setPrecision(3); s1.setValue(v[VX]) }
                if (!s2.hasFocus()) { s2.setPrecision(3); s2.setValue(v[VY]) }
                if (!s3.hasFocus()) { s3.setPrecision(3); s3.setValue(v[VZ]) }
            }
            eControlType.TYPE_QUAT -> {
                val q = LLQuaternion()
                q.setValue(sd)
                s1.setVisible(true); s1.setLabel("X")
                s2.setVisible(true); s2.setLabel("Y")
                s3.setVisible(true); s3.setLabel("Z")
                s4.setVisible(true); s4.setLabel("S")
                if (!s1.hasFocus()) { s1.setPrecision(4); s1.setValue(q.mQ[VX]) }
                if (!s2.hasFocus()) { s2.setPrecision(4); s2.setValue(q.mQ[VY]) }
                if (!s3.hasFocus()) { s3.setPrecision(4); s3.setValue(q.mQ[VZ]) }
                if (!s4.hasFocus()) { s4.setPrecision(4); s4.setValue(q.mQ[VS]) }
            }
            eControlType.TYPE_RECT -> {
                val r = LLRect()
                r.setValue(sd)
                s1.setVisible(true); s1.setLabel("Left")
                s2.setVisible(true); s2.setLabel("Right")
                s3.setVisible(true); s3.setLabel("Bottom")
                s4.setVisible(true); s4.setLabel("Top")
                if (!s1.hasFocus()) { s1.setPrecision(0); s1.setValue(r.mLeft) }
                if (!s2.hasFocus()) { s2.setPrecision(0); s2.setValue(r.mRight) }
                if (!s3.hasFocus()) { s3.setPrecision(0); s3.setValue(r.mBottom) }
                if (!s4.hasFocus()) { s4.setPrecision(0); s4.setValue(r.mTop) }
                for (spinner in listOf(s1, s2, s3, s4)) {
                    spinner.setMinValue(Int.MIN_VALUE.toFloat())
                    spinner.setMaxValue(Int.MAX_VALUE.toFloat())
                    spinner.setIncrement(1.0f)
                }
            }
            eControlType.TYPE_COL4 -> {
                val clr = LLColor4()
                clr.setValue(sd)
                cs.setVisible(true)
                if (clr != LLColor4(cs.getValue())) {
                    cs.set(LLColor4(sd), true, false)
                }
                s4.setVisible(true)
                s4.setLabel("Alpha")
                if (!s4.hasFocus()) {
                    s4.setPrecision(3)
                    s4.setMinValue(0.0)
                    s4.setMaxValue(1.0f)
                    s4.setValue(clr.mV[VALPHA])
                }
            }
            eControlType.TYPE_COL3 -> {
                val clr = LLColor3()
                clr.setValue(sd)
                cs.setVisible(true)
                cs.setValue(sd)
            }
            eControlType.TYPE_LLSD -> {
                mComment?.setText(sd.toPrettyNotation())
            }
            else -> {
                mComment?.setText("unknown")
            }
        }
    }

    private fun getControlVariable(): LLControlVariable? {
        val item = mSettingsScrollList?.getFirstSelected() ?: return null
        return item.getUserdata() as? LLControlVariable
    }

    companion object {
        fun showControl(control: String) {
            val instance = LLFloaterReg.showTypedInstance<LLFloaterSettingsDebug>("settings_debug", "all", true)
            instance?.mSearchSettingsInput?.setText(control)
            instance?.onUpdateFilter()
        }
    }
}
