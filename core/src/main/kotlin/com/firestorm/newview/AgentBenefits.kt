/**
 * AgentBenefits.kt
 * Kotlin port of llagentbenefits.h / llagentbenefits.cpp
 *
 * Models the premium-account benefit packages returned by the login/capability
 * server.  The C++ code used LLSingleton<LLAgentBenefitsMgr> to manage a map
 * of named packages plus a "current" package pointer.  Here we use idiomatic
 * Kotlin: a plain data class for a package, and companion-object factory
 * methods on the [AgentBenefits] singleton that replaces the manager class.
 *
 * Original: Copyright (C) 2019, Linden Research, Inc.  LGPL 2.1
 */

package com.firestorm.newview

/**
 * A snapshot of the limits/costs associated with one benefit tier
 * (e.g. "Base", "Premium", "PremiumPlus").
 *
 * All integer fields initialise to -1 (C++ convention for "not yet set").
 * Costs are in Linden Dollars (L$).
 *
 * C++ equivalent: LLAgentBenefits (the data-holding inner class, not the mgr)
 */
data class BenefitPackage(
    /** Display name of this tier, e.g. "Base", "Premium", "PremiumPlus". */
    val name: String,

    /** Maximum number of animated-object attachments allowed (-1 = unlimited / not set). */
    val animatedObjectLimit: Int = -1,

    /** Cost in L$ to upload an animation asset. */
    val animationUploadCost: Int = -1,

    /** Maximum simultaneous attachment count. */
    val attachmentLimit: Int = -1,

    /** Cost in L$ to create a new group. */
    val createGroupCost: Int = -1,

    /** Maximum number of groups this tier can join. */
    val groupLimit: Int = -1,

    /** Maximum number of profile picks. */
    val picksLimit: Int = -1,

    /** Cost in L$ to upload a sound asset. */
    val soundUploadCost: Int = -1,

    /** Cost in L$ to upload a standard (≤ 1 K²) texture. */
    val textureUploadCost: Int = -1,

    /**
     * Sorted list of costs for large (≥ [AgentBenefits.MIN_2K_TEXTURE_AREA])
     * texture uploads.  Matches C++ `m_2k_texture_upload_cost`.
     */
    val largeTextureUploadCosts: List<Int> = emptyList(),

    /** True when this tier includes sandbox premium access. */
    val sandboxPremium: Boolean = false,
) {
    /**
     * Resolve the upload cost for a texture of [width] × [height] pixels.
     *
     * If the pixel area meets or exceeds [AgentBenefits.MIN_2K_TEXTURE_AREA]
     * the first element of [largeTextureUploadCosts] is returned (matching the
     * C++ `get2KTextureUploadCost` logic); otherwise [textureUploadCost].
     */
    fun resolveTextureUploadCost(width: Int, height: Int): Int {
        if (width > 0 && height > 0 && width * height >= AgentBenefits.MIN_2K_TEXTURE_AREA) {
            return largeTextureUploadCosts.firstOrNull() ?: textureUploadCost
        }
        return textureUploadCost
    }
}

/**
 * Singleton manager for [BenefitPackage] instances.
 *
 * Replaces both `LLAgentBenefits` (the manager) and `LLAgentBenefitsMgr`
 * (the singleton wrapper) from C++.  All public API is on this object.
 *
 * Typical lifecycle
 * -----------------
 * 1. On login: call [initPackage] for every tier received from the server.
 * 2. Call [initCurrent] to mark which tier the logged-in agent is on.
 * 3. UI / world code reads properties via the convenience getters.
 */
object AgentBenefits {

    /**
     * Textures whose pixel area (w × h) is at or above this threshold are
     * considered "2 K" and may attract a different upload cost.
     *
     * C++ equivalent: LLAgentBenefits::MIN_2K_TEXTURE_AREA
     */
    const val MIN_2K_TEXTURE_AREA: Int = 1024 * 1024 + 1

    // -----------------------------------------------------------------------
    // Internal state
    // -----------------------------------------------------------------------

    /** Registry of all named packages received from the server. */
    private val packageMap: MutableMap<String, BenefitPackage> = mutableMapOf()

    /** Name of the package currently active for this agent, or empty string. */
    private var currentName: String = ""

    /** Default/fallback package used when no named package is found. */
    private val defaultPackage: BenefitPackage = BenefitPackage(name = "Default")

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** The [BenefitPackage] active for the logged-in agent, or the default if not set. */
    val currentPackage: BenefitPackage?
        get() = packageMap[currentName]

    /**
     * True when the agent is on a premium (non-default) benefit tier.
     * Mirrors the intent of `LLAgentBenefitsMgr::isCurrent` / checking
     * whether the agent has a named subscription package.
     */
    val isSubscribed: Boolean
        get() = currentName.isNotEmpty() && packageMap.containsKey(currentName)

    // -----------------------------------------------------------------------
    // Limit / cost getters (delegate to currentPackage with fallback)
    // -----------------------------------------------------------------------

    /**
     * Maximum animated-object attachments for the current tier.
     * Returns -1 when the package has not been initialised.
     *
     * C++ equivalent: LLAgentBenefits::getAnimatedObjectLimit()
     */
    fun getAnimatedObjectLimit(): Int = currentPackage?.animatedObjectLimit ?: -1

    /**
     * Cost in L$ to upload an animation for the current tier.
     *
     * C++ equivalent: LLAgentBenefits::getAnimationUploadCost()
     */
    fun getAnimationUploadCost(): Int = currentPackage?.animationUploadCost ?: -1

    /**
     * Maximum attachment count for the current tier.
     *
     * C++ equivalent: LLAgentBenefits::getAttachmentLimit()
     */
    fun getAttachmentLimit(): Int = currentPackage?.attachmentLimit ?: -1

    /**
     * Cost in L$ to create a group for the current tier.
     *
     * C++ equivalent: LLAgentBenefits::getCreateGroupCost()
     */
    fun getCreateGroupCost(): Int = currentPackage?.createGroupCost ?: -1

    /**
     * Maximum group membership count for the current tier.
     *
     * C++ equivalent: LLAgentBenefits::getGroupMembershipLimit()
     */
    fun getGroupLimit(): Int = currentPackage?.groupLimit ?: -1

    /**
     * Maximum profile picks for the current tier.
     *
     * C++ equivalent: LLAgentBenefits::getPicksLimit()
     */
    fun getPicksLimit(): Int = currentPackage?.picksLimit ?: -1

    /**
     * Cost in L$ to upload a sound for the current tier.
     *
     * C++ equivalent: LLAgentBenefits::getSoundUploadCost()
     */
    fun getSoundUploadCost(): Int = currentPackage?.soundUploadCost ?: -1

    /**
     * Cost in L$ to upload a standard texture for the current tier.
     *
     * C++ equivalent: LLAgentBenefits::getTextureUploadCost()
     */
    fun getTextureUploadCost(): Int = currentPackage?.textureUploadCost ?: -1

    /**
     * Resolve the upload cost for a texture of the given dimensions, taking
     * the 2 K surcharge into account.
     *
     * C++ equivalent: LLAgentBenefits::getTextureUploadCost(S32, S32)
     */
    fun getTextureUploadCost(width: Int, height: Int): Int =
        currentPackage?.resolveTextureUploadCost(width, height) ?: getTextureUploadCost()

    // -----------------------------------------------------------------------
    // Package registration (called during login / capability handling)
    // -----------------------------------------------------------------------

    /**
     * Register a [BenefitPackage] under [packageName].
     * Replaces any previously stored package with the same name.
     *
     * C++ equivalent: LLAgentBenefitsMgr::init(package, benefits_sd)
     */
    fun initPackage(packageName: String, pkg: BenefitPackage): Boolean {
        packageMap[packageName] = pkg
        return true
    }

    /**
     * Register a package and mark it as the agent's active tier.
     *
     * C++ equivalent: LLAgentBenefitsMgr::initCurrent(package, benefits_sd)
     */
    fun initCurrent(packageName: String, pkg: BenefitPackage): Boolean {
        packageMap[packageName] = pkg
        currentName = packageName
        return true
    }

    /** True when a package named [packageName] has been registered. */
    fun has(packageName: String): Boolean = packageMap.containsKey(packageName)

    /** True when [packageName] is the agent's currently active tier. */
    fun isCurrent(packageName: String): Boolean = currentName == packageName

    /**
     * Return the [BenefitPackage] registered under [packageName], or the
     * default package if none is found.
     *
     * C++ equivalent: LLAgentBenefitsMgr::get(package)
     */
    fun get(packageName: String): BenefitPackage = packageMap[packageName] ?: defaultPackage

    /** Return the currently active [BenefitPackage], or the default if not set. */
    fun current(): BenefitPackage = currentPackage ?: defaultPackage

    /** Clear all registered packages and reset to an uninitialised state. */
    fun reset() {
        packageMap.clear()
        currentName = ""
    }
}
