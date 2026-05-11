package com.firestorm.llui

import com.firestorm.llmath.Color4
import java.io.File

object UIColorTable {

    data class ColorEntry(val name: String, val color: Color4? = null, val reference: String? = null)

    private val loadedColors: MutableMap<String, UIColor> = mutableMapOf()
    private val userSetColors: MutableMap<String, UIColor> = mutableMapOf()

    fun getLoadedColors(): Map<String, UIColor> = loadedColors
    fun getUserColors(): Map<String, UIColor> = userSetColors

    fun get(name: String): Color4 = getColor(name).get()

    fun getColor(name: String, default: Color4 = Color4.MAGENTA): UIColor {
        userSetColors[name]?.let { return it }
        loadedColors[name]?.let { return it }
        return UIColor(default)
    }

    fun setColor(name: String, color: Color4) {
        val existing = userSetColors[name]
        if (existing != null) {
            existing.set(color)
            return
        }
        val base = loadedColors[name]
        if (base != null) {
            val originalColor = base.get()
            userSetColors[name] = UIColor(color)
            loadedColors[name] = UIColor(originalColor)
        } else {
            userSetColors[name] = UIColor(color)
        }
    }

    fun colorExists(name: String): Boolean =
        loadedColors.containsKey(name) || userSetColors.containsKey(name)

    fun isDefault(name: String): Boolean {
        val base = loadedColors[name] ?: return userSetColors.containsKey(name)
        val user = userSetColors[name] ?: return true
        return user.get() == base.get()
    }

    fun resetToDefault(name: String) {
        val user = userSetColors[name] ?: return
        val base = loadedColors[name] ?: return
        user.set(base.get())
    }

    fun clear() {
        clearTable(loadedColors)
        clearTable(userSetColors)
    }

    fun loadFromSettings(): Boolean {
        TODO("APR: use JVM equivalent of gDirUtilp->findSkinnedFilenames to load colors.xml from all skin dirs into loadedColors, then load user colors.xml into userSetColors")
    }

    fun saveUserSettings() {
        val entries = mutableListOf<ColorEntry>()
        for ((name, uiColor) in userSetColors) {
            val base = loadedColors[name]
            if (base != null && base.get() == uiColor.get()) continue
            entries.add(ColorEntry(name = name, color = uiColor.get()))
        }
        writeColorsXml(entries)
    }

    fun saveUserSettingsPaletteOnly() {
        val entries = mutableListOf<ColorEntry>()
        for ((name, uiColor) in userSetColors) {
            if (name.startsWith("ColorPaletteEntry")) {
                entries.add(ColorEntry(name = name, color = uiColor.get()))
            }
        }
        writeColorsXml(entries)
    }

    fun insertFromParams(entries: List<ColorEntry>) {
        insertFromEntries(entries, userSetColors)
    }

    private fun insertFromEntries(entries: List<ColorEntry>, table: MutableMap<String, UIColor>) {
        val unresolvedRefs: MutableMap<String, String> = mutableMapOf()
        for (entry in entries) {
            if (entry.color != null) {
                setColorInTable(entry.name, entry.color, table)
            } else if (entry.reference != null) {
                unresolvedRefs[entry.name] = entry.reference
            }
        }

        while (unresolvedRefs.isNotEmpty()) {
            val visitedRefs: MutableMap<String, String> = mutableMapOf()
            var current: String? = unresolvedRefs.keys.first()

            while (current != null) {
                if (visitedRefs.containsKey(current)) {
                    val cycle = visitedRefs.keys.joinToString("->") + "->$current"
                    System.err.println("Color cycle detected: $cycle")
                    visitedRefs.keys.forEach { unresolvedRefs.remove(it) }
                    break
                }
                val refTarget = unresolvedRefs[current]
                if (refTarget != null) {
                    visitedRefs[current] = refTarget
                    current = refTarget
                } else {
                    val resolved = loadedColors[unresolvedRefs[visitedRefs.keys.last()]]
                    if (resolved != null) {
                        for (k in visitedRefs.keys) {
                            setColorInTable(k, resolved.get(), loadedColors)
                            unresolvedRefs.remove(k)
                        }
                    } else {
                        for ((k, v) in visitedRefs) {
                            System.err.println("Color '$k' references non-existent color '${unresolvedRefs[k]}'")
                            unresolvedRefs.remove(k)
                        }
                    }
                    break
                }
            }
        }
    }

    fun loadFromFilename(filename: String, table: MutableMap<String, UIColor>): Boolean {
        TODO("APR: parse XML file '$filename' with root element 'colors', extract ColorEntry list, call insertFromEntries(entries, table)")
    }

    private fun clearTable(table: MutableMap<String, UIColor>) {
        for ((k, _) in table) {
            table[k] = UIColor(Color4.MAGENTA)
        }
    }

    private fun setColorInTable(name: String, color: Color4, table: MutableMap<String, UIColor>) {
        val existing = table[name]
        if (existing != null) {
            existing.set(color)
        } else {
            table[name] = UIColor(color)
        }
    }

    private fun writeColorsXml(entries: List<ColorEntry>) {
        TODO("APR: serialize 'entries' as XML <colors> document and write to gDirUtilp->getExpandedFilename(LL_PATH_USER_SETTINGS, 'colors.xml')")
    }
}
