package com.firestorm.llcrashlogger

abstract class CrashLookup {
    abstract fun initFromDump(dumpPath: String): Boolean

    var instructionAddress: ULong = 0UL
        protected set
    var moduleName: String = ""
        protected set
    var moduleBaseAddress: ULong = 0UL
        protected set
    var moduleTimeStamp: UInt = 0U
        protected set
    var moduleChecksum: UInt = 0U
        protected set
    var moduleVersion: ULong = 0UL
        protected set

    val moduleDisplacement: ULong
        get() = instructionAddress - moduleBaseAddress

    fun moduleVersionString(): String {
        val v = moduleVersion
        val a = (v shr 48).toUInt()
        val b = ((v shr 32) and 0xFFFFUL).toUInt()
        val c = ((v shr 16) and 0xFFFFUL).toUInt()
        val d = (v and 0xFFFFUL).toUInt()
        return "$a.$b.$c.$d"
    }
}
