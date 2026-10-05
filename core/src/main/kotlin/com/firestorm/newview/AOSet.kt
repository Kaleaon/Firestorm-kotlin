package com.firestorm.newview

import java.util.UUID

class AOSet(inventoryID: UUID) {

    companion object {
        const val Start = 0
        const val Standing = 0
        const val Walking = 1
        const val Running = 2
        const val Sitting = 3
        const val SittingOnGround = 4
        const val Crouching = 5
        const val CrouchWalking = 6
        const val Landing = 7
        const val SoftLanding = 8
        const val StandingUp = 9
        const val Falling = 10
        const val FlyingDown = 11
        const val FlyingUp = 12
        const val Flying = 13
        const val FlyingSlow = 14
        const val Hovering = 15
        const val Jumping = 16
        const val PreJumping = 17
        const val TurningRight = 18
        const val TurningLeft = 19
        const val Typing = 20
        const val Floating = 21
        const val SwimmingForward = 22
        const val SwimmingUp = 23
        const val SwimmingDown = 24
        const val AOSTATES_MAX = 25

        // ZHAO-II notecard state name aliases, pipe-separated; first entry is canonical name
        private val STATE_NAME_STRINGS = arrayOf(
            "Standing|Stand.1|Stand.2|Stand.3",
            "Walking|Walk.N",
            "Running",
            "Sitting|Sit.N",
            "Sitting On Ground|Sit.G",
            "Crouching|Crouch",
            "Crouch Walking|Walk.C",
            "Landing|Land.N",
            "Soft Landing",
            "Standing Up|Stand.U",
            "Falling",
            "Flying Down|Hover.D",
            "Flying Up|Hover.U",
            "Flying|Fly.N",
            "Flying Slow",
            "Hovering|Hover.N",
            "Jumping|Jump.N",
            "Pre Jumping|Jump.P",
            "Turning Right|Turn.R",
            "Turning Left|Turn.L",
            "Typing",
            "Floating|Swim.H",
            "Swimming Forward|Swim.N",
            "Swimming Up|Swim.U",
            "Swimming Down|Swim.D"
        )

        // Motion UUIDs that map each state to its Linden Lab animation.
        // Swim states share UUIDs with fly states and receive special treatment in AOEngine.
        private val STATE_REMAP_IDS = arrayOf(
            AnimationStates.ANIM_AGENT_STAND,
            AnimationStates.ANIM_AGENT_WALK,
            AnimationStates.ANIM_AGENT_RUN,
            AnimationStates.ANIM_AGENT_SIT,
            AnimationStates.ANIM_AGENT_SIT_GROUND_CONSTRAINED,
            AnimationStates.ANIM_AGENT_CROUCH,
            AnimationStates.ANIM_AGENT_CROUCHWALK,
            AnimationStates.ANIM_AGENT_LAND,
            AnimationStates.ANIM_AGENT_MEDIUM_LAND,
            AnimationStates.ANIM_AGENT_STANDUP,
            AnimationStates.ANIM_AGENT_FALLDOWN,
            AnimationStates.ANIM_AGENT_HOVER_DOWN,
            AnimationStates.ANIM_AGENT_HOVER_UP,
            AnimationStates.ANIM_AGENT_FLY,
            AnimationStates.ANIM_AGENT_FLYSLOW,
            AnimationStates.ANIM_AGENT_HOVER,
            AnimationStates.ANIM_AGENT_JUMP,
            AnimationStates.ANIM_AGENT_PRE_JUMP,
            AnimationStates.ANIM_AGENT_TURNRIGHT,
            AnimationStates.ANIM_AGENT_TURNLEFT,
            AnimationStates.ANIM_AGENT_TYPE,
            AnimationStates.ANIM_AGENT_HOVER,       // Floating – needs special treatment
            AnimationStates.ANIM_AGENT_FLY,         // SwimmingForward – needs special treatment
            AnimationStates.ANIM_AGENT_HOVER_UP,    // SwimmingUp – needs special treatment
            AnimationStates.ANIM_AGENT_HOVER_DOWN   // SwimmingDown – needs special treatment
        )
    }

    data class AOAnimation(
        var name: String = "",
        var assetUUID: UUID = UUID(0, 0),
        var inventoryUUID: UUID = UUID(0, 0),
        var originalUUID: UUID = UUID(0, 0),
        var sortOrder: Int = 0
    )

    data class AOState(
        var name: String = "",
        val alternateNames: MutableList<String> = mutableListOf(),
        val addQueue: MutableList<InventoryItem> = mutableListOf(),
        var remapId: UUID = UUID(0, 0),
        var cycle: Boolean = false,
        var random: Boolean = false,
        var cycleTime: Float = 0f,
        val animations: MutableList<AOAnimation> = mutableListOf(),
        var currentAnimation: UInt = 0u,
        var currentAnimationID: UUID = UUID(0, 0),
        var inventoryUUID: UUID = UUID(0, 0),
        var dirty: Boolean = false
    )

    var inventoryUUID: UUID = inventoryID
    var name: String = "** New AO Set **"
    var sitOverride: Boolean = false
    var smart: Boolean = false
    var mouselookStandDisable: Boolean = false
    var complete: Boolean = false
    var dirty: Boolean = false
    var currentMotion: UUID = UUID(0, 0)

    val stateNames: MutableList<String> = mutableListOf()

    private val states: Array<AOState> = Array(AOSTATES_MAX) { AOState() }

    // Timer state – backed by a simple flag; actual scheduling is platform-specific
    private var timerRunning: Boolean = false
    private var timerPeriod: Float = 10000f

    init {
        for (index in 0 until AOSTATES_MAX) {
            val nameList = STATE_NAME_STRINGS[index].split("|")
            states[index].name = nameList[0]
            states[index].alternateNames.addAll(nameList)
            states[index].remapId = STATE_REMAP_IDS[index]
            states[index].inventoryUUID = UUID(0, 0)
            states[index].currentAnimation = 0u
            states[index].currentAnimationID = UUID(0, 0)
            states[index].cycle = false
            states[index].random = false
            states[index].cycleTime = 0f
            states[index].dirty = false
            stateNames.add(nameList[0])
        }
        stopTimer()
    }

    fun getState(stateIndex: Int): AOState = states[stateIndex]

    fun getStateByName(stateName: String): AOState? {
        for (index in 0 until AOSTATES_MAX) {
            val state = states[index]
            if (state.alternateNames.any { it == stateName }) return state
        }
        return null
    }

    fun getStateByRemapID(id: UUID): AOState? {
        val remapId = if (id == AnimationStates.ANIM_AGENT_SIT_GROUND)
            AnimationStates.ANIM_AGENT_SIT_GROUND_CONSTRAINED else id
        for (index in 0 until AOSTATES_MAX) {
            if (states[index].remapId == remapId) return states[index]
        }
        return null
    }

    fun getAnimationForState(state: AOState): UUID {
        val numOfAnimations = state.animations.size
        if (numOfAnimations == 0) return UUID(0, 0)

        if (state.cycle) {
            if (state.random) {
                state.currentAnimation = (Math.random() * numOfAnimations).toInt().toUInt()
            } else {
                state.currentAnimation++
                if (state.currentAnimation >= numOfAnimations.toUInt()) {
                    state.currentAnimation = 0u
                }
            }
        }

        val anim = state.animations[state.currentAnimation.toInt()]

        if (anim.assetUUID == UUID(0, 0)) {
            // Attempt lazy resolution via inventory; delegated to platform layer
            val item = InventoryModel.getItem(anim.inventoryUUID)
            if (item != null) {
                anim.assetUUID = item.assetUUID
            }
        }

        return anim.assetUUID
    }

    fun startTimer(timeout: Float) {
        timerPeriod = timeout
        timerRunning = true
        System.err.println("AOSet: startTimer not yet implemented")
    }

    fun stopTimer() {
        timerRunning = false
    }

    fun resetTimer() {
        if (timerRunning) {
            stopTimer()
            startTimer(timerPeriod)
        }
    }

    fun tick(): Boolean {
        AOEngine.cycleTimeout(this)
        return false
    }
}
