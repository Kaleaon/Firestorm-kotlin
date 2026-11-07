/**
 * ViewerHelpUtil.kt
 * Kotlin port of llviewerhelputil.h / llviewerhelputil.cpp
 *
 * URL-building helpers for the viewer's Help system.
 * Turns a bare topic string into a fully-qualified help URL by applying
 * per-viewer substitutions (language, version, OS, debug mode, etc.).
 *
 * Original authors: Soft Linden, Linden Research, Inc.
 * LGPL-2.1 – Linden Research, Inc.
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

// ---------------------------------------------------------------------------
// Singleton: ViewerHelpUtil
// ---------------------------------------------------------------------------

/**
 * Static utility object for constructing help URLs.
 *
 * Corresponds to C++ `LLViewerHelpUtil` (static-only utility class).
 *
 * The C++ implementation delegates substitution expansion to
 * `LLWeb::expandURLSubstitutions`, which replaces tokens such as
 * `[LANGUAGE]`, `[VERSION]`, `[OS]`, `[TOPIC]`, and `[DEBUG_MODE]` in a
 * format string retrieved from `gSavedSettings("HelpURLFormat")`.  Those
 * platform-specific dependencies are stubbed here with `TODO()`.
 */
object ViewerHelpUtil {

    // ------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------

    /**
     * RFC-3986 "unreserved" characters that are safe to pass through
     * unescaped in a URL component.  The tilde (`~`) is intentionally
     * excluded because the viewer may assign it special meaning.
     *
     * Mirrors the `allowed` string in `LLViewerHelpUtil::helpURLEncode()`.
     */
    private val UNRESERVED_CHARS: Set<Char> =
        (('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('-', '.', '_')).toSet()

    /**
     * Default help URL format used when the settings system is unavailable.
     * Tokens: `{TOPIC}`, `{LANGUAGE}`, `{DEBUG_MODE}`.
     */
    private const val DEFAULT_HELP_URL_FORMAT =
        "https://wiki.firestormviewer.org/help/{LANGUAGE}/{TOPIC}{DEBUG_MODE}"

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Percent-encode a single URL component using the same character
     * allow-list as the C++ implementation.
     *
     * Corresponds to `LLViewerHelpUtil::helpURLEncode()`.
     *
     * Every character not in [UNRESERVED_CHARS] is percent-encoded.
     * This is stricter than [URLEncoder] (which allows `*`, `+`, etc.)
     * and matches the behaviour of the C++ `LLURI::escape()` call.
     */
    fun helpURLEncode(component: String): String {
        val sb = StringBuilder(component.length * 3)
        val bytes = component.toByteArray(StandardCharsets.UTF_8)
        for (b in bytes) {
            val c = (b.toInt() and 0xFF).toChar()
            if (c in UNRESERVED_CHARS) {
                sb.append(c)
            } else {
                sb.append('%')
                sb.append(String.format("%02X", b.toInt() and 0xFF))
            }
        }
        return sb.toString()
    }

    /**
     * Build a fully-qualified help URL for the given [topic].
     *
     * Corresponds to `LLViewerHelpUtil::buildHelpURL()`.
     *
     * The C++ version:
     * 1. Encodes the topic via [helpURLEncode].
     * 2. Appends `/debug` when the agent is god-like.
     * 3. Retrieves `HelpURLFormat` from `gSavedSettings`.
     * 4. Expands substitution tokens via `LLWeb::expandURLSubstitutions`,
     *    which also injects `[LANGUAGE]`, `[VERSION]`, `[OS]`, etc.
     *
     * Steps 2-4 involve platform singletons that are stubbed here.
     */
    fun buildHelpURL(topic: String): String {
        val encodedTopic = helpURLEncode(topic)
        val debugMode    = getDebugModeSuffix()
        val language     = getHelpLanguage()
        val urlFormat    = getHelpURLPrefix()

        // Expand the minimal set of substitutions we can resolve locally.
        return urlFormat
            .replace("{TOPIC}",      encodedTopic)
            .replace("{LANGUAGE}",   language)
            .replace("{DEBUG_MODE}", debugMode)
            // expands additional tokens ([VERSION], [OS], [CHANNEL], …)
            // via LLWeb::expandURLSubstitutions equivalent once the
            // settings and agent singletons are ported.
    }

    /**
     * Return the help URL format string.
     *
     * In C++ this was `gSavedSettings.getString("HelpURLFormat")`.
     * The default value is used until the settings system is ported.
     */
    fun getHelpURLPrefix(): String {
        // delegates to ViewerControl / gSavedSettings equivalent when ported:
        //       return ViewerControl.getString("HelpURLFormat")
        return DEFAULT_HELP_URL_FORMAT
    }

    /**
     * Return the BCP-47 language tag to embed in help URLs.
     *
     * The C++ `LLWeb::expandURLSubstitutions` injects `[LANGUAGE]` from the
     * viewer locale.  We use the JVM default locale here as a stand-in.
     */
    fun getHelpLanguage(): String {
        // reads from viewer's language setting rather than JVM default when settings are ported
        return Locale.getDefault().toLanguageTag().lowercase()
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Return `/debug` when the current agent has god-level privileges,
     * otherwise an empty string.
     *
     * In C++: `gAgent.isGodlike() ? "/debug" : ""`
     */
    private fun getDebugModeSuffix(): String {
        // delegates to LLAgent equivalent when agent is ported:
        //       return if (Agent.isGodlike()) "/debug" else ""
        return ""
    }
}
