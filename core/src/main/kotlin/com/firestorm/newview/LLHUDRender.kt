package com.firestorm.newview

fun hudRenderUtf8Text(
    str: String,
    posAgent: LLVector3,
    font: LLFontGL,
    style: UByte,
    shadow: Int,
    xOffset: Float,
    yOffset: Float,
    color: LLColor4,
    orthographic: Boolean
) {
    hudRenderText(str, posAgent, font, style, shadow, xOffset, yOffset, color, orthographic)
}

fun hudRenderText(
    wstr: String,
    posAgent: LLVector3,
    font: LLFontGL,
    style: UByte,
    shadow: Int,
    xOffset: Float,
    yOffset: Float,
    color: LLColor4,
    orthographic: Boolean
) {
    // no-op
}
