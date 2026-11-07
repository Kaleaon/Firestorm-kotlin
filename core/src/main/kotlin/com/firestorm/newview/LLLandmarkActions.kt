package com.firestorm.newview

import java.util.UUID

class LLVector3d(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {
    fun isExactlyZero(): Boolean = x == 0.0 && y == 0.0 && z == 0.0
}

class LLVector3f(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f)

class LLLandmark {
    fun getGlobalPos(pos: LLVector3d): Boolean {
        return false
    }
    fun getRegionPos(): LLVector3f {
        return LLVector3f()
    }
}

class LLViewerInventoryItemRef(val uuid: UUID, val assetUUID: UUID, val name: String, val type: String)

class LLLandmarkActions private constructor() {

    companion object {
        fun fetchLandmarksByName(name: String, ifUseSubstring: Boolean): MutableList<LLViewerInventoryItemRef> {
            val searchName = name.toLowerCase()
            return mutableListOf()
        }

        fun landmarkAlreadyExists(): Boolean = findLandmarkForAgentPos() != null

        fun hasParcelLandmark(): Boolean {
            return false
        }

        fun findLandmarkForGlobalPos(pos: LLVector3d): LLViewerInventoryItemRef? {
            return null
        }

        fun findLandmarkForAgentPos(): LLViewerInventoryItemRef? {
            return null
        }

        fun createLandmarkHere() {
            System.err.println("LLLandmarkActions: createLandmarkHere not yet implemented")
        }

        fun createLandmarkHere(name: String, desc: String, folderId: UUID) {
            System.err.println("LLLandmarkActions: createLandmarkHere(name, desc, folderId) not yet implemented")
        }

        fun getSLURLfromPosGlobal(globalPos: LLVector3d, cb: (String) -> Unit, escaped: Boolean = true) {
            System.err.println("LLLandmarkActions: getSLURLfromPosGlobal not yet implemented")
        }

        fun getSLURLfromPosGlobalAndLocal(
            globalPos: LLVector3d,
            regionPos: LLVector3f,
            cb: (String) -> Unit,
            escaped: Boolean = true
        ) {
            System.err.println("LLLandmarkActions: getSLURLfromPosGlobalAndLocal not yet implemented")
        }

        fun getRegionNameAndCoordsFromPosGlobal(globalPos: LLVector3d, cb: (String, Int, Int, Int) -> Unit) {
            System.err.println("LLLandmarkActions: getRegionNameAndCoordsFromPosGlobal not yet implemented")
        }

        fun getLandmarkGlobalPos(landmarkInventoryItemId: UUID, posGlobal: LLVector3d): Boolean {
            return false
        }

        fun getLandmark(landmarkInventoryItemId: UUID, cb: ((LLLandmark) -> Unit)? = null): LLLandmark? {
            return null
        }

        fun copySLURLtoClipboard(landmarkInventoryItemId: UUID) {
            val landmark = getLandmark(landmarkInventoryItemId)
            if (landmark != null) {
                val globalPos = LLVector3d()
                landmark.getGlobalPos(globalPos)
                if (!globalPos.isExactlyZero()) {
                    getSLURLfromPosGlobalAndLocal(globalPos, landmark.getRegionPos()) { slurl ->
                        copySlurlToClipboardCallback(slurl)
                    }
                } else {
                    System.err.println("LLLandmarkActions: copySLURLtoClipboard LandmarkLocationUnknown notification not yet implemented")
                }
            }
        }

        private fun onRegionResponseSLURL(
            cb: (String) -> Unit,
            globalPos: LLVector3d,
            escaped: Boolean,
            url: String
        ) {
            System.err.println("LLLandmarkActions: onRegionResponseSLURL not yet implemented")
        }

        private fun onRegionResponseNameAndCoords(
            cb: (String, Int, Int, Int) -> Unit,
            globalPos: LLVector3d,
            regionHandle: ULong
        ) {
            System.err.println("LLLandmarkActions: onRegionResponseNameAndCoords not yet implemented")
        }

        private fun copySlurlToClipboardCallback(slurl: String) {
            if (slurl.isEmpty()) {
                System.err.println("LLLandmarkActions: copySlurlToClipboardCallback LandmarkLocationUnknown notification not yet implemented")
                return
            }
            System.err.println("LLLandmarkActions: copySlurlToClipboardCallback clipboard copy not yet implemented")
        }
    }
}
