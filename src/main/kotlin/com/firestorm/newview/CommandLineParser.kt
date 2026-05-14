/**
 * CommandLineParser.kt
 * Kotlin port of llcommandlineparser.h / llcommandlineparser.cpp
 *
 * Handles defining and parsing the viewer's command-line arguments.
 * The C++ version backed its implementation with Boost.Program_options;
 * here a hand-rolled parser handles the same switches.
 *
 * Original authors: Linden Research, Inc.
 * LGPL-2.1 – Linden Research, Inc.
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// Singleton: CommandLineParser
// ---------------------------------------------------------------------------

/**
 * Parses and stores the viewer's command-line arguments.
 *
 * Corresponds to C++ `LLCommandLineParser` (instance class used as a
 * singleton in practice) merged with `LLControlGroupCLP`.  Switches that
 * take values are stored in an [OptionSet]; flag-only switches are stored
 * with an empty values list.
 *
 * Key switches recognised:
 *   --set KEY VALUE   override a settings variable
 *   --loginuri URI    set the login URI
 *   --grid GRID       select the grid
 *   --channel CH      set the release channel
 *   --logfile FILE    redirect logs to FILE
 *   --noprobe         skip hardware probe
 *   --novoice         disable voice
 *   --url URL         open a secondlife:// URL on start-up (must be last)
 *   --help            print usage and exit
 */
object CommandLineParser {

    // ------------------------------------------------------------------
    // Public types
    // ------------------------------------------------------------------

    /**
     * A parsed option together with its (possibly empty) value tokens.
     *
     * Corresponds to `token_vector_t` in C++ (which was
     * `std::vector<std::string>`).
     */
    data class OptionSet(
        /** Long option name, without the leading `--`. */
        val name: String,
        /** Ordered list of value tokens associated with this option. */
        val values: List<String>,
    )

    /**
     * Describes a single recognised option so the parser knows how many
     * tokens to consume and whether it may appear multiple times.
     *
     * Corresponds to the parameters of `LLCommandLineParser::addOptionDesc()`.
     */
    data class OptionDesc(
        val longName: String,
        val shortName: String? = null,
        val tokenCount: Int = 0,
        val description: String = "",
        val composing: Boolean = false,
        val positional: Boolean = false,
        val lastOption: Boolean = false,
        val notifyCallback: ((List<String>) -> Unit)? = null,
    )

    // ------------------------------------------------------------------
    // Internal state
    // ------------------------------------------------------------------

    /** Registered option descriptors, keyed by long name. */
    private val optionDescs: MutableMap<String, OptionDesc> = mutableMapOf()

    /** Short-name → long-name alias map. */
    private val shortNameIndex: MutableMap<String, String> = mutableMapOf()

    /** Parsed results, keyed by long option name. */
    private val parsedOptions: MutableMap<String, OptionSet> = mutableMapOf()

    /** The last error message produced during parsing, empty if none. */
    var errorMessage: String = ""
        private set

    // ------------------------------------------------------------------
    // Option registration
    // ------------------------------------------------------------------

    /**
     * Register a recognised option and how many value tokens it consumes.
     *
     * Mirrors `LLCommandLineParser::addOptionDesc()`.
     */
    fun addOptionDesc(desc: OptionDesc) {
        optionDescs[desc.longName] = desc
        desc.shortName?.let { shortNameIndex[it] = desc.longName }
    }

    /** Convenience builder that mirrors the C++ parameter list directly. */
    fun addOptionDesc(
        longName: String,
        tokenCount: Int = 0,
        description: String = "",
        shortName: String? = null,
        composing: Boolean = false,
        positional: Boolean = false,
        lastOption: Boolean = false,
        notifyCallback: ((List<String>) -> Unit)? = null,
    ) {
        addOptionDesc(
            OptionDesc(
                longName = longName,
                shortName = shortName,
                tokenCount = tokenCount,
                description = description,
                composing = composing,
                positional = positional,
                lastOption = lastOption,
                notifyCallback = notifyCallback,
            )
        )
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    /**
     * Parse [args] (typically `argv` minus `argv[0]`) and store the results.
     *
     * Returns `true` on success, `false` on error (see [errorMessage]).
     *
     * Handles the key viewer switches listed in the class-level doc comment.
     * Mirrors `LLCommandLineParser::parseCommandLine(argc, argv)`.
     *
     * Rules that mirror the C++ behaviour:
     * - Any option declared with `lastOption = true` stops further parsing.
     * - `--set KEY VALUE` stores a two-token OptionSet under key "set".
     * - Unknown options are stored as-is (with whatever tokens follow).
     */
    fun parseCommandLine(args: Array<String>): Boolean {
        errorMessage = ""
        parsedOptions.clear()

        var i = 0
        var pastLastOption = false

        try {
            while (i < args.size) {
                if (pastLastOption) {
                    // Tokens after a last_option switch are dropped, matching
                    // the LLCLPLastOption handling in the C++ implementation.
                    i++
                    continue
                }

                val arg = args[i]

                if (!arg.startsWith("-")) {
                    // Positional argument – not currently handled.
                    i++
                    continue
                }

                // Normalise: strip leading dashes to get the bare name.
                val rawName = when {
                    arg.startsWith("--") -> arg.removePrefix("--")
                    arg.startsWith("-")  -> arg.removePrefix("-")
                    else                 -> arg
                }

                // Resolve short names.
                val longName = shortNameIndex[rawName] ?: rawName

                val desc = optionDescs[longName]
                val tokenCount = desc?.tokenCount ?: 0
                val composing  = desc?.composing ?: false
                val isLast     = desc?.lastOption ?: false

                // Collect the expected number of value tokens.
                val values = mutableListOf<String>()
                for (t in 1..tokenCount) {
                    val tokenIdx = i + t
                    if (tokenIdx < args.size && !args[tokenIdx].startsWith("-")) {
                        values.add(args[tokenIdx])
                    } else {
                        // Fewer tokens than expected.
                        errorMessage = "Option --$longName expects $tokenCount value(s) " +
                            "but only ${values.size} were supplied."
                        return false
                    }
                }

                // Honour composing: accumulate rather than replace.
                if (composing && parsedOptions.containsKey(longName)) {
                    val existing = parsedOptions[longName]!!
                    parsedOptions[longName] = existing.copy(values = existing.values + values)
                } else {
                    parsedOptions[longName] = OptionSet(longName, values)
                }

                if (isLast) {
                    pastLastOption = true
                }

                i += 1 + tokenCount
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown parse error"
            return false
        }

        return true
    }

    /**
     * Parse options from a whitespace-delimited string.
     *
     * Mirrors `LLCommandLineParser::parseCommandLineString()`.
     * Quoted tokens are handled by splitting on whitespace outside of
     * double-quoted regions.
     */
    fun parseCommandLineString(str: String): Boolean {
        if (str.isBlank()) return true
        val tokens = tokenise(str)
        return parseCommandLine(tokens.toTypedArray())
    }

    /**
     * Invoke any registered notify-callbacks for all parsed options.
     *
     * Mirrors `LLCommandLineParser::notify()`.
     */
    fun notify(): Boolean {
        return try {
            for ((name, optSet) in parsedOptions) {
                optionDescs[name]?.notifyCallback?.invoke(optSet.values)
            }
            true
        } catch (e: Exception) {
            errorMessage = e.message ?: "Notify error"
            false
        }
    }

    // ------------------------------------------------------------------
    // Query API
    // ------------------------------------------------------------------

    /** Return `true` if [name] was present on the command line. */
    fun hasOption(name: String): Boolean = parsedOptions.containsKey(name)

    /**
     * Return the [OptionSet] for [name], or `null` if it was not specified.
     *
     * Mirrors `LLCommandLineParser::getOption()` (which returned an empty
     * token_vector_t for missing options; here `null` is more idiomatic).
     */
    fun getOption(name: String): OptionSet? = parsedOptions[name]

    /**
     * Print a summary of all parsed options to stdout.
     *
     * Mirrors `LLCommandLineParser::printOptions()`.
     */
    fun printOptions() {
        for ((name, optSet) in parsedOptions) {
            println("$name: ${optSet.values.joinToString(" ")}")
        }
    }

    /**
     * Print the description of all registered options to stdout.
     *
     * Mirrors `LLCommandLineParser::printOptionsDesc()`.
     */
    fun printOptionsDesc() {
        for ((name, desc) in optionDescs) {
            val short = desc.shortName?.let { " (-$it)" } ?: ""
            val tokens = if (desc.tokenCount > 0) " <${if (desc.tokenCount > 1) "args" else "arg"}>" else ""
            println("  --$name$short$tokens")
            if (desc.description.isNotEmpty()) println("      ${desc.description}")
        }
    }

    // ------------------------------------------------------------------
    // ControlGroup integration (LLControlGroupCLP equivalent)
    // ------------------------------------------------------------------

    /**
     * Configure the parser from an XML-based LLSD config file and bind each
     * described option to a setting in [controlGroup].
     *
     * Corresponds to `LLControlGroupCLP::configure()`.
     */
    fun configure(configFilename: String, controlGroup: Any?) {
        // parse the LLSD XML config file, iterate its map entries, and
        //       call addOptionDesc() for each, wiring a notifyCallback that
        //       calls controlGroup.getControl(mapTo).setValue(…) — when LLSD and control-group are ported
        System.err.println("CommandLineParser: configure not yet implemented")
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Split [str] on whitespace, respecting double-quoted regions.
     *
     * Mirrors the Boost.Tokenizer logic in `parseCommandLineString()`.
     */
    private fun tokenise(str: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < str.length) {
            val c = str[i]
            when {
                c == '"' -> {
                    inQuotes = !inQuotes
                }
                c == '\\' && i + 1 < str.length -> {
                    // Escape: consume the next character literally.
                    current.append(str[i + 1])
                    i++ // skip escaped char
                }
                c.isWhitespace() && !inQuotes -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(c)
            }
            i++
        }

        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }
}
