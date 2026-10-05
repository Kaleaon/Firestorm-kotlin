package com.firestorm.llfilesystem

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.AssetType

class LLVFile(
    val id: LLUUID,
    val type: AssetType,
    private val fileSystem: LLFileSystem
) {
    fun readData(): ByteArray? = fileSystem.get(id, type)
    fun writeData(data: ByteArray): Boolean = fileSystem.put(id, type, data)
    fun exists(): Boolean = fileSystem.exists(id, type)
    fun remove(): Boolean = fileSystem.remove(id, type)
    fun getSize(): Int = fileSystem.get(id, type)?.size ?: 0
}
