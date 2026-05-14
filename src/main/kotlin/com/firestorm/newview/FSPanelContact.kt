package com.firestorm.newview

import java.util.UUID

private const val MAX_SELECTIONS = 20

private val CS_SET_ALL_SETS  = "__all_sets__"
private val CS_SET_NO_SETS   = "__no_sets__"
private val CS_SET_PSEUDONYM = "__pseudonyms__"
private val CS_SET_EXTRA_AVS = "__extra_avs__"

class AvatarItemOnlineStatusComparator : Comparator<AvatarListItem> {
    override fun compare(a: AvatarListItem, b: AvatarListItem): Int {
        val onlineA = AvatarTracker.isBuddyOnline(a.avatarId)
        val onlineB = AvatarTracker.isBuddyOnline(b.avatarId)
        if (onlineA != onlineB) return if (onlineA) -1 else 1
        return a.comparableName.compareTo(b.comparableName)
    }
}

data class AvatarListItem(
    val avatarId: UUID,
    val comparableName: String,
)

object AvatarTracker {
    fun isBuddyOnline(id: UUID): Boolean {
        return false
    }

    fun addObserver(observer: Any) {
        System.err.println("AvatarTracker: register friend-status observer not yet implemented")
    }

    fun removeObserver(observer: Any) {
        System.err.println("AvatarTracker: unregister friend-status observer not yet implemented")
    }

    fun allBuddies(): Map<UUID, Any> {
        return emptyMap()
    }
}

object LGGContactSets {
    enum class ContactSetUpdate { UPDATED_LISTS, UPDATED_MEMBERS }

    fun getAllContactSets(): List<String> {
        return emptyList()
    }

    fun isInternalSetName(name: String): Boolean =
        name in setOf(CS_SET_ALL_SETS, CS_SET_NO_SETS, CS_SET_PSEUDONYM, CS_SET_EXTRA_AVS)

    fun getFriendsInAnySet(): MutableList<UUID> {
        return mutableListOf()
    }

    fun isFriendInAnySet(id: UUID): Boolean {
        return false
    }

    fun getListOfPseudonymAvs(): MutableList<UUID> {
        return mutableListOf()
    }

    fun getListOfNonFriends(): MutableList<UUID> {
        return mutableListOf()
    }

    fun getContactSetMembers(name: String): List<UUID> {
        return emptyList()
    }

    fun getSortByOnlineStatusForSet(name: String): Boolean {
        return false
    }

    fun hasPseudonym(ids: List<UUID>): Boolean {
        return false
    }

    fun hasDisplayNameRemoved(ids: List<UUID>): Boolean {
        return false
    }

    fun clearPseudonym(id: UUID) {
        System.err.println("LGGContactSets: remove stored pseudonym for this avatar not yet implemented")
    }

    fun removeDisplayName(id: UUID) {
        System.err.println("LGGContactSets: mark avatar's display name as hidden not yet implemented")
    }

    fun addToSet(ids: List<UUID>, setName: String) {
        System.err.println("LGGContactSets: add each id to the named contact set not yet implemented")
    }

    val onChanged: MutableList<(ContactSetUpdate) -> Unit> = mutableListOf()
}

class FSPanelContactSets {

    private val avatarSelections: MutableList<UUID> = mutableListOf()
    private var filterSubString: String = ""

    private var contactSetCombo: ComboBox? = null
    private var avatarList: AvatarListWidget? = null

    init {
        AvatarTracker.addObserver(this)
        LGGContactSets.onChanged.add { type -> updateSets(type) }
    }

    fun dispose() {
        AvatarTracker.removeObserver(this)
    }

    fun postBuild(): Boolean {
        System.err.println("FSPanelContactSets: inflate layout XML not yet implemented")
        return false
    }

    fun wireChildren(combo: ComboBox, list: AvatarListWidget, filterEditor: FilterEditor?) {
        contactSetCombo = combo
        combo.onCommit = { refreshSetList() }
        refreshContactSets()

        filterEditor?.onCommit = { text -> onFilterEdit(text) }

        avatarList = list
        list.onCommit = { onSelectAvatar() }
        list.onDoubleClick = { onClickStartIM() }
        list.onAvatarDrop = { id, drop -> handleAvatarDrop(id, drop) }
        generateAvatarList(combo.value)
    }

    private fun onSelectAvatar() {
        avatarSelections.clear()
        avatarSelections.addAll(avatarList?.selectedIds ?: emptyList())
        resetControls()
    }

    fun generateAvatarList(contactSet: String) {
        val list = avatarList ?: return
        list.clear()

        val ids: List<UUID> = when (contactSet) {
            CS_SET_ALL_SETS -> LGGContactSets.getFriendsInAnySet()
            CS_SET_NO_SETS -> {
                val all = AvatarTracker.allBuddies()
                all.keys.filterNot { LGGContactSets.isFriendInAnySet(it) }
            }
            CS_SET_PSEUDONYM -> LGGContactSets.getListOfPseudonymAvs()
            CS_SET_EXTRA_AVS -> LGGContactSets.getListOfNonFriends()
            else -> {
                if (!LGGContactSets.isInternalSetName(contactSet))
                    LGGContactSets.getContactSetMembers(contactSet)
                else emptyList()
            }
        }

        list.setIds(ids)
        System.err.println("FSPanelContactSets: update member-count label not yet implemented")
        updateAvatarListSorting()
        resetControls()
    }

    fun onFriendStatusChanged(changedMask: UInt) {
        val onlineBit: UInt = 0x01u
        if ((changedMask and onlineBit) != 0u && shouldSortByOnlineStatus()) {
            avatarList?.sort(AvatarItemOnlineStatusComparator())
        }
    }

    fun updateSets(type: LGGContactSets.ContactSetUpdate) {
        when (type) {
            LGGContactSets.ContactSetUpdate.UPDATED_LISTS -> {
                refreshContactSets()
                refreshSetList()
            }
            LGGContactSets.ContactSetUpdate.UPDATED_MEMBERS -> refreshSetList()
        }
    }

    private fun refreshContactSets() {
        val combo = contactSetCombo ?: return
        combo.clearRows()
        val sets = LGGContactSets.getAllContactSets()
        if (sets.isNotEmpty()) {
            sets.forEach { combo.addEntry(it) }
            combo.addSeparator()
        }
        combo.addEntry("All Sets", CS_SET_ALL_SETS)
        combo.addEntry("No Sets", CS_SET_NO_SETS)
        combo.addEntry("Pseudonyms", CS_SET_PSEUDONYM)
        combo.addEntry("Non-Friends", CS_SET_EXTRA_AVS)
        resetControls()
    }

    fun refreshSetList() {
        avatarList?.refreshNames()
        generateAvatarList(contactSetCombo?.value ?: CS_SET_ALL_SETS)
        resetControls()
    }

    private fun resetControls() {
        val mutableSet = !LGGContactSets.isInternalSetName(contactSetCombo?.value ?: "")
        val hasSelection = avatarSelections.isNotEmpty() && avatarSelections.size <= MAX_SELECTIONS

        System.err.println("FSPanelContactSets: enable/disable buttons not yet implemented")
    }

    private fun updateAvatarListSorting() {
        val list = avatarList ?: return
        if (shouldSortByOnlineStatus()) {
            list.sort(AvatarItemOnlineStatusComparator())
        } else {
            list.sortByName()
        }
    }

    private fun shouldSortByOnlineStatus(): Boolean {
        val selected = contactSetCombo?.value ?: return false
        if (LGGContactSets.isInternalSetName(selected)) return false
        return LGGContactSets.getSortByOnlineStatusForSet(selected)
    }

    fun handleAvatarDrop(avatarId: UUID, drop: Boolean): Boolean {
        val combo = contactSetCombo ?: return false
        if (avatarId == UUID(0, 0)) return false
        val setName = combo.value
        if (LGGContactSets.isInternalSetName(setName)) return false
        if (drop) LGGContactSets.addToSet(listOf(avatarId), setName)
        return true
    }

    fun onClickAddAvatar() {
        System.err.println("FSPanelContactSets: show avatar picker floater not yet implemented")
    }

    private fun handlePickerCallback(ids: List<UUID>, set: String) {
        if (ids.isEmpty() || contactSetCombo == null) return
        LGGContactSets.addToSet(ids, set)
    }

    fun onClickRemoveAvatar() {
        if (avatarList == null || contactSetCombo == null) return
        val set = contactSetCombo!!.value
        val count = avatarSelections.size
        System.err.println("FSPanelContactSets: show RemoveContact${if (count > 1) "s" else ""}FromSet notification not yet implemented")
    }

    fun onClickMoveAvatar() {
        if (contactSetCombo == null || avatarSelections.isEmpty()) return
        val set = contactSetCombo!!.value
        if (LGGContactSets.isInternalSetName(set)) return
        System.err.println("FSPanelContactSets: call AvatarActions.moveToContactSet not yet implemented")
    }

    fun onClickAddSet() {
        System.err.println("FSPanelContactSets: show AddNewContactSet notification not yet implemented")
    }

    fun onClickRemoveSet() {
        val set = contactSetCombo?.value ?: return
        System.err.println("FSPanelContactSets: show RemoveContactSet notification not yet implemented")
    }

    fun onClickConfigureSet() {
        val set = contactSetCombo?.value ?: return
        System.err.println("FSPanelContactSets: open FSFloaterContactSetConfiguration not yet implemented")
    }

    fun onClickOpenProfile() {
        avatarSelections.forEach { id ->
            System.err.println("FSPanelContactSets: AvatarActions.showProfile not yet implemented")
        }
    }

    fun onClickStartIM() {
        when (avatarSelections.size) {
            1    -> System.err.println("FSPanelContactSets: AvatarActions.startIM not yet implemented")
            else -> System.err.println("FSPanelContactSets: AvatarActions.startConference not yet implemented")
        }
    }

    fun onClickOfferTeleport() {
        System.err.println("FSPanelContactSets: AvatarActions.offerTeleport not yet implemented")
    }

    fun onClickSetPseudonym() {
        if (avatarSelections.isEmpty()) return
        System.err.println("FSPanelContactSets: show SetAvatarPseudonym${if (avatarSelections.size > 1) "Multiple" else ""} notification not yet implemented")
    }

    fun onClickRemovePseudonym() {
        avatarSelections.forEach { id ->
            if (LGGContactSets.hasPseudonym(listOf(id))) {
                LGGContactSets.clearPseudonym(id)
            }
        }
    }

    fun onClickRemoveDisplayName() {
        avatarSelections.forEach { id ->
            if (!LGGContactSets.hasDisplayNameRemoved(listOf(id))) {
                LGGContactSets.removeDisplayName(id)
            }
        }
    }

    private fun onFilterEdit(searchString: String) {
        val upper = searchString.trimStart().uppercase()
        if (filterSubString == upper) return
        filterSubString = upper
        avatarList?.setNameFilter(searchString.trimStart())
    }
}

interface ComboBox {
    var value: String
    var onCommit: ((String) -> Unit)?
    fun clearRows()
    fun addEntry(label: String, value: String = label)
    fun addSeparator()
}

interface AvatarListWidget {
    val selectedIds: List<UUID>
    var onCommit: (() -> Unit)?
    var onDoubleClick: (() -> Unit)?
    var onAvatarDrop: ((UUID, Boolean) -> Boolean)?
    fun clear()
    fun setIds(ids: List<UUID>)
    fun refreshNames()
    fun sort(comparator: Comparator<AvatarListItem>)
    fun sortByName()
    fun setNameFilter(filter: String)
}

interface FilterEditor {
    var onCommit: ((String) -> Unit)?
}
