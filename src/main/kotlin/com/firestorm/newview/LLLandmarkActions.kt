package com.firestorm.newview

import java.util.UUID

class LLVector3d(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {
    fun isExactlyZero(): Boolean = x == 0.0 && y == 0.0 && z == 0.0
}

class LLVector3f(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f)

class LLLandmark {
    fun getGlobalPos(pos: LLVector3d): Boolean {
        TODO("APR: use JVM equivalent - fill pos from loaded landmark data; return false if not yet loaded")
    }
    fun getRegionPos(): LLVector3f {
        TODO("APR: use JVM equivalent - return local region position of this landmark")
    }
}

class LLViewerInventoryItemRef(val uuid: UUID, val assetUUID: UUID, val name: String, val type: String)

class LLLandmarkActions private constructor() {

    companion object {
        fun fetchLandmarksByName(name: String, ifUseSubstring: Boolean): MutableList<LLViewerInventoryItemRef> {
            val searchName = name.toLowerCase()
            TODO("APR: use JVM equivalent - search Favorites and Landmarks folders in gInventory for loaded landmarks whose names match searchName (substring or exact), dedup by name, return item list")
        }

        fun landmarkAlreadyExists(): Boolean = findLandmarkForAgentPos() != null

        fun hasParcelLandmark(): Boolean {
            TODO("APR: use JVM equivalent - check cached last-found parcel landmark UUID, validate it still maps to current parcel via LLViewerParcelMgr; if miss, search inventory via LLFirstAgentParcelLandmark functor; cache and return result")
        }

        fun findLandmarkForGlobalPos(pos: LLVector3d): LLViewerInventoryItemRef? {
            TODO("APR: use JVM equivalent - search Favorites and Landmarks folders in gInventory for the first loaded landmark whose rounded global pos matches pos")
        }

        fun findLandmarkForAgentPos(): LLViewerInventoryItemRef? {
            TODO("APR: use JVM equivalent - return findLandmarkForGlobalPos(gAgent.getPositionGlobal())")
        }

        fun createLandmarkHere() {
            TODO("APR: use JVM equivalent - build landmark name/desc from LLAgentUI.buildLocationString, find Landmarks folder UUID, then call createLandmarkHere(name, desc, folderUUID)")
        }

        fun createLandmarkHere(name: String, desc: String, folderId: UUID) {
            TODO("APR: use JVM equivalent - verify agent region and parcel exist, then call create_inventory_item for AT_LANDMARK with given name/desc/folder")
        }

        fun getSLURLfromPosGlobal(globalPos: LLVector3d, cb: (String) -> Unit, escaped: Boolean = true) {
            TODO("APR: use JVM equivalent - look up sim info for globalPos in LLWorldMap; if found build SLURL string and invoke cb; otherwise send region handle request and invoke cb in onRegionResponseSLURL")
        }

        fun getSLURLfromPosGlobalAndLocal(
            globalPos: LLVector3d,
            regionPos: LLVector3f,
            cb: (String) -> Unit,
            escaped: Boolean = true
        ) {
            TODO("APR: use JVM equivalent - compute region-origin pos by subtracting regionPos from globalPos, look up sim name, build SLURL from sim name + regionPos; fall back to region handle request if sim not yet known")
        }

        fun getRegionNameAndCoordsFromPosGlobal(globalPos: LLVector3d, cb: (String, Int, Int, Int) -> Unit) {
            TODO("APR: use JVM equivalent - look up sim info for globalPos in LLWorldMap; if found compute local pos and invoke cb(name, x, y, z); otherwise send region handle request and invoke cb in onRegionResponseNameAndCoords")
        }

        fun getLandmarkGlobalPos(landmarkInventoryItemId: UUID, posGlobal: LLVector3d): Boolean {
            TODO("APR: use JVM equivalent - get item from gInventory by landmarkInventoryItemId, get landmark from gLandmarkList by asset UUID, fill posGlobal and return true; return false if item or landmark not found")
        }

        fun getLandmark(landmarkInventoryItemId: UUID, cb: ((LLLandmark) -> Unit)? = null): LLLandmark? {
            TODO("APR: use JVM equivalent - get item from gInventory, then get/load landmark from gLandmarkList passing cb; return loaded landmark or null")
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
                    TODO("APR: use JVM equivalent - LLNotificationsUtil.add(\"LandmarkLocationUnknown\")")
                }
            }
        }

        private fun onRegionResponseSLURL(
            cb: (String) -> Unit,
            globalPos: LLVector3d,
            escaped: Boolean,
            url: String
        ) {
            TODO("APR: use JVM equivalent - look up sim info for globalPos in LLWorldMap; if found build SLURL from sim origin + globalPos; invoke cb with result or empty string")
        }

        private fun onRegionResponseNameAndCoords(
            cb: (String, Int, Int, Int) -> Unit,
            globalPos: LLVector3d,
            regionHandle: ULong
        ) {
            TODO("APR: use JVM equivalent - look up sim info by regionHandle in LLWorldMap, compute local pos, invoke cb(name, roundedX, roundedY, roundedZ)")
        }

        private fun copySlurlToClipboardCallback(slurl: String) {
            if (slurl.isEmpty()) {
                TODO("APR: use JVM equivalent - LLNotificationsUtil.add(\"LandmarkLocationUnknown\")")
                return
            }
            TODO("APR: use JVM equivalent - copy slurl to system clipboard via gViewerWindow.getWindow().copyTextToClipboard(); show CopySLURL notification with SLURL arg")
        }
    }
}
