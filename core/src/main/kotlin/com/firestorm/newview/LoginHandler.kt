package com.firestorm.newview

import java.security.MessageDigest

data class LoginCredential(
    val identifier: MutableMap<String, String> = mutableMapOf(),
    val authenticator: MutableMap<String, String> = mutableMapOf()
) {
    fun userID(): String {
        return when (identifier["type"]) {
            "agent" -> {
                val first = identifier["first_name"] ?: ""
                val last  = identifier["last_name"] ?: ""
                "${first}_$last".lowercase()
            }
            "account" -> identifier["account_name"] ?: ""
            else -> ""
        }
    }
}

object LoginHandler {

    fun handle(queryMap: Map<String, String>, grid: String): Boolean {
        if (LoginInstance.authSuccess()) {
            return true
        }

        // no-op

        parse(queryMap)

        if (Startup.startupState == StartupState.STATE_FIRST) {
            return true
        }

        if (Startup.startupState < StartupState.STATE_LOGIN_CLEANUP) {
            PanelLogin.loadLoginPage()
            Startup.setStartupState(StartupState.STATE_LOGIN_CLEANUP)
        }
        return true
    }

    fun parseDirectLogin(url: String): Boolean {
        parse(parseQueryString(url))
        return true
    }

    fun loadSavedUserLoginInfo(): LoginCredential? {
        val cmdLineLogin = savedSettings("UserLoginInfoCmdLine") as? List<*>
        if (cmdLineLogin != null && cmdLineLogin.size == 3) {
            val password = cmdLineLogin[2].toString()
            val md5pass  = md5Hex(password)

            val identifier = mutableMapOf(
                "type"       to "agent",
                "first_name" to cmdLineLogin[0].toString(),
                "last_name"  to cmdLineLogin[1].toString()
            )
            val authenticator = mutableMapOf(
                "type"      to "hash",
                "algorithm" to "md5",
                "secret"    to md5pass
            )
            System.err.println("LoginHandler: loadSavedUserLoginInfo createCredential not yet implemented")
            return LoginCredential(identifier, authenticator)
        }
        System.err.println("LoginHandler: loadSavedUserLoginInfo loadCredential not yet implemented")
        return null
    }

    fun initializeLoginInfo(): LoginCredential = loadSavedUserLoginInfo() ?: LoginCredential()

    private fun parse(queryMap: Map<String, String>) {
        queryMap["grid"]?.let { grid ->
            System.err.println("LoginHandler: parse grid GridManager.setGridChoice not yet implemented")
        }

        when (queryMap["location"]) {
            "specify" -> {
                val region = queryMap.getOrDefault("region", "")
                System.err.println("LoginHandler: parse location=specify setStartSLURL not yet implemented")
            }
            "home" -> System.err.println("LoginHandler: parse location=home setStartSLURL not yet implemented")
            "last" -> System.err.println("LoginHandler: parse location=last setStartSLURL not yet implemented")
        }
    }

    private fun parseQueryString(url: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val queryPart = url.substringAfter('?', "")
        if (queryPart.isBlank()) return result
        queryPart.split('&').forEach { pair ->
            val parts = pair.split('=', limit = 2)
            result[parts[0]] = parts.getOrElse(1) { "" }
        }
        return result
    }

    private fun md5Hex(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun savedSettings(key: String): Any? {
        System.err.println("LoginHandler: savedSettings not yet implemented")
        return null
    }
}

object LoginInstance {
    fun authSuccess(): Boolean {
        System.err.println("LoginInstance: authSuccess not yet implemented")
        return false
    }
}
