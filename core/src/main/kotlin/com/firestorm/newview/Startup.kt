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

    fun getScreenLastFilename(): String {
        System.err.println("Startup: getScreenLastFilename not yet implemented")
        return ""
    }

    fun getScreenHomeFilename(): String {
        System.err.println("Startup: getScreenHomeFilename not yet implemented")
        return ""
    }

    fun multimediaInit() {
        System.err.println("Startup: multimediaInit not yet implemented")
    }

    fun fontInit() {
        System.err.println("Startup: fontInit not yet implemented")
    }

    fun initNameCache() {
        System.err.println("Startup: initNameCache not yet implemented")
    }

    fun initExperiences() {
        System.err.println("Startup: initExperiences not yet implemented")
    }

    fun cleanupNameCache() {
        System.err.println("Startup: cleanupNameCache not yet implemented")
    }

    fun loadInitialOutfit(outfitFolderName: String, genderName: String) {
        initialOutfit = outfitFolderName
        initialOutfitGender = genderName
        System.err.println("Startup: loadInitialOutfit not yet implemented")
    }

    fun getInitialOutfitName(): String = initialOutfit

    fun getUserId(): String {
        System.err.println("Startup: getUserId not yet implemented")
        return ""
    }

    fun dispatchURL(): Boolean {
        System.err.println("Startup: dispatchURL not yet implemented")
        return false
    }

    fun postStartupState() {
        for (listener in stateListeners) {
            listener(startupState)
        }
        System.err.println("Startup: postStartupState not yet implemented")
    }

    fun addStateListener(listener: (StartupState) -> Unit) {
        stateListeners.add(listener)
    }

    fun setStartSLURL(slurl: String) {
        startSLURL = slurl
        System.err.println("Startup: setStartSLURL not yet implemented")
    }

    fun getStartSLURL(): String = startSLURL

    fun setStartSLURLString(slurlString: String) {
        startSLURLString = slurlString
    }

    fun getStartSLURLString(): String = startSLURLString

    fun startLLProxy(): Boolean {
        System.err.println("Startup: startLLProxy not yet implemented")
        return false
    }

    fun idleStartup(): Boolean {
        System.err.println("Startup: idleStartup not yet implemented")
        return false
    }

    fun releaseStartScreen() {
        gStartTextureHandle = null
    }

    private fun startupStateToString(state: StartupState): String = state.name

    private fun startPhase(name: String) {
        System.err.println("Startup: startPhase not yet implemented")
    }

    private fun stopPhase(name: String) {
        System.err.println("Startup: stopPhase not yet implemented")
    }
}
