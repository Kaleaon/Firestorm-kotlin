package com.firestorm.newview

import java.util.UUID

fun fslslBridgeRequestSuccess(data: Map<String, Any>) {
    // debug-log only; avoid calling back into viewerToLSL here (infinite loop)
}

fun fslslBridgeRequestFailure(data: Map<String, Any>) {
    println("FSLSLBridgeRequest error: $data")
}

fun fslslBridgeRequestRadarPosSuccess(data: Map<String, Any>) {
    val content = data["content"] as? String ?: return
    val tokens = content.split(Regex("[, ]+")).filter { it.isNotEmpty() }
    var i = 0
    while (i + 1 < tokens.size) {
        val targetAv: UUID = try { UUID.fromString(tokens[i]) } catch (_: IllegalArgumentException) { i += 2; continue }
        val targetZ: Float = tokens[i + 1].toFloatOrNull() ?: 0f
        FSRadar.instance.getEntry(targetAv)?.setZOffset(targetZ)
        i += 2
    }
}
