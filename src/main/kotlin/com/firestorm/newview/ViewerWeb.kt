/**
 * ViewerWeb.kt
 * URL loading and browser dispatch helpers — Kotlin port of llweb.h / llweb.cpp
 *
 * Original: Copyright (C) 2006-2010 Linden Research, Inc.
 * Ported to Kotlin for the Firestorm viewer project.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1.
 */

package com.firestorm.newview

// ---------------------------------------------------------------------------
// ViewerWeb — static utilities for URL loading
// ---------------------------------------------------------------------------

/**
 * Provides methods to load URLs in either the viewer's built-in browser or
 * the operating system's default external browser, respecting the user's
 * browser preference setting.
 *
 * This maps directly to the C++ [LLWeb] class; all members were static there
 * so the entire public API lives in a Kotlin [object].
 *
 * Network / UI calls are stubbed with [TODO] — wire to the platform's
 * browser-spawn and internal-floater mechanisms before use.
 */
object ViewerWeb {

    // -----------------------------------------------------------------------
    // Preferred-browser policy (mirrors C++ [LLWeb::PreferredBrowser])
    // -----------------------------------------------------------------------

    /**
     * User-configurable policy for which browser handles a given URL.
     *
     * @property EXTERNAL_ONLY    Always use the OS default browser.
     * @property LL_INTERNAL_OTHERS_EXTERNAL  SL links in built-in browser,
     *                            everything else in the external browser.
     * @property INTERNAL_ONLY   Always use the built-in browser.
     */
    enum class PreferredBrowser {
        EXTERNAL_ONLY,
        LL_INTERNAL_OTHERS_EXTERNAL,
        INTERNAL_ONLY
    }

    /**
     * Current browser preference.  Read from viewer settings on startup;
     * default matches the C++ enum value 1 (internal for SL, external for
     * third-party URLs).
     */
    var preferredBrowser: PreferredBrowser = PreferredBrowser.LL_INTERNAL_OTHERS_EXTERNAL

    // -----------------------------------------------------------------------
    // Primary dispatch
    // -----------------------------------------------------------------------

    /**
     * Loads [url] using whichever browser is appropriate given [target] and
     * the current [preferredBrowser] setting.
     *
     * Special [target] values:
     * - `"_internal"` → force built-in browser, ignore preference.
     * - `"_external"` → force OS browser, ignore preference.
     *
     * Mirrors [LLWeb::loadURL].
     */
    fun loadURL(
        url: String,
        target: String = "",
        uuid: String = ""
    ) {
        when {
            target == "_internal" -> loadURLInternal(url, target = "", uuid = uuid)
            target == "_external" || useExternalBrowser(url) -> loadURLExternal(url, uuid = uuid)
            else -> loadURLInternal(url, target = target, uuid = uuid)
        }
    }

    /**
     * Opens [url] in the built-in browser floater (LLFloaterWebContent /
     * LLFloaterMarketplace in C++).
     *
     * Stub — wire to the UI floater registry before use.
     */
    fun loadURLInternal(
        url: String,
        target: String = "",
        uuid: String = "",
        devMode: Boolean = false
    ) {
        val escaped = escapeURL(url)
        TODO("Show internal web-content floater for url='$escaped' target='$target' uuid='$uuid' devMode=$devMode")
    }

    /**
     * Spawns the OS default browser for [url], optionally async so the call
     * returns immediately before the browser process has fully started.
     *
     * Shows a notification prompt (matching C++ `WebLaunchExternalTarget`)
     * before actually opening the URL.
     *
     * Stub — wire to the platform window / notification system before use.
     */
    fun loadURLExternal(
        url: String,
        async: Boolean = true,
        uuid: String = ""
    ) {
        val escaped = escapeURL(url)
        TODO("Show WebLaunchExternalTarget notification then spawn OS browser for url='$escaped' async=$async uuid='$uuid'")
    }

    // -----------------------------------------------------------------------
    // URL utilities
    // -----------------------------------------------------------------------

    /**
     * Percent-encodes characters that break URL parsing in older browsers.
     *
     * This is a "cheesy mini-escape" matching the C++ implementation: only
     * spaces (`' '` → `%20`) and backslashes (`'\'` → `%5C`) are encoded;
     * all other characters are left as-is.  A full RFC 3986 encoder should
     * be used for new code; this function exists for behavioural parity.
     */
    fun escapeURL(url: String): String = buildString(url.length + 8) {
        for (ch in url) {
            when (ch) {
                ' '  -> append("%20")
                '\\' -> append("%5C")
                else -> append(ch)
            }
        }
    }

    /**
     * Expands substitution tokens such as `[LANG]`, `[VERSION]`, `[OS]` in
     * [url], using [substitutions] as an override map.  Tokens not present
     * in [substitutions] are filled from viewer globals (version info, locale,
     * etc.).
     *
     * Stub — wire to the viewer's string-substitution / LLTrans layer.
     *
     * @param url           URL template containing `[TOKEN]` placeholders.
     * @param substitutions Override map of token name → value string.
     * @return Expanded URL string.
     */
    fun expandURLSubstitutions(
        url: String,
        substitutions: Map<String, String> = emptyMap()
    ): String {
        TODO("Expand [TOKEN] placeholders in url='$url' using substitutions=$substitutions plus viewer globals")
    }

    // -----------------------------------------------------------------------
    // Policy helper
    // -----------------------------------------------------------------------

    /**
     * Returns true when [url] should be opened in the OS browser rather than
     * the built-in browser.
     *
     * In the C++ viewer this inspects the `UseExternalBrowser` setting and
     * matches against known SL / Marketplace URL patterns.
     *
     * Stub — wire to the viewer control/settings layer before use.
     */
    fun useExternalBrowser(url: String): Boolean {
        // Default: obey the global preference setting.
        return preferredBrowser == PreferredBrowser.EXTERNAL_ONLY ||
            (preferredBrowser == PreferredBrowser.LL_INTERNAL_OTHERS_EXTERNAL &&
                !isSecondLifeURL(url))
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Returns true if [url] belongs to a Second Life or Linden domain. */
    private fun isSecondLifeURL(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("secondlife.com") ||
            lower.contains("lindenlab.com") ||
            lower.startsWith("secondlife://") ||
            lower.startsWith("hop://")
    }
}
