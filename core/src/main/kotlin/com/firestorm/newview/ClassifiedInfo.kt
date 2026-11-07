package com.firestorm.newview

import com.firestorm.llsd.LLSD

// ────────────────────────────────────────────────────────────────────────────
// ClassifiedInfo — holds the global category map for classified ads
// ────────────────────────────────────────────────────────────────────────────

object ClassifiedInfo {

    // Maps category id (U32 in C++) → category name
    val categories: MutableMap<UInt, String> = mutableMapOf()

    fun loadCategories(options: LLSD) {
        for (entry in options.asArray()) {
            val name = entry["category_name"]
            if (!name.isDefined()) continue
            val id = entry["category_id"]
            if (!id.isDefined()) continue
            categories[id.asInt().toUInt()] = name.asString()
        }
    }
}
