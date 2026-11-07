package com.firestorm.newview

import java.util.UUID

private fun modifyOutfit(append: Boolean, catId: UUID, model: LLInventoryModel) {
    val cat = model.getCategory(catId) ?: return
    System.err.println("LLInventoryGalleryMenu: use JVM equivalent - check wearable item count limit and wear/replace outfit via AppearanceMgr not yet implemented")
}

class LLInventoryGalleryContextMenu(private val gallery: LLInventoryGallery) : LLListContextMenu() {

    var isRootFolder: Boolean = false

    fun createMenu(): LLContextMenu {
        System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - build context menu from XML, register action/enable callbacks, call updateMenuItemsVisibility not yet implemented")
        return LLContextMenu()
    }

    fun doToSelected(userdata: Any) {
        val action = userdata.toString()
        val selectedId = uuids.firstOrNull() ?: return
        System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - look up object for selectedId, guard null not yet implemented")

        when (action) {
            "open_selected_folder" -> gallery.setRootFolder(selectedId)
            "open_in_new_window" -> newFolderWindow(selectedId)
            "properties" -> showItemProfile(selectedId)
            "restore" -> {
                for (id in uuids) {
                    System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - reparent category or item to appropriate default folder not yet implemented")
                }
            }
            "copy_uuid" -> {
                System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - copy asset UUID string to system clipboard not yet implemented")
            }
            "purge" -> {
                for (id in uuids) {
                    System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - permanently delete inventory object by UUID not yet implemented")
                }
            }
            "goto" -> showItemOriginal(selectedId)
            "thumbnail" -> {
                System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - show thumbnail change floater for selected UUIDs not yet implemented")
            }
            "cut" -> { if (gallery.canCut()) gallery.cut() }
            "paste" -> { if (gallery.canPaste()) gallery.paste() }
            "delete" -> gallery.deleteSelection()
            "copy" -> { if (gallery.canCopy()) gallery.copy() }
            "paste_link" -> gallery.pasteAsLink()
            "rename" -> rename(selectedId)
            "open", "open_original" -> {
                System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - invoke bridge action for item's asset type not yet implemented")
            }
            "ungroup_folder_items" -> ungroupFolderItems(selectedId)
            "add_to_favorites" -> uuids.forEach { setFavorite(it, true) }
            "remove_from_favorites" -> uuids.forEach { setFavorite(it, false) }
            "replaceoutfit" -> modifyOutfit(false, selectedId, LLInventoryModel())
            "addtooutfit" -> modifyOutfit(true, selectedId, LLInventoryModel())
            "removefromoutfit" -> {
                System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - call AppearanceMgr.takeOffOutfit for category linked UUID not yet implemented")
            }
            "take_off", "detach" -> {
                for (id in uuids) {
                    System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - call AppearanceMgr.removeItemFromAvatar(id) not yet implemented")
                }
            }
            "wear_add" -> {
                for (id in uuids) {
                    System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - call AppearanceMgr.wearItemOnAvatar(id, true, false) not yet implemented")
                }
            }
            "wear" -> {
                for (id in uuids) {
                    System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - call AppearanceMgr.wearItemOnAvatar(id, true, true) not yet implemented")
                }
            }
            "activate" -> {
                for (id in uuids) {
                    System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - activate gesture and notify inventory observers not yet implemented")
                }
            }
            "deactivate" -> {
                for (id in uuids) {
                    System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - deactivate gesture and notify inventory observers not yet implemented")
                }
            }
            "replace_links" -> {
                System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - show link-replace floater for selectedId not yet implemented")
            }
            "copy_slurl" -> {
                System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - resolve landmark position and copy SLURL to clipboard not yet implemented")
            }
            "about" -> {
                System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - show places panel with landmark details not yet implemented")
            }
            "show_on_map" -> {
                System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - resolve landmark position and track it on world map not yet implemented")
            }
            "save_as" -> {
                System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - open texture preview and trigger save-as dialog not yet implemented")
            }
            "copy_to_marketplace_listings", "move_to_marketplace_listings" -> {
                val copyOperation = action == "copy_to_marketplace_listings"
                System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - move or copy item to marketplace listings folder, confirming for no-copy items not yet implemented")
            }
        }
    }

    fun rename(itemId: UUID) {
        System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - show rename dialog pre-filled with object name, call onRename on confirm not yet implemented")
    }

    protected fun updateMenuItemsVisibility(menu: LLContextMenu) {
        val selectedId = uuids.firstOrNull() ?: return
        System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - compute item/folder flags and call hide_context_entries with appropriate item/disabled lists not yet implemented")
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
        System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - check that selected UUID is a category not yet implemented")
        return false
    }

    companion object {
        fun onRename(notification: Any, response: Any) {
            System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - parse new name from response, update category or item via server API not yet implemented")
        }
    }

    private fun enableContextMenuItem(userdata: Any): Boolean {
        System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - evaluate enable condition for given menu action string not yet implemented")
        return false
    }

    private fun checkContextMenuItem(userdata: Any): Boolean {
        System.err.println("LLInventoryGalleryContextMenu: use JVM equivalent - evaluate check/radio state for given menu action string not yet implemented")
        return false
    }
}

private fun isInboxFolder(itemId: UUID): Boolean {
    System.err.println("LLInventoryGalleryMenu: use JVM equivalent - check if itemId is a descendant of the FT_INBOX category not yet implemented")
    return false
}

private fun canListOnMarketplace(id: UUID): Boolean {
    System.err.println("LLInventoryGalleryMenu: use JVM equivalent - validate item/folder transfer permission and marketplace root existence not yet implemented")
    return false
}

private fun checkFolderForContentsOfType(
    id: UUID,
    model: LLInventoryModel,
    isType: LLInventoryCollectFunctor
): Boolean {
    System.err.println("LLInventoryGalleryMenu: use JVM equivalent - collectDescendentsIf with isType functor and return items non-empty not yet implemented")
    return false
}

// ---------------------------------------------------------------------------
// Stub base types
// ---------------------------------------------------------------------------

open class LLListContextMenu {
    val uuids: MutableList<UUID> = mutableListOf()
    open fun show(ctrl: Any, ids: List<UUID>, x: Int, y: Int) {
        uuids.clear()
        uuids.addAll(ids)
        // no-op
    }
    open fun hide() {
        // no-op
    }
}

class LLContextMenu

open class LLInventoryModel {
    open fun getCategory(id: UUID): Any? = null
}
