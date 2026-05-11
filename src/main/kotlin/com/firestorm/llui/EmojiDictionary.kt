package com.firestorm.llui

// llwchar in C++ is a 32-bit code point; Kotlin Int covers the full Unicode range.
typealias WChar = Int

data class EmojiDescriptor(
    val character: WChar,
    val category: String,
    val shortCodes: MutableList<String> = mutableListOf()
) {
    fun getShortCodes(): String = shortCodes.joinToString(", ")
}

data class EmojiGroup(
    val character: WChar,
    val categories: MutableList<String> = mutableListOf()
)

data class EmojiSearchResult(
    val character: WChar,
    val string: String,
    val begin: Int,
    val end: Int
)

object EmojiDictionary {

    private const val SKINNED_EMOJI_FILENAME = "emoji_characters.xml"
    private const val SKINNED_CATEGORY_FILENAME = "emoji_categories.xml"
    private const val COMMON_GROUP_FILENAME = "emoji_groups.xml"
    private const val GROUP_NAME_SKIP = "skip"
    // U+1F302 "Closed Umbrella" used as the icon for the catch-all "Others" group
    private const val GROUP_OTHERS_IMAGE_INDEX: WChar = 0x1F302

    private val groups: MutableList<EmojiGroup> = mutableListOf()
    private val emojis: MutableList<EmojiDescriptor> = mutableListOf()
    private val skipCategories: MutableList<String> = mutableListOf()

    private val translations: MutableMap<String, String> = mutableMapOf()
    private val category2Group: MutableMap<String, EmojiGroup> = mutableMapOf()
    val emoji2Descr: MutableMap<WChar, EmojiDescriptor> = mutableMapOf()
    val category2Descrs: MutableMap<String, MutableList<EmojiDescriptor>> = mutableMapOf()
    val shortCode2Descr: MutableMap<String, EmojiDescriptor> = mutableMapOf()

    fun getGroups(): List<EmojiGroup> = groups

    fun initClass() {
        loadTranslations()
        loadGroups()
        loadEmojis()
    }

    fun findMatchingEmojis(needle: String): String {
        val effectiveNeedle = if (needle.startsWith(":")) needle.substring(1) else needle
        val lower = effectiveNeedle.lowercase()
        return emojis
            .filter { descr ->
                descr.shortCodes.any { it.contains(lower, ignoreCase = true) } ||
                    descr.category.contains(lower, ignoreCase = true)
            }
            .map { String(Character.toChars(it.character)) }
            .joinToString("")
    }

    // Searches for needle (a shortcode prefix starting with ':') inside shortCode using
    // a loose match that skips separator characters ('-', '_', '+') in the shortcode.
    fun searchInShortCode(shortCode: String, needle: String): Pair<Int, Int>? {
        var end = 1
        var index = 1

        val needleLower = needle.lowercase()
        val codeLower = shortCode.lowercase()

        // Find begin: locate where needle[1] first appears in shortCode
        val d0 = needleLower.getOrNull(index++) ?: return null
        var begin = 0
        while (end < codeLower.length) {
            val s = codeLower[end++]
            if (s == d0) {
                begin = end - 1
                break
            }
        }
        if (begin == 0) return null

        // Find end: step through the rest of needle, tolerating separator chars in shortCode
        var d = needleLower.getOrNull(index++) ?: return Pair(begin, end)
        while (end < codeLower.length && index <= needleLower.length) {
            val s = codeLower[end++]
            if (s == d) {
                if (index == needleLower.length) return Pair(begin, end)
                d = needleLower.getOrNull(index++) ?: return Pair(begin, end)
                continue
            }
            if (s == '-' || s == '_' || s == '+') continue
            break
        }
        return null
    }

    fun findByShortCode(needle: String): List<EmojiSearchResult> {
        if (needle.isEmpty() || needle.first() != ':') return emptyList()

        // Group by begin position so results closest to the start of the shortcode appear first
        val grouped = sortedMapOf<Int, MutableList<EmojiSearchResult>>()

        for (d in emojis) {
            if (d.shortCodes.isEmpty()) continue
            val shortCode = d.shortCodes.first()
            if (shortCode.length >= needle.length && shortCode.first() == needle.first()) {
                val match = searchInShortCode(shortCode, needle) ?: continue
                grouped.getOrPut(match.first) { mutableListOf() }
                    .add(EmojiSearchResult(d.character, shortCode, match.first, match.second))
            }
        }

        return grouped.values.flatten()
    }

    fun getDescriptorFromEmoji(emoji: WChar): EmojiDescriptor? = emoji2Descr[emoji]

    fun getDescriptorFromShortCode(shortCode: String): EmojiDescriptor? = shortCode2Descr[shortCode]

    fun getNameFromEmoji(ch: WChar): String = emoji2Descr[ch]?.shortCodes?.firstOrNull() ?: ""

    fun isEmoji(ch: WChar): Boolean {
        if (ch == 0xA9 || ch == 0xAE || (ch in 0x2000 until 0x3300) || (ch in 0x1F000 until 0x20000)) {
            return emoji2Descr.containsKey(ch)
        }
        return false
    }

    private fun loadTranslations() {
        TODO("APR: use JVM equivalent — find and parse $SKINNED_CATEGORY_FILENAME, populate translations map")
    }

    private fun loadGroups() {
        TODO("APR: use JVM equivalent — parse $COMMON_GROUP_FILENAME, populate groups/skipCategories/category2Group")
    }

    private fun loadEmojis() {
        TODO("APR: use JVM equivalent — find and parse $SKINNED_EMOJI_FILENAME, populate emojis/emoji2Descr/category2Descrs/shortCode2Descr")
    }

    private fun loadIcon(sd: Any?): WChar {
        TODO("APR: use JVM equivalent — extract single-codepoint 'Character' field from LLSD/XML node")
    }

    private fun loadCategories(sd: Any?): MutableList<String> {
        TODO("APR: use JVM equivalent — extract 'Categories' array from LLSD/XML node")
    }

    private fun loadShortCodes(sd: Any?): MutableList<String> {
        TODO("APR: use JVM equivalent — extract 'ShortCodes' array from LLSD/XML node and lowercase each entry")
    }

    private fun translateCategories(categories: MutableList<String>) {
        for (i in categories.indices) {
            categories[i] = translations[categories[i]] ?: categories[i]
        }
    }
}
