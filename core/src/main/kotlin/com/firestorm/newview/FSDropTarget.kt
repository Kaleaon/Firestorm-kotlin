package com.firestorm.newview

import java.util.UUID

enum class EDragAndDropType {
    DAD_NONE,
    DAD_TEXTURE,
    DAD_SOUND,
    DAD_CALLINGCARD,
    DAD_LANDMARK,
    DAD_SCRIPT,
    DAD_CLOTHING,
    DAD_OBJECT,
    DAD_NOTECARD,
    DAD_CATEGORY,
    DAD_ROOT_CATEGORY,
    DAD_BODYPART,
    DAD_ANIMATION,
    DAD_GESTURE
}

enum class EAcceptance {
    ACCEPT_NO,
    ACCEPT_NO_LOCKED,
    ACCEPT_YES_COPY_SINGLE,
    ACCEPT_YES_SINGLE,
    ACCEPT_YES_COPY_MULTI,
    ACCEPT_YES_MULTI
}

const val PERM_COPY: Int     = 0x00008000
const val PERM_TRANSFER: Int = 0x00002000

interface LLInventoryItem {
    fun getUUID(): UUID
    fun getActualType(): LLAssetType
    fun getType(): LLAssetType
    fun getPermissions(): LLPermissions
}

enum class LLAssetType {
    AT_TEXTURE, AT_SOUND, AT_CALLINGCARD, AT_LANDMARK, AT_SCRIPT,
    AT_CLOTHING, AT_OBJECT, AT_NOTECARD, AT_CATEGORY, AT_LSL_TEXT,
    AT_LSL_BYTECODE, AT_TEXTURE_TGA, AT_BODYPART, AT_TRASH, AT_SNAPSHOT_CATEGORY,
    AT_LOST_AND_FOUND, AT_ANIMATION, AT_GESTURE, AT_SIMSTATE,
    AT_LINK, AT_LINK_FOLDER
}

interface LLPermissions {
    fun getMaskOwner(): Int
}

open class LLLineEditor {
    open var isEnabled: Boolean = true
    open fun postBuild(): Boolean = true
}

open class LLTextBox

class FSCopyTransInventoryDropTarget : LLLineEditor() {

    private val dadListeners: MutableList<(UUID) -> Unit> = mutableListOf()

    fun setDADCallback(cb: (UUID) -> Unit): () -> Unit {
        dadListeners.add(cb)
        return { dadListeners.remove(cb) }
    }

    override fun postBuild(): Boolean {
        isEnabled = false
        return super.postBuild()
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: EDragAndDropType,
        cargoData: Any?,
        accept: Array<EAcceptance>,
        tooltipMsg: StringBuilder
    ): Boolean {
        val item = cargoData as? LLInventoryItem

        if (cargoType >= EDragAndDropType.DAD_TEXTURE && cargoType <= EDragAndDropType.DAD_GESTURE &&
            item != null &&
            item.getActualType() != LLAssetType.AT_LINK &&
            item.getActualType() != LLAssetType.AT_LINK_FOLDER &&
            item.getType() != LLAssetType.AT_CATEGORY &&
            (item.getPermissions().getMaskOwner() and PERM_COPY) != 0 &&
            (item.getPermissions().getMaskOwner() and PERM_TRANSFER) != 0
        ) {
            if (drop) {
                dadListeners.forEach { it(item.getUUID()) }
            } else {
                accept[0] = EAcceptance.ACCEPT_YES_SINGLE
            }
        } else {
            accept[0] = EAcceptance.ACCEPT_NO
        }

        return true
    }
}

class FSEmbeddedItemDropTarget : LLTextBox() {

    protected val dadListeners: MutableList<(UUID) -> Unit> = mutableListOf()

    fun setDADCallback(cb: (UUID) -> Unit): () -> Unit {
        dadListeners.add(cb)
        return { dadListeners.remove(cb) }
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: EDragAndDropType,
        cargoData: Any?,
        accept: Array<EAcceptance>,
        tooltipMsg: StringBuilder
    ): Boolean {
        val item = cargoData as? LLInventoryItem

        if (cargoType >= EDragAndDropType.DAD_TEXTURE && cargoType <= EDragAndDropType.DAD_GESTURE &&
            item != null &&
            item.getActualType() != LLAssetType.AT_LINK &&
            item.getActualType() != LLAssetType.AT_LINK_FOLDER &&
            item.getType() != LLAssetType.AT_CATEGORY &&
            (item.getPermissions().getMaskOwner() and PERM_COPY) != 0 &&
            (item.getPermissions().getMaskOwner() and PERM_TRANSFER) != 0
        ) {
            if (drop) {
                dadListeners.forEach { it(item.getUUID()) }
            } else {
                accept[0] = EAcceptance.ACCEPT_YES_SINGLE
            }
        } else {
            accept[0] = EAcceptance.ACCEPT_NO
        }

        return true
    }
}
