package com.firestorm.newview

import java.util.UUID

class LLFloaterMyScripts(seed: LLSD) : LLFloater(seed) {

    private var gotAttachmentMemoryUsed: Boolean = false
    private var attachmentDetailsRequested: Boolean = false
    private var attachmentMemoryMax: Int = 0
    private var attachmentMemoryUsed: Int = 0

    private var gotAttachmentUrlsUsed: Boolean = false
    private var attachmentUrlsMax: Int = 0
    private var attachmentUrlsUsed: Int = 0

    companion object {
        private const val SIZE_OF_ONE_KB = 1024

        fun onClickRefresh(userdata: Any?) {
            val instance = LLFloaterReg.getTypedInstance<LLFloaterMyScripts>("my_scripts")
            if (instance != null) {
                val btn = instance.getChild<LLButton>("refresh_list_btn")
                // Disable during fetch to prevent accidental self-DoS from rapid clicking.
                btn?.setEnabled(false)
                instance.clearList()
                instance.attachmentDetailsRequested = instance.requestAttachmentDetails()
            }
        }
    }

    override fun postBuild(): Boolean {
        childSetAction("refresh_list_btn", ::onClickRefresh, this)

        val msgWaiting = LLTrans.getString("ScriptLimitsRequestWaiting")
        getChild<LLUICtrl>("loading_text").setValue(LLSD(msgWaiting))
        attachmentDetailsRequested = requestAttachmentDetails()
        return true
    }

    override fun onOpen(key: LLSD) {
        if (!attachmentDetailsRequested) {
            attachmentDetailsRequested = requestAttachmentDetails()
        }
        super.onOpen(key)
    }

    fun requestAttachmentDetails(): Boolean {
        if (gAgent.getRegion() == null) return false

        val url = gAgent.getRegion()!!.getCapability("AttachmentResources")
        return if (url.isNotEmpty()) {
            System.err.println("LLFloaterMyScripts: requestAttachmentDetails not yet implemented")
            true
        } else {
            false
        }
    }

    private fun getAttachmentLimitsCoro(url: String) {
        System.err.println("LLFloaterMyScripts: getAttachmentLimitsCoro not yet implemented")
    }

    fun setAttachmentDetails(content: LLSD) {
        val list = getChild<LLScrollListCtrl>("scripts_list") ?: return

        val numberAttachments = content["attachments"].size()

        for (i in 0 until numberAttachments) {
            val humanReadableLocation: String = if (content["attachments"][i].has("location")) {
                val actualLocation = content["attachments"][i]["location"].asString()
                LLTrans.getString(actualLocation)
            } else {
                ""
            }

            val numberObjects = content["attachments"][i]["objects"].size()
            for (j in 0 until numberObjects) {
                val taskId: UUID = content["attachments"][i]["objects"][j]["id"].asUUID()
                val size: Int = if (content["attachments"][i]["objects"][j]["resources"].has("memory")) {
                    content["attachments"][i]["objects"][j]["resources"]["memory"].asInt() / SIZE_OF_ONE_KB
                } else {
                    0
                }
                val urls: Int = if (content["attachments"][i]["objects"][j]["resources"].has("urls")) {
                    content["attachments"][i]["objects"][j]["resources"]["urls"].asInt()
                } else {
                    0
                }
                val name = content["attachments"][i]["objects"][j]["name"].asString()

                val element = LLSD()
                element["id"] = taskId

                element["columns"][0]["column"] = "size"
                element["columns"][0]["value"] = "%d".format(size)
                element["columns"][0]["font"] = "SANSSERIF"
                element["columns"][0]["halign"] = LLFontGL.RIGHT

                element["columns"][1]["column"] = "urls"
                element["columns"][1]["value"] = "%d".format(urls)
                element["columns"][1]["font"] = "SANSSERIF"
                element["columns"][1]["halign"] = LLFontGL.RIGHT

                element["columns"][2]["column"] = "name"
                element["columns"][2]["value"] = name
                element["columns"][2]["font"] = "SANSSERIF"

                element["columns"][3]["column"] = "location"
                element["columns"][3]["value"] = humanReadableLocation
                element["columns"][3]["font"] = "SANSSERIF"

                list.addElement(element)
            }
        }

        setAttachmentSummary(content)

        getChild<LLUICtrl>("loading_text").setValue(LLSD(""))
        getChild<LLButton>("refresh_list_btn")?.setEnabled(true)
    }

    fun clearList() {
        val list = childGetListInterface("scripts_list")
        list?.operateOnAll(LLCtrlListInterface.OP_DELETE)

        val msgWaiting = LLTrans.getString("ScriptLimitsRequestWaiting")
        getChild<LLUICtrl>("loading_text").setValue(LLSD(msgWaiting))
    }

    fun setAttachmentSummary(content: LLSD) {
        val usedType0 = content["summary"]["used"][0]["type"].asString()
        val usedType1 = content["summary"]["used"][1]["type"].asString()

        if (usedType0 == "memory") {
            attachmentMemoryUsed = content["summary"]["used"][0]["amount"].asInt() / SIZE_OF_ONE_KB
            attachmentMemoryMax = content["summary"]["available"][0]["amount"].asInt() / SIZE_OF_ONE_KB
            gotAttachmentMemoryUsed = true
        } else if (usedType1 == "memory") {
            attachmentMemoryUsed = content["summary"]["used"][1]["amount"].asInt() / SIZE_OF_ONE_KB
            attachmentMemoryMax = content["summary"]["available"][1]["amount"].asInt() / SIZE_OF_ONE_KB
            gotAttachmentMemoryUsed = true
        } else {
            return
        }

        if (usedType0 == "urls") {
            attachmentUrlsUsed = content["summary"]["used"][0]["amount"].asInt()
            attachmentUrlsMax = content["summary"]["available"][0]["amount"].asInt()
            gotAttachmentUrlsUsed = true
        } else if (usedType1 == "urls") {
            attachmentUrlsUsed = content["summary"]["used"][1]["amount"].asInt()
            attachmentUrlsMax = content["summary"]["available"][1]["amount"].asInt()
            gotAttachmentUrlsUsed = true
        } else {
            return
        }

        if (attachmentMemoryUsed >= 0 && attachmentMemoryMax >= 0) {
            val args: MutableMap<String, String> = mutableMapOf()
            args["[COUNT]"] = "%d".format(attachmentMemoryUsed)
            var translateMessage = "ScriptLimitsMemoryUsedSimple"

            if (attachmentMemoryMax > 0) {
                val available = attachmentMemoryMax - attachmentMemoryUsed
                args["[MAX]"] = "%d".format(attachmentMemoryMax)
                args["[AVAILABLE]"] = "%d".format(available)
                translateMessage = "ScriptLimitsMemoryUsed"
            }

            getChild<LLUICtrl>("memory_used").setValue(LLTrans.getString(translateMessage, args))
        }

        if (attachmentUrlsUsed >= 0 && attachmentUrlsMax >= 0) {
            val available = attachmentUrlsMax - attachmentUrlsUsed
            val args: MutableMap<String, String> = mutableMapOf(
                "[COUNT]" to "%d".format(attachmentUrlsUsed),
                "[MAX]" to "%d".format(attachmentUrlsMax),
                "[AVAILABLE]" to "%d".format(available)
            )
            val msg = LLTrans.getString("ScriptLimitsURLsUsed", args)
            getChild<LLUICtrl>("urls_used").setValue(LLSD(msg))
        }
    }
}
