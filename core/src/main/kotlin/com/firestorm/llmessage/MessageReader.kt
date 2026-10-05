package com.firestorm.llmessage

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder

const val BLOCK_NOT_IN_MESSAGE = -1
const val VARIABLE_NOT_IN_BLOCK = -2
const val MESSAGE_ERROR = -3

abstract class MessageReader {
    abstract fun getNumberOfBlocks(blockName: String): Int
    abstract fun getSize(blockName: String, varName: String): Int
    abstract fun getSize(blockName: String, blockNum: Int, varName: String): Int

    abstract fun getBinaryData(blockName: String, varName: String, size: Int, blockNum: Int = 0): ByteArray
    abstract fun getBool(blockName: String, varName: String, blockNum: Int = 0): Boolean
    abstract fun getS8(blockName: String, varName: String, blockNum: Int = 0): Byte
    abstract fun getU8(blockName: String, varName: String, blockNum: Int = 0): UByte
    abstract fun getS16(blockName: String, varName: String, blockNum: Int = 0): Short
    abstract fun getU16(blockName: String, varName: String, blockNum: Int = 0): UShort
    abstract fun getS32(blockName: String, varName: String, blockNum: Int = 0): Int
    abstract fun getF32(blockName: String, varName: String, blockNum: Int = 0): Float
    abstract fun getU32(blockName: String, varName: String, blockNum: Int = 0): UInt
    abstract fun getU64(blockName: String, varName: String, blockNum: Int = 0): ULong
    abstract fun getF64(blockName: String, varName: String, blockNum: Int = 0): Double
    abstract fun getVector3(blockName: String, varName: String, blockNum: Int = 0): Vector3
    abstract fun getVector4(blockName: String, varName: String, blockNum: Int = 0): FloatArray
    abstract fun getQuaternion(blockName: String, varName: String, blockNum: Int = 0): FloatArray
    abstract fun getUUID(blockName: String, varName: String, blockNum: Int = 0): LLUUID
    abstract fun getIPAddr(blockName: String, varName: String, blockNum: Int = 0): UInt
    abstract fun getIPPort(blockName: String, varName: String, blockNum: Int = 0): UShort
    abstract fun getString(blockName: String, varName: String, blockNum: Int = 0): String

    abstract fun clearMessage()
    abstract fun getMessageName(): String
    abstract fun getMessageSize(): Int

    companion object {
        var timeDecodes: Boolean = false
        var timeDecodesSpamThreshold: Float = 0.5f
    }
}

class TemplateMessageReader : MessageReader() {

    private var messageName: String = ""
    private var messageSize: Int = 0

    private val blockData: MutableMap<String, MutableList<MutableMap<String, ByteArray>>> = mutableMapOf()

    fun readMessage(buffer: ByteArray) {
        messageSize = buffer.size
        blockData.clear()
    }

    private fun getData(blockName: String, varName: String, blockNum: Int): ByteArray? =
        blockData[blockName]?.getOrNull(blockNum)?.get(varName)

    override fun getNumberOfBlocks(blockName: String): Int =
        blockData[blockName]?.size ?: 0

    override fun getSize(blockName: String, varName: String): Int =
        blockData[blockName]?.firstOrNull()?.get(varName)?.size ?: BLOCK_NOT_IN_MESSAGE

    override fun getSize(blockName: String, blockNum: Int, varName: String): Int =
        blockData[blockName]?.getOrNull(blockNum)?.get(varName)?.size ?: BLOCK_NOT_IN_MESSAGE

    override fun getBinaryData(blockName: String, varName: String, size: Int, blockNum: Int): ByteArray =
        getData(blockName, varName, blockNum)?.take(size)?.toByteArray() ?: ByteArray(0)

    override fun getBool(blockName: String, varName: String, blockNum: Int): Boolean =
        getData(blockName, varName, blockNum)?.firstOrNull()?.let { it != 0.toByte() } ?: false

    override fun getS8(blockName: String, varName: String, blockNum: Int): Byte =
        getData(blockName, varName, blockNum)?.firstOrNull() ?: 0

    override fun getU8(blockName: String, varName: String, blockNum: Int): UByte =
        getData(blockName, varName, blockNum)?.firstOrNull()?.toUByte() ?: 0u

    private fun buf(blockName: String, varName: String, blockNum: Int, order: ByteOrder = ByteOrder.LITTLE_ENDIAN): ByteBuffer? {
        val data = getData(blockName, varName, blockNum) ?: return null
        return ByteBuffer.wrap(data).order(order)
    }

    override fun getS16(blockName: String, varName: String, blockNum: Int): Short =
        buf(blockName, varName, blockNum)?.short ?: 0

    override fun getU16(blockName: String, varName: String, blockNum: Int): UShort =
        buf(blockName, varName, blockNum)?.short?.toUShort() ?: 0u

    override fun getS32(blockName: String, varName: String, blockNum: Int): Int =
        buf(blockName, varName, blockNum)?.int ?: 0

    override fun getF32(blockName: String, varName: String, blockNum: Int): Float =
        buf(blockName, varName, blockNum)?.float ?: 0f

    override fun getU32(blockName: String, varName: String, blockNum: Int): UInt =
        buf(blockName, varName, blockNum)?.int?.toUInt() ?: 0u

    override fun getU64(blockName: String, varName: String, blockNum: Int): ULong =
        buf(blockName, varName, blockNum)?.long?.toULong() ?: 0uL

    override fun getF64(blockName: String, varName: String, blockNum: Int): Double =
        buf(blockName, varName, blockNum)?.double ?: 0.0

    override fun getVector3(blockName: String, varName: String, blockNum: Int): Vector3 {
        val b = buf(blockName, varName, blockNum) ?: return Vector3(0f, 0f, 0f)
        return Vector3(b.float, b.float, b.float)
    }

    override fun getVector4(blockName: String, varName: String, blockNum: Int): FloatArray {
        val b = buf(blockName, varName, blockNum) ?: return FloatArray(4)
        return floatArrayOf(b.float, b.float, b.float, b.float)
    }

    override fun getQuaternion(blockName: String, varName: String, blockNum: Int): FloatArray {
        val b = buf(blockName, varName, blockNum) ?: return FloatArray(4)
        val x = b.float; val y = b.float; val z = b.float
        val wSq = (1f - x * x - y * y - z * z).coerceAtLeast(0f)
        return floatArrayOf(x, y, z, Math.sqrt(wSq.toDouble()).toFloat())
    }

    override fun getUUID(blockName: String, varName: String, blockNum: Int): LLUUID {
        val b = buf(blockName, varName, blockNum) ?: return LLUUID.randomUUID()
        val msb = b.long; val lsb = b.long
        return LLUUID(msb, lsb)
    }

    override fun getIPAddr(blockName: String, varName: String, blockNum: Int): UInt =
        buf(blockName, varName, blockNum, ByteOrder.BIG_ENDIAN)?.int?.toUInt() ?: 0u

    override fun getIPPort(blockName: String, varName: String, blockNum: Int): UShort =
        buf(blockName, varName, blockNum, ByteOrder.BIG_ENDIAN)?.short?.toUShort() ?: 0u

    override fun getString(blockName: String, varName: String, blockNum: Int): String {
        val data = getData(blockName, varName, blockNum) ?: return ""
        val end = data.indexOfFirst { it == 0.toByte() }.takeIf { it >= 0 } ?: data.size
        return String(data, 0, end, Charsets.UTF_8)
    }

    override fun clearMessage() {
        messageName = ""
        messageSize = 0
        blockData.clear()
    }

    override fun getMessageName(): String = messageName

    override fun getMessageSize(): Int = messageSize
}
