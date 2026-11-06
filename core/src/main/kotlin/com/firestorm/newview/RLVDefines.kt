package com.firestorm.newview

import java.util.UUID

// ============================================================================
// Version constants
// ============================================================================

const val RLV_VERSION_MAJOR = 3
const val RLV_VERSION_MINOR = 4
const val RLV_VERSION_PATCH = 3
const val RLV_VERSION_BUILD = 0

const val RLV_VERSION_MAJOR_COMPAT = 2
const val RLV_VERSION_MINOR_COMPAT = 9
const val RLV_VERSION_PATCH_COMPAT = 28
const val RLV_VERSION_BUILD_COMPAT = 0

const val RLVa_VERSION_MAJOR = 2
const val RLVa_VERSION_MINOR = 4
const val RLVa_VERSION_PATCH = 2
const val RLVa_IMPL_ID = 13

// ============================================================================
// String/character constants
// ============================================================================

const val RLV_ROOT_FOLDER = "#RLV"
const val RLV_CMD_PREFIX = '@'
const val RLV_MODIFIER_ANIMATION_FREQUENCY = 10
const val RLV_MODIFIER_TPLOCAL_DEFAULT = 256f
const val RLV_MODIFIER_FARTOUCH_DEFAULT = 1.5f
const val RLV_MODIFIER_SITTP_DEFAULT = 1.5f
const val RLV_OPTION_SEPARATOR = ";"
const val RLV_PUTINV_PREFIX = "#RLV/~"
const val RLV_PUTINV_SEPARATOR = "/"
const val RLV_PUTINV_MAXDEPTH = 4
const val RLV_SETGROUP_THROTTLE = 60f
const val RLV_SETROT_OFFSET = (Math.PI / 2).toFloat()
const val RLV_STRINGS_FILE = "rlva_strings.xml"

const val RLV_FOLDER_FLAG_NOSTRIP = "nostrip"
const val RLV_FOLDER_PREFIX_HIDDEN = '.'
const val RLV_FOLDER_PREFIX_PUTINV = '~'
const val RLV_FOLDER_INVALID_CHARS = "/"

// ============================================================================
// ERlvBehaviour
// ============================================================================

enum class ERlvBehaviour {
    RLV_BHVR_DETACH,
    RLV_BHVR_ADDATTACH,
    RLV_BHVR_REMATTACH,
    RLV_BHVR_ADDOUTFIT,
    RLV_BHVR_REMOUTFIT,
    RLV_BHVR_SHAREDWEAR,
    RLV_BHVR_SHAREDUNWEAR,
    RLV_BHVR_UNSHAREDWEAR,
    RLV_BHVR_UNSHAREDUNWEAR,
    RLV_BHVR_EMOTE,
    RLV_BHVR_SENDCHAT,
    RLV_BHVR_RECVCHAT,
    RLV_BHVR_RECVCHATFROM,
    RLV_BHVR_RECVEMOTE,
    RLV_BHVR_RECVEMOTEFROM,
    RLV_BHVR_REDIRCHAT,
    RLV_BHVR_REDIREMOTE,
    RLV_BHVR_CHATWHISPER,
    RLV_BHVR_CHATNORMAL,
    RLV_BHVR_CHATSHOUT,
    RLV_BHVR_SENDCHANNEL,
    RLV_BHVR_SENDCHANNELEXCEPT,
    RLV_BHVR_SENDIM,
    RLV_BHVR_SENDIMTO,
    RLV_BHVR_RECVIM,
    RLV_BHVR_RECVIMFROM,
    RLV_BHVR_STARTIM,
    RLV_BHVR_STARTIMTO,
    RLV_BHVR_SENDGESTURE,
    RLV_BHVR_PERMISSIVE,
    RLV_BHVR_NOTIFY,
    RLV_BHVR_SHARE,
    RLV_BHVR_SHOWINV,
    RLV_BHVR_SHOWMINIMAP,
    RLV_BHVR_SHOWWORLDMAP,
    RLV_BHVR_SHOWLOC,
    RLV_BHVR_SHOWNAMES,
    RLV_BHVR_SHOWNAMETAGS,
    RLV_BHVR_SHOWNEARBY,
    RLV_BHVR_SHOWHOVERTEXT,
    RLV_BHVR_SHOWHOVERTEXTHUD,
    RLV_BHVR_SHOWHOVERTEXTWORLD,
    RLV_BHVR_SHOWHOVERTEXTALL,
    RLV_BHVR_SHOWSELF,
    RLV_BHVR_SHOWSELFHEAD,
    RLV_BHVR_TPLM,
    RLV_BHVR_TPLOC,
    RLV_BHVR_TPLOCAL,
    RLV_BHVR_TPLURE,
    RLV_BHVR_TPREQUEST,
    RLV_BHVR_VIEWNOTE,
    RLV_BHVR_VIEWSCRIPT,
    RLV_BHVR_VIEWTEXTURE,
    RLV_BHVR_ACCEPTPERMISSION,
    RLV_BHVR_ACCEPTTP,
    RLV_BHVR_ACCEPTTPREQUEST,
    RLV_BHVR_ALLOWIDLE,
    RLV_BHVR_BUY,
    RLV_BHVR_EDIT,
    RLV_BHVR_EDITATTACH,
    RLV_BHVR_EDITOBJ,
    RLV_BHVR_EDITWORLD,
    RLV_BHVR_VIEWTRANSPARENT,
    RLV_BHVR_VIEWWIREFRAME,
    RLV_BHVR_PAY,
    RLV_BHVR_REZ,
    RLV_BHVR_FARTOUCH,
    RLV_BHVR_INTERACT,
    RLV_BHVR_TOUCHTHIS,
    RLV_BHVR_TOUCHATTACH,
    RLV_BHVR_TOUCHATTACHSELF,
    RLV_BHVR_TOUCHATTACHOTHER,
    RLV_BHVR_TOUCHHUD,
    RLV_BHVR_TOUCHWORLD,
    RLV_BHVR_TOUCHALL,
    RLV_BHVR_TOUCHME,
    RLV_BHVR_FLY,
    RLV_BHVR_JUMP,
    RLV_BHVR_SETGROUP,
    RLV_BHVR_UNSIT,
    RLV_BHVR_SIT,
    RLV_BHVR_SITGROUND,
    RLV_BHVR_SITTP,
    RLV_BHVR_STANDTP,
    RLV_BHVR_SETDEBUG,
    RLV_BHVR_SETENV,
    RLV_BHVR_ALWAYSRUN,
    RLV_BHVR_TEMPRUN,
    RLV_BHVR_DETACHME,
    RLV_BHVR_ATTACHTHIS,
    RLV_BHVR_ATTACHTHISEXCEPT,
    RLV_BHVR_DETACHTHIS,
    RLV_BHVR_DETACHTHISEXCEPT,
    RLV_BHVR_ADJUSTHEIGHT,
    RLV_BHVR_GETHEIGHTOFFSET,
    RLV_BHVR_TPTO,
    RLV_BHVR_VERSION,
    RLV_BHVR_VERSIONNEW,
    RLV_BHVR_VERSIONNUM,
    RLV_BHVR_GETATTACH,
    RLV_BHVR_GETATTACHNAMES,
    RLV_BHVR_GETADDATTACHNAMES,
    RLV_BHVR_GETREMATTACHNAMES,
    RLV_BHVR_GETOUTFIT,
    RLV_BHVR_GETOUTFITNAMES,
    RLV_BHVR_GETADDOUTFITNAMES,
    RLV_BHVR_GETREMOUTFITNAMES,
    RLV_BHVR_FINDFOLDER,
    RLV_BHVR_FINDFOLDERS,
    RLV_BHVR_GETPATH,
    RLV_BHVR_GETPATHNEW,
    RLV_BHVR_GETINV,
    RLV_BHVR_GETINVWORN,
    RLV_BHVR_GETGROUP,
    RLV_BHVR_GETSITID,
    RLV_BHVR_GETCOMMAND,
    RLV_BHVR_GETSTATUS,
    RLV_BHVR_GETSTATUSALL,
    RLV_CMD_FORCEWEAR,

    // Camera (behaviours)
    RLV_BHVR_SETCAM,
    RLV_BHVR_SETCAM_AVDIST,
    RLV_BHVR_SETCAM_AVDISTMIN,
    RLV_BHVR_SETCAM_AVDISTMAX,
    RLV_BHVR_SETCAM_ORIGINDISTMIN,
    RLV_BHVR_SETCAM_ORIGINDISTMAX,
    RLV_BHVR_SETCAM_EYEOFFSET,
    RLV_BHVR_SETCAM_EYEOFFSETSCALE,
    RLV_BHVR_SETCAM_FOCUSOFFSET,
    RLV_BHVR_SETCAM_FOCUS,
    RLV_BHVR_SETCAM_FOV,
    RLV_BHVR_SETCAM_FOVMIN,
    RLV_BHVR_SETCAM_FOVMAX,
    RLV_BHVR_SETCAM_MOUSELOOK,
    RLV_BHVR_SETCAM_TEXTURES,
    RLV_BHVR_SETCAM_UNLOCK,
    // Camera (behaviours - deprecated)
    RLV_BHVR_CAMZOOMMIN,
    RLV_BHVR_CAMZOOMMAX,
    // Camera (reply)
    RLV_BHVR_GETCAM_AVDIST,
    RLV_BHVR_GETCAM_AVDISTMIN,
    RLV_BHVR_GETCAM_AVDISTMAX,
    RLV_BHVR_GETCAM_FOV,
    RLV_BHVR_GETCAM_FOVMIN,
    RLV_BHVR_GETCAM_FOVMAX,
    RLV_BHVR_GETCAM_TEXTURES,
    // Camera (force)
    RLV_BHVR_SETCAM_MODE,

    // Effects
    RLV_BHVR_SETSPHERE,
    RLV_BHVR_SETOVERLAY,
    RLV_BHVR_SETOVERLAY_TOUCH,
    RLV_BHVR_SETOVERLAY_TWEEN,

    RLV_BHVR_COUNT,
    RLV_BHVR_UNKNOWN;

    companion object {
        fun succeeded(ret: ERlvCmdRet): Boolean =
            (ret.value and ERlvCmdRet.RLV_RET_SUCCESS.value) == ERlvCmdRet.RLV_RET_SUCCESS.value
    }
}

// ============================================================================
// ERlvBehaviourModifier
// ============================================================================

enum class ERlvBehaviourModifier {
    RLV_MODIFIER_FARTOUCHDIST,
    RLV_MODIFIER_RECVIMDISTMIN,
    RLV_MODIFIER_RECVIMDISTMAX,
    RLV_MODIFIER_SENDIMDISTMIN,
    RLV_MODIFIER_SENDIMDISTMAX,
    RLV_MODIFIER_STARTIMDISTMIN,
    RLV_MODIFIER_STARTIMDISTMAX,
    RLV_MODIFIER_SETCAM_AVDIST,
    RLV_MODIFIER_SETCAM_AVDISTMIN,
    RLV_MODIFIER_SETCAM_AVDISTMAX,
    RLV_MODIFIER_SETCAM_ORIGINDISTMIN,
    RLV_MODIFIER_SETCAM_ORIGINDISTMAX,
    RLV_MODIFIER_SETCAM_EYEOFFSET,
    RLV_MODIFIER_SETCAM_EYEOFFSETSCALE,
    RLV_MODIFIER_SETCAM_FOCUSOFFSET,
    RLV_MODIFIER_SETCAM_FOVMIN,
    RLV_MODIFIER_SETCAM_FOVMAX,
    RLV_MODIFIER_SETCAM_TEXTURE,
    RLV_MODIFIER_SHOWNAMETAGSDIST,
    RLV_MODIFIER_SITTPDIST,
    RLV_MODIFIER_TPLOCALDIST,

    RLV_MODIFIER_COUNT,
    RLV_MODIFIER_UNKNOWN
}

enum class ERlvLocalBhvrModifier {
    OverlayAlpha,
    OverlayTexture,
    OverlayTint,
    SphereMode,
    SphereOrigin,
    SphereColor,
    SphereParams,
    SphereDistMin,
    SphereDistMax,
    SphereDistExtend,
    SphereValueMin,
    SphereValueMax,
    SphereTween,
    Unknown
}

enum class ERlvBehaviourOptionType {
    RLV_OPTION_NONE,
    RLV_OPTION_EXCEPTION,
    RLV_OPTION_NONE_OR_EXCEPTION,
    RLV_OPTION_MODIFIER,
    RLV_OPTION_NONE_OR_MODIFIER
}

enum class ERlvParamType(val value: Int) {
    RLV_TYPE_UNKNOWN(0x00),
    RLV_TYPE_ADD(0x01),
    RLV_TYPE_REMOVE(0x02),
    RLV_TYPE_FORCE(0x04),
    RLV_TYPE_REPLY(0x08),
    RLV_TYPE_CLEAR(0x10);

    companion object {
        val RLV_TYPE_ADDREM = RLV_TYPE_ADD.value or RLV_TYPE_REMOVE.value
    }
}

enum class ERlvCmdRet(val value: Int) {
    RLV_RET_UNKNOWN(0x0000),
    RLV_RET_RETAINED(0x0001),
    RLV_RET_SUCCESS(0x0100),
    RLV_RET_SUCCESS_UNSET(0x0101),
    RLV_RET_SUCCESS_DUPLICATE(0x0102),
    RLV_RET_SUCCESS_DEPRECATED(0x0103),
    RLV_RET_SUCCESS_DELAYED(0x0104),
    RLV_RET_FAILED(0x0200),
    RLV_RET_FAILED_SYNTAX(0x0201),
    RLV_RET_FAILED_OPTION(0x0202),
    RLV_RET_FAILED_PARAM(0x0203),
    RLV_RET_FAILED_LOCK(0x0204),
    RLV_RET_FAILED_DISABLED(0x0205),
    RLV_RET_FAILED_UNKNOWN(0x0206),
    RLV_RET_FAILED_NOSHAREDROOT(0x0207),
    RLV_RET_FAILED_DEPRECATED(0x0208),
    RLV_RET_FAILED_NOBEHAVIOUR(0x0209),
    RLV_RET_FAILED_UNHELDBEHAVIOUR(0x020A),
    RLV_RET_FAILED_BLOCKED(0x020B),
    RLV_RET_FAILED_THROTTLED(0x020C),
    RLV_RET_NO_PROCESSOR(0x0300)
}

enum class ERlvExceptionCheck {
    Permissive,
    Strict,
    Default
}

enum class ERlvLockMask(val value: Int) {
    RLV_LOCK_NONE(0x00),
    RLV_LOCK_ADD(0x01),
    RLV_LOCK_REMOVE(0x02);

    companion object {
        val RLV_LOCK_ANY = RLV_LOCK_ADD.value or RLV_LOCK_REMOVE.value
    }
}

enum class ERlvWearMask(val value: Int) {
    RLV_WEAR_LOCKED(0x00),
    RLV_WEAR_ADD(0x01),
    RLV_WEAR_REPLACE(0x02);

    companion object {
        val RLV_WEAR = RLV_WEAR_ADD.value or RLV_WEAR_REPLACE.value
    }
}

enum class ERlvAttachGroupType {
    RLV_ATTACHGROUP_HEAD,
    RLV_ATTACHGROUP_TORSO,
    RLV_ATTACHGROUP_ARMS,
    RLV_ATTACHGROUP_LEGS,
    RLV_ATTACHGROUP_HUD,
    RLV_ATTACHGROUP_COUNT,
    RLV_ATTACHGROUP_INVALID
}

// ============================================================================
// Settings name constants
// ============================================================================

object RlvSettingNames {
    const val Main = "RestrainedLove"
    const val Debug = "RestrainedLoveDebug"
    const val CanOoc = "RestrainedLoveCanOOC"
    const val ForbidGiveToRlv = "RestrainedLoveForbidGiveToRLV"
    const val NoSetEnv = "RestrainedLoveNoSetEnv"
    const val ShowEllipsis = "RestrainedLoveShowEllipsis"
    const val WearAddPrefix = "RestrainedLoveStackWhenFolderBeginsWith"
    const val WearReplacePrefix = "RestrainedLoveReplaceWhenFolderBeginsWith"

    const val DebugHideUnsetDup = "RLVaDebugHideUnsetDuplicate"
    const val EnableIMQuery = "RLVaEnableIMQuery"
    const val EnableLegacyNaming = "RLVaEnableLegacyNaming"
    const val EnableSharedWear = "RLVaEnableSharedWear"
    const val EnableTempAttach = "RLVaEnableTemporaryAttachments"
    const val HideLockedLayer = "RLVaHideLockedLayers"
    const val HideLockedAttach = "RLVaHideLockedAttachments"
    const val HideLockedInventory = "RLVaHideLockedInventory"
    const val LoginLastLocation = "RLVaLoginLastLocation"
    const val SharedInvAutoRename = "RLVaSharedInvAutoRename"
    const val ShowAssertionFail = "RLVaShowAssertionFailures"
    const val ShowRedirectChatTyping = "RLVaShowRedirectChatTyping"
    const val SplitRedirectChat = "RLVaSplitRedirectChat"
    const val TopLevelMenu = "RLVaTopLevelMenu"
    const val WearReplaceUnlocked = "RLVaWearReplaceUnlocked"
}

// ============================================================================
// String key constants
// ============================================================================

object RlvStringKeys {
    object Blocked {
        const val AutoPilot = "blocked_autopilot"
        const val Generic = "blocked_generic"
        const val GroupChange = "blocked_groupchange"
        const val InvFolder = "blocked_invfolder"
        const val PermissionAttach = "blocked_permattach"
        const val PermissionTeleport = "blocked_permteleport"
        const val RecvIm = "blocked_recvim"
        const val RecvImRemote = "blocked_recvim_remote"
        const val SendIm = "blocked_sendim"
        const val Share = "blocked_share"
        const val ShareGeneric = "blocked_share_generic"
        const val StartConference = "blocked_startconf"
        const val StartIm = "blocked_startim"
        const val Teleport = "blocked_teleport"
        const val TeleportOffer = "blocked_teleport_offer"
        const val TpLureRequestRemote = "blocked_tplurerequest_remote"
        const val ViewXxx = "blocked_viewxxx"
        const val Wireframe = "blocked_wireframe"
    }

    object Hidden {
        const val Generic = "hidden_generic"
        const val Parcel = "hidden_parcel"
        const val Region = "hidden_region"
    }

    object StopIm {
        const val NoSession = "stopim_nosession"
        const val EndSessionRemote = "stopim_endsession_remote"
        const val EndSessionLocal = "stopim_endsession_local"
    }
}
