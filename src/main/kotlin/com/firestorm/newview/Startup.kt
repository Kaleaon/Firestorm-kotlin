package com.firestorm.newview

enum class StartupState {
    STATE_FIRST,
    STATE_FETCH_GRID_INFO,
    STATE_AUDIO_INIT,
    STATE_BROWSER_INIT,
    STATE_LOGIN_SHOW,
    STATE_LOGIN_WAIT,
    STATE_LOGIN_CLEANUP,
    STATE_AGENTS_WAIT,
    STATE_LOGIN_AUTH_INIT,
    STATE_LOGIN_CURL_UNSTUCK,
    STATE_LOGIN_PROCESS_RESPONSE,
    STATE_LOGIN_CONFIRM_NOTIFICATION,
    STATE_WORLD_INIT,
    STATE_MULTIMEDIA_INIT,
    STATE_FONT_INIT,
    STATE_SEED_GRANTED_WAIT,
    STATE_SEED_CAP_GRANTED,
    STATE_WORLD_WAIT,
    STATE_AGENT_SEND,
    STATE_AGENT_WAIT,
    STATE_INVENTORY_SEND,
    STATE_INVENTORY_CALLBACKS,
    STATE_INVENTORY_SKEL,
    STATE_INVENTORY_SEND2,
    STATE_MISC,
    STATE_PRECACHE,
    STATE_WEARABLES_WAIT,
    STATE_CLEANUP,
    STATE_STARTED;

    operator fun compareTo(other: StartupState): Int = ordinal.compareTo(other.ordinal)
    operator fun compareTo(other: Int): Int = ordinal.compareTo(other)
}

operator fun Int.compareTo(state: StartupState): Int = compareTo(state.ordinal)

enum class StartLocation {
    LAST,
    HOME,
    DIRECT,
    PARCEL,
    TELEHUB,
    URL
}

var gAgentMovementCompleted: Boolean = false
var gMaxAgentGroups: Int = 0
var gStartTextureHandle: Any? = null

object Startup {

    var startupState: StartupState = StartupState.STATE_FIRST
        private set

    private var startSLURL: String = ""
    private var startSLURLString: String = ""
    private var initialOutfit: String = ""
    private var initialOutfitGender: String = ""

    private val stateListeners: MutableList<(StartupState) -> Unit> = mutableListOf()

    fun setStartupState(state: StartupState) {
        stopPhase(startupStateToString(startupState))
        startupState = state
        startPhase(startupStateToString(state))
        postStartupState()
    }

    fun getStartupStateString(): String = startupStateToString(startupState)

    fun getScreenLastFilename(): String = TODO("APR: use JVM equivalent - build screen_last<suffix>.png path")

    fun getScreenHomeFilename(): String = TODO("APR: use JVM equivalent - build screen_home<suffix>.png path")

    fun multimediaInit() {
        TODO("APR: use JVM equivalent - initialise LLViewerMedia / streaming audio")
    }

    fun fontInit() {
        TODO("APR: use JVM equivalent - load default fonts not already loaded at start screen")
    }

    fun initNameCache() {
        TODO("APR: use JVM equivalent - create LLAvatarNameCache and LLCacheName instances")
    }

    fun initExperiences() {
        TODO("APR: use JVM equivalent - initialise LLExperienceCache")
    }

    fun cleanupNameCache() {
        TODO("APR: use JVM equivalent - shut down LLAvatarNameCache and LLCacheName")
    }

    fun loadInitialOutfit(outfitFolderName: String, genderName: String) {
        initialOutfit = outfitFolderName
        initialOutfitGender = genderName
        TODO("APR: use JVM equivalent - locate outfit folder in inventory, wear it; fall back to standard wearables")
    }

    fun getInitialOutfitName(): String = initialOutfit

    fun getUserId(): String = TODO("APR: use JVM equivalent - return gUserCredential.userID() or empty string")

    fun dispatchURL(): Boolean = TODO("APR: use JVM equivalent - dispatch pending SLURL / sim string via LLURLDispatcher")

    fun postStartupState() {
        for (listener in stateListeners) {
            listener(startupState)
        }
        TODO("APR: use JVM equivalent - post state change event to sStateWatcher pump")
    }

    fun addStateListener(listener: (StartupState) -> Unit) {
        stateListeners.add(listener)
    }

    fun setStartSLURL(slurl: String) {
        startSLURL = slurl
        TODO("APR: use JVM equivalent - update login panel via PanelLogin.onUpdateStartSLURL")
    }

    fun getStartSLURL(): String = startSLURL

    fun setStartSLURLString(slurlString: String) {
        startSLURLString = slurlString
    }

    fun getStartSLURLString(): String = startSLURLString

    fun startLLProxy(): Boolean = TODO("APR: use JVM equivalent - configure SOCKS5 proxy from settings")

    fun idleStartup(): Boolean = TODO("APR: use JVM equivalent - run one step of the startup state machine")

    fun releaseStartScreen() {
        gStartTextureHandle = null
    }

    private fun startupStateToString(state: StartupState): String = state.name

    private fun startPhase(name: String) {
        TODO("APR: use JVM equivalent - sPhases.startPhase($name)")
    }

    private fun stopPhase(name: String) {
        TODO("APR: use JVM equivalent - sPhases.stopPhase($name)")
    }
}
