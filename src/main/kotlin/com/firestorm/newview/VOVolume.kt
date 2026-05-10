package com.firestorm.newview

import kotlin.math.*

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
        TODO("GPU: deform src_volume faces by joint transforms from skin/avatar; rebuild octrees if requested")
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
            TODO("APR: read PrimMediaMasterEnabled; create LLObjectMediaDataClient/LLObjectMediaNavigateClient from settings")
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
            TODO("GPU: 256 + ARC_TEXTURE_COST*(fullHeight/128 + fullWidth/128); special-case alpha-grad textures")
        }

        fun setTEMaterialParamsCallbackTE(
            objectId: String,
            materialId: String,
            materialParams: Any?,
            te: UInt,
        ) {
            TODO("APR: look up VOVolume by objectId; if te matches pending materialId call setTEMaterialParams")
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

    // ---- lifecycle ----

    open fun markDead() {
        TODO("GPU: unregister sculpt/light textures; detach media impls; unregister reflection/hero probes; super.markDead()")
    }

    // ---- core overrides ----

    open fun isVisible(): Boolean {
        TODO("GPU: mDrawable.notNull && mDrawable.isVisible; or walk parent chain to find avatar's drawable")
    }

    open fun isActive(): Boolean = !mStatic   // mStatic comes from LLViewerObject

    open fun isAttachment(): Boolean {
        TODO("APR: mAttachmentState != 0")
    }

    open fun isRootEdit(): Boolean {
        TODO("APR: true unless parent exists and parent is not an avatar")
    }

    open fun isHUDAttachment(): Boolean {
        TODO("APR: ATTACHMENT_ID_FROM_STATE(mAttachmentState) in [31..38]")
    }

    open fun createDrawable(pipeline: Any?): Any? {
        TODO("GPU: pipeline.allocDrawable; setRenderType(VOLUME); addFace for each TE; makeActive if attachment; setLight if light; updateReflectionProbePtr; updateRadius; updateDistance")
    }

    fun deleteFaces() {
        TODO("GPU: mDrawable.deleteFaces(0, mNumFaces); mNumFaces = 0")
    }

    fun animateTextures() {
        TODO("GPU: mTextureAnimp.animateTextures(off_s,off_t,scale_s,scale_t,rot); apply texture matrix to each face; handle rate==0 reset")
    }

    open fun setParent(parent: Any?): Boolean {
        TODO("GPU: LLViewerObject.setParent; markMoved; markRebuild(VOLUME); onReparent")
    }

    fun getLod(): Int = lod

    fun setNoLod() {
        lod = NO_LOD
        lodChanged = true
    }

    fun isNoLod(): Boolean = lod == NO_LOD

    open fun getPivotPositionAgent(): FloatArray {
        return volumeImpl?.getPivotPosition() ?: TODO("APR: LLViewerObject.getPivotPositionAgent()")
    }

    fun getRelativeXform(): FloatArray = relativeXform
    fun getRelativeXformInvTrans(): FloatArray = relativeXformInvTrans

    open fun getRenderMatrix(): FloatArray {
        TODO("GPU: if active && !root return parent world matrix; else return drawable world matrix")
    }

    open fun getEstTrianglesMax(): Float {
        TODO("APR: gMeshRepo.getEstTrianglesMax(sculptID) for mesh; else 0")
    }

    open fun getEstTrianglesStreamingCost(): Float {
        TODO("APR: gMeshRepo.getEstTrianglesStreamingCost(sculptID) for mesh; else 0")
    }

    open fun getStreamingCost(): Float {
        TODO("APR: radius-based or triangle-based streaming cost from LLMeshCostData; add ANIMATED_OBJECT_BASE_COST for animated root")
    }

    open fun getCostData(costs: Any?): Boolean {
        TODO("APR: mesh → gMeshRepo.getCostData(sculptID); prim → getLoDTriangleCounts then build fake header")
    }

    open fun getTriangleCount(vcount: IntArray? = null): UInt {
        TODO("APR: volume.getNumTriangles(vcount)")
    }

    open fun getHighLODTriangleCount(): UInt = getLODTriangleCount(LOD_HIGH)

    open fun getLODTriangleCount(lodLevel: Int): UInt {
        TODO("APR: refVolume at lodLevel; getNumTriangles(); unrefVolume")
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
        TODO("GPU: ray-test each volume face octree; barycentric UV; alpha-mask check for transparent pick")
    }

    // ---- position / volume space transforms ----

    fun agentPositionToVolume(pos: FloatArray): FloatArray {
        TODO("APR: (pos - renderPosition) * ~renderRotation; optionally scale by invObjScale if not global")
    }

    fun agentDirectionToVolume(dir: FloatArray): FloatArray {
        TODO("APR: dir * ~renderRotation; optionally scale by invObjScale if not global")
    }

    fun volumePositionToAgent(pos: FloatArray): FloatArray {
        TODO("APR: pos * renderRotation + renderPosition; optionally scale by objScale if not global")
    }

    fun volumeDirectionToAgent(dir: FloatArray): FloatArray {
        TODO("APR: dir * renderRotation; optionally scale")
    }

    fun getVolumeChanged(): Boolean = volumeChanged

    open fun getVObjRadius(): Float = vobjRadius

    open fun getWorldMatrix(xform: Any): FloatArray {
        return volumeImpl?.getWorldMatrix(xform) ?: TODO("APR: xform.getWorldMatrix()")
    }

    override fun markForUpdate() {
        TODO("GPU: if drawable shrinkWrap(); LLViewerObject.markForUpdate(); volumeChanged = true")
    }

    fun faceMappingChanged() {
        faceMappingChanged = true
    }

    open fun onShift(shiftVector: FloatArray) {
        volumeImpl?.onShift(shiftVector)
        updateRelativeXform()
    }

    open fun parameterChanged(paramType: UShort, localOrigin: Boolean) {
        TODO("APR: LLViewerObject.parameterChanged; update light pipeline state; updateReflectionProbePtr")
    }

    open fun parameterChanged(paramType: UShort, data: Any?, inUse: Boolean, localOrigin: Boolean) {
        TODO("APR: LLViewerObject.parameterChanged; volumeImpl.onParameterChanged; handle PARAMS_EXTENDED_MESH animated flag; set pipeline light state; updateReflectionProbePtr")
    }

    fun updateReflectionProbePtr() {
        TODO("GPU: if isReflectionProbe: register with ReflectionMapManager or HeroProbeManager; else unregister")
    }

    open fun processUpdateMessage(blockNum: UInt, updateType: Int, dp: Any?): UInt {
        // Local mesh objects skip server updates entirely.
        if (isLocalMesh) return 0u
        TODO("APR: LLViewerObject.processUpdateMessage; unpack texture anim; unpack volume params; unpack TEs; requestMediaDataUpdate if media changed; onDrawableUpdateFromServer if dirty")
    }

    open fun setSelected(sel: Boolean) {
        TODO("GPU: LLViewerObject.setSelected; if animated object recursiveMarkForUpdate; else markForUpdate")
    }

    open fun setDrawableParent(parent: Any?): Boolean {
        TODO("GPU: LLViewerObject.setDrawableParent; markRebuild(VOLUME); propagate active state")
    }

    open fun setScale(scale: FloatArray, damped: Boolean) {
        TODO("GPU: if scale != current: LLViewerObject.setScale; volumeImpl.onSetScale; updateRadius; markRebuild(POSITION); shrinkWrap")
    }

    open fun changeTEImage(index: Int, imagep: Any?) {
        TODO("GPU: LLViewerObject.changeTEImage; if changed markTextured; faceMappingChanged=true")
    }

    open fun setNumTEs(numTes: UByte) {
        TODO("APR: grow/shrink mediaImplList matching; duplicate/remove media impls at boundaries; LLViewerObject.setNumTEs")
    }

    open fun setTEImage(te: UByte, imagep: Any?) {
        TODO("GPU: LLViewerObject.setTEImage; if changed markTextured; faceMappingChanged=true")
    }

    open fun setTETexture(te: UByte, uuid: String): Int {
        TODO("GPU: LLViewerObject.setTETexture; if changed shrinkWrap; markTextured; faceMappingChanged=true")
    }

    open fun setTEColor(te: UByte, r: Float, g: Float, b: Float): Int =
        setTEColor(te, r, g, b, 1f)

    open fun setTEColor(te: UByte, r: Float, g: Float, b: Float, a: Float): Int {
        TODO("GPU: compare with current TE color; if alpha changed markTextured+markRebuild(VOLUME)+lodChanged=true; LLPrimitive.setTEColor; colorChanged=true; REBUILD_COLOR; shrinkWrap; dirtyMesh")
    }

    open fun setTEBumpmap(te: UByte, bump: UByte): Int {
        TODO("GPU: LLViewerObject.setTEBumpmap; if changed markTextured; faceMappingChanged=true")
    }

    open fun setTEShiny(te: UByte, shiny: UByte): Int {
        TODO("GPU: LLViewerObject.setTEShiny; if changed markTextured; faceMappingChanged=true")
    }

    open fun setTEFullbright(te: UByte, fullbright: UByte): Int {
        TODO("GPU: LLViewerObject.setTEFullbright; if changed markTextured; faceMappingChanged=true")
    }

    open fun setTEBumpShinyFullbright(te: UByte, bump: UByte): Int {
        TODO("GPU: LLViewerObject.setTEBumpShinyFullbright; if changed markTextured; faceMappingChanged=true")
    }

    open fun setTEMediaFlags(te: UByte, mediaFlags: UByte): Int {
        TODO("GPU: LLViewerObject.setTEMediaFlags; if changed markTextured; faceMappingChanged=true")
    }

    open fun setTEGlow(te: UByte, glow: Float): Int {
        TODO("GPU: LLViewerObject.setTEGlow; if changed and drawable: markTextured; shrinkWrap; faceMappingChanged=true")
    }

    open fun setTEMaterialID(te: UByte, materialId: String): Int {
        TODO("APR: LLViewerObject.setTEMaterialID; async fetch material params; setChanged(ALL); markTextured; markRebuild(ALL); faceMappingChanged=true")
    }

    open fun setTEMaterialParams(te: UByte, materialParams: Any?): Int {
        TODO("APR: LLViewerObject.setTEMaterialParams; setChanged(ALL); markTextured; markRebuild(ALL); faceMappingChanged=true; return TEM_CHANGE_TEXTURE")
    }

    open fun setTEGLTFMaterialOverride(te: UByte, mat: Any?): Int {
        TODO("GPU: LLViewerObject.setTEGLTFMaterialOverride; if TEM_CHANGE_TEXTURE: markTextured; markRebuild(ALL); faceMappingChanged=true")
    }

    open fun setTEScale(te: UByte, s: Float, t: Float): Int {
        TODO("GPU: LLViewerObject.setTEScale; if changed markTextured; faceMappingChanged=true")
    }

    open fun setTEScaleS(te: UByte, s: Float): Int {
        TODO("GPU: LLViewerObject.setTEScaleS; if changed markTextured; faceMappingChanged=true")
    }

    open fun setTEScaleT(te: UByte, t: Float): Int {
        TODO("GPU: LLViewerObject.setTEScaleT; if changed markTextured; faceMappingChanged=true")
    }

    open fun setTETexGen(te: UByte, texgen: UByte): Int {
        TODO("GPU: LLViewerObject.setTETexGen; if changed markTextured; faceMappingChanged=true")
    }

    open fun setTEMediaTexGen(te: UByte, media: UByte): Int {
        TODO("GPU: LLViewerObject.setTEMediaTexGen; if changed markTextured; faceMappingChanged=true")
    }

    open fun setMaterial(material: UByte): Boolean {
        TODO("APR: LLViewerObject.setMaterial")
    }

    fun setTexture(face: Int) {
        TODO("GPU: gGL.getTexUnit(0).bind(getTEImage(face))")
    }

    fun getIndexInTex(ch: UInt): Int = indexInTex[ch.toInt()]
    fun setIndexInTex(ch: UInt, index: Int) { indexInTex[ch.toInt()] = index }

    fun unregisterOldMeshAndSkin() {
        TODO("APR: if sculpt type is mesh: gMeshRepo.unregisterMesh for all LODs; gMeshRepo.unregisterSkinInfo")
    }

    open fun setVolume(params: Any?, detail: Int, uniqueVolume: Boolean = false): Boolean {
        TODO("APR: determine real LOD (mesh may be 404); handle flexible flag; LLPrimitive.setVolume; updateSculptTexture; loadMesh/getSkinInfo for mesh; sculpt() for sculptie; GLTFSceneManager.addGLTFObject for GLTF")
    }

    fun updateSculptTexture() {
        TODO("GPU: fetch sculpt texture if sculpted non-mesh; remove old texture volume reference; add new")
    }

    fun sculpt() {
        TODO("GPU: read raw image data from mSculptTexture; call volume.sculpt(width, height, components, data, discardLevel, isMissingAsset)")
    }

    // static callback registered with LLMaterialMgr
    fun rebuildMeshAssetCallback(assetUuid: String, type: Int, status: Int) {
        TODO("APR: trigger geometry rebuild after mesh asset arrives")
    }

    fun updateRelativeXform(forceIdentity: Boolean = false) {
        if (volumeImpl != null) {
            volumeImpl!!.updateRelativeXform(forceIdentity)
            return
        }
        TODO("GPU: compute relativeXform and relativeXformInvTrans from drawable state (rigged / active / static)")
    }

    open fun updateGeometry(drawable: Any?): Boolean {
        TODO("GPU: handle REBUILD_RIGGED; delegate to volumeImpl if present; lodOrSculptChanged; regenFaces; genBBoxes; updateFaceFlags; sCompiles++; clear dirty flags")
    }

    open fun updateFaceSize(idx: Int) {
        TODO("GPU: if idx >= volume.numFaces: face.setSize(0,0); else face.setSize(numVerts, numIndices) padded")
    }

    open fun updateLOD(): Boolean {
        TODO("GPU: calcLOD(); if changed markRebuild(VOLUME); else check bin radius change for partition move")
    }

    override fun updateRadius() {
        TODO("GPU: vobjRadius = scale.length(); drawable.setRadius(vobjRadius)")
    }

    open fun updateTextures() {
        updateTextureVirtualSize()
    }

    fun updateTextureVirtualSize(forced: Boolean = false) {
        TODO("GPU: for each drawable face: compute vsize (HUD=screenArea, else face.getTextureVirtualSize); update pixelArea; handle sculpt texture discard; handle light texture stats; set debug text")
    }

    fun updateFaceFlags() {
        TODO("GPU: for each face: set FULLBRIGHT/HUD_RENDER/LIGHT state from TE and drawable state")
    }

    fun regenFaces() {
        TODO("GPU: if face count changed: deleteFaces/addFace; else reuse; set texture, normal map, specular map per face; re-link media textures")
    }

    fun genBBoxes(forceGlobal: Boolean, shouldUpdateOctreeBounds: Boolean = true): Boolean {
        TODO("GPU: genVolumeBBoxes per face; accumulate min/max; updateRiggedVolume if needed; setPositionGroup; updateRadius; movePartition")
    }

    fun preRebuild() {
        volumeImpl?.preRebuild()
    }

    open fun updateSpatialExtents(newMin: FloatArray, newMax: FloatArray) {}

    open fun getBinRadius(): Float {
        TODO("GPU: alpha wrap → min half-extent; shrink wrap → drawable.radius*0.25; else max(radius, size_factor); clamp to [0.5, 256]")
    }

    open fun getPartitionType(): Int {
        TODO("APR: return LLViewerRegion.PARTITION_VOLUME or PARTITION_BRIDGE for attachments")
    }

    // ---- lights ----

    fun setIsLight(isLight: Boolean) {
        TODO("GPU: toggle PARAMS_LIGHT; gPipeline.setLight(mDrawable, isLight)")
    }

    fun setLightSRGBColor(r: Float, g: Float, b: Float) {
        TODO("APR: convert sRGB→linear then setLightLinearColor")
    }

    fun setLightLinearColor(r: Float, g: Float, b: Float) {
        TODO("APR: getLightParams.setLinearColor; parameterChanged(PARAMS_LIGHT); markTextured; faceMappingChanged=true")
    }

    fun setLightIntensity(intensity: Float) {
        TODO("APR: getLightParams.setLinearColor (preserve rgb, change alpha=intensity); parameterChanged")
    }

    fun setLightRadius(radius: Float) {
        TODO("APR: getLightParams.setRadius; parameterChanged(PARAMS_LIGHT)")
    }

    fun setLightFalloff(falloff: Float) {
        TODO("APR: getLightParams.setFalloff; parameterChanged(PARAMS_LIGHT)")
    }

    fun setLightCutoff(cutoff: Float) {
        TODO("APR: getLightParams.setCutoff; parameterChanged(PARAMS_LIGHT)")
    }

    fun setLightTextureID(id: String) {
        TODO("GPU: if id.notNull: ensure PARAMS_LIGHT_IMAGE in use; remove old texture reference; set new; add new texture reference; else remove PARAMS_LIGHT_IMAGE")
    }

    fun setSpotLightParams(params: FloatArray) {
        TODO("APR: getLightImageParams.setParams; parameterChanged(PARAMS_LIGHT_IMAGE)")
    }

    fun getIsLight(): Boolean {
        isLightCached = TODO("APR: getLightParams() != null")
        @Suppress("UNREACHABLE_CODE")
        return isLightCached
    }

    fun getIsLightFast(): Boolean = isLightCached

    fun getLightSRGBBaseColor(): FloatArray {
        TODO("APR: srgbColor3(getLightLinearBaseColor())")
    }

    fun getLightLinearBaseColor(): FloatArray {
        TODO("APR: getLightParams?.linearColor ?: FloatArray(3){1f}")
    }

    fun getLightLinearColor(): FloatArray {
        TODO("APR: getLightLinearBaseColor() * intensity (alpha component)")
    }

    fun getLightSRGBColor(): FloatArray {
        TODO("APR: srgbColor3(getLightLinearColor())")
    }

    fun getLightTextureID(): String? {
        TODO("APR: getLightImageParams()?.lightTexture")
    }

    fun isLightSpotlight(): Boolean {
        TODO("APR: getLightImageParams()?.isLightSpotlight() ?: false")
    }

    fun getSpotLightParams(): FloatArray {
        TODO("APR: getLightImageParams()?.params ?: FloatArray(3)")
    }

    fun updateSpotLightPriority() {
        TODO("GPU: compute pixel area of light sphere in camera space; update mLightTexture stats")
    }

    fun getSpotLightPriority(): Float = spotLightPriority

    fun getLightTexture(): Any? {
        TODO("GPU: fetch texture for getLightTextureID() if not already cached")
    }

    fun getLightIntensity(): Float {
        TODO("APR: getLightParams()?.linearColor?.alpha ?: 1f")
    }

    fun getLightRadius(): Float {
        TODO("APR: getLightParams()?.radius ?: 0f")
    }

    fun getLightFalloff(fudgeFactor: Float = 1f): Float {
        TODO("APR: getLightParams()?.falloff * fudgeFactor ?: 0f")
    }

    fun getLightCutoff(): Float {
        TODO("APR: getLightParams()?.cutoff ?: 0f")
    }

    // ---- reflection probes ----

    fun setIsReflectionProbe(isProbe: Boolean): Boolean {
        TODO("GPU: toggle PARAMS_REFLECTION_PROBE; updateReflectionProbePtr; return whether changed")
    }

    fun setReflectionProbeAmbiance(ambiance: Float): Boolean {
        TODO("APR: getReflectionProbeParams.setAmbiance; parameterChanged; return true if changed")
    }

    fun setReflectionProbeNearClip(nearClip: Float): Boolean {
        TODO("APR: getReflectionProbeParams.setClipDistance; parameterChanged; return true if changed")
    }

    fun setReflectionProbeIsBox(isBox: Boolean): Boolean {
        TODO("APR: getReflectionProbeParams.setIsBox; parameterChanged; return true if changed")
    }

    fun setReflectionProbeIsDynamic(isDynamic: Boolean): Boolean {
        TODO("APR: getReflectionProbeParams.setIsDynamic; parameterChanged; return true if changed")
    }

    fun setReflectionProbeIsMirror(isMirror: Boolean): Boolean {
        TODO("GPU: getReflectionProbeParams.setIsMirror; parameterChanged; register/unregister HeroProbeManager; return true if changed")
    }

    fun isReflectionProbe(): Boolean {
        TODO("APR: getReflectionProbeParams() != null")
    }

    fun getReflectionProbeAmbiance(): Float {
        TODO("APR: getReflectionProbeParams()?.ambiance ?: 0f")
    }

    fun getReflectionProbeNearClip(): Float {
        TODO("APR: getReflectionProbeParams()?.clipDistance ?: 0f")
    }

    fun getReflectionProbeIsBox(): Boolean {
        TODO("APR: getReflectionProbeParams()?.isBox ?: false")
    }

    fun getReflectionProbeIsDynamic(): Boolean {
        TODO("APR: getReflectionProbeParams()?.isDynamic ?: false")
    }

    fun getReflectionProbeIsMirror(): Boolean {
        TODO("APR: getReflectionProbeParams()?.isMirror ?: false")
    }

    // ---- flexible objects ----

    fun getVolumeInterfaceID(): UInt = volumeImpl?.getId() ?: 0u

    open fun isFlexible(): Boolean {
        TODO("APR: getFlexibleObjectData() != null")
    }

    open fun isSculpted(): Boolean {
        TODO("APR: getSculptParams() != null")
    }

    open fun isMesh(): Boolean {
        TODO("APR: isSculpted && (sculptType & MASK) == LL_SCULPT_TYPE_MESH")
    }

    open fun isRiggedMesh(): Boolean = skinInfo != null

    open fun hasLightTexture(): Boolean {
        TODO("APR: getLightImageParams() != null")
    }

    fun isFlexibleFast(): Boolean {
        TODO("APR: volumep?.params?.pathParams?.curveType == LL_PCODE_PATH_FLEXIBLE")
    }

    fun isSculptedFast(): Boolean {
        TODO("APR: volumep?.params?.isSculpt()")
    }

    fun isMeshFast(): Boolean {
        TODO("APR: volumep?.params?.isMeshSculpt()")
    }

    fun isRiggedMeshFast(): Boolean = skinInfo != null

    fun isAnimatedObjectFast(): Boolean = isAnimatedObjectCached

    fun isVolumeGlobal(): Boolean {
        return volumeImpl?.isVolumeGlobal() ?: (riggedVolume != null)
    }

    fun canBeFlexible(): Boolean {
        TODO("APR: path curve type is FLEXIBLE or LINE")
    }

    fun setIsFlexible(isFlexible: Boolean): Boolean {
        TODO("APR: toggle path curve type FLEXIBLE/LINE; setFlags PHANTOM; toggle PARAMS_FLEXIBLE; setVolume; markForUpdate")
    }

    fun getSkinInfo(): Any? {
        TODO("APR: if volume exists return mSkinInfo else null")
    }

    fun isSkinInfoUnavailable(): Boolean = skinInfoUnavailable

    fun getMeshID(): String {
        TODO("APR: getVolume().params.sculptID")
    }

    // ---- extended mesh / animated objects ----

    fun getExtendedMeshFlags(): UInt {
        TODO("APR: getExtendedMeshParams()?.flags ?: 0u")
    }

    fun onSetExtendedMeshFlags(flags: UInt) {
        TODO("APR: recursiveMarkForUpdate; updateVisualComplexity; updateAttachmentOverrides for avatar ancestor")
    }

    fun setExtendedMeshFlags(flags: UInt) {
        TODO("APR: if flags changed: setParameterEntryInUse(PARAMS_EXTENDED_MESH); getExtendedMeshParams.setFlags; parameterChanged; onSetExtendedMeshFlags")
    }

    fun canBeAnimatedObject(): Boolean {
        TODO("APR: recursiveGetEstTrianglesMax() <= getAnimatedObjectMaxTris()")
    }

    open fun isAnimatedObject(): Boolean {
        TODO("APR: root is volume && root.extendedMeshFlags has ANIMATED_MESH_ENABLED_FLAG")
    }

    open fun onReparent(oldParent: Any?, newParent: Any?) {
        TODO("APR: if non-avatar new parent: discard control avatar; update control avatar overrides for old animated-object parent")
    }

    open fun afterReparent() {
        TODO("APR: if animated object with control avatar: updateAnimations()")
    }

    // ---- rigging ----

    override fun updateRiggingInfo() {
        TODO("APR: if riggedMesh: iterate volume faces; LLSkinningUtil.updateRiggingInfo; merge joint rigging info tab")
    }

    // ---- media ----

    fun updateObjectMediaData(mediaDataArray: Any?, mediaVersion: String) {
        TODO("APR: parse fetched_version; if newer than mLastFetchedMediaVersion: syncMediaData for each TE entry")
    }

    fun mediaNavigateBounceBack(textureIndex: UByte) {
        TODO("APR: find current/home URL; if empty or not whitelisted: setMediaFailed; else navigateTo")
    }

    enum class MediaPermType { MEDIA_PERM_INTERACT, MEDIA_PERM_CONTROL }

    fun hasMediaPermission(mediaEntry: Any?, permType: MediaPermType): Boolean {
        TODO("APR: check PERM_ANYONE | PERM_GROUP (agent in group) | PERM_OWNER (permYouOwner)")
    }

    fun mediaNavigated(impl: Any?, plugin: Any?, newLocation: String) {
        TODO("APR: whitelist check; permission check; if blocked: bounceBack; else sObjectMediaNavigateClient.navigate")
    }

    fun mediaEvent(impl: Any?, plugin: Any?, event: Int) {
        TODO("APR: handle LOCATION_CHANGED (broadcast/bounce based on nav state) and NAVIGATE_COMPLETE; handle FILE_DOWNLOAD (send empty response; show notification)")
    }

    fun syncMediaData(textureIndex: Int, mediaData: Any?, merge: Boolean, ignoreAgent: Boolean) {
        TODO("APR: merge or replace media data on TE; updateMediaImpl; autoplay for HUD media; addMediaImpl or removeMediaImpl")
    }

    fun sendMediaDataUpdate() {
        TODO("APR: sObjectMediaClient.updateMedia(LLMediaDataClientObjectImpl(this, false))")
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
        TODO("APR: F64_MAX if focused object; F64_MAX/2 if selected; else sum of impl.getInterest()")
    }

    fun hasMedia(): Boolean {
        TODO("APR: any TE where te.hasMedia()")
    }

    fun isMediaDataBeingFetched(): Boolean {
        TODO("APR: sObjectMediaClient?.isInQueue(LLMediaDataClientObjectImpl(this, false)) ?: false")
    }

    fun getLastFetchedMediaVersion(): Int = lastFetchedMediaVersion

    fun addMDCImpl() { mdcImplCount++ }
    fun removeMDCImpl() { mdcImplCount-- }
    fun getMDCImplCount(): Int = mdcImplCount

    // ---- silhouette / misc ----

    fun generateSilhouette(nodep: Any?, viewPoint: FloatArray) {
        TODO("GPU: transform view point to volume space; generateSilhouetteVertices with relativeXform")
    }

    fun getApproximateFaceNormal(faceId: UByte): FloatArray {
        TODO("GPU: average normals of all vertices on face; transform to agent space; normalize")
    }

    fun updateVisualComplexity() {
        TODO("APR: getAvatarAncestor?.updateVisualComplexity(); getAvatar?.updateVisualComplexity()")
    }

    fun notifyMeshLoaded() {
        TODO("APR: sculptChanged=true; markRebuild(GEOMETRY); check skin info availability; notify avatar/controlAvatar; updateVisualComplexity")
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
        TODO("GPU: if should be rigged: create/update riggedVolume; else clear it")
    }

    fun getRiggedVolume(): RiggedVolume? = riggedVolume

    fun treatAsRigged(): Boolean {
        TODO("APR: build tools open OR (isAttachment AND attached to self AND rendered as rigged)")
    }

    fun clearRiggedVolume() {
        riggedVolume = null
    }

    // ---- LOD internals ----

    fun computeLODDetail(distance: Float, radius: Float, lodFactor: Float): Int {
        return if (dynamicLod) {
            val tanAngle = (lodFactor * radius) / distance
            TODO("APR: LLVolumeLODGroup.getDetailFromTan(round(tanAngle, 0.01))")
        } else {
            (sqrt(radius) * lodFactor * 4f).toInt().coerceIn(0, 3)
        }
    }

    fun calcLOD(): Boolean {
        TODO("GPU: determine distance and radius (avatar box for rigged); apply distance factor/ramp; computeLODDetail; return changed")
    }

    fun forceLOD(lodLevel: Int) {
        lod = lodLevel
        TODO("GPU: markRebuild(VOLUME); lodChanged = true")
    }

    private fun lodOrSculptChanged(drawable: Any?, compiled: Boolean, shouldUpdateOctreeBounds: Boolean): Boolean {
        TODO("GPU: setVolume at current LOD; if lod or sculpt changed: update face count, regenFaces if needed, unbound spatial group on sculpt change")
    }

    private fun onDrawableUpdateFromServer() {
        serverDrawableUpdateCount++
        if (serverDrawableUpdateCount > 8u) {
            TODO("GPU: mDrawable.makeActive() to avoid octree disruption from scripted updates")
        }
    }

    // Stub fields referenced by methods above
    private val mStatic: Boolean = false
    private val dynamicLod: Boolean = true
    private val LOD_HIGH: Int = 3
    private val NO_LOD: Int = -1
}
