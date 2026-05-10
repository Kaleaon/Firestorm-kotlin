package com.firestorm.llcorehttp

class BufferArray {
    private val chunks = mutableListOf<ByteArray>()
    private var totalLen: Long = 0L

    companion object {
        const val BLOCK_ALLOC_SIZE = 65540
    }

    fun append(data: ByteArray) {
        if (data.isEmpty()) return
        chunks.add(data.copyOf())
        totalLen += data.size
    }

    fun append(data: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return
        chunks.add(data.copyOfRange(offset, offset + length))
        totalLen += length
    }

    fun read(offset: Long, dst: ByteArray, dstOffset: Int, len: Int): Int {
        if (len <= 0 || offset >= totalLen) return 0
        var remaining = len
        var srcPos = offset
        var dstPos = dstOffset
        var chunkStart = 0L
        var copied = 0

        for (chunk in chunks) {
            val chunkEnd = chunkStart + chunk.size
            if (srcPos < chunkEnd && remaining > 0) {
                val inChunkOffset = maxOf(0L, srcPos - chunkStart).toInt()
                val available = chunk.size - inChunkOffset
                val toCopy = minOf(available, remaining)
                chunk.copyInto(dst, dstPos, inChunkOffset, inChunkOffset + toCopy)
                dstPos += toCopy
                srcPos += toCopy
                remaining -= toCopy
                copied += toCopy
            }
            chunkStart = chunkEnd
            if (remaining <= 0) break
        }
        return copied
    }

    fun size(): Long = totalLen

    fun toByteArray(): ByteArray {
        val result = ByteArray(totalLen.toInt())
        var pos = 0
        for (chunk in chunks) {
            chunk.copyInto(result, pos)
            pos += chunk.size
        }
        return result
    }
}
