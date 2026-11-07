package com.firestorm.newview

import java.util.UUID
import java.util.concurrent.Future
import java.util.concurrent.Executors
import java.util.concurrent.Callable
import java.nio.file.Files
import java.nio.file.Paths

const val LOCAL_NUM_LODS = 4
const val LL_SCULPT_MESH_MAX_FACES = 8

enum class LocalMeshFileLOD {
    LOCAL_LOD_LOWEST,
    LOCAL_LOD_LOW,
    LOCAL_LOD_MEDIUM,
    LOCAL_LOD_HIGH;
    override fun toString() = ordinal.toString()
}

data class Vector4(val x: Float, val y: Float, val z: Float, val w: Float = 0f) {
    operator fun get(i: Int) = when (i) { 0 -> x; 1 -> y; 2 -> z; else -> w }
    operator fun plus(other: Vector4) = Vector4(x + other.x, y + other.y, z + other.z, w + other.w)
    operator fun minus(other: Vector4) = Vector4(x - other.x, y - other.y, z - other.z, w - other.w)
    operator fun times(s: Float) = Vector4(x * s, y * s, z * s, w * s)
    fun isExactlyZero() = x == 0f && y == 0f && z == 0f && w == 0f
    fun normalize(): Vector4 {
        val len = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        return if (len == 0f) this else Vector4(x / len, y / len, z / len, w)
    }
}

data class Vector2(val u: Float, val v: Float) {
    operator fun get(i: Int) = if (i == 0) u else v
}

data class MeshSkinInfo(
    val id: UUID = UUID.randomUUID(),
    var jointNames: MutableList<String> = mutableListOf(),
    var jointNums: MutableList<Int> = mutableListOf(),
    var invBindMatrix: MutableList<FloatArray> = mutableListOf(),
    var alternateBindMatrix: MutableList<FloatArray> = mutableListOf(),
    var bindPoseMatrix: MutableList<FloatArray> = mutableListOf(),
    var bindShapeMatrix: FloatArray = FloatArray(16).also { it[0] = 1f; it[5] = 1f; it[10] = 1f; it[15] = 1f },
    var invalidJointsScrubbed: Boolean = false,
    var jointNumsInitialized: Boolean = false,
    var meshId: UUID = UUID.randomUUID(),
    var hash: Long = 0L
) {
    fun updateHash() {
        System.err.println("MeshSkinInfo: updateHash not yet implemented")
    }
}

data class LLJointData(
    val name: String,
    val isJoint: Boolean,
    val restMatrix: FloatArray,
    val jointMatrix: FloatArray,
    val support: Int,
    val children: List<LLJointData>
) {
    companion object { const val SUPPORT_BASE = 0 }
}

class LLLocalMeshFace {
    data class LLLocalMeshSkinUnit(
        val jointIndices: IntArray = IntArray(4) { -1 },
        val jointWeights: FloatArray = FloatArray(4)
    )

    private val mIndices: MutableList<Int> = mutableListOf()
    private val mPositions: MutableList<FloatArray> = mutableListOf()
    private val mNormals: MutableList<FloatArray> = mutableListOf()
    private val mUVs: MutableList<FloatArray> = mutableListOf()
    private val mSkin: MutableList<LLLocalMeshSkinUnit> = mutableListOf()
    private var mFaceBoundingBoxMin: FloatArray = FloatArray(4)
    private var mFaceBoundingBoxMax: FloatArray = FloatArray(4)

    fun setFaceBoundingBox(dataIn: FloatArray, initialValues: Boolean = false) {
        if (initialValues) {
            mFaceBoundingBoxMin = dataIn.copyOf()
            mFaceBoundingBoxMax = dataIn.copyOf()
            return
        }
        for (i in 0 until 4) {
            if (dataIn[i] < mFaceBoundingBoxMin[i]) mFaceBoundingBoxMin[i] = dataIn[i]
            if (dataIn[i] > mFaceBoundingBoxMax[i]) mFaceBoundingBoxMax[i] = dataIn[i]
        }
    }

    fun getNumVerts(): Int = mPositions.size
    fun getNumIndices(): Int = mIndices.size
    fun getIndices(): MutableList<Int> = mIndices
    fun getPositions(): MutableList<FloatArray> = mPositions
    fun getNormals(): MutableList<FloatArray> = mNormals
    fun getUVs(): MutableList<FloatArray> = mUVs
    fun getSkin(): MutableList<LLLocalMeshSkinUnit> = mSkin
    fun getFaceBoundingBox(): Pair<FloatArray, FloatArray> = mFaceBoundingBoxMin to mFaceBoundingBoxMax

    fun clone(): LLLocalMeshFace {
        val cloned = LLLocalMeshFace()
        cloned.mIndices.addAll(mIndices)
        cloned.mPositions.addAll(mPositions.map { it.copyOf() })
        cloned.mNormals.addAll(mNormals.map { it.copyOf() })
        cloned.mUVs.addAll(mUVs.map { it.copyOf() })
        cloned.mSkin.addAll(mSkin.map { it.copy(jointIndices = it.jointIndices.copyOf(), jointWeights = it.jointWeights.copyOf()) })
        cloned.mFaceBoundingBoxMin = mFaceBoundingBoxMin.copyOf()
        cloned.mFaceBoundingBoxMax = mFaceBoundingBoxMax.copyOf()
        return cloned
    }

    fun logFaceInfo() {
        println("[LocalMesh] LLLocalMeshFace:")
        println("[LocalMesh]   bounding box: ${mFaceBoundingBoxMin.toList()} - ${mFaceBoundingBoxMax.toList()}")
        println("[LocalMesh]   indices: ${mIndices.joinToString { "[$it]" }}")
        println("[LocalMesh]   positions: [${mPositions.joinToString { it.toList().toString() }}]")
        println("[LocalMesh]   uvs: [${mUVs.joinToString { it.toList().toString() }}]")
        println("[LocalMesh]   normals: [${mNormals.joinToString { it.toList().toString() }}]")
        mSkin.forEachIndexed { i, unit ->
            println("[LocalMesh]   skin[$i]:")
            for (j in 0 until 4) {
                println("[LocalMesh]     $j: [${unit.jointIndices[j]}] = ${unit.jointWeights[j]}")
            }
        }
    }
}

class LLLocalMeshObject(name: String) {
    private val mFaces: Array<MutableList<LLLocalMeshFace>> = Array(LOCAL_NUM_LODS) { mutableListOf() }
    private var mObjectBoundingBoxMin: FloatArray = FloatArray(4)
    private var mObjectBoundingBoxMax: FloatArray = FloatArray(4)
    private val mObjectName: String = name
    private var mObjectTranslation: FloatArray = FloatArray(4)
    private var mObjectSize: FloatArray = FloatArray(4)
    private var mObjectScale: FloatArray = FloatArray(4)
    private var mMeshSkinInfoPtr: MeshSkinInfo? = null
    private val mSculptId: UUID = UUID.randomUUID()
    private var mVolumeParams: VolumeParams = VolumeParams(mSculptId)

    init {
        mVolumeParams = VolumeParams(mSculptId)
    }

    fun computeObjectBoundingBox() {
        val lod3Faces = mFaces[3]
        if (lod3Faces.isEmpty()) return

        val initBbox = lod3Faces[0].getFaceBoundingBox()
        mObjectBoundingBoxMin = initBbox.first.copyOf()
        mObjectBoundingBoxMax = initBbox.second.copyOf()

        for (faceIdx in 1 until lod3Faces.size) {
            val (bMin, bMax) = lod3Faces[faceIdx].getFaceBoundingBox()
            for (i in 0 until 4) {
                if (bMin[i] < mObjectBoundingBoxMin[i]) mObjectBoundingBoxMin[i] = bMin[i]
                if (bMax[i] > mObjectBoundingBoxMax[i]) mObjectBoundingBoxMax[i] = bMax[i]
            }
        }
    }

    fun computeObjectTransform(sceneTransform: FloatArray) {
        mObjectTranslation = FloatArray(4) { i -> -(mObjectBoundingBoxMin[i] + mObjectBoundingBoxMax[i]) * 0.5f }

        mObjectSize = FloatArray(4) { i -> mObjectBoundingBoxMax[i] - mObjectBoundingBoxMin[i] }

        val approxZero = 1e-6f
        for (i in 0 until 3) {
            if (mObjectSize[i] <= approxZero) mObjectSize[i] = 1.0f
        }

        mObjectScale = FloatArray(4) { 1f }
        for (i in 0 until 3) mObjectScale[i] = 1f / mObjectSize[i]
        mObjectScale[3] = 1f
    }

    fun normalizeFaceValues(lodIter: LocalMeshFileLOD) {
        val lodFaces = mFaces[lodIter.ordinal]
        if (lodFaces.isEmpty()) return

        for (face in lodFaces) {
            val bbox = face.getFaceBoundingBox()
            for (i in 0 until 4) {
                bbox.first[i] += mObjectTranslation[i]
                bbox.second[i] += mObjectTranslation[i]
            }
            for (i in 0 until 3) {
                bbox.first[i] *= mObjectScale[i]
                bbox.second[i] *= mObjectScale[i]
            }

            val positions = face.getPositions()
            for (pos in positions) {
                for (i in 0 until 4) pos[i] += mObjectTranslation[i]
                for (i in 0 until 3) pos[i] *= mObjectScale[i]
            }

            val normals = face.getNormals()
            if (normals.size != positions.size) continue
            for (norm in normals) {
                if (norm.all { it == 0f }) continue
                for (i in 0 until 3) norm[i] *= mObjectSize[i]
                val len = kotlin.math.sqrt((norm[0] * norm[0] + norm[1] * norm[1] + norm[2] * norm[2]).toDouble()).toFloat()
                if (len > 0f) for (i in 0 until 3) norm[i] /= len
            }
        }
    }

    fun fillVolume(lod: LocalMeshFileLOD) {
        if (mFaces[lod.ordinal].isEmpty()) return
        System.err.println("LLLocalMeshObject: fillVolume not yet implemented")
    }

    fun attachSkinInfo() {
        System.err.println("LLLocalMeshObject: attachSkinInfo not yet implemented")
    }

    fun getFaces(lod: LocalMeshFileLOD): MutableList<LLLocalMeshFace> = mFaces[lod.ordinal]
    fun getObjectBoundingBox(): Pair<FloatArray, FloatArray> = mObjectBoundingBoxMin to mObjectBoundingBoxMax
    fun getObjectTranslation(): FloatArray = mObjectTranslation
    fun getObjectName(): String = mObjectName
    fun getObjectSize(): FloatArray = mObjectSize
    fun getObjectScale(): FloatArray = mObjectScale
    fun getObjectMeshSkinInfo(): MeshSkinInfo? = mMeshSkinInfoPtr
    fun setObjectMeshSkinInfo(info: MeshSkinInfo?) { mMeshSkinInfoPtr = info }
    fun getVolumeParams(): VolumeParams = mVolumeParams

    fun getIsRiggedObject(): Boolean {
        val mainLodFaces = mFaces[LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal]
        if (mainLodFaces.isEmpty()) return false
        return mainLodFaces[0].getSkin().isNotEmpty()
    }

    fun clone(): LLLocalMeshObject {
        val cloned = LLLocalMeshObject(mObjectName)
        for (lod in LocalMeshFileLOD.values()) {
            val dest = cloned.getFaces(lod)
            dest.addAll(mFaces[lod.ordinal].map { it.clone() })
        }
        cloned.mObjectBoundingBoxMin = mObjectBoundingBoxMin.copyOf()
        cloned.mObjectBoundingBoxMax = mObjectBoundingBoxMax.copyOf()
        cloned.mObjectTranslation = mObjectTranslation.copyOf()
        cloned.mObjectSize = mObjectSize.copyOf()
        cloned.mObjectScale = mObjectScale.copyOf()
        cloned.mVolumeParams = mVolumeParams.copy(sculptId = mSculptId)
        if (mMeshSkinInfoPtr != null) {
            cloned.mMeshSkinInfoPtr = mMeshSkinInfoPtr!!.copy()
        }
        return cloned
    }

    fun logObjectInfo() {
        println("[LocalMesh] LLLocalMeshObject: $mObjectName")
        println("[LocalMesh]   sculptId: $mSculptId")
        println("[LocalMesh]   translation: ${mObjectTranslation.toList()}")
        println("[LocalMesh]   size: ${mObjectSize.toList()}")
        println("[LocalMesh]   scale: ${mObjectScale.toList()}")
    }
}

data class VolumeParams(val sculptId: UUID)

class LLLocalMeshFile(filename: String, tryLods: Boolean) {
    enum class LLLocalMeshFileStatus { STATUS_NONE, STATUS_LOADING, STATUS_ACTIVE, STATUS_ERROR }
    enum class LLLocalMeshFileExtension { EXTEN_DAE, EXTEN_GLTF, EXTEN_NONE }

    data class LLLocalMeshFileInfo(
        val name: String,
        val status: LLLocalMeshFileStatus,
        val localId: UUID,
        val lodAvailability: BooleanArray = BooleanArray(LOCAL_NUM_LODS),
        val objectList: MutableList<String> = mutableListOf()
    )

    data class LLLocalMeshLoaderReply(
        val changed: Boolean,
        val loaded: Boolean,
        val preserveActiveOnFailure: Boolean,
        val autoReload: Boolean,
        val log: MutableList<String>,
        val status: BooleanArray,
        val filenames: Array<String>,
        val lastModified: Array<String?>,
        val loadedObjectList: MutableList<LLLocalMeshObject>
    )

    data class LLLocalMeshReloadBinding(
        val sculptId: UUID,
        val objectIndex: Int,
        val objectName: String
    )

    private val mFilenames: Array<String> = Array(LOCAL_NUM_LODS) { "" }
    private val mLastModified: Array<String?> = Array(LOCAL_NUM_LODS) { null }
    private val mLoadedSuccessfully: BooleanArray = BooleanArray(LOCAL_NUM_LODS)
    private val mTryLodFiles: Boolean = tryLods
    private var mShortName: String = ""
    private val mLoadingLog: MutableList<String> = mutableListOf()
    private var mExtension: LLLocalMeshFileExtension = LLLocalMeshFileExtension.EXTEN_NONE
    private var mLocalMeshFileStatus: LLLocalMeshFileStatus = LLLocalMeshFileStatus.STATUS_NONE
    private val mLocalMeshFileId: UUID = UUID.randomUUID()
    private var mLocalMeshFileNeedsUIUpdate: Boolean = false
    private var mAsyncFuture: Future<LLLocalMeshLoaderReply>? = null
    private var mLoadedObjectList: MutableList<LLLocalMeshObject> = mutableListOf()
    private var mSavedObjectBindings: MutableList<LLLocalMeshReloadBinding> = mutableListOf()

    init {
        mShortName = Paths.get(filename).fileName?.toString()?.substringBeforeLast('.') ?: ""
        val baseLodFilename = stripSuffix(mShortName)
        pushLog("LLLocalMeshFile", "Initializing with base filename: $baseLodFilename")

        if (!Files.exists(Paths.get(filename))) {
            mLocalMeshFileStatus = LLLocalMeshFileStatus.STATUS_ERROR
            pushLog("LLLocalMeshFile", "Couldn't find filename: $filename", true)
            return
        }

        mFilenames[LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal] = filename

        val exten = filename.substringAfterLast('.').lowercase()
        when {
            exten == "dae" -> {
                mExtension = LLLocalMeshFileExtension.EXTEN_DAE
                pushLog("LLLocalMeshFile", "Extension found: COLLADA")
            }
            exten == "gltf" || exten == "glb" -> {
                mExtension = LLLocalMeshFileExtension.EXTEN_GLTF
                pushLog("LLLocalMeshFile", "Extension found: GLTF")
            }
        }

        if (mExtension == LLLocalMeshFileExtension.EXTEN_NONE) {
            mLocalMeshFileStatus = LLLocalMeshFileStatus.STATUS_ERROR
            pushLog("LLLocalMeshFile", "No valid file extension found.", true)
            return
        }

        reloadLocalMeshObjects(initialLoad = true, forceReload = true, autoReload = false)
    }

    fun reloadLocalMeshObjects(initialLoad: Boolean = false, forceReload: Boolean = true, autoReload: Boolean = false) {
        if (mLocalMeshFileStatus == LLLocalMeshFileStatus.STATUS_LOADING) return

        if (!initialLoad) mLoadingLog.clear()

        mSavedObjectBindings = collectReloadBindings()
        mLocalMeshFileStatus = LLLocalMeshFileStatus.STATUS_LOADING
        mLocalMeshFileNeedsUIUpdate = true

        val reloadFilenames = mFilenames.copyOf()
        discoverLodFilenames(mFilenames, reloadFilenames)

        val reloadLastModified = Array<String?>(LOCAL_NUM_LODS) { null }
        for (lodIdx in 0 until LOCAL_NUM_LODS) {
            reloadLastModified[lodIdx] = readLastModified(reloadFilenames[lodIdx])
        }

        val hasActiveReloadTarget = mLoadedObjectList.isNotEmpty() && mLoadedSuccessfully[LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal]
        if (reloadFilenames[LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal].isEmpty() || reloadLastModified[LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal].isNullOrEmpty()) {
            val warning = "Couldn't find filename: ${mFilenames[LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal]}"
            if (hasActiveReloadTarget) {
                pushLog("LLLocalMeshFile", "WARNING: $warning. Keeping last good local mesh active.")
                mLocalMeshFileStatus = LLLocalMeshFileStatus.STATUS_ACTIVE
            } else {
                pushLog("LLLocalMeshFile", warning, true)
                mLocalMeshFileStatus = LLLocalMeshFileStatus.STATUS_ERROR
            }
            mLocalMeshFileNeedsUIUpdate = true
            mSavedObjectBindings.clear()
            return
        }

        val changedLods = BooleanArray(LOCAL_NUM_LODS)
        var reloadDetected = initialLoad || forceReload
        if (!reloadDetected) {
            reloadDetected = detectReloadChanges(reloadFilenames, reloadLastModified, changedLods)
        } else {
            changedLods.fill(true)
        }

        if (!reloadDetected) {
            mLocalMeshFileStatus = if (hasActiveReloadTarget) LLLocalMeshFileStatus.STATUS_ACTIVE else LLLocalMeshFileStatus.STATUS_ERROR
            mLocalMeshFileNeedsUIUpdate = true
            mSavedObjectBindings.clear()
            return
        }

        val reloadAllLods = initialLoad || forceReload || changedLods[LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal]
        val workingObjectList: MutableList<LLLocalMeshObject> = if (reloadAllLods) mutableListOf() else cloneLoadedObjects()
        val previousStatus = mLoadedSuccessfully.copyOf()
        val extension = mExtension

        val callable = Callable<LLLocalMeshLoaderReply> {
            var changeHappened = reloadAllLods
            val log = mutableListOf<String>()
            val lodSuccess = previousStatus.copyOf()
            if (reloadAllLods) lodSuccess.fill(false)

            for (lodIdx in (LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal) downTo LocalMeshFileLOD.LOCAL_LOD_LOWEST.ordinal) {
                val currentLod = LocalMeshFileLOD.values()[lodIdx]
                val shouldReload = reloadAllLods || changedLods[lodIdx]

                if (!shouldReload) {
                    log.add("[ LLLocalMeshFile ] File for LOD $lodIdx was not modified, skipping.")
                    continue
                }

                if (reloadFilenames[lodIdx].isEmpty()) {
                    log.add("[ LLLocalMeshFile ] File for LOD $lodIdx was not found, clearing.")
                    if (!reloadAllLods) {
                        for (obj in workingObjectList) obj.getFaces(currentLod).clear()
                    }
                    lodSuccess[lodIdx] = false
                    changeHappened = true
                    continue
                }

                if (!reloadAllLods) {
                    for (obj in workingObjectList) obj.getFaces(currentLod).clear()
                }

                log.add("[ LLLocalMeshFile ] Attempting to load file for LOD $lodIdx")
                val (loaded, importLog) = when (extension) {
                    LLLocalMeshFileExtension.EXTEN_DAE -> {
                        val importer = LLLocalMeshImportDAE()
                        importer.loadFile(reloadFilenames[lodIdx], currentLod, workingObjectList)
                    }
                    LLLocalMeshFileExtension.EXTEN_GLTF -> {
                        val importer = FSLocalMeshImportGLTF()
                        importer.loadFile(reloadFilenames[lodIdx], currentLod, workingObjectList)
                    }
                    else -> {
                        log.add("[ LLLocalMeshFile ] ERROR, Loader for LOD $lodIdx called with invalid extension.")
                        false to mutableListOf()
                    }
                }
                lodSuccess[lodIdx] = loaded
                if (loaded) changeHappened = true
                log.addAll(importLog)

                if (!lodSuccess[lodIdx]) {
                    log.add("[ LLLocalMeshFile ] ERROR, attempted and failed to load LOD $lodIdx, stopping.")
                    return@Callable LLLocalMeshLoaderReply(
                        changed = false, loaded = false,
                        preserveActiveOnFailure = !initialLoad && hasActiveReloadTarget,
                        autoReload = autoReload, log = log, status = lodSuccess,
                        filenames = reloadFilenames, lastModified = reloadLastModified,
                        loadedObjectList = mutableListOf()
                    )
                }
            }

            if (workingObjectList.isEmpty()) {
                log.add("[ LLLocalMeshFile ] ERROR, no objects loaded, stopping.")
                lodSuccess.fill(false)
                return@Callable LLLocalMeshLoaderReply(
                    changed = false, loaded = false,
                    preserveActiveOnFailure = !initialLoad && hasActiveReloadTarget,
                    autoReload = autoReload, log = log, status = lodSuccess,
                    filenames = reloadFilenames, lastModified = reloadLastModified,
                    loadedObjectList = mutableListOf()
                )
            }

            LLLocalMeshLoaderReply(
                changed = changeHappened, loaded = true,
                preserveActiveOnFailure = false, autoReload = autoReload,
                log = log, status = lodSuccess, filenames = reloadFilenames,
                lastModified = reloadLastModified, loadedObjectList = workingObjectList
            )
        }

        mAsyncFuture = Executors.newSingleThreadExecutor().submit(callable)
    }

    fun reloadLocalMeshObjectsCheck(): LLLocalMeshFileStatus {
        val future = mAsyncFuture ?: return mLocalMeshFileStatus
        if (!future.isDone) return mLocalMeshFileStatus
        reloadLocalMeshObjectsCallback()
        return mLocalMeshFileStatus
    }

    private fun reloadLocalMeshObjectsCallback() {
        val reply = mAsyncFuture?.get() ?: return
        mLoadingLog.addAll(reply.log)

        if (reply.loaded) {
            var failureReason = ""
            if (!canApplyReloadBindings(reply.loadedObjectList, { failureReason = it })) {
                pushLog("LLLocalMeshFile", "WARNING: $failureReason Keeping last good local mesh active.")
                mLocalMeshFileStatus = if (mLoadedSuccessfully[LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal])
                    LLLocalMeshFileStatus.STATUS_ACTIVE else LLLocalMeshFileStatus.STATUS_ERROR
            } else {
                for (i in mFilenames.indices) mFilenames[i] = reply.filenames[i]
                for (i in mLastModified.indices) mLastModified[i] = reply.lastModified[i]
                for (i in mLoadedSuccessfully.indices) mLoadedSuccessfully[i] = reply.status[i]
                mLoadedObjectList = reply.loadedObjectList

                if (mLoadedSuccessfully[LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal]) {
                    mLocalMeshFileStatus = LLLocalMeshFileStatus.STATUS_ACTIVE
                    if (reply.changed) updateVObjects()
                } else {
                    mLocalMeshFileStatus = LLLocalMeshFileStatus.STATUS_ERROR
                }
            }
        } else if (reply.preserveActiveOnFailure) {
            if (reply.autoReload) {
                pushLog("LLLocalMeshFile", "WARNING: Auto-reload failed. Keeping last good local mesh active.")
            }
            mLocalMeshFileStatus = if (mLoadedSuccessfully[LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal])
                LLLocalMeshFileStatus.STATUS_ACTIVE else LLLocalMeshFileStatus.STATUS_ERROR
        } else {
            mLocalMeshFileStatus = LLLocalMeshFileStatus.STATUS_ERROR
        }

        mSavedObjectBindings.clear()
        mLocalMeshFileNeedsUIUpdate = true
    }

    private fun readLastModified(filename: String?): String? {
        if (filename.isNullOrEmpty()) return null
        return try {
            val modTime = Files.getLastModifiedTime(Paths.get(filename))
            modTime.toMillis().toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun discoverLodFilenames(seedFilenames: Array<String>, discoveredFilenames: Array<String>) {
        for (i in discoveredFilenames.indices) discoveredFilenames[i] = seedFilenames[i]

        if (seedFilenames[LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal].isEmpty()) {
            for (i in 0 until LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal) discoveredFilenames[i] = ""
            return
        }

        if (!mTryLodFiles) {
            for (i in 0 until LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal) discoveredFilenames[i] = ""
            return
        }

        val highPath = Paths.get(seedFilenames[LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal])
        val parent = highPath.parent ?: Paths.get(".")
        val extension = highPath.fileName.toString().substringAfterLast('.')

        for (lodIdx in 0 until LocalMeshFileLOD.LOCAL_LOD_HIGH.ordinal) {
            val suffix = getLodSuffix(lodIdx)
            val candidate = parent.resolve("$mShortName$suffix.$extension")
            discoveredFilenames[lodIdx] = if (Files.exists(candidate)) candidate.toString() else ""
        }
    }

    private fun detectReloadChanges(
        candidateFilenames: Array<String>,
        candidateLastModified: Array<String?>,
        changedLods: BooleanArray
    ): Boolean {
        var reloadRequired = false
        changedLods.fill(false)
        for (lodIdx in 0 until LOCAL_NUM_LODS) {
            val filenameChanged = candidateFilenames[lodIdx] != mFilenames[lodIdx]
            val timestampChanged = (candidateLastModified[lodIdx] ?: "") != (mLastModified[lodIdx] ?: "")
            if (filenameChanged || timestampChanged) {
                changedLods[lodIdx] = true
                reloadRequired = true
            }
        }
        return reloadRequired
    }

    private fun cloneLoadedObjects(): MutableList<LLLocalMeshObject> =
        mLoadedObjectList.mapTo(mutableListOf()) { it.clone() }

    private fun collectReloadBindings(): MutableList<LLLocalMeshReloadBinding> {
        val bindings = mutableListOf<LLLocalMeshReloadBinding>()
        for ((idx, obj) in mLoadedObjectList.withIndex()) {
            val sculptId = obj.getVolumeParams().sculptId
            val hasViewerObjects: Boolean = false
            if (hasViewerObjects) {
                bindings.add(LLLocalMeshReloadBinding(sculptId, idx, obj.getObjectName()))
            }
        }
        return bindings
    }

    private fun resolveBindingObjectIndex(
        binding: LLLocalMeshReloadBinding,
        objectList: List<LLLocalMeshObject>,
        indexOut: (Int) -> Unit
    ): Boolean {
        var nameMatch = -1
        var foundDuplicate = false
        for ((candidateIdx, candidate) in objectList.withIndex()) {
            if (candidate.getObjectName() != binding.objectName) continue
            if (nameMatch == -1) nameMatch = candidateIdx
            else { foundDuplicate = true; break }
        }

        if (nameMatch >= 0 && !foundDuplicate) { indexOut(nameMatch); return true }

        if (binding.objectIndex >= 0 && binding.objectIndex < objectList.size) {
            indexOut(binding.objectIndex)
            return binding.objectIndex >= 0
        }
        return false
    }

    private fun canApplyReloadBindings(
        objectList: List<LLLocalMeshObject>,
        failureReasonOut: (String) -> Unit
    ): Boolean {
        for (binding in mSavedObjectBindings) {
            var found = false
            resolveBindingObjectIndex(binding, objectList) { found = it >= 0 }
            if (!found) {
                failureReasonOut("Unable to remap submesh \"${binding.objectName}\" after reload.")
                return false
            }
        }
        return true
    }

    fun needsReload(): Boolean {
        if (mLocalMeshFileStatus == LLLocalMeshFileStatus.STATUS_LOADING) return false
        val candidate = mFilenames.copyOf()
        discoverLodFilenames(mFilenames, candidate)
        val candidateMod = Array<String?>(LOCAL_NUM_LODS) { readLastModified(candidate[it]) }
        val changed = BooleanArray(LOCAL_NUM_LODS)
        return detectReloadChanges(candidate, candidateMod, changed)
    }

    fun notifyNeedsUIUpdate(): Boolean {
        val result = mLocalMeshFileNeedsUIUpdate
        if (mLocalMeshFileNeedsUIUpdate) mLocalMeshFileNeedsUIUpdate = false
        return result
    }

    fun getFileInfo(): LLLocalMeshFileInfo {
        val info = LLLocalMeshFileInfo(
            name = mShortName,
            status = mLocalMeshFileStatus,
            localId = mLocalMeshFileId,
            lodAvailability = mLoadedSuccessfully.copyOf()
        )
        if (mLocalMeshFileStatus == LLLocalMeshFileStatus.STATUS_ACTIVE) {
            for (obj in getObjectVector()) info.objectList.add(obj.getObjectName())
        }
        return info
    }

    private fun updateVObjects() {
        for (binding in mSavedObjectBindings) {
            var objectIndex = -1
            resolveBindingObjectIndex(binding, mLoadedObjectList) { objectIndex = it }
            if (objectIndex < 0) continue

            val affectedIds: List<UUID> = emptyList()
            for (voId in affectedIds) {
                System.err.println("LLLocalMeshFile: updateVObjects applyToVObject not yet implemented")
            }
            System.err.println("LLLocalMeshFile: updateVObjects skinMap erase not yet implemented")
        }
    }

    fun applyToVObject(viewerObjectId: UUID, objectIndex: Int, useScale: Boolean) {
        if (objectIndex < 0 || objectIndex >= mLoadedObjectList.size) return
        val obj = mLoadedObjectList[objectIndex]
        System.err.println("LLLocalMeshFile: applyToVObject not yet implemented")
    }

    fun pushLog(who: String, what: String, isError: Boolean = false) {
        val prefix = if (isError) "[ $who ] [ ERROR ] " else "[ $who ] "
        val msg = prefix + what
        mLoadingLog.add(msg)
        println("[LocalMesh] $msg")
    }

    fun getObjectVector(): MutableList<LLLocalMeshObject> = mLoadedObjectList
    fun getFilename(lod: LocalMeshFileLOD): String = mFilenames[lod.ordinal]
    fun getFileId(): UUID = mLocalMeshFileId
    fun getFileLog(): MutableList<String> = mLoadingLog

    private fun stripSuffix(name: String): String {
        return ""
    }

    private fun getLodSuffix(lodIdx: Int): String {
        return ""
    }
}

object LLLocalMeshSystem {
    private val mSystemLog: MutableList<String> = mutableListOf()
    private val mLoadedFileList: MutableList<LLLocalMeshFile> = mutableListOf()
    private var mFileAsyncsOngoing: Boolean = false
    private var mFloaterPtr: VJFloaterLocalMesh? = null
    private var mAutoReloadTimer: Any? = null
    private var mAutoReloadConnection: (() -> Unit)? = null
    private var mAutoReloadPeriodConnection: (() -> Unit)? = null

    init {
        mAutoReloadConnection = {
            System.err.println("LLLocalMeshSystem: FSLocalMeshAutoReload connect not yet implemented")
        }
        mAutoReloadPeriodConnection = {
            System.err.println("LLLocalMeshSystem: FSLocalMeshAutoReloadPeriod connect not yet implemented")
        }
        refreshAutoReloadTimer()
    }

    fun addFile(filename: String, tryLods: Boolean) {
        mLoadedFileList.add(LLLocalMeshFile(filename, tryLods))
        triggerFloaterRefresh(false)
        refreshAutoReloadTimer()
        triggerCheckFileAsyncStatus()
    }

    fun deleteFile(localFileId: UUID) {
        val iter = mLoadedFileList.iterator()
        var deleteDone = false
        while (iter.hasNext()) {
            val file = iter.next()
            val info = file.getFileInfo()
            if (info.localId == localFileId) {
                if (info.status == LLLocalMeshFile.LLLocalMeshFileStatus.STATUS_LOADING) break
                iter.remove()
                deleteDone = true
            }
        }
        if (deleteDone) {
            triggerFloaterRefresh()
            refreshAutoReloadTimer()
        }
    }

    fun reloadFile(localFileId: UUID) {
        var reloadStarted = false
        for (file in mLoadedFileList) {
            val info = file.getFileInfo()
            if (info.localId == localFileId) {
                if (info.status == LLLocalMeshFile.LLLocalMeshFileStatus.STATUS_LOADING) continue
                file.reloadLocalMeshObjects(false, true, false)
                reloadStarted = true
            }
        }
        if (reloadStarted) triggerCheckFileAsyncStatus()
    }

    fun applyVObject(viewerObjectId: UUID, localFileId: UUID, objectIndex: Int, useScale: Boolean) {
        for (file in mLoadedFileList) {
            if (file.getFileId() == localFileId) {
                file.applyToVObject(viewerObjectId, objectIndex, useScale)
                break
            }
        }
    }

    fun clearVObject(viewerObjectId: UUID) {
        System.err.println("LLLocalMeshSystem: clearVObject not yet implemented")
    }

    fun triggerCheckFileAsyncStatus() {
        if (mFileAsyncsOngoing) return
        mFileAsyncsOngoing = true
        checkFileAsyncStatus()
    }

    fun checkFileAsyncStatus() {
        if (!mFileAsyncsOngoing) return
        var foundActiveAsyncs = false
        var needUiUpdate = false
        for (file in mLoadedFileList) {
            val status = file.reloadLocalMeshObjectsCheck()
            if (status == LLLocalMeshFile.LLLocalMeshFileStatus.STATUS_LOADING) foundActiveAsyncs = true
            if (file.notifyNeedsUIUpdate()) needUiUpdate = true
        }
        if (foundActiveAsyncs) {
            System.err.println("LLLocalMeshSystem: checkFileAsyncStatus idle reschedule not yet implemented")
        } else {
            mFileAsyncsOngoing = false
        }
        if (needUiUpdate) triggerFloaterRefresh()
    }

    fun refreshAutoReloadTimer() {
        mAutoReloadTimer = null
        val autoReloadEnabled: Boolean = false
        if (!autoReloadEnabled || mLoadedFileList.isEmpty()) return
        val period: Float = 0f
        System.err.println("LLLocalMeshSystem: refreshAutoReloadTimer schedule not yet implemented")
    }

    fun checkAutoReloadFiles() {
        val autoReloadEnabled: Boolean = false
        if (!autoReloadEnabled) { refreshAutoReloadTimer(); return }
        var reloadStarted = false
        for (file in mLoadedFileList) {
            if (file.needsReload()) {
                file.reloadLocalMeshObjects(false, false, true)
                reloadStarted = true
            }
        }
        if (reloadStarted) triggerCheckFileAsyncStatus()
    }

    fun registerFloaterPointer(floaterPtr: VJFloaterLocalMesh?) {
        mFloaterPtr = floaterPtr
    }

    fun getFloaterPointer(): VJFloaterLocalMesh? = mFloaterPtr

    fun triggerFloaterRefresh(keepSelection: Boolean = true) {
        mFloaterPtr?.reloadFileList(keepSelection)
    }

    fun getFileInfoVector(): List<LLLocalMeshFile.LLLocalMeshFileInfo> =
        mLoadedFileList.map { it.getFileInfo() }

    fun getFileLog(localFileId: UUID): List<String> =
        mLoadedFileList.firstOrNull { it.getFileId() == localFileId }?.getFileLog() ?: emptyList()

    fun pushLog(who: String, what: String, isError: Boolean = false) {
        val prefix = if (isError) "[ $who ] [ ERROR ] " else "[ $who ] "
        val msg = prefix + what
        mSystemLog.add(msg)
        println("[LocalMesh] $msg")
    }
}
