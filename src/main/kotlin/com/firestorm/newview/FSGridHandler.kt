package com.firestorm.newview

import java.util.UUID

const val DEFAULT_LOGIN_PAGE: String = "https://phoenixviewer.com/app/loginV3/"

const val KNOWN_GRIDS_SIZE: Int = 3

const val GRID_VALUE                  = "name"
const val GRID_LABEL_VALUE            = "gridname"
const val GRID_ID_VALUE               = "grid_login_id"
const val GRID_LOGIN_URI_VALUE        = "loginuri"
const val GRID_UPDATE_SERVICE_URL     = "update_query_url_base"
const val GRID_HELPER_URI_VALUE       = "helperuri"
const val GRID_LOGIN_PAGE_VALUE       = "loginpage"
const val GRID_IS_SYSTEM_GRID_VALUE   = "system_grid"
const val GRID_IS_FAVORITE_VALUE      = "favorite"
const val GRID_LOGIN_IDENTIFIER_TYPES = "login_identifier_types"

const val GRID_NICK_VALUE            = "gridnick"
const val GRID_REGISTER_NEW_ACCOUNT  = "register"
const val GRID_FORGOT_PASSWORD       = "password"
const val GRID_HELP                  = "help"
const val GRID_ABOUT                 = "about"
const val GRID_SEARCH                = "search"
const val GRID_WEB_PROFILE_VALUE     = "web_profile_url"
const val GRID_SENDGRIDINFO          = "SendGridInfoToViewerOnLogin"
const val GRID_DIRECTORY_FEE         = "DirectoryFee"
const val GRID_PLATFORM              = "platform"
const val GRID_MESSAGE               = "message"
const val GRID_SLURL_BASE            = "slurl_base"
const val GRID_APP_SLURL_BASE        = "app_slurl_base"

const val MAINGRID = "login.agni.lindenlab.com"

const val SYSTEM_GRID_APP_SLURL_BASE = "secondlife:///app"
const val MAIN_GRID_SLURL_BASE       = "https://maps.secondlife.com/secondlife/"

data class GridEntry(
    val grid: MutableMap<String, Any> = mutableMapOf(),
    var setCurrent: Boolean = false,
    var lastHttpError: String = ""
)

class LLInvalidGridName(val grid: String) : Exception("Invalid grid name: $grid")

object LLGridManager {

    enum class AddState {
        FETCH, FETCHTEMP, SYSTEM, MANUAL, RETRY, LOCAL, FINISH, TRYLEGACY, FAIL, REMOVE
    }

    private enum class GridPlatform {
        GP_NOTSET, GP_SLMAIN, GP_SLBETA, GP_OPENSIM, GP_AURORA
    }

    private var gridPlatform: GridPlatform = GridPlatform.GP_NOTSET

    var grid: String = ""
        private set
    private var gridFile: String = ""
    private var startupGrid: String = ""
    private val gridList: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()
    private val connectedGrid: MutableMap<String, Any> = mutableMapOf()
    private var responderCount: Int = 0
    var readyToLogin: Boolean = false
        private set
    private var commandLineDone: Boolean = false
    private var classifiedFee: Int = 0
    private var directoryFee: Int = 0

    private val gridListChangedListeners: MutableList<(Boolean) -> Unit> = mutableListOf()

    fun addGridListChangedCallback(cb: (Boolean) -> Unit): () -> Unit {
        gridListChangedListeners.add(cb)
        return { gridListChangedListeners.remove(cb) }
    }

    fun initGrids() {
        TODO("APR: use JVM equivalent — clear gridList, resolve grids.xml / grids.user.xml / grids.remote.xml paths, call initSystemGrids + initGridList for each file, then initCmdLineGrids; apply startupGrid if set")
    }

    fun initSystemGrids() {
        addSystemGrid("Loading...", "", "", "", "", DEFAULT_LOGIN_PAGE)
    }

    fun initGridList(gridFile: String, state: AddState) {
        TODO("APR: use JVM equivalent — open and parse LLSD XML grid file; for each map entry construct a GridEntry and call addGrid(entry, state)")
    }

    fun initCmdLineGrids() {
        commandLineDone = true
        TODO("APR: use JVM equivalent — read CmdLineGridChoice and CurrentGrid settings; resolve grid by nick/label/name or default to MAINGRID; call setGridChoice or addGrid(FETCH)")
    }

    fun resetGrids() {
        initGrids()
        if (grid.isNotEmpty()) {
            gridList[grid]?.putAll(connectedGrid)
        }
    }

    fun addGrid(loginUri: String) {
        val entry = GridEntry(setCurrent = true)
        entry.grid[GRID_VALUE] = loginUri
        addGrid(entry, AddState.FETCH)
    }

    fun addGrid(gridEntry: GridEntry?, state: AddState) {
        TODO("APR: use JVM equivalent — validate entry; handle FETCH/FETCHTEMP (HTTP GET get_grid_info via JVM HttpClient), TRYLEGACY (HTTP GET legacy login.cgi), SYSTEM (populate default URIs), FINISH (merge into gridList), FAIL/REMOVE; fire gridListChangedListeners on list change")
    }

    fun removeGrid(gridName: String) {
        val entry = GridEntry(setCurrent = false)
        entry.grid[GRID_VALUE] = gridName
        entry.grid["USER_DELETED"] = "TRUE"
        addGrid(entry, AddState.REMOVE)
    }

    fun reFetchGrid() {
        reFetchGrid(grid, true)
    }

    fun reFetchGrid(gridName: String, setCurrent: Boolean = false) {
        val entry = GridEntry(setCurrent = setCurrent)
        entry.grid[GRID_VALUE] = gridName
        addGrid(entry, AddState.FETCH)
    }

    fun getKnownGrids(): MutableMap<String, String> {
        val result: MutableMap<String, String> = mutableMapOf()
        for ((key, value) in gridList) {
            if (!value.containsKey("DEPRECATED") && !value.containsKey("USER_DELETED")) {
                result[key] = value[GRID_LABEL_VALUE] as? String ?: ""
            }
        }
        return result
    }

    fun getGridData(gridName: String, gridInfo: MutableMap<String, Any>) {
        gridList[gridName]?.let { gridInfo.putAll(it) }
    }

    fun getGridData(gridInfo: MutableMap<String, Any>) {
        getGridData(grid, gridInfo)
    }

    fun setGridChoice(gridName: String) {
        if (gridName.isEmpty()) return
        TODO("APR: use JVM equivalent — check login auth state; if gridList empty store in startupGrid for deferred application; resolve by nick/label/name; if found set mGrid, persist CurrentGrid setting, update CURRENT_GRID translation arg, call updateIsInProductionGrid(); otherwise fetch")
    }

    fun getGrid(gridName: String): String {
        return if (gridList.containsKey(gridName)) gridName else getGridByProbing(gridName)
    }

    fun getGridId(gridName: String): String {
        val resolved = getGrid(gridName)
        return if (resolved.isNotEmpty()) gridList[resolved]?.get(GRID_NICK_VALUE) as? String ?: "" else ""
    }

    fun getGridId(): String = getGridId(grid)

    fun getGridLabel(gridName: String): String {
        val resolved = getGrid(gridName)
        return if (resolved.isNotEmpty()) gridList[resolved]?.get(GRID_LABEL_VALUE) as? String ?: "" else ""
    }

    fun getGridLabel(): String = getGridLabel(grid)

    fun getGrid(): String = grid

    fun getLoginURI(gridName: String): String {
        val uris = gridList[gridName]?.get(GRID_LOGIN_URI_VALUE)
        return when (uris) {
            is List<*> -> uris.firstOrNull() as? String ?: ""
            is String  -> uris
            else       -> ""
        }
    }

    fun getLoginURIs(uris: MutableList<String>) {
        uris.clear()
        val raw = gridList[grid]?.get(GRID_LOGIN_URI_VALUE)
        when (raw) {
            is List<*> -> raw.filterIsInstance<String>().filterTo(uris) { it.isNotEmpty() }
            is String  -> if (raw.isNotEmpty()) uris.add(raw)
        }
    }

    fun getHelperURI(): String {
        TODO("APR: use JVM equivalent — check CmdLineHelperURI setting and LFSimFeatureHandler override before falling back to gridList entry")
    }

    fun getLoginPage(): String {
        TODO("APR: use JVM equivalent — check LoginPage setting override before falling back to gridList entry")
    }

    fun getGridLoginID(): String = gridList[grid]?.get(GRID_ID_VALUE) as? String ?: ""

    fun getLoginPage(gridName: String): String = gridList[gridName]?.get(GRID_LOGIN_PAGE_VALUE) as? String ?: ""

    fun getLoginIdentifierTypes(idTypes: MutableList<String>) {
        idTypes.clear()
        val raw = gridList[grid]?.get(GRID_LOGIN_IDENTIFIER_TYPES)
        if (raw is List<*>) idTypes.addAll(raw.filterIsInstance<String>())
    }

    fun trimHypergrid(trim: String): String {
        val pos = trim.lastIndexOf(':')
        if (pos == -1) return trim
        val part = trim.substring(pos + 1)
        return if (part.any { !it.isDigit() && it != '/' }) trim.substring(0, pos) else trim
    }

    fun getSLURLBase(gridName: String): String {
        val trimmed = trimHypergrid(gridName)
        val entry = gridList[trimmed]
        return if (entry != null && entry.containsKey(GRID_SLURL_BASE)) {
            entry[GRID_SLURL_BASE] as? String ?: ""
        } else {
            val norm = gridName.trimEnd('/')
            "hop://$norm/"
        }
    }

    fun getSLURLBase(): String = getSLURLBase(grid)

    fun getAppSLURLBase(gridName: String): String {
        val entry = gridList[gridName]
        if (entry != null && entry.containsKey(GRID_APP_SLURL_BASE)) {
            return entry[GRID_APP_SLURL_BASE] as? String ?: ""
        }
        val appBase = if (entry != null && (entry[GRID_SLURL_BASE] as? String)?.startsWith("hop://") == true) {
            "hop://%s/app"
        } else {
            "x-grid-location-info://%s/app"
        }
        val norm = gridName.trimEnd('/')
        return appBase.format(norm)
    }

    fun getAppSLURLBase(): String = getAppSLURLBase(grid)

    fun getWebProfileURL(gridName: String): String {
        val resolved = getGrid(gridName)
        return if (resolved.isNotEmpty()) gridList[resolved]?.get(GRID_WEB_PROFILE_VALUE) as? String ?: "" else ""
    }

    fun getWebProfileURL(): String = getWebProfileURL(grid)

    fun setWebProfileUrl(url: String) {
        gridList[grid]?.put(GRID_WEB_PROFILE_VALUE, url)
    }

    fun hasGrid(gridName: String): Boolean = gridList.containsKey(gridName)

    fun isTemporary(): Boolean = gridList[grid]?.containsKey("FLAG_TEMPORARY") ?: false

    fun isTemporary(gridName: String): Boolean = gridList[gridName]?.containsKey("FLAG_TEMPORARY") ?: false

    fun isHyperGrid(gridName: String): Boolean = gridList[gridName]?.containsKey("HG") ?: false

    fun getGatekeeper(): String = getGatekeeper(grid)

    fun getGatekeeper(gridName: String): String = gridList[gridName]?.get("gatekeeper") as? String ?: ""

    fun getGridByLabel(gridLabel: String, caseSensitive: Boolean = false): String =
        if (gridLabel.isEmpty()) "" else getGridByAttribute(GRID_LABEL_VALUE, gridLabel, caseSensitive)

    fun getGridByProbing(probeFor: String, caseSensitive: Boolean = false): String {
        var ret = getGridByHostName(probeFor, caseSensitive)
        if (ret.isEmpty()) ret = getGridByGridNick(probeFor, caseSensitive)
        if (ret.isEmpty()) ret = getGridByLabel(probeFor, caseSensitive)
        return ret
    }

    fun getGridByGridNick(gridNick: String, caseSensitive: Boolean = false): String =
        if (gridNick.isEmpty()) "" else getGridByAttribute(GRID_NICK_VALUE, gridNick, caseSensitive)

    fun getGridByHostName(hostName: String, caseSensitive: Boolean = false): String =
        if (hostName.isEmpty()) "" else getGridByAttribute(GRID_VALUE, hostName, caseSensitive)

    fun getGridByAttribute(attribute: String, attributeValue: String, caseSensitive: Boolean): String {
        if (attribute.isEmpty() || attributeValue.isEmpty()) return ""
        for ((key, value) in gridList) {
            val candidate = value[attribute] as? String ?: continue
            val match = if (caseSensitive) candidate == attributeValue
                        else candidate.equals(attributeValue, ignoreCase = true)
            if (match) return key
        }
        return ""
    }

    fun isSystemGrid(gridName: String): Boolean =
        gridList[gridName]?.let {
            it.containsKey(GRID_IS_SYSTEM_GRID_VALUE) && (it[GRID_IS_SYSTEM_GRID_VALUE] as? Boolean == true)
        } ?: false

    fun isSystemGrid(): Boolean = isSystemGrid(grid)

    fun isInSecondLife(): Boolean = gridPlatform == GridPlatform.GP_SLMAIN || gridPlatform == GridPlatform.GP_SLBETA

    fun isInSLMain(): Boolean = gridPlatform == GridPlatform.GP_SLMAIN

    fun isInSLBeta(): Boolean = gridPlatform == GridPlatform.GP_SLBETA

    fun isInOpenSim(): Boolean = gridPlatform == GridPlatform.GP_OPENSIM || gridPlatform == GridPlatform.GP_AURORA

    fun isInAuroraSim(): Boolean = gridPlatform == GridPlatform.GP_AURORA

    fun setClassifiedFee(fee: Int) { classifiedFee = fee }
    fun getClassifiedFee(): Int = classifiedFee

    fun setDirectoryFee(fee: Int) { directoryFee = fee }
    fun getDirectoryFee(): Int = directoryFee

    fun getUpdateServiceURL(): String {
        TODO("APR: use JVM equivalent — check SL_UPDATE_SERVICE env var, CmdLineUpdateService setting, then gridList GRID_UPDATE_SERVICE_URL entry")
    }

    fun saveGridList() {
        TODO("APR: use JVM equivalent — filter gridList to exclude DEPRECATED/FLAG_TEMPORARY entries, serialize to LLSD XML and write to mGridFile using JVM file I/O")
    }

    protected fun updateIsInProductionGrid() {
        gridPlatform = GridPlatform.GP_NOTSET
        val uris = mutableListOf<String>()
        getLoginURIs(uris)
        if (uris.isEmpty()) return

        val authority = uris[0].lowercase().removePrefix("https://").removePrefix("http://").substringBefore("/")
        when {
            authority.startsWith("login.agni.lindenlab.com") -> { gridPlatform = GridPlatform.GP_SLMAIN; return }
            authority.contains("lindenlab.com")              -> { gridPlatform = GridPlatform.GP_SLBETA; return }
        }

        if (gridList[grid]?.get(GRID_PLATFORM) as? String == "Aurora") {
            gridPlatform = GridPlatform.GP_AURORA
            return
        }

        val loginPageAuthority = getLoginPage().lowercase().removePrefix("https://").removePrefix("http://").substringBefore("/")
        if (loginPageAuthority.contains("lindenlab.com")) {
            setGridChoice(MAINGRID)
            return
        }

        gridPlatform = GridPlatform.GP_OPENSIM
    }

    protected fun addSystemGrid(
        label: String,
        name: String,
        nick: String,
        loginUri: String,
        helper: String,
        loginPage: String
    ) {
        val entry = GridEntry(setCurrent = false)
        entry.grid[GRID_VALUE]            = name
        entry.grid[GRID_LABEL_VALUE]      = label
        entry.grid[GRID_NICK_VALUE]       = nick
        entry.grid[GRID_HELPER_URI_VALUE] = helper
        entry.grid[GRID_LOGIN_URI_VALUE]  = mutableListOf(loginUri)
        entry.grid[GRID_LOGIN_PAGE_VALUE]       = loginPage
        entry.grid[GRID_IS_SYSTEM_GRID_VALUE]   = true
        entry.grid[GRID_LOGIN_IDENTIFIER_TYPES] = mutableListOf("agent")
        entry.grid[GRID_APP_SLURL_BASE]         = SYSTEM_GRID_APP_SLURL_BASE

        if (name == MAINGRID) {
            entry.grid[GRID_SLURL_BASE]        = MAIN_GRID_SLURL_BASE
            entry.grid[GRID_IS_FAVORITE_VALUE] = true
        } else {
            entry.grid[GRID_SLURL_BASE] = "secondlife://$label/secondlife/"
        }

        try {
            addGrid(entry, AddState.SYSTEM)
        } catch (_: LLInvalidGridName) {
        }
    }

    private fun gridInfoResponderCB(gridEntry: GridEntry) {
        TODO("APR: use JVM equivalent — parse XML info_root for login/gridname/gridnick/gatekeeper/welcome/register/password/help/about/search/web_profile_url/economy/helperuri/platform/message fields; set GRID_SLURL_BASE; call addGrid(FINISH)")
    }

    private fun incResponderCount() { responderCount++ }
    private fun decResponderCount() { responderCount-- }
}

class FSGridManagerCommandHandler {

    private var downloadConnection: (() -> Unit)? = null

    fun handle(params: List<String>, queryMap: Map<String, String>, grid: String): Boolean {
        if (params.size < 2) return false

        if (params[0] == "addgrid") {
            TODO("APR: use JVM equivalent — URL-decode params[1], register grid download callback, call LLGridManager.addGrid(loginUri)")
        }

        return false
    }

    private fun handleGridDownloadComplete(success: Boolean) {
        downloadConnection?.invoke()
        downloadConnection = null

        if (success) {
            LLGridManager.saveGridList()
            TODO("APR: use JVM equivalent — if startup state <= STATE_LOGIN_WAIT call FSPanelLogin.updateServer()")
        }
    }
}

val gFSGridManagerCommandHandler = FSGridManagerCommandHandler()
