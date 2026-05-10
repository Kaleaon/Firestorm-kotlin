package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llinventory.*

const val MAX_LOD_FACTOR: Float = 8.0f

enum class VolumeInterfaceType {
    INTERFACE_FLEXIBLE
}

interface VolumeInterface {
    fun getInterfaceType(): VolumeInterfaceType
    fun doIdleUpdate()
    fun doUpdateGeometry(drawable: Drawable): Boolean
    fun getPivotPosition(): Vector3
    fun onSetVolume(volumeParams: Any, detail: Int)
    fun onSetScale(scale: Vector3, damped: Boolean)
    fun onParameterChanged(paramType: UShort, data: Any?, inUse: Boolean, localOrigin: Boolean)
    fun onShift(shiftVector: Vector4)
    fun isVolumeUnique(): Boolean
    fun isVolumeGlobal(): Boolean
    fun isActive(): Boolean
    fun getWorldMatrix(xform: Any): Matrix4
    fun updateRelativeXform(forceIdentity: Boolean = false)
    fun getId(): UInt
    fun preRebuild()
}

open class RiggedVolume {
    companion object {
        const val UPDATE_ALL_FACES: Int = -1
        const val DO_NOT_UPDATE_FACES: Int = -2
    }

    var extraDebugText: String = ""

    fun update(
        skin: Any?,
        avatar: VOAvatar?,
        srcVolume: Any?,
        faceIndex: Int = UPDATE_ALL_FACES,
        rebuildFaceOctrees: Boolean = true
    ): Unit = TODO("GPU: rigged volume update")
}

open class VOVolume(id: LLUUID, localId: UInt, pCode: UInt) : ViewerObject(id, localId, pCode) {

    companion object {
        const val FORCE_SIMPLE_RENDER_AREA: Float = 512f
        const val FORCE_CULL_AREA: Float = 8f

        var sLODFactor: Float = 1f
        var sLODSlopDistanceFactor: Float = 0.5f
        var sDistanceFactor: Float = 1f

        private var renderComplexityLast: Int = 0
        private var renderComplexityCurrent: Int = 0

        fun getRenderComplexityMax(): Int = renderComplexityLast
        fun updateRenderComplexity() { renderComplexityLast = renderComplexityCurrent }

        fun initClass() {}
        fun cleanupClass() {}
        fun preUpdateGeom() {}

        fun getTextureCost(img: Any?): Int = TODO("GPU: texture cost")
    }

    var texAnimMode: UByte = 0u
    var lodDistance: Float = 0f
    var lodAdjustedDistance: Float = 0f
    var lodRadius: Float = 0f
    var volumeSurfaceArea: Float = -1f

    var lastRiggingInfoLod: Int = -1
    var isLocalMesh: Boolean = false
    var isLocalMeshUsingScale: Boolean = false

    private var faceMappingChanged: Boolean = false
    private var lod: Int = 0
    private var lodChanged: Boolean = false
    private var sculptChanged: Boolean = false
    private var colorChanged: Boolean = false
    private var spotLightPriority: Float = 0f
    private var relativeXform: Matrix4 = Matrix4.identity()
    private var volumeChanged: Boolean = false
    private var vobjRadius: Float = 0f
    private var volumeImpl: VolumeInterface? = null
    private val mediaImplList: MutableList<Any?> = mutableListOf()
    private var lastFetchedMediaVersion: Int = -1
    private var serverDrawableUpdateCount: UInt = 0u
    private val indexInTex: IntArray = IntArray(4)
    private var mdcImplCount: Int = 0
    private var isLightCached: Boolean = false
    private var isAnimatedObjectCached: Boolean = false
    private var riggedVolume: RiggedVolume? = null
    private var skinInfoUnavailable: Boolean = false

    override fun markDead() {
        super.markDead()
        cleanUpMediaImpls()
    }

    fun createDrawable(pipeline: Any?): Any? = TODO("GPU: createDrawable")

    fun deleteFaces() = TODO("GPU: deleteFaces")
    fun animateTextures() = TODO("GPU: animateTextures")

    fun isVisible(): Boolean = TODO("GPU: isVisible")
    override fun isActive(): Boolean = volumeImpl?.isActive() ?: false
    override fun isAttachment(): Boolean = TODO("attachment check")
    fun isRootEdit(): Boolean = TODO("root edit check")
    fun isHUDAttachment(): Boolean = TODO("HUD attachment check")

    fun getLOD(): Int = lod
    fun setNoLOD() { lod = Int.MIN_VALUE; lodChanged = true }
    fun isNoLOD(): Boolean = lod == Int.MIN_VALUE

    fun getVolumeChanged(): Boolean = volumeChanged
    fun getVObjRadius(): Float = vobjRadius

    override fun markForUpdate() = TODO("markForUpdate")
    fun faceMappingChanged() { faceMappingChanged = true }

    fun getRelativeXform(): Matrix4 = relativeXform

    fun updateRelativeXform(forceIdentity: Boolean = false): Unit = TODO("updateRelativeXform")
    fun updateGeometry(drawable: Drawable): Boolean = TODO("GPU: updateGeometry")
    fun updateFaceSize(idx: Int): Unit = TODO("GPU: updateFaceSize")
    fun updateLOD(): Boolean = TODO("updateLOD")
    fun updateRadius(): Unit = TODO("updateRadius")
    fun updateTextures(): Unit = TODO("updateTextures")
    fun updateTextureVirtualSize(forced: Boolean = false): Unit = TODO("updateTextureVirtualSize")
    fun updateSpatialExtents(min: Vector4, max: Vector4): Unit = TODO("GPU: updateSpatialExtents")
    fun getBinRadius(): Float = TODO("getBinRadius")
    fun getPartitionType(): UInt = TODO("getPartitionType")

    fun getRenderCost(textures: MutableSet<Any>): UInt = TODO("getRenderCost")
    fun getEstTrianglesMax(): Float = TODO("getEstTrianglesMax")
    fun getTriangleCount(vcount: IntArray? = null): UInt = TODO("getTriangleCount")
    fun getHighLODTriangleCount(): UInt = TODO("getHighLODTriangleCount")
    fun getLODTriangleCount(lod: Int): UInt = TODO("getLODTriangleCount")

    fun agentPositionToVolume(pos: Vector3): Vector3 = TODO("agentPositionToVolume")
    fun agentDirectionToVolume(dir: Vector3): Vector3 = TODO("agentDirectionToVolume")
    fun volumePositionToAgent(dir: Vector3): Vector3 = TODO("volumePositionToAgent")
    fun volumeDirectionToAgent(dir: Vector3): Vector3 = TODO("volumeDirectionToAgent")

    // Light API
    fun setIsLight(isLight: Boolean): Unit = TODO("setIsLight")
    fun setLightSRGBColor(color: Color4): Unit = TODO("setLightSRGBColor")
    fun setLightLinearColor(color: Color4): Unit = TODO("setLightLinearColor")
    fun setLightIntensity(intensity: Float): Unit = TODO("setLightIntensity")
    fun setLightRadius(radius: Float): Unit = TODO("setLightRadius")
    fun setLightFalloff(falloff: Float): Unit = TODO("setLightFalloff")
    fun setLightCutoff(cutoff: Float): Unit = TODO("setLightCutoff")
    fun setLightTextureID(id: LLUUID): Unit = TODO("setLightTextureID")
    fun setSpotLightParams(params: Vector3): Unit = TODO("setSpotLightParams")

    fun getIsLight(): Boolean = TODO("getIsLight")
    fun getIsLightFast(): Boolean = isLightCached
    fun getLightSRGBBaseColor(): Color4 = TODO("getLightSRGBBaseColor")
    fun getLightLinearBaseColor(): Color4 = TODO("getLightLinearBaseColor")
    fun getLightLinearColor(): Color4 = TODO("getLightLinearColor")
    fun getLightSRGBColor(): Color4 = TODO("getLightSRGBColor")
    fun getLightTextureID(): LLUUID = TODO("getLightTextureID")
    fun isLightSpotlight(): Boolean = TODO("isLightSpotlight")
    fun getSpotLightParams(): Vector3 = TODO("getSpotLightParams")
    fun updateSpotLightPriority(): Unit = TODO("updateSpotLightPriority")
    fun getSpotLightPriority(): Float = spotLightPriority
    fun getLightIntensity(): Float = TODO("getLightIntensity")
    fun getLightRadius(): Float = TODO("getLightRadius")
    fun getLightFalloff(fudgeFactor: Float = 1f): Float = TODO("getLightFalloff")
    fun getLightCutoff(): Float = TODO("getLightCutoff")

    // Reflection Probe API
    fun setIsReflectionProbe(isProbe: Boolean): Boolean = TODO("setIsReflectionProbe")
    fun setReflectionProbeAmbiance(ambiance: Float): Boolean = TODO("setReflectionProbeAmbiance")
    fun setReflectionProbeNearClip(nearClip: Float): Boolean = TODO("setReflectionProbeNearClip")
    fun setReflectionProbeIsBox(isBox: Boolean): Boolean = TODO("setReflectionProbeIsBox")
    fun setReflectionProbeIsDynamic(isDynamic: Boolean): Boolean = TODO("setReflectionProbeIsDynamic")
    fun setReflectionProbeIsMirror(isMirror: Boolean): Boolean = TODO("setReflectionProbeIsMirror")
    fun isReflectionProbe(): Boolean = TODO("isReflectionProbe")
    fun getReflectionProbeAmbiance(): Float = TODO("getReflectionProbeAmbiance")
    fun getReflectionProbeNearClip(): Float = TODO("getReflectionProbeNearClip")
    fun getReflectionProbeIsBox(): Boolean = TODO("getReflectionProbeIsBox")
    fun getReflectionProbeIsDynamic(): Boolean = TODO("getReflectionProbeIsDynamic")
    fun getReflectionProbeIsMirror(): Boolean = TODO("getReflectionProbeIsMirror")

    // Volume type queries
    fun isFlexible(): Boolean = volumeImpl?.getInterfaceType() == VolumeInterfaceType.INTERFACE_FLEXIBLE
    fun isFlexibleFast(): Boolean = isFlexible()
    fun isSculpted(): Boolean = TODO("isSculpted")
    fun isSculptedFast(): Boolean = isSculpted()
    fun isMesh(): Boolean = TODO("isMesh")
    fun isMeshFast(): Boolean = isMesh()
    fun isRiggedMesh(): Boolean = TODO("isRiggedMesh")
    fun isRiggedMeshFast(): Boolean = isRiggedMesh()
    fun isAnimatedObject(): Boolean = TODO("isAnimatedObject")
    fun isAnimatedObjectFast(): Boolean = isAnimatedObjectCached
    fun isVolumeGlobal(): Boolean = volumeImpl?.isVolumeGlobal() ?: false
    fun canBeFlexible(): Boolean = TODO("canBeFlexible")
    fun setIsFlexible(isFlexible: Boolean): Boolean = TODO("setIsFlexible")
    fun hasLightTexture(): Boolean = TODO("hasLightTexture")

    fun getVolumeInterfaceId(): UInt = volumeImpl?.getId() ?: 0u
    fun getSkinInfoUnavailable(): Boolean = skinInfoUnavailable
    fun getExtendedMeshFlags(): UInt = TODO("getExtendedMeshFlags")
    fun setExtendedMeshFlags(flags: UInt): Unit = TODO("setExtendedMeshFlags")
    fun canBeAnimatedObject(): Boolean = TODO("canBeAnimatedObject")

    fun updateRiggingInfo(): Unit = TODO("updateRiggingInfo")

    // Media
    fun hasMedia(): Boolean = TODO("hasMedia")
    fun getTotalMediaInterest(): Double = TODO("getTotalMediaInterest")
    fun getLastFetchedMediaVersion(): Int = lastFetchedMediaVersion
    fun addMDCImpl() { mdcImplCount++ }
    fun removeMDCImpl() { mdcImplCount-- }
    fun getMDCImplCount(): Int = mdcImplCount
    fun mediaNavigateBounceBack(textureIndex: UByte): Unit = TODO("mediaNavigateBounceBack")
    fun updateObjectMediaData(mediaDataArray: Any, mediaVersion: String): Unit = TODO("updateObjectMediaData")
    fun getApproximateFaceNormal(faceId: UByte): Vector3 = TODO("getApproximateFaceNormal")

    fun updateVisualComplexity(): Unit = TODO("updateVisualComplexity")
    fun notifyMeshLoaded(): Unit = TODO("notifyMeshLoaded")
    fun notifySkinInfoLoaded(skin: Any?): Unit = TODO("notifySkinInfoLoaded")
    fun notifySkinInfoUnavailable() { skinInfoUnavailable = true }

    fun getRiggedVolume(): RiggedVolume? = riggedVolume
    fun treatAsRigged(): Boolean = TODO("treatAsRigged")
    fun clearRiggedVolume() { riggedVolume = null }
    fun updateRiggedVolume(
        forceTreatAsRigged: Boolean,
        faceIndex: Int = RiggedVolume.UPDATE_ALL_FACES,
        rebuildFaceOctrees: Boolean = true
    ): Unit = TODO("GPU: updateRiggedVolume")

    fun forceLOD(lod: Int) { this.lod = lod; lodChanged = true }

    private fun cleanUpMediaImpls() = TODO("cleanUpMediaImpls")
}
