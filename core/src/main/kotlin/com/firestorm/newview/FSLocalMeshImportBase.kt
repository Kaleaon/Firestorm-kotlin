package com.firestorm.newview

import java.util.UUID

abstract class FSLocalMeshImportBase {

    typealias JointMap = MutableMap<String, String>
    typealias LoadFileReturn = Pair<Boolean, MutableList<String>>

    protected var mLod: LocalMeshFileLOD = LocalMeshFileLOD.LOCAL_LOD_HIGH
    protected val mLoadingLog: MutableList<String> = mutableListOf()
    protected var mLogToInfo: Boolean = true

    protected fun setLod(lod: LocalMeshFileLOD) {
        mLod = lod
        mLoadingLog.clear()
    }

    protected fun pushLog(who: String, what: String, isError: Boolean = false) {
        var logMsg = "[ $who ] "
        if (isError) logMsg += "[ ERROR ] "
        logMsg += what
        mLoadingLog.add(logMsg)
        if (mLogToInfo) {
            println("[LocalMesh] $logMsg")
        }
    }

    protected fun postProcessObject(obj: LLLocalMeshObject, sceneTransform: FloatArray, computeBounds: Boolean) {
        if (computeBounds) {
            obj.computeObjectBoundingBox()
            obj.computeObjectTransform(sceneTransform)
        }
        obj.normalizeFaceValues(mLod)
    }

    protected fun enforceRigJointLimit(
        who: String,
        obj: LLLocalMeshObject,
        skinInfo: MeshSkinInfo?,
        recognizedJointCount: UInt
    ): Boolean {
        val maxJoints: Int = 0
        if (recognizedJointCount.toInt() <= maxJoints) return true

        val warning = "WARNING: Skinning disabled for object \"${obj.getObjectName()}\"" +
            " due to too many joints: $recognizedJointCount, maximum: $maxJoints."
        pushLog(who, warning)

        if (skinInfo != null) {
            skinInfo.jointNames.clear()
            skinInfo.jointNums.clear()
            skinInfo.invBindMatrix.clear()
            skinInfo.alternateBindMatrix.clear()
            skinInfo.bindPoseMatrix.clear()
            skinInfo.bindShapeMatrix = FloatArray(16).also { it[0] = 1f; it[5] = 1f; it[10] = 1f; it[15] = 1f }
            skinInfo.invalidJointsScrubbed = false
            skinInfo.jointNumsInitialized = false
            skinInfo.updateHash()
        }

        obj.setObjectMeshSkinInfo(null)

        for (lod in 0 until LOCAL_NUM_LODS) {
            for (face in obj.getFaces(LocalMeshFileLOD.values()[lod])) {
                face.getSkin().clear()
            }
        }

        return false
    }

    companion object {
        fun loadJointMap(): MutableMap<String, String> {
            return mutableMapOf()
        }

        fun buildNormalizedTransformation(obj: LLLocalMeshObject): FloatArray {
            // no-op
            return FloatArray(16)
        }

        fun buildBindPoseMatrix(skinInfo: MeshSkinInfo?) {
            if (skinInfo == null) return
            // no-op
        }
    }
}
