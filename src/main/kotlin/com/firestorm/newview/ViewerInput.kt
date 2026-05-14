/**
 * ViewerInput.kt
 * Kotlin port of llviewerinput.h / llviewerinput.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * License: GNU Lesser General Public License v2.1
 */

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

// ---------------------------------------------------------------------------
// Constants (mirrors llviewerinput.h)
// ---------------------------------------------------------------------------

const val MAX_KEY_BINDINGS: Int = 128
const val KEYBINDINGS_XML_VERSION: Int = 1
const val SCRIPT_MOUSE_HANDLER_NAME: String = "script_trigger_lbutton"

// ---------------------------------------------------------------------------
// Input mode enum
// ---------------------------------------------------------------------------

/**
 * The camera / control mode that governs which key-binding table is active.
 *
 * Corresponds to C++ enum e_keyboard_mode / EKeyboardMode.
 */
enum class InputMode {
    /** First-person / mouselook camera. */
    FIRST_PERSON,
    /** Default third-person camera. */
    THIRD_PERSON,
    /** Avatar appearance / morph editor. */
    EDIT_AVATAR,
    /** Avatar is seated on an object. */
    SITTING,
}

// ---------------------------------------------------------------------------
// Mouse click type enum
// ---------------------------------------------------------------------------

/**
 * Identifies which mouse button was pressed.
 * Corresponds to C++ EMouseClickType from llkeyboard.h.
 */
enum class MouseClickType {
    NONE,
    LEFT,
    DOUBLE_LEFT,
    MIDDLE,
    BUTTON4,
    BUTTON5,
}

// ---------------------------------------------------------------------------
// Data classes
// ---------------------------------------------------------------------------

/**
 * Describes a keyboard key binding.
 *
 * Mirrors C++ struct LLKeyboardBinding + the Keys/KeyMode/KeyBinding XML
 * param-block structures flattened into a single data class.
 *
 * @param key           Numeric key code (matches constants in llkeyboard.h).
 * @param mask          Modifier bitmask (Shift=1, Ctrl=2, Alt=4, …).
 * @param mode          The [InputMode] this binding applies to.
 * @param command       Name of the registered action function to invoke.
 * @param isGlobal      True for bindings that fire even when a floater has focus.
 */
data class KeyBinding(
    val key: Int,
    val mask: Int,
    val mode: InputMode,
    val command: String,
    val isGlobal: Boolean = false,
)

/**
 * Describes a mouse-button binding.
 *
 * Mirrors C++ struct LLMouseBinding.
 *
 * @param button    Which mouse button ([MouseClickType]).
 * @param mask      Modifier bitmask.
 * @param mode      The [InputMode] this binding applies to.
 * @param command   Name of the registered action function to invoke.
 * @param isGlobal  True for bindings processed before floaters consume the event.
 */
data class MouseBinding(
    val button: MouseClickType,
    val mask: Int,
    val mode: InputMode,
    val command: String,
    val isGlobal: Boolean = false,
)

// ---------------------------------------------------------------------------
// Action registry (simplified substitute for C++ LLKeyboardActionRegistry)
// ---------------------------------------------------------------------------

/**
 * Registry that maps command-name strings to handler functions.
 *
 * In C++ this is built at static-initialisation time via REGISTER_KEYBOARD_ACTION
 * macros.  Here we use a plain map populated explicitly.
 *
 * Handlers receive a simple Boolean representing key-down (true) vs key-up (false)
 * instead of the full C++ EKeystate enum; callers that need more granularity
 * should extend this design.
 */
object ActionRegistry {
    /** Function type for action handlers. */
    typealias ActionHandler = (keyDown: Boolean) -> Boolean

    private val handlers: MutableMap<String, ActionHandler> = mutableMapOf()
    private val globalNames: MutableSet<String> = mutableSetOf()

    /** Register a handler for [name].  Global handlers are flagged so callers
     *  can decide whether to bypass focused-floater suppression. */
    fun register(name: String, global: Boolean = false, handler: ActionHandler) {
        handlers[name] = handler
        if (global) globalNames.add(name)
    }

    fun getHandler(name: String): ActionHandler? = handlers[name]
    fun isGlobal(name: String): Boolean = name in globalNames
}

// ---------------------------------------------------------------------------
// ViewerInput singleton
// ---------------------------------------------------------------------------

/**
 * Singleton that owns all key/mouse binding tables and dispatches input events
 * to the appropriate registered action.
 *
 * Corresponds to C++ class LLViewerInput (global instance gViewerInput).
 */
object ViewerInput {

    // -----------------------------------------------------------------------
    // Binding storage — one list per mode, split into normal vs global
    // -----------------------------------------------------------------------

    private val keyBindings:
            Map<InputMode, MutableList<KeyBinding>> =
        InputMode.entries.associateWith { mutableListOf() }

    private val globalKeyBindings:
            Map<InputMode, MutableList<KeyBinding>> =
        InputMode.entries.associateWith { mutableListOf() }

    private val mouseBindings:
            Map<InputMode, MutableList<MouseBinding>> =
        InputMode.entries.associateWith { mutableListOf() }

    private val globalMouseBindings:
            Map<InputMode, MutableList<MouseBinding>> =
        InputMode.entries.associateWith { mutableListOf() }

    // -----------------------------------------------------------------------
    // Binding management
    // -----------------------------------------------------------------------

    /**
     * Add a key binding.  If a binding for the same (mode, key, mask) already
     * exists it is replaced; otherwise a new entry is appended.
     * Corresponds to C++ LLViewerInput::bindKey().
     */
    fun addKeyBinding(binding: KeyBinding) {
        val list = if (binding.isGlobal)
            globalKeyBindings[binding.mode]!!
        else
            keyBindings[binding.mode]!!

        val idx = list.indexOfFirst { it.key == binding.key && it.mask == binding.mask }
        if (idx >= 0) list[idx] = binding else list.add(binding)
    }

    /**
     * Add a mouse binding.  Duplicates (same mode/button/mask) are replaced.
     * Corresponds to C++ LLViewerInput::bindMouse().
     */
    fun addMouseBinding(binding: MouseBinding) {
        val list = if (binding.isGlobal)
            globalMouseBindings[binding.mode]!!
        else
            mouseBindings[binding.mode]!!

        val idx = list.indexOfFirst { it.button == binding.button && it.mask == binding.mask }
        if (idx >= 0) list[idx] = binding else list.add(binding)
    }

    /** Remove all bindings from every mode. */
    fun resetBindings() {
        for (mode in InputMode.entries) {
            keyBindings[mode]!!.clear()
            globalKeyBindings[mode]!!.clear()
            mouseBindings[mode]!!.clear()
            globalMouseBindings[mode]!!.clear()
        }
    }

    // -----------------------------------------------------------------------
    // Mode resolution
    // -----------------------------------------------------------------------

    /**
     * Determine the active [InputMode] from the current agent/camera state.
     * Corresponds to C++ LLViewerInput::getMode().
     *
     * Full implementation inspects gAgentCamera / gMorphView / gAgentAvatarp;
     * stubbed here.
     */
    fun getMode(): InputMode {
        // queries camera, morph-view, avatar sitting state when agent/camera layer is ported
        return InputMode.THIRD_PERSON
    }

    /**
     * Parse a mode name string (as used in keys.xml) into an [InputMode].
     * Returns null on failure.  Corresponds to C++ LLViewerInput::modeFromString().
     */
    fun modeFromString(string: String): InputMode? {
        return when (string.lowercase().trim()) {
            "first_person"  -> InputMode.FIRST_PERSON
            "third_person"  -> InputMode.THIRD_PERSON
            "edit_avatar"   -> InputMode.EDIT_AVATAR
            "sitting"       -> InputMode.SITTING
            else            -> string.toIntOrNull()?.let { InputMode.entries.getOrNull(it) }
        }
    }

    /**
     * Parse a mouse-button string (as used in keys.xml) into a [MouseClickType].
     * Returns null on failure.  Corresponds to C++ LLViewerInput::mouseFromString().
     */
    fun mouseFromString(string: String): MouseClickType? = when (string) {
        "LMB"        -> MouseClickType.LEFT
        "Double LMB" -> MouseClickType.DOUBLE_LEFT
        "MMB"        -> MouseClickType.MIDDLE
        "MB4"        -> MouseClickType.BUTTON4
        "MB5"        -> MouseClickType.BUTTON5
        else         -> null
    }

    // -----------------------------------------------------------------------
    // Dispatch
    // -----------------------------------------------------------------------

    /**
     * Attempt to dispatch a key-down event.
     *
     * - Checks global bindings first (these bypass floater focus).
     * - Then checks per-mode bindings.
     *
     * Returns true if a handler claimed the event.
     * Corresponds to the combination of C++ LLViewerInput::handleKey() +
     * LLViewerInput::scanKey().
     *
     * @param key     Key code.
     * @param mask    Current modifier mask.
     * @param keyDown True for key-down events, false for key-up.
     */
    fun handleKey(key: Int, mask: Int, keyDown: Boolean = true): Boolean {
        val mode = getMode()

        // Check global bindings first
        val globalResult = scanKeyList(globalKeyBindings[mode]!!, key, mask, keyDown)
        if (globalResult) return true

        // Check per-mode bindings
        return scanKeyList(keyBindings[mode]!!, key, mask, keyDown)
    }

    /**
     * Attempt to dispatch a mouse-button event.
     *
     * Returns true if a handler claimed the event.
     * Corresponds to the scanMouse() family in C++ LLViewerInput.
     *
     * @param button  Which mouse button was pressed/released.
     * @param mask    Current modifier mask.
     * @param keyDown True for button-down, false for button-up.
     */
    fun handleMouseButton(button: Int, mask: Int, keyDown: Boolean = true): Boolean {
        val clickType = MouseClickType.entries.getOrNull(button) ?: return false
        val mode = getMode()

        val globalResult = scanMouseList(globalMouseBindings[mode]!!, clickType, mask, keyDown)
        if (globalResult) return true

        return scanMouseList(mouseBindings[mode]!!, clickType, mask, keyDown)
    }

    // -----------------------------------------------------------------------
    // XML binding loader (stub)
    // -----------------------------------------------------------------------

    /**
     * Load key/mouse bindings from an XML file.
     * Returns the number of bindings successfully loaded, 0 on error.
     * Corresponds to C++ LLViewerInput::loadBindingsXML().
     */
    fun loadBindingsXml(filename: String): Int {
        resetBindings()
        TODO("XML parsing and binding registration not yet implemented (filename=$filename)")
    }

    // -----------------------------------------------------------------------
    // String representation of a binding (for UI display)
    // -----------------------------------------------------------------------

    /**
     * Return a human-readable string listing all key/mouse bindings for [command]
     * in [modeName] (e.g. "Ctrl+W | LMB").
     * Corresponds to C++ LLViewerInput::getKeyBindingAsString().
     */
    fun getKeyBindingAsString(modeName: String, command: String): String {
        val mode = modeFromString(modeName) ?: getMode()
        val parts = mutableListOf<String>()

        for (b in keyBindings[mode]!!.filter { it.command == command }) {
            parts.add(maskAndKeyToString(b.mask, b.key))
        }
        for (b in mouseBindings[mode]!!.filter { it.command == command }) {
            parts.add(maskAndMouseToString(b.mask, b.button))
        }
        return parts.joinToString(" | ")
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun scanKeyList(
        bindings: List<KeyBinding>,
        key: Int,
        mask: Int,
        keyDown: Boolean,
    ): Boolean {
        for (b in bindings) {
            if (b.key == key && (b.mask and mask) == b.mask) {
                val handler = ActionRegistry.getHandler(b.command) ?: continue
                return handler(keyDown)
            }
        }
        return false
    }

    private fun scanMouseList(
        bindings: List<MouseBinding>,
        button: MouseClickType,
        mask: Int,
        keyDown: Boolean,
    ): Boolean {
        for (b in bindings) {
            if (b.button == button && (b.mask and mask) == b.mask) {
                val handler = ActionRegistry.getHandler(b.command) ?: continue
                return handler(keyDown)
            }
        }
        return false
    }

    /** Produce a display string like "Ctrl+W" from a mask + key code. */
    private fun maskAndKeyToString(mask: Int, key: Int): String {
        val parts = mutableListOf<String>()
        if (mask and 0x1 != 0) parts.add("Shift")
        if (mask and 0x2 != 0) parts.add("Ctrl")
        if (mask and 0x4 != 0) parts.add("Alt")
        parts.add(key.toChar().uppercaseChar().toString())
        return parts.joinToString("+")
    }

    /** Produce a display string like "Ctrl+LMB" from a mask + mouse button. */
    private fun maskAndMouseToString(mask: Int, button: MouseClickType): String {
        val parts = mutableListOf<String>()
        if (mask and 0x1 != 0) parts.add("Shift")
        if (mask and 0x2 != 0) parts.add("Ctrl")
        if (mask and 0x4 != 0) parts.add("Alt")
        parts.add(button.name)
        return parts.joinToString("+")
    }
}
