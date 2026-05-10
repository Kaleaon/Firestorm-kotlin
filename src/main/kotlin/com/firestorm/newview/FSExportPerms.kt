package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// Format version for the OXP export container.
const val OXP_FORMAT_VERSION: Int = 2

/**
 * Export-permission policy reported by an OpenSim grid's simulator features.
 * Mirrors `LFSimFeatureHandler::EExportPolicy`.
 */
enum class ExportPolicy { EXPORT_ALLOWED, EXPORT_UNDEFINED, EXPORT_DENIED }

/**
 * Permission bit constants mirroring the PERM_* macros.
 */
object PermBits {
    const val PERM_EXPORT: Int = 1 shl 13
    const val PERM_ITEM_UNRESTRICTED: Int = 0x0008_E000
}

/**
 * Utility functions for checking whether an in-world object or inventory asset
 * is eligible for export, honouring both Second Life and OpenSim grid policies.
 *
 * Mirrors the `FSExportPermsCheck` namespace from `fsexportperms.h/.cpp`.
 */
object ExportPermsCheck {

    /**
     * Return `true` if the selection node is exportable.
     *
     * Checks object ownership, creator identity, and — for sculpted/mesh objects —
     * the sculpt-map asset's permissions.  When [dae] is `true` meshes are allowed
     * (DAE export); when `false` meshes are blocked (OXP format cannot carry them).
     *
     * @param node The selected object node; returns `false` immediately if null.
     * @param dae  Whether the target format is Collada DAE (`true`) or OXP (`false`).
     */
    fun canExportNode(node: SelectNode?, dae: Boolean): Boolean {
        if (node == null) return false

        val obj = node.getObject() ?: return false
        val agentId = Agent.id
        val creatorId = node.permissions.creator

        var exportable = false

        when {
            GridManager.isInSecondLife() -> {
                exportable = obj.isOwnedByAgent && agentId == creatorId
                if (!exportable) {
                    // Megaprims created by known legacy accounts are exempt.
                    val maxScale = World.regionMaxPrimScale
                    val scale = obj.scale
                    if (scale.x > maxScale || scale.y > maxScale || scale.z > maxScale) {
                        val zwagoth = LLUUID("7ffd02d0-12f4-48b4-9640-695708fd4ae4")
                        exportable = creatorId == zwagoth || creatorId == agentId
                    }
                }
            }
            GridManager.isInOpenSim() -> {
                exportable = when (SimFeatureHandler.exportPolicy()) {
                    ExportPolicy.EXPORT_ALLOWED   -> node.permissions.allowOpenSimExportBy(agentId)
                    ExportPolicy.EXPORT_UNDEFINED ->
                        obj.isOwnedByAgent && obj.canModify && obj.canCopy && obj.canTransfer
                    ExportPolicy.EXPORT_DENIED    ->
                        obj.isOwnedByAgent && agentId == creatorId
                }
            }
        }

        if (!exportable) return false

        // Secondary check: sculpt-map / mesh asset permissions.
        if (!obj.isSculpted) return true   // plain prim — no asset to check

        val sculptParams = obj.sculptParams ?: return true

        return when {
            GridManager.isInSecondLife() -> {
                if (obj.isMesh) {
                    if (dae) {
                        // Mesh is allowed for DAE only when the agent created the mesh asset.
                        MeshRepository.getCreatorFromHeader(sculptParams.sculptTexture) == agentId
                    } else {
                        // Mesh cannot be packaged into OXP.
                        false
                    }
                } else {
                    checkSculptTexturePermissionsSL(sculptParams.sculptTexture, agentId)
                }
            }
            GridManager.isInOpenSim() -> {
                if (obj.isMesh) {
                    true   // OpenSim: mesh sculpts pass if the object itself passed
                } else {
                    checkSculptTexturePermissionsOpenSim(sculptParams.sculptTexture, agentId)
                }
            }
            else -> false
        }
    }

    private fun checkSculptTexturePermissionsSL(textureId: LLUUID, agentId: LLUUID): Boolean {
        // First try the embedded creator comment on the fetched texture.
        val imagep = TextureManager.getFetchedTexture(textureId)
        if (imagep != null) {
            val creatorFromComment = imagep.comments["a"]
            if (creatorFromComment != null && LLUUID(creatorFromComment) == agentId) return true
        }

        // Fall back to inventory search by asset ID.
        return inventoryCreatorCheck(textureId, agentId) { perms ->
            perms.creator == agentId
        }
    }

    private fun checkSculptTexturePermissionsOpenSim(textureId: LLUUID, agentId: LLUUID): Boolean =
        inventoryCreatorCheck(textureId, agentId) { perms ->
            when (SimFeatureHandler.exportPolicy()) {
                ExportPolicy.EXPORT_ALLOWED   -> (perms.maskOwner and PermBits.PERM_EXPORT) == PermBits.PERM_EXPORT
                ExportPolicy.EXPORT_UNDEFINED -> (perms.maskBase and PermBits.PERM_ITEM_UNRESTRICTED) == PermBits.PERM_ITEM_UNRESTRICTED
                ExportPolicy.EXPORT_DENIED    -> perms.creator == agentId
            }
        }

    /**
     * Return `true` if at least one inventory item that references [assetId] passes
     * the [permCheck] predicate.  Also populates [name] and [description] with the
     * metadata of the first matching item when they are non-null.
     *
     * Mirrors `FSExportPermsCheck::canExportAsset`.
     */
    fun canExportAsset(
        assetId: LLUUID,
        name: StringBuilder? = null,
        description: StringBuilder? = null,
    ): Boolean {
        val items = Inventory.findItemsByAssetId(assetId)
        if (items.isEmpty()) return false

        name?.apply { setLength(0); append(items[0].name) }
        description?.apply { setLength(0); append(items[0].description) }

        val agentId = Agent.id
        var exportable = false
        for (item in items) {
            if (exportable) break
            val perms = item.permissions
            if (GridManager.isInOpenSim()) {
                exportable = when (SimFeatureHandler.exportPolicy()) {
                    ExportPolicy.EXPORT_ALLOWED   -> (perms.maskOwner and PermBits.PERM_EXPORT) == PermBits.PERM_EXPORT
                    ExportPolicy.EXPORT_UNDEFINED -> (perms.maskBase and PermBits.PERM_ITEM_UNRESTRICTED) == PermBits.PERM_ITEM_UNRESTRICTED
                    ExportPolicy.EXPORT_DENIED    -> perms.creator == agentId
                }
            }
            if (GridManager.isInSecondLife() && perms.creator == agentId) {
                exportable = true
            }
        }
        return exportable
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun inventoryCreatorCheck(
        assetId: LLUUID,
        agentId: LLUUID,
        permCheck: (InventoryPermissions) -> Boolean,
    ): Boolean {
        val items = Inventory.findItemsByAssetId(assetId)
        return items.any { permCheck(it.permissions) }
    }
}

// =============================================================================
// Stub types — stand-ins for C++ viewer classes until those layers are ported
// =============================================================================

/** Stub: mirrors `LLSelectNode`. */
class SelectNode {
    val permissions: ObjectPermissions get() = TODO("Platform: return node permissions")
    fun getObject(): ViewerObject? = TODO("Platform: return associated LLViewerObject")
}

/** Stub: mirrors `LLPermissions` as seen from a select node. */
class ObjectPermissions {
    val creator: LLUUID get() = TODO("Platform: permissions.getCreator()")
    fun allowOpenSimExportBy(agentId: LLUUID): Boolean =
        TODO("Platform: permissions.allowOpenSimExportBy(agentId)")
}

/** Stub: mirrors `LLPermissions` as seen from an inventory item. */
class InventoryPermissions {
    val creator: LLUUID get() = TODO("Platform: perms.getCreator()")
    val maskOwner: Int get() = TODO("Platform: perms.getMaskOwner()")
    val maskBase: Int get() = TODO("Platform: perms.getMaskBase()")
}

/** Stub: relevant facets of `LLViewerObject`. */
class ViewerObject {
    val isOwnedByAgent: Boolean get() = TODO("Platform: object->permYouOwner()")
    val canModify: Boolean get() = TODO("Platform: object->permModify()")
    val canCopy: Boolean get() = TODO("Platform: object->permCopy()")
    val canTransfer: Boolean get() = TODO("Platform: object->permTransfer()")
    val scale: Triple<Float, Float, Float> get() = TODO("Platform: object->getScale()")
    val isSculpted: Boolean get() = TODO("Platform: volobjp->isSculpted()")
    val isMesh: Boolean get() = TODO("Platform: volobjp->isMesh()")
    val sculptParams: SculptParams? get() = TODO("Platform: object->getSculptParams()")
}

/** Stub: mirrors `LLSculptParams`. */
class SculptParams {
    val sculptTexture: LLUUID get() = TODO("Platform: sculpt_params->getSculptTexture()")
}

/** Stub: thin wrapper around the fetched texture's comment map. */
class FetchedTexture {
    val comments: Map<String, String> get() = TODO("Platform: imagep->mComment")
}

/** Stub: mirrors the texture manager singleton. */
object TextureManager {
    fun getFetchedTexture(id: LLUUID): FetchedTexture? =
        TODO("Platform: LLViewerTextureManager::getFetchedTexture(id)")
}

/** Stub: inventory item with name, description, and permissions. */
class InventoryItem {
    val name: String get() = TODO("Platform: items[i]->getName()")
    val description: String get() = TODO("Platform: items[i]->getDescription()")
    val permissions: InventoryPermissions get() = TODO("Platform: items[i]->getPermissions()")
}

/** Stub: inventory access singleton. */
object Inventory {
    fun findItemsByAssetId(assetId: LLUUID): List<InventoryItem> =
        TODO("Platform: gInventory.collectDescendentsIf(LLUUID::null, ..., LLAssetIDMatches(assetId))")
}

/** Stub: agent identity. */
object Agent {
    val id: LLUUID get() = TODO("Platform: gAgentID")
}

/** Stub: grid type query. */
object GridManager {
    fun isInSecondLife(): Boolean = TODO("Platform: LLGridManager::getInstance()->isInSecondLife()")
    fun isInOpenSim(): Boolean = TODO("Platform: LLGridManager::getInstance()->isInOpenSim()")
}

/** Stub: OpenSim simulator-feature handler. */
object SimFeatureHandler {
    fun exportPolicy(): ExportPolicy = TODO("Platform: LFSimFeatureHandler::instance().exportPolicy()")
}

/** Stub: world/region queries. */
object World {
    val regionMaxPrimScale: Float get() = TODO("Platform: LLWorld::getInstance()->getRegionMaxPrimScale()")
}

/** Stub: mesh-repository creator lookup. */
object MeshRepository {
    fun getCreatorFromHeader(assetId: LLUUID): LLUUID =
        TODO("Platform: gMeshRepo.getCreatorFromHeader(assetId)")
}
