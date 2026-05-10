package com.firestorm.newview

import java.util.UUID

// ─── Stubs for platform types not yet ported ────────────────────────────────

typealias InventoryItem = Any          // placeholder for LLInventoryItem
typealias InventoryFuncType = (UUID) -> Unit

object InventoryModel {
    fun getItem(uuid: UUID): AOSet.AOAnimation? = TODO("APR: use JVM inventory model")
    fun isCategoryComplete(uuid: UUID): Boolean = TODO("APR: use JVM inventory model")
    fun fetchDescendentsOf(uuid: UUID): Unit = TODO("APR: use JVM inventory model")
    fun getDirectDescendentsOf(uuid: UUID): Pair<List<Any>, List<Any>> = TODO("APR: use JVM inventory model")
    fun createNewCategory(parentId: UUID, name: String, callback: (UUID) -> Unit): Unit = TODO("APR: use JVM inventory model")
    fun updateItem(item: Any): Unit = TODO("APR: use JVM inventory model")
    fun removeCategory(uuid: UUID): Unit = TODO("APR: use JVM inventory model")
    fun notifyObservers(): Unit = TODO("APR: use JVM inventory model")
    fun changeItemParent(item: Any, newParent: UUID, restamp: Boolean): Unit = TODO("APR: use JVM inventory model")
    fun getRootFolderID(): UUID = TODO("APR: use JVM inventory model")
    fun findCategoryByName(name: String): UUID = TODO("APR: use JVM inventory model")
    fun findCategoryUUIDForType(type: Any): UUID = TODO("APR: use JVM inventory model")
    fun getCategory(uuid: UUID): Any? = TODO("APR: use JVM inventory model")
}

object AnimationStates {
    val ANIM_AGENT_STAND: UUID = UUID.fromString("2408fe9e-df1d-1d7d-f4ff-1384fa7b350f")
    val ANIM_AGENT_WALK: UUID = UUID.fromString("6ed24bd8-91aa-4b12-ccc7-c97c857ab4e0")
    val ANIM_AGENT_RUN: UUID = UUID.fromString("05ddbff8-aaa9-92a1-2b74-8fe77a29b445")
    val ANIM_AGENT_SIT: UUID = UUID.fromString("1a5fe8ac-a804-8a5d-7cbd-56bd83184568")
    val ANIM_AGENT_SIT_GROUND: UUID = UUID.fromString("1c7600d6-661f-b87b-efe2-d7421eb93c86")
    val ANIM_AGENT_SIT_GROUND_CONSTRAINED: UUID = UUID.fromString("1a2bd58e-87ff-0df8-0b4c-53e047b0bb6e")
    val ANIM_AGENT_CROUCH: UUID = UUID.fromString("201f3fdf-cb1f-dbec-201f-7333e328ae7c")
    val ANIM_AGENT_CROUCHWALK: UUID = UUID.fromString("47f5f6fb-22e5-ae44-f871-73aaaf4a6022")
    val ANIM_AGENT_LAND: UUID = UUID.fromString("7a17b059-12b2-41b1-570a-186368b6aa6f")
    val ANIM_AGENT_MEDIUM_LAND: UUID = UUID.fromString("f4f00d6e-b9fe-9292-f4cb-0ae06ea58d57")
    val ANIM_AGENT_STANDUP: UUID = UUID.fromString("3da1d753-028a-5446-24f3-9c9b420a8015")
    val ANIM_AGENT_FALLDOWN: UUID = UUID.fromString("666307d9-a860-572d-6fd4-c3ab8865c094")
    val ANIM_AGENT_HOVER_DOWN: UUID = UUID.fromString("20f959ab-1264-f160-6b37-b1c5afa54188")
    val ANIM_AGENT_HOVER_UP: UUID = UUID.fromString("62c5de58-cb33-5743-3d07-9e4cd4352864")
    val ANIM_AGENT_FLY: UUID = UUID.fromString("aec4610c-757f-bc4e-c092-c6e9caf18daf")
    val ANIM_AGENT_FLYSLOW: UUID = UUID.fromString("2b5a38b2-5e00-3a97-a495-4c826bc443e6")
    val ANIM_AGENT_HOVER: UUID = UUID.fromString("4ae8016b-31b9-03bb-c401-b1ea941db41d")
    val ANIM_AGENT_JUMP: UUID = UUID.fromString("2305bd75-1ca9-b03b-1faa-b176b8a8c49e")
    val ANIM_AGENT_PRE_JUMP: UUID = UUID.fromString("7a4e87fe-de39-6fcb-6223-024b00893244")
    val ANIM_AGENT_TURNRIGHT: UUID = UUID.fromString("56e0ba0d-4a9f-7f27-6117-32f2ebbf6135")
    val ANIM_AGENT_TURNLEFT: UUID = UUID.fromString("68a8d552-4f65-0d71-a6c4-99bfb657f1c4")
    val ANIM_AGENT_TYPE: UUID = UUID.fromString("c541c47f-e0c0-058b-ad1a-d6ae3a4584d9")
    val ANIM_AGENT_STAND_1: UUID = UUID.fromString("b2f1f165-f1de-1a56-af5b-4e0e8b0b16f4")
    val ANIM_AGENT_STAND_2: UUID = UUID.fromString("f22fed8b-a5ed-2c93-64d5-bdd8b93c889f")
    val ANIM_AGENT_STAND_3: UUID = UUID.fromString("518b4c0f-b9ec-1b63-8628-9ef86706bd2f")
    val ANIM_AGENT_STAND_4: UUID = UUID.fromString("b4416cc7-d1c1-b26c-3328-97ddb1c3b8f4")
    val ANIM_AGENT_WALK_NEW: UUID = UUID.fromString("33339176-7ddc-9397-94a4-bf3403cbc8f5")
    val ANIM_AGENT_FEMALE_WALK: UUID = UUID.fromString("1a53a34e-31ed-f949-e2e2-fdf3e13b3c52")
    val ANIM_AGENT_FEMALE_WALK_NEW: UUID = UUID.fromString("689f8ecb-1bed-2454-7fd0-b8bf7b87e1de")
    val ANIM_AGENT_RUN_NEW: UUID = UUID.fromString("85995026-eade-5d78-d364-94a64512cb66")
    val ANIM_AGENT_FEMALE_RUN_NEW: UUID = UUID.fromString("aerobic-placeholder-uuid-run-female")
    val ANIM_AGENT_SIT_FEMALE: UUID = UUID.fromString("b1709c8d-ecd3-54a1-4f28-d55ac0840782")
    val ANIM_AGENT_SIT_GENERIC: UUID = UUID.fromString("sit-generic-placeholder-uuid-00000")
}

// ─── Timer collection ────────────────────────────────────────────────────────

class AOTimerCollection {
    private val inventoryPollingIntervalMs: Long = 5_000L

    var inventoryTimer: Boolean = true
        set(value) { field = value; updateTimers() }
    var settingsTimer: Boolean = false
        set(value) { field = value; updateTimers() }
    var reloadTimer: Boolean = false
        set(value) { field = value; updateTimers() }
    var importTimer: Boolean = false
        set(value) { field = value; updateTimers() }

    fun enableInventoryTimer(enable: Boolean) { inventoryTimer = enable }
    fun enableSettingsTimer(enable: Boolean) { settingsTimer = enable }
    fun enableReloadTimer(enable: Boolean) { reloadTimer = enable }
    fun enableImportTimer(enable: Boolean) { importTimer = enable }

    fun tick() {
        if (inventoryTimer) AOEngine.tick()
        if (settingsTimer) AOEngine.saveSettings()
        if (reloadTimer) AOEngine.reload(fromTimer = true)
        if (importTimer) AOEngine.processImport(fromTimer = true)
    }

    private fun updateTimers() {
        val anyActive = inventoryTimer || settingsTimer || reloadTimer || importTimer
        TODO("APR: use JVM equivalent – ${if (anyActive) "start" else "stop"} periodic timer at $inventoryPollingIntervalMs ms")
    }
}

// ─── Sit-cancel timer ────────────────────────────────────────────────────────

class AOSitCancelTimer {
    private var tickCount: Int = 0
    private var running: Boolean = false

    fun oneShot() {
        tickCount = 0
        running = true
        TODO("APR: use JVM equivalent – schedule tick() every 100 ms")
    }

    fun stop() {
        running = false
    }

    fun tick(): Boolean {
        tickCount++
        AOEngine.checkSitCancel()
        if (tickCount == 10) stop()
        return false
    }
}

// ─── AOEngine singleton ──────────────────────────────────────────────────────

object AOEngine {

    enum class CycleMode { CycleAny, CycleNext, CyclePrevious }

    private const val ROOT_AO_FOLDER = "#AO"
    private val ENCRYPTION_MAGIC_ID: UUID = UUID.fromString("4b552ff5-fd63-408c-8288-cd09429852ba")

    private val timerCollection = AOTimerCollection()
    private val sitCancelTimer = AOSitCancelTimer()

    private var enabled: Boolean = false
    private var enabledStands: Boolean = false
    private var inMouselookFlag: Boolean = false
    private var underWater: Boolean = false

    private var aoFolder: UUID = UUID(0, 0)
    private var lastMotion: UUID = AnimationStates.ANIM_AGENT_STAND
    private var lastOverriddenMotion: UUID = AnimationStates.ANIM_AGENT_STAND
    private var transitionId: UUID = UUID(0, 0)
    private var ignoreMotionStopOnce: UUID = UUID(0, 0)

    private val sets: MutableList<AOSet> = mutableListOf()
    private val oldSets: MutableList<AOSet> = mutableListOf()
    private var currentSet: AOSet? = null
    private var defaultSet: AOSet? = null

    private var importSet: AOSet? = null
    private val oldImportSets: MutableList<AOSet> = mutableListOf()
    private var importRetryCount: Int = 0

    // Signal callbacks
    private val updatedListeners: MutableList<() -> Unit> = mutableListOf()
    private val animationChangedListeners: MutableList<(UUID) -> Unit> = mutableListOf()

    fun setReloadCallback(cb: () -> Unit) { updatedListeners.add(cb) }
    fun setAnimationChangedCallback(cb: (UUID) -> Unit) { animationChangedListeners.add(cb) }

    private fun fireUpdated() = updatedListeners.forEach { it() }
    private fun fireAnimationChanged(id: UUID) = animationChangedListeners.forEach { it(id) }

    // ── Initialisation (called from login-complete hook) ─────────────────────

    fun onLoginComplete() = init()

    private fun init() {
        val doEnable = SavedPerAccountSettings.getBool("UseAO")
        val doEnableStands = SavedPerAccountSettings.getBool("UseAOStands")
        if (doEnable) {
            enabled = true
            enableStands(true)
        } else {
            enableStands(doEnableStands)
            enable(false)
        }
    }

    private fun onToggleAOControl() {
        enable(SavedPerAccountSettings.getBool("UseAO"))
        if (enabled) {
            SavedPerAccountSettings.setBool("UseAOStands", true)
        }
    }

    private fun onToggleAOStandsControl() {
        enableStands(SavedPerAccountSettings.getBool("UseAOStands"))
    }

    private fun onPauseAO() {
        if (SavedPerAccountSettings.getBool("UseAO")) {
            enable(!SavedPerAccountSettings.getBool("PauseAO"))
        }
    }

    // ── Enable / disable ─────────────────────────────────────────────────────

    fun enable(doEnable: Boolean) {
        enabled = doEnable
        val current = currentSet ?: return

        val state = current.getStateByRemapID(lastMotion)
        if (enabled) {
            if (state != null && state.animations.isNotEmpty()) {
                if (lastOverriddenMotion != AnimationStates.ANIM_AGENT_SIT_GROUND &&
                    lastOverriddenMotion != AnimationStates.ANIM_AGENT_SIT_GROUND_CONSTRAINED &&
                    lastOverriddenMotion != AnimationStates.ANIM_AGENT_SIT
                ) {
                    Agent.sendAnimationRequest(lastOverriddenMotion, AnimRequest.STOP)
                }

                val animation = override(lastMotion, start = true)
                if (animation == UUID(0, 0)) return

                when (lastMotion) {
                    AnimationStates.ANIM_AGENT_STAND -> {
                        if (!enabledStands) return
                        stopAllStandVariants()
                    }
                    AnimationStates.ANIM_AGENT_WALK -> {
                        Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_WALK_NEW, AnimRequest.STOP)
                        Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_FEMALE_WALK, AnimRequest.STOP)
                        Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_FEMALE_WALK_NEW, AnimRequest.STOP)
                        AgentAvatar.stopMotion(AnimationStates.ANIM_AGENT_WALK_NEW)
                        AgentAvatar.stopMotion(AnimationStates.ANIM_AGENT_FEMALE_WALK)
                        AgentAvatar.stopMotion(AnimationStates.ANIM_AGENT_FEMALE_WALK_NEW)
                    }
                    AnimationStates.ANIM_AGENT_RUN -> {
                        Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_RUN_NEW, AnimRequest.STOP)
                        Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_FEMALE_RUN_NEW, AnimRequest.STOP)
                        AgentAvatar.stopMotion(AnimationStates.ANIM_AGENT_RUN_NEW)
                        AgentAvatar.stopMotion(AnimationStates.ANIM_AGENT_FEMALE_RUN_NEW)
                    }
                    AnimationStates.ANIM_AGENT_SIT -> {
                        stopAllSitVariants()
                        Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_SIT_GENERIC, AnimRequest.START)
                    }
                }
                Agent.sendAnimationRequest(animation, AnimRequest.START)
                fireAnimationChanged(state.animations[state.currentAnimation.toInt()].inventoryUUID)
                ignoreMotionStopOnce = lastMotion
            }
        } else {
            fireAnimationChanged(UUID(0, 0))
            if (lastOverriddenMotion == AnimationStates.ANIM_AGENT_SIT) {
                Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_SIT_GENERIC, AnimRequest.STOP)
            }
            for (index in 0 until AOSet.AOSTATES_MAX) {
                val s = current.getState(index)
                val animation = s.currentAnimationID
                if (animation != UUID(0, 0)) {
                    Agent.sendAnimationRequest(animation, AnimRequest.STOP)
                    AgentAvatar.stopMotion(animation)
                    s.currentAnimationID = UUID(0, 0)
                }
            }
            if (lastOverriddenMotion != AnimationStates.ANIM_AGENT_SIT || !foreignAnimations()) {
                Agent.sendAnimationRequest(lastMotion, AnimRequest.START)
            }
            current.stopTimer()
        }
    }

    fun enableStands(doEnable: Boolean) {
        enabledStands = doEnable
        enable(enabled)
    }

    // ── Core override logic ───────────────────────────────────────────────────

    fun override(motion: UUID, start: Boolean): UUID {
        if (!enabled) {
            if (start && currentSet != null) {
                val state = currentSet!!.getStateByRemapID(motion)
                if (state != null) {
                    setLastMotion(motion)
                    if (state.animations.isNotEmpty()) setLastOverriddenMotion(motion)
                }
            }
            return UUID(0, 0)
        }

        if (sets.isEmpty()) return UUID(0, 0)
        val current = currentSet ?: return UUID(0, 0)

        if (!start && motion == ignoreMotionStopOnce) {
            ignoreMotionStopOnce = UUID(0, 0)
            if (motion == AnimationStates.ANIM_AGENT_SIT) {
                Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_SIT_GENERIC, AnimRequest.STOP)
            }
            return UUID(0, 0)
        }

        val state = getStateForMotion(motion) ?: return UUID(0, 0)

        fireAnimationChanged(UUID(0, 0))

        if (motion != AnimationStates.ANIM_AGENT_TYPE) {
            val cleanupStates = intArrayOf(
                AOSet.Standing, AOSet.Walking, AOSet.Running, AOSet.Sitting,
                AOSet.SittingOnGround, AOSet.Crouching, AOSet.CrouchWalking,
                AOSet.Falling, AOSet.FlyingDown, AOSet.FlyingUp, AOSet.Flying,
                AOSet.FlyingSlow, AOSet.Hovering, AOSet.Jumping, AOSet.TurningRight,
                AOSet.TurningLeft, AOSet.Floating, AOSet.SwimmingForward,
                AOSet.SwimmingUp, AOSet.SwimmingDown
            )
            for (stateNum in cleanupStates) {
                val stateToCheck = current.getState(stateNum)
                if (stateToCheck !== state && stateToCheck.currentAnimationID != UUID(0, 0)) {
                    Agent.sendAnimationRequest(stateToCheck.currentAnimationID, AnimRequest.STOP)
                    AgentAvatar.stopMotion(stateToCheck.currentAnimationID)
                    stateToCheck.currentAnimationID = UUID(0, 0)
                }
            }
        }

        var animation: UUID
        current.stopTimer()

        if (start) {
            setLastMotion(motion)

            if (current.mouselookStandDisable && motion == AnimationStates.ANIM_AGENT_STAND && inMouselookFlag) {
                return UUID(0, 0)
            }
            if (!enabledStands && (motion == AnimationStates.ANIM_AGENT_STAND ||
                        motion == AnimationStates.ANIM_AGENT_TURNRIGHT ||
                        motion == AnimationStates.ANIM_AGENT_TURNLEFT)) {
                return UUID(0, 0)
            }
            if (!current.sitOverride && motion == AnimationStates.ANIM_AGENT_SIT) {
                return UUID(0, 0)
            }
            if (motion == AnimationStates.ANIM_AGENT_SIT_GROUND ||
                motion == AnimationStates.ANIM_AGENT_SIT_GROUND_CONSTRAINED
            ) {
                val agentRootId = AgentAvatar.getRootId()
                if (agentRootId != null && agentRootId != Agent.agentId) {
                    return UUID(0, 0)
                }
            }

            if (state.animations.isNotEmpty()) setLastOverriddenMotion(motion)

            if (motion != AnimationStates.ANIM_AGENT_TYPE) {
                current.setMotion(motion)
            }

            animation = current.getAnimationForState(state)

            if (state.currentAnimationID != UUID(0, 0)) {
                Agent.sendAnimationRequest(state.currentAnimationID, AnimRequest.STOP)
                AgentAvatar.stopMotion(state.currentAnimationID)
            }

            state.currentAnimationID = animation

            if (animation != UUID(0, 0) && state.currentAnimation < state.animations.size.toUInt()) {
                fireAnimationChanged(state.animations[state.currentAnimation.toInt()].inventoryUUID)
            }

            setStateCycleTimer(state)

            if (motion == AnimationStates.ANIM_AGENT_SIT) {
                Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_SIT_GENERIC, AnimRequest.START)
                if (current.smart) sitCancelTimer.oneShot()
            } else if (motion == AnimationStates.ANIM_AGENT_SIT_GROUND ||
                motion == AnimationStates.ANIM_AGENT_SIT_GROUND_CONSTRAINED ||
                motion == AnimationStates.ANIM_AGENT_PRE_JUMP ||
                motion == AnimationStates.ANIM_AGENT_STANDUP ||
                motion == AnimationStates.ANIM_AGENT_LAND ||
                motion == AnimationStates.ANIM_AGENT_MEDIUM_LAND
            ) {
                Agent.sendAnimationRequest(animation, AnimRequest.START)
                return UUID(0, 0)
            }
        } else {
            if (motion == transitionId) {
                transitionId = UUID(0, 0)
                return UUID(0, 0)
            }
            transitionId = UUID(0, 0)

            animation = state.currentAnimationID
            state.currentAnimationID = UUID(0, 0)

            if (motion == AnimationStates.ANIM_AGENT_TYPE) {
                val previousState = current.getStateByRemapID(lastMotion)
                if (previousState != null) setStateCycleTimer(previousState)
                return animation
            }

            if (motion == AnimationStates.ANIM_AGENT_SIT) stopAllSitVariants()

            if (motion != current.currentMotion) return animation

            current.setMotion(UUID(0, 0))

            if (motion == AnimationStates.ANIM_AGENT_SIT_GROUND ||
                motion == AnimationStates.ANIM_AGENT_SIT_GROUND_CONSTRAINED ||
                motion == AnimationStates.ANIM_AGENT_PRE_JUMP ||
                motion == AnimationStates.ANIM_AGENT_STANDUP ||
                motion == AnimationStates.ANIM_AGENT_LAND ||
                motion == AnimationStates.ANIM_AGENT_MEDIUM_LAND
            ) {
                Agent.sendAnimationRequest(animation, AnimRequest.STOP)
                AgentAvatar.stopMotion(animation)
                setStateCycleTimer(state)
                return UUID(0, 0)
            }
        }

        return animation
    }

    // ── Cycling ───────────────────────────────────────────────────────────────

    fun cycleTimeout(set: AOSet) {
        if (!enabled) return
        if (set !== currentSet) return
        cycle(CycleMode.CycleAny)
    }

    fun cycle(cycleMode: CycleMode, resetTimer: Boolean = false) {
        if (!enabled) return
        val current = currentSet ?: return
        if (lastMotion == AnimationStates.ANIM_AGENT_SIT && !current.sitOverride) return
        if (lastMotion == AnimationStates.ANIM_AGENT_STAND && current.mouselookStandDisable && inMouselookFlag) return

        val state = current.getStateByRemapID(lastMotion) ?: return
        if (state.animations.isEmpty()) return
        if (!state.cycle && cycleMode == CycleMode.CycleAny) return

        val oldAnimation = state.currentAnimationID
        val animation: UUID

        if (cycleMode == CycleMode.CycleAny) {
            animation = current.getAnimationForState(state)
        } else {
            when (cycleMode) {
                CycleMode.CyclePrevious -> {
                    if (state.currentAnimation == 0u)
                        state.currentAnimation = (state.animations.size - 1).toUInt()
                    else
                        state.currentAnimation--
                }
                CycleMode.CycleNext -> {
                    state.currentAnimation++
                    if (state.currentAnimation == state.animations.size.toUInt())
                        state.currentAnimation = 0u
                }
                else -> Unit
            }
            val anim = state.animations[state.currentAnimation.toInt()]
            if (anim.assetUUID == UUID(0, 0)) {
                val item = InventoryModel.getItem(anim.originalUUID)
                if (item != null) anim.assetUUID = item.assetUUID
            }
            animation = anim.assetUUID
        }

        if (animation == oldAnimation) return

        fireAnimationChanged(UUID(0, 0))
        state.currentAnimationID = animation
        if (animation != UUID(0, 0)) {
            Agent.sendAnimationRequest(animation, AnimRequest.START)
            fireAnimationChanged(state.animations[state.currentAnimation.toInt()].inventoryUUID)
        }
        if (oldAnimation != UUID(0, 0)) {
            Agent.sendAnimationRequest(oldAnimation, AnimRequest.STOP)
            AgentAvatar.stopMotion(oldAnimation)
        }
        if (resetTimer) current.resetTimer()
    }

    // ── Play a specific animation by inventory UUID ───────────────────────────

    fun playAnimation(animation: UUID) {
        if (!enabled) return
        val current = currentSet ?: return
        if (lastMotion == AnimationStates.ANIM_AGENT_SIT && !current.sitOverride) return
        if (lastMotion == AnimationStates.ANIM_AGENT_STAND && current.mouselookStandDisable && inMouselookFlag) return

        val state = current.getStateByRemapID(lastMotion) ?: return
        if (state.animations.isEmpty()) return

        val item = InventoryModel.getItem(animation) ?: return

        val newAnimation = item.assetUUID
        if (newAnimation == UUID(0, 0)) return

        val oldAnimation = state.currentAnimationID
        if (newAnimation == oldAnimation) return

        fireAnimationChanged(UUID(0, 0))

        val idx = state.animations.indexOfFirst { it.assetUUID == newAnimation }
        if (idx == -1) return

        state.currentAnimation = idx.toUInt()
        state.currentAnimationID = newAnimation
        Agent.sendAnimationRequest(newAnimation, AnimRequest.START)
        fireAnimationChanged(state.animations[idx].inventoryUUID)

        if (oldAnimation != UUID(0, 0)) {
            Agent.sendAnimationRequest(oldAnimation, AnimRequest.STOP)
            AgentAvatar.stopMotion(oldAnimation)
        }

        current.resetTimer()
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    fun getCurrentSet(): AOSet? = currentSet
    fun getCurrentState(): AOSet.AOState? = currentSet?.getStateByRemapID(lastMotion)
    fun getAOFolder(): UUID = aoFolder
    fun getSetList(): List<AOSet> = sets.toList()
    fun getCurrentSetName(): String = currentSet?.name ?: ""
    fun getDefaultSet(): AOSet? = defaultSet

    fun getSetByName(name: String): AOSet? = sets.firstOrNull { it.name == name }

    fun selectSet(set: AOSet) {
        if (enabled && currentSet != null) {
            val state = currentSet!!.getStateByRemapID(lastOverriddenMotion)
            if (state != null) {
                Agent.sendAnimationRequest(state.currentAnimationID, AnimRequest.STOP)
                state.currentAnimationID = UUID(0, 0)
                currentSet!!.stopTimer()
            }
        }
        currentSet = set
        if (enabled) {
            Agent.sendAnimationRequest(override(lastMotion, start = true), AnimRequest.START)
        }
    }

    fun selectSetByName(name: String): AOSet? {
        val set = getSetByName(name) ?: return null
        selectSet(set)
        return set
    }

    fun renameSet(set: AOSet, name: String): Boolean {
        if (name.isEmpty() || name.contains(':')) return false
        set.name = name
        set.dirty = true
        return true
    }

    // ── Mutators that propagate dirty flag and trigger side effects ───────────

    fun setDefaultSet(set: AOSet?) {
        defaultSet = set
        sets.forEach { it.dirty = true }
    }

    fun setOverrideSits(set: AOSet, overrideSit: Boolean) {
        set.sitOverride = overrideSit
        set.dirty = true
        if (currentSet !== set || lastMotion != AnimationStates.ANIM_AGENT_SIT || !enabled) return
        if (overrideSit) {
            stopAllSitVariants()
            Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_SIT_GENERIC, AnimRequest.START)
        } else {
            Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_SIT_GENERIC, AnimRequest.STOP)
            val sitState = currentSet!!.getState(AOSet.Sitting)
            val animation = sitState.currentAnimationID
            if (animation != UUID(0, 0)) {
                Agent.sendAnimationRequest(animation, AnimRequest.STOP)
                sitState.currentAnimationID = UUID(0, 0)
            }
            if (!foreignAnimations()) {
                Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_SIT, AnimRequest.START)
            }
        }
    }

    fun setSmart(set: AOSet, smart: Boolean) {
        set.smart = smart
        set.dirty = true
        if (!enabled) return
        if (smart) {
            val rootId = AgentAvatar.getRootId()
            if (rootId != null && rootId != Agent.agentId) sitCancelTimer.oneShot()
        }
    }

    fun setDisableMouselookStands(set: AOSet, disabled: Boolean) {
        set.mouselookStandDisable = disabled
        set.dirty = true
        if (currentSet !== set || !enabled) return
        inMouselookFlag = !AgentCamera.cameraMouselook()
        inMouselook(!inMouselookFlag)
    }

    fun setCycle(state: AOSet.AOState, cycle: Boolean) { state.cycle = cycle; state.dirty = true }
    fun setRandomize(state: AOSet.AOState, randomize: Boolean) { state.random = randomize; state.dirty = true }
    fun setCycleTime(state: AOSet.AOState, time: Float) { state.cycleTime = time; state.dirty = true }

    // ── Mouselook ─────────────────────────────────────────────────────────────

    fun inMouselook(mouselook: Boolean) {
        if (inMouselookFlag == mouselook) return
        inMouselookFlag = mouselook
        val current = currentSet ?: return
        if (!current.mouselookStandDisable || !enabled || lastMotion != AnimationStates.ANIM_AGENT_STAND) return
        if (mouselook) {
            val state = current.getState(AOSet.Standing)
            val animation = state.currentAnimationID
            if (animation != UUID(0, 0)) {
                Agent.sendAnimationRequest(animation, AnimRequest.STOP)
                AgentAvatar.stopMotion(animation)
                state.currentAnimationID = UUID(0, 0)
            }
            Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_STAND, AnimRequest.START)
        } else {
            stopAllStandVariants()
            Agent.sendAnimationRequest(override(AnimationStates.ANIM_AGENT_STAND, start = true), AnimRequest.START)
        }
    }

    // ── Inventory / set management ────────────────────────────────────────────

    fun addSet(name: String, callback: InventoryFuncType, reload: Boolean = true) {
        if (aoFolder == UUID(0, 0)) {
            tick(); return
        }
        InventoryModel.createNewCategory(aoFolder, name, callback)
        if (reload) timerCollection.enableReloadTimer(true)
    }

    fun removeSet(set: AOSet): Boolean {
        purgeFolder(set.inventoryUUID)
        timerCollection.enableReloadTimer(true)
        return true
    }

    fun addAnimation(set: AOSet, state: AOSet.AOState, item: InventoryItem, reload: Boolean = true) {
        TODO("APR: use JVM inventory model – build AOAnimation from item and add to state")
    }

    fun removeAnimation(set: AOSet, state: AOSet.AOState, index: Int): Boolean {
        if (index < 0 || state.animations.isEmpty()) return false
        TODO("APR: use JVM inventory model – remove inventory link and erase animations[index]")
    }

    fun swapWithPrevious(state: AOSet.AOState, index: Int): Boolean {
        if (state.animations.size < 2 || index == 0) return false
        val tmp = state.animations.removeAt(index)
        state.animations.add(index - 1, tmp)
        updateSortOrder(state)
        return true
    }

    fun swapWithNext(state: AOSet.AOState, index: Int): Boolean {
        if (state.animations.size < 2 || index == state.animations.size - 1) return false
        val tmp = state.animations.removeAt(index)
        state.animations.add(index + 1, tmp)
        updateSortOrder(state)
        return true
    }

    fun reloadStateAnimations(set: AOSet, state: AOSet.AOState) {
        state.animations.clear()
        TODO("APR: use JVM inventory model – fetch descendents of state.inventoryUUID and rebuild animations list")
    }

    // ── Reload / update ───────────────────────────────────────────────────────

    fun reload(fromTimer: Boolean) {
        val wasEnabled = enabled
        timerCollection.enableReloadTimer(false)
        if (wasEnabled) enable(false)
        Agent.stopCurrentAnimations()
        lastOverriddenMotion = AnimationStates.ANIM_AGENT_STAND
        clear(fromTimer)
        aoFolder = UUID(0, 0)
        timerCollection.enableInventoryTimer(true)
        tick()
        if (wasEnabled) enable(true)
    }

    fun update() {
        if (aoFolder == UUID(0, 0)) return
        if (!InventoryModel.isCategoryComplete(aoFolder)) {
            InventoryModel.fetchDescendentsOf(aoFolder)
            return
        }
        TODO("APR: use JVM inventory model – walk AO folder tree and rebuild sets/states")
    }

    fun tick() {
        if (!AgentAvatar.isValid()) return
        TODO("APR: use JVM inventory model – locate #Firestorm/#AO folders and call update()")
    }

    fun clear(fromTimer: Boolean) {
        oldSets.addAll(sets)
        sets.clear()
        currentSet = null
        if (!fromTimer) {
            oldSets.clear()
            oldImportSets.clear()
        }
    }

    // ── Notecard import ───────────────────────────────────────────────────────

    fun importNotecard(item: InventoryItem): Boolean {
        TODO("APR: use JVM asset storage – download notecard asset and call parseNotecard()")
    }

    fun parseNotecard(buffer: String?) {
        if (buffer == null) {
            importSet = null
            fireUpdated()
            return
        }
        TODO("APR: parse ZHAO-II notecard format and populate importSet states")
    }

    fun processImport(fromTimer: Boolean) {
        val impSet = importSet ?: return
        if (impSet.inventoryUUID == UUID(0, 0)) {
            addSet(impSet.name, { newId -> impSet.inventoryUUID = newId }, false)
            importRetryCount++
            if (importRetryCount >= 5) {
                timerCollection.enableImportTimer(false)
                importSet = null
                fireUpdated()
            }
            return
        }
        TODO("APR: use JVM inventory model – create state folders and link animations for impSet")
    }

    // ── Underwater / sit-cancel helpers ──────────────────────────────────────

    fun checkSitCancel() {
        if (!foreignAnimations()) return
        val sitState = currentSet?.getStateByRemapID(AnimationStates.ANIM_AGENT_SIT) ?: return
        val animation = sitState.currentAnimationID
        if (animation != UUID(0, 0)) {
            Agent.sendAnimationRequest(animation, AnimRequest.STOP)
            Agent.sendAnimationRequest(AnimationStates.ANIM_AGENT_SIT_GENERIC, AnimRequest.STOP)
            AgentAvatar.stopMotion(animation)
            sitCancelTimer.stop()
            currentSet?.stopTimer()
        }
    }

    fun checkBelowWater(checkUnderwater: Boolean) {
        if (underWater == checkUnderwater) return
        val mapped = mapSwimming(lastMotion)
        if (mapped == null || mapped.animations.isEmpty()) {
            underWater = checkUnderwater
            return
        }
        var id = override(lastMotion, start = false)
        if (id == UUID(0, 0)) id = lastMotion
        Agent.sendAnimationRequest(id, AnimRequest.STOP)
        if (!underWater) transitionId = id
        underWater = checkUnderwater
        id = override(lastMotion, start = true)
        if (id == UUID(0, 0)) id = lastMotion
        Agent.sendAnimationRequest(id, AnimRequest.START)
    }

    // ── Save settings ─────────────────────────────────────────────────────────

    fun saveSettings() {
        for (set in sets) {
            if (set.dirty) {
                saveSet(set)
                set.dirty = false
            }
            for (stateIndex in 0 until AOSet.AOSTATES_MAX) {
                val state = set.getState(stateIndex)
                if (state.dirty) {
                    saveState(state)
                    state.dirty = false
                }
            }
        }
    }

    // ── Region change ─────────────────────────────────────────────────────────

    private fun onRegionChange() {
        if (!enabled) return
        val current = currentSet ?: return
        if (lastMotion == AnimationStates.ANIM_AGENT_SIT) {
            if (!current.sitOverride || lastOverriddenMotion != AnimationStates.ANIM_AGENT_SIT) return
            val state = current.getState(AOSet.Sitting)
            if (state.currentAnimationID == UUID(0, 0)) return
        }
        Agent.sendAnimationRequest(lastMotion, AnimRequest.START)
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun setLastMotion(motion: UUID) {
        if (motion != AnimationStates.ANIM_AGENT_TYPE) lastMotion = motion
    }

    private fun setLastOverriddenMotion(motion: UUID) {
        if (motion != AnimationStates.ANIM_AGENT_TYPE) lastOverriddenMotion = motion
    }

    private fun setStateCycleTimer(state: AOSet.AOState) {
        if (state.cycleTime > 0f) currentSet?.startTimer(state.cycleTime)
    }

    private fun stopAllStandVariants() {
        listOf(
            AnimationStates.ANIM_AGENT_STAND_1, AnimationStates.ANIM_AGENT_STAND_2,
            AnimationStates.ANIM_AGENT_STAND_3, AnimationStates.ANIM_AGENT_STAND_4
        ).forEach { id ->
            Agent.sendAnimationRequest(id, AnimRequest.STOP)
            AgentAvatar.stopMotion(id)
        }
    }

    private fun stopAllSitVariants() {
        listOf(AnimationStates.ANIM_AGENT_SIT_FEMALE, AnimationStates.ANIM_AGENT_SIT_GENERIC).forEach { id ->
            Agent.sendAnimationRequest(id, AnimRequest.STOP)
            AgentAvatar.stopMotion(id)
        }
        val rootId = AgentAvatar.getRootId()
        if (rootId != null && rootId != Agent.agentId) return
        listOf(AnimationStates.ANIM_AGENT_SIT_GROUND, AnimationStates.ANIM_AGENT_SIT_GROUND_CONSTRAINED).forEach { id ->
            Agent.sendAnimationRequest(id, AnimRequest.STOP)
            AgentAvatar.stopMotion(id)
        }
    }

    private fun foreignAnimations(): Boolean {
        val current = currentSet ?: return false
        if (!current.smart) return false
        val rootId = AgentAvatar.getRootId() ?: return false
        if (rootId == Agent.agentId) return false
        return AgentAvatar.hasNonAgentAnimationOnSeat(rootId)
    }

    private fun mapSwimming(motion: UUID): AOSet.AOState? {
        val stateNum = when (motion) {
            AnimationStates.ANIM_AGENT_HOVER -> AOSet.Floating
            AnimationStates.ANIM_AGENT_FLY -> AOSet.SwimmingForward
            AnimationStates.ANIM_AGENT_HOVER_UP -> AOSet.SwimmingUp
            AnimationStates.ANIM_AGENT_HOVER_DOWN -> AOSet.SwimmingDown
            else -> return null
        }
        return currentSet?.getState(stateNum)
    }

    private fun getStateForMotion(motion: UUID): AOSet.AOState? {
        val current = currentSet ?: return null
        val default = current.getStateByRemapID(motion)
        if (!underWater) return default
        val mapped = mapSwimming(motion) ?: return default
        return if (mapped.animations.isEmpty()) default else mapped
    }

    private fun updateSortOrder(state: AOSet.AOState) {
        for (index in state.animations.indices) {
            if (state.animations[index].sortOrder != index) {
                state.animations[index].sortOrder = index
                TODO("APR: use JVM inventory model – update item description to \"$index\"")
            }
        }
    }

    private fun saveSet(set: AOSet) {
        var params = set.name
        if (set.sitOverride) params += ":SO"
        if (set.smart) params += ":SM"
        if (set.mouselookStandDisable) params += ":DM"
        if (set === defaultSet) params += ":**"
        TODO("APR: use JVM inventory model – rename category set.inventoryUUID to \"$params\"")
        fireUpdated()
    }

    private fun saveState(state: AOSet.AOState) {
        var params = state.name
        if (state.cycleTime > 0f) params += ":CT${"%.2f".format(state.cycleTime)}"
        if (state.cycle) params += ":CY"
        if (state.random) params += ":RN"
        TODO("APR: use JVM inventory model – rename category state.inventoryUUID to \"$params\"")
    }

    private fun findForeignItems(uuid: UUID): Boolean {
        TODO("APR: use JVM inventory model – walk subtree and move non-animation-link items to lost-and-found")
    }

    private fun purgeFolder(uuid: UUID) {
        TODO("APR: use JVM inventory model – move non-links to lost-and-found, trash and purge folder")
    }

    private fun createAnimationLink(state: AOSet.AOState, item: InventoryItem): Boolean {
        if (state.inventoryUUID == UUID(0, 0)) return false
        TODO("APR: use JVM inventory model – link item into state.inventoryUUID folder")
    }

    // ── Platform stubs ────────────────────────────────────────────────────────

    private object Agent {
        val agentId: UUID get() = TODO("APR: use JVM equivalent")
        fun sendAnimationRequest(id: UUID, request: AnimRequest): Unit = TODO("APR: use JVM equivalent")
        fun stopCurrentAnimations(): Unit = TODO("APR: use JVM equivalent")
        fun allowOperation(perm: Int, permissions: Any, group: Int): Boolean = TODO("APR: use JVM equivalent")
    }

    private object AgentAvatar {
        fun isValid(): Boolean = TODO("APR: use JVM equivalent")
        fun getRootId(): UUID? = TODO("APR: use JVM equivalent")
        fun stopMotion(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun hasNonAgentAnimationOnSeat(seatId: UUID): Boolean = TODO("APR: use JVM equivalent")
    }

    private object AgentCamera {
        fun cameraMouselook(): Boolean = TODO("APR: use JVM equivalent")
    }

    private object SavedPerAccountSettings {
        fun getBool(key: String): Boolean = TODO("APR: use JVM equivalent")
        fun setBool(key: String, value: Boolean): Unit = TODO("APR: use JVM equivalent")
    }

    private enum class AnimRequest { START, STOP }
}
