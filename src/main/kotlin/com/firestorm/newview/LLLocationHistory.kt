package com.firestorm.newview

// ---------------------------------------------------------------------------
// ELocationType
// ---------------------------------------------------------------------------

enum class ELocationType {
    TYPED_REGION_SLURL,
    LANDMARK,
    TELEPORT_HISTORY
}

// ---------------------------------------------------------------------------
// LLLocationHistoryItem
// ---------------------------------------------------------------------------

data class LLLocationHistoryItem(
    val mLocation: String = "",
    val mGlobalPos: DoubleArray = DoubleArray(3),
    val mToolTip: String = "",
    val mType: ELocationType = ELocationType.TYPED_REGION_SLURL,
    val mSLURL: String = ""
) {
    constructor(data: Map<String, Any>) : this(
        mLocation  = data["location"] as? String ?: "",
        mGlobalPos = (data["global_pos"] as? DoubleArray) ?: DoubleArray(3),
        mToolTip   = data["tooltip"] as? String ?: "",
        mType      = ELocationType.values().getOrElse((data["item_type"] as? Int) ?: 0) { ELocationType.TYPED_REGION_SLURL },
        mSLURL     = data["slurl"] as? String ?: ""
    )

    fun toLLSD(): Map<String, Any> = mapOf(
        "location"   to mLocation,
        "global_pos" to mGlobalPos,
        "tooltip"    to mToolTip,
        "item_type"  to mType.ordinal,
        "slurl"      to mSLURL
    )

    fun getLocation(): String = mLocation
    fun getToolTip(): String = mToolTip

    override fun equals(other: Any?): Boolean {
        if (other !is LLLocationHistoryItem) return false
        return mLocation == other.mLocation && mType == other.mType
    }

    override fun hashCode(): Int = 31 * mLocation.hashCode() + mType.hashCode()

    companion object {
        fun equalByLocation(item: LLLocationHistoryItem, itemLocation: String): Boolean {
            return item.getLocation() == itemLocation
        }
    }
}

// ---------------------------------------------------------------------------
// LLLocationHistory  (singleton → object)
// ---------------------------------------------------------------------------

object LLLocationHistory {

    enum class EChangeType {
        ADD,
        CLEAR,
        LOAD
    }

    private val mItems: MutableList<LLLocationHistoryItem> = mutableListOf()
    private const val mFilename: String = "typed_locations.txt"
    private val mChangedCallbacks: MutableList<(EChangeType) -> Unit> = mutableListOf()

    private const val MAX_ITEMS_DEFAULT: Int = 100

    fun setChangedCallback(cb: (EChangeType) -> Unit) {
        mChangedCallbacks.add(cb)
    }

    private fun fireChanged(event: EChangeType) {
        for (cb in mChangedCallbacks) cb(event)
    }

    fun addItem(item: LLLocationHistoryItem) {
        val maxItems = MAX_ITEMS_DEFAULT

        val existing = mItems.indexOf(item)
        if (existing >= 0) {
            mItems.removeAt(existing)
        }

        mItems.add(item)

        if (mItems.size > maxItems) {
            val excess = mItems.size - maxItems
            repeat(excess) { mItems.removeAt(0) }
        }

        fireChanged(EChangeType.ADD)
    }

    fun touchItem(item: LLLocationHistoryItem): Boolean {
        val idx = mItems.indexOf(item)
        if (idx >= 0) {
            mItems.removeAt(idx)
            mItems.add(item)
            return true
        }
        return false
    }

    fun removeItems() {
        mItems.clear()
        fireChanged(EChangeType.CLEAR)
    }

    fun getItemCount(): Int = mItems.size

    fun getItems(): List<LLLocationHistoryItem> = mItems

    fun getMatchingItems(substring: String, result: MutableList<LLLocationHistoryItem>): Boolean {
        result.clear()
        val needle = substring.toLowerCase()
        for (item in mItems) {
            if (item.getLocation().toLowerCase().contains(needle)) {
                result.add(item)
            }
        }
        return result.isNotEmpty()
    }

    fun dump() {
        mItems.forEachIndexed { i, item ->
            println("#${i.toString().padStart(2, '0')}: ${item.getLocation()}")
        }
    }

    fun save() {
        TODO("APR: use JVM equivalent to resolve per-account path for $mFilename and write items as LLSD notation")
    }

    fun load() {
        TODO("APR: use JVM equivalent to resolve per-account path for $mFilename, parse LLSD notation lines, populate mItems, then fireChanged(EChangeType.LOAD)")
    }
}
