package com.firestorm.newview

import com.firestorm.llmath.Color4
import java.util.UUID

object HUDManager {

    var parentColor: Color4 = Color4(0f, 0f, 0f, 1f)
    var childColor: Color4  = Color4(0f, 0f, 0f, 1f)

    private val hudEffects: MutableList<HUDEffect> = mutableListOf()

    init {
        System.err.println("HUDManager: init not yet implemented")
    }

    fun createViewerEffect(type: UByte, sendToSim: Boolean = true, originatedHere: Boolean = true): HUDEffect? {
        val effect = HUDObject.addHUDEffect(type) ?: return null
        effect.setID(UUID.randomUUID())
        effect.setNeedsSendToSim(sendToSim)
        effect.setOriginatedHere(originatedHere)
        hudEffects.add(effect)
        return effect
    }

    fun updateEffects() {
        for (effect in hudEffects) {
            if (effect.isDead()) continue
            effect.update()
        }
    }

    fun sendEffects() {
        for (effect in hudEffects) {
            if (effect.isDead()) {
                System.err.println("WARN: Trying to send dead HUD effect!")
                continue
            }
            if (effect.type.toInt() < HUDObject.LL_HUD_EFFECT_BEAM) {
                System.err.println("WARN: Effect type ${effect.type} is not a real effect and should not be in the list!")
                continue
            }
            if (effect.getNeedsSendToSim() && effect.getOriginatedHere()) {
                System.err.println("HUDManager: sendEffects not yet implemented")
            }
        }
    }

    fun cleanupEffects() {
        hudEffects.removeAll { it.isDead() }
    }

    fun shutdown() {
        hudEffects.clear()
    }

    fun processViewerEffect(mesgsys: Any) {
        System.err.println("HUDManager: processViewerEffect not yet implemented")
        val numberOfBlocks: Int = 0
        for (k in 0 until numberOfBlocks) {
            val (effectId, effectType) = HUDEffect.getIDType(mesgsys, k)

            var effectp: HUDEffect? = null
            val iter = hudEffects.iterator()
            while (iter.hasNext()) {
                val cur = iter.next()
                if (cur.isDead()) { iter.remove(); continue }
                if (cur.getID() == effectId) {
                    if (cur.getType() != effectType) {
                        System.err.println("WARN: Viewer effect update type mismatch!")
                    }
                    effectp = cur
                    break
                }
            }

            if (effectType != 0.toUByte()) {
                if (effectp == null) {
                    effectp = createViewerEffect(effectType, sendToSim = false, originatedHere = false)
                }
                effectp?.unpackData(mesgsys, k)
            } else {
                System.err.println("WARN: Received viewer effect of type $effectType which is not a real effect!")
            }
        }
    }
}
