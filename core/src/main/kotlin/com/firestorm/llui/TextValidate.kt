package com.firestorm.llui

object TextValidate {

    abstract class ValidatorImpl {
        var lastErrorName: String = ""
            private set
        var lastErrorValues: Map<String, Any> = emptyMap()
            private set
        var lastErrorShowTime: UInt = 0u
            private set

        abstract fun validate(str: String): Boolean

        protected fun setError(name: String, values: Map<String, Any> = emptyMap()): Boolean {
            lastErrorName = name
            lastErrorValues = values
            return false
        }

        protected fun resetError(): Boolean {
            lastErrorName = ""
            lastErrorValues = emptyMap()
            return true
        }

        fun setLastErrorShowTime() {
            lastErrorShowTime = (System.currentTimeMillis() / 1000).toUInt()
        }
    }

    class Validator(private val impl: ValidatorImpl? = null) {
        fun validate(str: String): Boolean = impl?.validate(str) ?: true

        operator fun invoke(str: String): Boolean = validate(str)

        fun showLastErrorUsingTimeout(timeoutSec: UInt = SHOW_LAST_ERROR_TIMEOUT_SEC) {
            val now = (System.currentTimeMillis() / 1000).toUInt()
            if (impl != null && now >= impl.lastErrorShowTime + timeoutSec) {
                impl.setLastErrorShowTime()
                // Surface the error; UI notification stubbed — use JVM equivalent
                println("Validation error: ${impl.lastErrorName}")
            }
        }

        companion object {
            val SHOW_LAST_ERROR_TIMEOUT_SEC: UInt = 30u
        }
    }

    private object FloatImpl : ValidatorImpl() {
        override fun validate(str: String): Boolean {
            val trimmed = str.trim()
            if (trimmed.isEmpty()) return resetError()
            var i = 0
            if (trimmed[0] == '-') i++
            while (i < trimmed.length) {
                val ch = trimmed[i]
                if (ch != '.' && !ch.isDigit()) {
                    return setError("Validator_ShouldBeDigitOrDot", mapOf("NR" to (i + 1), "CH" to ch.toString()))
                }
                i++
            }
            return resetError()
        }
    }

    private object IntImpl : ValidatorImpl() {
        override fun validate(str: String): Boolean {
            val trimmed = str.trim()
            if (trimmed.isEmpty()) return resetError()
            var i = 0
            if (trimmed[0] == '-') i++
            while (i < trimmed.length) {
                val ch = trimmed[i]
                if (!ch.isDigit()) {
                    return setError("Validator_ShouldBeDigit", mapOf("NR" to (i + 1), "CH" to ch.toString()))
                }
                i++
            }
            return resetError()
        }
    }

    private object PositiveS32Impl : ValidatorImpl() {
        override fun validate(str: String): Boolean {
            val trimmed = str.trim()
            if (trimmed.isEmpty()) return resetError()
            val first = trimmed[0]
            if (first == '-' || first == '0') {
                return setError("Validator_ShouldNotBeMinusOrZero", mapOf("CH" to first.toString()))
            }
            trimmed.forEachIndexed { i, ch ->
                if (!ch.isDigit()) {
                    return setError("Validator_ShouldBeDigit", mapOf("NR" to (i + 1), "CH" to ch.toString()))
                }
            }
            val v = trimmed.toLongOrNull() ?: 0L
            if (v <= 0) {
                return setError("Validator_InvalidNumericString", mapOf("STR" to trimmed))
            }
            return resetError()
        }
    }

    private object NonNegativeS32Impl : ValidatorImpl() {
        override fun validate(str: String): Boolean {
            val trimmed = str.trim()
            if (trimmed.isEmpty()) return resetError()
            if (trimmed[0] == '-') {
                return setError("Validator_ShouldNotBeMinus", mapOf("CH" to "-"))
            }
            trimmed.forEachIndexed { i, ch ->
                if (!ch.isDigit()) {
                    return setError("Validator_ShouldBeDigit", mapOf("NR" to (i + 1), "CH" to ch.toString()))
                }
            }
            val v = trimmed.toLongOrNull() ?: 0L
            if (v < 0) {
                return setError("Validator_InvalidNumericString", mapOf("STR" to trimmed))
            }
            return resetError()
        }
    }

    private object NonNegativeS32NoSpaceImpl : ValidatorImpl() {
        override fun validate(str: String): Boolean {
            if (str.isEmpty()) return resetError()
            if (str[0] == '-') {
                return setError("Validator_ShouldNotBeMinus", mapOf("CH" to "-"))
            }
            str.forEachIndexed { i, ch ->
                if (!ch.isDigit() || ch.isWhitespace()) {
                    return setError("Validator_ShouldBeDigitNotSpace", mapOf("NR" to (i + 1), "CH" to ch.toString()))
                }
            }
            val v = str.toLongOrNull() ?: 0L
            if (v < 0) {
                return setError("Validator_InvalidNumericString", mapOf("STR" to str))
            }
            return resetError()
        }
    }

    private object AlphaNumImpl : ValidatorImpl() {
        override fun validate(str: String): Boolean {
            str.forEachIndexed { i, ch ->
                if (!ch.isLetterOrDigit()) {
                    return setError("Validator_ShouldBeDigitOrAlpha", mapOf("NR" to (i + 1), "CH" to ch.toString()))
                }
            }
            return resetError()
        }
    }

    private object AlphaNumSpaceImpl : ValidatorImpl() {
        override fun validate(str: String): Boolean {
            str.forEachIndexed { i, ch ->
                if (!ch.isLetterOrDigit() && ch != ' ') {
                    return setError("Validator_ShouldBeDigitOrAlphaOrSpace", mapOf("NR" to (i + 1), "CH" to ch.toString()))
                }
            }
            return resetError()
        }
    }

    // Pipe excluded because old server file formats used | as multiline separator
    private object ASCIIPrintableNoPipeImpl : ValidatorImpl() {
        override fun validate(str: String): Boolean {
            str.forEachIndexed { i, ch ->
                val code = ch.code
                if (code < 0x20 || code > 0x7f || ch == '|' ||
                    (ch != ' ' && !ch.isLetterOrDigit() && !ch.isISOControl())) {
                    return setError("Validator_ShouldBeDigitOrAlphaOrPunct", mapOf("NR" to (i + 1), "CH" to ch.toString()))
                }
            }
            return resetError()
        }
    }

    private object ASCIIPrintableNoSpaceImpl : ValidatorImpl() {
        override fun validate(str: String): Boolean {
            str.forEachIndexed { i, ch ->
                val code = ch.code
                if (code <= 0x20 || code > 0x7f || ch.isWhitespace() ||
                    (!ch.isLetterOrDigit() && !ch.isISOControl())) {
                    return setError("Validator_ShouldBeDigitOrAlphaOrPunctNotSpace", mapOf("NR" to (i + 1), "CH" to ch.toString()))
                }
            }
            return resetError()
        }
    }

    private open class ASCIIImpl : ValidatorImpl() {
        override fun validate(str: String): Boolean {
            str.forEachIndexed { i, ch ->
                val code = ch.code
                if (code < 0x20 || code > 0x7f) {
                    return setError("Validator_ShouldBeASCII", mapOf("NR" to (i + 1), "CH" to ch.toString()))
                }
            }
            return resetError()
        }
    }

    private object ASCIINoLeadingSpaceImpl : ASCIIImpl() {
        override fun validate(str: String): Boolean {
            if (str.isNotEmpty() && str[0].isWhitespace()) return false
            return super.validate(str)
        }
    }

    // Newline (0x0A) is explicitly allowed for multiline server-stored text
    private object ASCIIWithNewLineImpl : ValidatorImpl() {
        override fun validate(str: String): Boolean {
            str.forEachIndexed { i, ch ->
                val code = ch.code
                if ((code < 0x20 && code != 0x0A) || code > 0x7f) {
                    return setError("Validator_ShouldBeNewLineOrASCII", mapOf("NR" to (i + 1), "CH" to ch.toString()))
                }
            }
            return resetError()
        }
    }

    val validateFloat              = Validator(FloatImpl)
    val validateInt                = Validator(IntImpl)
    val validatePositiveS32        = Validator(PositiveS32Impl)
    val validateNonNegativeS32     = Validator(NonNegativeS32Impl)
    val validateNonNegativeS32NoSpace = Validator(NonNegativeS32NoSpaceImpl)
    val validateAlphaNum           = Validator(AlphaNumImpl)
    val validateAlphaNumSpace      = Validator(AlphaNumSpaceImpl)
    val validateASCIIPrintableNoPipe  = Validator(ASCIIPrintableNoPipeImpl)
    val validateASCIIPrintableNoSpace = Validator(ASCIIPrintableNoSpaceImpl)
    val validateASCII              = Validator(ASCIIImpl())
    val validateASCIINoLeadingSpace   = Validator(ASCIINoLeadingSpaceImpl)
    val validateASCIIWithNewLine      = Validator(ASCIIWithNewLineImpl)

    val validators: Map<String, Validator> = mapOf(
        "ascii"                   to validateASCII,
        "float"                   to validateFloat,
        "int"                     to validateInt,
        "positive_s32"            to validatePositiveS32,
        "non_negative_s32"        to validateNonNegativeS32,
        "alpha_num"               to validateAlphaNum,
        "alpha_num_space"         to validateAlphaNumSpace,
        "ascii_printable_no_pipe" to validateASCIIPrintableNoPipe,
        "ascii_printable_no_space" to validateASCIIPrintableNoSpace,
        "ascii_with_newline"      to validateASCIIWithNewLine
    )
}
