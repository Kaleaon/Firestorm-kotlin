/**
 * ViewerWearable.kt
 * Converted from llviewerwearable.h / llviewerwearable.cpp
 *
 * Viewer-side wearable asset — extends the appearance-layer Wearable with
 * inventory-item binding, dirty-tracking, bake integration, and asset saving.
 *
 * Mirrors LLViewerWearable : public LLWearable.
 */

package com.firestorm.newview

import com.firestorm.llappearance.Wearable
import com.firestorm.llappearance.WearableType
import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// ViewerWearable
// ---------------------------------------------------------------------------

/**
 * A wearable asset as seen by the viewer, adding inventory-item tracking,
 * dirty state, baked-texture support, and server-save capability on top of
 * the base [Wearable] from the appearance library.
 *
 * Instances are created exclusively by [WearableList]; constructors are
 * internal to the package.
 *
 * @param assetId  The asset UUID for this wearable (the content hash).
 * @param type     Which wearable slot this asset occupies (shirt, skin, …).
 */
class ViewerWearable internal constructor(
    val assetId: LLUUID,
    override val type: WearableType
) : Wearable(type) {

    // -----------------------------------------------------------------------
    // Inventory binding
    // -----------------------------------------------------------------------

    /**
     * The inventory-item UUID that caused this wearable to be worn.
     * One asset may be worn via multiple items (see EXT-6252 in WearableList).
     * Mirrors LLViewerWearable::mItemID.
     */
    var itemId: LLUUID = LLUUID.NULL
        private set

    /** Update the inventory-item binding. */
    fun setItemId(id: LLUUID) {
        itemId = id
    }

    // -----------------------------------------------------------------------
    // Transaction ID (used when creating new wearable assets)
    // -----------------------------------------------------------------------

    /**
     * Transaction ID from which [assetId] was derived for newly-created
     * wearables.  Null for wearables loaded from existing assets.
     * Mirrors LLViewerWearable::mTransactionID.
     */
    var transactionId: LLUUID = LLUUID.NULL
        internal set

    // -----------------------------------------------------------------------
    // Volatile flag
    // -----------------------------------------------------------------------

    /**
     * True while a preview render is in progress.  Some texture updates are
     * suppressed to avoid thrashing the bake pipeline.
     * Mirrors LLViewerWearable::mVolatile.
     */
    var isVolatile: Boolean = false

    // -----------------------------------------------------------------------
    // Dirty / version state
    // -----------------------------------------------------------------------

    /**
     * Returns true if any parameter or texture has been changed since the
     * wearable was last saved to the asset store.
     */
    fun isDirty(): Boolean {
        System.err.println("ViewerWearable: isDirty not yet implemented")
        return false
    }

    /**
     * Returns true if this wearable was created with an older definition
     * version and may be missing fields introduced in later viewer versions.
     */
    fun isOldVersion(): Boolean {
        System.err.println("ViewerWearable: isOldVersion not yet implemented")
        return false
    }

    // -----------------------------------------------------------------------
    // Avatar application
    // -----------------------------------------------------------------------

    /**
     * Apply this wearable's parameters and textures to [avatar].
     * Overrides LLWearable::writeToAvatar.
     *
     * @param avatar The avatar appearance object to modify; typed [Any?] to
     *               avoid a hard dependency on the avatar module.
     */
    override fun writeToAvatar(avatar: Any?) {
        System.err.println("ViewerWearable: writeToAvatar not yet implemented")
    }

    /**
     * Remove this wearable type's contribution from the avatar.
     *
     * @param uploadBake If true, trigger a new baked texture upload after
     *                   removing (legacy bake path).
     */
    fun removeFromAvatar(uploadBake: Boolean) {
        removeFromAvatar(type, uploadBake)
    }

    // -----------------------------------------------------------------------
    // Local texture data readiness
    // -----------------------------------------------------------------------

    /**
     * Returns true if at least one packet of texture data has arrived for
     * every local texture this wearable references.
     * Delegates to the avatar self's texture tracking.
     */
    fun isLocalTextureDataAvailable(): Boolean {
        System.err.println("ViewerWearable: isLocalTextureDataAvailable not yet implemented")
        return false
    }

    /**
     * Returns true if the highest-available LOD of every referenced local
     * texture has been fully downloaded.
     */
    fun isLocalTextureDataFinal(): Boolean {
        System.err.println("ViewerWearable: isLocalTextureDataFinal not yet implemented")
        return false
    }

    // -----------------------------------------------------------------------
    // Param / texture defaults
    // -----------------------------------------------------------------------

    /**
     * Reset all visual parameters to their type-default values.
     * Mirrors LLViewerWearable::setParamsToDefaults().
     */
    fun setParamsToDefaults() {
        System.err.println("ViewerWearable: setParamsToDefaults not yet implemented")
    }

    /**
     * Reset all texture references to their type-default UUIDs.
     * Mirrors LLViewerWearable::setTexturesToDefaults().
     */
    fun setTexturesToDefaults() {
        System.err.println("ViewerWearable: setTexturesToDefaults not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Data copy
    // -----------------------------------------------------------------------

    /**
     * Deep-copy visual parameters, textures, and metadata from [src].
     * Mirrors LLViewerWearable::copyDataFrom().
     *
     * @param src The wearable to copy from.
     */
    fun copyDataFrom(src: ViewerWearable) {
        System.err.println("ViewerWearable: copyDataFrom not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Save / revert
    // -----------------------------------------------------------------------

    /**
     * Serialise this wearable and upload it as a new asset to the server.
     * The callback [onSaveNewAssetComplete] is invoked upon completion.
     * Mirrors LLViewerWearable::saveNewAsset().
     */
    fun saveNewAsset() {
        System.err.println("ViewerWearable: saveNewAsset not yet implemented")
    }

    /**
     * Snapshot the current parameter/texture values so they can be
     * restored later via [revertValues].
     */
    fun saveValues() {
        System.err.println("ViewerWearable: saveValues not yet implemented")
    }

    /**
     * Restore the parameter/texture values to the last [saveValues] snapshot
     * and trigger an avatar update.
     */
    fun revertValues() {
        System.err.println("ViewerWearable: revertValues not yet implemented")
    }

    /**
     * Restore values without triggering a baked-texture update.
     * Used when rolling back changes that should not produce a new bake.
     */
    fun revertValuesWithoutUpdate() {
        System.err.println("ViewerWearable: revertValuesWithoutUpdate not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Baked texture support  (legacy bake)
    // -----------------------------------------------------------------------

    /**
     * Contribute this wearable's texture UUIDs to the MD5 hash used to
     * detect whether a new bake is needed.
     * Mirrors LLViewerWearable::addToBakedTextureHash().
     */
    fun addToBakedTextureHash() {
        // no-op
    }

    /**
     * Refresh the wearable's display name to match the inventory item's name.
     * Called when the wearable is first worn.
     * Mirrors LLViewerWearable::refreshName().
     */
    fun refreshName() {
        System.err.println("ViewerWearable: refreshName not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Companion object  (static helpers)
    // -----------------------------------------------------------------------

    companion object {

        /**
         * Static variant of [removeFromAvatar] operating on a wearable type.
         *
         * @param type       The wearable slot to clear.
         * @param uploadBake Whether to re-upload the baked texture afterwards.
         */
        fun removeFromAvatar(type: WearableType, uploadBake: Boolean) {
            System.err.println("ViewerWearable: removeFromAvatar not yet implemented")
        }

        /**
         * Asset-storage callback fired when [saveNewAsset] completes.
         *
         * @param assetUuid UUID assigned to the newly saved asset.
         * @param wearable  The [ViewerWearable] that initiated the save.
         * @param status    0 on success; negative on failure.
         */
        fun onSaveNewAssetComplete(
            assetUuid: LLUUID,
            wearable: ViewerWearable?,
            status: Int
        ) {
            System.err.println("ViewerWearable: onSaveNewAssetComplete not yet implemented")
        }
    }
}
