package com.firestorm.newview

import java.util.UUID

class LLFloaterMemLeak(key: LLSD) : LLFloater(key) {

    private val mLeakedMem: MutableList<ByteArray> = mutableListOf()

    init {
        setTitle("Memory Leaking Simulation Floater")
    }

    override fun postBuild(): Boolean {
        var a = getChild<LLUICtrl>("leak_speed").getValue().asReal().toFloat()
        sMemLeakingSpeed = if (a > 0xFFFFFFFFL.toFloat()) {
            0xFFFFFFFFu
        } else {
            a.toUInt()
        }

        val b = getChild<LLUICtrl>("max_leak").getValue().asReal().toFloat()
        sMaxLeakedMem = if (b > 0xFFF.toFloat()) {
            0xFFFFFFFFu
        } else {
            b.toUInt() shl 20
        }

        sbAllocationFailed = false
        return true
    }

    fun onChangeLeakingSpeed() {
        val tmp = getChild<LLUICtrl>("leak_speed").getValue().asReal().toFloat()
        sMemLeakingSpeed = if (tmp > 0xFFFFFFFFL.toFloat()) 0xFFFFFFFFu else tmp.toUInt()
    }

    fun onChangeMaxMemLeaking() {
        val tmp = getChild<LLUICtrl>("max_leak").getValue().asReal().toFloat()
        sMaxLeakedMem = if (tmp > 0xFFF.toFloat()) 0xFFFFFFFFu else tmp.toUInt() shl 20
    }

    fun onClickStart() {
        sStatus = STATUS_START
        gSimulateMemLeak = true
    }

    fun onClickStop() {
        sStatus = STATUS_STOP
    }

    fun onClickRelease() {
        sStatus = STATUS_RELEASE
    }

    fun onClickClose() {
        setVisible(false)
    }

    fun idle() {
        if (sStatus == STATUS_STOP) return

        sbAllocationFailed = false

        if (sStatus == STATUS_RELEASE) {
            release()
            return
        }

        if (sMemLeakingSpeed > 0u && sTotalLeaked < sMaxLeakedMem) {
            try {
                val block = ByteArray(sMemLeakingSpeed.toInt())
                mLeakedMem.add(block)
                sTotalLeaked += sMemLeakingSpeed
            } catch (e: OutOfMemoryError) {
                stop()
            }
        } else if (sMemLeakingSpeed > 0u) {
            stop()
        }
    }

    fun stop() {
        sStatus = STATUS_STOP
        sbAllocationFailed = true
    }

    override fun draw() {
        if (sTotalLeaked > 0u) {
            val bytesString = StringBuilder()
            LLResMgr.getInstance().getIntegerString(bytesString, (sTotalLeaked shr 10).toLong())
            getChild<LLUICtrl>("total_leaked_label").setTextArg("[SIZE]", bytesString.toString())
        } else {
            getChild<LLUICtrl>("total_leaked_label").setTextArg("[SIZE]", "0")
        }

        if (sbAllocationFailed) {
            getChild<LLUICtrl>("note_label_1").setTextArg("[NOTE1]", "Memory leaking simulation stops. Reduce leaking speed or")
            getChild<LLUICtrl>("note_label_2").setTextArg("[NOTE2]", "increase max leaked memory, then press Start to continue.")
        } else {
            getChild<LLUICtrl>("note_label_1").setTextArg("[NOTE1]", "")
            getChild<LLUICtrl>("note_label_2").setTextArg("[NOTE2]", "")
        }

        super.draw()
    }

    private fun release() {
        if (mLeakedMem.isEmpty()) return
        mLeakedMem.clear()
        sStatus = STATUS_STOP
        sTotalLeaked = 0u
        sbAllocationFailed = false
        gSimulateMemLeak = false
    }

    override fun onDestroy() {
        release()
        sMemLeakingSpeed = 0u
        sMaxLeakedMem = 0u
        super.onDestroy()
    }

    companion object {
        const val STATUS_RELEASE: Int = -1
        const val STATUS_STOP: Int = 0
        const val STATUS_START: Int = 1

        var sMemLeakingSpeed: UInt = 0u
        var sMaxLeakedMem: UInt = 0u
        var sTotalLeaked: UInt = 0u
        var sStatus: Int = STATUS_STOP
        var sbAllocationFailed: Boolean = false
    }
}
