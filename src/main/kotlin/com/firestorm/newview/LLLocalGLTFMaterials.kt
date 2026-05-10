package com.firestorm.newview

import java.io.File
import java.util.UUID

class LLLocalGLTFMaterial(filename: String, index: Int) : LLFetchedGLTFMaterial() {

    enum class LinkStatus { LS_ON, LS_BROKEN }
    enum class Extension { ET_MATERIAL_GLTF, ET_MATERIAL_GLB }

    private var mFilename: String = filename
    private var mShortName: String = File(filename).nameWithoutExtension
    private var mTrackingID: UUID = UUID.randomUUID()
    private var mWorldID: UUID = UUID(0, 0)
    private var mLastModified: Long = 0L
    private var mExtension: Extension
    private var mLinkStatus: LinkStatus = LinkStatus.LS_ON
    private var mUpdateRetries: Int = LL_LOCAL_UPDATE_RETRIES
    private var mMaterialIndex: Int = index

    init {
        mExtension = when (File(filename).extension.toLowerCase()) {
            "gltf" -> Extension.ET_MATERIAL_GLTF
            "glb"  -> Extension.ET_MATERIAL_GLB
            else   -> throw IllegalArgumentException("No valid GLTF extension for file: $filename")
        }
    }

    fun getFilename(): String = mFilename
    fun getShortName(): String = mShortName
    fun getTrackingID(): UUID = mTrackingID
    fun getWorldID(): UUID = mWorldID
    fun getIndexInFile(): Int = mMaterialIndex

    fun updateSelf(): Boolean {
        var updated = false
        if (mLinkStatus == LinkStatus.LS_ON) {
            val file = File(mFilename)
            if (file.exists()) {
                val newLastModified = file.lastModified()
                if (mLastModified != newLastModified) {
                    if (loadMaterial()) {
                        if (mWorldID == UUID(0, 0)) {
                            mWorldID = UUID.randomUUID()
                        }
                        mLastModified = newLastModified
                        TODO("APR: use JVM equivalent — gGLTFMaterialList.addMaterial(mWorldID, this)")
                        mUpdateRetries = LL_LOCAL_UPDATE_RETRIES
                        materialBegin()
                        materialComplete(true)
                        updated = true
                    } else {
                        if (mUpdateRetries > 0) {
                            mUpdateRetries--
                        } else {
                            mLinkStatus = LinkStatus.LS_BROKEN
                            materialBegin()
                            materialComplete(false)
                        }
                    }
                }
            } else {
                mLinkStatus = LinkStatus.LS_BROKEN
                materialBegin()
                materialComplete(false)
            }
        }
        return updated
    }

    private fun loadMaterial(): Boolean {
        return when (mExtension) {
            Extension.ET_MATERIAL_GLTF,
            Extension.ET_MATERIAL_GLB -> {
                TODO("APR: use JVM equivalent — LLTinyGLTFHelper.loadModel and getMaterialFromModel for $mFilename index $mMaterialIndex")
            }
        }
    }

    companion object {
        private const val LL_LOCAL_UPDATE_RETRIES = 5
    }
}

class LLLocalGLTFMaterialTimer {
    private var running: Boolean = false

    fun startTimer() { running = true }
    fun stopTimer() { running = false }
    fun isRunning(): Boolean = running

    fun tick(): Boolean {
        LLLocalGLTFMaterialMgr.doUpdates()
        return false
    }
}

object LLLocalGLTFMaterialMgr {

    private val mMaterialList: MutableList<LLLocalGLTFMaterial> = mutableListOf()
    private val mTimer = LLLocalGLTFMaterialTimer()

    fun addUnit(filenames: MutableList<String>): Int =
        filenames.filter { it.isNotEmpty() }.fold(0) { acc, f -> acc + addUnit(f) }

    fun addUnit(filename: String): Int {
        TODO("APR: use JVM equivalent — LLTinyGLTFHelper.loadModel($filename) to count materials, then addUnit per index")
    }

    fun delUnit(trackingId: UUID) {
        mMaterialList.removeAll { it.getTrackingID() == trackingId }
    }

    fun getWorldID(trackingId: UUID): UUID =
        mMaterialList.firstOrNull { it.getTrackingID() == trackingId }?.getWorldID() ?: UUID(0, 0)

    fun isLocal(worldId: UUID): Boolean =
        mMaterialList.any { it.getWorldID() == worldId }

    fun getFilenameAndIndex(trackingId: UUID): Pair<String, Int> {
        val unit = mMaterialList.firstOrNull { it.getTrackingID() == trackingId }
        return Pair(unit?.getFilename() ?: "", unit?.getIndexInFile() ?: 0)
    }

    fun feedScrollList(ctrl: Any?) {
        TODO("APR: use JVM equivalent — populate scroll list control with material entries")
    }

    fun doUpdates() {
        mTimer.stopTimer()
        for (mat in mMaterialList) {
            mat.updateSelf()
        }
        mTimer.startTimer()
    }
}
