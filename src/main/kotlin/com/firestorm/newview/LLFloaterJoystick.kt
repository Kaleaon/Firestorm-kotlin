package com.firestorm.newview

import java.util.UUID

class LLFloaterJoystick private constructor(data: Any) : LLFloater(data) {

    private var mJoystickEnabled: Boolean = false
    private var mJoystickId: Any? = null
    private val mJoystickAxis: IntArray = IntArray(7)
    private var m3DCursor: Boolean = false
    private var mAutoLeveling: Boolean = false
    private var mZoomDirect: Boolean = false

    private var mAvatarEnabled: Boolean = false
    private var mBuildEnabled: Boolean = false
    private var mFlycamEnabled: Boolean = false
    private val mAvatarAxisScale: FloatArray = FloatArray(6)
    private val mBuildAxisScale: FloatArray = FloatArray(6)
    private val mFlycamAxisScale: FloatArray = FloatArray(7)
    private val mAvatarAxisDeadZone: FloatArray = FloatArray(6)
    private val mBuildAxisDeadZone: FloatArray = FloatArray(6)
    private val mFlycamAxisDeadZone: FloatArray = FloatArray(7)
    private var mAvatarFeathering: Float = 0f
    private var mBuildFeathering: Float = 0f
    private var mFlycamFeathering: Float = 0f

    private var mCheckFlycamEnabled: LLCheckBoxCtrl? = null
    private var mJoysticksCombo: LLComboBox? = null

    private var mHasDeviceList: Boolean = false
    private var mJoystickInitialized: Boolean = false
    private var mCurrentDeviceId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    private val mAxisStatsBar: Array<LLStatBar?> = arrayOfNulls(MAX_JOYSTICK_AXES)
    private val mButtonsLights: Array<LLIconCtrl?> = arrayOfNulls(MAX_JOYSTICK_BUTTONS)
    private val mAxisViews: Array<LLStatView?> = arrayOfNulls(MAX_JOYSTICK_AXES)

    init {
        if (!LLViewerJoystick.getInstance().isJoystickInitialized()) {
            LLViewerJoystick.getInstance().init(false)
        }
        initFromSettings()
    }

    open fun postBuild(): Boolean {
        center()
        val range = 0.5f

        for (i in 0 until MAX_JOYSTICK_AXES) {
            val statName = "Joystick axis $i"
            val axisname = "axis$i"
            mAxisStatsBar[i] = getChild<LLStatBar>(axisname)
            mAxisStatsBar[i]?.setStat(statName)
            mAxisStatsBar[i]?.setRange(-range, range)
            mAxisViews[i] = getChild<LLStatView>("axis_view_$i")

            val spin = getChild<LLSpinCtrl>("JoystickAxis$i")
            spin.setMaxValue((MAX_JOYSTICK_AXES - 1).toDouble())
        }

        for (i in 0 until MAX_JOYSTICK_BUTTONS) {
            mButtonsLights[i] = getChild<LLIconCtrl>("button_light_$i")
        }

        mJoysticksCombo = getChild<LLComboBox>("joystick_combo")
        mJoysticksCombo!!.setCommitCallback { onCommitJoystickEnabled(this) }
        mCheckFlycamEnabled = getChild<LLCheckBoxCtrl>("JoystickFlycamEnabled")
        mCheckFlycamEnabled!!.setCommitCallback { onCommitJoystickEnabled(this) }

        getChild<LLButton>("SpaceNavigatorDefaults").setClickedCallback { setSNDefaults() }
        getChild<LLButton>("cancel_btn").setClickedCallback { onClickCancel(this) }
        getChild<LLButton>("ok_btn").setClickedCallback { onClickOK(this) }

        refresh()
        refreshListOfDevices()
        updateAxesAndButtons()
        return true
    }

    open fun refresh() {
        super.refresh()
        initFromSettings()
    }

    open fun apply() {}

    open fun cancel() {
        gSavedSettings.setBool("JoystickEnabled", mJoystickEnabled)
        gSavedSettings.setLLSD("JoystickDeviceUUID", mJoystickId)

        for (i in 0..6) gSavedSettings.setInt("JoystickAxis$i", mJoystickAxis[i])

        gSavedSettings.setBool("Cursor3D", m3DCursor)
        gSavedSettings.setBool("AutoLeveling", mAutoLeveling)
        gSavedSettings.setBool("ZoomDirect", mZoomDirect)

        gSavedSettings.setBool("JoystickAvatarEnabled", mAvatarEnabled)
        gSavedSettings.setBool("JoystickBuildEnabled", mBuildEnabled)
        gSavedSettings.setBool("JoystickFlycamEnabled", mFlycamEnabled)

        for (i in 0..5) {
            gSavedSettings.setFloat("AvatarAxisScale$i", mAvatarAxisScale[i])
            gSavedSettings.setFloat("BuildAxisScale$i", mBuildAxisScale[i])
            gSavedSettings.setFloat("AvatarAxisDeadZone$i", mAvatarAxisDeadZone[i])
            gSavedSettings.setFloat("BuildAxisDeadZone$i", mBuildAxisDeadZone[i])
        }

        for (i in 0..6) {
            gSavedSettings.setFloat("FlycamAxisScale$i", mFlycamAxisScale[i])
            gSavedSettings.setFloat("FlycamAxisDeadZone$i", mFlycamAxisDeadZone[i])
        }

        gSavedSettings.setFloat("AvatarFeathering", mAvatarFeathering)
        gSavedSettings.setFloat("BuildFeathering", mBuildFeathering)
        gSavedSettings.setFloat("FlycamFeathering", mFlycamFeathering)
    }

    open fun draw() {
        val joystick = LLViewerJoystick.getInstance()
        val joystickInited = joystick.isJoystickInitialized()
        val nullUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (!mHasDeviceList
            || mJoystickInitialized != joystickInited
            || (joystick.isDeviceUUIDSet() && joystick.getDeviceUUID().asUUID() != mCurrentDeviceId)
            || (!joystick.isDeviceUUIDSet() && mCurrentDeviceId != nullUuid)) {
            refreshListOfDevices()
        }

        if (gFrameIntervalSeconds.value() == 0.0f) {
            joystick.updateStatus()
        }

        for (i in 0 until joystick.getNumOfJoystickAxes()) {
            val value = joystick.getJoystickAxis(i)
            TODO("GPU: sample joystick axis stat $i with value=$value")
            val bar = mAxisStatsBar[i]
            if (bar != null) {
                val (minbar, maxbar) = bar.getRange()
                if (Math.abs(value) > maxbar) {
                    val range = Math.abs(value)
                    bar.setRange(-range, range)
                }
            }
        }

        if (mJoystickEnabled) {
            val bright = LLUIColorTable.instance().getColor("White").get()
            val dim = LLUIColorTable.instance().getColor("Gray").get()
            for (i in 0 until joystick.getNumOfJoystickButtons()) {
                val value = joystick.getJoystickButton(i)
                mButtonsLights[i]?.setColor(if (value != 0) bright else dim)
            }
        }

        LLFloater.draw()
    }

    fun addDevice(name: String, value: Any) {
        mJoysticksCombo?.add(name, value, ADD_BOTTOM, 1)
    }

    fun updateAxesAndButtons() {
        val axes: Int
        val buttons: Int
        if (mJoystickEnabled) {
            axes = LLViewerJoystick.getInstance().getNumOfJoystickAxes()
            buttons = LLViewerJoystick.getInstance().getNumOfJoystickButtons()
        } else {
            axes = 0
            buttons = 0
        }

        for (i in 0 until MAX_JOYSTICK_AXES) {
            mAxisViews[i]?.setDisplayChildren(i < axes)
            mAxisViews[i]?.reshape(0, 0, false)
        }

        val dark = LLUIColorTable.instance().getColor("DkGray").get()
        val dim = LLUIColorTable.instance().getColor("Gray").get()
        for (i in 0 until MAX_JOYSTICK_BUTTONS) {
            mButtonsLights[i]?.setColor(if (i < buttons) dim else dark)
        }
    }

    protected fun refreshListOfDevices() {
        mJoysticksCombo!!.removeall()
        val noDevice = getString("JoystickDisabled")
        var noDeviceValue: Any = 0
        addDevice(noDevice, noDeviceValue)
        mHasDeviceList = false

        TODO("APR: use JVM equivalent — enumerate input devices via platform API")

        val joystick = LLViewerJoystick.getInstance()
        val isDeviceIdSet = joystick.isDeviceUUIDSet()
        val nullUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")

        if (joystick.isJoystickInitialized() && (!mHasDeviceList || !isDeviceIdSet)) {
            val desc = joystick.getDescription()
            if (desc.isNotEmpty()) {
                addDevice(desc, 1)
                mHasDeviceList = true
            }
        }

        if (gSavedSettings.getBool("JoystickEnabled") && mHasDeviceList) {
            if (isDeviceIdSet) {
                val guid = joystick.getDeviceUUID()
                mCurrentDeviceId = guid.asUUID()
                mJoysticksCombo!!.selectByValue(guid)
            } else {
                mCurrentDeviceId = nullUuid
                mJoysticksCombo!!.selectByValue(1)
            }
        } else {
            mJoysticksCombo!!.selectByValue(0)
        }

        if (isDeviceIdSet) {
            mCurrentDeviceId = joystick.getDeviceUUID().asUUID()
        } else {
            mCurrentDeviceId = nullUuid
        }
        mJoystickInitialized = joystick.isJoystickInitialized()
    }

    protected fun onClose(appQuitting: Boolean) {
        if (appQuitting) cancel()
    }

    protected fun onClickCloseBtn(appQuitting: Boolean) {
        cancel()
        closeFloater(appQuitting)
    }

    private fun initFromSettings() {
        mJoystickEnabled = gSavedSettings.getBool("JoystickEnabled")
        mJoystickId = gSavedSettings.getLLSD("JoystickDeviceUUID")

        for (i in 0..6) mJoystickAxis[i] = gSavedSettings.getInt("JoystickAxis$i")

        m3DCursor = gSavedSettings.getBool("Cursor3D")
        mAutoLeveling = gSavedSettings.getBool("AutoLeveling")
        mZoomDirect = gSavedSettings.getBool("ZoomDirect")

        mAvatarEnabled = gSavedSettings.getBool("JoystickAvatarEnabled")
        mBuildEnabled = gSavedSettings.getBool("JoystickBuildEnabled")
        mFlycamEnabled = gSavedSettings.getBool("JoystickFlycamEnabled")

        for (i in 0..5) {
            mAvatarAxisScale[i] = gSavedSettings.getFloat("AvatarAxisScale$i")
            mBuildAxisScale[i] = gSavedSettings.getFloat("BuildAxisScale$i")
            mAvatarAxisDeadZone[i] = gSavedSettings.getFloat("AvatarAxisDeadZone$i")
            mBuildAxisDeadZone[i] = gSavedSettings.getFloat("BuildAxisDeadZone$i")
        }

        for (i in 0..6) {
            mFlycamAxisScale[i] = gSavedSettings.getFloat("FlycamAxisScale$i")
            mFlycamAxisDeadZone[i] = gSavedSettings.getFloat("FlycamAxisDeadZone$i")
        }

        mAvatarFeathering = gSavedSettings.getFloat("AvatarFeathering")
        mBuildFeathering = gSavedSettings.getFloat("BuildFeathering")
        mFlycamFeathering = gSavedSettings.getFloat("FlycamFeathering")
    }

    companion object {
        const val MAX_JOYSTICK_AXES: Int = 8
        const val MAX_JOYSTICK_BUTTONS: Int = 16

        fun setSNDefaults() {
            LLViewerJoystick.getInstance().setSNDefaults()
        }

        private fun addDeviceCallback(name: String, value: Any, userdata: LLFloaterJoystick): Boolean {
            userdata.addDevice(name, value)
            return false
        }

        private fun onCommitJoystickEnabled(self: LLFloaterJoystick) {
            val value = self.mJoysticksCombo!!.getValue()
            val joystickEnabled: Boolean
            if (value is Int) {
                joystickEnabled = value != 0
            } else {
                LLViewerJoystick.getInstance().initDevice(value)
                joystickEnabled = true
            }
            gSavedSettings.setBool("JoystickEnabled", joystickEnabled)
            val flycamEnabled = self.mCheckFlycamEnabled!!.get()

            if (!joystickEnabled || !flycamEnabled) {
                val joystick = LLViewerJoystick.getInstance()
                if (joystick.getOverrideCamera()) {
                    joystick.toggleFlycam()
                }
            }

            LLViewerJoystick.getInstance().saveDeviceIdToSettings()
            self.refreshListOfDevices()
            self.mJoystickEnabled = joystickEnabled
            self.updateAxesAndButtons()
        }

        private fun onClickCancel(self: LLFloaterJoystick) {
            self.cancel()
            self.closeFloater()
        }

        private fun onClickOK(self: LLFloaterJoystick) {
            self.closeFloater()
        }
    }
}
