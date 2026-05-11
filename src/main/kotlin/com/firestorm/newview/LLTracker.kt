package com.firestorm.newview

import java.util.UUID

class LLTracker private constructor() {

    enum class ETrackingStatus {
        TRACKING_NOTHING,
        TRACKING_AVATAR,
        TRACKING_LANDMARK,
        TRACKING_LOCATION
    }

    enum class ETrackingLocationType {
        LOCATION_NOTHING,
        LOCATION_EVENT,
        LOCATION_ITEM,
        LOCATION_AVATAR
    }

    companion object {
        private const val DESTINATION_REACHED_RADIUS = 3.0f
        private const val DESTINATION_VISITED_RADIUS = 6.0f
        private const val DESTINATION_UNVISITED_RADIUS = 12.0f
        private const val ARROW_OFF_RADIUS_SQRD = 100
        private const val HUD_ARROW_SIZE = 32

        var sTrackerp: LLTracker? = null
        var sCheesyBeacon: Boolean = false

        fun instance(): LLTracker {
            if (sTrackerp == null) {
                sTrackerp = LLTracker()
            }
            return sTrackerp!!
        }

        fun cleanupInstance() {
            sTrackerp = null
        }

        fun getTrackingStatus(): ETrackingStatus = instance().mTrackingStatus
        fun getTrackedLocationType(): ETrackingLocationType = instance().mTrackingLocationType
        fun isTracking(): Boolean = instance().mTrackingStatus != ETrackingStatus.TRACKING_NOTHING

        fun stopTracking(clearUi: Boolean) {
            instance().stopTrackingAll(clearUi)
        }

        fun clearFocus() {
            instance().mTrackingStatus = ETrackingStatus.TRACKING_NOTHING
        }

        fun getTrackedLandmarkAssetID(): UUID = instance().mTrackedLandmarkAssetID
        fun getTrackedLandmarkItemID(): UUID = instance().mTrackedLandmarkItemID

        fun trackAvatar(avatarId: UUID, name: String) {
            instance().stopTrackingLandmark()
            instance().stopTrackingLocation()
            TODO("APR: call LLAvatarTracker.instance().track(avatarId, name)")
            instance().mTrackingStatus = ETrackingStatus.TRACKING_AVATAR
            instance().mLabel = name
            instance().mToolTip = ""
        }

        fun trackLandmark(landmarkAssetId: UUID, landmarkItemId: UUID, name: String) {
            instance().stopTrackingAvatar()
            instance().stopTrackingLocation()
            instance().mTrackedLandmarkAssetID = landmarkAssetId
            instance().mTrackedLandmarkItemID = landmarkItemId
            instance().mTrackedLandmarkName = name
            instance().cacheLandmarkPosition()
            instance().mTrackingStatus = ETrackingStatus.TRACKING_LANDMARK
            instance().mLabel = name
            instance().mToolTip = ""
        }

        fun trackLocation(posGlobal: DoubleArray, fullName: String, tooltip: String, locationType: ETrackingLocationType = ETrackingLocationType.LOCATION_NOTHING) {
            instance().stopTrackingAvatar()
            instance().stopTrackingLandmark()
            instance().mTrackedPositionGlobal = posGlobal.clone()
            instance().mTrackedLocationName = fullName
            instance().mIsTrackingLocation = true
            instance().mTrackingStatus = ETrackingStatus.TRACKING_LOCATION
            instance().mTrackingLocationType = locationType
            instance().mLabel = fullName
            instance().mToolTip = tooltip
        }

        fun getTrackedPositionGlobal(): DoubleArray {
            return when (getTrackingStatus()) {
                ETrackingStatus.TRACKING_AVATAR -> TODO("APR: return LLAvatarTracker.instance().getGlobalPos() if haveTrackingInfo")
                ETrackingStatus.TRACKING_LANDMARK -> {
                    if (instance().mHasLandmarkPosition) instance().mTrackedPositionGlobal.clone()
                    else DoubleArray(3)
                }
                ETrackingStatus.TRACKING_LOCATION -> instance().mTrackedPositionGlobal.clone()
                else -> DoubleArray(3)
            }
        }

        fun hasLandmarkPosition(): Boolean {
            if (!instance().mHasLandmarkPosition) {
                instance().cacheLandmarkPosition()
            }
            return instance().mHasLandmarkPosition
        }

        fun getTrackedLocationName(): String = instance().mTrackedLocationName

        fun drawHUDArrow() {
            TODO("GPU: based on tracking status, call instance().drawMarker for avatar/landmark/location position using map track colour; clamp location Z to terrain height")
        }

        fun render3D() {
            TODO("GPU: for each tracking mode, check distance to destination, auto-stop tracking when reached, call renderBeacon; stop avatar tracking if offline or not friend")
        }

        fun handleMouseDown(x: Int, y: Int): Boolean {
            val distSqrd = (x - instance().mHUDArrowCenterX) * (x - instance().mHUDArrowCenterX) +
                (y - instance().mHUDArrowCenterY) * (y - instance().mHUDArrowCenterY)
            if (distSqrd < ARROW_OFF_RADIUS_SQRD && getTrackingStatus() != ETrackingStatus.TRACKING_NOTHING) {
                instance().stopTrackingAll()
                return true
            }
            return false
        }

        fun getLabel(): String = instance().mLabel
        fun getToolTip(): String = instance().mToolTip

        private fun drawBeacon(posAgent: FloatArray, direction: String, foggedColor: FloatArray, dist: Float) {
            TODO("GPU: render animated beacon column (triangle strip rows with pulse_func amplitude, shockwave fan at base) via gGL matrix push/translate/triangles/pop")
        }

        private fun renderBeacon(
            posGlobal: DoubleArray,
            color: FloatArray,
            colorUnder: FloatArray,
            hudTextp: LLHUDText?,
            label: String
        ) {
            TODO("GPU: compute fogged colour from distance/far-clip ratio; call drawBeacon UP and DOWN; format distance text; set hudTextp font/colour/string/position; optionally play tracker beacon sound at distance-based interval")
        }
    }

    var mTrackingStatus: ETrackingStatus = ETrackingStatus.TRACKING_NOTHING
    var mTrackingLocationType: ETrackingLocationType = ETrackingLocationType.LOCATION_NOTHING
    var mBeaconText: LLHUDText? = null
    var mHUDArrowCenterX: Int = 0
    var mHUDArrowCenterY: Int = 0

    var mTrackedPositionGlobal: DoubleArray = DoubleArray(3)

    var mLabel: String = ""
    var mToolTip: String = ""

    var mTrackedLandmarkName: String = ""
    var mTrackedLandmarkAssetID: UUID = UUID(0, 0)
    var mTrackedLandmarkItemID: UUID = UUID(0, 0)
    val mLandmarkAssetIDList: MutableList<UUID> = mutableListOf()
    val mLandmarkItemIDList: MutableList<UUID> = mutableListOf()
    var mHasReachedLandmark: Boolean = false
    var mHasLandmarkPosition: Boolean = false
    var mLandmarkHasBeenVisited: Boolean = false

    var mTrackedLocationName: String = ""
    var mIsTrackingLocation: Boolean = false
    var mHasReachedLocation: Boolean = false

    fun stopTrackingAll(clearUi: Boolean = false) {
        when (mTrackingStatus) {
            ETrackingStatus.TRACKING_AVATAR -> stopTrackingAvatar(clearUi)
            ETrackingStatus.TRACKING_LANDMARK -> stopTrackingLandmark(clearUi)
            ETrackingStatus.TRACKING_LOCATION -> stopTrackingLocation(clearUi)
            else -> mTrackingStatus = ETrackingStatus.TRACKING_NOTHING
        }
    }

    fun stopTrackingAvatar(clearUi: Boolean = false) {
        TODO("APR: call LLAvatarTracker.instance().untrack() for tracked avatar ID; purgeBeaconText; gFloaterWorldMap.clearAvatarSelection(clearUi)")
        mTrackingStatus = ETrackingStatus.TRACKING_NOTHING
    }

    fun stopTrackingLocation(clearUi: Boolean = false, destReached: Boolean = false) {
        purgeBeaconText()
        mTrackedLocationName = ""
        mIsTrackingLocation = false
        mTrackedPositionGlobal = DoubleArray(3)
        TODO("APR: gFloaterWorldMap.clearLocationSelection(clearUi, destReached)")
        mTrackingStatus = ETrackingStatus.TRACKING_NOTHING
        mTrackingLocationType = ETrackingLocationType.LOCATION_NOTHING
    }

    fun stopTrackingLandmark(clearUi: Boolean = false) {
        purgeBeaconText()
        mTrackedLandmarkAssetID = UUID(0, 0)
        mTrackedLandmarkItemID = UUID(0, 0)
        mTrackedLandmarkName = ""
        mTrackedPositionGlobal = DoubleArray(3)
        mHasLandmarkPosition = false
        mHasReachedLandmark = false
        mLandmarkHasBeenVisited = true
        TODO("APR: gFloaterWorldMap.clearLandmarkSelection(clearUi)")
        mTrackingStatus = ETrackingStatus.TRACKING_NOTHING
    }

    fun drawMarker(posGlobal: DoubleArray, color: FloatArray, isIff: Boolean = false) {
        TODO("GPU: project global position to screen; compute clamped arrow position on HUD ellipse; draw scaled+rotated arrow image at mHUDArrowCenterX/Y; skip if isIff and on_screen")
    }

    private fun setLandmarkVisited() {
        TODO("APR: look up landmark inventory item by mTrackedLandmarkItemID; set II_FLAGS_LANDMARK_VISITED flag; send ChangeInventoryItemFlags message via gMessageSystem; notify inventory observers")
    }

    private fun cacheLandmarkPosition() {
        TODO("APR: check home landmark ID via LLFloaterWorldMap.getHomeID(); otherwise look up in gLandmarkList; set mTrackedPositionGlobal and mHasLandmarkPosition when asset available")
    }

    private fun purgeBeaconText() {
        mBeaconText = null
    }
}
