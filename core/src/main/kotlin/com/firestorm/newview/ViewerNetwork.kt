/**
 * ViewerNetwork.kt
 * Kotlin port of llviewernetwork.h / llviewernetwork.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * License: GNU Lesser General Public License v2.1
 */

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

// ---------------------------------------------------------------------------
// Well-known grid DNS names (mirrors C++ constants)
// ---------------------------------------------------------------------------

/** DNS key for the Second Life main (production) grid. */
const val MAINGRID: String = "util.agni.lindenlab.com"

/** Human-readable label for the main grid. */
const val SECOND_LIFE_MAIN_LABEL: String = "Second Life"

/** Human-readable label for the beta / staging grid. */
const val SECOND_LIFE_BETA_LABEL: String = "Second Life Beta"

// ---------------------------------------------------------------------------
// GridInfo data class
// ---------------------------------------------------------------------------

/**
 * Holds all metadata for a single grid entry.
 *
 * Corresponds to the LLSD map stored per grid in C++ LLGridManager::mGridList.
 *
 * @param name          DNS key used to identify this grid (e.g. "util.agni.lindenlab.com").
 * @param label         User-visible long-form name shown in the grid selector.
 * @param gridId        Short-form identifier used in URL path components (e.g. "Agni").
 * @param loginUris     One or more login CGI URIs (the auth end-point).
 * @param helperUri     Base URI for web helper functions.
 * @param loginPage     Splash page URL shown before login.
 * @param updateUrlBase Base URL for the software-update service.
 * @param webProfileUrl Base URL for the resident profile web site.
 * @param slUrlBase     Base URL for SLURLs (region map links).
 * @param appSlUrlBase  Base URL for app-protocol SLURLs (secondlife:///app/…).
 * @param isSystemGrid  True for the hard-coded Linden grids (Agni / Aditi).
 */
data class GridInfo(
    val name: String,
    val label: String,
    val gridId: String = name,
    val loginUris: List<String> = emptyList(),
    val helperUri: String = "",
    val loginPage: String = "",
    val updateUrlBase: String = "",
    val webProfileUrl: String = "",
    val slUrlBase: String = "",
    val appSlUrlBase: String = "secondlife:///app",
    val isSystemGrid: Boolean = false,
)

// ---------------------------------------------------------------------------
// Exception
// ---------------------------------------------------------------------------

/** Thrown when an operation is requested for a grid that is not registered. */
class InvalidGridNameException(val gridName: String) :
    Exception("Unknown or invalid grid: '$gridName'")

// ---------------------------------------------------------------------------
// ViewerNetwork singleton
// ---------------------------------------------------------------------------

/**
 * Singleton that manages the list of known grids and the currently-selected
 * grid.  Mirrors C++ LLGridManager (an LLSingleton).
 *
 * Default grids (Agni and Aditi) are pre-populated on construction, matching
 * the hard-coded entries in LLGridManager::initialize().
 */
object ViewerNetwork {

    // -----------------------------------------------------------------------
    // Well-known URL constants (mirrors llviewernetwork.cpp top-level consts)
    // -----------------------------------------------------------------------

    private const val MAIN_GRID_LOGIN_URI =
        "https://login.agni.lindenlab.com/cgi-bin/login.cgi"
    private const val SL_UPDATE_QUERY_URL =
        "https://update.secondlife.com/update"
    private const val MAIN_GRID_SLURL_BASE =
        "https://maps.secondlife.com/secondlife/"
    private const val MAIN_GRID_WEB_PROFILE_URL =
        "https://my.secondlife.com/"
    private const val DEFAULT_LOGIN_PAGE =
        "https://phoenixviewer.com/app/loginV3/"
    private const val SYSTEM_GRID_APP_SLURL_BASE =
        "secondlife:///app"

    // -----------------------------------------------------------------------
    // Internal state
    // -----------------------------------------------------------------------

    /** Grid registry keyed by [GridInfo.name] (the DNS key). */
    private val gridList: MutableMap<String, GridInfo> = mutableMapOf()

    /** DNS key of the currently-selected grid. */
    private var currentGridName: String = MAINGRID

    // -----------------------------------------------------------------------
    // Initialisation — populate default grids
    // -----------------------------------------------------------------------

    init {
        addSystemGrid(
            label = SECOND_LIFE_MAIN_LABEL,
            name = MAINGRID,
            loginUri = MAIN_GRID_LOGIN_URI,
            helper = "https://secondlife.com/helpers/",
            loginPage = DEFAULT_LOGIN_PAGE,
            updateUrlBase = SL_UPDATE_QUERY_URL,
            webProfileUrl = MAIN_GRID_WEB_PROFILE_URL,
            loginId = "Agni",
            slUrlBase = MAIN_GRID_SLURL_BASE,
        )
        addSystemGrid(
            label = SECOND_LIFE_BETA_LABEL,
            name = "util.aditi.lindenlab.com",
            loginUri = "https://login.aditi.lindenlab.com/cgi-bin/login.cgi",
            helper = "https://secondlife.aditi.lindenlab.com/helpers/",
            loginPage = DEFAULT_LOGIN_PAGE,
            updateUrlBase = SL_UPDATE_QUERY_URL,
            webProfileUrl = "https://my.secondlife-beta.com/",
            loginId = "Aditi",
            slUrlBase = "secondlife://Aditi/secondlife/",
        )
    }

    // -----------------------------------------------------------------------
    // Grid registration
    // -----------------------------------------------------------------------

    /**
     * Register a new grid.  If a grid with the same [GridInfo.name] (or the
     * same [GridInfo.gridId]) already exists the call is silently ignored and
     * false is returned.
     *
     * Corresponds to C++ LLGridManager::addGrid(LLSD&).
     */
    fun addGrid(info: GridInfo): Boolean {
        val key = info.name.lowercase()
        if (gridList.containsKey(key)) {
            System.err.println("ViewerNetwork: duplicate grid name '$key' ignored")
            return false
        }
        if (gridList.values.any { it.gridId.equals(info.gridId, ignoreCase = true) }) {
            System.err.println("ViewerNetwork: duplicate grid id '${info.gridId}' ignored")
            return false
        }
        gridList[key] = info
        return true
    }

    /**
     * Look up a grid by its DNS name or its short-form id (case-insensitive).
     * Returns the [GridInfo] or null if not found.
     *
     * Corresponds to C++ LLGridManager::getGrid(const std::string&).
     */
    fun getGrid(nameOrId: String): GridInfo? {
        val lower = nameOrId.lowercase()
        // Try exact name match first
        gridList[lower]?.let { return it }
        // Fall back to grid-id match
        return gridList.values.firstOrNull { it.gridId.equals(nameOrId, ignoreCase = true) }
    }

    /**
     * Return the currently-selected grid.
     * Throws [InvalidGridNameException] if the internal state is corrupt.
     */
    fun getCurrentGrid(): GridInfo =
        gridList[currentGridName.lowercase()]
            ?: throw InvalidGridNameException(currentGridName)

    /**
     * Select a grid by DNS name or short id.
     * Throws [InvalidGridNameException] when the name/id is not registered.
     *
     * Corresponds to C++ LLGridManager::setGridChoice(const std::string&).
     */
    fun setCurrentGrid(nameOrId: String) {
        val resolved = getGrid(nameOrId)
            ?: throw InvalidGridNameException(nameOrId)
        currentGridName = resolved.name.lowercase()
        // persists to saved settings ("CurrentGrid") and updates LLTrans default args when settings are ported
    }

    // -----------------------------------------------------------------------
    // Convenience accessors for the selected grid
    // -----------------------------------------------------------------------

    fun getLoginUris(): List<String> = getCurrentGrid().loginUris

    fun getHelperUri(): String = getCurrentGrid().helperUri

    fun getLoginPage(): String = getCurrentGrid().loginPage

    fun getWebProfileUrl(): String = getCurrentGrid().webProfileUrl

    fun getSLURLBase(): String = getCurrentGrid().slUrlBase

    fun getAppSLURLBase(): String = getCurrentGrid().appSlUrlBase

    fun getGridLabel(): String = getCurrentGrid().label

    fun getGridId(): String = getCurrentGrid().gridId

    fun getUpdateServiceUrl(): String {
        // honours CmdLineUpdateService setting and SL_UPDATE_SERVICE env var when settings are ported
        return getCurrentGrid().updateUrlBase
    }

    // -----------------------------------------------------------------------
    // Grid classification
    // -----------------------------------------------------------------------

    /**
     * True when [grid] is one of the hard-coded Linden system grids.
     * Corresponds to C++ LLGridManager::isSystemGrid(const std::string&).
     */
    fun isSystemGrid(nameOrId: String): Boolean =
        getGrid(nameOrId)?.isSystemGrid == true

    /** True when the selected grid is a system grid. */
    fun isSystemGrid(): Boolean = getCurrentGrid().isSystemGrid

    /**
     * True when the selected grid is the main Second Life production grid.
     * Corresponds to C++ LLGridManager::isInProductionGrid().
     */
    fun isInProductionGrid(): Boolean =
        getCurrentGrid().loginUris.any { it == MAIN_GRID_LOGIN_URI }

    fun isInSLMain(): Boolean = isInProductionGrid()

    fun isInSLBeta(): Boolean = isSystemGrid() && !isInProductionGrid()

    fun isInSecondLife(): Boolean = isInSLMain() || isInSLBeta()

    // -----------------------------------------------------------------------
    // Whole-list query
    // -----------------------------------------------------------------------

    /**
     * Return a map of grid-name → grid-label for all registered grids.
     * Corresponds to C++ LLGridManager::getKnownGrids().
     */
    fun getKnownGrids(): Map<String, String> =
        gridList.mapValues { it.value.label }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Register one of the hard-coded Linden system grids.
     * Corresponds to C++ LLGridManager::addSystemGrid().
     */
    private fun addSystemGrid(
        label: String,
        name: String,
        loginUri: String,
        helper: String,
        loginPage: String,
        updateUrlBase: String,
        webProfileUrl: String,
        loginId: String,
        slUrlBase: String,
    ) {
        val info = GridInfo(
            name = name,
            label = label,
            gridId = loginId.ifEmpty { name },
            loginUris = listOf(loginUri),
            helperUri = helper,
            loginPage = loginPage,
            updateUrlBase = updateUrlBase,
            webProfileUrl = webProfileUrl,
            slUrlBase = slUrlBase,
            appSlUrlBase = SYSTEM_GRID_APP_SLURL_BASE,
            isSystemGrid = true,
        )
        gridList[name.lowercase()] = info
    }
}
