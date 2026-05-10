package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import java.util.UUID

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
    fun canExportNode(node: ExportSelectNode?, dae: Boolean): Boolean {
        if (node == null) return false

        val obj = node.getObject() ?: return false
        val agentId = ExportAgent.id
        val creatorId = node.permissions.creator

        var exportable = false

        when {
            ExportGridManager.isInSecondLife() -> {
                exportable = obj.isOwnedByAgent && agentId == creatorId
                if (!exportable) {
                    // Megaprims created by known legacy accounts are exempt.
                    val maxScale = ExportWorld.regionMaxPrimScale
                    val scale = obj.scale
                    if (scale.first > maxScale || scale.second > maxScale || scale.third > maxScale) {
                        val zwagoth = LLUUID(UUID.fromString("7ffd02d0-12f4-48b4-9640-695708fd4ae4"))
                        exportable = creatorId == zwagoth || creatorId == agentId
                    }
                }
            }
            ExportGridManager.isInOpenSim() -> {
                exportable = when (ExportSimFeatureHandler.exportPolicy()) {
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
            ExportGridManager.isInSecondLife() -> {
                if (obj.isMesh) {
                    if (dae) {
                        // Mesh is allowed for DAE only when the agent created the mesh asset.
                        ExportMeshRepository.getCreatorFromHeader(sculptParams.sculptTexture) == agentId
                    } else {
                        // Mesh cannot be packaged into OXP.
                        false
                    }
                } else {
                    checkSculptTexturePermissionsSL(sculptParams.sculptTexture, agentId)
                }
            }
            ExportGridManager.isInOpenSim() -> {
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
        val imagep = ExportTextureManager.getFetchedTexture(textureId)
        if (imagep != null) {
            val creatorFromComment = imagep.comments["a"]
            if (creatorFromComment != null &&
                LLUUID(UUID.fromString(creatorFromComment)) == agentId
            ) return true
        }
        return inventoryCreatorCheck(textureId) { perms -> perms.creator == agentId }
    }

    private fun checkSculptTexturePermissionsOpenSim(textureId: LLUUID, agentId: LLUUID): Boolean =
        inventoryCreatorCheck(textureId) { perms ->
            when (ExportSimFeatureHandler.exportPolicy()) {
                ExportPolicy.EXPORT_ALLOWED ->
                    (perms.maskOwner and PermBits.PERM_EXPORT) == PermBits.PERM_EXPORT
                ExportPolicy.EXPORT_UNDEFINED ->
                    (perms.maskBase and PermBits.PERM_ITEM_UNRESTRICTED) == PermBits.PERM_ITEM_UNRESTRICTED
                ExportPolicy.EXPORT_DENIED ->
                    perms.creator == agentId
            }
        }

    /**
     * Return `true` if at least one inventory item that references [assetId] passes
     * the creator/permission rules.  Also populates [name] and [description] with
     * the metadata of the first matching item.
     *
     * Mirrors `FSExportPermsCheck::canExportAsset`.
     */
    fun canExportAsset(
        assetId: LLUUID,
        name: StringBuilder? = null,
        description: StringBuilder? = null,
    ): Boolean {
        val items = ExportInventory.findItemsByAssetId(assetId)
        if (items.isEmpty()) return false

        name?.apply { setLength(0); append(items[0].name) }
        description?.apply { setLength(0); append(items[0].description) }

        val agentId = ExportAgent.id
        var exportable = false
        for (item in items) {
            if (exportable) break
            val perms = item.permissions
            if (ExportGridManager.isInOpenSim()) {
                exportable = when (ExportSimFeatureHandler.exportPolicy()) {
                    ExportPolicy.EXPORT_ALLOWED ->
                        (perms.maskOwner and PermBits.PERM_EXPORT) == PermBits.PERM_EXPORT
                    ExportPolicy.EXPORT_UNDEFINED ->
                        (perms.maskBase and PermBits.PERM_ITEM_UNRESTRICTED) == PermBits.PERM_ITEM_UNRESTRICTED
                    ExportPolicy.EXPORT_DENIED ->
                        perms.creator == agentId
                }
            }
            if (ExportGridManager.isInSecondLife() && perms.creator == agentId) {
                exportable = true
            }
        }
        return exportable
    }

    private fun inventoryCreatorCheck(
        assetId: LLUUID,
        permCheck: (ExportInventoryPermissions) -> Boolean,
    ): Boolean = ExportInventory.findItemsByAssetId(assetId).any { permCheck(it.permissions) }
}

// =============================================================================
// Stub types — these are local to the export-perms domain and do not clash
// with any other stub file in the project.  All names carry the "Export" prefix.
// =============================================================================

/** Stub: a selection node as seen by the export-perms checker. */
class ExportSelectNode {
    val permissions: ExportObjectPermissions get() = TODO("Platform: node->mPermissions")
    fun getObject(): ExportViewerObject? = TODO("Platform: node->getObject()")
}

/** Stub: object-level permissions. */
class ExportObjectPermissions {
    val creator: LLUUID get() = TODO("Platform: permissions->getCreator()")
    fun allowOpenSimExportBy(agentId: LLUUID): Boolean =
        TODO("Platform: permissions->allowOpenSimExportBy(agentId)")
}

/** Stub: inventory-item permission mask. */
class ExportInventoryPermissions {
    val creator: LLUUID get() = TODO("Platform: perms.getCreator()")
    val maskOwner: Int get() = TODO("Platform: perms.getMaskOwner()")
    val maskBase: Int get() = TODO("Platform: perms.getMaskBase()")
}

/** Stub: viewer-object facets relevant to export. */
class ExportViewerObject {
    val isOwnedByAgent: Boolean get() = TODO("Platform: object->permYouOwner()")
    val canModify: Boolean get() = TODO("Platform: object->permModify()")
    val canCopy: Boolean get() = TODO("Platform: object->permCopy()")
    val canTransfer: Boolean get() = TODO("Platform: object->permTransfer()")
    /** Triple of (x, y, z) scale components. */
    val scale: Triple<Float, Float, Float> get() = TODO("Platform: object->getScale()")
    val isSculpted: Boolean get() = TODO("Platform: volobjp->isSculpted()")
    val isMesh: Boolean get() = TODO("Platform: volobjp->isMesh()")
    val sculptParams: ExportSculptParams? get() = TODO("Platform: object->getSculptParams()")
}

/** Stub: sculpt-parameter block. */
class ExportSculptParams {
    val sculptTexture: LLUUID get() = TODO("Platform: sculpt_params->getSculptTexture()")
}

/** Stub: fetched-texture comment map. */
class ExportFetchedTexture {
    val comments: Map<String, String> get() = TODO("Platform: imagep->mComment")
}

/** Stub: texture manager. */
object ExportTextureManager {
    fun getFetchedTexture(id: LLUUID): ExportFetchedTexture? =
        TODO("Platform: LLViewerTextureManager::getFetchedTexture(id)")
}

/** Stub: inventory item with asset-id lookup. */
class ExportInventoryItem {
    val name: String get() = TODO("Platform: items[i]->getName()")
    val description: String get() = TODO("Platform: items[i]->getDescription()")
    val permissions: ExportInventoryPermissions get() = TODO("Platform: items[i]->getPermissions()")
}

/** Stub: inventory model. */
object ExportInventory {
    fun findItemsByAssetId(assetId: LLUUID): List<ExportInventoryItem> =
        TODO("Platform: gInventory.collectDescendentsIf(LLUUID::null, ..., LLAssetIDMatches(assetId))")
}

/** Stub: agent identity. */
object ExportAgent {
    val id: LLUUID get() = TODO("Platform: gAgentID")
}

/** Stub: grid-type query. */
object ExportGridManager {
    fun isInSecondLife(): Boolean = TODO("Platform: LLGridManager::getInstance()->isInSecondLife()")
    fun isInOpenSim(): Boolean = TODO("Platform: LLGridManager::getInstance()->isInOpenSim()")
}

/** Stub: OpenSim simulator-feature export policy. */
object ExportSimFeatureHandler {
    fun exportPolicy(): ExportPolicy = TODO("Platform: LFSimFeatureHandler::instance().exportPolicy()")
}

/** Stub: region max prim scale. */
object ExportWorld {
    val regionMaxPrimScale: Float get() = TODO("Platform: LLWorld::getInstance()->getRegionMaxPrimScale()")
}

/** Stub: mesh-repository creator lookup. */
object ExportMeshRepository {
    fun getCreatorFromHeader(assetId: LLUUID): LLUUID =
        TODO("Platform: gMeshRepo.getCreatorFromHeader(assetId)")
}
