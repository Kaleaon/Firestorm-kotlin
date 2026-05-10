/**
 * LoginHandler.kt
 * Converted from llloginhandler.h / llloginhandler.cpp
 *
 * Handles filling in the login panel information from a SLURL such as
 * secondlife:///app/login?first=Bob&last=Dobbs
 *
 * Original: Copyright (C) 2010, Linden Research, Inc. (LGPL v2.1)
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// LoginStatus — mirrors the viewer's startup-state concept for login flow
// ---------------------------------------------------------------------------

enum class LoginStatus {
    NOT_STARTED,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED
}

// ---------------------------------------------------------------------------
// LoginCredential — thin wrapper for identifier + authenticator LLSD maps
// ---------------------------------------------------------------------------

data class LoginCredential(
    val identifier: MutableMap<String, String> = mutableMapOf(),
    val authenticator: MutableMap<String, String> = mutableMapOf()
)

// ---------------------------------------------------------------------------
// LoginHandler — singleton (maps to the C++ global gLoginHandler instance)
//
// In C++ this is a LLCommandHandler subclass that auto-registers with the
// LLCommandDispatcher for the "login" SLURL app.  Here it is an object that
// provides the same surface area without the registration machinery.
// ---------------------------------------------------------------------------

object LoginHandler {

    // Current login status — consumers can observe this property.
    var loginStatus: LoginStatus = LoginStatus.NOT_STARTED
        private set

    // Last failure reason, set by handleLoginFailed().
    var lastFailureReason: String = ""
        private set

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Handle an incoming "login" SLURL command dispatched from an external
     * browser or the OS URL-handler.  Mirrors LLLoginHandler::handle().
     *
     * @param queryMap  parsed query parameters from the SLURL
     * @param grid      grid identifier string (may be empty)
     * @return true if the command was consumed
     */
    fun handle(queryMap: MutableMap<String, String>, grid: String): Boolean {
        if (loginStatus == LoginStatus.SUCCEEDED) {
            // Already logged in — ignore the request.
            return true
        }
        parse(queryMap)
        TODO("Integrate with startup-state machine and login panel")
    }

    /**
     * Parse a direct-login URL of the form
     * secondlife:///app/login?first=Bob&last=Dobbs
     * and extract login credentials / start location from it.
     *
     * Mirrors LLLoginHandler::parseDirectLogin().
     */
    fun parseDirectLogin(url: String): Boolean {
        val queryParams = parseQueryString(url)
        parse(queryParams)
        // NOTE: identity-evolution direct-login token parsing goes here.
        return true
    }

    /**
     * Called by the login subsystem when authentication completes
     * successfully.  Mirrors the success path in the C++ login flow.
     */
    fun handleLoginComplete() {
        loginStatus = LoginStatus.SUCCEEDED
        lastFailureReason = ""
        TODO("Notify UI / startup-state machine of successful login")
    }

    /**
     * Called by the login subsystem when authentication fails.
     * Mirrors the failure path in the C++ login flow.
     *
     * @param reason human-readable failure description
     */
    fun handleLoginFailed(reason: String) {
        loginStatus = LoginStatus.FAILED
        lastFailureReason = reason
        TODO("Notify UI / startup-state machine of failed login")
    }

    /**
     * Dispatch a secondlife:// (SLurl) to the appropriate handler.
     * Mirrors the LLCommandDispatcher routing that the C++ LLCommandHandler
     * base class provides automatically.
     *
     * @param url the raw secondlife:// URL
     */
    fun dispatchSLURL(url: String) {
        if (!url.startsWith("secondlife://")) {
            return
        }
        // Delegate to handle() after parsing the query map.
        val queryParams = parseQueryString(url)
        val grid = queryParams.getOrDefault("grid", "")
        handle(queryParams, grid)
    }

    /**
     * Load the saved user login info (e.g. from command-line flags or the
     * protected credential store) and return it as a [LoginCredential].
     * Mirrors LLLoginHandler::loadSavedUserLoginInfo().
     */
    fun loadSavedUserLoginInfo(): LoginCredential? {
        TODO("Read UserLoginInfoCmdLine setting; MD5-hash password; build credential")
    }

    /**
     * Initialize login info from URL / command-line / credential store.
     * Always returns a [LoginCredential] (possibly empty).
     * Mirrors LLLoginHandler::initializeLoginInfo().
     */
    fun initializeLoginInfo(): LoginCredential {
        return loadSavedUserLoginInfo() ?: LoginCredential()
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Extract grid choice and start-location from a parsed query map and
     * apply them to the startup state.
     * Mirrors LLLoginHandler::parse().
     */
    private fun parse(queryMap: MutableMap<String, String>) {
        queryMap["grid"]?.let { grid ->
            TODO("Call GridManager.setGridChoice($grid)")
        }

        when (queryMap["location"]) {
            "specify" -> {
                val region = queryMap.getOrDefault("region", "")
                TODO("Call StartUp.setStartSLURL(SLURL(gridLoginId, region=$region))")
            }
            "home"    -> TODO("Call StartUp.setStartSLURL(SLURL(SIM_LOCATION_HOME))")
            "last"    -> TODO("Call StartUp.setStartSLURL(SLURL(SIM_LOCATION_LAST))")
        }
    }

    /**
     * Minimal query-string parser.  Splits on '?' then '&' and '='.
     * The C++ code delegates this to LLURI::queryMap().
     */
    private fun parseQueryString(url: String): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        val queryPart = url.substringAfter('?', "")
        if (queryPart.isBlank()) return result
        queryPart.split('&').forEach { pair ->
            val (key, value) = pair.split('=', limit = 2).let {
                it[0] to (it.getOrNull(1) ?: "")
            }
            result[key] = value
        }
        return result
    }
}
