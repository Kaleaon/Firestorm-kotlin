package com.firestorm.llui

import java.util.UUID

object Clipboard {
    private val objects: MutableList<UUID> = mutableListOf()
    private var string: String = ""
    private var cutMode: Boolean = false
    private var generation: Int = 0

    fun reset() {
        generation++
        objects.clear()
        cutMode = false
        string = ""
    }

    fun getGeneration(): Int = generation

    fun copyToClipboard(src: String, pos: Int, len: Int, usePrimary: Boolean = false): Boolean {
        return addToClipboard(src, pos, len, usePrimary)
    }

    fun addToClipboard(src: String, pos: Int, len: Int, usePrimary: Boolean = false): Boolean {
        return try {
            string = src.substring(pos, pos + len)
            if (usePrimary) {
                TODO("APR: use JVM equivalent for copyTextToPrimary")
            } else {
                TODO("APR: use JVM equivalent for copyTextToClipboard (e.g. java.awt.Toolkit.getDefaultToolkit().systemClipboard)")
            }
        } catch (e: Exception) {
            false
        }
    }

    fun pasteFromClipboard(usePrimary: Boolean = false): Pair<Boolean, String> {
        return if (usePrimary) {
            TODO("APR: use JVM equivalent for pasteTextFromPrimary")
        } else {
            TODO("APR: use JVM equivalent for pasteTextFromClipboard (e.g. java.awt.Toolkit.getDefaultToolkit().systemClipboard)")
        }
    }

    fun isTextAvailable(usePrimary: Boolean = false): Boolean {
        return if (usePrimary) {
            TODO("APR: use JVM equivalent for isPrimaryTextAvailable")
        } else {
            TODO("APR: use JVM equivalent for isClipboardTextAvailable")
        }
    }

    fun copyToClipboard(src: UUID, type: Int = ASSET_TYPE_NONE): Boolean {
        reset()
        return addToClipboard(src, type)
    }

    fun addToClipboard(src: UUID, type: Int = ASSET_TYPE_NONE): Boolean {
        if (src == NIL_UUID) return false
        var res = true
        if (isAssetIdKnowable(type)) {
            val (ok, _) = runCatching {
                addToClipboard(src.toString(), 0, src.toString().length)
            }.fold({ it }, { false to "" })
            res = ok as? Boolean ?: false
        }
        if (res) {
            objects.add(src)
            generation++
        }
        return res
    }

    fun pasteFromClipboard(invObjects: MutableList<UUID>): Boolean {
        if (objects.isEmpty()) return false
        invObjects.clear()
        invObjects.addAll(objects)
        return true
    }

    fun hasContents(): Boolean = objects.isNotEmpty()

    fun isOnClipboard(obj: UUID): Boolean = objects.contains(obj)

    fun isCutMode(): Boolean = cutMode

    fun setCutMode(mode: Boolean) {
        cutMode = mode
        generation++
    }

    private val NIL_UUID: UUID = UUID(0L, 0L)
    private const val ASSET_TYPE_NONE = -1

    private fun isAssetIdKnowable(type: Int): Boolean {
        TODO("APR: use JVM equivalent for LLAssetType::lookupIsAssetIDKnowable")
    }
}
