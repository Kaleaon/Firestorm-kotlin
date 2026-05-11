package com.firestorm.llmessage

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

const val MAX_BUFFER_SIZE = 65536
const val MESSAGE_MAX_PER_FRAME = 400

const val FLAG_ZERO_CODE: UByte = 0x80u
const val FLAG_RELIABLE: UByte  = 0x40u
const val FLAG_RESENT: UByte    = 0x20u
const val FLAG_ACK: UByte       = 0x10u

class MessageSystem private constructor() {

    var messageBuilder: MessageBuilder? = null
    var messageReader: MessageReader = TemplateMessageReader()

    private var socket: DatagramSocket? = null
    private var sequenceNumber: UInt = 0u
    private val sendBuffer: ByteBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN)

    private val messageHandlers: MutableMap<String, (MessageSystem) -> Unit> = mutableMapOf()

    var verboseLog: Boolean = false
    var packetsIn: UInt = 0u
    var packetsOut: UInt = 0u
    var bytesIn: ULong = 0uL
    var bytesOut: ULong = 0uL

    fun newMessage(name: String) {
        messageBuilder?.newMessage(name)
    }

    fun nextBlock(blockName: String) {
        messageBuilder?.nextBlock(blockName)
    }

    fun addU8(varName: String, value: UByte) = messageBuilder?.addU8(varName, value)
    fun addU16(varName: String, value: UShort) = messageBuilder?.addU16(varName, value)
    fun addU32(varName: String, value: UInt) = messageBuilder?.addU32(varName, value)
    fun addS32(varName: String, value: Int) = messageBuilder?.addS32(varName, value)
    fun addF32(varName: String, value: Float) = messageBuilder?.addF32(varName, value)
    fun addBool(varName: String, value: Boolean) = messageBuilder?.addBool(varName, value)
    fun addString(varName: String, value: String) = messageBuilder?.addString(varName, value)
    fun addUUID(varName: String, value: LLUUID) = messageBuilder?.addUUID(varName, value)
    fun addVector3(varName: String, value: Vector3) = messageBuilder?.addVector3(varName, value)
    fun addColor4(varName: String, r: Float, g: Float, b: Float, a: Float) =
        messageBuilder?.addVector4(varName, r, g, b, a)
    fun addIPAddr(varName: String, ip: UInt) = messageBuilder?.addIPAddr(varName, ip)
    fun addIPPort(varName: String, port: UShort) = messageBuilder?.addIPPort(varName, port)

    fun getU8(blockName: String, varName: String): UByte = messageReader.getU8(blockName, varName)
    fun getU16(blockName: String, varName: String): UShort = messageReader.getU16(blockName, varName)
    fun getU32(blockName: String, varName: String): UInt = messageReader.getU32(blockName, varName)
    fun getS32(blockName: String, varName: String): Int = messageReader.getS32(blockName, varName)
    fun getF32(blockName: String, varName: String): Float = messageReader.getF32(blockName, varName)
    fun getBool(blockName: String, varName: String): Boolean = messageReader.getBool(blockName, varName)
    fun getString(blockName: String, varName: String): String = messageReader.getString(blockName, varName)
    fun getUUID(blockName: String, varName: String): LLUUID = messageReader.getUUID(blockName, varName)
    fun getVector3(blockName: String, varName: String): Vector3 = messageReader.getVector3(blockName, varName)
    fun getNumberOfBlocks(blockName: String): Int = messageReader.getNumberOfBlocks(blockName)

    fun sendReliable(host: Host) = sendPacket(host, reliable = true)

    fun sendUnreliable(host: Host) = sendPacket(host, reliable = false)

    private fun sendPacket(host: Host, reliable: Boolean) {
        val payload = messageBuilder?.buildMessage() ?: return
        val seq = ++sequenceNumber
        val header = ByteArray(6)
        header[0] = if (reliable) FLAG_RELIABLE.toByte() else 0
        header[1] = (seq shr 24).toByte()
        header[2] = (seq shr 16).toByte()
        header[3] = (seq shr 8).toByte()
        header[4] = seq.toByte()
        header[5] = 0 // offset
        val packet = header + payload
        try {
            val addr = InetAddress.getByName(host.address)
            val dgram = DatagramPacket(packet, packet.size, addr, host.port)
            socket?.send(dgram)
            packetsOut++
            bytesOut += packet.size.toULong()
        } catch (_: Exception) {}
        messageBuilder?.clearMessage()
    }

    fun processIncoming() {}

    fun establishBidirectionalTrust(host: Host, id: Long) {}

    fun setHandlerFunc(name: String, handler: (MessageSystem) -> Unit) {
        messageHandlers[name] = handler
    }

    fun openSocket(port: Int) {
        socket = DatagramSocket(port)
    }

    fun closeSocket() {
        socket?.close()
        socket = null
    }

    companion object {
        private var sInstance: MessageSystem? = null

        fun instance(): MessageSystem = sInstance ?: MessageSystem().also { sInstance = it }

        fun createInstance(): MessageSystem {
            sInstance = MessageSystem()
            return sInstance!!
        }

        fun destroyInstance() {
            sInstance?.closeSocket()
            sInstance = null
        }
    }
}
