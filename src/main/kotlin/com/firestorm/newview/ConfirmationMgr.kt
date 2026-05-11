/**
 * ConfirmationMgr.kt
 * Kotlin port of llconfirmationmanager.h / llconfirmationmanager.cpp
 *
 * Manages confirmation dialogs that gate purchases and other sensitive actions.
 * The user may be asked for a simple click-through confirmation or must supply
 * a password before the action proceeds.
 *
 * Original authors: Linden Research, Inc.
 * LGPL-2.1 – Linden Research, Inc.
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// Singleton: ConfirmationMgr
// ---------------------------------------------------------------------------

/**
 * Manages confirmation dialogs for actions that require user consent.
 *
 * Corresponds to C++ `LLConfirmationManager` (static-only utility class).
 * In the original C++ the class owns a `ListenerBase*` whose lifetime it
 * controls; here that pattern is replaced by a simple functional callback
 * ([Responder]) that is safe to hold as a reference.
 */
object ConfirmationMgr {

    // ------------------------------------------------------------------
    // Public types
    // ------------------------------------------------------------------

    /**
     * Callback interface delivered to [proceed] by callers that need to
     * react when the user confirms or cancels.
     *
     * Corresponds to the combination of `LLConfirmationManager::ListenerBase`
     * and the template `Listener<T>`.  Using a functional interface keeps
     * callers free of boilerplate while preserving the polymorphic dispatch
     * of the original design.
     */
    interface Responder {
        /** Called when the user confirms the action (optionally with [password]). */
        fun onConfirm(password: String = "")

        /** Called when the user dismisses or cancels the dialog. */
        fun onCancel()
    }

    /**
     * The confirmation type requested by the caller.
     *
     * Mirrors `LLConfirmationManager::Type`.
     */
    enum class ConfirmType {
        /** No dialog – confirm immediately without user interaction. */
        NONE,

        /** A simple click-through alert (no text input). */
        CLICK,

        /** A password-entry dialog; the entered text is forwarded to [Responder.onConfirm]. */
        PASSWORD,
    }

    /**
     * A pending confirmation token keyed by a logical [name].
     *
     * Not present in the original C++ (which had no persistent token map),
     * but added here to satisfy the design spec so callers can retrieve
     * pending confirmations by name.
     */
    data class Token(
        val name: String,
        val responder: Responder,
    )

    // ------------------------------------------------------------------
    // Internal state
    // ------------------------------------------------------------------

    /** Active tokens, keyed by their logical name. */
    private val pendingTokens: MutableMap<String, Token> = mutableMapOf()

    // ------------------------------------------------------------------
    // Core API
    // ------------------------------------------------------------------

    /**
     * Request a confirmation dialog of [type] for the logical action [name].
     *
     * The [responder] will receive [Responder.onConfirm] if the user proceeds
     * or [Responder.onCancel] if they dismiss the dialog.
     *
     * When [type] is [ConfirmType.NONE] the responder is confirmed immediately
     * with an empty password, mirroring the C++ `TYPE_NONE` case.
     *
     * Corresponds to both overloads of `LLConfirmationManager::confirm()`.
     */
    fun proceed(name: String, responder: Responder, type: ConfirmType = ConfirmType.NONE) {
        // Register the token so it can be retrieved by getToken().
        pendingTokens[name] = Token(name, responder)

        when (type) {
            ConfirmType.NONE -> {
                // Confirm immediately – mirrors C++ TYPE_NONE / default branch.
                responder.onConfirm("")
                pendingTokens.remove(name)
            }

            ConfirmType.CLICK -> {
                // TODO: show a click-through alert (analogous to
                //   LLNotificationsUtil::add("ConfirmPurchase", …)) and call
                //   responder.onConfirm("") on option 0, onCancel() otherwise.
                TODO("Wire up click-through confirmation dialog for '$name'")
            }

            ConfirmType.PASSWORD -> {
                // TODO: show a password-entry dialog (analogous to
                //   LLNotificationsUtil::add("ConfirmPurchasePassword", …)) and
                //   call responder.onConfirm(enteredPassword) on option 0,
                //   onCancel() otherwise.
                TODO("Wire up password confirmation dialog for '$name'")
            }
        }
    }

    /**
     * Convenience overload that accepts a string [typeName] ("none", "click",
     * "password") instead of a [ConfirmType] enum value.
     *
     * Mirrors `LLConfirmationManager::confirm(const std::string& type, …)`.
     */
    fun proceed(name: String, responder: Responder, typeName: String) {
        val type = when (typeName.lowercase()) {
            "click"    -> ConfirmType.CLICK
            "password" -> ConfirmType.PASSWORD
            else       -> ConfirmType.NONE
        }
        proceed(name, responder, type)
    }

    /**
     * Return the pending [Token] for [name], or `null` if none exists.
     */
    fun getToken(name: String): Token? = pendingTokens[name]

    /**
     * Remove a pending token, typically after the dialog has been resolved.
     *
     * This corresponds to the C++ `delete listener` call inside the dialog
     * callbacks.
     */
    fun dismissToken(name: String) {
        pendingTokens.remove(name)
    }
}
