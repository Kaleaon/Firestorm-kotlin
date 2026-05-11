package com.firestorm.newview

import java.util.UUID

typealias LoadedCallback = (LLLandmark) -> Unit

class LLLandmark {
    fun getGlobalPos(pos: DoubleArray): Boolean = TODO("GPU: getGlobalPos")
    fun getRegionID(regionId: UUID): Boolean = TODO("APR: use JVM equivalent")

    companion object {
        fun constructFromString(data: String, size: Int): LLLandmark? =
            TODO("APR: use JVM equivalent")

        fun requestRegionHandle(
            msgSystem: Any?,
            regionHost: Any?,
            regionId: UUID,
            callback: () -> Unit
        ): Unit = TODO("APR: use JVM equivalent")
    }
}

class LLLandmarkList {

    private val mList: MutableMap<UUID, LLLandmark> = mutableMapOf()
    private val mBadList: MutableSet<UUID> = mutableSetOf()
    private val mRetryList: MutableSet<UUID> = mutableSetOf()
    private val mRequestedList: MutableMap<UUID, Float> = mutableMapOf()
    // multimap: each UUID may have multiple waiting callbacks
    private val mLoadedCallbackMap: MutableList<Pair<UUID, LoadedCallback>> = mutableListOf()

    fun assetExists(assetUuid: UUID): Boolean =
        mList.containsKey(assetUuid) || mBadList.contains(assetUuid)

    fun getAsset(assetUuid: UUID, cb: LoadedCallback? = null): LLLandmark? {
        val landmark = mList[assetUuid]
        if (landmark != null) {
            val dummy = DoubleArray(3)
            if (cb != null && !landmark.getGlobalPos(dummy)) {
                // landmark is not completely loaded yet; queue the callback
                mLoadedCallbackMap.add(Pair(assetUuid, cb))
            }
            return landmark
        }

        if (mBadList.contains(assetUuid)) {
            return null
        }

        if (cb != null) {
            mLoadedCallbackMap.add(Pair(assetUuid, cb))
        }

        val prevRequest = mRequestedList[assetUuid]
        if (prevRequest != null) {
            val rerequestTime = 30f
            val gFrameTimeSeconds: Float = TODO("APR: use JVM equivalent")
            @Suppress("UNREACHABLE_CODE")
            if (gFrameTimeSeconds - prevRequest < rerequestTime) {
                return null
            }
        }

        val gFrameTimeSeconds2: Float = TODO("APR: use JVM equivalent")
        @Suppress("UNREACHABLE_CODE")
        mRequestedList[assetUuid] = gFrameTimeSeconds2

        TODO("APR: use JVM equivalent") // gAssetStorage->getAssetData(...)
    }

    fun isAssetInLoadedCallbackMap(assetUuid: UUID): Boolean =
        mLoadedCallbackMap.any { it.first == assetUuid }

    protected fun onRegionHandle(landmarkId: UUID) {
        val landmark = getAsset(landmarkId)
        if (landmark == null) {
            eraseCallbacks(landmarkId)
            return
        }

        val pos = DoubleArray(3)
        if (!landmark.getGlobalPos(pos)) {
            eraseCallbacks(landmarkId)
            return
        }

        makeCallbacks(landmarkId)
    }

    protected fun eraseCallbacks(landmarkId: UUID) {
        mLoadedCallbackMap.removeAll { it.first == landmarkId }
    }

    protected fun makeCallbacks(landmarkId: UUID) {
        val landmark = getAsset(landmarkId)
        while (true) {
            val entry = mLoadedCallbackMap.firstOrNull { it.first == landmarkId } ?: break
            if (landmark != null) {
                entry.second(landmark)
            }
            mLoadedCallbackMap.remove(entry)
        }
    }

    companion object {
        fun processGetAssetReply(
            uuid: UUID,
            type: Any,
            userData: Any?,
            status: Int,
            extStatus: Any
        ) {
            val ERR_NO_CAP = -3
            val ERR_ASSET_REQUEST_FAILED = -1
            val ERR_ASSET_REQUEST_NOT_IN_DATABASE = -2

            if (status == 0) {
                TODO("APR: use JVM equivalent") // read from LLFileSystem, parse, store in gLandmarkList
            } else {
                if (status == ERR_NO_CAP) {
                    gLandmarkList.mRequestedList.remove(uuid)
                    gLandmarkList.eraseCallbacks(uuid)
                    gLandmarkList.mRetryList.remove(uuid)
                    return
                }
                if (gLandmarkList.mBadList.contains(uuid)) {
                    gLandmarkList.mRequestedList.remove(uuid)
                    gLandmarkList.eraseCallbacks(uuid)
                    return
                }
                if (status == ERR_ASSET_REQUEST_FAILED &&
                    !gLandmarkList.mRetryList.contains(uuid)
                ) {
                    gLandmarkList.mRetryList.add(uuid)
                    gLandmarkList.mRequestedList.remove(uuid)
                    gLandmarkList.eraseCallbacks(uuid)
                    return
                }

                gLandmarkList.mRetryList.remove(uuid)
                gLandmarkList.mBadList.add(uuid)
                gLandmarkList.mRequestedList.remove(uuid)
                gLandmarkList.eraseCallbacks(uuid)
            }
        }
    }
}

val gLandmarkList = LLLandmarkList()
