package com.firestorm.newview

private const val MOVE_BUTTON_DELAY: Float = 0.0f
private const val YAW_NUDGE_RATE: Float = 0.05f
private const val NUDGE_TIME: Float = 0.25f

class LLFloaterMove(private val key: Any) {

    enum class EMovementMode {
        MM_WALK,
        MM_RUN,
        MM_FLY
    }

    var forwardButton: Any? = null
    var backwardButton: Any? = null
    var slideLeftButton: Any? = null
    var slideRightButton: Any? = null
    var turnLeftButton: Any? = null
    var turnRightButton: Any? = null
    var moveUpButton: Any? = null
    var moveDownButton: Any? = null

    private var modeActionsPanel: Any? = null
    private val modeControlTooltipsMap: MutableMap<EMovementMode, MutableMap<Any, String>> = mutableMapOf()
    private val modeControlButtonMap: MutableMap<EMovementMode, Any> = mutableMapOf()
    private var currentMode: EMovementMode = EMovementMode.MM_WALK

    open fun postBuild(): Boolean {
        forwardButton = null
        // LLFloaterMove: getChild<LLJoystickAgentTurn>("forward btn") not yet implemented
        // LLFloaterMove: forwardButton.setHeldDownDelay(MOVE_BUTTON_DELAY) not yet implemented

        backwardButton = null
        // LLFloaterMove: getChild<LLJoystickAgentTurn>("backward btn") not yet implemented
        // LLFloaterMove: backwardButton.setHeldDownDelay(MOVE_BUTTON_DELAY) not yet implemented

        slideLeftButton = null
        // LLFloaterMove: getChild<LLJoystickAgentSlide>("move left btn") not yet implemented
        // LLFloaterMove: slideLeftButton.setHeldDownDelay(MOVE_BUTTON_DELAY) not yet implemented

        slideRightButton = null
        // LLFloaterMove: getChild<LLJoystickAgentSlide>("move right btn") not yet implemented
        // LLFloaterMove: slideRightButton.setHeldDownDelay(MOVE_BUTTON_DELAY) not yet implemented

        turnLeftButton = null
        // LLFloaterMove: turnLeftButton.setHeldDownDelay(MOVE_BUTTON_DELAY); .setHeldDownCallback { turnLeft() } not yet implemented

        turnRightButton = null
        // LLFloaterMove: turnRightButton.setHeldDownDelay(MOVE_BUTTON_DELAY); .setHeldDownCallback { turnRight() } not yet implemented

        moveUpButton = null
        // LLFloaterMove: moveUpButton.setHeldDownDelay(MOVE_BUTTON_DELAY); .setHeldDownCallback { moveUp() } not yet implemented

        moveDownButton = null
        // LLFloaterMove: moveDownButton.setHeldDownDelay(MOVE_BUTTON_DELAY); .setHeldDownCallback { moveDown() } not yet implemented

        modeActionsPanel = null
        // LLFloaterMove: getChild<LLPanel>("panel_modes") not yet implemented

        // LLFloaterMove: wire mode_walk_btn, mode_run_btn, mode_fly_btn commit callbacks not yet implemented

        initModeTooltips()
        initModeButtonMap()
        initMovementMode()

        System.err.println("LLFloaterMove: gAgent.addParcelChangedCallback { sUpdateMovementStatus() } not yet implemented")

        return true
    }

    open fun setVisible(visible: Boolean) {
        val currentVisible: Boolean = false
        if (currentVisible == visible) {
            // LLFloaterMove: LLFloater::setVisible(visible) not yet implemented
            return
        }
        if (visible) {
            System.err.println("LLFloaterMove: LLFirstUse::notMoving(false) not yet implemented")
            val ssfPanel = LLPanelStandStopFlying.getInstance()
            ssfPanel.reparent(this)
            // LLFloaterMove: ssf_panel.setOrigin(modeActionsRect.mLeft, modeActionsRect.mBottom) not yet implemented
        } else {
            LLPanelStandStopFlying.getInstance().reparent(null)
        }
        // LLFloaterMove: LLFloater::setVisible(visible) not yet implemented
    }

    open fun onOpen(key: Any) {
        val agentFlying: Boolean = false
        if (agentFlying) {
            setFlyingMode(true)
            showModeButtons(false)
        }
        val avatarSitting: Boolean = false
        if (avatarSitting) {
            setSittingMode(true)
            showModeButtons(false)
        }
        sUpdateMovementStatus()
    }

    fun getCurrentTransparency(): Float {
        return 0f
    }

    protected fun turnLeft() {
        val time: Float = 0f
        System.err.println("LLFloaterMove: gAgent.moveYaw(getYawRate(time)) not yet implemented")
    }

    protected fun turnRight() {
        val time: Float = 0f
        System.err.println("LLFloaterMove: gAgent.moveYaw(-getYawRate(time)) not yet implemented")
    }

    protected fun moveUp() {
        System.err.println("LLFloaterMove: gAgent.moveUp(1) not yet implemented")
    }

    protected fun moveDown() {
        System.err.println("LLFloaterMove: gAgent.moveUp(-1) not yet implemented")
    }

    private fun onWalkButtonClick() { setMovementMode(EMovementMode.MM_WALK) }
    private fun onRunButtonClick() { setMovementMode(EMovementMode.MM_RUN) }
    private fun onFlyButtonClick() { setMovementMode(EMovementMode.MM_FLY) }

    private fun setMovementMode(mode: EMovementMode) {
        currentMode = mode
        if (mode == EMovementMode.MM_FLY) {
            System.err.println("LLFloaterMove: LLAgent::toggleFlying() not yet implemented")
        } else {
            System.err.println("LLFloaterMove: gAgent.setFlying(false) not yet implemented")
        }

        val agentFlying: Boolean = false
        if (mode == EMovementMode.MM_FLY && !agentFlying) return

        when (mode) {
            EMovementMode.MM_RUN -> System.err.println("LLFloaterMove: gAgent.setAlwaysRun() not yet implemented")
            EMovementMode.MM_WALK -> System.err.println("LLFloaterMove: gAgent.clearAlwaysRun() not yet implemented")
            else -> {}
        }

        val agentRunning: Boolean = false
        if (mode == EMovementMode.MM_WALK || mode == EMovementMode.MM_RUN) {
            currentMode = if (agentRunning) EMovementMode.MM_RUN else EMovementMode.MM_WALK
        }

        updateButtonsWithMovementMode(currentMode)

        val avatarSitting: Boolean = false
        val hideModeButtons = currentMode == EMovementMode.MM_FLY || avatarSitting
        showModeButtons(!hideModeButtons)
    }

    private fun updateButtonsWithMovementMode(newMode: EMovementMode) {
        setModeTooltip(newMode)
        setModeButtonToggleState(newMode)
        setModeTitle(newMode)
    }

    private fun initModeTooltips() {
        // LLFloaterMove: populate modeControlTooltipsMap for MM_WALK, MM_RUN, MM_FLY not yet implemented
    }

    private fun initModeButtonMap() {
        // LLFloaterMove: populate modeControlButtonMap: MM_WALK->"mode_walk_btn", MM_RUN->"mode_run_btn", MM_FLY->"mode_fly_btn" not yet implemented
    }

    private fun initMovementMode() {
        val alwaysRun: Boolean = false
        val agentFlying: Boolean = false
        val initMode = when {
            agentFlying -> EMovementMode.MM_FLY
            alwaysRun -> EMovementMode.MM_RUN
            else -> EMovementMode.MM_WALK
        }
        currentMode = initMode
        val avatarSitting: Boolean = false
        val hideModeButtons = currentMode == EMovementMode.MM_FLY || avatarSitting
        updateButtonsWithMovementMode(currentMode)
        showModeButtons(!hideModeButtons)
    }

    private fun setModeTooltip(mode: EMovementMode) {
        val tipMap = modeControlTooltipsMap[mode] ?: return
        tipMap.forEach { (_, _) ->
            // LLFloaterMove: ctrl.setToolTip(tooltip) not yet implemented
        }
    }

    private fun setModeTitle(mode: EMovementMode) {
        val title = when (mode) {
            EMovementMode.MM_WALK -> ""
            EMovementMode.MM_RUN -> ""
            EMovementMode.MM_FLY -> ""
        }
        // LLFloaterMove: setTitle(title) not yet implemented
        @Suppress("UNUSED_EXPRESSION")
        title
    }

    private fun setModeButtonToggleState(mode: EMovementMode) {
        modeControlButtonMap.forEach { (_, _) ->
            // LLFloaterMove: btn.setToggleState(false) not yet implemented
        }
        // LLFloaterMove: activeBtn.setToggleState(true) not yet implemented
    }

    private fun showModeButtons(show: Boolean) {
        val panelVisible: Boolean = false
        if (panelVisible == show) return
        // LLFloaterMove: modeActionsPanel.setVisible(show) not yet implemented
    }

    companion object {
        fun getYawRate(time: Float): Float {
            return if (time < NUDGE_TIME) {
                YAW_NUDGE_RATE + time * (1f - YAW_NUDGE_RATE) / NUDGE_TIME
            } else {
                1f
            }
        }

        fun setFlyingMode(fly: Boolean) {
            val instance: LLFloaterMove? = null
            instance?.let {
                it.setFlyingModeImpl(fly)
                val avatarSitting: Boolean = false
                it.showModeButtons(!fly && !avatarSitting)
            }
            if (fly) {
                LLPanelStandStopFlying.setStandStopFlyingMode(LLPanelStandStopFlying.EStandStopFlyingMode.SSFM_STOP_FLYING)
            } else {
                LLPanelStandStopFlying.clearStandStopFlyingMode(LLPanelStandStopFlying.EStandStopFlyingMode.SSFM_STOP_FLYING)
            }
        }

        fun setAlwaysRunMode(run: Boolean) {
            val instance: LLFloaterMove? = null
            instance?.setAlwaysRunModeImpl(run)
        }

        fun setSittingMode(sitting: Boolean) {
            if (sitting) {
                LLPanelStandStopFlying.setStandStopFlyingMode(LLPanelStandStopFlying.EStandStopFlyingMode.SSFM_STAND)
            } else {
                LLPanelStandStopFlying.clearStandStopFlyingMode(LLPanelStandStopFlying.EStandStopFlyingMode.SSFM_STAND)
                val agentFlying: Boolean = false
                if (agentFlying) {
                    LLPanelStandStopFlying.setStandStopFlyingMode(LLPanelStandStopFlying.EStandStopFlyingMode.SSFM_STOP_FLYING)
                }
            }
            enableInstance()
        }

        fun enableInstance() {
            val instance: LLFloaterMove? = null
            instance?.let {
                val agentFlying: Boolean = false
                if (agentFlying) {
                    it.showModeButtons(false)
                } else {
                    val avatarValid: Boolean = false
                    it.showModeButtons(avatarValid)
                }
            }
        }

        fun sUpdateMovementStatus() {
            val floater: LLFloaterMove? = null
            floater?.let {
                // LLFloaterMove: modeControlButtonMap[MM_RUN].setEnabled(canRun); modeControlButtonMap[MM_FLY].setEnabled(canFly) not yet implemented
            }
        }
    }

    fun setFlyingModeImpl(fly: Boolean) {
        val alwaysRun: Boolean = false
        updateButtonsWithMovementMode(
            if (fly) EMovementMode.MM_FLY
            else if (alwaysRun) EMovementMode.MM_RUN
            else EMovementMode.MM_WALK
        )
    }

    fun setAlwaysRunModeImpl(run: Boolean) {
        val agentFlying: Boolean = false
        if (!agentFlying) {
            updateButtonsWithMovementMode(if (run) EMovementMode.MM_RUN else EMovementMode.MM_WALK)
        }
    }
}

class LLPanelStandStopFlying private constructor() {

    enum class EStandStopFlyingMode {
        SSFM_STAND,
        SSFM_STOP_FLYING,
        SSFM_FLYCAM
    }

    private var standButton: Any? = null
    private var stopFlyingButton: Any? = null
    private var flycamButton: Any? = null
    private var originalParent: Any? = null
    private var attached: Boolean = false

    open fun postBuild(): Boolean {
        standButton = null
        // LLPanelStandStopFlying: standButton.setCommitCallback/setVisible not yet implemented
        // LLPanelStandStopFlying: LLHints::getInstance()->registerHintTarget("stand_btn", ...) not yet implemented

        stopFlyingButton = null
        // LLPanelStandStopFlying: stopFlyingButton.setCommitCallback/setVisible not yet implemented

        // LLPanelStandStopFlying: gViewerWindow.setOnWorldViewRectUpdated { updatePosition() } not yet implemented

        flycamButton = null
        // LLPanelStandStopFlying: flycamButton.setVisible(false) not yet implemented

        return true
    }

    open fun setVisible(visible: Boolean) {
        val cameraMode: Int = 0
        val mouseLook: Int = 0
        val showInMouselook: Boolean = false
        val effectiveVisible = if (cameraMode == mouseLook && !showInMouselook) false else visible

        if (effectiveVisible) updatePosition()

        if (!attached) {
            // LLPanelStandStopFlying: if (getParent() != null) getParent().setVisible(effectiveVisible) not yet implemented
        }
        // LLPanelStandStopFlying: LLPanel::setVisible(effectiveVisible) not yet implemented
    }

    fun draw() {
        updatePosition()
        // LLPanelStandStopFlying: LLPanel::draw() not yet implemented
    }

    fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        // LLPanelStandStopFlying: LLToolTipMgr::instance().unblockToolTips() not yet implemented
        val standVisible: Boolean = false
        val stopFlyVisible: Boolean = false
        if (standVisible) {
            // LLPanelStandStopFlying: LLToolTipMgr::instance().show(standButton.getToolTip()) not yet implemented
        } else if (stopFlyVisible) {
            // LLPanelStandStopFlying: LLToolTipMgr::instance().show(stopFlyingButton.getToolTip()) not yet implemented
        }
        return false
    }

    fun reparent(moveView: LLFloaterMove?) {
        val parent: Any? = null
        if (parent == null) return

        if (moveView != null) {
            if (originalParent == null) {
                originalParent = null
            }
            // LLPanelStandStopFlying: parent.removeChild(this) not yet implemented
            val modesContainer: Any? = null
            if (modesContainer != null) {
                // LLPanelStandStopFlying: modesContainer.addChild(this) not yet implemented
            } else {
                // LLPanelStandStopFlying: moveView.addChild(this) not yet implemented
            }
            attached = true
        } else {
            val orig = originalParent ?: return
            // LLPanelStandStopFlying: parent.removeChild(this); orig.addChild(this); orig.setVisible(getVisible()) not yet implemented
            @Suppress("UNUSED_EXPRESSION")
            orig
            attached = false
            updatePosition()
        }

        // LLPanelStandStopFlying: reshape(getParent().getRect().getWidth(), getParent().getRect().getHeight(), false) not yet implemented
    }

    private fun onStandButtonClick() {
        val rlvEnabled: Boolean = false
        val canStand: Boolean = false
        if (!rlvEnabled || canStand) {
            System.err.println("LLPanelStandStopFlying: LLFirstUse::sit(false) not yet implemented")
            System.err.println("LLPanelStandStopFlying: LLSelectMgr::getInstance()->deselectAllForStandingUp() not yet implemented")
            System.err.println("LLPanelStandStopFlying: gAgent.setControlFlags(AGENT_CONTROL_STAND_UP) not yet implemented")
        }
        // LLPanelStandStopFlying: setFocus(false) not yet implemented
    }

    private fun onStopFlyingButtonClick() {
        System.err.println("LLPanelStandStopFlying: gAgent.setFlying(false) not yet implemented")
        // LLPanelStandStopFlying: setFocus(false) not yet implemented
    }

    private fun updatePosition() {
        // Position is managed by the parent container at layout time; no explicit repositioning needed.
    }

    companion object {
        private var instance: LLPanelStandStopFlying? = null

        fun getInstance(): LLPanelStandStopFlying {
            return instance ?: getStandStopFlyingPanel().also { instance = it }
        }

        fun setStandStopFlyingMode(mode: EStandStopFlyingMode) {
            val panel = getInstance()
            when (mode) {
                EStandStopFlyingMode.SSFM_FLYCAM -> {
                    // LLPanelStandStopFlying: panel.flycamButton.setVisible(true) not yet implemented
                }
                EStandStopFlyingMode.SSFM_STAND -> {
                    System.err.println("LLPanelStandStopFlying: LLFirstUse::sit(); LLFirstUse::notMoving(false) not yet implemented")
                    // LLPanelStandStopFlying: panel.standButton.setVisible(true); panel.stopFlyingButton.setVisible(false) not yet implemented
                }
                EStandStopFlyingMode.SSFM_STOP_FLYING -> {
                    // LLPanelStandStopFlying: panel.standButton.setVisible(false); panel.stopFlyingButton.setVisible(true) not yet implemented
                }
            }
            panel.setVisible(true)
        }

        fun clearStandStopFlyingMode(mode: EStandStopFlyingMode) {
            val panel = getInstance()
            when (mode) {
                EStandStopFlyingMode.SSFM_STAND -> {
                    // LLPanelStandStopFlying: panel.standButton.setVisible(false) not yet implemented
                }
                EStandStopFlyingMode.SSFM_STOP_FLYING -> {
                    // LLPanelStandStopFlying: panel.stopFlyingButton.setVisible(false) not yet implemented
                }
                EStandStopFlyingMode.SSFM_FLYCAM -> {
                    // LLPanelStandStopFlying: panel.flycamButton.setVisible(false); panel.setFocus(false) not yet implemented
                }
            }
        }

        private fun getStandStopFlyingPanel(): LLPanelStandStopFlying {
            val panel = LLPanelStandStopFlying()
            // LLPanelStandStopFlying: panel.buildFromFile("panel_stand_stop_flying.xml"); panel.setVisible(false); panel.updatePosition() not yet implemented
            return panel
        }
    }
}
