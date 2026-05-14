package com.firestorm.newview

import com.firestorm.llcommon.LLSD
import com.firestorm.llui.LLView

object FSChatOptionsMenu {

    fun onMenuItemClick(userdata: LLSD, source: LLView?) {
        val option = userdata.asString()
        when (option) {
            "blocklist" -> {
                if (gSavedSettings.getBool("FSUseStandaloneBlocklistFloater")) {
                    FloaterReg.toggleInstance("fs_blocklist")
                } else {
                    val panel = FloaterSidePanelContainer.getPanel("people", "panel_people")
                        ?: return
                    if (isPanelInVisibleChain(panel)) {
                        FloaterReg.hideInstance("people")
                    } else {
                        FloaterSidePanelContainer.showPanel(
                            "people",
                            "panel_people",
                            mapOf("people_panel_tab_name" to "blocked_panel")
                        )
                    }
                }
            }
            "font_size_small"  -> gSavedSettings.setS32("ChatFontSize", 0)
            "font_size_medium" -> gSavedSettings.setS32("ChatFontSize", 1)
            "font_size_large"  -> gSavedSettings.setS32("ChatFontSize", 2)
            "font_size_huge"   -> gSavedSettings.setS32("ChatFontSize", 3)
            "new_message_notification" -> when (source) {
                is FSFloaterNearbyChat -> gSavedSettings.setBool(
                    "FSNotifyUnreadChatMessages",
                    !gSavedSettings.getBool("FSNotifyUnreadChatMessages")
                )
                is FSFloaterIM -> gSavedSettings.setBool(
                    "FSNotifyUnreadIMMessages",
                    !gSavedSettings.getBool("FSNotifyUnreadIMMessages")
                )
            }
        }
    }

    fun onMenuItemEnable(userdata: LLSD, source: LLView?): Boolean {
        val option = userdata.asString()
        return when (option) {
            "typing_chevron"         -> (source as? FSFloaterIM)?.isP2PChat ?: false
            "show_channel_selection" -> gSavedSettings.getBool("FSNearbyChatbar")
            "show_send_button"       -> gSavedSettings.getBool("FSNearbyChatbar")
            else -> false
        }
    }

    fun onMenuItemVisible(userdata: LLSD, source: LLView?): Boolean {
        val option = userdata.asString()
        return when (option) {
            "typing_chevron"         -> source is FSFloaterIM
            "show_chat_bar"          -> source is FSFloaterNearbyChat
            "show_channel_selection" -> source is FSFloaterNearbyChat
            "show_send_button"       -> source is FSFloaterNearbyChat
            "show_im_send_button"    -> source is FSFloaterIM
            "show_mini_icons"        -> !gSavedSettings.getBool("PlainTextChatHistory")
            else -> false
        }
    }

    fun onMenuItemCheck(userdata: LLSD, source: LLView?): Boolean {
        val option = userdata.asString()
        return when (option) {
            "blocklist" -> {
                gSavedSettings.getBool("FSUseStandaloneBlocklistFloater") &&
                    FloaterReg.instanceVisible("fs_blocklist")
            }
            "font_size_small"  -> gSavedSettings.getS32("ChatFontSize") == 0
            "font_size_medium" -> gSavedSettings.getS32("ChatFontSize") == 1
            "font_size_large"  -> gSavedSettings.getS32("ChatFontSize") == 2
            "font_size_huge"   -> gSavedSettings.getS32("ChatFontSize") == 3
            "new_message_notification" -> when (source) {
                is FSFloaterNearbyChat -> gSavedSettings.getBool("FSNotifyUnreadChatMessages")
                is FSFloaterIM         -> gSavedSettings.getBool("FSNotifyUnreadIMMessages")
                else -> false
            }
            else -> false
        }
    }

    private fun isPanelInVisibleChain(panel: LLPanelBase): Boolean {
        return false
    }
}
