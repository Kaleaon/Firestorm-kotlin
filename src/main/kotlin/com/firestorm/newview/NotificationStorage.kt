package com.firestorm.newview

import java.io.File
import java.io.FileReader
import java.io.FileWriter

interface NotificationResponderInterface {
    fun fromLLSD(params: Map<String, Any>)
    fun toLLSD(): Map<String, Any>
}

typealias NotificationPtr = Notification
typealias ResponderConstructor = (params: Map<String, Any>) -> NotificationResponderInterface

object ResponderRegistry {
    private val registry: MutableMap<String, ResponderConstructor> = mutableMapOf()

    fun register(name: String, constructor: ResponderConstructor) {
        registry[name] = constructor
    }

    fun createResponder(notificationName: String, params: Map<String, Any>): NotificationResponderInterface? {
        return registry[notificationName]?.invoke(params)
    }
}

open class NotificationStorage(private var fileName: String) {
    private var oldFileName: String = ""

    protected fun setFileName(name: String) { fileName = name }
    protected fun setOldFileName(name: String) { oldFileName = name }

    protected fun writeNotifications(notificationData: Map<String, Any>): Boolean {
        return try {
            val file = File(fileName)
            val writer = FileWriter(file)
            // TODO("APR: use JVM XML serializer instead of LLSDXMLFormatter")
            writer.write(serializeLLSD(notificationData))
            writer.close()
            true
        } catch (e: Exception) {
            System.err.println("NotificationStorage: Failed to open file '$fileName': ${e.message}")
            false
        }
    }

    protected fun readNotifications(notificationData: MutableMap<String, Any>, isNewFilename: Boolean = true): Boolean {
        val filename = if (isNewFilename) fileName else oldFileName
        System.out.println("NotificationStorage: starting read '$filename'")

        notificationData.clear()

        return try {
            val file = File(filename)
            if (!file.exists()) {
                System.err.println("NotificationStorage: Failed to open file '$filename'")
                if (isNewFilename && oldFileName.isNotEmpty()) {
                    val ok = readNotifications(notificationData, false)
                    if (ok) {
                        writeNotifications(notificationData)
                        File(oldFileName).delete()
                    }
                    return ok
                }
                return false
            }
            // TODO("APR: use JVM XML parser instead of LLSDXMLParser")
            val parsed = parseLLSD(FileReader(file).readText())
            notificationData.putAll(parsed)
            true
        } catch (e: Exception) {
            System.err.println("NotificationStorage: Failed to parse notifications from '$filename': ${e.message}")
            File(filename).delete()
            System.err.println("NotificationStorage: Removed invalid open notifications file '$filename'")
            if (isNewFilename && oldFileName.isNotEmpty()) {
                val ok = readNotifications(notificationData, false)
                if (ok) {
                    writeNotifications(notificationData)
                    File(oldFileName).delete()
                }
                ok
            } else {
                false
            }
        }
    }

    protected fun createResponder(notificationName: String, params: Map<String, Any>): NotificationResponderInterface? {
        return ResponderRegistry.createResponder(notificationName, params)
    }

    // Stubs for LLSD serialization — real implementation would use an XML library.
    private fun serializeLLSD(data: Map<String, Any>): String {
        TODO("APR: use JVM equivalent XML serialization for LLSD")
    }

    private fun parseLLSD(xml: String): Map<String, Any> {
        TODO("APR: use JVM equivalent XML parsing for LLSD")
    }
}
