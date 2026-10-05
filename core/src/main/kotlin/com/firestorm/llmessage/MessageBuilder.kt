package com.firestorm.llmessage

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.LLSD
import com.firestorm.llmath.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class MessageBuilder {
    abstract fun newMessage(name: String)
    abstract fun nextBlock(blockName: String)
    abstract fun removeLastBlock(): Boolean

    abstract fun addBinaryData(varName: String, data: ByteArray)
    abstract fun addBool(varName: String, value: Boolean)
    abstract fun addS8(varName: String, value: Byte)
    abstract fun addU8(varName: String, value: UByte)
    abstract fun addS16(varName: String, value: Short)
    abstract fun addU16(varName: String, value: UShort)
    abstract fun addF32(varName: String, value: Float)
    abstract fun addS32(varName: String, value: Int)
    abstract fun addU32(varName: String, value: UInt)
    abstract fun addU64(varName: String, value: ULong)
    abstract fun addF64(varName: String, value: Double)
    abstract fun addVector3(varName: String, value: Vector3)
    abstract fun addVector4(varName: String, x: Float, y: Float, z: Float, w: Float)
    abstract fun addQuaternion(varName: String, x: Float, y: Float, z: Float, w: Float)
    abstract fun addUUID(varName: String, value: LLUUID)
    abstract fun addIPAddr(varName: String, ip: UInt)
    abstract fun addIPPort(varName: String, port: UShort)
    abstract fun addString(varName: String, value: String)

    abstract fun isMessageFull(blockName: String): Boolean
    abstract fun getMessageSize(): Int
    abstract fun isBuilt(): Boolean
    abstract fun isClear(): Boolean
    abstract fun buildMessage(): ByteArray
    abstract fun clearMessage()
    abstract fun setBuilt(b: Boolean)
    abstract fun getMessageName(): String
    abstract fun copyFromLLSD(data: LLSD)
}

class TemplateMessageBuilder(private val template: MessageTemplate) : MessageBuilder() {

    private var messageName: String = ""
    private var currentBlock: String = ""
    private var built: Boolean = false
    private var clear: Boolean = true

    private val blockData: MutableMap<String, MutableList<MutableMap<String, ByteArray>>> = mutableMapOf()
    private var currentBlockIndex: MutableMap<String, Int> = mutableMapOf()

    override fun newMessage(name: String) {
        messageName = name
        blockData.clear()
        currentBlockIndex.clear()
        currentBlock = ""
        built = false
        clear = false
    }

    override fun nextBlock(blockName: String) {
        currentBlock = blockName
        val list = blockData.getOrPut(blockName) { mutableListOf() }
        list.add(mutableMapOf())
        currentBlockIndex[blockName] = list.size - 1
    }

    override fun removeLastBlock(): Boolean {
        val list = blockData[currentBlock] ?: return false
        if (list.isEmpty()) return false
        list.removeAt(list.size - 1)
        currentBlockIndex[currentBlock] = (list.size - 1).coerceAtLeast(0)
        return true
    }

    private fun putData(varName: String, data: ByteArray) {
        val blockIdx = currentBlockIndex[currentBlock] ?: return
        blockData[currentBlock]?.get(blockIdx)?.set(varName, data)
    }

    override fun addBinaryData(varName: String, data: ByteArray) = putData(varName, data)

    override fun addBool(varName: String, value: Boolean) =
        putData(varName, byteArrayOf(if (value) 1 else 0))

    override fun addS8(varName: String, value: Byte) =
        putData(varName, byteArrayOf(value))

    override fun addU8(varName: String, value: UByte) =
        putData(varName, byteArrayOf(value.toByte()))

    override fun addS16(varName: String, value: Short) =
        putData(varName, ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array())

    override fun addU16(varName: String, value: UShort) =
        putData(varName, ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array())

    override fun addF32(varName: String, value: Float) =
        putData(varName, ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array())

    override fun addS32(varName: String, value: Int) =
        putData(varName, ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array())

    override fun addU32(varName: String, value: UInt) =
        putData(varName, ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value.toInt()).array())

    override fun addU64(varName: String, value: ULong) =
        putData(varName, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value.toLong()).array())

    override fun addF64(varName: String, value: Double) =
        putData(varName, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array())

    override fun addVector3(varName: String, value: Vector3) {
        val buf = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
        buf.putFloat(value.x).putFloat(value.y).putFloat(value.z)
        putData(varName, buf.array())
    }

    override fun addVector4(varName: String, x: Float, y: Float, z: Float, w: Float) {
        val buf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        buf.putFloat(x).putFloat(y).putFloat(z).putFloat(w)
        putData(varName, buf.array())
    }

    override fun addQuaternion(varName: String, x: Float, y: Float, z: Float, w: Float) {
        val buf = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
        buf.putFloat(x).putFloat(y).putFloat(z)
        putData(varName, buf.array())
    }

    override fun addUUID(varName: String, value: LLUUID) {
        val bytes = ByteArray(16)
        val bb = ByteBuffer.wrap(bytes)
        bb.putLong(value.mostSignificantBits)
        bb.putLong(value.leastSignificantBits)
        putData(varName, bytes)
    }

    override fun addIPAddr(varName: String, ip: UInt) =
        putData(varName, ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(ip.toInt()).array())

    override fun addIPPort(varName: String, port: UShort) =
        putData(varName, ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(port.toShort()).array())

    override fun addString(varName: String, value: String) {
        val encoded = value.toByteArray(Charsets.UTF_8)
        val data = ByteArray(encoded.size + 1)
        encoded.copyInto(data)
        data[encoded.size] = 0
        putData(varName, data)
    }

    override fun isMessageFull(blockName: String): Boolean = false

    override fun getMessageSize(): Int = buildMessage().size

    override fun isBuilt(): Boolean = built

    override fun isClear(): Boolean = clear

    override fun buildMessage(): ByteArray {
        val out = mutableListOf<Byte>()
        for ((_, instances) in blockData) {
            for (vars in instances) {
                for ((_, bytes) in vars) {
                    out.addAll(bytes.toList())
                }
            }
        }
        built = true
        return out.toByteArray()
    }

    override fun clearMessage() {
        messageName = ""
        currentBlock = ""
        blockData.clear()
        currentBlockIndex.clear()
        built = false
        clear = true
    }

    override fun setBuilt(b: Boolean) { built = b }

    override fun getMessageName(): String = messageName

    override fun copyFromLLSD(data: LLSD) {}
}
