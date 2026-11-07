package com.firestorm.newview

import java.net.URLDecoder
import java.util.UUID

// ---------------------------------------------------------------------------
// Constants  (mirrors constexpr values in fscommon.h)
// ---------------------------------------------------------------------------

/** Z-offset sentinel for avatars at unknown height. */
const val AVATAR_UNKNOWN_Z_OFFSET: Double = -1.0

/** Range sentinel for avatars at unknown distance. */
const val AVATAR_UNKNOWN_RANGE: Float = -1f

// Linden-staff last-name set for isLinden() on SL main grid
private val LINDEN_LAST_NAMES = setOf("Linden", "Mole", "ProductEngine", "Scout", "Tester")

// Built-in default texture UUIDs (matches LL_DEFAULT_* and UIImg* constants in C++)
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
// Supporting stub types used across multiple newview files
// ---------------------------------------------------------------------------

/** Mirrors LLAvatarName — expand when the name-cache layer is ported. */
data class AvatarName(
    val userName: String = "",
    val displayName: String = "",
    val completeName: String = ""
)

/** Mirrors LLViewerObject — expand when the viewer-object layer is ported. */
open class LLViewerObject

/** Action types for the FS registrar / context-menu system (mirrors EFSRegistrarFunctionActionType). */
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

// ---------------------------------------------------------------------------
// FSCommon object  (mirrors the FSCommon namespace in C++)
// ---------------------------------------------------------------------------

object FSCommon {

    /**
     * Tracks the number of ObjectAdd messages sent to the simulator.
     *
     * HACK: works around a LL design flaw where _PREHASH_ObjectAdd,
     * _PREHASH_RezObject, and _PREHASH_RezObjectFromNotecard all produce
     * the same object-update packet on the return path.
     */
    var sObjectAddMsg: Int = 0

    // -----------------------------------------------------------------------
    // Chat helpers
    // -----------------------------------------------------------------------

    /** Post a system-sourced [message] into the nearby-chat channel. */
    fun reportToNearbyChat(message: String) {
        System.err.println("FSCommon: reportToNearbyChat not yet implemented")
    }

    /**
     * Substitute [args] tokens in [text] (mirrors LLStringUtil::format).
     * Token syntax is `[KEY]`.
     */
    fun formatString(text: String, args: Map<String, String>): String {
        var result = text
        for ((key, value) in args) result = result.replace("[$key]", value)
        return result
    }

    /**
     * Returns `true` when [text] opens with an IRC-style `/me ` or `/me'` prefix.
     */
    fun isIrcMePrefix(text: String): Boolean {
        if (text.length < 4) return false
        val prefix = text.substring(0, 4)
        return prefix == "/me " || prefix == "/me'"
    }

    /**
     * URL-decode a percent-encoded [name] (mirrors `curl_unescape`).
     */
    fun unescapeName(name: String): String =
        URLDecoder.decode(name, Charsets.UTF_8)

    // -----------------------------------------------------------------------
    // Chat-text transformations
    // -----------------------------------------------------------------------

    /**
     * Append a closing `))` or `]]` when an OOC opener is present without a
     * matching closer, provided the `AutoCloseOOC` setting is enabled.
     */
    fun applyAutoCloseOoc(message: String): String {
        val autoClose = false.also { System.err.println("FSCommon: gSavedSettings.getBOOL(\"AutoCloseOOC\") not yet implemented") }
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
     * Convert a MU*-style `:pose` prefix to `/me` IRC-emote format when
     * `AllowMUpose` is enabled.
     */
    fun applyMuPose(message: String): String {
        val allowMuPose = false.also { System.err.println("FSCommon: gSavedSettings.getBOOL(\"AllowMUpose\") not yet implemented") }
        if (!allowMuPose || !message.startsWith(":") || message.length <= 3) return message

        return when {
            message.startsWith(":'") -> "/me" + message.substring(1)
            !message[1].isDigit() && !message[1].isPunct() && !message[1].isWhitespace() ->
                "/me " + message.substring(1)
            else -> message
        }
    }

    // -----------------------------------------------------------------------
    // Date / time
    // -----------------------------------------------------------------------

    /**
     * Parse [str] with [format] (Boost.DateTime / strftime specifiers) and
     * return whole seconds since the Unix epoch.
     *
     * The C++ implementation uses Boost.Date_Time's `time_input_facet`.
     * Replace with `java.time` or `kotlinx-datetime` when porting.
     */
    fun secondsSinceEpochFromString(format: String, str: String): Int {
        System.err.println("FSCommon: secondsSinceEpochFromString not yet implemented")
        return 0
    }

    // -----------------------------------------------------------------------
    // Build preferences
    // -----------------------------------------------------------------------

    /**
     * Apply the user's default build preferences (texture, colour, alpha, glow,
     * shininess, fullbright, next-owner permissions, physics/temporary/phantom
     * flags) to [viewerObject] immediately after it is rezzed.
     */
    fun applyDefaultBuildPreferences(viewerObject: LLViewerObject) {
        System.err.println("FSCommon: applyDefaultBuildPreferences not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Avatar / identity helpers
    // -----------------------------------------------------------------------

    /**
     * Returns `true` when [avId] belongs to a Linden Lab employee on SL main
     * grid, or to a grid-god account on OpenSim.
     */
    fun isLinden(avId: UUID): Boolean {
        System.err.println("FSCommon: isLinden not yet implemented")
        return false
    }

    /**
     * Returns `true` when [assetId] matches one of the viewer's built-in
     * default textures.
     */
    fun isDefaultTexture(assetId: UUID): Boolean =
        assetId.toString() in DEFAULT_TEXTURE_IDS

    /**
     * Returns `true` when the active UI skin is the legacy "Vintage" skin.
     */
    fun isLegacySkin(): Boolean {
        System.err.println("FSCommon: isLegacySkin not yet implemented")
        return false
    }

    /**
     * Returns `true` when [key] + [mask] matches the Ctrl+F filter-editor
     * shortcut (only when `FSSelectLocalSearchEditorOnShortcut` is enabled).
     */
    fun isFilterEditorKeyCombo(key: Int, mask: Int): Boolean {
        val enabled = false.also { System.err.println("FSCommon: gSavedSettings.getBOOL(\"FSSelectLocalSearchEditorOnShortcut\") not yet implemented") }
        return mask == MASK_CONTROL && key == 'F'.code && enabled
    }

    // -----------------------------------------------------------------------
    // Group / permission helpers
    // -----------------------------------------------------------------------

    /**
     * Ensure group data for [groupId] is in the local cache; sends a server
     * request if it is not.
     *
     * @return `true` if data is already available locally.
     */
    fun requestGroupData(groupId: UUID): Boolean {
        System.err.println("FSCommon: requestGroupData not yet implemented")
        return false
    }

    /**
     * Evaluate whether registrar action [actionType] is currently enabled
     * for avatar [avId].
     */
    fun checkIsActionEnabled(avId: UUID, actionType: EFSRegistrarFunctionActionType): Boolean {
        System.err.println("FSCommon: checkIsActionEnabled not yet implemented")
        return false
    }

    /**
     * Build a localised string describing the agent's current group-slot
     * usage (count / remaining).
     */
    fun populateGroupCount(): String {
        System.err.println("FSCommon: populateGroupCount not yet implemented")
        return ""
    }

    /**
     * Return the preferred display form of [avName] based on the active
     * `UseDisplayNames` / `NameTagShowUsernames` settings.
     */
    fun getAvatarNameByDisplaySettings(avName: AvatarName): String {
        val showUsernames = false.also { System.err.println("FSCommon: gSavedSettings.getBOOL(\"NameTagShowUsernames\") not yet implemented") }
        val useDisplayNames = false.also { System.err.println("FSCommon: gSavedSettings.getBOOL(\"UseDisplayNames\") not yet implemented") }
        return when {
            showUsernames && useDisplayNames -> avName.completeName
            useDisplayNames -> avName.displayName
            else -> avName.userName
        }
    }

    /**
     * Return the UUID of the group that should own newly rezzed objects,
     * respecting the `RezUnderLandGroup` preference and the agent's parcel.
     */
    fun getGroupForRezzing(): UUID {
        val groupId = UUID(0L, 0L).also { System.err.println("FSCommon: gAgent.getGroupID() not yet implemented") }
        val rezUnderLandGroup = false.also { System.err.println("FSCommon: gSavedSettings.getBOOL(\"RezUnderLandGroup\") not yet implemented") }
        if (rezUnderLandGroup) {
            val parcelGroupId = null.also { System.err.println("FSCommon: LLViewerParcelMgr.getInstance().getAgentParcel()?.getGroupID() not yet implemented") } as UUID?
            val agentInGroup = false.also { System.err.println("FSCommon: gAgent.isInGroup(parcelGroupId) not yet implemented") }
            if (parcelGroupId != null && parcelGroupId != UUID(0L, 0L) && agentInGroup) {
                return parcelGroupId
            }
        }
        return groupId
    }

    /**
     * Record any emoji characters in [text] as recently used and persist the
     * updated state.
     */
    fun updateUsedEmojis(text: String) {
        System.err.println("FSCommon: updateUsedEmojis not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Private constants
    // -----------------------------------------------------------------------

    private const val MASK_CONTROL = 0x0001
}

// ---------------------------------------------------------------------------
// Extension to check if a Char is punctuation (used in applyMuPose)
// ---------------------------------------------------------------------------

private fun Char.isPunct(): Boolean = !isLetterOrDigit() && !isWhitespace()
