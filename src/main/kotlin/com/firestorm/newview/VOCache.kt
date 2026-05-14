package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llinventory.*

enum class VOCacheState(val bits: UInt) {
    INACTIVE(0x00000000u),
    IN_QUEUE(0x00000001u),
    WAITING (0x00000002u),
    ACTIVE  (0x00000004u),
    IN_VO_TREE(0x00010000u);

    companion object {
        const val LOW_BITS: UInt  = 0x0000ffffu
        const val HIGH_BITS: UInt = 0xffff0000u
    }
}

data class GLTFOverrideCacheEntry(
    val objectId: LLUUID             = LLUUID.NULL,
    val localId: UInt                = 0u,
    val sides: MutableMap<Int, Any>  = mutableMapOf(),
    val regionHandle: ULong          = 0uL
)

class VOCacheEntry(
    val localId: UInt,
    var crc: UInt,
    var serializedData: ByteArray
) : Comparable<VOCacheEntry> {

    var state: UInt = VOCacheState.INACTIVE.bits
        private set

    var parentId: UInt       = 0u
    var hitCount: Int        = 0
    var dupeCount: Int       = 0
    var crcChangeCount: Int  = 0
    var sceneContrib: Float  = 0f
    var updateFlags: UInt    = 0u
    var valid: Boolean       = true
    var lastCameraUpdated: Int = 0

    var bSphereCenter: Vector3  = Vector3.ZERO
    var bSphereRadius: Float    = 0f

    private val children: MutableSet<VOCacheEntry> = mutableSetOf()

    companion object {
        var sMinFrameRange: UInt    = 0u
        var sNearRadius: Float      = 96f
        var sRearFarRadius: Float   = 96f
        var sFrontPixelThreshold: Float = 1f
        var sRearPixelThreshold: Float  = 1f

        fun updateDebugSettings() { System.err.println("VOCacheEntry: updateDebugSettings not yet implemented") }
        fun getSquaredPixelThreshold(isFront: Boolean): Float =
            if (isFront) sFrontPixelThreshold * sFrontPixelThreshold
            else         sRearPixelThreshold  * sRearPixelThreshold
    }

    fun clearState(mask: UInt) { state = state and mask.inv() }
    fun hasState(mask: UInt): Boolean = (state and mask) != 0u
    fun setState(mask: UInt) {
        if (mask and VOCacheState.LOW_BITS != 0u)
            state = (state and VOCacheState.HIGH_BITS) or (mask and VOCacheState.LOW_BITS)
        else
            state = state or mask
    }
    fun isState(mask: UInt): Boolean = (state and VOCacheState.LOW_BITS) == mask
    fun getLowState(): UInt = state and VOCacheState.LOW_BITS

    fun isChild(): Boolean = parentId > 0u

    fun getNumChildren(): Int = children.size

    fun addChild(entry: VOCacheEntry) { children.add(entry) }

    fun removeChild(entry: VOCacheEntry) { children.remove(entry) }

    fun removeAllChildren() { children.clear() }

    fun getChild(): VOCacheEntry? {
        val first = children.firstOrNull() ?: return null
        children.remove(first)
        return first
    }

    fun updateEntry(newCrc: UInt, data: ByteArray) {
        if (newCrc != crc) crcChangeCount++
        crc = newCrc
        serializedData = data
    }

    fun recordHit() { hitCount++ }
    fun recordDupe() { dupeCount++ }

    fun calcSceneContribution(cameraOrigin: Vector3, needsUpdate: Boolean,
                               lastUpdate: UInt, distThreshold: Float) {
        System.err.println("VOCacheEntry: calcSceneContribution not yet implemented")
    }

    fun setBoundingInfo(pos: Vector3, scale: Vector3) {
        bSphereCenter = pos
        bSphereRadius = scale.length() * 0.5f
    }

    fun updateParentBoundingInfo() {
        System.err.println("VOCacheEntry: updateParentBoundingInfo not yet implemented")
    }

    fun isAnyVisible(cameraOrigin: Vector3, localCameraOrigin: Vector3,
                     distThreshold: Float): Boolean {
        System.err.println("VOCacheEntry: isAnyVisible not yet implemented")
        return false
    }

    override fun compareTo(other: VOCacheEntry): Int =
        compareValuesBy(other, this) { it.sceneContrib }

    fun dump() {
        println("VOCacheEntry localId=$localId crc=$crc hits=$hitCount state=${getLowState()}")
    }
}

private data class HeaderEntryInfo(val index: Int, val handle: ULong, var time: UInt)

object VOCache {
    private var enabled: Boolean       = false
    private var initialized: Boolean   = false
    private var readOnly: Boolean      = false
    private var cacheSize: UInt        = 0u
    private var numEntries: UInt       = 0u
    private var headerFileName: String = ""
    private var cacheDirName: String   = ""

    private val headerEntries: MutableMap<ULong, HeaderEntryInfo> = mutableMapOf()

    val cacheEntries: UInt    get() = numEntries
    val cacheEntriesMax: UInt get() = cacheSize

    fun initCache(size: UInt, cacheVersion: UInt) {
        cacheSize = size
        initialized = true
        enabled = true
        System.err.println("VOCache: initCache not yet implemented")
    }

    fun removeCache(started: Boolean = false) {
        System.err.println("VOCache: removeCache not yet implemented")
    }

    fun readFromCache(handle: ULong, id: LLUUID): MutableMap<UInt, VOCacheEntry> {
        System.err.println("VOCache: readFromCache not yet implemented")
        return mutableMapOf()
    }

    fun writeToCache(handle: ULong, id: LLUUID,
                     entryMap: Map<UInt, VOCacheEntry>,
                     dirtyCache: Boolean, removalEnabled: Boolean) {
        System.err.println("VOCache: writeToCache not yet implemented")
    }

    fun readGLTFExtrasFromCache(handle: ULong, id: LLUUID,
                                baseEntries: Map<UInt, VOCacheEntry>): MutableMap<UInt, GLTFOverrideCacheEntry> {
        TODO("Deserialize GLTF material overrides from extras cache file for handle")
    }

    fun writeGLTFExtrasToCache(handle: ULong, id: LLUUID,
                               extrasMap: Map<UInt, GLTFOverrideCacheEntry>,
                               dirtyCache: Boolean, removalEnabled: Boolean) {
        TODO("Serialize extrasMap to region extras cache file")
    }

    fun removeEntry(handle: ULong) {
        headerEntries.remove(handle)
        if (numEntries > 0u) numEntries--
        TODO("Delete the region cache file for this handle from disk")
    }

    private fun getObjectCacheFilename(handle: ULong): String =
        "$cacheDirName/${handle.toString(16)}.slc"

    private fun getObjectCacheExtrasFilename(handle: ULong): String =
        "$cacheDirName/${handle.toString(16)}_extras.slc"

    private fun purgeEntries(targetSize: UInt) {
        TODO("Evict oldest header entries until numEntries <= targetSize")
    }

    private fun readCacheHeader() {
        TODO("Read binary cache header: version, address size, entry index")
    }

    private fun writeCacheHeader() {
        TODO("Write binary cache header to disk")
    }

    private fun clearCacheInMemory() {
        headerEntries.clear()
        numEntries = 0u
    }
}
