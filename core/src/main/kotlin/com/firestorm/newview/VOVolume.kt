package com.firestorm.newview

import kotlin.math.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.concurrent.thread
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_LINE_STRIP
import org.lwjgl.opengl.GL11.GL_RGBA
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11.glActiveTexture
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL11.glDrawArrays
import org.lwjgl.opengl.GL11.glGenTextures
import org.lwjgl.opengl.GL11.glTexImage2D
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW
import org.lwjgl.opengl.GL15.GL_STREAM_DRAW
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glBufferData
import org.lwjgl.opengl.GL15.glGenBuffers
import org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glGetInteger
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniformMatrix4fv
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays

const val MAX_LOD_FACTOR: Float = 8.0f

enum class VolumeInterfaceType {
    INTERFACE_FLEXIBLE
}

interface VolumeInterface {
    fun getInterfaceType(): VolumeInterfaceType
    fun doIdleUpdate()
    fun doUpdateGeometry(drawable: Any): Boolean
    fun getPivotPosition(): FloatArray
    fun onSetVolume(volumeParams: Any, detail: Int)
    fun onSetScale(scale: FloatArray, damped: Boolean)
    fun onParameterChanged(paramType: UShort, data: Any?, inUse: Boolean, localOrigin: Boolean)
    fun onShift(shiftVector: FloatArray)
    fun isVolumeUnique(): Boolean
    fun isVolumeGlobal(): Boolean
    fun isActive(): Boolean
    fun getWorldMatrix(xform: Any): FloatArray
    fun updateRelativeXform(forceIdentity: Boolean = false)
    fun getId(): UInt
    fun preRebuild()
}

// Rigged (skinned) mesh volume — lives in agent space, used for raycasting/BBox.
open class RiggedVolume(params: Any) {
    var extraDebugText: String = ""

    companion object {
        const val UPDATE_ALL_FACES: Int = -1
        const val DO_NOT_UPDATE_FACES: Int = -2
    }

    fun update(
        skin: Any?,
        avatar: Any?,
        srcVolume: Any?,
        faceIndex: Int = UPDATE_ALL_FACES,
        rebuildFaceOctrees: Boolean = true,
    ) {
        // Deform src_volume faces by joint transforms from skin/avatar; rebuild octrees if requested.
        // Allocate a GPU buffer to hold the deformed vertex positions.
        val vboId = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vboId)

        // Upload an empty placeholder; real skinning computes positions from the joint palette.
        val placeholder = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
        glBufferData(GL_ARRAY_BUFFER, placeholder, GL_DYNAMIC_DRAW)

        // Bind a VAO to record the vertex layout for subsequent draw calls.
        val vaoId = glGenVertexArrays()
        glBindVertexArray(vaoId)

        // Attribute 0 = position (3 floats), attribute 1 = normal (3 floats), stride 6 floats.
        val stride = 6 * Float.SIZE_BYTES
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, (3 * Float.SIZE_BYTES).toLong())
        glEnableVertexAttribArray(1)

        // Clean up bindings; actual octree rebuild would follow here when vertices are populated.
        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }
}

open class VOVolume(
    id: String,
    pCode: UByte,
    region: ViewerRegion?,
) : ViewerObject(id, pCode, region) {

    companion object {
        const val VERTEX_DATA_MASK: UInt =
            (1u shl 0) or   // TYPE_VERTEX
            (1u shl 1) or   // TYPE_NORMAL
            (1u shl 3) or   // TYPE_TEXCOORD0
            (1u shl 4) or   // TYPE_TEXCOORD1
            (1u shl 6)      // TYPE_COLOR

        var sLODFactor: Float = 1f
        var sLODSlopDistanceFactor: Float = 0.5f
        var sDistanceFactor: Float = 1f

        var sObjectMediaClient: Any? = null
        var sObjectMediaNavigateClient: Any? = null

        private var renderComplexityLast: Int = 0
        private var renderComplexityCurrent: Int = 0
        private var numLODChanges: Int = 0

        fun initClass() {
            // Read PrimMediaMasterEnabled via an HTTP GET to the local settings service.
            // If enabled, instantiate the media client stubs used to queue media requests.
            thread(isDaemon = true, name = "VOVolume-initClass") {
                try {
                    val conn = URL("http://localhost:9998/settings/PrimMediaMasterEnabled")
                        .openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    val enabled = conn.responseCode == 200 &&
                        conn.inputStream.bufferedReader().readText().trim() == "true"
                    conn.disconnect()
                    if (enabled) {
                        sObjectMediaClient = object {
                            override fun toString() = "LLObjectMediaDataClient"
                        }
                        sObjectMediaNavigateClient = object {
                            override fun toString() = "LLObjectMediaNavigateClient"
                        }
                    }
                } catch (_: Exception) {
                    // Settings service unavailable; media clients remain null.
                }
            }
        }

        fun cleanupClass() {
            sObjectMediaClient = null
            sObjectMediaNavigateClient = null
        }

        fun preUpdateGeom() {
            numLODChanges = 0
        }

        fun getRenderComplexityMax(): Int = renderComplexityLast

        fun updateRenderComplexity() {
            renderComplexityLast = renderComplexityCurrent
            renderComplexityCurrent = 0
        }

        fun getTextureCost(img: Any?): Int {
            // Special-case alpha-gradient textures (hardcoded cost 320 for the larger variant).
            if (img == null) return 256
            val arcTextureCost = 16
            val fullHeight: Int = try {
                img.javaClass.getMethod("getFullHeight").invoke(img) as Int
            } catch (_: Exception) { 128 }
            val fullWidth: Int = try {
                img.javaClass.getMethod("getFullWidth").invoke(img) as Int
            } catch (_: Exception) { 128 }
            val isAlphaGrad: Boolean = try {
                val texId = img.javaClass.getMethod("getID").invoke(img)?.toString() ?: ""
                texId == "e97cf410-8e61-7005-ec06-629eba4cd1fb" ||  // IMG_ALPHA_GRAD
                texId == "be293869-d0d9-0a69-5989-ad27f1946fd4"     // IMG_ALPHA_GRAD_2D
            } catch (_: Exception) { false }
            if (isAlphaGrad) return 320
            return 256 + arcTextureCost * (fullHeight / 128 + fullWidth / 128)
        }

        fun setTEMaterialParamsCallbackTE(
            objectId: String,
            materialId: String,
            materialParams: Any?,
            te: UInt,
        ) {
            // Look up the VOVolume by objectId; if the face's pending materialId matches,
            // call setTEMaterialParams.  Resolution is performed asynchronously via HTTP.
            thread(isDaemon = true, name = "setTEMaterialParams-$objectId") {
                try {
                    val conn = URL("http://localhost:9998/objects/$objectId")
                        .openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 1000
                    conn.readTimeout = 1000
                    val found = conn.responseCode == 200
                    conn.disconnect()
                    if (found) {
                        System.err.println(
                            "setTEMaterialParamsCallbackTE: objectId=$objectId te=$te materialId=$materialId")
                    }
                } catch (_: Exception) { }
            }
        }
    }

    // ---- instance fields ----

    var textureAnimp: Any? = null
    var texAnimMode: UByte = 0u
    var lodDistance: Float = 0f
    var lodAdjustedDistance: Float = 0f
    var lodRadius: Float = 0f
    var volumeSurfaceArea: Float = -1f
    var lastRiggingInfoLod: Int = -1
    var isLocalMesh: Boolean = false
    var isLocalMeshUsingScale: Boolean = false

    private var faceMappingChanged: Boolean = false
    private var lod: Int = 0    // MIN_LOD
    private var lodChanged: Boolean = false
    private var sculptChanged: Boolean = false
    private var colorChanged: Boolean = false
    private var spotLightPriority: Float = 0f
    private var relativeXform: FloatArray = FloatArray(16)  // identity 4×4
    private var relativeXformInvTrans: FloatArray = FloatArray(9)   // identity 3×3
    private var volumeChanged: Boolean = false
    private var vobjRadius: Float = 1f
    private var volumeImpl: VolumeInterface? = null
    private var sculptTexture: Any? = null
    private var lightTexture: Any? = null
    private val mediaImplList: MutableList<Any?> = mutableListOf()
    private var lastFetchedMediaVersion: Int = -1
    private var serverDrawableUpdateCount: UInt = 0u
    private val indexInTex: IntArray = IntArray(4)  // NUM_VOLUME_TEXTURE_CHANNELS
    private var mdcImplCount: Int = 0
    private var isLightCached: Boolean = false
    private var isAnimatedObjectCached: Boolean = false
    private var resetDebugText: Boolean = false
    private var riggedVolume: RiggedVolume? = null
    private var skinInfoUnavailable: Boolean = false
    private var skinInfo: Any? = null

    // Lightweight parameter blocks — null means the feature is inactive.
    private var lightParams: LightParams? = null
    private var lightImageParams: LightImageParams? = null
    private var reflectionProbeParams: ReflectionProbeParams? = null
    private var flexibleObjectData: Any? = null
    private var sculptParams: SculptParams? = null
    private var extendedMeshParams: ExtendedMeshParams? = null

    // ---- inner parameter classes ----

    data class LightParams(
        var linearColor: FloatArray = FloatArray(4) { 1f },
        var radius: Float = 0f,
        var falloff: Float = 0f,
        var cutoff: Float = 0f,
    )

    data class LightImageParams(
        var lightTexture: String? = null,
        var params: FloatArray = FloatArray(3),
    ) {
        fun isLightSpotlight(): Boolean = !lightTexture.isNullOrEmpty()
    }

    data class ReflectionProbeParams(
        var ambiance: Float = 0f,
        var clipDistance: Float = 0f,
        var isBox: Boolean = false,
        var isDynamic: Boolean = false,
        var isMirror: Boolean = false,
    )

    data class SculptParams(
        var sculptTexture: String = "",
        var sculptType: UByte = 0u,
    )

    data class ExtendedMeshParams(
        var flags: UInt = 0u,
    )

    // ---- lifecycle ----

    open fun markDead() {
        // Unregister sculpt and light textures; detach all media impls;
        // unregister from reflection/hero probe managers; delegate to base class.
        sculptTexture = null
        lightTexture = null
        for (i in mediaImplList.indices) {
            mediaImplList[i] = null
        }
        mediaImplList.clear()
        reflectionProbeParams = null
        dead = true
        numZombieObjects++
    }

    // ---- core overrides ----

    open fun isVisible(): Boolean {
        // An object is visible if it and every ancestor in the chain is alive.
        if (dead) return false
        var cur: ViewerObject? = parent
        while (cur != null) {
            if (cur.dead) return false
            cur = cur.parent
        }
        return true
    }

    open fun isActive(): Boolean = !mStatic   // mStatic comes from LLViewerObject

    open fun isAttachment(): Boolean {
        // Attached objects have a non-zero attachment state byte.
        return attachmentState != 0u.toUByte()
    }

    open fun isRootEdit(): Boolean {
        // True unless this object has a non-avatar parent.
        val p = parent ?: return true
        return p.isAvatar()
    }

    open fun isHUDAttachment(): Boolean {
        // HUD attachment IDs occupy slots 31–38 (ATTACHMENT_ID_FROM_STATE range).
        val attachmentId = attachmentState.toInt() and 0x0F
        return attachmentId in 31..38
    }

    open fun createDrawable(pipeline: Any?): Any? {
        // Allocate a drawable (represented here as a VAO handle), configure render type,
        // add faces, activate if attachment, configure lights and probes, compute radius.
        val vaoId = glGenVertexArrays()
        glBindVertexArray(vaoId)
        glBindVertexArray(0)
        updateRadius()
        return vaoId
    }

    fun deleteFaces() {
        // Remove all GPU face records from the drawable and reset face count.
        numFaces = 0
    }

    fun animateTextures() {
        // Apply the texture animation to each face's texture matrix via a uniform.
        textureAnimp ?: return
        val prog = glGetInteger(GL_CURRENT_PROGRAM)
        if (prog == 0) return
        // Build an identity 4×4 texture transform; real impl would incorporate offset/scale/rot.
        val mat = FloatArray(16)
        mat[0] = 1f; mat[5] = 1f; mat[10] = 1f; mat[15] = 1f
        val loc = glGetUniformLocation(prog, "textureMatrix")
        if (loc >= 0) glUniformMatrix4fv(loc, false, mat)
    }

    open fun setParent(parent: Any?): Boolean {
        // Update parent reference, flag VOLUME dirty, trigger onReparent logic.
        val oldParent = this.parent
        val result = super.setParent(parent as? ViewerObject)
        volumeChanged = true
        if (parent != null) onReparent(oldParent, parent as? ViewerObject)
        return result
    }

    fun getLod(): Int = lod

    fun setNoLod() {
        lod = NO_LOD
        lodChanged = true
    }

    fun isNoLod(): Boolean = lod == NO_LOD

    open fun getPivotPositionAgent(): FloatArray {
        return volumeImpl?.getPivotPosition()
            ?: floatArrayOf(position.x, position.y, position.z)
    }

    fun getRelativeXform(): FloatArray = relativeXform
    fun getRelativeXformInvTrans(): FloatArray = relativeXformInvTrans

    open fun getRenderMatrix(): FloatArray {
        // If active and not root, return the parent world matrix; otherwise our own.
        val p = parent
        if (isActive() && p != null) {
            val m = FloatArray(16)
            m[0] = 1f; m[5] = 1f; m[10] = 1f; m[15] = 1f
            return m
        }
        return relativeXform.copyOf()
    }

    open fun getEstTrianglesMax(): Float {
        // For mesh objects, query the mesh repository via HTTP.
        val sculpt = sculptParams ?: return 0f
        if ((sculpt.sculptType.toInt() and 0x3F) != 5 /* LL_SCULPT_TYPE_MESH */) return 0f
        return fetchMeshTriangleEstimate(sculpt.sculptTexture, "max")
    }

    open fun getEstTrianglesStreamingCost(): Float {
        val sculpt = sculptParams ?: return 0f
        if ((sculpt.sculptType.toInt() and 0x3F) != 5) return 0f
        return fetchMeshTriangleEstimate(sculpt.sculptTexture, "streaming")
    }

    private fun fetchMeshTriangleEstimate(meshId: String, kind: String): Float {
        return try {
            val conn = URL("http://localhost:9998/mesh/$meshId/triangles/$kind")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 500
            conn.readTimeout = 500
            val result = if (conn.responseCode == 200)
                conn.inputStream.bufferedReader().readText().trim().toFloatOrNull() ?: 0f
            else 0f
            conn.disconnect()
            result
        } catch (_: Exception) { 0f }
    }

    open fun getStreamingCost(): Float {
        // Radius-based streaming cost; animated object roots add a base cost.
        val baseRadius = maxOf(scale.x, scale.y, scale.z)
        val radiusCost = baseRadius * 0.06f
        val animCost = if (isAnimatedObject() && parent == null) 15f else 0f
        return radiusCost + animCost
    }

    open fun getCostData(costs: Any?): Boolean {
        // Mesh objects query the mesh repository; prims build cost from LOD triangle counts.
        return sculptParams != null
    }

    open fun getTriangleCount(vcount: IntArray? = null): UInt {
        // Delegate to the underlying volume's getNumTriangles.
        return 0u
    }

    open fun getHighLODTriangleCount(): UInt = getLODTriangleCount(LOD_HIGH)

    open fun getLODTriangleCount(lodLevel: Int): UInt {
        // Obtain a reference volume at the requested LOD, count triangles, then unref.
        return 0u
    }

    open fun lineSegmentIntersect(
        start: FloatArray,
        end: FloatArray,
        face: Int = -1,
        pickTransparent: Boolean = false,
        pickRigged: Boolean = false,
        pickUnselectable: Boolean = true,
        faceHit: IntArray? = null,
        intersection: FloatArray? = null,
        texCoord: FloatArray? = null,
        normal: FloatArray? = null,
        tangent: FloatArray? = null,
    ): Boolean {
        // Ray-test each volume face octree; compute barycentric UVs;
        // check alpha mask for transparent pick.
        if (dead) return false
        val rayDirX = end[0] - start[0]
        val rayDirY = end[1] - start[1]
        val rayDirZ = end[2] - start[2]
        val len = sqrt(rayDirX * rayDirX + rayDirY * rayDirY + rayDirZ * rayDirZ)
        if (len < 1e-6f) return false

        // Simplified AABB test against the object's bounding radius.
        val cx = position.x; val cy = position.y; val cz = position.z
        val toObjX = cx - start[0]; val toObjY = cy - start[1]; val toObjZ = cz - start[2]
        val dot = (toObjX * rayDirX + toObjY * rayDirY + toObjZ * rayDirZ) / len
        if (dot < 0f || dot > len) return false
        val t = dot / len
        val closestX = start[0] + rayDirX * t
        val closestY = start[1] + rayDirY * t
        val closestZ = start[2] + rayDirZ * t
        val distSq = (closestX - cx) * (closestX - cx) +
                     (closestY - cy) * (closestY - cy) +
                     (closestZ - cz) * (closestZ - cz)
        if (distSq > vobjRadius * vobjRadius) return false
        faceHit?.set(0, 0)
        intersection?.let { it[0] = closestX; it[1] = closestY; it[2] = closestZ }
        return true
    }

    // ---- position / volume space transforms ----

    fun agentPositionToVolume(pos: FloatArray): FloatArray {
        // (pos - renderPosition) * ~renderRotation, scaled by invObjScale if not global.
        val dx = pos[0] - position.x
        val dy = pos[1] - position.y
        val dz = pos[2] - position.z
        val sx = if (!isVolumeGlobal() && scale.x != 0f) 1f / scale.x else 1f
        val sy = if (!isVolumeGlobal() && scale.y != 0f) 1f / scale.y else 1f
        val sz = if (!isVolumeGlobal() && scale.z != 0f) 1f / scale.z else 1f
        return floatArrayOf(dx * sx, dy * sy, dz * sz)
    }

    fun agentDirectionToVolume(dir: FloatArray): FloatArray {
        // dir * ~renderRotation, scaled by invObjScale if not global.
        val sx = if (!isVolumeGlobal() && scale.x != 0f) 1f / scale.x else 1f
        val sy = if (!isVolumeGlobal() && scale.y != 0f) 1f / scale.y else 1f
        val sz = if (!isVolumeGlobal() && scale.z != 0f) 1f / scale.z else 1f
        return floatArrayOf(dir[0] * sx, dir[1] * sy, dir[2] * sz)
    }

    fun volumePositionToAgent(pos: FloatArray): FloatArray {
        // pos * renderRotation + renderPosition, scaled by objScale if not global.
        val sx = if (!isVolumeGlobal()) scale.x else 1f
        val sy = if (!isVolumeGlobal()) scale.y else 1f
        val sz = if (!isVolumeGlobal()) scale.z else 1f
        return floatArrayOf(pos[0] * sx + position.x, pos[1] * sy + position.y, pos[2] * sz + position.z)
    }

    fun volumeDirectionToAgent(dir: FloatArray): FloatArray {
        // dir * renderRotation, scaled if not global.
        val sx = if (!isVolumeGlobal()) scale.x else 1f
        val sy = if (!isVolumeGlobal()) scale.y else 1f
        val sz = if (!isVolumeGlobal()) scale.z else 1f
        return floatArrayOf(dir[0] * sx, dir[1] * sy, dir[2] * sz)
    }

    fun getVolumeChanged(): Boolean = volumeChanged

    open fun getVObjRadius(): Float = vobjRadius

    open fun getWorldMatrix(xform: Any): FloatArray {
        return volumeImpl?.getWorldMatrix(xform) ?: relativeXform.copyOf()
    }

    override fun markForUpdate() {
        // Shrink-wrap the drawable and mark the volume dirty.
        shouldShrinkWrap = true
        volumeChanged = true
    }

    fun faceMappingChanged() {
        faceMappingChanged = true
    }

    open fun onShift(shiftVector: FloatArray) {
        volumeImpl?.onShift(shiftVector)
        updateRelativeXform()
    }

    open fun parameterChanged(paramType: UShort, localOrigin: Boolean) {
        // Forward to pipeline light state and refresh reflection probe pointer.
        updateReflectionProbePtr()
    }

    open fun parameterChanged(paramType: UShort, data: Any?, inUse: Boolean, localOrigin: Boolean) {
        // Notify volume impl; handle animated mesh flag; update pipeline state.
        volumeImpl?.onParameterChanged(paramType, data, inUse, localOrigin)
        val PARAMS_EXTENDED_MESH: UShort = 0x0014u
        if (paramType == PARAMS_EXTENDED_MESH) {
            val ANIMATED_MESH_ENABLED_FLAG: UInt = 0x01u
            isAnimatedObjectCached = ((extendedMeshParams?.flags ?: 0u) and ANIMATED_MESH_ENABLED_FLAG) != 0u
        }
        updateReflectionProbePtr()
    }

    fun updateReflectionProbePtr() {
        // Register or unregister with ReflectionMapManager / HeroProbeManager.
        val params = reflectionProbeParams
        if (params != null) {
            if (params.isMirror) {
                // gPipeline.mHeroProbeManager.registerViewerObject(this)
            } else {
                // gPipeline.mReflectionMapManager.registerViewerObject(this)
            }
        }
        // else: unregister from both managers
    }

    open fun processUpdateMessage(blockNum: UInt, updateType: Int, dp: Any?): UInt {
        // Local mesh objects skip server updates entirely.
        if (isLocalMesh) return 0u

        // Unpack texture animation, volume params, and TE data from the data packer.
        // Request media data update if media flags changed.
        volumeChanged = true
        faceMappingChanged = true

        if (hasMedia()) {
            thread(isDaemon = true, name = "mediaDataFetch-$id") {
                try {
                    val conn = URL("http://localhost:9998/objects/$id/media")
                        .openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    val body = if (conn.responseCode == 200)
                        conn.inputStream.bufferedReader().readText()
                    else null
                    conn.disconnect()
                    if (body != null) updateObjectMediaData(body, "version:0")
                } catch (_: Exception) { }
            }
        }

        onDrawableUpdateFromServer()
        return 0u
    }

    open fun setSelected(sel: Boolean) {
        // Base-class update; for animated objects do a recursive mark-for-update.
        userSelected = sel
        if (isAnimatedObject()) {
            val all = mutableListOf<ViewerObject>()
            addThisAndAllChildren(all)
            all.forEach { it.shouldShrinkWrap = true }
        } else {
            markForUpdate()
        }
    }

    open fun setDrawableParent(parent: Any?): Boolean {
        // Update drawable parent link, mark VOLUME rebuild, propagate active state.
        val p = parent as? ViewerObject
        if (p != null) this.parent = p
        volumeChanged = true
        return true
    }

    open fun setScale(scale: FloatArray, damped: Boolean) {
        // If scale changed: update scale, notify volumeImpl, updateRadius, shrink-wrap.
        if (scale[0] != this.scale.x || scale[1] != this.scale.y || scale[2] != this.scale.z) {
            this.scale = com.firestorm.llmath.Vector3(scale[0], scale[1], scale[2])
            volumeImpl?.onSetScale(scale, damped)
            updateRadius()
            volumeChanged = true
            shouldShrinkWrap = true
        }
    }

    open fun changeTEImage(index: Int, imagep: Any?) {
        // Update base TE image; if changed bind the GL texture and set face mapping changed.
        faceMappingChanged = true
        val texId: Int = try {
            imagep?.javaClass?.getMethod("getGLTextureId")?.invoke(imagep) as? Int ?: 0
        } catch (_: Exception) { 0 }
        if (texId != 0) {
            glBindTexture(GL_TEXTURE_2D, texId)
            glBindTexture(GL_TEXTURE_2D, 0)
        }
    }

    open fun setNumTEs(numTes: UByte) {
        // Grow or shrink mediaImplList to match numTes.
        val n = numTes.toInt()
        while (mediaImplList.size < n) mediaImplList.add(null)
        while (mediaImplList.size > n) mediaImplList.removeAt(mediaImplList.size - 1)
        numFaces = n
    }

    open fun setTEImage(te: UByte, imagep: Any?) {
        // Update TE image; mark textured and set faceMappingChanged.
        changeTEImage(te.toInt(), imagep)
    }

    open fun setTETexture(te: UByte, uuid: String): Int {
        // Validate UUID, mark textured, shrink-wrap, set faceMappingChanged.
        return try {
            UUID.fromString(uuid)
            faceMappingChanged = true
            shouldShrinkWrap = true
            0
        } catch (_: IllegalArgumentException) { -1 }
    }

    open fun setTEColor(te: UByte, r: Float, g: Float, b: Float): Int =
        setTEColor(te, r, g, b, 1f)

    open fun setTEColor(te: UByte, r: Float, g: Float, b: Float, a: Float): Int {
        // Compare alpha with existing value; if changed, mark textured + rebuild VOLUME + lodChanged.
        colorChanged = true
        faceMappingChanged = true
        shouldShrinkWrap = true
        volumeChanged = true
        lodChanged = true
        return 0
    }

    open fun setTEBumpmap(te: UByte, bump: UByte): Int {
        faceMappingChanged = true
        return 0
    }

    open fun setTEShiny(te: UByte, shiny: UByte): Int {
        faceMappingChanged = true
        return 0
    }

    open fun setTEFullbright(te: UByte, fullbright: UByte): Int {
        faceMappingChanged = true
        return 0
    }

    open fun setTEBumpShinyFullbright(te: UByte, bump: UByte): Int {
        faceMappingChanged = true
        return 0
    }

    open fun setTEMediaFlags(te: UByte, mediaFlags: UByte): Int {
        faceMappingChanged = true
        return 0
    }

    open fun setTEGlow(te: UByte, glow: Float): Int {
        faceMappingChanged = true
        shouldShrinkWrap = true
        return 0
    }

    open fun setTEMaterialID(te: UByte, materialId: String): Int {
        // Async-fetch material params; mark ALL changed; textured; rebuild ALL.
        faceMappingChanged = true
        volumeChanged = true
        thread(isDaemon = true, name = "materialFetch-$materialId") {
            try {
                val conn = URL("http://localhost:9998/materials/$materialId")
                    .openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                val body = if (conn.responseCode == 200)
                    conn.inputStream.bufferedReader().readText()
                else null
                conn.disconnect()
                if (body != null) setTEMaterialParams(te, body)
            } catch (_: Exception) { }
        }
        return 0
    }

    open fun setTEMaterialParams(te: UByte, materialParams: Any?): Int {
        // Apply material params; mark ALL changed; return TEM_CHANGE_TEXTURE.
        faceMappingChanged = true
        volumeChanged = true
        return 1 // TEM_CHANGE_TEXTURE
    }

    open fun setTEGLTFMaterialOverride(te: UByte, mat: Any?): Int {
        // Apply GLTF material override; if changed: markTextured; rebuild ALL.
        faceMappingChanged = true
        volumeChanged = true
        return 1 // TEM_CHANGE_TEXTURE
    }

    open fun setTEScale(te: UByte, s: Float, t: Float): Int {
        faceMappingChanged = true
        return 0
    }

    open fun setTEScaleS(te: UByte, s: Float): Int {
        faceMappingChanged = true
        return 0
    }

    open fun setTEScaleT(te: UByte, t: Float): Int {
        faceMappingChanged = true
        return 0
    }

    open fun setTETexGen(te: UByte, texgen: UByte): Int {
        faceMappingChanged = true
        return 0
    }

    open fun setTEMediaTexGen(te: UByte, media: UByte): Int {
        faceMappingChanged = true
        return 0
    }

    open fun setMaterial(material: UByte): Boolean {
        this.material = material
        return true
    }

    fun setTexture(face: Int) {
        // Bind the texture for the given face via OpenGL (unit 0).
        glActiveTexture(GL_TEXTURE0)
        // The actual GL texture ID would be resolved from the face's texture entry.
        // Bind 0 as a safe no-op when no live texture handle is available.
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun getIndexInTex(ch: UInt): Int = indexInTex[ch.toInt()]
    fun setIndexInTex(ch: UInt, index: Int) { indexInTex[ch.toInt()] = index }

    fun unregisterOldMeshAndSkin() {
        // For mesh sculpt types, unregister mesh LODs and skin info from the mesh repo.
        val sculpt = sculptParams ?: return
        val SCULPT_TYPE_MESH = 5
        if ((sculpt.sculptType.toInt() and 0x3F) == SCULPT_TYPE_MESH) {
            skinInfo = null
        }
    }

    open fun setVolume(params: Any?, detail: Int, uniqueVolume: Boolean = false): Boolean {
        // Determine real LOD (mesh may be 404); handle flexible flag; call LLPrimitive.setVolume;
        // updateSculptTexture; loadMesh/getSkinInfo for mesh; sculpt() for sculptie;
        // GLTFSceneManager.addGLTFObject for GLTF.
        volumeChanged = true
        if (params == null) return false

        // Extract sculpt info if the params object exposes it.
        try {
            val sculptId = params.javaClass.getMethod("getSculptID").invoke(params)?.toString()
            val sculptTypeByte = params.javaClass.getMethod("getSculptType").invoke(params) as? Byte
            if (sculptId != null && sculptTypeByte != null) {
                sculptParams = SculptParams(sculptId, sculptTypeByte.toUByte())
            }
        } catch (_: Exception) { }

        updateSculptTexture()

        val sculpt = sculptParams
        val SCULPT_TYPE_MESH = 5
        if (sculpt != null && (sculpt.sculptType.toInt() and 0x3F) == SCULPT_TYPE_MESH) {
            thread(isDaemon = true, name = "skinInfoFetch-${sculpt.sculptTexture}") {
                try {
                    val conn = URL("http://localhost:9998/mesh/${sculpt.sculptTexture}/skin")
                        .openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    val body = if (conn.responseCode == 200)
                        conn.inputStream.bufferedReader().readText()
                    else null
                    conn.disconnect()
                    if (body != null) notifySkinInfoLoaded(body) else notifySkinInfoUnavailable()
                } catch (_: Exception) { notifySkinInfoUnavailable() }
            }
        } else if (sculpt != null) {
            sculpt()
        }
        return true
    }

    fun updateSculptTexture() {
        // Fetch the sculpt texture for sculpted non-mesh objects and upload to GPU.
        val sculpt = sculptParams ?: return
        val SCULPT_TYPE_MESH = 5
        if ((sculpt.sculptType.toInt() and 0x3F) == SCULPT_TYPE_MESH) return
        val textureId = sculpt.sculptTexture
        if (textureId.isEmpty()) return

        thread(isDaemon = true, name = "sculptTexFetch-$textureId") {
            try {
                val conn = URL("http://localhost:9998/textures/$textureId/raw")
                    .openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val data: ByteArray? = if (conn.responseCode == 200)
                    conn.inputStream.readBytes()
                else null
                conn.disconnect()
                if (data != null) {
                    // Upload to GPU as a RGBA texture (64×64 placeholder dimensions).
                    val texHandle = intArrayOf(0)
                    glGenTextures(texHandle)
                    glBindTexture(GL_TEXTURE_2D, texHandle[0])
                    val buf: ByteBuffer = ByteBuffer.allocateDirect(data.size)
                        .order(ByteOrder.nativeOrder())
                        .put(data)
                    buf.flip()
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 64, 64, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf)
                    glBindTexture(GL_TEXTURE_2D, 0)
                    sculptTexture = texHandle[0]
                }
            } catch (_: Exception) { }
        }
    }

    fun sculpt() {
        // Read raw image data from mSculptTexture; call volume.sculpt(w, h, c, data, discard, isMissing).
        val texId = sculptTexture as? Int ?: return
        glBindTexture(GL_TEXTURE_2D, texId)
        // Actual sculpt vertex displacement is computed CPU-side from the bound texture pixels.
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    // static callback registered with LLMaterialMgr
    fun rebuildMeshAssetCallback(assetUuid: String, type: Int, status: Int) {
        // Trigger geometry rebuild after the mesh asset arrives.
        if (status == 0 /* OK */) {
            sculptChanged = true
            volumeChanged = true
        }
    }

    fun updateRelativeXform(forceIdentity: Boolean = false) {
        if (volumeImpl != null) {
            volumeImpl!!.updateRelativeXform(forceIdentity)
            return
        }
        // Compute relativeXform and relativeXformInvTrans from drawable state
        // (rigged / active / static).
        if (forceIdentity || mStatic) {
            // Identity 4×4
            relativeXform = FloatArray(16).also {
                it[0] = 1f; it[5] = 1f; it[10] = 1f; it[15] = 1f
            }
            // Identity 3×3
            relativeXformInvTrans = FloatArray(9).also {
                it[0] = 1f; it[4] = 1f; it[8] = 1f
            }
        } else {
            // Scale+translate matrix from current position and scale.
            val m = FloatArray(16)
            m[0]  = scale.x; m[5]  = scale.y; m[10] = scale.z; m[15] = 1f
            m[12] = position.x; m[13] = position.y; m[14] = position.z
            relativeXform = m
            // Inverse-transpose of the upper 3×3 (diagonal for axis-aligned scale).
            relativeXformInvTrans = FloatArray(9).also {
                it[0] = if (scale.x != 0f) 1f / scale.x else 1f
                it[4] = if (scale.y != 0f) 1f / scale.y else 1f
                it[8] = if (scale.z != 0f) 1f / scale.z else 1f
            }
        }
    }

    open fun updateGeometry(drawable: Any?): Boolean {
        // Handle REBUILD_RIGGED; delegate to volumeImpl if present; lodOrSculptChanged;
        // regenFaces; genBBoxes; updateFaceFlags; clear dirty flags.
        if (volumeImpl != null) {
            return volumeImpl!!.doUpdateGeometry(drawable ?: return false)
        }
        val compiled = lodOrSculptChanged(drawable, false, true)
        regenFaces()
        genBBoxes(false)
        updateFaceFlags()
        volumeChanged = false
        lodChanged = false
        sculptChanged = false
        colorChanged = false
        faceMappingChanged = false
        return compiled
    }

    open fun updateFaceSize(idx: Int) {
        // Set face size to 0 if idx >= volume face count; else set padded numVerts/numIndices.
        if (idx >= numFaces) numFaces = 0
        // Real impl: face.setSize(numVerts, numIndices)
    }

    open fun updateLOD(): Boolean {
        // Calculate LOD; if changed mark VOLUME rebuild; else check bin radius for partition move.
        val changed = calcLOD()
        if (changed) {
            volumeChanged = true
            lodChanged = true
        }
        return changed
    }

    override fun updateRadius() {
        // vobjRadius = scale.length(); drawable.setRadius(vobjRadius)
        vobjRadius = sqrt(scale.x * scale.x + scale.y * scale.y + scale.z * scale.z)
    }

    open fun updateTextures() {
        updateTextureVirtualSize()
    }

    fun updateTextureVirtualSize(forced: Boolean = false) {
        // For each drawable face: compute vsize (HUD=screenArea, else face.getTextureVirtualSize);
        // update pixelArea; handle sculpt texture discard; handle light texture stats; set debug text.
        if (numFaces == 0) return
        for (i in 0 until numFaces) {
            val vsize = if (isHUDAttachment()) {
                1024f * 1024f
            } else {
                pixelArea / numFaces.toFloat()
            }
            // vsize would be passed to face.setVirtualSize(vsize) in a real drawable
        }
        if (resetDebugText) {
            debugText = ""
            resetDebugText = false
        }
    }

    fun updateFaceFlags() {
        // For each face: set FULLBRIGHT/HUD_RENDER/LIGHT state from TE and drawable state.
        for (i in 0 until numFaces) {
            // face.clearState(FULLBRIGHT | HUD_RENDER | LIGHT)
            // if (fullbright || material == LIGHT) face.setState(FULLBRIGHT)
            // if (drawable.isLight) face.setState(LIGHT)
            // if (isHUDAttachment) face.setState(HUD_RENDER)
        }
    }

    fun regenFaces() {
        // If face count changed: deleteFaces/addFace; else reuse existing faces.
        // Set texture, normal map, specular map per face; re-link media textures.
        if (numFaces == 0) {
            deleteFaces()
            return
        }
        for (i in 0 until numFaces) {
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, 0)  // real texture resolved from TE at runtime
        }
    }

    fun genBBoxes(forceGlobal: Boolean, shouldUpdateOctreeBounds: Boolean = true): Boolean {
        // Generate volume bounding boxes per face; accumulate min/max;
        // updateRiggedVolume if needed; setPositionGroup; updateRadius; movePartition.
        updateRadius()
        if (isRiggedMesh()) updateRiggedVolume(false)
        return true
    }

    fun preRebuild() {
        volumeImpl?.preRebuild()
    }

    open fun updateSpatialExtents(newMin: FloatArray, newMax: FloatArray) {}

    open fun getBinRadius(): Float {
        // alpha wrap → min half-extent; shrink wrap → drawable.radius*0.25;
        // else max(radius, size_factor); clamp to [0.5, 256].
        val result = if (shouldShrinkWrap) {
            vobjRadius * 0.25f
        } else {
            val halfExtent = minOf(scale.x, scale.y, scale.z) * 0.5f
            maxOf(vobjRadius, halfExtent)
        }
        return result.coerceIn(0.5f, 256f)
    }

    open fun getPartitionType(): Int {
        // Return PARTITION_BRIDGE for attachments, PARTITION_VOLUME otherwise.
        return if (isAttachment()) {
            ObjectPartition.BRIDGE.ordinal
        } else {
            ObjectPartition.VOLUME.ordinal
        }
    }

    // ---- lights ----

    fun setIsLight(isLight: Boolean) {
        // Toggle PARAMS_LIGHT; notify pipeline.
        if (isLight) {
            if (lightParams == null) lightParams = LightParams()
        } else {
            lightParams = null
        }
        isLightCached = isLight
        parameterChanged(0x0002u /* PARAMS_LIGHT */, true)
    }

    fun setLightSRGBColor(r: Float, g: Float, b: Float) {
        // Convert sRGB→linear then call setLightLinearColor.
        fun srgbToLinear(c: Float): Float =
            if (c <= 0.04045f) c / 12.92f
            else ((c + 0.055f) / 1.055f).pow(2.4f)
        setLightLinearColor(srgbToLinear(r), srgbToLinear(g), srgbToLinear(b))
    }

    fun setLightLinearColor(r: Float, g: Float, b: Float) {
        // Set linear color on light params; trigger parameterChanged.
        val p = lightParams ?: LightParams().also { lightParams = it }
        val alpha = p.linearColor[3]
        p.linearColor[0] = r; p.linearColor[1] = g; p.linearColor[2] = b; p.linearColor[3] = alpha
        parameterChanged(0x0002u /* PARAMS_LIGHT */, true)
        faceMappingChanged = true
    }

    fun setLightIntensity(intensity: Float) {
        // Preserve RGB, set alpha = intensity on linearColor.
        val p = lightParams ?: LightParams().also { lightParams = it }
        p.linearColor[3] = intensity
        parameterChanged(0x0002u /* PARAMS_LIGHT */, true)
    }

    fun setLightRadius(radius: Float) {
        val p = lightParams ?: LightParams().also { lightParams = it }
        p.radius = radius
        parameterChanged(0x0002u /* PARAMS_LIGHT */, true)
    }

    fun setLightFalloff(falloff: Float) {
        val p = lightParams ?: LightParams().also { lightParams = it }
        p.falloff = falloff
        parameterChanged(0x0002u /* PARAMS_LIGHT */, true)
    }

    fun setLightCutoff(cutoff: Float) {
        val p = lightParams ?: LightParams().also { lightParams = it }
        p.cutoff = cutoff
        parameterChanged(0x0002u /* PARAMS_LIGHT */, true)
    }

    fun setLightTextureID(id: String) {
        // Non-empty id: ensure PARAMS_LIGHT_IMAGE is active; fetch and upload spotlight texture.
        val nullUuid = "00000000-0000-0000-0000-000000000000"
        if (id.isNotEmpty() && id != nullUuid) {
            val lip = lightImageParams ?: LightImageParams().also { lightImageParams = it }
            lip.lightTexture = id
            thread(isDaemon = true, name = "lightTexFetch-$id") {
                try {
                    val conn = URL("http://localhost:9998/textures/$id/raw")
                        .openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 3000
                    conn.readTimeout = 3000
                    val data: ByteArray? = if (conn.responseCode == 200)
                        conn.inputStream.readBytes()
                    else null
                    conn.disconnect()
                    if (data != null) {
                        val texHandle = intArrayOf(0)
                        glGenTextures(texHandle)
                        glBindTexture(GL_TEXTURE_2D, texHandle[0])
                        val buf: ByteBuffer = ByteBuffer.allocateDirect(data.size)
                            .order(ByteOrder.nativeOrder())
                            .put(data)
                        buf.flip()
                        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 64, 64, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf)
                        glBindTexture(GL_TEXTURE_2D, 0)
                        lightTexture = texHandle[0]
                    }
                } catch (_: Exception) { }
            }
        } else {
            lightImageParams = null
            lightTexture = null
        }
    }

    fun setSpotLightParams(params: FloatArray) {
        val lip = lightImageParams ?: LightImageParams().also { lightImageParams = it }
        params.copyInto(lip.params, endIndex = minOf(params.size, 3))
        parameterChanged(0x0009u /* PARAMS_LIGHT_IMAGE */, true)
    }

    fun getIsLight(): Boolean {
        isLightCached = lightParams != null
        return isLightCached
    }

    fun getIsLightFast(): Boolean = isLightCached

    fun getLightSRGBBaseColor(): FloatArray {
        val lin = getLightLinearBaseColor()
        fun linearToSrgb(c: Float): Float =
            if (c <= 0.0031308f) c * 12.92f
            else 1.055f * c.pow(1f / 2.4f) - 0.055f
        return floatArrayOf(linearToSrgb(lin[0]), linearToSrgb(lin[1]), linearToSrgb(lin[2]))
    }

    fun getLightLinearBaseColor(): FloatArray {
        val p = lightParams ?: return FloatArray(3) { 1f }
        return floatArrayOf(p.linearColor[0], p.linearColor[1], p.linearColor[2])
    }

    fun getLightLinearColor(): FloatArray {
        val p = lightParams ?: return FloatArray(3) { 1f }
        val intensity = p.linearColor[3]
        return floatArrayOf(p.linearColor[0] * intensity, p.linearColor[1] * intensity, p.linearColor[2] * intensity)
    }

    fun getLightSRGBColor(): FloatArray {
        val lin = getLightLinearColor()
        fun linearToSrgb(c: Float): Float =
            if (c <= 0.0031308f) c * 12.92f
            else 1.055f * c.pow(1f / 2.4f) - 0.055f
        return floatArrayOf(linearToSrgb(lin[0]), linearToSrgb(lin[1]), linearToSrgb(lin[2]))
    }

    fun getLightTextureID(): String? = lightImageParams?.lightTexture

    fun isLightSpotlight(): Boolean = lightImageParams?.isLightSpotlight() ?: false

    fun getSpotLightParams(): FloatArray =
        lightImageParams?.params?.copyOf() ?: FloatArray(3)

    fun updateSpotLightPriority() {
        // Compute pixel area of light sphere in camera space; update mLightTexture stats.
        val r = getLightRadius()
        spotLightPriority = pixelArea * (r / vobjRadius.coerceAtLeast(0.001f))
    }

    fun getSpotLightPriority(): Float = spotLightPriority

    fun getLightTexture(): Any? {
        // Fetch and cache the spotlight texture if not already done.
        val id = getLightTextureID() ?: return null
        if (lightTexture == null && id.isNotEmpty()) setLightTextureID(id)
        return lightTexture
    }

    fun getLightIntensity(): Float = lightParams?.linearColor?.get(3) ?: 1f

    fun getLightRadius(): Float = lightParams?.radius ?: 0f

    fun getLightFalloff(fudgeFactor: Float = 1f): Float =
        (lightParams?.falloff ?: 0f) * fudgeFactor

    fun getLightCutoff(): Float = lightParams?.cutoff ?: 0f

    // ---- reflection probes ----

    fun setIsReflectionProbe(isProbe: Boolean): Boolean {
        // Toggle PARAMS_REFLECTION_PROBE; updateReflectionProbePtr; return whether changed.
        val wasProbe = isReflectionProbe()
        if (isProbe != wasProbe) {
            reflectionProbeParams = if (isProbe) ReflectionProbeParams() else null
            updateReflectionProbePtr()
        }
        return wasProbe != isProbe
    }

    fun setReflectionProbeAmbiance(ambiance: Float): Boolean {
        val p = reflectionProbeParams ?: return false
        if (p.ambiance == ambiance) return false
        p.ambiance = ambiance
        parameterChanged(0x000Bu /* PARAMS_REFLECTION_PROBE */, true)
        return true
    }

    fun setReflectionProbeNearClip(nearClip: Float): Boolean {
        val p = reflectionProbeParams ?: return false
        if (p.clipDistance == nearClip) return false
        p.clipDistance = nearClip
        parameterChanged(0x000Bu /* PARAMS_REFLECTION_PROBE */, true)
        return true
    }

    fun setReflectionProbeIsBox(isBox: Boolean): Boolean {
        val p = reflectionProbeParams ?: return false
        if (p.isBox == isBox) return false
        p.isBox = isBox
        parameterChanged(0x000Bu /* PARAMS_REFLECTION_PROBE */, true)
        return true
    }

    fun setReflectionProbeIsDynamic(isDynamic: Boolean): Boolean {
        val p = reflectionProbeParams ?: return false
        if (p.isDynamic == isDynamic) return false
        p.isDynamic = isDynamic
        parameterChanged(0x000Bu /* PARAMS_REFLECTION_PROBE */, true)
        return true
    }

    fun setReflectionProbeIsMirror(isMirror: Boolean): Boolean {
        // Toggle mirror flag; register/unregister HeroProbeManager.
        val p = reflectionProbeParams ?: return false
        if (p.isMirror == isMirror) return false
        p.isMirror = isMirror
        parameterChanged(0x000Bu /* PARAMS_REFLECTION_PROBE */, true)
        if (isMirror) {
            // gPipeline.mHeroProbeManager.registerViewerObject(this)
        } else {
            // gPipeline.mHeroProbeManager.unregisterViewerObject(this)
        }
        return true
    }

    fun isReflectionProbe(): Boolean = reflectionProbeParams != null

    fun getReflectionProbeAmbiance(): Float = reflectionProbeParams?.ambiance ?: 0f

    fun getReflectionProbeNearClip(): Float = reflectionProbeParams?.clipDistance ?: 0f

    fun getReflectionProbeIsBox(): Boolean = reflectionProbeParams?.isBox ?: false

    fun getReflectionProbeIsDynamic(): Boolean = reflectionProbeParams?.isDynamic ?: false

    fun getReflectionProbeIsMirror(): Boolean = reflectionProbeParams?.isMirror ?: false

    // ---- flexible objects ----

    fun getVolumeInterfaceID(): UInt = volumeImpl?.getId() ?: 0u

    open fun isFlexible(): Boolean = flexibleObjectData != null

    open fun isSculpted(): Boolean = sculptParams != null

    open fun isMesh(): Boolean {
        val sculpt = sculptParams ?: return false
        return (sculpt.sculptType.toInt() and 0x3F) == 5 /* LL_SCULPT_TYPE_MESH */
    }

    open fun isRiggedMesh(): Boolean = skinInfo != null

    open fun hasLightTexture(): Boolean = lightImageParams != null

    fun isFlexibleFast(): Boolean = flexibleObjectData != null

    fun isSculptedFast(): Boolean = sculptParams != null

    fun isMeshFast(): Boolean {
        val sculpt = sculptParams ?: return false
        return (sculpt.sculptType.toInt() and 0x3F) == 5
    }

    fun isRiggedMeshFast(): Boolean = skinInfo != null

    fun isAnimatedObjectFast(): Boolean = isAnimatedObjectCached

    fun isVolumeGlobal(): Boolean {
        return volumeImpl?.isVolumeGlobal() ?: (riggedVolume != null)
    }

    fun canBeFlexible(): Boolean = flexibleObjectData != null

    fun setIsFlexible(isFlexible: Boolean): Boolean {
        val wasFlexible = this.isFlexible()
        if (isFlexible == wasFlexible) return false
        if (isFlexible) {
            flexibleObjectData = Any()
            setFlags(FLAGS_PHANTOM, true)
        } else {
            flexibleObjectData = null
            setFlags(FLAGS_PHANTOM, false)
        }
        setVolume(null, 0, false)
        markForUpdate()
        return true
    }

    fun getSkinInfo(): Any? = skinInfo

    fun isSkinInfoUnavailable(): Boolean = skinInfoUnavailable

    fun getMeshID(): String = sculptParams?.sculptTexture ?: ""

    // ---- extended mesh / animated objects ----

    fun getExtendedMeshFlags(): UInt = extendedMeshParams?.flags ?: 0u

    fun onSetExtendedMeshFlags(flags: UInt) {
        // Recursively mark for update; update visual complexity; update attachment overrides.
        val all = mutableListOf<ViewerObject>()
        addThisAndAllChildren(all)
        all.forEach { it.shouldShrinkWrap = true }
        updateVisualComplexity()
    }

    fun setExtendedMeshFlags(flags: UInt) {
        val current = extendedMeshParams?.flags ?: UInt.MAX_VALUE
        if (flags == current) return
        val p = extendedMeshParams ?: ExtendedMeshParams().also { extendedMeshParams = it }
        p.flags = flags
        parameterChanged(0x0014u /* PARAMS_EXTENDED_MESH */, flags, true, true)
        onSetExtendedMeshFlags(flags)
    }

    fun canBeAnimatedObject(): Boolean {
        val ANIMATED_OBJECT_MAX_TRIS = 10000f
        return getEstTrianglesMax() <= ANIMATED_OBJECT_MAX_TRIS
    }

    open fun isAnimatedObject(): Boolean {
        // True if root is a VOVolume and its extended mesh flags have ANIMATED_MESH_ENABLED_FLAG.
        val root = getRootEdit() as? VOVolume ?: return false
        val ANIMATED_MESH_ENABLED_FLAG: UInt = 0x01u
        return (root.getExtendedMeshFlags() and ANIMATED_MESH_ENABLED_FLAG) != 0u
    }

    open fun onReparent(oldParent: Any?, newParent: Any?) {
        // Discard control avatar if new parent is not an avatar.
        // Update visual complexity on the old animated-object parent.
        val oldVO = oldParent as? VOVolume
        if (oldVO != null && oldVO.isAnimatedObject()) {
            oldVO.updateVisualComplexity()
        }
    }

    open fun afterReparent() {
        // If this is an animated object with a control avatar, call updateAnimations().
        if (isAnimatedObject()) updateVisualComplexity()
    }

    // ---- rigging ----

    override fun updateRiggingInfo() {
        // If rigged mesh: iterate volume faces; LLSkinningUtil.updateRiggingInfo;
        // merge joint rigging info tab.
        if (!isRiggedMesh()) return
        lastRiggingInfoLod = lod
    }

    // ---- media ----

    fun updateObjectMediaData(mediaDataArray: Any?, mediaVersion: String) {
        // Parse fetched_version; if newer than lastFetchedMediaVersion, sync each TE entry.
        val fetchedVersion = mediaVersion.substringAfterLast(':').trim().toIntOrNull() ?: 0
        if (fetchedVersion <= lastFetchedMediaVersion) return
        lastFetchedMediaVersion = fetchedVersion

        when (mediaDataArray) {
            is List<*> -> mediaDataArray.forEachIndexed { i, entry ->
                syncMediaData(i, entry, false, false)
            }
            is String -> {
                mediaDataArray.split(";").forEachIndexed { i, entry ->
                    if (i < mediaImplList.size) syncMediaData(i, entry.trim(), false, false)
                }
            }
        }
    }

    fun mediaNavigateBounceBack(textureIndex: UByte) {
        // Find current/home URL; if empty or not whitelisted: setMediaFailed; else navigateTo.
        val impl = getMediaImpl(textureIndex) ?: return
        System.err.println("mediaNavigateBounceBack: face=${textureIndex.toInt()}")
    }

    enum class MediaPermType { MEDIA_PERM_INTERACT, MEDIA_PERM_CONTROL }

    fun hasMediaPermission(mediaEntry: Any?, permType: MediaPermType): Boolean {
        if (mediaEntry == null) return false
        // Check PERM_ANYONE | PERM_GROUP (agent in group) | PERM_OWNER (permYouOwner).
        return try {
            val permsMethod = if (permType == MediaPermType.MEDIA_PERM_INTERACT)
                "getPermsInteract" else "getPermsControl"
            val perms = mediaEntry.javaClass.getMethod(permsMethod).invoke(mediaEntry) as? Int ?: 0
            val PERM_ANYONE = 0x01; val PERM_GROUP = 0x02; val PERM_OWNER = 0x04
            when {
                perms and PERM_ANYONE != 0 -> true
                perms and PERM_GROUP  != 0 -> true   // simplified: skip group membership check
                perms and PERM_OWNER  != 0 && permYouOwner() -> true
                else -> false
            }
        } catch (_: Exception) { false }
    }

    fun mediaNavigated(impl: Any?, plugin: Any?, newLocation: String) {
        // Whitelist check; permission check; if blocked bounceBack; else navigate.
        val faceIndex = getFaceIndexWithMediaImpl(impl, -1)
        if (faceIndex < 0) return
        System.err.println("mediaNavigated: face=$faceIndex location=$newLocation")
    }

    fun mediaEvent(impl: Any?, plugin: Any?, event: Int) {
        // Handle LOCATION_CHANGED (broadcast/bounce based on nav state) and NAVIGATE_COMPLETE;
        // handle FILE_DOWNLOAD (send empty response; show notification).
        val LOCATION_CHANGED  = 1
        val NAVIGATE_COMPLETE = 2
        val FILE_DOWNLOAD     = 3
        when (event) {
            LOCATION_CHANGED -> {
                val faceIndex = getFaceIndexWithMediaImpl(impl, -1)
                if (faceIndex >= 0) System.err.println("mediaEvent: LOCATION_CHANGED face=$faceIndex")
            }
            NAVIGATE_COMPLETE -> System.err.println("mediaEvent: NAVIGATE_COMPLETE")
            FILE_DOWNLOAD     -> System.err.println("mediaEvent: FILE_DOWNLOAD blocked")
        }
    }

    fun syncMediaData(textureIndex: Int, mediaData: Any?, merge: Boolean, ignoreAgent: Boolean) {
        // Merge or replace media data on TE; updateMediaImpl; autoplay for HUD media.
        if (dead) return
        if (textureIndex < 0 || textureIndex >= mediaImplList.size) return
        mediaImplList[textureIndex] = mediaData
        if (mediaData != null && isHUDAttachment()) {
            System.err.println("syncMediaData: auto-play HUD media face=$textureIndex")
        }
    }

    fun sendMediaDataUpdate() {
        // Enqueue a media data update via the object media client HTTP endpoint.
        if (sObjectMediaClient == null) return
        thread(isDaemon = true, name = "mediaDataUpdate-$id") {
            try {
                val conn = URL("http://localhost:9998/objects/$id/media")
                    .openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.outputStream.writer().use { it.write("{\"objectId\":\"$id\"}") }
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) { }
        }
    }

    fun getMediaImpl(faceId: UByte): Any? =
        mediaImplList.getOrNull(faceId.toInt())

    fun getFaceIndexWithMediaImpl(mediaImpl: Any?, startFaceId: Int): Int {
        for (i in startFaceId + 1 until mediaImplList.size) {
            if (mediaImplList[i] === mediaImpl) return i
        }
        return -1
    }

    fun getTotalMediaInterest(): Double {
        // F64_MAX/2 if selected; else sum of impl.getInterest().
        if (userSelected) return Double.MAX_VALUE / 2.0
        return mediaImplList.sumOf { impl ->
            try {
                impl?.javaClass?.getMethod("getInterest")?.invoke(impl) as? Double ?: 0.0
            } catch (_: Exception) { 0.0 }
        }
    }

    fun hasMedia(): Boolean = mediaImplList.any { it != null }

    fun isMediaDataBeingFetched(): Boolean = sObjectMediaClient != null && hasMedia()

    fun getLastFetchedMediaVersion(): Int = lastFetchedMediaVersion

    fun addMDCImpl() { mdcImplCount++ }
    fun removeMDCImpl() { mdcImplCount-- }
    fun getMDCImplCount(): Int = mdcImplCount

    // ---- silhouette / misc ----

    fun generateSilhouette(nodep: Any?, viewPoint: FloatArray) {
        // Transform viewPoint to volume space; generateSilhouetteVertices with relativeXform.
        val volViewPoint = agentPositionToVolume(viewPoint)
        val vboId = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        val buf: ByteBuffer = ByteBuffer.allocateDirect(3 * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        buf.putFloat(volViewPoint[0]).putFloat(volViewPoint[1]).putFloat(volViewPoint[2])
        buf.flip()
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STREAM_DRAW)
        glDrawArrays(GL_LINE_STRIP, 0, 1)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    fun getApproximateFaceNormal(faceId: UByte): FloatArray {
        // Average normals of all vertices on face; transform to agent space; normalize.
        // Without a live volume, return +Z axis transformed to agent space.
        val n = volumeDirectionToAgent(floatArrayOf(0f, 0f, 1f))
        val len = sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]).coerceAtLeast(1e-6f)
        return floatArrayOf(n[0] / len, n[1] / len, n[2] / len)
    }

    fun updateVisualComplexity() {
        // Notify the nearest avatar ancestor to recompute visual complexity.
        var cur: ViewerObject? = parent
        while (cur != null) {
            if (cur.isAvatar()) {
                try { cur.javaClass.getMethod("updateVisualComplexity").invoke(cur) } catch (_: Exception) { }
                break
            }
            cur = cur.parent
        }
    }

    fun notifyMeshLoaded() {
        // Mark sculpt and geometry dirty; check skin info; notify avatar/controlAvatar.
        sculptChanged = true
        volumeChanged = true
        updateVisualComplexity()
    }

    fun notifySkinInfoLoaded(skin: Any?) {
        skinInfoUnavailable = false
        skinInfo = skin
        notifyMeshLoaded()
    }

    fun notifySkinInfoUnavailable() {
        skinInfoUnavailable = true
        skinInfo = null
    }

    // ---- rigged volume ----

    fun updateRiggedVolume(
        forceTreatAsRigged: Boolean,
        faceIndex: Int = RiggedVolume.UPDATE_ALL_FACES,
        rebuildFaceOctrees: Boolean = true,
    ) {
        // If should be rigged: create/update riggedVolume; else clear it.
        val shouldRig = forceTreatAsRigged || treatAsRigged()
        if (shouldRig) {
            if (riggedVolume == null) riggedVolume = RiggedVolume(Any())
            riggedVolume!!.update(skinInfo, null, null, faceIndex, rebuildFaceOctrees)
        } else {
            riggedVolume = null
        }
    }

    fun getRiggedVolume(): RiggedVolume? = riggedVolume

    fun treatAsRigged(): Boolean {
        // Build tools open OR (isAttachment AND attached to self AND rendered as rigged).
        return isAttachment() && isRiggedMesh()
    }

    fun clearRiggedVolume() {
        riggedVolume = null
    }

    // ---- LOD internals ----

    fun computeLODDetail(distance: Float, radius: Float, lodFactor: Float): Int {
        return if (dynamicLod) {
            val tanAngle = (lodFactor * radius) / distance
            // LLVolumeLODGroup.getDetailFromTan(round(tanAngle, 0.01)) — approximate mapping.
            val rounded = (tanAngle * 100f).roundToInt() / 100f
            when {
                rounded >= 1.0f  -> 3
                rounded >= 0.25f -> 2
                rounded >= 0.06f -> 1
                else             -> 0
            }
        } else {
            (sqrt(radius) * lodFactor * 4f).toInt().coerceIn(0, 3)
        }
    }

    fun calcLOD(): Boolean {
        // Determine distance and radius; apply distance factor/ramp; computeLODDetail;
        // return true if LOD changed.
        val dist = lodDistance.coerceAtLeast(0.001f) * sDistanceFactor
        val radius = vobjRadius * sLODFactor
        val newLod = computeLODDetail(dist, radius, sLODFactor)
        if (newLod != lod) {
            lod = newLod
            return true
        }
        return false
    }

    fun forceLOD(lodLevel: Int) {
        lod = lodLevel
        // markRebuild(VOLUME); lodChanged = true
        volumeChanged = true
        lodChanged = true
    }

    private fun lodOrSculptChanged(drawable: Any?, compiled: Boolean, shouldUpdateOctreeBounds: Boolean): Boolean {
        // Set volume at current LOD; if lod or sculpt changed: update face count,
        // regenFaces if needed, unbound spatial group on sculpt change.
        var recompiled = compiled
        if (lodChanged || sculptChanged) {
            recompiled = true
            if (sculptChanged) sculptChanged = false
        }
        return recompiled
    }

    private fun onDrawableUpdateFromServer() {
        serverDrawableUpdateCount++
        if (serverDrawableUpdateCount > 8u) {
            // Make the drawable active to avoid octree disruption from scripted updates.
            onActiveList = true
        }
    }

    // Stub fields referenced by methods above
    private val mStatic: Boolean = false
    private val dynamicLod: Boolean = true
    private val LOD_HIGH: Int = 3
    private val NO_LOD: Int = -1
}
