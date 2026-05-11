/**
 * LLCipher.kt
 * Kotlin port of llcipher.h
 *
 * Encryption-cipher abstraction used throughout the Second Life message layer.
 * Concrete implementations (AES, XOR, null) implement this interface.
 */
package com.firestorm.llmessage

/**
 * Abstract cipher interface.
 *
 * Mirrors the C++ `LLCipher` pure-virtual base class.  All sizes are in bytes
 * and the type mapping follows the project convention:
 *   C++ `U8*`  → Kotlin `ByteArray`
 *   C++ `U32`  → Kotlin `UInt`
 */
interface LLCipher {

    /**
     * Encrypt [srcLen] bytes from [src] and write the result into [dst].
     *
     * @param src    Input plaintext buffer.
     * @param srcLen Number of plaintext bytes to encrypt.
     * @param dst    Output ciphertext buffer; must be at least
     *               [requiredEncryptionSpace] bytes long.
     * @param dstLen Available space in [dst].
     * @return       Number of bytes written into [dst], or 0 on error.
     */
    fun encrypt(src: ByteArray, srcLen: Int, dst: ByteArray, dstLen: Int): Int

    /**
     * Decrypt [srcLen] bytes from [src] and write the result into [dst].
     *
     * @param src    Input ciphertext buffer.
     * @param srcLen Number of ciphertext bytes to decrypt.
     * @param dst    Output plaintext buffer.
     * @param dstLen Available space in [dst].
     * @return       Number of bytes written into [dst], or 0 on error.
     */
    fun decrypt(src: ByteArray, srcLen: Int, dst: ByteArray, dstLen: Int): Int

    /**
     * Return the minimum output-buffer size required to encrypt [srcLen] bytes
     * of plaintext.
     *
     * This is an *estimate*; callers must still check the return value of
     * [encrypt]. For block ciphers with padding the value is typically rounded
     * up to the next block boundary.
     *
     * @param srcLen Number of plaintext bytes to be encrypted.
     * @return Minimum number of bytes the ciphertext output buffer must hold.
     */
    fun requiredEncryptionSpace(srcLen: Int): Int
}
