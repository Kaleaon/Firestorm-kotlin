package com.firestorm.newview

const val PARCEL_GRID_STEP_METERS: Float = 4f
const val DWELL_NAN: Float = -1f

class Parcel {
    var localId: Int = -1
    var name: String = ""
    var desc: String = ""
    var area: Int = 0
    var forSale: Boolean = false
    var salePrice: Int = 0
    var claimPricePerMeter: Int = 0
    var passPrice: Int = 0
    var passHours: Float = 0f
    var totalRent: Int = 0
    var ownerID: LLUUID = LLUUID.NULL
    var groupID: LLUUID = LLUUID.NULL

    val accessList: MutableMap<LLUUID, Any> = mutableMapOf()
    val banList: MutableMap<LLUUID, Any> = mutableMapOf()

    fun getLocalID(): Int = localId
    fun setLocalID(id: Int) { localId = id }
    fun getName(): String = name
    fun setName(n: String) { name = n }
    fun getDesc(): String = desc
    fun setDesc(d: String) { desc = d }
    fun getArea(): Int = area
    fun getForSale(): Boolean = forSale
    fun getSalePrice(): Int = salePrice
    fun getClaimPricePerMeter(): Int = claimPricePerMeter
    fun getTotalRent(): Int = totalRent
    fun getPassPrice(): Int = passPrice
    fun setPassPrice(p: Int) { passPrice = p }
    fun getPassHours(): Float = passHours
    fun setPassHours(h: Float) { passHours = h }

    fun dump() {
        println("Parcel: id=$localId name=$name area=$area forSale=$forSale")
    }
}

typealias LLUUID = com.firestorm.llcommon.LLUUID

class ParcelSelection(parcel: Parcel? = null) {
    private var parcel: Parcel? = parcel

    var selectedMultipleOwners: Boolean = false
    var wholeParcelSelected: Boolean = false
    var selectedSelfCount: Int = 0
    var selectedOtherCount: Int = 0
    var selectedPublicCount: Int = 0

    fun getParcel(): Parcel? = parcel

    internal fun setParcel(p: Parcel?) { parcel = p }

    fun getSelfCount(): Int = selectedSelfCount

    fun getClaimableArea(): Int {
        val unitArea = (PARCEL_GRID_STEP_METERS * PARCEL_GRID_STEP_METERS).toInt()
        return selectedPublicCount * unitArea
    }

    fun hasOthersSelected(): Boolean = selectedOtherCount != 0

    fun getMultipleOwners(): Boolean = selectedMultipleOwners

    fun getWholeParcelSelected(): Boolean = wholeParcelSelected
}

typealias ParcelSelectionHandle = ParcelSelection?
