/**
 * TextureAnim.kt
 * Kotlin conversion of lltextureanim.h / lltextureanim.cpp (indra/llprimitive)
 *
 * Represents the animated-texture parameters that are stored per-prim in
 * Second Life / Firestorm.  Serialisation helpers (pack/unpack) are stubbed
 * because they depend on LLMessageSystem / LLDataPacker which are not yet
 * ported.
 */

package com.firestorm.llprimitive

import java.nio.ByteBuffer
import java.nio.ByteOrder

// ---------------------------------------------------------------------------
// TextureAnim – animated texture parameters
// ---------------------------------------------------------------------------
data class TextureAnim(
    /** Mode flags; see TAM_* constants in the companion object. */
    var mode: UByte   = 0u,

    /**
     * Which face to animate.  -1 (as a signed byte) means "all faces".
     * Stored as [Byte] to mirror the C++ S8 mFace field.
     */
    var face: Byte    = -1,

    /** Number of tiles in the S (horizontal) direction. */
    var sizeX: UByte  = 4u,

    /** Number of tiles in the T (vertical) direction. */
    var sizeY: UByte  = 4u,

    /** Starting frame / texture offset. */
    var start: Float  = 0f,

    /** Total length of the animation in seconds (0 = play-once / loop). */
    var length: Float = 0f,

    /** Playback rate in frames per second. */
    var rate: Float   = 1f,
) {
    // ------------------------------------------------------------------
    // Companion: TAM_* mode bit-flag constants
    // ------------------------------------------------------------------
    companion object {
        // Mirrors the anonymous enum inside LLTextureAnim
        const val TAM_ON: UByte        = 0x01u
        const val TAM_LOOP: UByte      = 0x02u
        const val TAM_REVERSE: UByte   = 0x04u
        const val TAM_PING_PONG: UByte = 0x08u
        const val TAM_SMOOTH: UByte    = 0x10u
        const val TAM_ROTATE: UByte    = 0x20u
        const val TAM_SCALE: UByte     = 0x40u

        /** Wire-format block size in bytes (matches TA_BLOCK_SIZE in C++). */
        const val BLOCK_SIZE: Int = 16

        /** Return a default (off) TextureAnim. */
        fun default(): TextureAnim = TextureAnim()
    }

    // ------------------------------------------------------------------
    // Convenience flag tests
    // ------------------------------------------------------------------
    fun isOn(): Boolean       = (mode.toInt() and TAM_ON.toInt())        != 0
    fun isLoop(): Boolean     = (mode.toInt() and TAM_LOOP.toInt())      != 0
    fun isReverse(): Boolean  = (mode.toInt() and TAM_REVERSE.toInt())   != 0
    fun isPingPong(): Boolean = (mode.toInt() and TAM_PING_PONG.toInt()) != 0
    fun isSmooth(): Boolean   = (mode.toInt() and TAM_SMOOTH.toInt())    != 0
    fun isRotate(): Boolean   = (mode.toInt() and TAM_ROTATE.toInt())    != 0
    fun isScale(): Boolean    = (mode.toInt() and TAM_SCALE.toInt())     != 0

    // ------------------------------------------------------------------
    // Reset to defaults  (mirrors LLTextureAnim::reset())
    // ------------------------------------------------------------------
    fun reset() {
        mode   = 0u
        face   = -1
        sizeX  = 4u
        sizeY  = 4u
        start  = 0f
        length = 0f
        rate   = 1f
    }

    // ------------------------------------------------------------------
    // Equality helper  (mirrors LLTextureAnim::equals())
    // ------------------------------------------------------------------
    fun equalsOther(other: TextureAnim): Boolean =
        mode   == other.mode   &&
        face   == other.face   &&
        sizeX  == other.sizeX  &&
        sizeY  == other.sizeY  &&
        start  == other.start  &&
        length == other.length &&
        rate   == other.rate

    // ------------------------------------------------------------------
    // Binary packing / unpacking
    //
    // In the C++ viewer these write/read a 16-byte little-endian block:
    //   [0]     mode   (U8)
    //   [1]     face   (S8)
    //   [2]     sizeX  (U8)
    //   [3]     sizeY  (U8)
    //   [4..7]  start  (F32 LE)
    //   [8..11] length (F32 LE)
    //  [12..15] rate   (F32 LE)
    // ------------------------------------------------------------------

    /**
     * Serialise this animation into a 16-byte little-endian [ByteArray].
     *
     * Mirrors LLTextureAnim::packTAMessage(LLDataPacker&).
     */
    /**
     * Serialise this animation into a 16-byte little-endian [ByteArray].
     *
     * Wire layout (matches C++ LLTextureAnim::packTAMessage):
     *   [0]    mode   (U8)
     *   [1]    face   (S8)
     *   [2]    sizeX  (U8)
     *   [3]    sizeY  (U8)
     *   [4-7]  start  (F32 LE)
     *   [8-11] length (F32 LE)
     *  [12-15] rate   (F32 LE)
     */
    fun packTAMessage(): ByteArray {
        val buf = ByteBuffer.allocate(BLOCK_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(mode.toByte())
        buf.put(face)
        buf.put(sizeX.toByte())
        buf.put(sizeY.toByte())
        buf.putFloat(start)
        buf.putFloat(length)
        buf.putFloat(rate)
        return buf.array()
    }

    /**
     * Deserialise from a 16-byte little-endian [ByteArray].
     *
     * Wire layout matches [packTAMessage]. Tile sizes are clamped to >= 1
     * unless [TAM_SMOOTH] is set, in which case they may be 0.
     */
    fun unpackTAMessage(data: ByteArray) {
        require(data.size >= BLOCK_SIZE) { "TextureAnim data too short: ${data.size} < $BLOCK_SIZE" }
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        mode   = buf.get().toUByte()
        face   = buf.get()
        sizeX  = buf.get().toUByte()
        sizeY  = buf.get().toUByte()
        start  = buf.float
        length = buf.float
        rate   = buf.float
        // Clamp tile sizes: must be >= 1 unless TAM_SMOOTH is set
        if (!isSmooth()) {
            if (sizeX == 0u.toUByte()) sizeX = 1u
            if (sizeY == 0u.toUByte()) sizeY = 1u
        }
    }
}
