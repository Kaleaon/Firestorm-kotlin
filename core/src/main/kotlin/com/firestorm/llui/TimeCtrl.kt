package com.firestorm.llui

private const val AMPM_LEN = 3u
private const val MINUTES_MIN = 0u
private const val MINUTES_MAX = 59u
private const val HOURS_MIN = 1u
private const val HOURS_MAX = 12u
private const val MINUTES_PER_HOUR = 60u
private const val MINUTES_PER_DAY = 24u * MINUTES_PER_HOUR

enum class DayPeriod { AM, PM }

enum class EditingPart { HOURS, MINUTES, DAYPART, NONE }

class TimeCtrl(
    val label: String = "",
    val snapToMin: UInt = 5u,
    val allowEdit: Boolean = true,
    val textEnabledColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    val textDisabledColor: FloatArray = floatArrayOf(0.5f, 0.5f, 0.5f, 1f)
) {
    private var time: UInt = 0u

    var displayText: String = "12:00 AM"
        private set

    var cursorPos: Int = 0

    val commitCallbacks: MutableList<(TimeCtrl) -> Unit> = mutableListOf()

    init {
        updateText()
    }

    fun getTime24(): Float = time.toFloat() / MINUTES_PER_HOUR.toFloat()

    fun getHours24(): UInt = getTime24().toUInt()

    fun getMinutes(): UInt = time % MINUTES_PER_HOUR

    fun setTime24(t: Float) {
        val clamped = t.coerceIn(0f, 23.99f)
        time = (clamped * MINUTES_PER_HOUR.toFloat()).toUInt().let {
            ((clamped * MINUTES_PER_HOUR.toFloat() + 0.5f).toUInt())
        }
        updateText()
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        val keyUp = 0x103
        val keyDown = 0x104
        val keyReturn = 0x00D
        return when (key) {
            keyUp -> { onUpBtn(); true }
            keyDown -> { onDownBtn(); true }
            keyReturn -> { onCommit(); true }
            else -> false
        }
    }

    fun onFocusLost() {
        updateText()
        onCommit()
    }

    fun onUpBtn() {
        when (getEditingPart()) {
            EditingPart.HOURS -> increaseHours()
            EditingPart.MINUTES -> increaseMinutes()
            EditingPart.DAYPART -> switchDayPeriod()
            EditingPart.NONE -> {}
        }
        updateText()
        onCommit()
    }

    fun onDownBtn() {
        when (getEditingPart()) {
            EditingPart.HOURS -> decreaseHours()
            EditingPart.MINUTES -> decreaseMinutes()
            EditingPart.DAYPART -> switchDayPeriod()
            EditingPart.NONE -> {}
        }
        updateText()
        onCommit()
    }

    fun onTextEntry(text: String) {
        val h12 = parseHours(getHoursString(text))
        val m = parseMinutes(getMinutesString(text))
        val pm = parseAMPM(getAMPMString(text))

        val adjustedH12 = if (h12 == 12u) 0u else h12
        val h24 = if (pm) adjustedH12 + 12u else adjustedH12
        time = h24 * MINUTES_PER_HOUR + m
    }

    private fun increaseMinutes() {
        time = (time + snapToMin) % MINUTES_PER_DAY - (time % snapToMin)
    }

    private fun increaseHours() {
        time = (time + MINUTES_PER_HOUR) % MINUTES_PER_DAY
    }

    private fun decreaseMinutes() {
        if (time < snapToMin) {
            time = MINUTES_PER_DAY - time
        }
        time -= if (time % snapToMin != 0u) time % snapToMin else snapToMin
    }

    private fun decreaseHours() {
        time = if (time < MINUTES_PER_HOUR) {
            23u * MINUTES_PER_HOUR + time
        } else {
            time - MINUTES_PER_HOUR
        }
    }

    private fun isPM(): Boolean = time >= MINUTES_PER_DAY / 2u

    private fun switchDayPeriod() {
        time = if (isPM()) {
            time - MINUTES_PER_DAY / 2u
        } else {
            time + MINUTES_PER_DAY / 2u
        }
    }

    private fun updateText() {
        val h24 = getHours24()
        val m = getMinutes()
        val h12 = if (h24 > 12u) h24 - 12u else h24
        val displayH12 = if (h12 == 0u) 12u else h12
        val ampm = if (isPM()) "PM" else "AM"
        displayText = "%d:%02d %s".format(displayH12.toInt(), m.toInt(), ampm)
    }

    private fun getEditingPart(): EditingPart {
        val colonIndex = displayText.indexOf(':')
        val ampmStart = displayText.length - AMPM_LEN.toInt()
        return when {
            cursorPos <= colonIndex -> EditingPart.HOURS
            cursorPos > colonIndex && cursorPos <= ampmStart -> EditingPart.MINUTES
            cursorPos > ampmStart -> EditingPart.DAYPART
            else -> EditingPart.NONE
        }
    }

    private fun onCommit() {
        commitCallbacks.forEach { it(this) }
    }

    companion object {
        fun getHoursString(str: String): String {
            val colonIndex = str.indexOf(':')
            return if (colonIndex >= 0) str.substring(0, colonIndex) else str
        }

        fun getMinutesString(str: String): String {
            val colonIndex = str.indexOf(':')
            if (colonIndex < 0) return ""
            val start = colonIndex + 1
            val end = str.length - AMPM_LEN.toInt()
            return if (end > start) str.substring(start, end) else ""
        }

        fun getAMPMString(str: String): String {
            return if (str.length >= 2) str.substring(str.length - 2) else ""
        }

        fun isHoursStringValid(str: String): Boolean {
            if (str.length >= 3) return false
            val hours = str.toUIntOrNull() ?: return true
            return hours <= HOURS_MAX
        }

        fun isMinutesStringValid(str: String): Boolean {
            if (str.length >= 3) return false
            val minutes = str.toUIntOrNull() ?: return true
            return minutes <= MINUTES_MAX
        }

        fun isPMAMStringValid(str: String): Boolean {
            if (str.length < 2) return false
            val last = str.last()
            val second = str[str.length - 2]
            return last == 'M' && (second == 'P' || second == 'A')
        }

        fun parseHours(str: String): UInt {
            val hours = str.toUIntOrNull() ?: return HOURS_MIN
            return if (hours in HOURS_MIN..HOURS_MAX) hours else HOURS_MIN
        }

        fun parseMinutes(str: String): UInt {
            val minutes = str.toUIntOrNull() ?: return MINUTES_MIN
            return if (minutes <= MINUTES_MAX) minutes else MINUTES_MIN
        }

        fun parseAMPM(str: String): Boolean = str == "PM"
    }
}
