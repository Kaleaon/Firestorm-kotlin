package com.firestorm.newview

import com.firestorm.ui.LLFloater
import com.firestorm.ui.LLButton
import com.firestorm.ui.LLCheckBoxCtrl
import com.firestorm.ui.LLLineEditor
import com.firestorm.ui.LLUICtrl
import com.firestorm.ui.LLView
import com.firestorm.llsd.LLSD

private val CHECKBOX_PREFIXES = charArrayOf('s', 'm', 't', 'w', 'r', 'f', 'a')
private const val CHECKBOX_NAME = "_chk"

class LLFloaterRegionRestartSchedule(owner: LLView) : LLFloater(LLSD()) {

    private val mOwnerHandle: LLHandle<LLView> = owner.getHandle()
    private var mContextConeOpacity: Float = 0f

    private var mHoursLineEditor: LLLineEditor? = null
    private var mMinutesLineEditor: LLLineEditor? = null
    private var mPMAMButton: LLButton? = null
    private var mSaveButton: LLButton? = null
    private var mCancelButton: LLButton? = null

    private var mTimeAM: Boolean = true

    init {
        buildFromFile("floater_region_restart_schedule.xml")
    }

    override fun postBuild(): Boolean {
        mPMAMButton = getChild("am_pm_btn")
        mPMAMButton!!.setClickedCallback { onPMAMButtonClicked() }

        if (mPMAMButton!!.getVisible()) {
            val use24h = gSavedSettings.getBool("Use24HourClock")
            if (use24h) {
                mPMAMButton!!.setVisible(false)
                val lbl = getChild<LLUICtrl>("utc_label")
                lbl.translate(-mPMAMButton!!.getRect().getWidth(), 0)
            }
        }

        mSaveButton = getChild("save_btn")
        mSaveButton!!.setClickedCallback { onSaveButtonClicked() }

        mCancelButton = getChild("cancel_btn")
        mCancelButton!!.setClickedCallback { closeFloater(false) }

        mHoursLineEditor = getChild("hours_edt")
        mHoursLineEditor!!.setPrevalidate(LLTextValidate::validateNonNegativeS32)
        mHoursLineEditor!!.setCommitCallback { _, value -> onCommitHours(value) }

        mMinutesLineEditor = getChild("minutes_edt")
        mMinutesLineEditor!!.setPrevalidate(LLTextValidate::validateNonNegativeS32)
        mMinutesLineEditor!!.setCommitCallback { _, value -> onCommitMinutes(value) }

        for (c in CHECKBOX_PREFIXES) {
            val name = c + CHECKBOX_NAME
            getChild<LLCheckBoxCtrl>(name).setCommitCallback { _, _ -> mSaveButton!!.setEnabled(true) }
        }

        resetUI(false)
        return true
    }

    open fun onOpen(key: LLSD) {
        val url = gAgent.getRegionCapability("RegionSchedule")
        if (url.isNotEmpty()) {
            System.err.println("LLFloaterRegionRestartSchedule: onOpen not yet implemented")
            mSaveButton?.setEnabled(false)
        }
    }

    open fun draw() {
        val owner = mOwnerHandle.get()
        if (owner != null) {
            val maxOpacity = gSavedSettings.getFloat("PickerContextOpacity", 0.4f)
            drawConeToOwner(mContextConeOpacity, maxOpacity, owner)
        }
        super.draw()
    }

    fun onPMAMButtonClicked() {
        mSaveButton?.setEnabled(true)
        mTimeAM = !mTimeAM
        updateAMPM()
    }

    fun onSaveButtonClicked() {
        val url = gAgent.getRegionCapability("RegionSchedule")
        if (url.isEmpty()) return

        val days = StringBuilder()
        for (c in CHECKBOX_PREFIXES) {
            val name = c + CHECKBOX_NAME
            val chk = getChild<LLCheckBoxCtrl>(name)
            if (chk.getValue()) {
                days.append(c)
            }
        }

        val restart = LLSD()
        if (days.length < 7) {
            restart["type"] = "W"
            restart["days"] = days.toString().toUpperCase()
        } else {
            restart["type"] = "D"
        }

        var hours = mHoursLineEditor!!.getValue().asInteger()
        if (mPMAMButton!!.getVisible()) {
            if (hours == 12) {
                hours = 0 // 12:00 AM == 0:00; 12:00 PM == 12:00
            }
            if (!mTimeAM) {
                hours += 12
            }
        }
        restart["time"] = hours * 3600 + mMinutesLineEditor!!.getValue().asInteger() * 60

        val body = LLSD()
        body["restart"] = restart

        System.err.println("LLFloaterRegionRestartSchedule: onSaveButtonClicked not yet implemented")
        mSaveButton?.setEnabled(false)
    }

    fun onCommitHours(value: LLSD) {
        var hours = value.asInteger()
        if (mPMAMButton!!.getVisible()) {
            if (hours == 0) hours = 12 // 0:00 → 12:00 AM in 12h display
            hours = hours.coerceIn(1, 12)
        } else {
            hours = hours.coerceIn(0, 23)
        }
        mHoursLineEditor!!.setText(hours.toString().padStart(2, '0'))
        mSaveButton?.setEnabled(true)
    }

    fun onCommitMinutes(value: LLSD) {
        val minutes = value.asInteger().coerceIn(0, 59)
        mMinutesLineEditor!!.setText(minutes.toString().padStart(2, '0'))
        mSaveButton?.setEnabled(true)
    }

    fun resetUI(enableUi: Boolean) {
        for (c in CHECKBOX_PREFIXES) {
            val name = c + CHECKBOX_NAME
            val chk = getChild<LLCheckBoxCtrl>(name)
            chk.setValue(false)
            chk.setEnabled(enableUi)
        }
        if (mPMAMButton!!.getVisible()) {
            mHoursLineEditor!!.setValue("12")
            mPMAMButton!!.setEnabled(enableUi)
        } else {
            mHoursLineEditor!!.setValue("00")
        }
        mMinutesLineEditor!!.setValue("00")
        mMinutesLineEditor!!.setEnabled(enableUi)
        mHoursLineEditor!!.setEnabled(enableUi)
        mTimeAM = true
        updateAMPM()
    }

    fun updateAMPM() {
        val label = if (mTimeAM) getString("am_string") else getString("pm_string")
        mPMAMButton?.setLabel(label)
    }

    companion object {
        fun canUse(): Boolean {
            val url = gAgent.getRegionCapability("RegionSchedule")
            return url.isNotEmpty()
        }

        fun requestRegionShcheduleCoro(url: String, handle: LLHandle<LLFloater>) {
            System.err.println("LLFloaterRegionRestartSchedule: requestRegionShcheduleCoro not yet implemented")
        }

        fun setRegionShcheduleCoro(url: String, body: LLSD, handle: LLHandle<LLFloater>) {
            TODO("APR: use JVM equivalent — HTTP POST $url with body; on completion call floater.closeFloater() via handle")
        }
    }
}
