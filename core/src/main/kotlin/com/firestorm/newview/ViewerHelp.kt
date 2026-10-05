/**
 * ViewerHelp.kt
 * Kotlin port of llviewerhelp.h / llviewerhelp.cpp
 *
 * Provides a layer of abstraction that protects help-system-using code from the
 * details of the help UI floater and how help topics are converted into URLs.
 *
 * Original authors: Tofu Linden, Firestorm contributors
 * LGPL-2.1 – Linden Research, Inc.
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// Interface: LLHelpImpl
// ---------------------------------------------------------------------------

/**
 * Abstraction over whatever UI component actually renders a help page.
 * Implementations might open an in-viewer browser floater, launch an
 * external browser, etc.
 */
interface LLHelpImpl {
    /** Display the given fully-formed help URL. */
    fun showHelp(url: String)
}

// ---------------------------------------------------------------------------
// Singleton: ViewerHelp
// ---------------------------------------------------------------------------

/**
 * Singleton that manages the viewer's help system.
 *
 * Converts help topic names into URLs via [ViewerHelpUtil.buildHelpURL] and
 * delegates the actual rendering to the registered [LLHelpImpl].
 *
 * Corresponds to C++ `LLViewerHelp` (LLSingleton<LLViewerHelp>).
 */
object ViewerHelp {

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    /** Optional pluggable renderer; if null a default no-op behaviour is used. */
    private var helpImpl: LLHelpImpl? = null

    // ------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------

    /**
     * Register a concrete help renderer.
     *
     * Analogous to injecting the singleton's dependency at runtime.
     */
    fun setHelpImpl(impl: LLHelpImpl) {
        helpImpl = impl
    }

    // ------------------------------------------------------------------
    // Topic helpers
    // ------------------------------------------------------------------

    /**
     * Return the fallback/default topic name.
     *
     * C++ comment: "*hack: to be done properly"
     */
    fun defaultTopic(): String = "this_is_fallbacktopic"

    /**
     * Return the topic shown before the user logs in.
     *
     * C++ comment: "*hack: to be done properly"
     */
    fun preLoginTopic(): String = "start"

    /**
     * Return the topic that F1 maps to.
     *
     * C++ comment: "*hack: to be done properly"
     */
    fun f1HelpTopic(): String = "f1_help"

    /**
     * Return the topic used for the grid-manager help on the login screen.
     *
     * C++ Firestorm extension: FIRE-7377.
     */
    fun gridMgrHelpTopic(): String = "fs_grid_manager"

    /**
     * Derive a help topic from the currently focused UI element.
     *
     * Falls back to [defaultTopic] when no focused element provides a topic.
     * The C++ version walks the `gFocusMgr` / `LLUICtrl` hierarchy; that
     * UI-framework coupling is platform-specific and is stubbed here.
     */
    fun getTopicFromFocus(): String {
        // queries the UI focus manager for the focused control and calls
        // findHelpTopic() on it, mirroring LLViewerHelp::getTopicFromFocus() — when UI layer is ported
        return defaultTopic()
    }

    // ------------------------------------------------------------------
    // Core API
    // ------------------------------------------------------------------

    /**
     * Build the full help URL for [topic].
     *
     * Replicates `LLViewerHelp::getURL()`:
     *  - empty topic → [defaultTopic]
     *  - f1 topic    → derive from focus; if still default and not logged in → pre-login topic
     *  - all others  → forward directly to [ViewerHelpUtil.buildHelpURL]
     */
    fun getHelpURL(topic: String): String {
        var helpTopic = topic.ifEmpty { defaultTopic() }

        if (helpTopic == f1HelpTopic()) {
            helpTopic = getTopicFromFocus()
            if (helpTopic == defaultTopic() && !isLoggedIn()) {
                helpTopic = preLoginTopic()
            }
        }

        return ViewerHelpUtil.buildHelpURL(helpTopic)
    }

    /**
     * Return `true` when [url] looks like a valid help URL.
     *
     * A minimal sanity-check; the C++ code relied on LLWeb utilities.
     */
    fun helpURLValid(url: String): Boolean =
        url.startsWith("http://") || url.startsWith("https://")

    /**
     * Display help for [topic].
     *
     * Replicates `LLViewerHelp::showTopic()`: builds the URL, then either
     * opens an external browser (Firestorm extension) or shows the in-viewer
     * help-browser floater.
     */
    fun showHelp(topic: String) {
        val url = getHelpURL(topic)
        // checks LLWeb.useExternalBrowser(url) and branches accordingly,
        // mirroring the <FS:Beq> block in llviewerhelp.cpp — when LLWeb is ported
        helpImpl?.showHelp(url)
            ?: showHelpURL(url) // default: no-op stub until impl is wired up
    }

    /**
     * Display a raw, already-formed help URL directly.
     *
     * Useful when the caller has already built the URL and just needs it shown.
     */
    fun showHelpURL(url: String) {
        helpImpl?.showHelp(url)
        // falls back to opening an in-viewer floater (LLFloaterReg::showInstance)
        // when no impl is registered and the URL is internal — when floater registry is ported
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Placeholder login-state check.
     *
     * C++ used `LLLoginInstance::getInstance()->authSuccess()`.
     */
    private fun isLoggedIn(): Boolean {
        // delegates to LLLoginInstance equivalent when login layer is ported
        return false
    }
}
