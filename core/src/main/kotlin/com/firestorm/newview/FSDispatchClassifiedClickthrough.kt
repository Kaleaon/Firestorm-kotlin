package com.firestorm.newview

import java.util.UUID

// Dispatcher handler for the "classifiedclickthrough" generic message.
// strings[0]=classified_id, [1]=teleport_clicks, [2]=map_clicks, [3]=profile_clicks
class FSDispatchClassifiedClickThrough : LLDispatchHandler {

    override fun invoke(
        dispatcher: LLDispatcher,
        key: String,
        invoice: UUID,
        strings: List<String>
    ): Boolean {
        if (strings.size != 4) return false

        val classifiedId = UUID.fromString(strings[0])
        val teleportClicks = strings[1].toIntOrNull() ?: return false
        val mapClicks = strings[2].toIntOrNull() ?: return false
        val profileClicks = strings[3].toIntOrNull() ?: return false

        LLPanelProfileClassified.setClickThrough(
            classifiedId, teleportClicks, mapClicks, profileClicks, false
        )

        return true
    }
}
