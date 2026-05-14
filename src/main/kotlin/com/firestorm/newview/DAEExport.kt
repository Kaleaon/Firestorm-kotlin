package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

// =============================================================================
// Constants
// =============================================================================

private const val TEXTURE_DOWNLOAD_TIMEOUT: Float = 60f
private const val EXPANDED_WIDTH: Int = 500
private const val COLLAPSED_WIDTH: Int = 250

// =============================================================================
// Well-known texture IDs
// =============================================================================

private val LL_TEXTURE_TRANSPARENT = LLUUID(UUID.fromString("8dcd4a48-2d37-4909-9f78-f7a9eb4ef903"))
private val LL_TEXTURE_BLANK = LLUUID(UUID.fromString("5748decc-f629-461c-9a36-a35a221fe21f"))

// =============================================================================
// Image format enum
// =============================================================================

enum class ImageFormatType(val ext: String) {
    TGA("tga"),
    PNG("png"),
    J2C("j2c"),
    ;

    companion object {
        fun fromIndex(index: Int): ImageFormatType = entries.getOrElse(index) { TGA }
    }
}

// =============================================================================
// MaterialInfo — describes a single face material (texture + colour)
// =============================================================================

/**
 * Carries the texture UUID and vertex colour for one face of a prim.
 *
 * Mirrors `DAESaver::MaterialInfo` from `daeexport.h`.
 */
data class MaterialInfo(
    val textureId: LLUUID = LLUUID.NULL,
    val color: DaeColor4 = DaeColor4.WHITE,
    val name: String = "",
) {
    fun matchesFace(te: DaeTextureEntry): Boolean =
        textureId == te.textureId && color == te.color
}

// =============================================================================
// DAESaver — builds the in-memory Collada DOM and writes the .dae file
// =============================================================================

/**
 * Accumulates a list of viewer objects and saves them as a Collada DAE file.
 *
 * Mirrors `DAESaver` from `daeexport.h/.cpp`.  All Collada DOM interactions
 * are stubbed with `System.err.println("GPU: …")` because the colladadom library does not
 * have a JVM counterpart.
 */
class DAESaver {

    val allMaterials: MutableList<MaterialInfo> = mutableListOf()
    val textures: MutableList<LLUUID> = mutableListOf()
    val textureNames: MutableList<String> = mutableListOf()
    val objects: MutableList<Pair<DaeViewerObject, String>> = mutableListOf()
    var offset: DaeVector3 = DaeVector3.ZERO
    var imageFormat: String = ImageFormatType.TGA.ext
    var totalNumMaterials: Int = 0

    fun add(prim: DaeViewerObject, name: String) {
        objects.add(Pair(prim, name))
    }

    /**
     * Rebuild [textures] and [textureNames] from the current [objects] list,
     * applying SL/OpenSim creator-permission checks via [ExportPermsCheck].
     */
    fun updateTextureInfo() {
        textures.clear()
        textureNames.clear()

        for ((obj, _) in objects) {
            val numFaces = obj.numVolumeFaces
            for (faceNum in 0 until numFaces) {
                val te = obj.getTextureEntry(faceNum)
                val id = te.textureId
                if (id in textures) continue

                textures.add(id)
                var exportable = false

                // SL-specific creator comment embedded in the texture asset.
                if (ExportGridManager.isInSecondLife()) {
                    val imagep = ExportTextureManager.getFetchedTexture(id)
                    val commentCreator = imagep?.comments?.get("a")
                    if (commentCreator != null &&
                        LLUUID(UUID.fromString(commentCreator)) == ExportAgent.id
                    ) {
                        exportable = true
                    }
                }

                val name = StringBuilder()
                val desc = StringBuilder()
                if (exportable) {
                    ExportPermsCheck.canExportAsset(id, name, desc)
                } else {
                    exportable = ExportPermsCheck.canExportAsset(id, name, desc)
                }

                if (id != LL_TEXTURE_BLANK && exportable) {
                    val safeName = scrubAndSanitize(name.toString())
                    textureNames.add(safeName)
                } else {
                    textureNames.add("")
                }
            }
        }
    }

    /**
     * Write all accumulated objects to a Collada DAE file at [filename].
     *
     * Returns `true` on success.
     */
    fun saveDAE(filename: String): Boolean {
        System.err.println(
            "GPU: build Collada DOM — see daeexport.cpp DAESaver::saveDAE.\n" +
            "Steps:\n" +
            "  1. Escape filename to a URI.\n" +
            "  2. Create dae root element with asset/created/modified/unit/up_axis/contributor.\n" +
            "  3. For each object: add geometry source arrays (positions, normals, UVs),\n" +
            "     vertices node, polylist entries, and a scene node with the transform matrix.\n" +
            "  4. Call generateEffects() and generateImagesSection().\n" +
            "  5. Add library_materials entries.\n" +
            "  6. Invoke dae.writeAll()."
        )
        return false
    }

    // -------------------------------------------------------------------------
    // Private geometry helpers (stub — require Collada DOM or equivalent)
    // -------------------------------------------------------------------------

    private fun transformTexCoord(
        numVert: Int,
        coord: Array<DaeVector2>,
        positions: Array<DaeVector3>,
        normals: Array<DaeVector3>,
        te: DaeTextureEntry,
        scale: DaeVector3,
    ) {
        val cosAngle = cos(te.rotation)
        val sinAngle = sin(te.rotation)

        for (ii in 0 until numVert) {
            if (te.texGen == DaeTexGen.PLANAR) {
                val normal = normals[ii]
                val pos = positions[ii]
                val d = normal dot DaeVector3.X_AXIS
                val binormal = when {
                    d >= 0.5f || d <= -0.5f ->
                        if (normal.x < 0f) -DaeVector3.Y_AXIS else DaeVector3.Y_AXIS
                    else ->
                        if (normal.y > 0f) -DaeVector3.X_AXIS else DaeVector3.X_AXIS
                }
                val tangent = binormal cross normal
                val scaledPos = pos * scale
                coord[ii] = DaeVector2(
                    x = 1f + (binormal.dot(scaledPos) * 2f - 0.5f),
                    y = -((tangent.dot(scaledPos)) * 2f - 0.5f),
                )
            }

            val (repeatU, repeatV) = te.getScale()
            val (offsetU, offsetV) = te.getOffset()
            val tX = coord[ii].x - 0.5f
            val tY = coord[ii].y - 0.5f

            coord[ii] = DaeVector2(
                x = (tX * cosAngle + tY * sinAngle) * repeatU + offsetU + 0.5f,
                y = (-tX * sinAngle + tY * cosAngle) * repeatV + offsetV + 0.5f,
            )
        }
    }

    private fun skipFace(te: DaeTextureEntry): Boolean =
        DaeSavedSettings.getBool("DAEExportSkipTransparent") &&
            (te.color.alpha < 0.01f || te.textureId == LL_TEXTURE_TRANSPARENT)

    private fun getMaterial(te: DaeTextureEntry): MaterialInfo {
        if (DaeSavedSettings.getBool("DAEExportConsolidateMaterials")) {
            allMaterials.firstOrNull { it.matchesFace(te) }?.let { return it }
        }
        val mat = MaterialInfo(
            textureId = te.textureId,
            color = te.color,
            name = "Material${allMaterials.size}",
        )
        allMaterials.add(mat)
        return allMaterials.last()
    }

    private fun getMaterials(obj: DaeViewerObject): List<MaterialInfo> {
        val result = mutableListOf<MaterialInfo>()
        val consolidate = DaeSavedSettings.getBool("DAEExportConsolidateMaterials")
        for (faceNum in 0 until obj.numVolumeFaces) {
            val te = obj.getTextureEntry(faceNum)
            if (skipFace(te)) continue
            val mat = getMaterial(te)
            if (!consolidate || mat !in result) result.add(mat)
        }
        return result
    }

    private fun getFacesWithMaterial(obj: DaeViewerObject, mat: MaterialInfo): List<Int> =
        (0 until obj.numVolumeFaces).filter { getMaterial(obj.getTextureEntry(it)) == mat }

    private fun generateEffects(effects: DaeElement) {
        System.err.println(
            "GPU: iterate allMaterials; for each, add a profile_COMMON/technique/phong " +
            "element with colour or texture sampler reference"
        )
    }

    private fun generateImagesSection(images: DaeElement) {
        System.err.println("GPU: iterate textureNames; for each non-empty name add an image/init_from element")
    }

    private fun addSource(mesh: DaeElement, srcId: String, params: String, vals: List<Float>) {
        System.err.println("GPU: add <source id='$srcId'><float_array> with vals; accessor stride=${params.length}")
    }

    private fun addPolygons(
        mesh: DaeElement,
        geomId: String,
        materialId: String,
        obj: DaeViewerObject,
        facesToInclude: List<Int>?,
    ) {
        System.err.println(
            "GPU: build <polylist material='$materialId'> with VERTEX/NORMAL/TEXCOORD inputs " +
            "and per-triangle index and vcount arrays"
        )
    }

    private fun scrubAndSanitize(name: String): String =
        name.replace(' ', '_')   // gDirUtilp->getScrubbedFileName would also strip illegal chars
}

// =============================================================================
// CacheReadResponder — reads a texture from the viewer cache and saves it
// =============================================================================

/**
 * Responds to an asynchronous texture-cache read and saves the result to disk
 * in the format selected by the user.
 *
 * Mirrors `ColladaExportFloater::CacheReadResponder` from `daeexport.h/.cpp`.
 */
class CacheReadResponder(
    private val id: LLUUID,
    private val name: String,
    private val imageType: ImageFormatType,
) {
    fun setData(data: ByteArray, datasize: Int, imagesize: Int, imageformat: Int, imagelocal: Boolean) {
        System.err.println("GPU: validate codec; append or set image data; store imagesize and imagelocal")
    }

    fun completed(success: Boolean) {
        System.err.println("GPU: on success decode J2C raw data; re-encode to TGA/PNG/J2C; save to disk at name")
    }

    companion object {
        /** Idle-callback worker that drains [ColladaExportFloater.texturesToSave] one entry at a time. */
        fun saveTexturesWorker(floater: ColladaExportFloater) {
            System.err.println(
                "GPU: check mTexturesToSave; for head entry find fetched texture; " +
                "when discardLevel==0 read from cache via CacheReadResponder; " +
                "handle timeout via mTimer; call onTexturesSaved() when map is empty"
            )
        }
    }
}

// =============================================================================
// ColladaExportFloater — the export dialog
// =============================================================================

/**
 * Floater that lets the user export the current selection to a Collada .DAE file.
 *
 * Mirrors `ColladaExportFloater` from `daeexport.h/.cpp`.
 *
 * @param key LLSD construction key from the floater registry.
 */
class ColladaExportFloater(val key: Any) {

    private val saver = DAESaver()

    private var total: Int = 0
    private var included: Int = 0
    private var numTextures: Int = 0
    private var numExportableTextures: Int = 0
    private var objectName: String = ""
    private var filename: String = ""
    private var currentObjectId: LLUUID = LLUUID.NULL
    private var dirty: Boolean = true

    val texturesToSave: MutableMap<LLUUID, String> = mutableMapOf()

    private var objectSelection: DaeObjectSelection? = null

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    fun postBuild(): Boolean {
        System.err.println(
            "Platform: bind ColladaExport.TextureExport callback; " +
            "bind export_btn to onClickExport; connect LLSelectMgr.mUpdateSignal to updateSelection"
        )
        return true
    }

    fun draw() {
        if (dirty) {
            refresh()
            dirty = false
        }
        System.err.println("Platform: LLFloater::draw()")
    }

    fun onOpen(key: Any) {
        val selection = DaeSelectMgr.selection
        if (selection.primaryObject == null) {
            closeFloater()
            return
        }
        objectSelection = DaeSelectMgr.editSelection
        refresh()
    }

    // -------------------------------------------------------------------------
    // UI update
    // -------------------------------------------------------------------------

    private fun refresh() {
        addSelectedObjects()
        onTextureExportCheck()
        addTexturePreview()
        updateUI()
    }

    private fun markDirty() { dirty = true }

    private fun updateUI() {
        System.err.println(
            "Platform: set child text fields for NameText, exportable_prims, exportable_textures; " +
            "set title; enable/disable export_textures_check and export_btn"
        )
    }

    private fun updateTitleProgress() {
        System.err.println("Platform: format and set floater title with object name and remaining texture count")
    }

    // -------------------------------------------------------------------------
    // Selection handling
    // -------------------------------------------------------------------------

    fun updateSelection() {
        val selection = DaeSelectMgr.selection
        val node = selection.firstRootNode
        if (node != null && !node.isValid && node.getObject()?.objectId == currentObjectId) return
        objectSelection = selection
        markDirty()
        refresh()
    }

    private fun addSelectedObjects() {
        total = 0; included = 0; numTextures = 0; numExportableTextures = 0
        saver.objects.clear(); saver.textures.clear(); saver.textureNames.clear()

        val sel = objectSelection ?: return
        val rootNode = sel.firstRootNode ?: run { objectName = ""; return }

        currentObjectId = rootNode.getObject()?.objectId ?: LLUUID.NULL
        saver.offset = -(sel.firstRootObject?.renderPosition ?: DaeVector3.ZERO)
        objectName = rootNode.nodeName

        for (node in sel.nodes()) {
            total++
            val obj = node.getObject() ?: continue
            if (!obj.hasVolume || !ExportPermsCheck.canExportNode(
                    ExportSelectNode().also {
                        System.err.println("Platform: wrap node as ExportSelectNode")
                    },
                    dae = true,
                )
            ) continue
            included++
            saver.add(obj, node.nodeName)
        }

        if (saver.objects.isEmpty()) return

        saver.updateTextureInfo()
        numTextures = saver.textures.size
        numExportableTextures = saver.textureNames.count { it.isNotEmpty() }
    }

    // -------------------------------------------------------------------------
    // Texture panel
    // -------------------------------------------------------------------------

    private fun onTextureExportCheck() {
        val showTexPanel = DaeSavedSettings.getBool("DAEExportTextures") && numExportableTextures > 0
        System.err.println(
            "Platform: set tex_layout_panel visible=$showTexPanel; " +
            "reshape to ${if (showTexPanel) EXPANDED_WIDTH else COLLAPSED_WIDTH}"
        )
    }

    private fun addTexturePreview() {
        if (numExportableTextures == 0) return
        System.err.println(
            "Platform: clear texturesPanel children; for each exportable texture " +
            "create an LLTextureCtrl child positioned in a 2-column grid"
        )
    }

    // -------------------------------------------------------------------------
    // Export flow
    // -------------------------------------------------------------------------

    private fun onClickExport() {
        System.err.println(
            "Platform: LLFilePickerReplyThread.startPicker(FFSAVE_COLLADA, " +
            "scrubbed '$objectName.dae') → onExportFileSelected"
        )
    }

    private fun onExportFileSelected(filenames: List<String>) {
        filename = filenames[0]
        if (DaeSavedSettings.getBool("DAEExportTextures")) {
            saveTextures()
        } else {
            onTexturesSaved()
        }
    }

    private fun saveTextures() {
        texturesToSave.clear()
        for (i in saver.textures.indices) {
            if (saver.textureNames[i].isEmpty()) continue
            texturesToSave[saver.textures[i]] = saver.textureNames[i]
        }
        saver.imageFormat = ImageFormatType.fromIndex(
            DaeSavedSettings.getInt("DAEExportTexturesFormat")
        ).ext
        updateTitleProgress()
        System.err.println("Platform: start mTimer; register CacheReadResponder.saveTexturesWorker as idle callback")
    }

    fun onTexturesSaved() {
        val success = saver.saveDAE(filename)
        if (success) {
            System.err.println("Platform: LLNotificationsUtil.add(\"ExportColladaSuccess\", object=$objectName, filename=$filename)")
        } else {
            System.err.println("Platform: LLNotificationsUtil.add(\"ExportColladaFailure\", object=$objectName, filename=$filename)")
        }
        closeFloater()
    }

    private fun closeFloater() {
        System.err.println("Platform: LLFloater::closeFloater()")
    }
}

// =============================================================================
// Stub types specific to DAE export
// (Prefixed "Dae" to avoid clashing with stubs in other files)
// =============================================================================

/** Opaque handle for a Collada DOM element. */
interface DaeElement

enum class DaeTexGen { DEFAULT, PLANAR }

data class DaeColor4(val r: Float, val g: Float, val b: Float, val alpha: Float) {
    companion object { val WHITE = DaeColor4(1f, 1f, 1f, 1f) }
}

data class DaeVector2(val x: Float, val y: Float)

data class DaeVector3(val x: Float, val y: Float, val z: Float) {
    operator fun unaryMinus(): DaeVector3 = DaeVector3(-x, -y, -z)
    operator fun times(other: DaeVector3): DaeVector3 = DaeVector3(x * other.x, y * other.y, z * other.z)
    infix fun dot(other: DaeVector3): Float = x * other.x + y * other.y + z * other.z
    infix fun cross(other: DaeVector3): DaeVector3 = DaeVector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )
    companion object {
        val ZERO = DaeVector3(0f, 0f, 0f)
        val X_AXIS = DaeVector3(1f, 0f, 0f)
        val Y_AXIS = DaeVector3(0f, 1f, 0f)
    }
}

/** Stub: mirrors `LLTextureEntry` as used by DAESaver. */
class DaeTextureEntry {
    val textureId: LLUUID get() { System.err.println("Platform: te->getID()"); return LLUUID.NULL }
    val color: DaeColor4 get() { System.err.println("Platform: te->getColor()"); return DaeColor4.WHITE }
    val rotation: Float get() { System.err.println("Platform: te->getRotation()"); return 0f }
    val texGen: DaeTexGen get() { System.err.println("Platform: te->getTexGen()"); return DaeTexGen.DEFAULT }
    fun getScale(): Pair<Float, Float> { System.err.println("Platform: te->getScale(&repeatU, &repeatV)"); return Pair(0f, 0f) }
    fun getOffset(): Pair<Float, Float> { System.err.println("Platform: te->getOffset(&offsetU, &offsetV)"); return Pair(0f, 0f) }
}

/** Stub: viewer-object facets needed by DAESaver. */
class DaeViewerObject {
    val numVolumeFaces: Int get() { System.err.println("Platform: obj->getVolume()->getNumVolumeFaces()"); return 0 }
    val hasVolume: Boolean get() { System.err.println("Platform: obj->getVolume() != null"); return false }
    val renderPosition: DaeVector3 get() { System.err.println("Platform: obj->getRenderPosition()"); return DaeVector3.ZERO }
    val objectId: LLUUID get() { System.err.println("Platform: obj->getID()"); return LLUUID.NULL }
    fun getTextureEntry(face: Int): DaeTextureEntry { System.err.println("Platform: obj->getTE(face)"); return DaeTextureEntry() }
}

/** Stub: object selection handle used by ColladaExportFloater. */
class DaeObjectSelection {
    val firstRootNode: DaeSelectNode? get() { System.err.println("Platform: mObjectSelection->getFirstRootNode()"); return null }
    val firstRootObject: DaeViewerObject? get() { System.err.println("Platform: mObjectSelection->getFirstRootObject()"); return null }
    val primaryObject: DaeViewerObject? get() { System.err.println("Platform: object_selection->getPrimaryObject()"); return null }
    fun nodes(): Iterable<DaeSelectNode> { System.err.println("Platform: iterate mObjectSelection"); return emptyList() }
}

/** Stub: selection node as used by ColladaExportFloater. */
class DaeSelectNode {
    val nodeName: String get() { System.err.println("Platform: node->mName"); return "" }
    val isValid: Boolean get() { System.err.println("Platform: node->mValid"); return false }
    fun getObject(): DaeViewerObject? { System.err.println("Platform: node->getObject()"); return null }
}

/** Stub: selection manager. */
object DaeSelectMgr {
    val selection: DaeObjectSelection get() { System.err.println("Platform: LLSelectMgr::getInstance()->getSelection()"); return DaeObjectSelection() }
    val editSelection: DaeObjectSelection get() { System.err.println("Platform: LLSelectMgr::getInstance()->getEditSelection()"); return DaeObjectSelection() }
}

/** Stub: saved user settings accessor for DAE export settings. */
object DaeSavedSettings {
    fun getBool(key: String): Boolean { System.err.println("Platform: gSavedSettings.getBOOL(\"$key\")"); return false }
    fun getInt(key: String): Int { System.err.println("Platform: gSavedSettings.getS32(\"$key\")"); return 0 }
}
