package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// LLGLTFFolderItem – minimal model item used by the folder view
// ---------------------------------------------------------------------------

enum class LLGLTFFolderItemType {
    TYPE_ROOT,
    TYPE_SCENE,
    TYPE_NODE,
    TYPE_MESH,
    TYPE_SKIN,
}

open class LLGLTFFolderItem(
    val itemId: Int = -1,
    private var name: String = "",
    val itemType: LLGLTFFolderItemType = LLGLTFFolderItemType.TYPE_ROOT
) {
    val children: MutableList<LLGLTFFolderItem> = mutableListOf()

    open fun getName(): String = name
    open fun getDisplayName(): String = name
    open fun getSearchableName(): String = name

    fun getSearchableDescription(): String = ""
    fun getSearchableCreatorName(): String = ""
    fun getSearchableUUIDString(): String = ""
    fun getSearchableAll(): String = ""

    fun hasChildren(): Boolean = children.isNotEmpty()

    fun isItemRenameable(): Boolean = false
    fun isItemMovable(): Boolean = false
    fun isItemRemovable(): Boolean = false
    fun isItemCopyable(): Boolean = false
    fun potentiallyVisible(): Boolean = true
    fun isFavorite(): Boolean = false
    fun isItemInTrash(): Boolean = false
    fun isAgentInventory(): Boolean = false
}

// ---------------------------------------------------------------------------
// LLGLTFSort – comparator used by the view model
// ---------------------------------------------------------------------------

class LLGLTFSort {
    fun compare(a: LLGLTFFolderItem, b: LLGLTFFolderItem): Boolean {
        // Dictionary comparison; returns true when a sorts before b
        return a.getName().compareTo(b.getName(), ignoreCase = false) < 0
    }
}

// ---------------------------------------------------------------------------
// LLGLTFFilter – pass-through filter (never active, never times out)
// ---------------------------------------------------------------------------

class LLGLTFFilter {
    private val empty: String = ""

    fun check(item: LLGLTFFolderItem): Boolean = true
    fun checkFolder(folder: LLGLTFFolderItem): Boolean = true
    fun setEmptyLookupMessage(message: String) {}
    fun getEmptyLookupMessage(isEmptyFolder: Boolean = false): String = empty
    fun showAllResults(): Boolean = true
    fun getStringMatchOffset(item: LLGLTFFolderItem): Int = -1  // std::string::npos analogue
    fun getFilterStringSize(): Int = 0

    fun isActive(): Boolean = false
    fun isModified(): Boolean = false
    fun clearModified() {}
    fun getName(): String = empty
    fun getFilterText(): String = empty
    fun setModified() {}

    fun resetTime(timeout: Int) {}
    fun isTimedOut(): Boolean = false

    fun isDefault(): Boolean = true
    fun isNotDefault(): Boolean = false
    fun markDefault() {}
    fun resetDefault() {}

    fun getCurrentGeneration(): Int = 0
    fun getFirstSuccessGeneration(): Int = 0
    fun getFirstRequiredGeneration(): Int = 0
}

// ---------------------------------------------------------------------------
// LLGLTFViewModel – ties sort and filter together
// ---------------------------------------------------------------------------

class LLGLTFViewModel {
    private val sorter = LLGLTFSort()
    val filter = LLGLTFFilter()

    fun sort(items: MutableList<LLGLTFFolderItem>) {
        items.sortWith(Comparator { a, b -> if (sorter.compare(a, b)) -1 else 1 })
    }

    fun startDrag(items: MutableList<LLGLTFFolderItem>): Boolean = false
}

// ---------------------------------------------------------------------------
// LLGLTFNode – concrete folder-view item (internal, mirrors C++ local class)
// ---------------------------------------------------------------------------

class LLGLTFNode(
    itemId: Int = -1,
    name: String = "",
    itemType: LLGLTFFolderItemType = LLGLTFFolderItemType.TYPE_NODE
) : LLGLTFFolderItem(itemId, name, itemType)
