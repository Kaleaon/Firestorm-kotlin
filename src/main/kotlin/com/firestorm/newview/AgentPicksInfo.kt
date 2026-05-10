package com.firestorm.newview

import com.firestorm.agent.Agent
import com.firestorm.agent.AgentBenefitsMgr
import com.firestorm.avatar.AvatarData
import com.firestorm.avatar.AvatarPicks
import com.firestorm.avatar.AvatarPropertiesObserver
import com.firestorm.avatar.AvatarPropertiesProcessor
import com.firestorm.avatar.EAvatarProcessorType

object AgentPicksInfo {

    // Block pick creation until the server confirms the current count,
    // so we never exceed the cap on first login before the response arrives.
    private var numberOfPicks: Int = Int.MAX_VALUE
    private var agentPicksObserver: AgentPicksObserver? = null

    fun requestNumberOfPicks() {
        if (agentPicksObserver == null) {
            agentPicksObserver = AgentPicksObserver { picks ->
                onServerRespond(picks)
            }
        }
        agentPicksObserver?.sendAgentPicksRequest()
    }

    fun getNumberOfPicks(): Int = numberOfPicks

    fun getMaxNumberOfPicks(): Int = AgentBenefitsMgr.current().getPicksLimit()

    fun isPickLimitReached(): Boolean = numberOfPicks >= AgentBenefitsMgr.current().getPicksLimit()

    fun incrementNumberOfPicks() { numberOfPicks++ }
    fun decrementNumberOfPicks() { numberOfPicks-- }

    fun onServerRespond(picks: AvatarData?) {
        requireNotNull(picks) { "onServerRespond received null AvatarData" }
        setNumberOfPicks(picks.picksList.size)
    }

    private fun setNumberOfPicks(number: Int) { numberOfPicks = number }

    private class AgentPicksObserver(
        private val respondCallback: (AvatarData) -> Unit,
    ) : AvatarPropertiesObserver {

        init {
            AvatarPropertiesProcessor.instance.addObserver(Agent.instance.getID(), this)
        }

        fun destroy() {
            AvatarPropertiesProcessor.instance.removeObserver(Agent.instance.getID(), this)
        }

        fun sendAgentPicksRequest() {
            val agentId = Agent.instance.getID()
            if (Agent.instance.getRegionCapability("AgentProfile").isNotEmpty()) {
                AvatarPropertiesProcessor.instance.sendAvatarPropertiesRequest(agentId)
            } else {
                AvatarPropertiesProcessor.instance.sendAvatarPicksRequest(agentId)
            }
        }

        override fun processProperties(data: Any?, type: EAvatarProcessorType) {
            when (type) {
                EAvatarProcessorType.APT_PROPERTIES -> {
                    val picks = data as? AvatarData ?: return
                    if (Agent.instance.getID() == picks.avatarId) {
                        respondCallback(picks)
                    }
                }
                EAvatarProcessorType.APT_PICKS -> {
                    val picks = data as? AvatarPicks ?: return
                    if (Agent.instance.getID() == picks.targetId) {
                        val avatarData = AvatarData(picksList = picks.picksList)
                        respondCallback(avatarData)
                    }
                }
                else -> Unit
            }
        }
    }
}
