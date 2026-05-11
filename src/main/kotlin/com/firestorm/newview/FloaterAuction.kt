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
        TODO("APR: Floater.draw()")
        if (image != null) {
            TODO("GPU: gGL.getTexUnit(0).unbind(TT_TEXTURE); " +
                 "gl_rect_2d(snapshotIconRect, black); " +
                 "gl_draw_scaled_image(rect, image)")
        }
    }

    private fun initialize() {
        parcelUpdateCapUrl = ""

        parcel = TODO("APR: LLViewerParcelMgr::getInstance().getParcelSelection()")
        val region = TODO<Any?>("APR: LLViewerParcelMgr::getInstance().getSelectionRegion()")
        val parcelData = TODO<Any?>("APR: parcel.getParcel()")

        if (parcelData != null && region != null && !TODO<Boolean>("APR: parcelData.getForSale()")) {
            parcelHost     = TODO("APR: region.getHost()")
            parcelId       = TODO("APR: parcelData.getLocalID()")
            parcelUpdateCapUrl = TODO("APR: region.getCapability(\"ParcelPropertiesUpdate\")")

            findChild<UICtrl>("parcel_text")?.setValue(TODO("APR: parcelData.getName()"))
            setChildEnabled("snapshot_btn",      true)
            setChildEnabled("reset_parcel_btn",  true)
            setChildEnabled("start_auction_btn", true)

            val estateId = TODO<UInt>("APR: LLEstateInfoModel::instance().getID()")
            setChildEnabled("sell_to_anyone_btn", estateId == ESTATE_TEEN || estateId == 0u)
        } else {
            parcelHost = TODO("APR: LLHost().invalidate()")
            if (parcelData != null && TODO<Boolean>("APR: parcelData.getForSale()")) {
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
        val raw = TODO<Any?>("GPU: allocate LLImageRaw")

        TODO("APR: gForceRenderLandFence = getChildBool(\"fence_check\")")
        val success = TODO<Boolean>("GPU: gViewerWindow.rawSnapshot(raw, windowWidth, windowHeight, ...)")
        TODO("APR: gForceRenderLandFence = false")

        if (success) {
            transactionId = TODO("APR: generate new LLTransactionID")
            imageId = TODO("APR: transactionId.makeAssetID(gAgent.getSecureSessionID())")

            val playSound = TODO<Boolean>("APR: gSavedSettings.getBOOL(\"PlayModeUISndSnapshot\")")
            if (!playSound) TODO("APR: gViewerWindow.playSnapshotAnimAndSound()")

            TODO("APR: encode raw to TGA, write to LLFileSystem(imageId, AT_IMAGE_TGA)")
            TODO("APR: biasedScaleToPowerOfTwo on raw to max texture size")
            TODO("APR: encode raw to J2C, write to LLFileSystem(imageId, AT_TEXTURE)")

            image = TODO("GPU: LLViewerTextureManager::getLocalTexture(raw, false)")
            TODO("GPU: gGL.getTexUnit(0).bind(image); image.setAddressMode(TAM_CLAMP)")
        } else {
            TODO("APR: log warning: unable to take snapshot")
        }
    }

    private fun onClickStartAuction() {
        if (imageId != LLUUID.NULL) {
            val parcelName = findChild<UICtrl>("parcel_text")?.getValue()?.toString() ?: ""
            TODO("APR: gAssetStorage.storeAssetData(transactionId, AT_IMAGE_TGA, ::auctionTgaUploadDone, parcelName)")
            TODO("APR: gViewerWindow.getWindow().incBusyCount()")
            TODO("APR: gAssetStorage.storeAssetData(transactionId, AT_TEXTURE, ::auctionJ2cUploadDone, parcelName)")
            TODO("APR: gViewerWindow.getWindow().incBusyCount()")
            TODO("APR: LLNotificationsUtil::add(\"UploadingAuctionSnapshot\")")
        }

        TODO("APR: send ViewerStartAuction message to parcelHost with parcelId and imageId")
        cleanupAndClose()
    }

    private fun onClickResetParcel() = doResetParcel()

    private fun onClickSellToAnyone() {
        val parcelData = TODO<Any?>("APR: parcel.getParcel()")
        val area       = TODO<Int>("APR: parcelData.getArea()")

        TODO("APR: show ConfirmLandSaleToAnyoneChange notification with area/price args, " +
             "callback = ::onSellToAnyoneConfirmed")
    }

    private fun onSellToAnyoneConfirmed(notification: Any, response: Any): Boolean {
        val option = TODO<Int>("APR: LLNotificationsUtil::getSelectedOption(notification, response)")
        if (option == 0) doSellToAnyone()
        return false
    }

    private fun doResetParcel() {
        val parcelData = TODO<Any?>("APR: parcel.getParcel()")
        val region     = TODO<Any?>("APR: LLViewerParcelMgr::getInstance().getSelectionRegion()")

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

        val centerPoint = TODO<Triple<Int,Int,Int>>("APR: parcelData.getCenterpoint().snap(0)")
        val regionName  = TODO<String>("APR: region.getName()")
        val simAccess   = TODO<String>("APR: region.getSimAccessString()")
        val area        = TODO<Int>("APR: parcelData.getArea()")
        val newName     = "$regionName (${centerPoint.first},${centerPoint.second}) $simAccess ${area}m"

        findChild<UICtrl>("parcel_text")?.setValue(newName)

        val body = buildMap<String, Any> {
            put("flags",          0x01)
            put("local_id",       TODO("APR: parcelData.getLocalID()"))
            put("parcel_flags",   parcelFlags)
            put("name",           newName)
            put("sale_price",     0)
            put("description",    "")
            put("music_url",      "")
            put("media_url",      "")
            put("media_desc",     "")
            put("media_type",     TODO("APR: LLMIMETypes::getDefaultMimeType()"))
            put("media_width",    0)
            put("media_height",   0)
            put("auto_scale",     0)
            put("media_loop",     0)
            put("obscure_media",  0)
            put("obscure_music",  0)
            put("media_id",       LLUUID.NULL)
            put("group_id",       TODO("APR: MAINTENANCE_GROUP_ID"))
            put("pass_price",     10)
            put("pass_hours",     0.0f)
            put("category",       0)
            put("auth_buyer_id",  LLUUID.NULL)
            put("snapshot_id",    LLUUID.NULL)
            put("user_location",  floatArrayOf(0f, 0f, 0f))
            put("user_look_at",   floatArrayOf(0f, 0f, 0f))
            put("landing_type",   0)
        }

        TODO("APR: LLCoreHttpUtil::HttpCoroutineAdapter::messageHttpPost(parcelUpdateCapUrl, body, ...)")

        TODO("APR: send ParcelSetOtherCleanTime(localId=parcelData.getLocalID(), otherCleanTime=5) to region host")

        clearParcelAccessList(parcelData, region, AL_ACCESS)
        clearParcelAccessList(parcelData, region, AL_BAN)
        clearParcelAccessList(parcelData, region, AL_ALLOW_EXPERIENCE)
        clearParcelAccessList(parcelData, region, AL_BLOCK_EXPERIENCE)
    }

    private fun doSellToAnyone() {
        val parcelData = TODO<Any?>("APR: parcel.getParcel()")
        val region     = TODO<Any?>("APR: LLViewerParcelMgr::getInstance().getSelectionRegion()")

        if (parcelData == null || region == null || parcelUpdateCapUrl.isEmpty()) return

        val currentFlags = TODO<UInt>("APR: parcelData.getParcelFlags()")
        val parcelFlags  = (currentFlags or PF_FOR_SALE) and PF_FOR_SALE_OBJECTS.inv()

        val body = mapOf(
            "flags"          to 0x01,
            "local_id"       to TODO("APR: parcelData.getLocalID()"),
            "parcel_flags"   to parcelFlags,
            "sale_price"     to TODO("APR: parcelData.getArea()"),
            "auth_buyer_id"  to LLUUID.NULL
        )

        TODO("APR: LLCoreHttpUtil::HttpCoroutineAdapter::messageHttpPost(parcelUpdateCapUrl, body, ...)")
        cleanupAndClose()
    }

    private fun clearParcelAccessList(parcelData: Any?, region: Any?, list: UInt) {
        if (region == null || parcelData == null) return
        val txId = LLUUID.generate()
        TODO("APR: send ParcelAccessListUpdate message to region.getHost() " +
             "with flags=$list, localId, transactionId=$txId, sequenceId=1, sections=0, " +
             "and one empty List block (id=NULL, time=0, flags=0)")
    }

    private fun cleanupAndClose() {
        imageId    = LLUUID.NULL
        image      = null
        parcelId   = -1
        parcelHost = TODO("APR: LLHost().invalidate()")
        TODO("APR: closeFloater()")
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun <T> findChild(name: String): T? = null
    private fun setChildEnabled(name: String, enabled: Boolean) {
        TODO("APR: getChildView(name).setEnabled(enabled)")
    }
    private fun getString(key: String): String = TODO("APR: XUI string lookup for key=$key")
}

// -------------------------------------------------------------------------
// Standalone asset-upload completion callbacks (C++ file-scope functions).
// -------------------------------------------------------------------------

fun auctionTgaUploadDone(assetId: LLUUID, name: String, status: Int) {
    TODO("APR: gViewerWindow.getWindow().decBusyCount()")
    if (status == 0) {
        TODO("APR: LLNotificationsUtil::add(\"UploadWebSnapshotDone\")")
    } else {
        TODO("APR: LLNotificationsUtil::add(\"UploadAuctionSnapshotFail\", args=[reason=errorString(status)])")
    }
}

fun auctionJ2cUploadDone(assetId: LLUUID, name: String, status: Int) {
    TODO("APR: gViewerWindow.getWindow().decBusyCount()")
    if (status == 0) {
        TODO("APR: LLNotificationsUtil::add(\"UploadSnapshotDone\")")
    } else {
        TODO("APR: LLNotificationsUtil::add(\"UploadAuctionSnapshotFail\", args=[reason=errorString(status)])")
    }
}
