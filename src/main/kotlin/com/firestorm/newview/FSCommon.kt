/**
 * @file FSCommon.kt
 * @brief Central object for common utility functions in Firestorm.
 *
 * Ported from fscommon.h / fscommon.cpp
 * Original copyright (c) 2012 Ansariel Hiller @ Second Life
 * Phoenix Firestorm Project — LGPL v2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

/** Z-offset sentinel for avatars whose height is unknown. */
const val AVATAR_UNKNOWN_Z_OFFSET: Double = -1.0

/** Range sentinel for avatars whose distance is unknown. */
const val AVATAR_UNKNOWN_RANGE: Float = -1f

// Known Linden-staff last names used to identify LL employees on SL main grid.
private val LINDEN_LAST_NAMES = setOf("Linden", "Mole", "ProductEngine", "Scout", "Tester")

// Default texture UUIDs bundled with the viewer.
private val DEFAULT_TEXTURE_IDS: Set<String> = setOf(
    "89556747-24cb-43ed-920b-47caed15465f", // LL_DEFAULT_WOOD_UUID
    "87d41ffc-7b19-48c8-9d83-fc88cc09f5d8", // LL_DEFAULT_STONE_UUID
    "6262e388-bf59-4b9b-8f18-8e21f9e7e8c0", // LL_DEFAULT_METAL_UUID
    "f3a1c8e4-bcc5-4e9a-9875-e53f30da3b22", // LL_DEFAULT_GLASS_UUID
    "c228d1cf-4b5d-4ba8-84f4-899a0796aa97", // LL_DEFAULT_FLESH_UUID
    "aaed5ffd-d781-4f6a-9d28-bc5c68cb44dc", // LL_DEFAULT_PLASTIC_UUID
    "fcf5f020-9a93-45ef-a3b0-cdb0378c8438", // LL_DEFAULT_RUBBER_UUID
    "6bc10bb3-76af-4a22-8671-12d3e66b5a4e", // LL_DEFAULT_LIGHT_UUID
    "5748decc-f629-461c-9a36-a35a221fe21f", // IMG_WHITE / UIImgWhiteUUID (legacy)
    "8dcd4a48-2d37-4909-9f78-f7a9eb4ef903", // UIImgTransparentUUID
    "f54a0c32-3cd1-d49a-5b4f-7b792bebc204", // UIImgInvisibleUUID
    "6522e74d-1660-4e7f-b601-6f48c1659a77", // UIImgDefaultEyesUUID
    "7ca39b4c-bd19-4699-aff7-f93fd03d3e7b", // UIImgDefaultHairUUID
)

// ---------------------------------------------------------------------------
// FSCommon singleton object
// ---------------------------------------------------------------------------

/**
 * Central utility namespace for Firestorm-specific helper functions.
 *
 * Corresponds to the `FSCommon` namespace in C++.
 */
object FSCommon {

    /**
     * Tracks the number of ObjectAdd messages sent to the simulator.
     *
     * HACK: works around a LL design flaw where _PREHASH_ObjectAdd,
     * _PREHASH_RezObject and _PREHASH_RezObjectFromNotecard all return
     * the same object-update packet.
     */
    var objectAddMsgCount: Int = 0

    // -----------------------------------------------------------------------
    // Chat helpers
    // -----------------------------------------------------------------------

    /**
     * Post a system-sourced [message] to the nearby-chat channel.
     */
    fun reportToNearbyChat(message: String) {
        TODO("Requires LLNotificationManager / chat pipeline integration")
    }

    /**
     * Perform token substitution in [text] using the [args] map and return
     * the result (mirrors `LLStringUtil::format`).
     */
    fun formatString(text: String, args: Map<String, String>): String {
        var result = text
        for ((key, value) in args) {
            result = result.replace("[$key]", value)
        }
        return result
    }

    /**
     * Returns `true` when [text] starts with an IRC-style `/me ` or `/me'`
     * emote prefix.
     */
    fun isIrcMePrefix(text: String): Boolean {
        if (text.length < 4) return false
        val prefix = text.substring(0, 4)
        return prefix == "/me " || prefix == "/me'"
    }

    /**
     * URL-decode a percent-encoded [name] string (mirrors `curl_unescape`).
     */
    fun unescapeName(name: String): String {
        return java.net.URLDecoder.decode(name, Charsets.UTF_8)
    }

    // -----------------------------------------------------------------------
    // Chat-text transformations
    // -----------------------------------------------------------------------

    /**
     * If the user setting `AutoCloseOOC` is enabled, append the matching
     * closing bracket sequence when an OOC opener `((` or `[[` is present
     * without a corresponding closer.
     */
    fun applyAutoCloseOoc(message: String): String {
        // Setting lookup is stubbed – production code must query gSavedSettings.
        val autoClose = true // TODO: read from settings
        if (!autoClose) return message

        return when {
            message.contains("(( ") && !message.contains("))") -> "$message ))"
            message.contains("((") && !message.contains("))") ->
                if (message.endsWith(")")) "$message ))" else "$message))"
            message.contains("[[ ") && !message.contains("]]") -> "$message ]]"
            message.contains("[[") && !message.contains("]]") ->
                if (message.endsWith("]")) "$message ]]" else "$message]]"
            else -> message
        }
    }

    /**
     * Convert MU*-style pose prefix (`:`) to an IRC `/me` emote when the
     * `AllowMUpose` setting is enabled.
     */
    fun applyMuPose(message: String): String {
        val allowMuPose = true // TODO: read from settings
        if (!allowMuPose || !message.startsWith(":") || message.length <= 3) return message

        return when {
            message.startsWith(":'") -> "/me" + message.substring(1)
            !message[1].isDigit() && !message[1].isLetterOrDigit().not() &&
                    !message[1].isWhitespace() -> "/me " + message.substring(1)
            else -> message
        }
    }

    // -----------------------------------------------------------------------
    // Date / time
    // -----------------------------------------------------------------------

    /**
     * Parse [str] according to [format] (Boost.DateTime/strftime specifiers)
     * and return the number of whole seconds since the Unix epoch (1970-01-01).
     *
     * @param format A strftime-compatible format string, e.g. `"%A %b %d, %Y"`.
     * @param str    The date/time string to parse.
     */
    fun secondsSinceEpochFromString(format: String, str: String): Int {
        TODO("Requires a date-parsing library (e.g. java.time or Kotlin-datetime)")
    }

    // -----------------------------------------------------------------------
    // Avatar / identity helpers
    // -----------------------------------------------------------------------

    /**
     * Returns `true` when [avId] belongs to a Linden Lab employee account on
     * the Second Life main grid, or to a grid-god account on OpenSim.
     */
    fun isLinden(avId: LLUUID): Boolean {
        TODO("Requires avatar name cache and optional grid-manager integration")
    }

    /**
     * Returns `true` when [assetId] is one of the viewer's built-in default
     * textures.
     */
    fun isDefaultTexture(assetId: LLUUID): Boolean {
        return assetId.toString() in DEFAULT_TEXTURE_IDS
    }

    /**
     * Returns `true` when the active UI skin is the legacy "Vintage" skin.
     */
    fun isLegacySkin(): Boolean {
        TODO("Requires settings lookup for 'FSInternalSkinCurrent'")
    }

    /**
     * Returns `true` when the key combination [key] + [mask] matches the
     * configured filter-editor shortcut (`Ctrl+F` when enabled).
     */
    fun isFilterEditorKeyCombo(key: Char, mask: Int): Boolean {
        TODO("Requires settings lookup for 'FSSelectLocalSearchEditorOnShortcut'")
    }

    // -----------------------------------------------------------------------
    // Group / permission helpers
    // -----------------------------------------------------------------------

    /**
     * Ensure group data for [groupId] is cached; sends a server request if it
     * is not.
     *
     * @return `true` if the data is already available locally.
     */
    fun requestGroupData(groupId: LLUUID): Boolean {
        TODO("Requires LLGroupMgr integration")
    }

    /**
     * Evaluate whether a registrar action [actionType] is currently enabled
     * for avatar [avId].
     */
    fun checkIsActionEnabled(avId: LLUUID, actionType: EFSRegistrarFunctionActionType): Boolean {
        TODO("Requires agent, RLVa, and avatar-action integration")
    }

    /**
     * Build an LLSD-compatible map describing the current group slot usage
     * (count / remaining) for display in the UI.
     */
    fun populateGroupCount(): Map<String, String> {
        TODO("Requires agent group membership and benefit-tier integration")
    }

    /**
     * Determine the preferred display form of [avName] based on the active
     * display-name settings (`UseDisplayNames`, `NameTagShowUsernames`).
     */
    fun getAvatarNameByDisplaySettings(avName: AvatarName): String {
        TODO("Requires settings lookup and AvatarName model")
    }

    /**
     * Returns the LLUUID of the group that should own newly rezzed objects,
     * respecting the `RezUnderLandGroup` preference and the agent's current
     * parcel.
     */
    fun getGroupForRezzing(): LLUUID {
        TODO("Requires agent, parcel manager, and settings integration")
    }

    /**
     * Record emoji characters found in [text] as recently used, and persist
     * the updated usage history.
     */
    fun updateUsedEmojis(text: String) {
        TODO("Requires LLEmojiDictionary and LLFloaterEmojiPicker integration")
    }

    /**
     * Apply default build preferences (texture, colour, alpha, permissions,
     * physics flags, etc.) to a newly created [viewerObject].
     */
    fun applyDefaultBuildPreferences(viewerObject: LLViewerObject) {
        TODO("Requires viewer-object, texture, and message-system integration")
    }
}

// ---------------------------------------------------------------------------
// Supporting types (stubs — expand as other modules are ported)
// ---------------------------------------------------------------------------

/** Placeholder for the avatar-name data class (mirrors LLAvatarName). */
data class AvatarName(
    val userName: String,
    val displayName: String,
    val completeName: String,
)

/** Action types understood by the registrar / context-menu system. */
enum class EFSRegistrarFunctionActionType {
    FS_RGSTR_ACT_ADD_FRIEND,
    FS_RGSTR_ACT_REMOVE_FRIEND,
    FS_RGSTR_ACT_SEND_IM,
    FS_RGSTR_ACT_VIEW_TRANSCRIPT,
    FS_RGSTR_ACT_ZOOM_IN,
    FS_RGSTR_ACT_OFFER_TELEPORT,
    FS_RGSTR_ACT_REQUEST_TELEPORT,
    FS_RGSTR_ACT_SHOW_PROFILE,
    FS_RGSTR_ACT_TRACK_AVATAR,
    FS_RGSTR_ACT_TELEPORT_TO,
    FS_RGSTR_CHK_AVATAR_BLOCKED,
    FS_RGSTR_CHK_IS_SELF,
    FS_RGSTR_CHK_IS_NOT_SELF,
    FS_RGSTR_CHK_WAITING_FOR_GROUP_DATA,
    FS_RGSTR_CHK_HAVE_GROUP_DATA,
    FS_RGSTR_CHK_CAN_LEAVE_GROUP,
    FS_RGSTR_CHK_CAN_JOIN_GROUP,
    FS_RGSTR_CHK_GROUP_NOT_ACTIVE,
}
