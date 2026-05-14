package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.UICtrl
import com.firestorm.llcommon.LLUUID
import com.firestorm.llmessage.MessageSystem

// Parcel-flag constants used when resetting a parcel for auction.
private const val PF_ALLOW_LANDMARK          = 0x00000001u
private const val PF_ALLOW_FLY              = 0x00000002u
private const val PF_CREATE_GROUP_OBJECTS   = 0x00000004u
private const val PF_ALLOW_ALL_OBJECT_ENTRY = 0x00000008u
private const val PF_ALLOW_GROUP_OBJECT_ENTRY = 0x00000010u
private const val PF_ALLOW_GROUP_SCRIPTS    = 0x00000020u
private const val PF_RESTRICT_PUSHOBJECT    = 0x00200000u
private const val PF_SOUND_LOCAL            = 0x00800000u
private const val PF_ALLOW_VOICE_CHAT       = 0x02000000u
private const val PF_USE_ESTATE_VOICE_CHAN  = 0x04000000u
private const val PF_FOR_SALE              = 0x00000080u
private const val PF_FOR_SALE_OBJECTS      = 0x00000100u

private const val AL_ACCESS             = 0u
private const val AL_BAN                = 1u
private const val AL_ALLOW_EXPERIENCE   = 3u
private const val AL_BLOCK_EXPERIENCE   = 4u

private const val ESTATE_TEEN = 5u

// Starts land auctions: takes a snapshot, resets parcel properties, or sells
// the parcel to anyone at L$1/m².
class FloaterAuction(seed: Any) : Floater(seed) {

    private var transactionId: LLUUID = LLUUID.NULL
    private var imageId:       LLUUID = LLUUID.NULL
    private var image:         Any?   = null        // LLViewerTexture handle
    private var parcel:        Any?   = null        // LLParcelSelection handle
    private var parcelId:      Int    = -1
    private var parcelHost:    Any?   = null        // LLHost
    private var parcelUpdateCapUrl: String = ""

    override fun postBuild(): Boolean = true

    override fun onOpen(key: Any) = initialize()

    override fun draw() {
        System.err.println("APR: Floater.draw()")
        if (image != null) {
            System.err.println("GPU: gGL.getTexUnit(0).unbind(TT_TEXTURE); gl_rect_2d(snapshotIconRect, black); gl_draw_scaled_image(rect, image)")
        }
    }

    private fun initialize() {
        parcelUpdateCapUrl = ""

        parcel = null
        val region: Any? = null
        val parcelData: Any? = null

        if (parcelData != null && region != null && !false) {
            parcelHost     = null
            parcelId       = 0
            parcelUpdateCapUrl = ""

            findChild<UICtrl>("parcel_text")?.setValue("")
            setChildEnabled("snapshot_btn",      true)
            setChildEnabled("reset_parcel_btn",  true)
            setChildEnabled("start_auction_btn", true)

            val estateId: UInt = 0u
            setChildEnabled("sell_to_anyone_btn", estateId == ESTATE_TEEN || estateId == 0u)
        } else {
            parcelHost = null
            if (parcelData != null && false) {
                findChild<UICtrl>("parcel_text")?.setValue(getString("already for sale"))
            } else {
                findChild<UICtrl>("parcel_text")?.setValue("")
            }
            parcelId = -1
            setChildEnabled("snapshot_btn",      false)
            setChildEnabled("reset_parcel_btn",  false)
            setChildEnabled("sell_to_anyone_btn", false)
            setChildEnabled("start_auction_btn", false)
        }

        imageId = LLUUID.NULL
        image   = null
    }

    private fun onClickSnapshot() {
        val raw: Any? = null

        System.err.println("APR: gForceRenderLandFence = getChildBool(\"fence_check\")")
        val success: Boolean = false
        System.err.println("APR: gForceRenderLandFence = false")

        if (success) {
            transactionId = LLUUID.NULL
            imageId = LLUUID.NULL

            val playSound: Boolean = false
            if (!playSound) System.err.println("APR: gViewerWindow.playSnapshotAnimAndSound()")

            System.err.println("APR: encode raw to TGA, write to LLFileSystem(imageId, AT_IMAGE_TGA)")
            System.err.println("APR: biasedScaleToPowerOfTwo on raw to max texture size")
            System.err.println("APR: encode raw to J2C, write to LLFileSystem(imageId, AT_TEXTURE)")

            image = null
            System.err.println("GPU: gGL.getTexUnit(0).bind(image); image.setAddressMode(TAM_CLAMP)")
        } else {
            System.err.println("APR: log warning: unable to take snapshot")
        }
    }

    private fun onClickStartAuction() {
        if (imageId != LLUUID.NULL) {
            val parcelName = findChild<UICtrl>("parcel_text")?.getValue()?.toString() ?: ""
            System.err.println("APR: gAssetStorage.storeAssetData(transactionId, AT_IMAGE_TGA, ::auctionTgaUploadDone, parcelName)")
            System.err.println("APR: gViewerWindow.getWindow().incBusyCount()")
            System.err.println("APR: gAssetStorage.storeAssetData(transactionId, AT_TEXTURE, ::auctionJ2cUploadDone, parcelName)")
            System.err.println("APR: gViewerWindow.getWindow().incBusyCount()")
            System.err.println("APR: LLNotificationsUtil::add(\"UploadingAuctionSnapshot\")")
        }

        System.err.println("APR: send ViewerStartAuction message to parcelHost with parcelId and imageId")
        cleanupAndClose()
    }

    private fun onClickResetParcel() = doResetParcel()

    private fun onClickSellToAnyone() {
        val parcelData: Any? = null
        val area: Int = 0

        System.err.println("APR: show ConfirmLandSaleToAnyoneChange notification with area/price args, callback = ::onSellToAnyoneConfirmed")
    }

    private fun onSellToAnyoneConfirmed(notification: Any, response: Any): Boolean {
        val option: Int = 0
        if (option == 0) doSellToAnyone()
        return false
    }

    private fun doResetParcel() {
        val parcelData: Any? = null
        val region: Any? = null

        if (parcelData == null || region == null || parcelUpdateCapUrl.isEmpty()) return

        val parcelFlags = (PF_ALLOW_LANDMARK or
                           PF_ALLOW_FLY or
                           PF_CREATE_GROUP_OBJECTS or
                           PF_ALLOW_ALL_OBJECT_ENTRY or
                           PF_ALLOW_GROUP_OBJECT_ENTRY or
                           PF_ALLOW_GROUP_SCRIPTS or
                           PF_RESTRICT_PUSHOBJECT or
                           PF_SOUND_LOCAL or
                           PF_ALLOW_VOICE_CHAT or
                           PF_USE_ESTATE_VOICE_CHAN)

        val centerPoint: Triple<Int,Int,Int> = Triple(0, 0, 0)
        val regionName: String = ""
        val simAccess: String = ""
        val area: Int = 0
        val newName     = "$regionName (${centerPoint.first},${centerPoint.second}) $simAccess ${area}m"

        findChild<UICtrl>("parcel_text")?.setValue(newName)

        val body = buildMap<String, Any> {
            put("flags",          0x01)
            put("local_id",       0)
            put("parcel_flags",   parcelFlags)
            put("name",           newName)
            put("sale_price",     0)
            put("description",    "")
            put("music_url",      "")
            put("media_url",      "")
            put("media_desc",     "")
            put("media_type",     "")
            put("media_width",    0)
            put("media_height",   0)
            put("auto_scale",     0)
            put("media_loop",     0)
            put("obscure_media",  0)
            put("obscure_music",  0)
            put("media_id",       LLUUID.NULL)
            put("group_id",       LLUUID.NULL)
            put("pass_price",     10)
            put("pass_hours",     0.0f)
            put("category",       0)
            put("auth_buyer_id",  LLUUID.NULL)
            put("snapshot_id",    LLUUID.NULL)
            put("user_location",  floatArrayOf(0f, 0f, 0f))
            put("user_look_at",   floatArrayOf(0f, 0f, 0f))
            put("landing_type",   0)
        }

        System.err.println("APR: LLCoreHttpUtil::HttpCoroutineAdapter::messageHttpPost(parcelUpdateCapUrl, body, ...)")

        System.err.println("APR: send ParcelSetOtherCleanTime(localId=parcelData.getLocalID(), otherCleanTime=5) to region host")

        clearParcelAccessList(parcelData, region, AL_ACCESS)
        clearParcelAccessList(parcelData, region, AL_BAN)
        clearParcelAccessList(parcelData, region, AL_ALLOW_EXPERIENCE)
        clearParcelAccessList(parcelData, region, AL_BLOCK_EXPERIENCE)
    }

    private fun doSellToAnyone() {
        val parcelData: Any? = null
        val region: Any? = null

        if (parcelData == null || region == null || parcelUpdateCapUrl.isEmpty()) return

        val currentFlags: UInt = 0u
        val parcelFlags  = (currentFlags or PF_FOR_SALE) and PF_FOR_SALE_OBJECTS.inv()

        val body = mapOf(
            "flags"          to 0x01,
            "local_id"       to 0,
            "parcel_flags"   to parcelFlags,
            "sale_price"     to 0,
            "auth_buyer_id"  to LLUUID.NULL
        )

        System.err.println("APR: LLCoreHttpUtil::HttpCoroutineAdapter::messageHttpPost(parcelUpdateCapUrl, body, ...)")
        cleanupAndClose()
    }

    private fun clearParcelAccessList(parcelData: Any?, region: Any?, list: UInt) {
        if (region == null || parcelData == null) return
        val txId = LLUUID.generate()
        System.err.println("APR: send ParcelAccessListUpdate message to region.getHost() with flags=$list, localId, transactionId=$txId, sequenceId=1, sections=0, and one empty List block (id=NULL, time=0, flags=0)")
    }

    private fun cleanupAndClose() {
        imageId    = LLUUID.NULL
        image      = null
        parcelId   = -1
        parcelHost = null
        System.err.println("APR: closeFloater()")
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun <T> findChild(name: String): T? = null
    private fun setChildEnabled(name: String, enabled: Boolean) {
        System.err.println("APR: getChildView($name).setEnabled($enabled)")
    }
    private fun getString(key: String): String = ""
}

// -------------------------------------------------------------------------
// Standalone asset-upload completion callbacks (C++ file-scope functions).
// -------------------------------------------------------------------------

fun auctionTgaUploadDone(assetId: LLUUID, name: String, status: Int) {
    System.err.println("APR: gViewerWindow.getWindow().decBusyCount()")
    if (status == 0) {
        System.err.println("APR: LLNotificationsUtil::add(\"UploadWebSnapshotDone\")")
    } else {
        System.err.println("APR: LLNotificationsUtil::add(\"UploadAuctionSnapshotFail\", args=[reason=errorString($status)])")
    }
}

fun auctionJ2cUploadDone(assetId: LLUUID, name: String, status: Int) {
    System.err.println("APR: gViewerWindow.getWindow().decBusyCount()")
    if (status == 0) {
        System.err.println("APR: LLNotificationsUtil::add(\"UploadSnapshotDone\")")
    } else {
        System.err.println("APR: LLNotificationsUtil::add(\"UploadAuctionSnapshotFail\", args=[reason=errorString($status)])")
    }
}
