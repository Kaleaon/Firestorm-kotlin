/**
 * AgentData.kt
 * Kotlin port of llagentdata.h / llagentdata.cpp
 *
 * Contains commonly used agent identity data, originally held as C++ global
 * variables (gAgentID, gAgentSessionID, gAgentUsername).  In Kotlin these
 * become mutable properties on a singleton object, keeping the same
 * single-instance semantics without relying on process-global state.
 *
 * Original author: James Cook
 * Copyright (C) 2010, Linden Research, Inc.  LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

/**
 * Singleton holding the core identity of the currently logged-in agent.
 *
 * C++ equivalents
 * ---------------
 *  gAgentID        → agentId
 *  gAgentSessionID → sessionId
 *  gAgentUsername  → firstName / lastName  (split for convenience)
 *
 * The C++ translation unit also declared a bare `gAgentUsername` string; the
 * Firestorm viewer stores first and last names separately elsewhere (LLAgent),
 * so we model both the split fields and the legacy combined username here.
 */
object AgentData {

    /** Unique identifier for this agent (avatar). Null UUID before login. */
    var agentId: LLUUID = LLUUID.NULL

    /** Session token issued by the login server for this session. */
    var sessionId: LLUUID = LLUUID.NULL

    /**
     * Secure session token used for capability authentication.
     * Not exposed in the original llagentdata.h but logically belongs here
     * alongside the other session UUIDs.
     */
    var secureSessionId: LLUUID = LLUUID.NULL

    /** Agent's first name as returned by the login response. */
    var firstName: String = ""

    /** Agent's last name as returned by the login response. */
    var lastName: String = ""

    /**
     * Legacy flat username string (corresponds to C++ gAgentUsername).
     * Typically "firstname.lastname" in the SL grid naming convention.
     */
    var agentUsername: String = ""

    /**
     * Display name built from [firstName] and [lastName].
     * Mirrors the common C++ pattern `gAgent.getFullname()`.
     */
    val fullName: String
        get() = "$firstName $lastName"

    /**
     * True once [agentId] has been set to a non-null UUID, i.e. after a
     * successful login response has been processed.
     */
    val isLoggedIn: Boolean
        get() = agentId != LLUUID.NULL

    /**
     * Reset all fields to their initial (logged-out) values.
     * Call this on logout or before a new login attempt.
     */
    fun reset() {
        agentId = LLUUID.NULL
        sessionId = LLUUID.NULL
        secureSessionId = LLUUID.NULL
        firstName = ""
        lastName = ""
        agentUsername = ""
    }
}
