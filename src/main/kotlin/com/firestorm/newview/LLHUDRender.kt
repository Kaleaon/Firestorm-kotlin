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
    TODO("GPU: camera direction culling - skip if wstr is empty or (not orthographic and dir dot atAxis <= 0)")

    val rightAxis: LLVector3
    val upAxis: LLVector3

    if (orthographic) {
        TODO("GPU: rightAxis = (0, -1/worldViewHeight, 0); upAxis = (0, 0, 1/worldViewHeight)")
    } else {
        TODO("GPU: camera.getPixelVectors(posAgent, upAxis, rightAxis)")
    }

    TODO("GPU: build rotation quaternion from camera frame for orthographic or perspective mode")

    TODO("GPU: renderPos = posAgent + floor(xOffset)*rightAxis + floor(yOffset)*upAxis")

    TODO("GPU: project renderPos to screen using glm::project with current modelview/projection and world view rect viewport")

    TODO("GPU: gGL.matrixMode(MM_PROJECTION); gGL.pushMatrix(); gGL.matrixMode(MM_MODELVIEW); gGL.pushMatrix(); LLUI.pushMatrix()")
    TODO("GPU: gl_state_for_2d(worldViewWidth, worldViewHeight); gViewerWindow.setup3DViewport()")

    TODO("GPU: adjust win_coord by subtracting world view rect origin")
    TODO("GPU: LLUI.loadIdentity(); gGL.loadIdentity()")
    TODO("GPU: LLUI.translate(winCoord.x / sScaleX, winCoord.y / sScaleY, -(winCoord.z * 2f - 1f))")

    TODO("GPU: font.render(wstr, 0, 0, 1, color, LEFT, BASELINE, style, shadow, wstr.length, 1000, rightX, useEllipses=false, useColor=true)")

    TODO("GPU: LLUI.popMatrix(); gGL.popMatrix(); gGL.matrixMode(MM_PROJECTION); gGL.popMatrix(); gGL.matrixMode(MM_MODELVIEW)")
}
