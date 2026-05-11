package com.firestorm.newview

import java.util.UUID

const val MIN_ALPHA_SIZE: Float = 1024.0f
const val MIN_TEX_ANIM_SIZE: Float = 16.0f
const val FACE_DO_NOT_BATCH_TEXTURES: UByte = 255u

fun planarProjection(
    normal: FloatArray,
    center: FloatArray,
    vec: FloatArray
): FloatArray {
    val binormal = FloatArray(4)
    val d = normal[0]
    if (d >= 0.5f || d <= -0.5f) {
        if (d < 0) { binormal[0] = 0f; binormal[1] = -1f; binormal[2] = 0f }
        else       { binormal[0] = 0f; binormal[1] =  1f; binormal[2] = 0f }
    } else {
        if (normal[1] > 0) { binormal[0] = -1f; binormal[1] = 0f; binormal[2] = 0f }
        else               { binormal[0] =  1f; binormal[1] = 0f; binormal[2] = 0f }
    }
    val tangent = floatArrayOf(
        binormal[1] * normal[2] - binormal[2] * normal[1],
        binormal[2] * normal[0] - binormal[0] * normal[2],
        binormal[0] * normal[1] - binormal[1] * normal[0]
    )
    val tdotv = tangent[0]*vec[0] + tangent[1]*vec[1] + tangent[2]*vec[2]
    val bdotv = binormal[0]*vec[0] + binormal[1]*vec[1] + binormal[2]*vec[2]
    return floatArrayOf(
        1.0f + (bdotv * 2 - 0.5f),
        -((tdotv * 2 - 0.5f))
    )
}

class LLFace(
    drawablep: LLDrawable,
    objp: LLViewerObject
) {
    enum class EMasks(val bit: Int) {
        LIGHT(0x0001),
        GLOBAL(0x0002),
        FULLBRIGHT(0x0004),
        HUD_RENDER(0x0008),
        USE_FACE_COLOR(0x0010),
        TEXTURE_ANIM(0x0020),
        RIGGED(0x0040),
        PARTICLE(0x0080)
    }

    val NUM_TEXTURE_CHANNELS = 4

    var mCenterLocal: FloatArray = FloatArray(3)
    var mCenterAgent: FloatArray = FloatArray(3)
    var mTexExtents: Array<FloatArray> = arrayOf(floatArrayOf(0f, 0f), floatArrayOf(1f, 1f))
    var mDistance: Float = 0f
    var mLastUpdateTime: Float = 0f
    var mLastSkinTime: Float = 0f
    var mLastMoveTime: Float = 0f
    var mTextureMatrix: Any? = null
    var mSpecMapMatrix: Any? = null
    var mNormalMapMatrix: Any? = null
    var mDrawInfo: Any? = null
    var mAvatar: LLVOAvatar? = null
    var mSkinInfo: LLMeshSkinInfo? = null
    var mInFrustum: Boolean = false
    var mLastTextureUpdate: UInt = 0u

    var mExtents: Array<FloatArray> = arrayOf(FloatArray(4), FloatArray(4))
    var mRiggedExtents: Array<FloatArray> = arrayOf(floatArrayOf(0f,0f,0f,0f), floatArrayOf(0f,0f,0f,0f))

    private var mVertexBuffer: Any? = null
    private var mVertexBufferGLTF: Any? = null
    private var mState: Int = EMasks.GLOBAL.bit
    private var mDrawPoolp: LLFacePool? = null
    private var mPoolType: UInt = 0u
    private var mFaceColor: FloatArray = floatArrayOf(1f, 0f, 0f, 1f)
    private var mGeomCount: Int = 0
    private var mGeomIndex: Int = 0
    private var mTextureIndex: UByte = FACE_DO_NOT_BATCH_TEXTURES
    private var mIndicesCount: UInt = 0u
    private var mIndicesIndex: UInt = 0xFFFFFFFFu
    private val mIndexInTex: IntArray = IntArray(NUM_TEXTURE_CHANNELS)
    private var mXform: Any? = null
    private val mTexture: Array<LLViewerTexture?> = arrayOfNulls(NUM_TEXTURE_CHANNELS)
    private var mDrawablep: LLDrawable? = null
    private var mVObjp: LLViewerObject? = null
    private var mTEOffset: Int = -1
    private var mReferenceIndex: Int = -1
    private val mRiggedIndex: MutableList<Int> = mutableListOf()
    private var mLastPixelAreaUpdate: Float = 0f
    private var mVSize: Float = 0f
    private var mPixelArea: Float = 16f
    private var mImportanceToCamera: Float = 0f
    private var mBoundingSphereRadius: Float = 0f
    private var mHasMedia: Boolean = false
    private var mIsMediaAllowed: Boolean = true
    private var mDrawOrderIndex: UInt = 0u
    private var mShowDiffTexture: Boolean = true
    private var mOrigDiffTexture: LLViewerTexture? = null

    init {
        init(drawablep, objp)
    }

    fun init(drawablep: LLDrawable, objp: LLViewerObject) {
        mLastUpdateTime = frameTimeSeconds()
        mLastMoveTime = 0f
        mLastSkinTime = frameTimeSeconds()
        mVSize = 0f
        mPixelArea = 16f
        mState = EMasks.GLOBAL.bit
        mDrawPoolp = null
        mPoolType = 0u
        mCenterLocal = objp.getPosition()
        mCenterAgent = drawablep.getPositionAgent()
        mDistance = 0f
        mGeomCount = 0
        mGeomIndex = 0
        mIndicesCount = 0u
        mIndicesIndex = 0xFFFFFFFFu
        for (i in 0 until NUM_TEXTURE_CHANNELS) {
            mIndexInTex[i] = 0
            mTexture[i] = null
        }
        mTEOffset = -1
        mTextureIndex = FACE_DO_NOT_BATCH_TEXTURES
        setDrawable(drawablep)
        mVObjp = objp
        mReferenceIndex = -1
        mTextureMatrix = null
        mDrawInfo = null
        mFaceColor = floatArrayOf(1f, 0f, 0f, 1f)
        mImportanceToCamera = 0f
        mBoundingSphereRadius = 0f
        mTexExtents[0] = floatArrayOf(0f, 0f)
        mTexExtents[1] = floatArrayOf(1f, 1f)
        mHasMedia = false
        mIsMediaAllowed = true
        mShowDiffTexture = true
    }

    fun destroy() {
        for (i in 0 until NUM_TEXTURE_CHANNELS) {
            mTexture[i]?.removeFace(i, this)
            mTexture[i] = null
        }
        if (isState(EMasks.PARTICLE.bit)) clearState(EMasks.PARTICLE.bit)
        mDrawPoolp?.removeFace(this)
        mDrawPoolp = null
        if (mTextureMatrix != null) {
            mTextureMatrix = null
            mDrawablep?.getSpatialGroup()?.let { group ->
                group.dirtyGeom()
                TODO("GPU: markRebuild(group)")
            }
        }
        setDrawInfo(null)
        mDrawablep = null
        mVObjp = null
    }

    fun setWorldMatrix(mat: Any) {
        error("Faces on this drawable are not independently modifiable")
    }

    fun setPool(pool: LLFacePool) {
        mDrawPoolp = pool
    }

    fun setPool(newPool: LLFacePool, texturep: LLViewerTexture?) {
        if (newPool != mDrawPoolp) {
            mDrawPoolp?.let { old ->
                old.removeFace(this)
                mDrawablep?.let { TODO("GPU: markRebuild REBUILD_ALL") }
            }
            mGeomIndex = 0
            newPool.addFace(this)
            mDrawPoolp = newPool
        }
        setTexture(texturep)
    }

    fun setTexture(ch: Int, tex: LLViewerTexture?) {
        if (ch == 0 && !mShowDiffTexture) {
            mOrigDiffTexture = tex
            return
        }
        if (mTexture[ch] == tex) return
        mTexture[ch]?.removeFace(ch, this)
        tex?.addFace(ch, this)
        mTexture[ch] = tex
    }

    fun setTexture(tex: LLViewerTexture?) = setDiffuseMap(tex)
    fun setDiffuseMap(tex: LLViewerTexture?) = setTexture(0, tex)
    fun setAlternateDiffuseMap(tex: LLViewerTexture?) = setTexture(3, tex)
    fun setNormalMap(tex: LLViewerTexture?) = setTexture(1, tex)
    fun setSpecularMap(tex: LLViewerTexture?) = setTexture(2, tex)

    fun dirtyTexture() {
        val drawablep = getDrawable() ?: return
        if (mVObjp?.getVolume() != null) {
            for (ch in 0 until NUM_TEXTURE_CHANNELS) {
                if (mTexture[ch] != null && mTexture[ch]!!.getComponents() == 4) {
                    TODO("GPU: markRebuild REBUILD_VOLUME / update LOD")
                }
            }
        }
        TODO("GPU: pipeline.markTextured(drawablep)")
    }

    fun switchTexture(ch: Int, newTexture: LLViewerTexture) {
        if (mTexture[ch] == newTexture) return
        if (ch == 0) {
            getViewerObject()?.changeTEImage(mTEOffset, newTexture)
        }
        setTexture(ch, newTexture)
        dirtyTexture()
    }

    fun setTEOffset(teOffset: Int) { mTEOffset = teOffset }

    fun setFaceColor(color: FloatArray) {
        mFaceColor = color
        setState(EMasks.USE_FACE_COLOR.bit)
    }

    fun unsetFaceColor() = clearState(EMasks.USE_FACE_COLOR.bit)
    fun getFaceColor(): FloatArray = mFaceColor

    fun setDrawable(drawable: LLDrawable) {
        mDrawablep = drawable
        mXform = drawable.mXform
    }

    fun setSize(numVertices: Int, numIndices: Int = 0, align: Boolean = false) {
        val verts = if (align) (numVertices + 0x3) and 0x3.inv() else numVertices
        if (mGeomCount != verts || mIndicesCount != numIndices.toUInt()) {
            mGeomCount = verts
            mIndicesCount = numIndices.toUInt()
            mVertexBuffer = null
        }
    }

    fun setGeomIndex(idx: Int) {
        if (mGeomIndex != idx) { mGeomIndex = idx; mVertexBuffer = null }
    }

    fun setTextureIndex(index: UByte) {
        if (index != mTextureIndex) {
            mTextureIndex = index
            if (mTextureIndex != FACE_DO_NOT_BATCH_TEXTURES) {
                mDrawablep?.setState(LLDrawable.REBUILD_POSITION)
            }
        }
    }

    fun setIndicesIndex(idx: Int) {
        if (mIndicesIndex != idx.toUInt()) { mIndicesIndex = idx.toUInt(); mVertexBuffer = null }
    }

    fun getState(): Int = mState
    fun setState(state: Int) { mState = mState or state }
    fun clearState(state: Int) { mState = mState and state.inv() }
    fun isState(state: Int): Boolean = (mState and state) != 0

    fun setVirtualSize(size: Float) { mVSize = size }
    fun setPixelArea(area: Float) { mPixelArea = area }
    fun getVirtualSize(): Float = mVSize
    fun getPixelArea(): Float = mPixelArea

    fun getXform(): Any? = mXform
    fun hasGeometry(): Boolean = mGeomCount > 0
    fun getIndicesCount(): UInt = mIndicesCount
    fun getIndicesStart(): Int = mIndicesIndex.toInt()
    fun getGeomCount(): Int = mGeomCount
    fun getGeomIndex(): Int = mGeomIndex
    fun getGeomStart(): Int = mGeomIndex
    fun getTextureIndex(): UByte = mTextureIndex
    fun getIndexInTex(ch: Int): Int = mIndexInTex[ch]
    fun setIndexInTex(ch: Int, index: Int) { mIndexInTex[ch] = index }
    fun getPool(): LLFacePool? = mDrawPoolp
    fun getPoolType(): UInt = mPoolType
    fun getDrawable(): LLDrawable? = mDrawablep
    fun getViewerObject(): LLViewerObject? = mVObjp
    fun getLOD(): Int = mVObjp?.getLOD() ?: 0
    fun setPoolType(type: UInt) { mPoolType = type }
    fun getTEOffset(): Int = mTEOffset
    fun getTexture(ch: Int = 0): LLViewerTexture? = mTexture[ch]
    fun getTextureEntry(): LLTextureEntry? = mVObjp?.getTE(mTEOffset)
    fun getKey(): Float = mDistance
    fun getReferenceIndex(): Int = mReferenceIndex
    fun setReferenceIndex(index: Int) { mReferenceIndex = index }
    fun setDrawInfo(drawInfo: Any?) { mDrawInfo = drawInfo }
    fun setHasMedia(hasMedia: Boolean) { mHasMedia = hasMedia }
    fun hasMedia(): Boolean = mHasMedia
    fun setMediaAllowed(isMediaAllowed: Boolean) { mIsMediaAllowed = isMediaAllowed }
    fun isMediaAllowed(): Boolean = mIsMediaAllowed
    fun setDrawOrderIndex(index: UInt) { mDrawOrderIndex = index }
    fun getDrawOrderIndex(): UInt = mDrawOrderIndex

    fun setVertexBuffer(buffer: Any?) { mVertexBuffer = buffer }
    fun clearVertexBuffer() { mVertexBuffer = null }
    fun getVertexBuffer(): Any? = mVertexBuffer

    fun getRiggedIndex(type: UInt): Int =
        if (type.toInt() < mRiggedIndex.size) mRiggedIndex[type.toInt()] else -1

    fun getSkinHash(): ULong = mSkinInfo?.mHash ?: 0uL

    fun isInAlphaPool(): Boolean = TODO("GPU: check draw pool type")

    fun updateCenterAgent() {
        mCenterAgent = if (mDrawablep?.isActive() == true) {
            transformVector(mCenterLocal, getRenderMatrix())
        } else {
            mCenterLocal.copyOf()
        }
    }

    fun getRenderMatrix(): Any = TODO("GPU: getRenderMatrix")

    fun getPositionAgent(): FloatArray = TODO("GPU: getPositionAgent")

    fun renderSelected(imagep: LLViewerTexture, color: FloatArray) {
        TODO("GPU: render selected face with texture")
    }

    fun renderIndexed() {
        TODO("GPU: renderIndexed")
    }

    fun renderOneWireframe(color: FloatArray, fogCfx: Float, wireframeSelection: Boolean, bRenderHiddenSelections: Boolean, shader: Boolean) {
        TODO("GPU: renderOneWireframe")
    }

    fun renderSelectedUV() {
        TODO("GPU: renderSelectedUV")
    }

    fun updateRebuildFlags() {
        TODO("GPU: updateRebuildFlags")
    }

    fun canRenderAsMask(): Boolean {
        TODO("GPU: canRenderAsMask")
    }

    fun getGeometryVolume(
        volume: LLVolume,
        faceIndex: Int,
        matVert: Any,
        matNormal: Any,
        indexOffset: Int,
        forceRebuild: Boolean = false,
        noDebugAssert: Boolean = false,
        rebuildForGltf: Boolean = false
    ): Boolean {
        TODO("GPU: getGeometryVolume")
    }

    fun getGeometryAvatar(
        vertices: Any, normals: Any, texCoords: Any,
        vertexWeights: Any, clothingWeights: Any
    ): Int {
        TODO("GPU: getGeometryAvatar via vertex buffer stribers")
    }

    fun getGeometry(vertices: Any, normals: Any, texCoords: Any, indices: Any): Int {
        TODO("GPU: getGeometry via vertex buffer stribers")
    }

    fun getColors(colors: Any): Int {
        TODO("GPU: getColors strider")
    }

    fun getIndices(indices: Any): Int {
        TODO("GPU: getIndices strider")
    }

    fun genVolumeBBoxes(volume: LLVolume, f: Int, matVertIn: Any, globalVolume: Boolean = false): Boolean {
        TODO("GPU: genVolumeBBoxes")
    }

    fun update() {
        TODO("GPU: update face")
    }

    fun getTextureVirtualSize(): Float {
        TODO("GPU: getTextureVirtualSize via pixel area calculation")
    }

    fun resetVirtualSize() {
        mVSize = 0f
        mPixelArea = 0f
    }

    fun isDefaultTexture(nChannel: UInt): Boolean {
        TODO("GPU: isDefaultTexture check")
    }

    fun setDefaultTexture(nChannel: UInt, fShowDefault: Boolean) {
        TODO("GPU: setDefaultTexture toggle")
    }

    fun verify(indicesArray: IntArray? = null): Boolean {
        TODO("GPU: verify geometry consistency")
    }

    fun printDebugInfo() {
        TODO("GPU: printDebugInfo - dump face state to log")
    }

    fun surfaceToTexture(surfaceCoord: FloatArray, position: FloatArray, normal: FloatArray): FloatArray {
        TODO("GPU: surfaceToTexture projection")
    }

    fun getPlanarProjectedParams(faceRot: Any, facePos: FloatArray, scale: FloatArray) {
        TODO("GPU: getPlanarProjectedParams")
    }

    fun calcAlignedPlanarTE(alignTo: LLFace, stOffset: FloatArray, stScale: FloatArray, stRot: FloatArray, map: Int = 0): Boolean {
        TODO("GPU: calcAlignedPlanarTE")
    }

    fun setViewerObject(obj: LLViewerObject) { mVObjp = obj }

    companion object {
        var sSafeRenderSelect: Boolean = true

        fun calcImportanceToCamera(toViewDir: Float, dist: Float): Float {
            TODO("GPU: calcImportanceToCamera")
        }

        fun adjustPixelArea(importance: Float, pixelArea: Float): Float {
            TODO("GPU: adjustPixelArea")
        }
    }

    class CompareDistanceGreater : Comparator<LLFace?> {
        override fun compare(lhs: LLFace?, rhs: LLFace?): Int {
            if (lhs == null) return 1
            if (rhs == null) return -1
            return rhs.mDistance.compareTo(lhs.mDistance)
        }
    }

    class CompareTexture : Comparator<LLFace> {
        override fun compare(lhs: LLFace, rhs: LLFace): Int =
            compareValuesBy(lhs, rhs) { it.getTexture() }
    }

    class CompareTextureAndGeomCount : Comparator<LLFace> {
        override fun compare(lhs: LLFace, rhs: LLFace): Int =
            if (lhs.getTexture() == rhs.getTexture())
                lhs.getGeomCount().compareTo(rhs.getGeomCount())
            else
                compareValuesBy(rhs, lhs) { it.getTexture() }
    }

    class CompareTextureAndLOD : Comparator<LLFace> {
        override fun compare(lhs: LLFace, rhs: LLFace): Int =
            if (lhs.getTexture() == rhs.getTexture())
                lhs.getLOD().compareTo(rhs.getLOD())
            else
                compareValuesBy(lhs, rhs) { it.getTexture() }
    }

    class CompareTextureAndTime : Comparator<LLFace> {
        override fun compare(lhs: LLFace, rhs: LLFace): Int =
            if (lhs.getTexture() == rhs.getTexture())
                lhs.mLastUpdateTime.compareTo(rhs.mLastUpdateTime)
            else
                compareValuesBy(lhs, rhs) { it.getTexture() }
    }
}

private fun frameTimeSeconds(): Float = TODO("APR: use JVM equivalent for gFrameTimeSeconds")
private fun transformVector(v: FloatArray, matrix: Any): FloatArray = TODO("GPU: matrix-vector multiply")
