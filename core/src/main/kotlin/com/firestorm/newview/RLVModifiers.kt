package com.firestorm.newview

import java.util.UUID

// ============================================================================
// RlvBehaviourModifierComp — base comparator for modifier value ordering
// ============================================================================

open class RlvBehaviourModifierComp {
    var primaryObjectId: UUID = UUID(0, 0)

    // Values belonging to the primary object take precedence; otherwise preserve
    // relative insertion order.
    open fun compare(lhs: RlvBehaviourModifierValueTuple, rhs: RlvBehaviourModifierValueTuple): Boolean {
        if (rhs.second == primaryObjectId && lhs.second != primaryObjectId) return false
        return true
    }
}

// ============================================================================
// RlvBehaviourModifierCompMin / CompMax — ordering policies for the registry
// ============================================================================

class RlvBehaviourModifierCompMin : RlvBehaviourModifierComp() {
    override fun compare(lhs: RlvBehaviourModifierValueTuple, rhs: RlvBehaviourModifierValueTuple): Boolean {
        val bothNull = primaryObjectId == UUID(0, 0)
        val bothPrimary = lhs.second == primaryObjectId && rhs.second == primaryObjectId
        return if (bothNull || bothPrimary) {
            lhs.first < rhs.first
        } else {
            super.compare(lhs, rhs)
        }
    }
}

class RlvBehaviourModifierCompMax : RlvBehaviourModifierComp() {
    override fun compare(lhs: RlvBehaviourModifierValueTuple, rhs: RlvBehaviourModifierValueTuple): Boolean {
        val bothNull = primaryObjectId == UUID(0, 0)
        val bothPrimary = lhs.second == primaryObjectId && rhs.second == primaryObjectId
        return if (bothNull || bothPrimary) {
            rhs.first < lhs.first
        } else {
            super.compare(lhs, rhs)
        }
    }
}

// ============================================================================
// RlvBehaviourModifierValueTuple — (value, ownerObjectId) pair
// ============================================================================

// Comparable<*> erasure: use a sealed wrapper so numeric ordering is possible
// without reflection at the call site.
data class RlvBehaviourModifierValueTuple(
    val first: RlvBehaviourModifierValue,
    val second: UUID
)

// Convenience operator that delegates to the Comparable value stored inside
// RlvBehaviourModifierValue.  Returns negative / zero / positive like compareTo.
private operator fun RlvBehaviourModifierValue.compareTo(other: RlvBehaviourModifierValue): Int =
    compareValuesBy(this, other) { it.numericKey() }

private fun RlvBehaviourModifierValue.numericKey(): Double = when (this) {
    is RlvBehaviourModifierValue.FloatVal  -> value.toDouble()
    is RlvBehaviourModifierValue.IntVal    -> value.toDouble()
    is RlvBehaviourModifierValue.BoolVal   -> if (value) 1.0 else 0.0
    is RlvBehaviourModifierValue.StringVal -> 0.0
    is RlvBehaviourModifierValue.UUIDVal   -> 0.0
}

private operator fun RlvBehaviourModifierValue.compareTo(other: RlvBehaviourModifierValue, dummy: Unit = Unit): Boolean =
    this.numericKey() < other.numericKey()

// Infix shim so the comparator bodies read naturally
private infix fun RlvBehaviourModifierValue.lt(other: RlvBehaviourModifierValue): Boolean =
    this.numericKey() < other.numericKey()

// Rewrite comparators with the infix helper
private fun RlvBehaviourModifierCompMin.compareValues(
    lhs: RlvBehaviourModifierValueTuple,
    rhs: RlvBehaviourModifierValueTuple
): Boolean {
    val bothNull = primaryObjectId == UUID(0, 0)
    val bothPrimary = lhs.second == primaryObjectId && rhs.second == primaryObjectId
    return if (bothNull || bothPrimary) lhs.first lt rhs.first
    else super.compare(lhs, rhs)
}

private fun RlvBehaviourModifierCompMax.compareValues(
    lhs: RlvBehaviourModifierValueTuple,
    rhs: RlvBehaviourModifierValueTuple
): Boolean {
    val bothNull = primaryObjectId == UUID(0, 0)
    val bothPrimary = lhs.second == primaryObjectId && rhs.second == primaryObjectId
    return if (bothNull || bothPrimary) rhs.first lt lhs.first
    else super.compare(lhs, rhs)
}

// ============================================================================
// RlvBehaviourModifierValue — discriminated union replacing boost::variant
// ============================================================================

sealed class RlvBehaviourModifierValue {
    data class FloatVal(val value: Float)   : RlvBehaviourModifierValue()
    data class IntVal(val value: Int)       : RlvBehaviourModifierValue()
    data class BoolVal(val value: Boolean)  : RlvBehaviourModifierValue()
    data class StringVal(val value: String) : RlvBehaviourModifierValue()
    data class UUIDVal(val value: UUID)     : RlvBehaviourModifierValue()

    fun asFloat():  Float?   = (this as? FloatVal)?.value
    fun asInt():    Int?     = (this as? IntVal)?.value
    fun asBool():   Boolean? = (this as? BoolVal)?.value
    fun asString(): String?  = (this as? StringVal)?.value
    fun asUUID():   UUID?    = (this as? UUIDVal)?.value
}

// ============================================================================
// RlvBehaviourModifierCache — shared, reference-counted cache for a modifier
// value; subscribers are notified via the modifier's signal list.
// ============================================================================

class RlvBehaviourModifierCache<T>(
    val modifier: ERlvBehaviourModifier,
    private val extract: (RlvBehaviourModifierValue) -> T,
    private val dictionary: RlvBehaviourDictionary
) {
    var cachedValue: T
        private set

    // Slot held so the lambda can be disconnected on GC / explicit disposal.
    private var connectionSlot: ((RlvBehaviourModifierValue) -> Unit)? = null

    init {
        val bhvrModifier = dictionary.getModifier(modifier)
        if (bhvrModifier != null) {
            val slot: (RlvBehaviourModifierValue) -> Unit = { newValue -> cachedValue = extract(newValue) }
            connectionSlot = slot
            bhvrModifier.signal.add(slot)
            cachedValue = extract(bhvrModifier.getValue())
        } else {
            @Suppress("UNCHECKED_CAST")
            cachedValue = null as T
        }
    }

    fun dispose() {
        val slot = connectionSlot ?: return
        val bhvrModifier = dictionary.getModifier(modifier) ?: return
        bhvrModifier.signal.remove(slot)
        connectionSlot = null
    }

    fun getValue(): T = cachedValue
}

// ============================================================================
// RlvCachedBehaviourModifier — lightweight accessor backed by a shared cache
// ============================================================================

class RlvCachedBehaviourModifier<T>(
    eModifier: ERlvBehaviourModifier,
    extract: (RlvBehaviourModifierValue) -> T,
    dictionary: RlvBehaviourDictionary
) {
    private val cache: RlvBehaviourModifierCache<T> =
        RlvBehaviourModifierCache(eModifier, extract, dictionary)

    operator fun invoke(): T = cache.getValue()
    fun getValue(): T = cache.getValue()
}

// ============================================================================
// RlvBehaviourModifier — base class for a named modifier entry in the registry
// ============================================================================

open class RlvBehaviourModifier(
    val name: String,
    val defaultValue: RlvBehaviourModifierValue,
    private val addDefaultOnEmpty: Boolean,
    private val valueComparator: RlvBehaviourModifierComp?
) {
    // Subscribers receive the new effective value on every change.
    val signal: MutableList<(RlvBehaviourModifierValue) -> Unit> = mutableListOf()

    private val values: MutableList<RlvBehaviourModifierValueTuple> = mutableListOf()

    fun getValue(): RlvBehaviourModifierValue =
        if (values.isEmpty()) defaultValue else values.first().first

    fun addValue(value: RlvBehaviourModifierValue, objectId: UUID) {
        val tuple = RlvBehaviourModifierValueTuple(value, objectId)
        values.add(tuple)
        sortValues()
        fireSignal()
    }

    fun removeValue(objectId: UUID) {
        values.removeAll { it.second == objectId }
        fireSignal()
    }

    fun clearValues() {
        values.clear()
        fireSignal()
    }

    private fun sortValues() {
        val comp = valueComparator ?: return
        values.sortWith(Comparator { a, b -> if (comp.compare(a, b)) -1 else 1 })
    }

    private fun fireSignal() {
        val current = getValue()
        signal.forEach { it(current) }
        onValueChange()
    }

    protected open fun onValueChange() {}
}

// ============================================================================
// RlvBehaviourModifierHandler — subclass that triggers onValueChange override
// per modifier identity (generic parameter encodes the enum constant at type level)
// ============================================================================

class RlvBehaviourModifierHandler(
    val modifierType: ERlvBehaviourModifier,
    name: String,
    defaultValue: RlvBehaviourModifierValue,
    addDefaultOnEmpty: Boolean,
    valueComparator: RlvBehaviourModifierComp?
) : RlvBehaviourModifier(name, defaultValue, addDefaultOnEmpty, valueComparator) {

    override fun onValueChange() {
        RlvBehaviourModifierHandlerDispatch.dispatch(modifierType, getValue())
    }
}

// ============================================================================
// RlvBehaviourModifierHandlerDispatch — central dispatch for modifier changes
// (replaces C++ template specialisations of onValueChange)
// ============================================================================

object RlvBehaviourModifierHandlerDispatch {
    private val handlers: MutableMap<ERlvBehaviourModifier, (RlvBehaviourModifierValue) -> Unit> =
        mutableMapOf()

    fun register(modifier: ERlvBehaviourModifier, handler: (RlvBehaviourModifierValue) -> Unit) {
        handlers[modifier] = handler
    }

    fun dispatch(modifier: ERlvBehaviourModifier, value: RlvBehaviourModifierValue) {
        handlers[modifier]?.invoke(value)
    }
}
