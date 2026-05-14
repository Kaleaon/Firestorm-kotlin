package com.firestorm.newview

import java.util.UUID
import java.util.regex.Pattern

typealias AssetType = String

data class ScriptQueueData(val item: InventoryItem?)

class InventoryItem(val name: String, val assetUuid: UUID, val type: AssetType)

open class ScriptEdCore(
    var mUserdata: Any? = null,
    var mEditor: EditorWidget? = null,
    var mPostEditor: PreprocViewer? = null,
    var mPostScript: String = "",
    var mErrorList: ErrorListWidget? = null
) {
    open fun enableSave(enable: Boolean) {}
    open fun doSaveComplete(ctx: Any?, close: Boolean, sync: Boolean) {}
}

class EditorWidget { fun getText(): String = "" }
class PreprocViewer { fun setText(s: String) {} }
class ErrorListWidget {
    fun addCommentText(msg: String) {}
    fun addElement(row: Map<String, Any>) {}
}
class Preview(val item: InventoryItem?)

private const val ENCODE_START = "//start_unprocessed_text\n/*"
private const val ENCODE_END = "*/\n//end_unprocessed_text"

class FSLSLPreprocessor private constructor(
    val mCore: ScriptEdCore? = null,
    var mWaving: Boolean = false,
    var mClose: Boolean = false,
    var mSync: Boolean = false,
    val mStandalone: Boolean = false
) {
    var mMainScriptName: String = ""
    var mDefinitionCaching: Boolean = false
    val cachingFiles: MutableSet<String> = mutableSetOf()
    val defcachedFiles: MutableSet<String> = mutableSetOf()
    var mScript: String = ""
    var mAssetID: UUID = UUID.randomUUID()
    var mData: ScriptQueueData? = null
    var mType: AssetType = ""

    constructor(corep: ScriptEdCore) : this(mCore = corep, mStandalone = false)
    constructor() : this(mStandalone = true)

    companion object {
        val cachedAssetids: MutableMap<String, UUID> = mutableMapOf()

        fun monoDirective(text: String, agentInv: Boolean = true): Boolean {
            return when {
                text.contains("//mono\n") -> true
                text.contains("//lsl2\n") -> false
                else -> agentInv
            }
        }

        fun findInventoryByName(name: String): UUID? {
            return null
        }

        fun fsProcCacheCallback(uuid: UUID, type: AssetType, userdata: Any?, result: Int) {
            System.err.println("FSLSLPreprocessor: fsProcCacheCallback not yet implemented")
        }
    }

    fun encode(script: String): String {
        var otext = decode(script)

        otext = otext.replace(Regex("([/*])(?=[/*|])"), "$1|")
        otext = ENCODE_START + otext + ENCODE_END
        otext += "\n//nfo_preprocessor_version 0"
        otext += "\n//program_version Firestorm"

        val timeStr = java.time.Instant.now().toString()
        otext += "\n//last_compiled $timeStr"
        otext += "\n"
        otext += if (monoDirective(script)) "//mono\n" else "//lsl2\n"

        return otext
    }

    fun decode(script: String): String {
        val startLen = ENCODE_START.length
        val tip = if (script.length >= startLen) script.substring(0, startLen) else script
        if (tip != ENCODE_START) return script

        val end = script.indexOf(ENCODE_END)
        if (end == -1) return script

        val data = script.substring(startLen, end)
        return data.replace(Regex("([/*])\\|"), "$1")
    }

    fun lslopt(script: String): String {
        return try {
            lsloptInternal(script)
        } catch (e: Exception) {
            displayError("Optimizer error: ${e.message}")
            throw e
        }
    }

    private fun lsloptInternal(scriptIn: String): String {
        val keptFunctions = mutableSetOf<String>()
        val functions = mutableMapOf<String, String>()
        val gvars = mutableListOf<Pair<String, String>>()

        val CMNT = """//[^\n]*+\n|/\*(?:(?!\*/).)*+\*/"""
        val SPC = """[^\]\[{}()<>@A-Za-z0-9_.,:;!~&|^"=%/*+-]"""
        val REQ_SPC = """(?:$CMNT|$SPC)++"""
        val OPT_SPC = """(?:$CMNT|$SPC)*+"""
        val TYPE_ID = """[a-z]++"""
        val IDENT = """[A-Za-z_][A-Za-z0-9_]*+"""
        val CMNT_OR_STR = """$CMNT|"(?:[^"\\]|\\[^\n])*+""""

        val finddecls = Pattern.compile(
            """(?s)(^$OPT_SPC$TYPE_ID$REQ_SPC($IDENT)$OPT_SPC(?:=(?:$CMNT_OR_STR|[^;])++)?;)""" +
            """|(^$OPT_SPC(?:$TYPE_ID$REQ_SPC)?($IDENT))$OPT_SPC\(""" +
            """|(^$OPT_SPC(?:$CMNT_OR_STR|(?!$CMNT_OR_STR|(?<![A-Za-z0-9_])default(?![A-Za-z0-9_])).)*+(?<![A-Za-z0-9_])default(?![A-Za-z0-9_]))"""
        )

        var top = "\n$scriptIn"
        var bottom = ""

        var m = finddecls.matcher(top)
        while (m.find()) {
            val len: Int
            when {
                m.group(1) != null -> {
                    gvars.add(Pair(m.group(2) ?: "", m.group(0)))
                    len = m.end()
                }
                m.group(3) != null -> {
                    val funcname = m.group(4) ?: ""
                    val funcb = scopeExtract(top, 0)
                    functions[funcname] = funcb
                    len = funcb.length
                }
                else -> {
                    bottom = top
                    break
                }
            }
            top = top.substring(len)
            m = finddecls.matcher(top)
        }

        if (bottom.isEmpty()) return scriptIn

        var repass: Boolean
        do {
            repass = false
            for ((funcname, function) in functions) {
                if (!keptFunctions.contains(funcname)) {
                    val findcalls = Pattern.compile(
                        """(?s)(?<![A-Za-z0-9_])($funcname)$OPT_SPC\(""" +
                        """|(?:$CMNT_OR_STR|(?!$CMNT_OR_STR|(?<![A-Za-z0-9_])$funcname$OPT_SPC\().)"""
                    )
                    val cm = findcalls.matcher(bottom)
                    while (cm.find()) {
                        if (cm.group(1) != null) {
                            keptFunctions.add(funcname)
                            bottom = function + bottom
                            repass = true
                            break
                        }
                    }
                }
            }
        } while (repass)

        for ((varname, declaration) in gvars.asReversed()) {
            val findvcalls = Pattern.compile(
                """(?s)(?<![a-zA-Z0-9_.])($varname)(?![a-zA-Z0-9_"])""" +
                """|(?:$CMNT_OR_STR|(?!$CMNT_OR_STR|(?<![a-zA-Z0-9_.])$varname(?![a-zA-Z0-9_"])).)"""
            )
            val vm = findvcalls.matcher(bottom)
            if (vm.find() && vm.group(1) != null) {
                bottom = declaration + bottom
            }
        }

        return bottom
    }

    private fun scopeExtract(text: String, fstart: Int, left: Char = '{', right: Char = '}'): String {
        if (fstart >= text.length) return "begin out of bounds"
        var cursor = fstart
        var noscoped = true
        var inLiteral = false
        var count = 0
        var ltoken = ' '
        while ((count > 0 || noscoped) && cursor < text.length) {
            var token = text[cursor]
            if (token == '"' && ltoken != '\\') {
                inLiteral = !inLiteral
            } else if (token == '\\' && ltoken == '\\') {
                token = ' '
            } else if (!inLiteral) {
                if (token == left) { count++; noscoped = false }
                else if (token == right) { count--; noscoped = false }
            }
            ltoken = token
            cursor++
        }
        return text.substring(fstart, cursor)
    }

    fun lslcomp(script: String): String {
        return try {
            val shredded = shred(script)
            shredded.replace(Regex("""(\s+)"""), "\n")
        } catch (e: Exception) {
            displayError("Compress error: ${e.message}")
            throw e
        }
    }

    private fun shred(text: String): String {
        if (text.isEmpty()) return "No text to shredder."
        val sb = StringBuilder(text)
        var cursor = 0
        var ltoken = ' '
        while (cursor < sb.length) {
            var token = sb[cursor]
            if (token == '"' && ltoken != '\\') {
                ltoken = token
                cursor++
                while (cursor < sb.length) {
                    token = sb[cursor]
                    if (token == '\\' && ltoken == '\\') token = ' '
                    if (token == '"' && ltoken != '\\') break
                    ltoken = token
                    cursor++
                }
            } else if (token == '\\' && ltoken == '\\') {
                token = ' '
            }
            val code = token.code
            if (code != 0xA && code != 0x9 && (
                    code < 0x20 || token == '#' || token == '$' ||
                    token == '\\' || token == '\'' || token == '?' || code >= 0x7F)) {
                sb[cursor] = ' '
            }
            ltoken = token
            cursor++
        }
        return sb.toString()
    }

    fun preprocessScript(close: Boolean = false, sync: Boolean = false, defcache: Boolean = false) {
        mClose = close
        mSync = sync
        mDefinitionCaching = defcache
        cachingFiles.clear()
        displayMessage("Preprocessor starting…")

        if (mMainScriptName.isEmpty()) {
            val preview = mCore?.mUserdata as? Preview
            mMainScriptName = preview?.item?.name ?: "(Unknown)"
        }
        cachedAssetids[mMainScriptName] = UUID.nameUUIDFromBytes(ByteArray(16))
        System.err.println("FSLSLPreprocessor: preprocessScript not yet implemented")
    }

    fun preprocessScript(assetId: UUID, data: ScriptQueueData, type: AssetType, scriptData: String) {
        if (data.item == null) return

        val script = decode(scriptData)
        mScript = script
        mAssetID = assetId
        mData = data
        mType = type
        mDefinitionCaching = false
        cachingFiles.clear()
        displayMessage("Preprocessor starting…")

        mMainScriptName = data.item.name
        cachedAssetids[mMainScriptName] = UUID.nameUUIDFromBytes(ByteArray(16))
        System.err.println("FSLSLPreprocessor: preprocessScript not yet implemented")
    }

    fun startProcess() {
        if (mWaving) return
        mWaving = true

        val rawInput = if (mStandalone) mScript else mCore?.mEditor?.getText() ?: ""

        val disabledIdx = rawInput.indexOf("//fspreprocessor off")
        val preprocessorEnabled = disabledIdx == -1

        if (!preprocessorEnabled) {
            val lineNum = rawInput.substring(0, disabledIdx).count { it == '\n' }
            displayMessage("Preprocessor disabled by script marker at line $lineNum")
        }

        val input = if (preprocessorEnabled) normalizeMultilineStrings(rawInput) + "\n" else rawInput
        var output = ""
        var errored = false
        var lackDefault = false

        if (preprocessorEnabled) {
            try {
                output = runBoostWaveEquivalent(input)
            } catch (e: Exception) {
                errored = true
                displayError("Preprocessor error: ${e.message}")
            }
        }

        if (preprocessorEnabled && !errored) {
            output = tryLazyLists(output) ?: run { errored = true; output }
            if (!errored) {
                val (switched, ld) = trySwitchStatements(output)
                lackDefault = ld
                output = switched ?: run { errored = true; output }
            }
        }

        if (!mDefinitionCaching) {
            if (!errored) {
                output = tryOptimizer(output) ?: run { errored = true; output }
            } else {
                if (output.length > 128 * 1024) {
                    output = output.substring(0, 128 * 1024)
                    displayError("Preprocessor output truncated to prevent viewer freeze")
                }
            }

            if (!errored) {
                output = tryCompress(output) ?: run { errored = true; output }
            }

            output = if (preprocessorEnabled) encode(rawInput) + "\n\n" + output else rawInput

            if (mStandalone) {
                System.err.println("FSLSLPreprocessor: startProcess not yet implemented")
            } else {
                mCore?.mPostEditor?.setText(output)
                mCore?.mPostScript = output
                mCore?.enableSave(true)
                mCore?.doSaveComplete(mCore, mClose, mSync)
            }
        }

        if (lackDefault) displayMessage("Warning: switch statement missing default label")
        mWaving = false
    }

    private fun normalizeMultilineStrings(input: String): String {
        val sb = StringBuilder()
        var state = 0
        var nlines = 0
        for (ch in input) {
            when (state) {
                1 -> when (ch) {
                    '\n' -> { sb.append("\\n"); nlines++; continue }
                    '\\' -> state = 2
                    '"' -> {
                        sb.append('"')
                        repeat(nlines) { sb.append('\n') }
                        nlines = 0
                        state = 0
                        continue
                    }
                }
                2 -> state = 1
                3 -> state = when (ch) { '*' -> 4; '/' -> 6; else -> 0 }
                4 -> if (ch == '*') state = 5
                5 -> state = when { ch == '/' -> 0; ch != '*' -> 4; else -> 5 }
                6 -> if (ch == '\n') state = 0
                else -> when (ch) { '"' -> state = 1; '/' -> state = 3 }
            }
            sb.append(ch)
        }
        return sb.toString()
    }

    private fun runBoostWaveEquivalent(input: String): String {
        System.err.println("FSLSLPreprocessor: runBoostWaveEquivalent not yet implemented")
        return ""
    }

    private fun tryLazyLists(output: String): String? {
        return try {
            displayMessage("Reformatting lazy lists…")
            reformatLazyLists(output)
        } catch (e: Exception) {
            displayError("Lazy list error: ${e.message}")
            null
        }
    }

    private fun reformatLazyLists(script: String): String {
        val lazySetFunc = """
            list lazy_list_set(list L, integer i, list v)
            {
                while (llGetListLength(L) < i)
                    L = L + 0;
                return llListReplaceList(L, v, i, i);
            }
        """.trimIndent()

        var result = script.replace(
            Regex("""([a-zA-Z_][a-zA-Z0-9_]*)\s*\[([^\]]+)\]\s*=(?!=)\s*(.+?)(;|\))""",
                setOf(RegexOption.DOT_MATCHES_ALL)),
            { mr -> "${mr.groupValues[1]}=lazy_list_set(${mr.groupValues[1]},${mr.groupValues[2]},[${mr.groupValues[3]}])${mr.groupValues[4]}" }
        )
        result = "$lazySetFunc\n$result"
        return result
    }

    private fun trySwitchStatements(output: String): Pair<String?, Boolean> {
        return try {
            displayMessage("Reformatting switch statements…")
            var lackDefault = false
            val result = reformatSwitchStatements(output) { lackDefault = true }
            Pair(result, lackDefault)
        } catch (e: Exception) {
            displayError("Switch statement error: ${e.message}")
            Pair(null, false)
        }
    }

    private fun reformatSwitchStatements(script: String, onLackDefault: () -> Unit): String {
        val switchPattern = Regex("""(?<![A-Za-z0-9_])switch\s*(\()""")
        var result = script
        var offset = 0
        for (mr in switchPattern.findAll(script)) {
            val matchStart = mr.range.first + offset
            val label = quickLabel()
            onLackDefault()
            result = result.substring(0, matchStart) +
                "{ jump $label; @$label;\n" +
                result.substring(matchStart + mr.value.length)
            offset += ("{ jump $label; @$label;\n".length - mr.value.length)
        }
        return result
    }

    private fun tryOptimizer(output: String): String? {
        return try {
            displayMessage("Optimizer starting…")
            lslopt(output)
        } catch (e: Exception) {
            displayError("Optimizer unexpected error: ${e.message}")
            null
        }
    }

    private fun tryCompress(output: String): String? {
        return try {
            displayMessage("Compression starting…")
            lslcomp(output)
        } catch (e: Exception) {
            displayError("Compress unexpected error: ${e.message}")
            null
        }
    }

    private fun quickLabel(): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        return "c" + (1..5).map { chars.random() }.joinToString("")
    }

    fun displayMessage(msg: String) {
        if (mStandalone) {
            System.err.println("FSLSLPreprocessor: displayMessage not yet implemented")
        } else {
            mCore?.mErrorList?.addCommentText(msg)
        }
    }

    fun displayError(err: String) {
        if (mStandalone) {
            System.err.println("FSLSLPreprocessor: displayError not yet implemented")
        } else {
            mCore?.mErrorList?.addElement(mapOf(
                "columns" to listOf(mapOf("value" to err, "font" to "SANSSERIF_SMALL"))
            ))
        }
    }
}
