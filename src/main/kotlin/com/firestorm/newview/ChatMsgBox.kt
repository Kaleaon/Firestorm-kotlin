package com.firestorm.newview

open class ChatMsgBox(
    blockSpacing: Int = 10
) : TextBox() {

    private val mBlockSpacing: Int = blockSpacing

    fun addText(text: String, inputParams: StyleParams = StyleParams()) {
        val length = getLength()
        if (length > 0) {
            // Insert a visual separator segment just before the null terminator so
            // each chat block is visually delimited from the previous one.
            insertChatSeparatorSegment(length - 1, length - 1)
        }
        appendText(text, prependNewline = length > 0, params = inputParams)
    }

    private fun insertChatSeparatorSegment(start: Int, end: Int) {
        TODO("GPU: draw a horizontal grey line across full document width at this segment position")
    }
}
