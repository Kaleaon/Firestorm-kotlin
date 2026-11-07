package com.firestorm.i18n

import java.util.concurrent.ConcurrentHashMap

object LLTrans {
    private var currentLocale: String = "en"

    private val translations = ConcurrentHashMap<String, MutableMap<String, String>>()

    init {
        initializeDefaultTranslations()
    }

    private fun initializeDefaultTranslations() {
        val enMap = ConcurrentHashMap<String, String>()
        val esMap = ConcurrentHashMap<String, String>()

        // Default English strings
        enMap["LoadingData"] = "Loading data..."
        enMap["RetrievingData"] = "Retrieving data..."
        enMap["NoGroupDataFound"] = "No group data found."
        enMap["GodToolsObjectOwnedBy"] = "Object owned by"
        enMap["TexturePickerOutfitHeader"] = "Outfit Textures"
        enMap["land_type_unknown"] = "Unknown"
        enMap["MediaFilterWhitelist"] = "Whitelist"
        enMap["MediaFilterBlacklist"] = "Blacklist"
        enMap["IM_typing_start_string"] = "is typing..."
        enMap["IM_unavailable_text_label"] = "Unavailable"
        enMap["IM_muted_text_label"] = "Muted"
        enMap["IM_to_label"] = "To"
        enMap["TimeHour"] = "HH"
        enMap["TimeMin"] = "MM"
        enMap["TimeHour12"] = "hh"
        enMap["TimeAMPM"] = "AM/PM"
        enMap["NearbyChatTitle"] = "Nearby Chat"
        enMap["ScriptLimitsRequestWaiting"] = "Requesting script limits..."
        enMap["UserDictionary"] = "User Dictionary"
        enMap["GraphicsQualityLow"] = "Low"
        enMap["GraphicsQualityMid"] = "Mid"
        enMap["GraphicsQualityHigh"] = "High"
        enMap["no_limit"] = "No limit"
        enMap["unknown_script"] = "Unknown script"
        enMap["voice_morphing_url"] = "https://secondlife.com"
        enMap["GroupsNone"] = "None"
        enMap["AvatarNameWaiting"] = "Loading..."
        enMap["SocialFlickrConnecting"] = "Connecting to Flickr..."
        enMap["SocialFlickrPosting"] = "Posting to Flickr..."
        enMap["SocialFlickrErrorConnecting"] = "Error connecting to Flickr"
        enMap["SocialFlickrErrorPosting"] = "Error posting to Flickr"

        // Default Spanish strings
        esMap["LoadingData"] = "Cargando datos..."
        esMap["RetrievingData"] = "Recuperando datos..."
        esMap["NoGroupDataFound"] = "No se encontraron datos del grupo."
        esMap["GodToolsObjectOwnedBy"] = "Objeto propiedad de"
        esMap["TexturePickerOutfitHeader"] = "Texturas del atuendo"
        esMap["land_type_unknown"] = "Desconocido"
        esMap["MediaFilterWhitelist"] = "Lista blanca"
        esMap["MediaFilterBlacklist"] = "Lista negra"
        esMap["IM_typing_start_string"] = "está escribiendo..."
        esMap["IM_unavailable_text_label"] = "No disponible"
        esMap["IM_muted_text_label"] = "Silenciado"
        esMap["IM_to_label"] = "Para"
        esMap["TimeHour"] = "HH"
        esMap["TimeMin"] = "MM"
        esMap["TimeHour12"] = "hh"
        esMap["TimeAMPM"] = "AM/PM"
        esMap["NearbyChatTitle"] = "Chat cercano"
        esMap["ScriptLimitsRequestWaiting"] = "Solicitando límites de script..."
        esMap["UserDictionary"] = "Diccionario de usuario"
        esMap["GraphicsQualityLow"] = "Baja"
        esMap["GraphicsQualityMid"] = "Media"
        esMap["GraphicsQualityHigh"] = "Alta"
        esMap["no_limit"] = "Sin límite"
        esMap["unknown_script"] = "Script desconocido"
        esMap["voice_morphing_url"] = "https://secondlife.com"
        esMap["GroupsNone"] = "Ninguno"
        esMap["AvatarNameWaiting"] = "Cargando..."

        translations["en"] = enMap
        translations["es"] = esMap
    }

    fun setLocale(locale: String) {
        if (locale.isNotBlank()) {
            currentLocale = locale.lowercase()
        }
    }

    fun getLocale(): String = currentLocale

    fun loadTranslations(locale: String, bundle: Map<String, String>) {
        val targetMap = translations.computeIfAbsent(locale.lowercase()) { ConcurrentHashMap() }
        targetMap.putAll(bundle)
    }

    fun getString(key: String): String {
        if (key.isBlank()) return key

        // 1. Try current locale
        val activeMap = translations[currentLocale]
        if (activeMap != null) {
            val value = activeMap[key]
            if (value != null) return value
        }

        // 2. Fallback to English
        if (currentLocale != "en") {
            val enMap = translations["en"]
            if (enMap != null) {
                val enValue = enMap[key]
                if (enValue != null) return enValue
            }
        }

        // 3. Fallback to key itself (Never return "" or print stub messages!)
        return key
    }

    fun getString(key: String, args: Map<String, Any?>): String {
        var template = getString(key)
        for ((argKey, argVal) in args) {
            val strVal = argVal?.toString() ?: ""
            template = template.replace("[$argKey]", strVal)
                .replace("{$argKey}", strVal)
        }
        return template
    }

    fun getString(key: String, vararg args: Any?): String {
        val template = getString(key)
        if (args.isEmpty()) return template
        return try {
            String.format(template, *args)
        } catch (_: Exception) {
            template
        }
    }
}
