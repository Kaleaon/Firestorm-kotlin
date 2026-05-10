package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
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

private val LL_TEXTURE_TRANSPARENT = LLUUID("8dcd4a48-2d37-4909-9f78-f7a9eb4ef903")
private val LL_TEXTURE_BLANK = LLUUID("5748decc-f629-461c-9a36-a35a221fe21f")

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
    val textureId: LLUUID = LLUUID.nullId(),
    val color: Color4 = Color4.WHITE,
    val name: String = "",
) {
    fun matchesFace(te: TextureEntry): Boolean =
        textureId == te.textureId && color == te.color
}

// =============================================================================
// DAESaver — builds the in-memory Collada DOM and writes the .dae file
// =============================================================================

/**
 * Accumulates a list of viewer objects and saves them as a Collada DAE file.
 *
 * Mirrors `DAESaver` from `daeexport.h/.cpp`.  All Collada DOM interactions
 * are stubbed with `TODO("GPU: …")` because the colladadom library does not
 * have a JVM counterpart.
 */
class DAESaver {

    val allMaterials: MutableList<MaterialInfo> = mutableListOf()
    val textures: MutableList<LLUUID> = mutableListOf()
    val textureNames: MutableList<String> = mutableListOf()
    val objects: MutableList<Pair<ViewerObject, String>> = mutableListOf()
    var offset: Vector3 = Vector3.ZERO
    var imageFormat: String = ImageFormatType.TGA.ext
    var totalNumMaterials: Int = 0

    fun add(prim: ViewerObject, name: String) {
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
                if (GridManager.isInSecondLife()) {
                    val imagep = TextureManager.getFetchedTexture(id)
                    val commentCreator = imagep?.comments?.get("a")
                    if (commentCreator != null && LLUUID(commentCreator) == Agent.id) {
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
                    val safeName = name.toString().replace(' ', '_').scrubFileName()
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
     *
     * The full Collada DOM build is stubbed — each distinct section is
     * annotated with what the C++ code does.
     */
    fun saveDAE(filename: String): Boolean {
        TODO("GPU: build Collada DOM — see daeexport.cpp DAESaver::saveDAE.\n" +
            "Steps:\n" +
            "  1. Escape filename to a URI.\n" +
            "  2. Create dae root element with asset/created/modified/unit/up_axis/contributor.\n" +
            "  3. For each object: add geometry source arrays (positions, normals, UVs),\n" +
            "     vertices node, polylist entries, and a scene node with the transform matrix.\n" +
            "  4. Call generateEffects() and generateImagesSection().\n" +
            "  5. Add library_materials entries.\n" +
            "  6. Invoke dae.writeAll().")
    }

    // -------------------------------------------------------------------------
    // Private helpers (stubs — require Collada DOM or equivalent)
    // -------------------------------------------------------------------------

    private fun transformTexCoord(
        numVert: Int,
        coord: Array<Vector2>,
        positions: Array<Vector3>,
        normals: Array<Vector3>,
        te: TextureEntry,
        scale: Vector3,
    ) {
        val cosAngle = cos(te.rotation)
        val sinAngle = sin(te.rotation)

        for (ii in 0 until numVert) {
            if (te.texGen == TexGen.PLANAR) {
                val normal = normals[ii]
                val pos = positions[ii]
                val d = normal dot Vector3.X_AXIS
                val binormal = when {
                    d >= 0.5f || d <= -0.5f ->
                        if (normal.x < 0f) -Vector3.Y_AXIS else Vector3.Y_AXIS
                    else ->
                        if (normal.y > 0f) -Vector3.X_AXIS else Vector3.X_AXIS
                }
                val tangent = binormal cross normal
                val scaledPos = pos * scale
                coord[ii] = Vector2(
                    x = 1f + (binormal.dot(scaledPos) * 2f - 0.5f),
                    y = -((tangent.dot(scaledPos)) * 2f - 0.5f),
                )
            }

            val (repeatU, repeatV) = te.getScale()
            val (offsetU, offsetV) = te.getOffset()
            val tX = coord[ii].x - 0.5f
            val tY = coord[ii].y - 0.5f

            coord[ii] = Vector2(
                x = (tX * cosAngle + tY * sinAngle) * repeatU + offsetU + 0.5f,
                y = (-tX * sinAngle + tY * cosAngle) * repeatV + offsetV + 0.5f,
            )
        }
    }

    private fun skipFace(te: TextureEntry): Boolean =
        SavedSettings.getBool("DAEExportSkipTransparent") &&
            (te.color.alpha < 0.01f || te.textureId == LL_TEXTURE_TRANSPARENT)

    private fun getMaterial(te: TextureEntry): MaterialInfo {
        if (SavedSettings.getBool("DAEExportConsolidateMaterials")) {
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

    private fun getMaterials(obj: ViewerObject): List<MaterialInfo> {
        val result = mutableListOf<MaterialInfo>()
        val consolidate = SavedSettings.getBool("DAEExportConsolidateMaterials")
        val numFaces = obj.numVolumeFaces
        for (faceNum in 0 until numFaces) {
            val te = obj.getTextureEntry(faceNum)
            if (skipFace(te)) continue
            val mat = getMaterial(te)
            if (!consolidate || mat !in result) result.add(mat)
        }
        return result
    }

    private fun getFacesWithMaterial(obj: ViewerObject, mat: MaterialInfo): List<Int> =
        (0 until obj.numVolumeFaces).filter { getMaterial(obj.getTextureEntry(it)) == mat }

    private fun generateEffects(effects: DaeElement) {
        TODO("GPU: iterate allMaterials; for each, add a profile_COMMON/technique/phong " +
            "element with colour or texture sampler reference")
    }

    private fun generateImagesSection(images: DaeElement) {
        TODO("GPU: iterate textureNames; for each non-empty name add an image/init_from element")
    }

    private fun addSource(mesh: DaeElement, srcId: String, params: String, vals: List<Float>) {
        TODO("GPU: add <source id='$srcId'><float_array> with vals; accessor stride=${params.length}")
    }

    private fun addPolygons(
        mesh: DaeElement,
        geomId: String,
        materialId: String,
        obj: ViewerObject,
        facesToInclude: List<Int>?,
    ) {
        TODO("GPU: build <polylist material='$materialId'> with VERTEX/NORMAL/TEXCOORD inputs " +
            "and per-triangle index and vcount arrays")
    }
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
    private var imageData: ByteArray? = null
    private var imageSize: Int = 0
    private var imageLocal: Boolean = false

    fun setData(data: ByteArray, datasize: Int, imagesize: Int, imageformat: Int, imagelocal: Boolean) {
        TODO("GPU: validate codec; append or set image data; store imagesize and imagelocal")
    }

    fun completed(success: Boolean) {
        TODO("GPU: on success decode J2C raw data; re-encode to TGA/PNG/J2C; save to disk at name")
    }

    companion object {
        /** Idle-callback worker that drains [ColladaExportFloater.texturesToSave] one entry at a time. */
        fun saveTexturesWorker(floater: ColladaExportFloater) {
            TODO("GPU: check mTexturesToSave; for head entry find fetched texture; " +
                "when discardLevel==0 read from cache via CacheReadResponder; " +
                "handle timeout via mTimer; call onTexturesSaved() when map is empty")
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
    private var currentObjectId: LLUUID = LLUUID.nullId()
    private var dirty: Boolean = true

    val texturesToSave: MutableMap<LLUUID, String> = mutableMapOf()

    private var objectSelection: ObjectSelection? = null
    private var texturePanel: Panel? = null

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    fun postBuild(): Boolean {
        texturePanel = getChild("textures_panel")
        TODO("Platform: bind ColladaExport.TextureExport callback; " +
            "bind export_btn to onClickExport; connect LLSelectMgr.mUpdateSignal to updateSelection")
        return true
    }

    fun draw() {
        if (dirty) {
            refresh()
            dirty = false
        }
        TODO("Platform: LLFloater::draw()")
    }

    fun onOpen(key: Any) {
        val selection = SelectMgr.selection
        if (selection.primaryObject == null) {
            closeFloater()
            return
        }
        objectSelection = SelectMgr.editSelection
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
        TODO("Platform: set child text fields for NameText, exportable_prims, exportable_textures; " +
            "set title; enable/disable export_textures_check and export_btn")
    }

    private fun updateTitleProgress() {
        TODO("Platform: format and set floater title with object name and remaining texture count")
    }

    // -------------------------------------------------------------------------
    // Selection handling
    // -------------------------------------------------------------------------

    fun updateSelection() {
        val selection = SelectMgr.selection
        val node = selection.firstRootNode
        if (node != null && !node.isValid && node.getObject()?.id == currentObjectId) return

        objectSelection = selection
        markDirty()
        refresh()
    }

    private fun addSelectedObjects() {
        total = 0; included = 0; numTextures = 0; numExportableTextures = 0
        saver.objects.clear(); saver.textures.clear(); saver.textureNames.clear()

        val sel = objectSelection ?: return
        val rootNode = sel.firstRootNode ?: run { objectName = ""; return }

        currentObjectId = rootNode.getObject()?.id ?: LLUUID.nullId()
        saver.offset = -(sel.firstRootObject?.renderPosition ?: Vector3.ZERO)
        objectName = rootNode.name

        for (node in sel.nodes()) {
            total++
            val obj = node.getObject() ?: continue
            if (!obj.hasVolume || !ExportPermsCheck.canExportNode(node, dae = true)) continue
            included++
            saver.add(obj, node.name)
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
        val showTexPanel = SavedSettings.getBool("DAEExportTextures") && numExportableTextures > 0
        TODO("Platform: set tex_layout_panel visible=$showTexPanel; " +
            "reshape to ${if (showTexPanel) EXPANDED_WIDTH else COLLAPSED_WIDTH}")
    }

    private fun addTexturePreview() {
        if (numExportableTextures == 0) return
        TODO("Platform: clear texturePanel children; for each exportable texture " +
            "create an LLTextureCtrl child positioned in a 2-column grid")
    }

    // -------------------------------------------------------------------------
    // Export flow
    // -------------------------------------------------------------------------

    private fun onClickExport() {
        TODO("Platform: LLFilePickerReplyThread.startPicker(FFSAVE_COLLADA, " +
            "scrubbed '$objectName.dae') → onExportFileSelected")
    }

    private fun onExportFileSelected(filenames: List<String>) {
        filename = filenames[0]
        if (SavedSettings.getBool("DAEExportTextures")) {
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
            SavedSettings.getInt("DAEExportTexturesFormat")
        ).ext
        updateTitleProgress()
        TODO("Platform: start mTimer; register CacheReadResponder.saveTexturesWorker as idle callback")
    }

    fun onTexturesSaved() {
        val success = saver.saveDAE(filename)
        if (success) {
            TODO("Platform: LLNotificationsUtil.add(\"ExportColladaSuccess\", object=$objectName, filename=$filename)")
        } else {
            TODO("Platform: LLNotificationsUtil.add(\"ExportColladaFailure\", object=$objectName, filename=$filename)")
        }
        closeFloater()
    }

    private fun closeFloater() {
        TODO("Platform: LLFloater::closeFloater()")
    }

    // -------------------------------------------------------------------------
    // Platform stubs
    // -------------------------------------------------------------------------

    private fun <T> getChild(name: String): T? =
        TODO("Platform: resolve child widget '$name'")
}

// =============================================================================
// Stub types specific to DAE export
// =============================================================================

/** Opaque handle for a Collada DOM element. */
interface DaeElement

enum class TexGen { DEFAULT, PLANAR }

data class Color4(val r: Float, val g: Float, val b: Float, val alpha: Float) {
    companion object { val WHITE = Color4(1f, 1f, 1f, 1f) }
}

data class Vector2(val x: Float, val y: Float)

data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun unaryMinus(): Vector3 = Vector3(-x, -y, -z)
    operator fun times(other: Vector3): Vector3 = Vector3(x * other.x, y * other.y, z * other.z)
    infix fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z
    infix fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )
    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val X_AXIS = Vector3(1f, 0f, 0f)
        val Y_AXIS = Vector3(0f, 1f, 0f)
    }
}

/** Stub: mirrors `LLTextureEntry`. */
class TextureEntry {
    val textureId: LLUUID get() = TODO("Platform: te->getID()")
    val color: Color4 get() = TODO("Platform: te->getColor()")
    val rotation: Float get() = TODO("Platform: te->getRotation()")
    val texGen: TexGen get() = TODO("Platform: te->getTexGen()")
    fun getScale(): Pair<Float, Float> = TODO("Platform: te->getScale(&repeatU, &repeatV)")
    fun getOffset(): Pair<Float, Float> = TODO("Platform: te->getOffset(&offsetU, &offsetV)")
}

/** Stub: extended ViewerObject facets needed by DAESaver. */
val ViewerObject.numVolumeFaces: Int get() = TODO("Platform: obj->getVolume()->getNumVolumeFaces()")
val ViewerObject.hasVolume: Boolean get() = TODO("Platform: obj->getVolume() != null")
val ViewerObject.renderPosition: Vector3 get() = TODO("Platform: obj->getRenderPosition()")
val ViewerObject.id: LLUUID get() = TODO("Platform: obj->getID()")
fun ViewerObject.getTextureEntry(face: Int): TextureEntry = TODO("Platform: obj->getTE(face)")

/** Stub: object selection handle. */
class ObjectSelection {
    val firstRootNode: SelectNode? get() = TODO("Platform: mObjectSelection->getFirstRootNode()")
    val firstRootObject: ViewerObject? get() = TODO("Platform: mObjectSelection->getFirstRootObject()")
    val primaryObject: ViewerObject? get() = TODO("Platform: object_selection->getPrimaryObject()")
    fun nodes(): Iterable<SelectNode> = TODO("Platform: iterate mObjectSelection")
}

/** Stub: selection manager. */
object SelectMgr {
    val selection: ObjectSelection get() = TODO("Platform: LLSelectMgr::getInstance()->getSelection()")
    val editSelection: ObjectSelection get() = TODO("Platform: LLSelectMgr::getInstance()->getEditSelection()")
}

/** Stub: a UI panel widget reference. */
class Panel

/** Stub: saved user settings accessor. */
object SavedSettings {
    fun getBool(key: String): Boolean = TODO("Platform: gSavedSettings.getBOOL(\"$key\")")
    fun getInt(key: String): Int = TODO("Platform: gSavedSettings.getS32(\"$key\")")
}

/** Stub: SelectNode extended property used by the floater. */
val SelectNode.name: String get() = TODO("Platform: node->mName")
val SelectNode.isValid: Boolean get() = TODO("Platform: node->mValid")

private fun String.scrubFileName(): String = TODO("Platform: gDirUtilp->getScrubbedFileName(this)")
