package com.firestorm.newview

object ConfirmationManager {

    enum class Type { NONE, CLICK, PASSWORD }

    abstract class ListenerBase {
        abstract fun confirmed(password: String)
    }

    class Listener<T>(
        private val obj: T,
        private val func: T.(String) -> Unit
    ) : ListenerBase() {
        override fun confirmed(password: String) {
            obj.func(password)
        }
    }

    fun confirm(type: Type, action: String, listener: ListenerBase) {
        val args = mapOf("ACTION" to action)
        when (type) {
            Type.CLICK -> NotificationUtil.add("ConfirmPurchase", args) { notification, response ->
                if (NotificationUtil.getSelectedOption(notification, response) == 0) {
                    listener.confirmed("")
                }
            }
            Type.PASSWORD -> NotificationUtil.add("ConfirmPurchasePassword", args) { notification, response ->
                val text = response["message"] ?: ""
                if (NotificationUtil.getSelectedOption(notification, response) == 0) {
                    listener.confirmed(text)
                }
            }
            Type.NONE -> listener.confirmed("")
        }
    }

    fun confirm(type: String, action: String, listener: ListenerBase) {
        val decodedType = when (type) {
            "click" -> Type.CLICK
            "password" -> Type.PASSWORD
            else -> Type.NONE
        }
        confirm(decodedType, action, listener)
    }

    fun <T> confirm(type: Type, action: String, obj: T, func: T.(String) -> Unit) {
        confirm(type, action, Listener(obj, func))
    }

    fun <T> confirm(type: String, action: String, obj: T, func: T.(String) -> Unit) {
        confirm(type, action, Listener(obj, func))
    }
}

object NotificationUtil {
    fun add(
        name: String,
        args: Map<String, String> = emptyMap(),
        callback: ((Map<String, String>, Map<String, String>) -> Unit)? = null
    ) {
        System.err.println("NotificationUtil: add not yet implemented")
    }

    fun getSelectedOption(notification: Map<String, String>, response: Map<String, String>): Int {
        return 0
    }
}
