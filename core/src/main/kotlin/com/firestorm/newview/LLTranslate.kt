package com.firestorm.newview

private const val AZURE_NOTRANSLATE_OPENING_TAG = """<div translate="no">"""
private const val AZURE_NOTRANSLATE_CLOSING_TAG = "</div>"

typealias KeyVerificationResult_fn = (LLTranslate.EService, Boolean, Int) -> Unit
typealias TranslationSuccess_fn = (String, String) -> Unit
typealias TranslationFailure_fn = (Int, String) -> Unit

abstract class LLTranslationAPIHandler {

    data class LanguagePair(val from: String, val to: String)

    abstract fun getTranslateUrl(fromLang: String, toLang: String, text: String): String

    abstract fun getKeyVerificationUrl(key: Map<String, Any>): String

    abstract fun checkVerificationResponse(response: Map<String, Any>, status: Int): Boolean

    abstract fun parseResponse(
        httpResponse: Map<String, Any>,
        status: Int,
        body: String,
        translation: StringBuilder,
        detectedLang: StringBuilder,
        errMsg: StringBuilder
    ): Boolean

    abstract fun isConfigured(): Boolean

    abstract fun getCurrentService(): LLTranslate.EService

    abstract fun verifyKey(key: Map<String, Any>, fnc: KeyVerificationResult_fn)

    open fun translateMessage(
        fromTo: LanguagePair,
        msg: String,
        success: TranslationSuccess_fn,
        failure: TranslationFailure_fn
    ) {
        System.err.println("LLTranslationAPIHandler: translateMessage not yet implemented")
    }

    abstract fun initHttpHeader(headers: MutableMap<String, String>, userAgent: String)

    abstract fun initHttpHeader(headers: MutableMap<String, String>, userAgent: String, key: Map<String, Any>)

    abstract fun sendMessageAndSuspend(
        url: String,
        msg: String,
        fromLang: String,
        toLang: String
    ): Map<String, Any>

    abstract fun verifyAndSuspend(url: String): Map<String, Any>

    fun verifyKeyCoro(service: LLTranslate.EService, key: Map<String, Any>, fnc: KeyVerificationResult_fn) {
        System.err.println("LLTranslationAPIHandler: verifyKeyCoro not yet implemented")
    }

    fun translateMessageCoro(
        fromTo: LanguagePair,
        msg: String,
        success: TranslationSuccess_fn,
        failure: TranslationFailure_fn
    ) {
        System.err.println("LLTranslationAPIHandler: translateMessageCoro not yet implemented")
    }
}

private class LLGoogleTranslationHandler : LLTranslationAPIHandler() {

    override fun getTranslateUrl(fromLang: String, toLang: String, text: String): String {
        val encoded = java.net.URLEncoder.encode(text, "UTF-8")
        var url = "https://www.googleapis.com/language/translate/v2?key=${getApiKey()}&q=$encoded&target=$toLang"
        if (fromLang.isNotEmpty()) url += "&source=$fromLang"
        return url
    }

    override fun getKeyVerificationUrl(key: Map<String, Any>): String =
        "https://www.googleapis.com/language/translate/v2/languages?key=${key["value"]}&target=en"

    override fun checkVerificationResponse(response: Map<String, Any>, status: Int): Boolean =
        status == 200

    override fun parseResponse(
        httpResponse: Map<String, Any>,
        status: Int,
        body: String,
        translation: StringBuilder,
        detectedLang: StringBuilder,
        errMsg: StringBuilder
    ): Boolean {
        System.err.println("LLGoogleTranslationHandler: parseResponse not yet implemented")
        return false
    }

    override fun isConfigured(): Boolean = getApiKey().isNotEmpty()

    override fun getCurrentService(): LLTranslate.EService = LLTranslate.EService.SERVICE_GOOGLE

    override fun verifyKey(key: Map<String, Any>, fnc: KeyVerificationResult_fn) {
        verifyKeyCoro(LLTranslate.EService.SERVICE_GOOGLE, key, fnc)
    }

    override fun initHttpHeader(headers: MutableMap<String, String>, userAgent: String) {
        headers["Accept"] = "application/json"
        headers["User-Agent"] = userAgent
    }

    override fun initHttpHeader(headers: MutableMap<String, String>, userAgent: String, key: Map<String, Any>) {
        initHttpHeader(headers, userAgent)
    }

    override fun sendMessageAndSuspend(url: String, msg: String, fromLang: String, toLang: String): Map<String, Any> {
        System.err.println("LLGoogleTranslationHandler: sendMessageAndSuspend not yet implemented")
        return emptyMap()
    }

    override fun verifyAndSuspend(url: String): Map<String, Any> {
        System.err.println("LLGoogleTranslationHandler: verifyAndSuspend not yet implemented")
        return emptyMap()
    }

    private fun getApiKey(): String {
        System.err.println("LLGoogleTranslationHandler: getApiKey not yet implemented")
        return ""
    }
}

private class LLAzureTranslationHandler : LLTranslationAPIHandler() {

    override fun getTranslateUrl(fromLang: String, toLang: String, text: String): String {
        val key = getApiKey()
        val endpoint = (key["endpoint"] as? String)?.trimEnd('/') ?: return ""
        return "$endpoint/translate?api-version=3.0&to=${getApiLanguageCode(toLang)}"
    }

    override fun getKeyVerificationUrl(key: Map<String, Any>): String {
        val endpoint = (key["endpoint"] as? String)?.trimEnd('/') ?: return ""
        return "$endpoint/translate?api-version=3.0&to=en"
    }

    override fun checkVerificationResponse(response: Map<String, Any>, status: Int): Boolean {
        if (status == 401 || status == 404) return false
        if (status != 400) return false
        val errorBody = response["error_body"] as? String ?: return false
        return try {
            System.err.println("LLAzureTranslationHandler: checkVerificationResponse JSON parse not yet implemented")
            false
        } catch (_: Exception) {
            false
        }
    }

    override fun parseResponse(
        httpResponse: Map<String, Any>,
        status: Int,
        body: String,
        translation: StringBuilder,
        detectedLang: StringBuilder,
        errMsg: StringBuilder
    ): Boolean {
        if (status != 200) {
            val errorBody = httpResponse["error_body"] as? String
            if (errorBody != null) errMsg.append(parseErrorResponse(errorBody))
            return false
        }
        System.err.println("LLAzureTranslationHandler: parseResponse JSON parse not yet implemented")
        return false
    }

    override fun isConfigured(): Boolean = getApiKey().isNotEmpty()

    override fun getCurrentService(): LLTranslate.EService = LLTranslate.EService.SERVICE_AZURE

    override fun verifyKey(key: Map<String, Any>, fnc: KeyVerificationResult_fn) {
        verifyKeyCoro(LLTranslate.EService.SERVICE_AZURE, key, fnc)
    }

    override fun initHttpHeader(headers: MutableMap<String, String>, userAgent: String) {
        initHttpHeader(headers, userAgent, getApiKey())
    }

    override fun initHttpHeader(headers: MutableMap<String, String>, userAgent: String, key: Map<String, Any>) {
        headers["Content-Type"] = "application/json"
        headers["User-Agent"] = userAgent
        (key["id"] as? String)?.let { headers["Ocp-Apim-Subscription-Key"] = it }
        (key["region"] as? String)?.let { headers["Ocp-Apim-Subscription-Region"] = it }
    }

    override fun sendMessageAndSuspend(url: String, msg: String, fromLang: String, toLang: String): Map<String, Any> {
        System.err.println("LLAzureTranslationHandler: sendMessageAndSuspend not yet implemented")
        return emptyMap()
    }

    override fun verifyAndSuspend(url: String): Map<String, Any> {
        System.err.println("LLAzureTranslationHandler: verifyAndSuspend not yet implemented")
        return emptyMap()
    }

    private fun parseErrorResponse(body: String): String {
        System.err.println("LLAzureTranslationHandler: parseErrorResponse not yet implemented")
        return ""
    }

    private fun getApiKey(): Map<String, Any> {
        System.err.println("LLAzureTranslationHandler: getApiKey not yet implemented")
        return emptyMap()
    }

    private fun getApiLanguageCode(lang: String): String =
        if (lang == "zh") "zh-CHT" else lang
}

private class LLDeepLTranslationHandler : LLTranslationAPIHandler() {

    override fun getTranslateUrl(fromLang: String, toLang: String, text: String): String {
        val key = getApiKey()
        val domain = (key["domain"] as? String)?.trimEnd('/') ?: return ""
        return "$domain/v2/translate"
    }

    override fun getKeyVerificationUrl(key: Map<String, Any>): String {
        val domain = (key["domain"] as? String)?.trimEnd('/') ?: return ""
        return "$domain/v2/translate"
    }

    override fun checkVerificationResponse(response: Map<String, Any>, status: Int): Boolean =
        status == 200

    override fun parseResponse(
        httpResponse: Map<String, Any>,
        status: Int,
        body: String,
        translation: StringBuilder,
        detectedLang: StringBuilder,
        errMsg: StringBuilder
    ): Boolean {
        if (status != 200) {
            val errorBody = httpResponse["error_body"] as? String
            if (errorBody != null) errMsg.append(parseErrorResponse(errorBody))
            return false
        }
        System.err.println("LLDeepLTranslationHandler: parseResponse JSON parse not yet implemented")
        return false
    }

    override fun isConfigured(): Boolean = getApiKey().isNotEmpty()

    override fun getCurrentService(): LLTranslate.EService = LLTranslate.EService.SERVICE_DEEPL

    override fun verifyKey(key: Map<String, Any>, fnc: KeyVerificationResult_fn) {
        verifyKeyCoro(LLTranslate.EService.SERVICE_DEEPL, key, fnc)
    }

    override fun initHttpHeader(headers: MutableMap<String, String>, userAgent: String) {
        initHttpHeader(headers, userAgent, getApiKey())
    }

    override fun initHttpHeader(headers: MutableMap<String, String>, userAgent: String, key: Map<String, Any>) {
        headers["Content-Type"] = "application/x-www-form-urlencoded"
        headers["User-Agent"] = userAgent
        (key["id"] as? String)?.let { headers["Authorization"] = "DeepL-Auth-Key $it" }
    }

    override fun sendMessageAndSuspend(url: String, msg: String, fromLang: String, toLang: String): Map<String, Any> {
        System.err.println("LLDeepLTranslationHandler: sendMessageAndSuspend not yet implemented")
        return emptyMap()
    }

    override fun verifyAndSuspend(url: String): Map<String, Any> {
        System.err.println("LLDeepLTranslationHandler: verifyAndSuspend not yet implemented")
        return emptyMap()
    }

    private fun parseErrorResponse(body: String): String {
        System.err.println("LLDeepLTranslationHandler: parseErrorResponse not yet implemented")
        return ""
    }

    private fun getApiKey(): Map<String, Any> {
        System.err.println("LLDeepLTranslationHandler: getApiKey not yet implemented")
        return emptyMap()
    }
}

object LLTranslate {

    enum class EService {
        SERVICE_AZURE,
        SERVICE_GOOGLE,
        SERVICE_DEEPL
    }

    private var charsSeen: Long = 0L
    private var charsSent: Long = 0L
    private var failureCount: Int = 0
    private var successCount: Int = 0

    private val googleHandler = LLGoogleTranslationHandler()
    private val azureHandler = LLAzureTranslationHandler()
    private val deepLHandler = LLDeepLTranslationHandler()

    fun translateMessage(
        fromLang: String,
        toLang: String,
        mesg: String,
        success: TranslationSuccess_fn,
        failure: TranslationFailure_fn
    ) {
        val handler = getPreferredHandler()
        handler.translateMessage(
            LLTranslationAPIHandler.LanguagePair(fromLang, toLang),
            addNoTranslateTags(mesg),
            success,
            failure
        )
    }

    fun verifyKey(service: EService, key: Map<String, Any>, fnc: KeyVerificationResult_fn) {
        getHandler(service).verifyKey(key, fnc)
    }

    fun getTranslateLanguage(): String {
        System.err.println("LLTranslate: getTranslateLanguage not yet implemented")
        return ""
    }

    fun isTranslationConfigured(): Boolean = getPreferredHandler().isConfigured()

    fun addNoTranslateTags(mesg: String): String {
        return when (getPreferredHandler().getCurrentService()) {
            EService.SERVICE_GOOGLE, EService.SERVICE_DEEPL -> mesg
            EService.SERVICE_AZURE -> {
                System.err.println("LLTranslate: addNoTranslateTags Azure URL tagging not yet implemented")
                mesg
            }
        }
    }

    fun removeNoTranslateTags(mesg: String): String {
        return when (getPreferredHandler().getCurrentService()) {
            EService.SERVICE_GOOGLE, EService.SERVICE_DEEPL -> mesg
            EService.SERVICE_AZURE -> {
                System.err.println("LLTranslate: removeNoTranslateTags Azure URL stripping not yet implemented")
                mesg
            }
        }
    }

    fun logCharsSeen(count: Long) { charsSeen += count }
    fun logCharsSent(count: Long) { charsSent += count }
    fun logSuccess(count: Int) { successCount += count }
    fun logFailure(count: Int) { failureCount += count }

    fun asLLSD(): Map<String, Any> {
        System.err.println("LLTranslate: asLLSD not yet implemented")
        return emptyMap()
    }

    private fun getPreferredHandler(): LLTranslationAPIHandler {
        System.err.println("LLTranslate: getPreferredHandler not yet implemented")
        return googleHandler
    }

    private fun getHandler(service: EService): LLTranslationAPIHandler = when (service) {
        EService.SERVICE_AZURE -> azureHandler
        EService.SERVICE_GOOGLE -> googleHandler
        EService.SERVICE_DEEPL -> deepLHandler
    }
}
