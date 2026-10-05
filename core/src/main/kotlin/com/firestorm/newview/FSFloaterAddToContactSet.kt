package com.firestorm.newview

import java.util.UUID

class FSFloaterAddToContactSet(target: LLSD) : LLFloater(target) {

    private var hasMultipleAgents: Boolean = false
    private var isMoveOperation: Boolean = false
    private var agentId: UUID = UUID(0, 0)
    private val agentIds: MutableList<UUID> = mutableListOf()
    private var sourceSet: String = ""

    private var contactSetsCombo: LLComboBox? = null

    private var contactSetChangedConnection: ((LGGContactSets.EContactSetUpdate) -> Unit)? = null

    init {
        when {
            target.isMap() -> {
                isMoveOperation = (target["operation"].asString() == "move")
                sourceSet = target["source_set"].asString()
                val ids = target["ids"]
                when {
                    ids.isArray() -> {
                        hasMultipleAgents = true
                        ids.asArray().forEach { agentIds.add(it.asUUID()) }
                    }
                    target.has("id") -> {
                        hasMultipleAgents = false
                        agentId = target["id"].asUUID()
                    }
                }
            }
            target.isArray() -> {
                hasMultipleAgents = true
                target.asArray().forEach { agentIds.add(it.asUUID()) }
            }
            else -> {
                hasMultipleAgents = false
                agentId = target.asUUID()
            }
        }

        contactSetChangedConnection = { type -> updateSets(type) }
        LGGContactSets.getInstance().setContactSetChangeCallback(contactSetChangedConnection!!)
    }

    override fun postBuild(): Boolean {
        if (hasMultipleAgents) {
            childSetValue("textfield", getString("text_add_multiple").replace("{COUNT}", agentIds.size.toString()))
        } else {
            childSetValue("textfield", getString("text_add_single").replace("{NAME}", LLSLURL("agent", agentId, "inspect").getSLURLString()))
        }

        contactSetsCombo = getChild<LLComboBox>("contact_sets")
        populateContactSets()

        childSetAction("add_btn") { onClickAdd() }
        childSetAction("cancel_btn") { onClickCancel() }
        childSetAction("add_set_btn") { onClickAddSet() }

        return true
    }

    private fun onClickAdd() {
        val set = contactSetsCombo?.getSimple() ?: return
        if (set.isEmpty()) return

        if (!hasMultipleAgents) {
            if (agentId == UUID(0, 0)) return
            agentIds.add(agentId)
        } else if (agentIds.isEmpty()) {
            return
        }

        LGGContactSets.instance().addToSet(agentIds, set)
        if (isMoveOperation && set != sourceSet) {
            for (id in agentIds) {
                LGGContactSets.instance().removeFriendFromSet(id, sourceSet)
            }
        }

        when {
            !isMoveOperation && hasMultipleAgents -> {
                LLNotificationsUtil.add("AddToContactSetMultipleSuccess", mapOf("COUNT" to agentIds.size.toString(), "SET" to set))
            }
            !isMoveOperation -> {
                LLNotificationsUtil.add("AddToContactSetSingleSuccess", mapOf("NAME" to LLSLURL("agent", agentId, "inspect").getSLURLString(), "SET" to set))
            }
        }
        closeFloater()
    }

    private fun onClickCancel() {
        closeFloater()
    }

    private fun onClickAddSet() {
        LLNotificationsUtil.add("AddNewContactSet", emptyMap(), emptyMap(), LGGContactSets::handleAddContactSetCallback)
    }

    private fun updateSets(type: LGGContactSets.EContactSetUpdate) {
        if (type != LGGContactSets.EContactSetUpdate.NONE) {
            populateContactSets()
        }
    }

    private fun populateContactSets() {
        val combo = contactSetsCombo ?: return
        combo.clearRows()
        val contactSets = LGGContactSets.getInstance().getAllContactSets()
        var isEmptySet = true
        if (contactSets.isEmpty()) {
            combo.add(getString("no_sets"), "No Set")
        } else {
            for (setName in contactSets) {
                if (isMoveOperation && setName == sourceSet) continue
                combo.add(setName)
                isEmptySet = false
            }
        }
        getChild<LLButton>("add_btn").setEnabled(!isEmptySet)
    }
}
