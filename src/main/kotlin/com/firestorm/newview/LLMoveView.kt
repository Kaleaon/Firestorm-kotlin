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
        forwardButton = TODO("GPU: getChild<LLJoystickAgentTurn>(\"forward btn\")")
        TODO("GPU: forwardButton.setHeldDownDelay(MOVE_BUTTON_DELAY)")

        backwardButton = TODO("GPU: getChild<LLJoystickAgentTurn>(\"backward btn\")")
        TODO("GPU: backwardButton.setHeldDownDelay(MOVE_BUTTON_DELAY)")

        slideLeftButton = TODO("GPU: getChild<LLJoystickAgentSlide>(\"move left btn\")")
        TODO("GPU: slideLeftButton.setHeldDownDelay(MOVE_BUTTON_DELAY)")

        slideRightButton = TODO("GPU: getChild<LLJoystickAgentSlide>(\"move right btn\")")
        TODO("GPU: slideRightButton.setHeldDownDelay(MOVE_BUTTON_DELAY)")

        turnLeftButton = TODO("GPU: getChild<LLButton>(\"turn left btn\")")
        TODO("GPU: turnLeftButton.setHeldDownDelay(MOVE_BUTTON_DELAY); .setHeldDownCallback { turnLeft() }")

        turnRightButton = TODO("GPU: getChild<LLButton>(\"turn right btn\")")
        TODO("GPU: turnRightButton.setHeldDownDelay(MOVE_BUTTON_DELAY); .setHeldDownCallback { turnRight() }")

        moveUpButton = TODO("GPU: getChild<LLButton>(\"move up btn\")")
        TODO("GPU: moveUpButton.setHeldDownDelay(MOVE_BUTTON_DELAY); .setHeldDownCallback { moveUp() }")

        moveDownButton = TODO("GPU: getChild<LLButton>(\"move down btn\")")
        TODO("GPU: moveDownButton.setHeldDownDelay(MOVE_BUTTON_DELAY); .setHeldDownCallback { moveDown() }")

        modeActionsPanel = TODO("GPU: getChild<LLPanel>(\"panel_modes\")")

        TODO("GPU: wire mode_walk_btn, mode_run_btn, mode_fly_btn commit callbacks to onWalkButtonClick/onRunButtonClick/onFlyButtonClick")

        initModeTooltips()
        initModeButtonMap()
        initMovementMode()

        TODO("APR: gAgent.addParcelChangedCallback { sUpdateMovementStatus() }")

        return true
    }

    open fun setVisible(visible: Boolean) {
        val currentVisible: Boolean = TODO("GPU: getVisible()")
        if (currentVisible == visible) {
            TODO("GPU: LLFloater::setVisible(visible)")
            return
        }
        if (visible) {
            TODO("APR: LLFirstUse::notMoving(false)")
            val ssfPanel = LLPanelStandStopFlying.getInstance()
            ssfPanel.reparent(this)
            TODO("GPU: ssf_panel.setOrigin(modeActionsRect.mLeft, modeActionsRect.mBottom)")
        } else {
            LLPanelStandStopFlying.getInstance().reparent(null)
        }
        TODO("GPU: LLFloater::setVisible(visible)")
    }

    open fun onOpen(key: Any) {
        val agentFlying: Boolean = TODO("APR: gAgent.getFlying()")
        if (agentFlying) {
            setFlyingMode(true)
            showModeButtons(false)
        }
        val avatarSitting: Boolean = TODO("APR: isAgentAvatarValid() && gAgentAvatarp->isSitting()")
        if (avatarSitting) {
            setSittingMode(true)
            showModeButtons(false)
        }
        sUpdateMovementStatus()
    }

    fun getCurrentTransparency(): Float {
        TODO("APR: gSavedSettings.getF32(\"CameraOpacity\")")
    }

    protected fun turnLeft() {
        val time: Float = TODO("GPU: turnLeftButton.getHeldDownTime()")
        TODO("APR: gAgent.moveYaw(getYawRate(time))")
    }

    protected fun turnRight() {
        val time: Float = TODO("GPU: turnRightButton.getHeldDownTime()")
        TODO("APR: gAgent.moveYaw(-getYawRate(time))")
    }

    protected fun moveUp() {
        TODO("APR: gAgent.moveUp(1)")
    }

    protected fun moveDown() {
        TODO("APR: gAgent.moveUp(-1)")
    }

    private fun onWalkButtonClick() { setMovementMode(EMovementMode.MM_WALK) }
    private fun onRunButtonClick() { setMovementMode(EMovementMode.MM_RUN) }
    private fun onFlyButtonClick() { setMovementMode(EMovementMode.MM_FLY) }

    private fun setMovementMode(mode: EMovementMode) {
        currentMode = mode
        if (mode == EMovementMode.MM_FLY) {
            TODO("APR: LLAgent::toggleFlying()")
        } else {
            TODO("APR: gAgent.setFlying(false)")
        }

        val agentFlying: Boolean = TODO("APR: gAgent.getFlying()")
        if (mode == EMovementMode.MM_FLY && !agentFlying) return

        when (mode) {
            EMovementMode.MM_RUN -> TODO("APR: gAgent.setAlwaysRun()")
            EMovementMode.MM_WALK -> TODO("APR: gAgent.clearAlwaysRun()")
            else -> {}
        }

        val agentRunning: Boolean = TODO("APR: gAgent.getRunning()")
        if (mode == EMovementMode.MM_WALK || mode == EMovementMode.MM_RUN) {
            currentMode = if (agentRunning) EMovementMode.MM_RUN else EMovementMode.MM_WALK
        }

        updateButtonsWithMovementMode(currentMode)

        val avatarSitting: Boolean = TODO("APR: isAgentAvatarValid() && gAgentAvatarp->isSitting()")
        val hideModeButtons = currentMode == EMovementMode.MM_FLY || avatarSitting
        showModeButtons(!hideModeButtons)
    }

    private fun updateButtonsWithMovementMode(newMode: EMovementMode) {
        setModeTooltip(newMode)
        setModeButtonToggleState(newMode)
        setModeTitle(newMode)
    }

    private fun initModeTooltips() {
        TODO("GPU: populate modeControlTooltipsMap for MM_WALK, MM_RUN, MM_FLY using getString() for each button tooltip key")
    }

    private fun initModeButtonMap() {
        TODO("GPU: populate modeControlButtonMap: MM_WALK->\"mode_walk_btn\", MM_RUN->\"mode_run_btn\", MM_FLY->\"mode_fly_btn\"")
    }

    private fun initMovementMode() {
        val alwaysRun: Boolean = TODO("APR: gAgent.getAlwaysRun()")
        val agentFlying: Boolean = TODO("APR: gAgent.getFlying()")
        val initMode = when {
            agentFlying -> EMovementMode.MM_FLY
            alwaysRun -> EMovementMode.MM_RUN
            else -> EMovementMode.MM_WALK
        }
        currentMode = initMode
        val avatarSitting: Boolean = TODO("APR: isAgentAvatarValid() && gAgentAvatarp->isSitting()")
        val hideModeButtons = currentMode == EMovementMode.MM_FLY || avatarSitting
        updateButtonsWithMovementMode(currentMode)
        showModeButtons(!hideModeButtons)
    }

    private fun setModeTooltip(mode: EMovementMode) {
        val tipMap = modeControlTooltipsMap[mode] ?: return
        tipMap.forEach { (ctrl, tooltip) ->
            TODO("GPU: ctrl.setToolTip(tooltip)")
        }
    }

    private fun setModeTitle(mode: EMovementMode) {
        val title = when (mode) {
            EMovementMode.MM_WALK -> TODO("GPU: getString(\"walk_title\")")
            EMovementMode.MM_RUN -> TODO("GPU: getString(\"run_title\")")
            EMovementMode.MM_FLY -> TODO("GPU: getString(\"fly_title\")")
        }
        TODO("GPU: setTitle(title)")
    }

    private fun setModeButtonToggleState(mode: EMovementMode) {
        modeControlButtonMap.forEach { (_, btn) ->
            TODO("GPU: btn.setToggleState(false)")
        }
        val activeBtn = modeControlButtonMap[mode]
        TODO("GPU: activeBtn.setToggleState(true)")
    }

    private fun showModeButtons(show: Boolean) {
        val panelVisible: Boolean = TODO("GPU: modeActionsPanel.getVisible()")
        if (panelVisible == show) return
        TODO("GPU: modeActionsPanel.setVisible(show)")
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
            val instance: LLFloaterMove? = TODO("GPU: LLFloaterReg::findTypedInstance<LLFloaterMove>(\"moveview\")")
            instance?.let {
                it.setFlyingModeImpl(fly)
                val avatarSitting: Boolean = TODO("APR: check gAgentAvatarp region/dead/sitting")
                it.showModeButtons(!fly && !avatarSitting)
            }
            if (fly) {
                LLPanelStandStopFlying.setStandStopFlyingMode(LLPanelStandStopFlying.EStandStopFlyingMode.SSFM_STOP_FLYING)
            } else {
                LLPanelStandStopFlying.clearStandStopFlyingMode(LLPanelStandStopFlying.EStandStopFlyingMode.SSFM_STOP_FLYING)
            }
        }

        fun setAlwaysRunMode(run: Boolean) {
            val instance: LLFloaterMove? = TODO("GPU: LLFloaterReg::findTypedInstance<LLFloaterMove>(\"moveview\")")
            instance?.setAlwaysRunModeImpl(run)
        }

        fun setSittingMode(sitting: Boolean) {
            if (sitting) {
                LLPanelStandStopFlying.setStandStopFlyingMode(LLPanelStandStopFlying.EStandStopFlyingMode.SSFM_STAND)
            } else {
                LLPanelStandStopFlying.clearStandStopFlyingMode(LLPanelStandStopFlying.EStandStopFlyingMode.SSFM_STAND)
                val agentFlying: Boolean = TODO("APR: gAgent.getFlying()")
                if (agentFlying) {
                    LLPanelStandStopFlying.setStandStopFlyingMode(LLPanelStandStopFlying.EStandStopFlyingMode.SSFM_STOP_FLYING)
                }
            }
            enableInstance()
        }

        fun enableInstance() {
            val instance: LLFloaterMove? = TODO("GPU: LLFloaterReg::findTypedInstance<LLFloaterMove>(\"moveview\")")
            instance?.let {
                val agentFlying: Boolean = TODO("APR: gAgent.getFlying()")
                if (agentFlying) {
                    it.showModeButtons(false)
                } else {
                    val avatarValid: Boolean = TODO("APR: isAgentAvatarValid() && !gAgentAvatarp->isSitting()")
                    it.showModeButtons(avatarValid)
                }
            }
        }

        fun sUpdateMovementStatus() {
            val floater: LLFloaterMove? = TODO("GPU: LLFloaterReg::findTypedInstance<LLFloaterMove>(\"moveview\")")
            floater?.let {
                val canRun: Boolean = TODO("APR: !RlvActions::hasBehaviour(RLV_BHVR_ALWAYSRUN)")
                val canFly: Boolean = TODO("APR: gAgent.canFly()")
                TODO("GPU: floater.modeControlButtonMap[MM_RUN].setEnabled(canRun); floater.modeControlButtonMap[MM_FLY].setEnabled(canFly)")
            }
        }
    }

    fun setFlyingModeImpl(fly: Boolean) {
        val alwaysRun: Boolean = TODO("APR: gAgent.getAlwaysRun()")
        updateButtonsWithMovementMode(
            if (fly) EMovementMode.MM_FLY
            else if (alwaysRun) EMovementMode.MM_RUN
            else EMovementMode.MM_WALK
        )
    }

    fun setAlwaysRunModeImpl(run: Boolean) {
        val agentFlying: Boolean = TODO("APR: gAgent.getFlying()")
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
        standButton = TODO("GPU: getChild<LLButton>(\"stand_btn\")")
        TODO("GPU: standButton.setCommitCallback { onStandButtonClick() }; .setCommitCallback { LLFloaterMove.enableInstance() }; .setVisible(false)")
        TODO("GPU: LLHints::getInstance()->registerHintTarget(\"stand_btn\", standButton.getHandle())")

        stopFlyingButton = TODO("GPU: getChild<LLButton>(\"stop_fly_btn\")")
        TODO("GPU: stopFlyingButton.setCommitCallback { onStopFlyingButtonClick() }; .setVisible(false)")

        TODO("GPU: gViewerWindow.setOnWorldViewRectUpdated { updatePosition() }")

        flycamButton = TODO("GPU: getChild<LLButton>(\"flycam_btn\")")
        TODO("GPU: flycamButton.setVisible(false)")

        return true
    }

    open fun setVisible(visible: Boolean) {
        val cameraMode: Int = TODO("GPU: gAgentCamera.getCameraMode()")
        val mouseLook: Int = TODO("GPU: CAMERA_MODE_MOUSELOOK constant")
        val showInMouselook: Boolean = TODO("APR: gSavedSettings.getBOOL(\"FSShowInterfaceInMouselook\")")
        val effectiveVisible = if (cameraMode == mouseLook && !showInMouselook) false else visible

        if (effectiveVisible) updatePosition()

        if (!attached) {
            TODO("GPU: if (getParent() != null) getParent().setVisible(effectiveVisible)")
        }
        TODO("GPU: LLPanel::setVisible(effectiveVisible)")
    }

    fun draw() {
        updatePosition()
        TODO("GPU: LLPanel::draw()")
    }

    fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        TODO("GPU: LLToolTipMgr::instance().unblockToolTips()")
        val standVisible: Boolean = TODO("GPU: standButton.getVisible()")
        val stopFlyVisible: Boolean = TODO("GPU: stopFlyingButton.getVisible()")
        if (standVisible) {
            TODO("GPU: LLToolTipMgr::instance().show(standButton.getToolTip())")
        } else if (stopFlyVisible) {
            TODO("GPU: LLToolTipMgr::instance().show(stopFlyingButton.getToolTip())")
        }
        TODO("GPU: return LLPanel::handleToolTip(x, y, mask)")
    }

    fun reparent(moveView: LLFloaterMove?) {
        val parent: Any? = TODO("GPU: dynamic_cast<LLPanel*>(getParent())")
        if (parent == null) return

        if (moveView != null) {
            if (originalParent == null) {
                originalParent = TODO("GPU: parent.getHandle()")
            }
            TODO("GPU: parent.removeChild(this)")
            val modesContainer: Any? = TODO("GPU: moveView.findChildView(\"modes_container\")")
            if (modesContainer != null) {
                TODO("GPU: modesContainer.addChild(this)")
            } else {
                TODO("GPU: moveView.addChild(this)")
            }
            attached = true
        } else {
            val orig = originalParent ?: return
            TODO("GPU: parent.removeChild(this); orig.addChild(this); orig.setVisible(getVisible())")
            attached = false
            updatePosition()
        }

        TODO("GPU: if (getParent() != null) reshape(getParent().getRect().getWidth(), getParent().getRect().getHeight(), false)")
    }

    private fun onStandButtonClick() {
        val rlvEnabled: Boolean = TODO("APR: RlvActions::isRlvEnabled()")
        val canStand: Boolean = TODO("APR: RlvActions::canStand()")
        if (!rlvEnabled || canStand) {
            TODO("APR: LLFirstUse::sit(false)")
            TODO("APR: LLSelectMgr::getInstance()->deselectAllForStandingUp()")
            TODO("APR: gAgent.setControlFlags(AGENT_CONTROL_STAND_UP)")
        }
        TODO("GPU: setFocus(false)")
    }

    private fun onStopFlyingButtonClick() {
        TODO("APR: gAgent.setFlying(false)")
        TODO("GPU: setFocus(false)")
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
                    TODO("GPU: panel.flycamButton.setVisible(true)")
                }
                EStandStopFlyingMode.SSFM_STAND -> {
                    TODO("APR: LLFirstUse::sit(); LLFirstUse::notMoving(false)")
                    TODO("GPU: panel.standButton.setVisible(true); panel.stopFlyingButton.setVisible(false)")
                }
                EStandStopFlyingMode.SSFM_STOP_FLYING -> {
                    TODO("GPU: panel.standButton.setVisible(false); panel.stopFlyingButton.setVisible(true)")
                }
            }
            panel.setVisible(true)
        }

        fun clearStandStopFlyingMode(mode: EStandStopFlyingMode) {
            val panel = getInstance()
            when (mode) {
                EStandStopFlyingMode.SSFM_STAND -> TODO("GPU: panel.standButton.setVisible(false)")
                EStandStopFlyingMode.SSFM_STOP_FLYING -> TODO("GPU: panel.stopFlyingButton.setVisible(false)")
                EStandStopFlyingMode.SSFM_FLYCAM -> {
                    TODO("GPU: panel.flycamButton.setVisible(false); panel.setFocus(false)")
                }
            }
        }

        private fun getStandStopFlyingPanel(): LLPanelStandStopFlying {
            val panel = LLPanelStandStopFlying()
            TODO("GPU: panel.buildFromFile(\"panel_stand_stop_flying.xml\"); panel.setVisible(false); panel.updatePosition()")
            return panel
        }
    }
}
