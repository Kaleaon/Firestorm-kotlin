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
        TODO("APR: use JVM equivalent for LLAvatarTracker::isBuddyOnline")
    }

    fun addObserver(observer: Any) {
        TODO("APR: register friend-status observer")
    }

    fun removeObserver(observer: Any) {
        TODO("APR: unregister friend-status observer")
    }

    fun allBuddies(): Map<UUID, Any> {
        TODO("APR: return copy of the buddy map")
    }
}

object LGGContactSets {
    enum class ContactSetUpdate { UPDATED_LISTS, UPDATED_MEMBERS }

    fun getAllContactSets(): List<String> {
        TODO("APR: return all user-defined contact set names")
    }

    fun isInternalSetName(name: String): Boolean =
        name in setOf(CS_SET_ALL_SETS, CS_SET_NO_SETS, CS_SET_PSEUDONYM, CS_SET_EXTRA_AVS)

    fun getFriendsInAnySet(): MutableList<UUID> {
        TODO("APR: return UUIDs of all avatars that belong to at least one set")
    }

    fun isFriendInAnySet(id: UUID): Boolean {
        TODO("APR: return true if avatar is in at least one set")
    }

    fun getListOfPseudonymAvs(): MutableList<UUID> {
        TODO("APR: return UUIDs of avatars that have a pseudonym assigned")
    }

    fun getListOfNonFriends(): MutableList<UUID> {
        TODO("APR: return UUIDs of non-friend extra avatars")
    }

    fun getContactSetMembers(name: String): List<UUID> {
        TODO("APR: return member UUIDs for the named set")
    }

    fun getSortByOnlineStatusForSet(name: String): Boolean {
        TODO("APR: read per-set sort preference")
    }

    fun hasPseudonym(ids: List<UUID>): Boolean {
        TODO("APR: return true if any id in ids has a pseudonym")
    }

    fun hasDisplayNameRemoved(ids: List<UUID>): Boolean {
        TODO("APR: return true if any id in ids has display-name removal active")
    }

    fun clearPseudonym(id: UUID) {
        TODO("APR: remove stored pseudonym for this avatar")
    }

    fun removeDisplayName(id: UUID) {
        TODO("APR: mark avatar's display name as hidden")
    }

    fun addToSet(ids: List<UUID>, setName: String) {
        TODO("APR: add each id to the named contact set")
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
        TODO("UI: inflate layout XML, then call the wiring below once widgets are available")
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
        TODO("UI: update member-count label with ids.size")
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

        TODO("UI: enable/disable buttons: remove_set_btn=$mutableSet, config_btn=$mutableSet, " +
             "add_btn=$mutableSet, move_btn=${mutableSet && hasSelection}, " +
             "remove_btn=${mutableSet && hasSelection}, profile_btn=$hasSelection, " +
             "start_im_btn=$hasSelection, offer_teleport_btn=$hasSelection, " +
             "set_pseudonym_btn=$hasSelection, " +
             "remove_pseudonym_btn=${hasSelection && LGGContactSets.hasPseudonym(avatarSelections)}, " +
             "remove_displayname_btn=${hasSelection && !LGGContactSets.hasDisplayNameRemoved(avatarSelections)}")
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
        TODO("UI: show avatar picker floater; on confirm call handlePickerCallback(ids, currentSet)")
    }

    private fun handlePickerCallback(ids: List<UUID>, set: String) {
        if (ids.isEmpty() || contactSetCombo == null) return
        LGGContactSets.addToSet(ids, set)
    }

    fun onClickRemoveAvatar() {
        if (avatarList == null || contactSetCombo == null) return
        val set = contactSetCombo!!.value
        val count = avatarSelections.size
        TODO("UI: show RemoveContact${if (count > 1) "s" else ""}FromSet notification with set=$set and ids=$avatarSelections")
    }

    fun onClickMoveAvatar() {
        if (contactSetCombo == null || avatarSelections.isEmpty()) return
        val set = contactSetCombo!!.value
        if (LGGContactSets.isInternalSetName(set)) return
        TODO("UI: call AvatarActions.moveToContactSet(avatarSelections, set)")
    }

    fun onClickAddSet() {
        TODO("UI: show AddNewContactSet notification; callback → LGGContactSets.handleAddContactSetCallback")
    }

    fun onClickRemoveSet() {
        val set = contactSetCombo?.value ?: return
        TODO("UI: show RemoveContactSet notification with set=$set")
    }

    fun onClickConfigureSet() {
        val set = contactSetCombo?.value ?: return
        TODO("UI: open FSFloaterContactSetConfiguration for set=$set")
    }

    fun onClickOpenProfile() {
        avatarSelections.forEach { id ->
            TODO("UI: AvatarActions.showProfile($id)")
        }
    }

    fun onClickStartIM() {
        when (avatarSelections.size) {
            1    -> TODO("UI: AvatarActions.startIM(${avatarSelections[0]})")
            else -> TODO("UI: AvatarActions.startConference(avatarSelections)")
        }
    }

    fun onClickOfferTeleport() {
        TODO("UI: AvatarActions.offerTeleport(avatarSelections)")
    }

    fun onClickSetPseudonym() {
        if (avatarSelections.isEmpty()) return
        TODO("UI: show SetAvatarPseudonym${if (avatarSelections.size > 1) "Multiple" else ""} notification")
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
