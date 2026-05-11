package com.firestorm.newview

import com.firestorm.ui.MediaCtrl
import com.firestorm.startup.Startup

private const val THROTTLE_PERIOD = 5.0

abstract class CommandHandler(val command: String, val untrustedAccess: UntrustedAccess) {

    enum class UntrustedAccess {
        ALLOW,
        BLOCK,
        CLICK_ONLY,
        THROTTLE,
    }

    open fun canHandleUntrusted(
        params: Any,
        queryMap: Map<String, Any?>,
        web: MediaCtrl?,
        navType: String,
    ): Boolean = true

    abstract fun handle(
        params: Any,
        queryMap: Map<String, Any?>,
        grid: String,
        web: MediaCtrl?,
    ): Boolean

    init {
        CommandHandlerRegistry.add(command, untrustedAccess, this)
    }

    companion object {
        const val NAV_TYPE_CLICKED = "clicked"
        const val NAV_TYPE_EXTERNAL = "external"
        const val NAV_TYPE_NAVIGATED = "navigated"
    }
}

private data class CommandHandlerInfo(
    val untrustedAccess: CommandHandler.UntrustedAccess,
    val handler: CommandHandler,
)

private object CommandHandlerRegistry {
    private val map: MutableMap<String, CommandHandlerInfo> = mutableMapOf()

    fun add(cmd: String, untrustedAccess: CommandHandler.UntrustedAccess, handler: CommandHandler) {
        map[cmd] = CommandHandlerInfo(untrustedAccess, handler)
    }

    fun dispatch(
        cmd: String,
        params: Any,
        queryMap: Map<String, Any?>,
        grid: String,
        web: MediaCtrl?,
        navType: String,
        trustedBrowser: Boolean,
    ): Boolean {
        val info = map[cmd] ?: return false

        if (!trustedBrowser) {
            when (info.untrustedAccess) {
                CommandHandler.UntrustedAccess.ALLOW -> Unit

                CommandHandler.UntrustedAccess.BLOCK -> {
                    notifySlurlBlocked()
                    return true
                }

                CommandHandler.UntrustedAccess.CLICK_ONLY -> {
                    if (navType == CommandHandler.NAV_TYPE_CLICKED &&
                        info.handler.canHandleUntrusted(params, queryMap, web, navType)
                    ) {
                        Unit
                    } else {
                        notifySlurlBlocked()
                        return true
                    }
                }

                CommandHandler.UntrustedAccess.THROTTLE -> {
                    if (Startup.getStartupState() == Startup.State.FIRST) return true

                    if (!info.handler.canHandleUntrusted(params, queryMap, web, navType)) {
                        notifySlurlBlocked()
                        return true
                    }

                    if (navType != CommandHandler.NAV_TYPE_CLICKED) {
                        val curTime = System.currentTimeMillis() / 1000.0
                        if (curTime < lastThrottleTime + THROTTLE_PERIOD) {
                            notifySlurlThrottled()
                            return true
                        }
                        lastThrottleTime = curTime
                    }
                }
            }
        }

        return info.handler.handle(params, queryMap, grid, web)
    }

    fun enumerate(): Map<String, Map<String, Any>> =
        map.mapValues { (_, info) ->
            mapOf(
                "untrusted" to info.untrustedAccess.ordinal,
                "untrusted_str" to info.untrustedAccess.name,
            )
        }

    private var lastThrottleTime = 0.0
    private var slurlBlocked = false
    private var slurlThrottled = false

    private fun notifySlurlBlocked() {
        if (!slurlBlocked) {
            if (Startup.getStartupState() >= Startup.State.BROWSER_INIT) {
                TODO("APR: use JVM equivalent for LLNotificationsUtil::add(\"BlockedSLURL\")")
            }
            slurlBlocked = true
        }
    }

    private fun notifySlurlThrottled() {
        if (!slurlThrottled) {
            if (Startup.getStartupState() >= Startup.State.BROWSER_INIT) {
                TODO("APR: use JVM equivalent for LLNotificationsUtil::add(\"ThrottledSLURL\")")
            }
            slurlThrottled = true
        }
    }
}

object CommandDispatcher {
    fun dispatch(
        cmd: String,
        params: Any,
        queryMap: Map<String, Any?>,
        grid: String,
        web: MediaCtrl? = null,
        navType: String,
        trustedBrowser: Boolean,
    ): Boolean = CommandHandlerRegistry.dispatch(cmd, params, queryMap, grid, web, navType, trustedBrowser)

    fun enumerate(): Map<String, Map<String, Any>> = CommandHandlerRegistry.enumerate()
}
