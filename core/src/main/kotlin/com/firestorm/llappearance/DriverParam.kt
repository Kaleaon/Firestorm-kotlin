package com.firestorm.llappearance

data class DriverEntry(
    val id: Int,
    val min1: Float,
    val max1: Float,
    val min2: Float,
    val max2: Float,
)

class DriverParam : VisualParam() {
    private val entries = mutableListOf<DriverEntry>()
    private val drivenParams = mutableMapOf<Int, VisualParam>()

    fun addEntry(entry: DriverEntry) {
        entries += entry
    }

    fun linkParam(id: Int, param: VisualParam) {
        drivenParams[id] = param
    }

    override fun setWeight(weight: Float, upload: Boolean) {
        super.setWeight(weight, upload)
        propagateToDriven(this.weight, upload)
    }

    private fun drivenWeight(entry: DriverEntry, input: Float): Float {
        return when {
            input <= entry.min1 -> 0f
            input <= entry.max1 -> (input - entry.min1) / (entry.max1 - entry.min1)
            input <= entry.min2 -> 1f
            input <= entry.max2 -> 1f - (input - entry.min2) / (entry.max2 - entry.min2)
            else -> 0f
        }
    }

    private fun propagateToDriven(inputWeight: Float, upload: Boolean) {
        for (entry in entries) {
            val driven = drivenParams[entry.id] ?: continue
            val w = drivenWeight(entry, inputWeight)
            val mapped = driven.minWeight + w * (driven.maxWeight - driven.minWeight)
            driven.setWeight(mapped, upload)
        }
    }
}
