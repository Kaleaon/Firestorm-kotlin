/**
 * AgentAccess.kt
 * Kotlin port of llagentaccess.h / llagentaccess.cpp
 *
 * Manages the agent's maturity/access level and god-mode state.  The original
 * C++ class (LLAgentAccess) was instantiated once inside LLAgent; here we use
 * a singleton object that reads preferred maturity from [PreferencesMgr].
 *
 * Original: Copyright (C) 2010, Linden Research, Inc.  LGPL 2.1
 */

package com.firestorm.newview

/**
 * Maturity / sim-access levels as defined by indra_constants.h.
 *
 * [code] matches the wire-protocol byte values used by the sim and stored in
 * saved settings as "PreferredMaturity".
 *
 * C++ originals: SIM_ACCESS_PG = 13, SIM_ACCESS_MATURE = 21, SIM_ACCESS_ADULT = 42
 */
enum class AccessLevel(val code: Int) {
    PG(13),
    MATURE(21),
    ADULT(42);

    companion object {
        /** Convert a numeric code to the matching [AccessLevel], or null if unknown. */
        fun fromCode(code: Int): AccessLevel? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Singleton that tracks the agent's current sim-access rating, preferred
 * maturity, and god-mode state.
 *
 * Preference reads/writes delegate to [PreferencesMgr] (key "PreferredMaturity"),
 * matching the C++ LLControlGroup pattern: `mSavedSettings.getU32("PreferredMaturity")`.
 */
object AgentAccess {

    // -----------------------------------------------------------------------
    // Companion — sim-access constants (mirrors indra_constants.h)
    // -----------------------------------------------------------------------

    companion object {
        /** Minimum access code — effectively "unknown / unset". */
        const val SIM_ACCESS_MIN: Int = 0

        /** General / PG access. */
        const val SIM_ACCESS_PG: Int = 13

        /** Mature access. */
        const val SIM_ACCESS_MATURE: Int = 21

        /** Adult access. */
        const val SIM_ACCESS_ADULT: Int = 42

        /** Sim is temporarily down. */
        const val SIM_ACCESS_DOWN: Int = 254

        // God levels (from indra_constants.h)
        const val GOD_NOT: UByte = 0u
        const val GOD_LIKE: UByte = 1u
        const val GOD_CUSTOMER_SERVICE: UByte = 100u
        const val GOD_LIAISON: UByte = 150u
        const val GOD_FULL: UByte = 200u
        const val GOD_MAINTENANCE: UByte = 250u

        /**
         * Convert a single-character maturity code ('P', 'M', 'A') to its
         * integer sim-access value.  Returns [SIM_ACCESS_MIN] for unrecognised input.
         *
         * C++ equivalent: LLAgentAccess::convertTextToMaturity(char)
         */
        fun convertTextToMaturity(text: Char): Int = when (text) {
            'A' -> SIM_ACCESS_ADULT
            'M' -> SIM_ACCESS_MATURE
            'P' -> SIM_ACCESS_PG
            else -> SIM_ACCESS_MIN
        }
    }

    // -----------------------------------------------------------------------
    // Private state — matches C++ mAccess / mGodLevel / mAdminOverride
    // -----------------------------------------------------------------------

    /** Current sim-access rating as received from the server (SIM_ACCESS_* code). */
    private var mAccess: Int = SIM_ACCESS_PG

    /** God level assigned by the login server. */
    private var mGodLevel: UByte = GOD_NOT

    /**
     * Client-side admin override flag.  When true [isGodlike] returns true
     * regardless of [mGodLevel].
     */
    private var mAdminOverride: Boolean = false

    // -----------------------------------------------------------------------
    // Admin / god-mode API
    // -----------------------------------------------------------------------

    fun getAdminOverride(): Boolean = mAdminOverride

    fun setAdminOverride(b: Boolean) {
        mAdminOverride = b
    }

    fun setGodLevel(godLevel: UByte) {
        mGodLevel = godLevel
    }

    fun getGodLevel(): UByte {
        if (mAdminOverride) return GOD_FULL
        return mGodLevel
    }

    /** True if the agent has any elevated privilege (admin override or non-zero god level). */
    fun isGodlike(): Boolean = mAdminOverride || mGodLevel > GOD_NOT

    /** True if the agent has a genuine server-assigned god level (ignores admin override). */
    fun isGodlikeWithoutAdminMenuFakery(): Boolean = mGodLevel > GOD_NOT

    // -----------------------------------------------------------------------
    // Preferred-maturity helpers (read from saved settings)
    // -----------------------------------------------------------------------

    /** True when the agent prefers PG content only. */
    fun prefersPG(): Boolean {
        val access = PreferencesMgr.getS32("PreferredMaturity")
        return access < SIM_ACCESS_MATURE
    }

    /** True when the agent prefers at least Mature content. */
    fun prefersMature(): Boolean {
        val access = PreferencesMgr.getS32("PreferredMaturity")
        return access >= SIM_ACCESS_MATURE
    }

    /** True when the agent prefers Adult content. */
    fun prefersAdult(): Boolean {
        val access = PreferencesMgr.getS32("PreferredMaturity")
        return access >= SIM_ACCESS_ADULT
    }

    // -----------------------------------------------------------------------
    // Account-level maturity checks (based on mAccess from server)
    // -----------------------------------------------------------------------

    /** True when the agent's account is rated below Mature (teen / PG account). */
    fun isTeen(): Boolean = mAccess < SIM_ACCESS_MATURE

    /** True when the agent's account is rated Mature or higher. */
    fun isMature(): Boolean = mAccess >= SIM_ACCESS_MATURE

    /** True when the agent's account is rated Adult. */
    fun isAdult(): Boolean = mAccess >= SIM_ACCESS_ADULT

    // -----------------------------------------------------------------------
    // Combined access predicates used by UI / world code
    // -----------------------------------------------------------------------

    /** True when the agent should see PG content only (teen or PG preference, non-god). */
    fun wantsPGOnly(): Boolean = (prefersPG() || isTeen()) && !isGodlike()

    /** True when the agent may enter Mature sims. */
    fun canAccessMature(): Boolean = isGodlike() || (prefersMature() && !isTeen())

    /** True when the agent may enter Adult sims. */
    fun canAccessAdult(): Boolean = isGodlike() || (prefersAdult() && isAdult())

    // -----------------------------------------------------------------------
    // Convenience wrappers matching the specification
    // -----------------------------------------------------------------------

    /** Current effective access level derived from [mAccess]. */
    val effectiveMaturity: AccessLevel
        get() = when {
            mAccess >= SIM_ACCESS_ADULT  -> AccessLevel.ADULT
            mAccess >= SIM_ACCESS_MATURE -> AccessLevel.MATURE
            else                         -> AccessLevel.PG
        }

    /** Preferred maturity level read from settings. */
    val preferredMaturity: AccessLevel
        get() {
            val code = PreferencesMgr.getS32("PreferredMaturity")
            return when {
                code >= SIM_ACCESS_ADULT  -> AccessLevel.ADULT
                code >= SIM_ACCESS_MATURE -> AccessLevel.MATURE
                else                      -> AccessLevel.PG
            }
        }

    /**
     * Set the preferred maturity level in saved settings.
     *
     * Only stores the value if the agent is actually allowed to access that
     * level (delegates to [canSetMaturity]).
     */
    fun setMaturity(level: AccessLevel) {
        if (canSetMaturity(level.code)) {
            PreferencesMgr.setS32("PreferredMaturity", level.code)
        }
    }

    /**
     * Set the agent's access level from a single-character code received from
     * the server ('P', 'M', 'A').  After updating [mAccess], the preferred
     * maturity is clamped downward if the agent is no longer entitled to their
     * current preference.
     *
     * C++ equivalent: LLAgentAccess::setMaturity(char)
     */
    fun setMaturityFromChar(text: Char) {
        mAccess = convertTextToMaturity(text)

        // Clamp preferred access downward until canSetMaturity passes
        var preferred = PreferencesMgr.getS32("PreferredMaturity")
        while (!canSetMaturity(preferred)) {
            preferred = if (preferred == SIM_ACCESS_ADULT) {
                SIM_ACCESS_MATURE
            } else {
                SIM_ACCESS_PG
            }
        }
        PreferencesMgr.setS32("PreferredMaturity", preferred)
    }

    /**
     * True when the agent is allowed to set their preferred maturity to
     * [maturity].  Gods and Adults may always set any level; otherwise only PG
     * (and Mature if [isMature]) are permitted.
     *
     * C++ equivalent: LLAgentAccess::canSetMaturity(S32)
     */
    fun canSetMaturity(maturity: Int): Boolean {
        if (isGodlike()) return true
        if (isAdult()) return true
        return maturity == SIM_ACCESS_PG || (maturity == SIM_ACCESS_MATURE && isMature())
    }

    /** Convenience overload accepting an [AccessLevel] enum value. */
    fun canAccessMaturity(level: AccessLevel): Boolean = when (level) {
        AccessLevel.PG     -> true
        AccessLevel.MATURE -> canAccessMature()
        AccessLevel.ADULT  -> canAccessAdult()
    }

    /** True when the agent is currently in an Adult-rated sim. */
    fun isInAdultSim(): Boolean = mAccess >= SIM_ACCESS_ADULT

    /** True when the agent is currently in a Mature-or-higher-rated sim. */
    fun isInMatureSim(): Boolean = mAccess >= SIM_ACCESS_MATURE
}
