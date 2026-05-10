/**
 * FloaterReg.kt
 * Converted from: indra/llui/llfloaterreg.h / llfloaterreg.cpp
 * Original: LLFloaterReg — central floater registry for the Second Life viewer.
 *
 * Copyright (C) 2010, Linden Research, Inc. (LGPL 2.1)
 * Kotlin port: Firestorm-kotlin project.
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLSD
import com.firestorm.llcommon.LLUUID
import com.firestorm.llui.LLFloater

// ---------------------------------------------------------------------------
// Type aliases mirroring the C++ typedefs
// ---------------------------------------------------------------------------

/** Factory function that builds a floater from an LLSD key. */
typealias FloaterBuildFunc = (key: LLSD) -> LLFloater?

// ---------------------------------------------------------------------------
// BuildData — holds the factory function and XUI file name for one floater type.
// ---------------------------------------------------------------------------

data class FloaterBuildData(
    val buildFunc: FloaterBuildFunc,
    val file: String
)

// ---------------------------------------------------------------------------
// FloaterReg — singleton registry (C++ LLFloaterReg, all-static → object).
// ---------------------------------------------------------------------------

/**
 * Central floater registry.  Floater types are registered once at startup
 * with [add]; instances are then created on demand via [getInstance] and
 * surfaced to the user through [showInstance], [hideInstance], and
 * [toggleInstance].
 *
 * Instances are stored in per-group lists so that multiple keyed instances of
 * the same floater type (e.g. IM windows) can coexist.
 */
object FloaterReg {

    // ------------------------------------------------------------------
    // Internal state  (mirrors the C++ static data members)
    // ------------------------------------------------------------------

    /** All live instances, keyed by group name. */
    private val instanceMap: MutableMap<String, MutableList<LLFloater>> = mutableMapOf()

    /** Factory + XUI-file records, keyed by floater name. */
    private val buildMap: MutableMap<String, FloaterBuildData> = mutableMapOf()

    /**
     * Maps every registered name (and group name) to its canonical group name.
     * A name with no explicit group maps to itself.
     */
    private val groupMap: MutableMap<String, String> = mutableMapOf()

    /** When true, [showInstance] is a no-op unless the name is in [alwaysShowableList]. */
    var blockShowFloaters: Boolean = false
        private set

    /** Names that bypass [blockShowFloaters]. */
    private val alwaysShowableList: MutableSet<String> = mutableSetOf()

    /** Empty sentinel list returned when no instances exist for a name. */
    private val nullInstanceList: List<LLFloater> = emptyList()

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    /**
     * Register a floater type.
     *
     * @param name       Unique floater name (e.g. `"preferences"`).
     * @param file       XUI layout file (e.g. `"floater_preferences.xml"`).
     * @param buildFunc  Factory lambda; receives the LLSD key, returns a new instance.
     * @param groupName  Optional group; floaters in the same group share cascading state.
     *                   Defaults to [name] when omitted.
     */
    fun add(
        name: String,
        file: String,
        buildFunc: FloaterBuildFunc,
        groupName: String = ""
    ) {
        buildMap[name] = FloaterBuildData(buildFunc, file)
        val resolvedGroup = if (groupName.isEmpty()) name else groupName
        groupMap[name] = resolvedGroup
        groupMap[resolvedGroup] = resolvedGroup   // allow direct group-name lookup
    }

    /** Returns true if [name] has been registered via [add]. */
    fun isRegistered(name: String): Boolean = buildMap.containsKey(name)

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Returns the last visible, non-minimised floater in the group that
     * [name] belongs to, or `null` if none exists.
     */
    fun getLastFloaterInGroup(name: String): LLFloater? {
        val groupName = groupMap[name] ?: return null
        if (groupName.isEmpty()) return null
        val list = instanceMap[groupName] ?: return null
        return list.asReversed().firstOrNull { it.isVisible && !it.isMinimized }
    }

    /**
     * Returns the topmost cascading floater across all groups, or `null`.
     * (Mirrors `LLFloaterReg::getLastFloaterCascading`.)
     */
    fun getLastFloaterCascading(): LLFloater? {
        TODO("Requires LLFloater positioning API")
    }

    /**
     * Returns all floaters that belong to the same group as [floater].
     * Returns an empty list when the group cannot be determined.
     */
    fun getAllFloatersInGroup(floater: LLFloater): List<LLFloater> {
        for ((_, groupName) in groupMap) {
            if (groupName.isEmpty()) continue
            val instances = instanceMap[groupName] ?: continue
            if (floater in instances) {
                return instances.toList()
            }
        }
        return emptyList()
    }

    // ------------------------------------------------------------------
    // Find / get (create) / remove / destroy
    // ------------------------------------------------------------------

    /**
     * Returns an existing instance that matches [name] and [key], or `null`
     * if none has been created yet.
     */
    fun findInstance(name: String, key: LLSD? = null): LLFloater? {
        val groupName = groupMap[name] ?: return null
        if (groupName.isEmpty()) return null
        val list = instanceMap[groupName] ?: return null
        return list.firstOrNull { it.matchesKey(key) }
    }

    /**
     * Returns an existing instance or creates a new one.
     * Returns `null` when [name] is not registered or construction fails.
     */
    fun getInstance(name: String, key: LLSD? = null): LLFloater? {
        findInstance(name, key)?.let { return it }

        val buildData = buildMap[name] ?: run {
            System.err.println("FloaterReg: floater type '$name' not registered.")
            return null
        }
        val groupName = groupMap[name] ?: return null
        if (groupName.isEmpty()) return null

        val list = instanceMap.getOrPut(groupName) { mutableListOf() }
        val instance = buildData.buildFunc(key ?: LLSD.emptyMap()) ?: run {
            System.err.println("FloaterReg: failed to build floater type '$name'.")
            return null
        }

        val success = instance.buildFromFile(buildData.file)
        if (!success) {
            System.err.println("FloaterReg: failed to build floater type '$name' from '${buildData.file}'.")
            return null
        }

        if (instance.key == null || instance.key!!.isUndefined) {
            instance.key = key
        }
        instance.instanceName = name

        val lastFloater = list.lastOrNull()
        instance.applyControlsAndPosition(lastFloater)

        list.add(instance)
        return instance
    }

    /**
     * Removes and returns the instance matching [name] + [key] from the
     * registry without deleting it.  Returns `null` if not found.
     */
    fun removeInstance(name: String, key: LLSD? = null): LLFloater? {
        val groupName = groupMap[name] ?: return null
        if (groupName.isEmpty()) return null
        val list = instanceMap[groupName] ?: return null
        val iter = list.iterator()
        while (iter.hasNext()) {
            val inst = iter.next()
            if (inst.matchesKey(key)) {
                iter.remove()
                return inst
            }
        }
        return null
    }

    /**
     * Removes and disposes of the instance matching [name] + [key].
     * Returns `true` if an instance was found and destroyed.
     */
    fun destroyInstance(name: String, key: LLSD? = null): Boolean {
        val inst = removeInstance(name, key) ?: return false
        inst.destroy()
        return true
    }

    // ------------------------------------------------------------------
    // Iterators
    // ------------------------------------------------------------------

    /**
     * Returns a read-only snapshot of all live instances for [name].
     * Returns an empty list when [name] has no instances.
     */
    fun getFloaterList(name: String): List<LLFloater> =
        instanceMap[name]?.toList() ?: nullInstanceList

    // ------------------------------------------------------------------
    // Visibility management
    // ------------------------------------------------------------------

    /**
     * Returns `true` when the validate signal allows [name] to be shown.
     * (RLVa hook — always true in the base port.)
     */
    fun canShowInstance(name: String, key: LLSD? = null): Boolean {
        TODO("Requires RLVa validate-signal infrastructure")
    }

    /**
     * Makes the floater for [name] + [key] visible, creating it if needed.
     * Returns `null` when blocked or when construction fails.
     *
     * @param focus If `true`, the floater will receive keyboard focus.
     */
    fun showInstance(name: String, key: LLSD? = null, focus: Boolean = false): LLFloater? {
        if (blockShowFloaters && name !in alwaysShowableList) return null
        val instance = getInstance(name, key) ?: return null
        instance.openFloater(key)
        if (focus) instance.setFocus(true)
        return instance
    }

    /**
     * Closes / hides the floater for [name] + [key].
     * Returns `true` if an instance was found.
     */
    fun hideInstance(name: String, key: LLSD? = null): Boolean {
        val instance = findInstance(name, key) ?: return false
        instance.closeHostedFloater()
        return true
    }

    /**
     * Toggles visibility for [name] + [key].
     * Returns `true` if the floater is visible after the call.
     */
    fun toggleInstance(name: String, key: LLSD? = null): Boolean {
        val instance = findInstance(name, key)
        if (instance != null && instance.isShown) {
            instance.closeHostedFloater()
            return false
        }
        return showInstance(name, key, focus = true) != null
    }

    /**
     * Returns `true` if a live instance for [name] + [key] exists and is
     * currently visible (minimised or not).
     */
    fun instanceVisible(name: String, key: LLSD? = null): Boolean {
        val instance = findInstance(name, key) ?: return false
        return LLFloater.isVisible(instance)
    }

    /** Shows every registered floater whose saved-visibility control is `true`. */
    fun showInitialVisibleInstances() {
        TODO("Requires LLControlGroup / persisted visibility controls")
    }

    /**
     * Pushes all visible instances to hidden state, except those whose name
     * is in [exceptions].
     */
    fun hideVisibleInstances(exceptions: Set<String> = emptySet()) {
        for ((name, list) in instanceMap) {
            if (name in exceptions) continue
            list.forEach { it.pushVisible(false) }
        }
    }

    /** Restores the pushed visibility state for all live instances. */
    fun restoreVisibleInstances() {
        instanceMap.values.forEach { list -> list.forEach { it.popVisible() } }
    }

    // ------------------------------------------------------------------
    // Control-variable naming helpers
    // ------------------------------------------------------------------

    fun getBaseControlName(name: String): String = name.replace(' ', '_')
    fun getRectControlName(name: String): String = "floater_rect_${getBaseControlName(name)}"
    fun declareRectControl(name: String): String = getRectControlName(name).also {
        TODO("Requires LLControlGroup infrastructure")
    }
    fun declarePosXControl(name: String): String = "floater_pos_${getBaseControlName(name)}_x".also {
        TODO("Requires LLControlGroup infrastructure")
    }
    fun declarePosYControl(name: String): String = "floater_pos_${getBaseControlName(name)}_y".also {
        TODO("Requires LLControlGroup infrastructure")
    }
    fun getVisibilityControlName(name: String): String = "floater_vis_${getBaseControlName(name)}"
    fun declareVisibilityControl(name: String): String = getVisibilityControlName(name).also {
        TODO("Requires LLControlGroup infrastructure")
    }
    fun getDockStateControlName(name: String): String = "floater_dock_${getBaseControlName(name)}"
    fun declareDockStateControl(name: String): String = getDockStateControlName(name).also {
        TODO("Requires LLControlGroup infrastructure")
    }

    /** Iterates all registered names and declares their rect / visibility controls. */
    fun registerControlVariables() {
        TODO("Requires LLControlGroup infrastructure")
    }

    // ------------------------------------------------------------------
    // Callback wrappers  (toolbar button semantics)
    // ------------------------------------------------------------------

    /**
     * Implements the 4-state toolbar-button behaviour:
     * minimised → restore; closed → open; not-front → focus; open+front → close.
     */
    fun toggleInstanceOrBringToFront(sdName: LLSD, key: LLSD? = null) {
        val name = sdName.asString()
        val instance = getInstance(name, key) ?: run {
            System.err.println("FloaterReg: unable to get instance of floater '$name'")
            return
        }
        val host = instance.host
        if (host != null) {
            if (host.isMinimized || !host.isShown || !host.isFrontmost) {
                host.isMinimized = false
                instance.openFloater(key)
                instance.setVisibleAndFrontmost(true, key)
            } else if (!instance.isVisible) {
                instance.openFloater(key)
                instance.setVisibleAndFrontmost(true, key)
                instance.setFocus(true)
            } else {
                instance.closeHostedFloater()
            }
        } else {
            when {
                instance.isMinimized -> {
                    instance.isMinimized = false
                    instance.setVisibleAndFrontmost(true, key)
                }
                !instance.isShown -> {
                    instance.openFloater(key)
                    instance.setVisibleAndFrontmost(true, key)
                }
                !instance.hasFocus || !instance.isFrontmost -> {
                    instance.setVisibleAndFrontmost(true, key)
                }
                else -> instance.closeHostedFloater()
            }
        }
    }

    /**
     * Like [toggleInstanceOrBringToFront] but never closes the floater —
     * only opens / restores / brings to front.
     */
    fun showInstanceOrBringToFront(sdName: LLSD, key: LLSD? = null) {
        val name = sdName.asString()
        val instance = getInstance(name, key) ?: run {
            System.err.println("FloaterReg: unable to get instance of floater '$name'")
            return
        }
        val host = instance.host
        if (host != null) {
            if (host.isMinimized || !host.isShown || !host.isFrontmost) {
                host.isMinimized = false
                instance.openFloater(key)
                instance.setVisibleAndFrontmost(true, key)
            } else if (!instance.isVisible) {
                instance.openFloater(key)
                instance.setVisibleAndFrontmost(true, key)
                instance.setFocus(true)
            }
        } else {
            when {
                instance.isMinimized -> {
                    instance.isMinimized = false
                    instance.setVisibleAndFrontmost(true, key)
                }
                !instance.isShown -> {
                    instance.openFloater(key)
                    instance.setVisibleAndFrontmost(true, key)
                }
                !instance.isFrontmost -> {
                    instance.setVisibleAndFrontmost(true, key)
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Typed convenience accessors (replaces C++ template methods)
    // ------------------------------------------------------------------

    /**
     * Typed variant of [findInstance].
     * Returns the instance cast to [T], or `null` if absent or wrong type.
     */
    inline fun <reified T : LLFloater> findTypedInstance(name: String, key: LLSD? = null): T? =
        findInstance(name, key) as? T

    /**
     * Typed variant of [getInstance].
     * Returns the instance cast to [T], or `null` if wrong type or not found.
     */
    inline fun <reified T : LLFloater> getTypedInstance(name: String, key: LLSD? = null): T? =
        getInstance(name, key) as? T

    /**
     * Typed variant of [showInstance].
     * Returns the shown instance cast to [T], or `null`.
     */
    inline fun <reified T : LLFloater> showTypedInstance(
        name: String,
        key: LLSD? = null,
        focus: Boolean = false
    ): T? = showInstance(name, key, focus) as? T

    // ------------------------------------------------------------------
    // Statistics
    // ------------------------------------------------------------------

    /** Returns the count of currently visible, non-minimised floaters across all groups. */
    fun getVisibleFloaterInstanceCount(): UInt {
        var count = 0u
        for ((groupName, _) in groupMap) {
            val instances = instanceMap[groupName] ?: continue
            instances.forEach { if (it.isVisible && !it.isMinimized) count++ }
        }
        return count
    }

    // ------------------------------------------------------------------
    // Block control
    // ------------------------------------------------------------------

    /** Enables or disables the global show-block. */
    fun setBlockShowFloaters(value: Boolean) {
        blockShowFloaters = value
    }
}
