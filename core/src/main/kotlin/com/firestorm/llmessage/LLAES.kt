/**
 * LLAES.kt
 * Kotlin port of the AES cipher layer (no llaes.h/cpp found in the source
 * tree; this implementation is derived from the project specification and
 * mirrors the structure of LLAESCipher used in the broader Linden codebase).
 *
 * Provides AES-256-CBC encryption/decryption backed by the JVM's
 * javax.crypto APIs, implementing the [LLCipher] interface.
 */
package com.firestorm.llmessage

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256 block cipher with CBC mode and PKCS#5 padding.
 *
 * Mirrors the C++ `LLAESCipher` class.  A 32-byte (256-bit) key is required;
 * the 16-byte IV is prepended to every encrypted output and consumed from the
 * front of every ciphertext during decryption.
 *
 * Usage:
 * ```kotlin
 * val cipher = LLAES()
 * cipher.setKey(myKey32Bytes)
 * val required = cipher.requiredEncryptionSpace(plaintext.size)
 * val output   = ByteArray(required)
 * val written  = cipher.encrypt(plaintext, plaintext.size, output, output.size)
 * ```
 */
class LLAES : LLCipher {

    companion object {
        /** AES block size in bytes. */
        const val BLOCK_SIZE: Int = 16

        /** Required key length in bytes for AES-256. */
        const val KEY_LENGTH: Int = 32

        private const val ALGORITHM      = "AES"
        private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    }

    // ── Key material ──────────────────────────────────────────────────────────

    private var keySpec: SecretKeySpec? = null

    /**
     * Set the 256-bit (32-byte) encryption key.
     *
     * @throws IllegalArgumentException if [key] is not exactly [KEY_LENGTH] bytes.
     */
    fun setKey(key: ByteArray) {
        require(key.size == KEY_LENGTH) {
            "LLAES requires a $KEY_LENGTH-byte key; got ${key.size} bytes."
        }
        keySpec = SecretKeySpec(key.copyOf(), ALGORITHM)
    }

    // =========================================================================
    // LLCipher implementation
    // =========================================================================

    /**
     * Encrypt [srcLen] bytes from [src] into [dst].
     *
     * The output format is:
     *   `[16-byte random IV][PKCS5-padded ciphertext]`
     *
     * @return Number of bytes written (IV + ciphertext), or 0 on error.
     */
    override fun encrypt(src: ByteArray, srcLen: Int, dst: ByteArray, dstLen: Int): Int {
        val key = keySpec ?: return 0
        return try {
            // Generate a random IV for every encryption operation.
            val iv = ByteArray(BLOCK_SIZE).also { java.security.SecureRandom().nextBytes(it) }
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)

            val ciphertext = cipher.doFinal(src, 0, srcLen)
            val totalLen   = BLOCK_SIZE + ciphertext.size

            if (dstLen < totalLen) return 0

            iv.copyInto(dst, destinationOffset = 0)
            ciphertext.copyInto(dst, destinationOffset = BLOCK_SIZE)
            totalLen
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Decrypt [srcLen] bytes from [src] into [dst].
     *
     * Expects the input to begin with the 16-byte IV produced by [encrypt].
     *
     * @return Number of plaintext bytes written, or 0 on error.
     */
    override fun decrypt(src: ByteArray, srcLen: Int, dst: ByteArray, dstLen: Int): Int {
        val key = keySpec ?: return 0
        if (srcLen < BLOCK_SIZE) return 0
        return try {
            val iv         = src.copyOfRange(0, BLOCK_SIZE)
            val ivSpec     = IvParameterSpec(iv)
            val ciphertext = src.copyOfRange(BLOCK_SIZE, srcLen)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)

            val plaintext = cipher.doFinal(ciphertext)
            if (dstLen < plaintext.size) return 0

            plaintext.copyInto(dst)
            plaintext.size
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Return the output buffer size needed to encrypt [srcLen] plaintext bytes.
     *
     * Formula: 16-byte IV + plaintext rounded up to the next AES block boundary
     * (PKCS#5 always adds at least one padding byte, so we use `+ BLOCK_SIZE`
     * rather than `- 1` to match the C++ convention of over-estimating).
     */
    override fun requiredEncryptionSpace(srcLen: Int): Int {
        // Next multiple of BLOCK_SIZE that is > srcLen (PKCS5 always pads).
        val paddedSize = ((srcLen / BLOCK_SIZE) + 1) * BLOCK_SIZE
        return BLOCK_SIZE + paddedSize   // IV + padded ciphertext
    }
}
