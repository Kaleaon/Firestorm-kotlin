package com.firestorm.newview

import java.util.UUID

private fun modifyOutfit(append: Boolean, catId: UUID, model: LLInventoryModel) {
    val cat = model.getCategory(catId) ?: return
    TODO("APR: use JVM equivalent - check wearable item count limit and wear/replace outfit via AppearanceMgr")
}

class LLInventoryGalleryContextMenu(private val gallery: LLInventoryGallery) : LLListContextMenu() {

    var isRootFolder: Boolean = false

    fun createMenu(): LLContextMenu {
        TODO("APR: use JVM equivalent - build context menu from XML, register action/enable callbacks, call updateMenuItemsVisibility")
    }

    fun doToSelected(userdata: Any) {
        val action = userdata.toString()
        val selectedId = uuids.firstOrNull() ?: return
        TODO("APR: use JVM equivalent - look up object for selectedId, guard null")

        when (action) {
            "open_selected_folder" -> gallery.setRootFolder(selectedId)
            "open_in_new_window" -> newFolderWindow(selectedId)
            "properties" -> showItemProfile(selectedId)
            "restore" -> {
                for (id in uuids) {
                    TODO("APR: use JVM equivalent - reparent category or item to appropriate default folder")
                }
            }
            "copy_uuid" -> {
                TODO("APR: use JVM equivalent - copy asset UUID string to system clipboard")
            }
            "purge" -> {
                for (id in uuids) {
                    TODO("APR: use JVM equivalent - permanently delete inventory object by UUID")
                }
            }
            "goto" -> showItemOriginal(selectedId)
            "thumbnail" -> {
                TODO("APR: use JVM equivalent - show thumbnail change floater for selected UUIDs")
            }
            "cut" -> { if (gallery.canCut()) gallery.cut() }
            "paste" -> { if (gallery.canPaste()) gallery.paste() }
            "delete" -> gallery.deleteSelection()
            "copy" -> { if (gallery.canCopy()) gallery.copy() }
            "paste_link" -> gallery.pasteAsLink()
            "rename" -> rename(selectedId)
            "open", "open_original" -> {
                TODO("APR: use JVM equivalent - invoke bridge action for item's asset type")
            }
            "ungroup_folder_items" -> ungroupFolderItems(selectedId)
            "add_to_favorites" -> uuids.forEach { setFavorite(it, true) }
            "remove_from_favorites" -> uuids.forEach { setFavorite(it, false) }
            "replaceoutfit" -> modifyOutfit(false, selectedId, TODO("APR: reference global inventory model"))
            "addtooutfit" -> modifyOutfit(true, selectedId, TODO("APR: reference global inventory model"))
            "removefromoutfit" -> {
                TODO("APR: use JVM equivalent - call AppearanceMgr.takeOffOutfit for category linked UUID")
            }
            "take_off", "detach" -> {
                for (id in uuids) {
                    TODO("APR: use JVM equivalent - call AppearanceMgr.removeItemFromAvatar(id)")
                }
            }
            "wear_add" -> {
                for (id in uuids) {
                    TODO("APR: use JVM equivalent - call AppearanceMgr.wearItemOnAvatar(id, true, false)")
                }
            }
            "wear" -> {
                for (id in uuids) {
                    TODO("APR: use JVM equivalent - call AppearanceMgr.wearItemOnAvatar(id, true, true)")
                }
            }
            "activate" -> {
                for (id in uuids) {
                    TODO("APR: use JVM equivalent - activate gesture and notify inventory observers")
                }
            }
            "deactivate" -> {
                for (id in uuids) {
                    TODO("APR: use JVM equivalent - deactivate gesture and notify inventory observers")
                }
            }
            "replace_links" -> {
                TODO("APR: use JVM equivalent - show link-replace floater for selectedId")
            }
            "copy_slurl" -> {
                TODO("APR: use JVM equivalent - resolve landmark position and copy SLURL to clipboard")
            }
            "about" -> {
                TODO("APR: use JVM equivalent - show places panel with landmark details")
            }
            "show_on_map" -> {
                TODO("APR: use JVM equivalent - resolve landmark position and track it on world map")
            }
            "save_as" -> {
                TODO("APR: use JVM equivalent - open texture preview and trigger save-as dialog")
            }
            "copy_to_marketplace_listings", "move_to_marketplace_listings" -> {
                val copyOperation = action == "copy_to_marketplace_listings"
                TODO("APR: use JVM equivalent - move or copy item to marketplace listings folder, confirming for no-copy items")
            }
        }
    }

    fun rename(itemId: UUID) {
        TODO("APR: use JVM equivalent - show rename dialog pre-filled with object name, call onRename on confirm")
    }

    protected fun updateMenuItemsVisibility(menu: LLContextMenu) {
        val selectedId = uuids.firstOrNull() ?: return
        TODO("APR: use JVM equivalent - compute item/folder flags and call hide_context_entries with appropriate item/disabled lists")
    }

    protected fun fileUploadLocation(userdata: Any) {
        val param = userdata.toString()
        LLInventoryAction.fileUploadLocation(uuids.first(), param)
    }

    protected fun isUploadLocationSelected(userdata: Any): Boolean {
        val param = userdata.toString()
        return LLInventoryAction.isFileUploadLocation(uuids.first(), param)
    }

    protected fun canSetUploadLocation(userdata: Any): Boolean {
        if (uuids.size != 1) return false
        TODO("APR: use JVM equivalent - check that selected UUID is a category")
    }

    companion object {
        fun onRename(notification: Any, response: Any) {
            TODO("APR: use JVM equivalent - parse new name from response, update category or item via server API")
        }
    }

    private fun enableContextMenuItem(userdata: Any): Boolean {
        TODO("APR: use JVM equivalent - evaluate enable condition for given menu action string")
    }

    private fun checkContextMenuItem(userdata: Any): Boolean {
        TODO("APR: use JVM equivalent - evaluate check/radio state for given menu action string")
    }
}

private fun isInboxFolder(itemId: UUID): Boolean {
    TODO("APR: use JVM equivalent - check if itemId is a descendant of the FT_INBOX category")
}

private fun canListOnMarketplace(id: UUID): Boolean {
    TODO("APR: use JVM equivalent - validate item/folder transfer permission and marketplace root existence")
}

private fun checkFolderForContentsOfType(
    id: UUID,
    model: LLInventoryModel,
    isType: LLInventoryCollectFunctor
): Boolean {
    TODO("APR: use JVM equivalent - collectDescendentsIf with isType functor and return items non-empty")
}

// ---------------------------------------------------------------------------
// Stub base types
// ---------------------------------------------------------------------------

open class LLListContextMenu {
    val uuids: MutableList<UUID> = mutableListOf()
    open fun show(ctrl: Any, ids: List<UUID>, x: Int, y: Int) {
        uuids.clear()
        uuids.addAll(ids)
        TODO("GPU: position and display context menu widget")
    }
    open fun hide() { TODO("GPU: hide the context menu widget") }
}

class LLContextMenu
