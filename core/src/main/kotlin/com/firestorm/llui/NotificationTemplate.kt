package com.firestorm.llui

enum class CombineBehavior {
    REPLACE_WITH_NEW,
    COMBINE_WITH_NEW,
    KEEP_OLD,
    CANCEL_OLD
}

data class NotificationTemplateUrl(
    val option: Int = -1,
    val value: String = "",
    val target: String = "_blank"
)

data class NotificationTemplate(
    val name: String,
    val type: String = "",
    val message: String = "",
    val footer: String = "",
    val label: String = "",
    val icon: String = "",
    val unique: Boolean = false,
    val combineBehavior: CombineBehavior = CombineBehavior.REPLACE_WITH_NEW,
    val uniqueContext: MutableList<String> = mutableListOf(),
    val expireSeconds: UInt = 0u,
    val expireOption: UInt = 0u,
    val url: String = "",
    val urlOption: UInt = 0u,
    val urlTarget: String = "_blank",
    val forceUrlsExternal: Boolean = false,
    val persist: Boolean = false,
    val defaultFunctor: String = "",
    val form: NotificationForm = NotificationForm(),
    val priority: NotificationPriority = NotificationPriority.UNSPECIFIED,
    val soundName: String = "",
    val tags: MutableList<String> = mutableListOf(),
    val logToChat: Boolean = true,
    val logToIM: Boolean = false,
    val showToast: Boolean = true,
    val fadeToast: Boolean = true
)
