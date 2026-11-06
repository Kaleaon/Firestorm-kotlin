package com.firestorm.newview

import java.util.UUID

class LLFloaterAvatarTextures(id: LLSD) : LLFloater(id) {

    private val mID: UUID = id.asUUID()
    private var mTitle: String = ""
    private val mTextures: Array<LLTextureCtrl?> = arrayOfNulls(TEX_NUM_INDICES)

    override fun postBuild(): Boolean {
        for (i in 0 until TEX_NUM_INDICES) {
            val texName = LLAvatarAppearance.getDictionary().getTexture(i).mName
            mTextures[i] = getChild<LLTextureCtrl>(texName)
            mTextures[i]?.setIsMasked(true)
            mTextures[i]?.setEnabled(false)
        }
        mTitle = getTitle()

        childSetAction("Dump", ::onClickDump)

        childSetVisible("Dump", gAgent.isGodlike())

        refresh()
        return true
    }

    override fun draw() {
        refresh()
        super.draw()
    }

    override fun refresh() {
        val avatarp = findAvatar(mID)
        if (avatarp != null) {
            val avName = LLAvatarName()
            if (LLAvatarNameCache.get(avatarp.getID(), avName)) {
                val displayName = if (gRlvHandler.hasBehaviour(RLV_BHVR_SHOWNAMES)) {
                    RlvStrings.getAnonym(avName)
                } else {
                    avName.getCompleteName()
                }
                setTitle("$mTitle: $displayName")
            }
            for (i in 0 until TEX_NUM_INDICES) {
                mTextures[i]?.let { updateTextureCtrl(avatarp, it, i) }
            }
        } else {
            setTitle("$mTitle: ${getString("InvalidAvatar")} (${mID})")
        }
    }

    companion object {
        private fun updateTextureCtrl(avatarp: LLVOAvatar, ctrl: LLTextureCtrl, te: Int) {
            var id: UUID = IMG_DEFAULT_AVATAR
            val texEntry = LLAvatarAppearance.getDictionary().getTexture(te)
            if (texEntry != null && texEntry.mIsLocalTexture) {
                if (avatarp.isSelf()) {
                    val wearableType = texEntry.mWearableType
                    val wearable = gAgentWearables.getViewerWearable(wearableType, 0)
                    if (wearable != null) {
                        val lto = wearable.getLocalTextureObject(te)
                        if (lto != null) {
                            id = lto.getID()
                        }
                    }
                }
            } else {
                id = if (texEntry != null) avatarp.getTEref(te).getID() else IMG_DEFAULT_AVATAR
            }

            if (id == IMG_DEFAULT_AVATAR) {
                ctrl.setImageAssetID(UUID_NULL)
                ctrl.setToolTip("${texEntry?.mName} : IMG_DEFAULT_AVATAR")
            } else {
                ctrl.setImageAssetID(id)
                ctrl.setToolTip("${texEntry?.mName} : ${id.toString().substring(0, 7)}")
            }
        }

        private fun findAvatar(id: UUID): LLVOAvatar? {
            var obj: LLViewerObject? = gObjectList.findObject(id)
            while (obj != null && obj.isAttachment()) {
                obj = obj.getParent() as? LLViewerObject
            }
            return if (obj != null && obj.isAvatar()) obj as LLVOAvatar else null
        }

        fun onClickDump(data: Any?) {
            if (!gAgent.isGodlike()) return
            val avatarp = gAgentAvatarp ?: return
            for (i in 0 until avatarp.getNumTEs()) {
                val te = avatarp.getTE(i) ?: continue
                val texEntry = LLAvatarAppearance.getDictionary().getTexture(i) ?: continue

                if (LLVOAvatar.isIndexLocalTexture(i)) {
                    var id: UUID = IMG_DEFAULT_AVATAR
                    val wearableType = LLAvatarAppearance.getDictionary().getTEWearableType(i)
                    if (avatarp.isSelf()) {
                        val wearable = gAgentWearables.getViewerWearable(wearableType, 0)
                        if (wearable != null) {
                            val lto = wearable.getLocalTextureObject(i)
                            if (lto != null) {
                                id = lto.getID()
                            }
                        }
                    }
                    if (id != IMG_DEFAULT_AVATAR) {
                        LLLog.info("TE $i name:${texEntry.mName} id:$id")
                    } else {
                        LLLog.info("TE $i name:${texEntry.mName} id:<DEFAULT>")
                    }
                } else {
                    LLLog.info("TE $i name:${texEntry.mName} id:${te.getID()}")
                }
            }
        }
    }
}
