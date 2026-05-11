package com.firestorm.newview

// ---------------------------------------------------------------------------
// Inventory action initialisation helpers
//
// These functions register UI commit callbacks on panel/floater objects so
// that inventory actions (open, rename, delete, etc.) are wired to the
// correct handlers.  The registrar pattern mirrors LLUICtrl::
// CommitCallbackRegistry::Registrar from the C++ code.
// ---------------------------------------------------------------------------

fun initObjectInventoryPanelActions(
    panel: LLPanelInventory,
    registrar: LLUICtrl.CommitCallbackRegistry.Registrar
) {
    registrar.add("Inventory.DoToSelected") { control, userdata ->
        panel.doToSelected(userdata.asString())
    }
}

fun initInventoryActions(
    floater: LLInventoryView,
    registrar: LLUICtrl.CommitCallbackRegistry.Registrar
) {
    registrar.add("Inventory.DoToSelected") { control, userdata ->
        floater.doToSelected(userdata.asString())
    }
    registrar.add("Inventory.CloseAllFolders") { _, _ ->
        floater.closeAllFolders()
    }
    registrar.add("Inventory.EmptyTrash") { _, _ ->
        floater.emptyTrash()
    }
    registrar.add("Inventory.SortByName") { _, _ ->
        floater.setSortBy("name")
    }
    registrar.add("Inventory.SortByDate") { _, _ ->
        floater.setSortBy("date")
    }
    registrar.add("Inventory.FoldersAlwaysByName") { _, _ ->
        floater.setSortBy("foldersalwaysbyname")
    }
    registrar.add("Inventory.SystemFoldersToTop") { _, _ ->
        floater.setSortBy("systemfolderstotop")
    }
    registrar.add("Inventory.Search") { _, _ ->
        floater.toggleSearch()
    }
    registrar.add("Inventory.NewWindow") { _, _ ->
        floater.newInventoryWindow()
    }
    registrar.add("Inventory.ShowFilters") { _, _ ->
        floater.toggleFilters()
    }
    registrar.add("Inventory.ResetFilter") { _, _ ->
        floater.resetFilter()
    }
    registrar.add("Inventory.SetSortBy") { _, userdata ->
        floater.setSortBy(userdata.asString())
    }
}

fun initInventoryPanelActions(
    panel: LLInventoryPanel,
    registrar: LLUICtrl.CommitCallbackRegistry.Registrar
) {
    registrar.add("Inventory.DoToSelected") { _, userdata ->
        panel.doToSelected(userdata.asString())
    }
    registrar.add("Inventory.CloseAllFolders") { _, _ ->
        panel.closeAllFolders()
    }
    registrar.add("Inventory.EmptyTrash") { _, _ ->
        panel.emptyTrash()
    }
    registrar.add("Inventory.SortByName") { _, _ ->
        panel.setSortBy("name")
    }
    registrar.add("Inventory.SortByDate") { _, _ ->
        panel.setSortBy("date")
    }
    registrar.add("Inventory.FoldersAlwaysByName") { _, _ ->
        panel.setSortBy("foldersalwaysbyname")
    }
    registrar.add("Inventory.SystemFoldersToTop") { _, _ ->
        panel.setSortBy("systemfolderstotop")
    }
}
