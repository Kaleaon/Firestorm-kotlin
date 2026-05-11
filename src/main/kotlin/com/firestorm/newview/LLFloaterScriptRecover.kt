package com.firestorm.newview

import java.util.UUID

private const val NEW_LSL_NAME = "New Script"

private fun fixNewScriptDefaultName(scriptName: String): String {
    // The default new-script name contains a space that breaks the upload API; replace with underscore
    return if (scriptName == NEW_LSL_NAME) scriptName.replace(' ', '_') else scriptName
}

// ============================================================================
// LLFloaterScriptRecover
// ============================================================================

class LLFloaterScriptRecover private constructor(sdKey: Any) : LLFloater(sdKey) {

    open fun onOpen(sdKey: Any) {
        val pListCtrl = findChild<LLScrollListCtrl>("script_list")
        pListCtrl?.clearRows()

        val files = (sdKey as? Map<*, *>)?.get("files") as? List<*> ?: return
        for (sdFile in files) {
            val fileMap = sdFile as? Map<*, *> ?: continue
            val row = LLScrollListRow(
                checkValue = true,
                nameValue = fileMap["name"]?.toString() ?: "",
                rowValue = sdFile
            )
            pListCtrl?.addElement(row, AddPosition.BOTTOM)
        }
    }

    open fun postBuild(): Boolean {
        findChild<LLUICtrl>("recover_btn")?.setCommitCallback { onBtnRecover() }
        findChild<LLUICtrl>("cancel_btn")?.setCommitCallback { onBtnCancel() }
        return true
    }

    protected fun onBtnCancel() {
        val pListCtrl = findChild<LLScrollListCtrl>("script_list")
        val items = pListCtrl?.getAllData() ?: emptyList()
        for (item in items) {
            LLFile.remove(item.getValue())
        }
        closeFloater()
    }

    protected fun onBtnRecover() {
        val pListCtrl = findChild<LLScrollListCtrl>("script_list")
        val items = pListCtrl?.getAllData() ?: emptyList()
        val sdFiles = mutableListOf<Any>()

        for (item in items) {
            val checkColumn = item.getColumn(0) as? LLScrollListCheck ?: continue
            val sdFile = item.getValueObject()
            if (checkColumn.getCheckBox().getValue()) {
                sdFiles.add(sdFile)
            } else {
                val path = (sdFile as? Map<*, *>)?.get("path")?.toString() ?: continue
                LLFile.remove(path)
            }
        }

        if (sdFiles.isNotEmpty()) {
            LLScriptRecoverQueue(sdFiles)
        }
        closeFloater()
    }

    companion object {
        fun create(sdKey: Any): LLFloaterScriptRecover = LLFloaterScriptRecover(sdKey)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findChild(name: String): T? = TODO("UI: findChild '$name'")
    private fun closeFloater() { TODO("UI: close this floater") }
}

// ============================================================================
// LLScriptRecoverQueue
// ============================================================================

class LLScriptRecoverQueue(sdFiles: List<Any>) {

    private val fileQueue: MutableMap<String, Any> = mutableMapOf()

    init {
        for (sdFile in sdFiles) {
            val fileMap = sdFile as? Map<*, *> ?: continue
            val path = fileMap["path"]?.toString() ?: continue
            if (LLFile.isFile(path)) {
                fileQueue[path] = sdFile
            }
        }
        recoverNext()
    }

    companion object {
        fun recoverIfNeeded() {
            val strTempPath = LLFile.tmpdir()
            val sdFiles = mutableListOf<Map<String, String>>()

            val backupFiles = LLFile.listFiles(strTempPath, "*.lslbackup")
            for (strFilename in backupFiles) {
                var strName = LLFile.getBaseFileName(strFilename, stripExtension = true)

                val agentIdStr = gAgentID.toString().replace("-", "")
                val agentDelimOffset = strName.indexOf('_')
                if (agentDelimOffset > 0) {
                    val fileAgentIdStr = strName.substring(0, agentDelimOffset)
                    if (fileAgentIdStr != agentIdStr) {
                        continue
                    }
                    strName = strName.substring(agentDelimOffset + 1)
                }

                val offset = strName.lastIndexOf('-')
                if (offset > 0 && offset == strName.length - 9) {
                    strName = strName.substring(0, strName.length - 9)
                }

                strName = strName.trim()
                if (strName.isEmpty()) {
                    strName = LLTrans.getString("unknown_script")
                }

                sdFiles.add(mapOf("path" to strTempPath + strFilename, "name" to strName))
            }

            if (sdFiles.isNotEmpty()) {
                LLFloaterReg.showInstance("script_recover", mapOf("files" to sdFiles))
            }
        }
    }

    fun recoverNext(): Boolean {
        val idFNF = gInventoryModel.findCategoryUUIDForType(LLFolderType.LOST_AND_FOUND)

        val itFile = fileQueue.entries.firstOrNull { (_, value) ->
            val fileData = value as? Map<*, *>
            fileData?.get("item") == null
        }

        if (itFile == null) {
            val pInvPanel = LLInventoryPanel.getActiveInventoryPanel(createIfNeeded = true)
            val pFVF = pInvPanel?.getItemByID(idFNF) as? LLFolderViewFolder
            if (pFVF != null) {
                pFVF.setOpenArrangeRecursively(open = true, mode = RecurseMode.UP)
                pInvPanel?.setSelection(idFNF, focus = true)
            }
            return false
        }

        val fileData = itFile.value as? Map<*, *>
        val strItemDescr = LLViewerAssetType.generateDescriptionFor(LLAssetType.LSL_TEXT)
        val strScriptName = fixNewScriptDefaultName(fileData?.get("name")?.toString() ?: "")

        createInventoryItem(
            agentId = gAgent.getID(),
            sessionId = gAgent.getSessionID(),
            parentId = idFNF,
            name = strScriptName,
            description = strItemDescr,
            assetType = LLAssetType.LSL_TEXT,
            callback = { idItem -> onCreateScript(idItem) }
        )
        return true
    }

    fun onCreateScript(idItem: UUID) {
        val pItem = gInventoryModel.getItem(idItem) ?: return

        var strFilePath = ""
        for ((path, value) in fileQueue) {
            val fileData = value as? Map<*, *> ?: continue
            if (fixNewScriptDefaultName(fileData["name"]?.toString() ?: "") != pItem.getName()) continue
            fileQueue[path] = buildFileDataWithItem(fileData, idItem)
            strFilePath = path
            break
        }

        val strCapsUrl = gAgent.getRegionCapability("UpdateScriptAgent")
        if (strCapsUrl.isNotEmpty()) {
            val buffer = readFileBytes(strFilePath)
            LLViewerAssetUpload.enqueueInventoryUpload(strCapsUrl, idItem, buffer) { itemId, newAssetId, newItemId, response ->
                onSavedScript(itemId, newAssetId, newItemId, response)
            }
        }
    }

    fun onSavedScript(itemId: UUID, newAssetId: UUID, newItemId: UUID, response: Any) {
        val httpOk = LLCoreHttpUtil.isHttpOk(response)

        val itFile = fileQueue.entries.firstOrNull { (_, value) ->
            val fileData = value as? Map<*, *>
            fileData?.get("item")?.let { it as? UUID } == itemId
        } ?: return

        if (httpOk) {
            val pItem = gInventoryModel.getItem(itemId)
            if (pItem != null) {
                val fileData = itFile.value as? Map<*, *>
                val strScriptName = fileData?.get("name")?.toString() ?: ""
                if (strScriptName == NEW_LSL_NAME) {
                    // Rename back: the item was created with the underscore-sanitised name
                    val pNewItem = LLViewerInventoryItemWrapper(pItem)
                    pNewItem.rename(strScriptName)
                    pNewItem.updateServer(false)
                    gInventoryModel.updateItem(pNewItem)
                    gInventoryModel.notifyObservers()
                }
                LLFile.remove(itFile.key)
                fileQueue.remove(itFile.key)
            }
        } else {
            val pItem = gInventoryModel.getItem(itemId)
            if (pItem != null) {
                gInventoryModel.changeItemParent(pItem, gInventoryModel.findCategoryUUIDForType(LLFolderType.TRASH), restamp = false)
            }
            fileQueue.remove(itFile.key)
        }
        recoverNext()
    }

    private fun buildFileDataWithItem(original: Map<*, *>, idItem: UUID): MutableMap<String, Any> {
        val result = mutableMapOf<String, Any>()
        for ((k, v) in original) { if (k != null && v != null) result[k.toString()] = v }
        result["item"] = idItem
        return result
    }

    private fun readFileBytes(path: String): ByteArray {
        TODO("APR: use JVM equivalent - java.io.File(path).readBytes()")
    }
}

// ============================================================================
// Stubs not defined elsewhere in the package
// ============================================================================

object LLFile {
    fun remove(path: String) { TODO("APR: use JVM equivalent - java.io.File(path).delete()") }
    fun isFile(path: String): Boolean { TODO("APR: use JVM equivalent - java.io.File(path).isFile") }
    fun tmpdir(): String { TODO("APR: use JVM equivalent - System.getProperty(\"java.io.tmpdir\")") }
    fun listFiles(dir: String, glob: String): List<String> { TODO("APR: use JVM equivalent - list files matching glob in dir") }
    fun getBaseFileName(path: String, stripExtension: Boolean): String { TODO("APR: derive base filename from path") }
}

enum class LLFolderType { LOST_AND_FOUND, TRASH }
enum class AddPosition { BOTTOM }
enum class RecurseMode { UP }

object LLViewerAssetType {
    fun generateDescriptionFor(assetType: LLAssetType): String { TODO("Asset: generate description for $assetType") }
}

val gAgentID: UUID get() = TODO("Agent: global agent UUID")

object gInventoryModel {
    fun getItem(id: UUID): Any? = TODO("Inventory: getItem")
    fun findCategoryUUIDForType(type: LLFolderType): UUID = TODO("Inventory: findCategoryUUIDForType")
    fun changeItemParent(item: Any, parentId: UUID, restamp: Boolean) { TODO("Inventory: changeItemParent") }
    fun updateItem(item: Any) { TODO("Inventory: updateItem") }
    fun notifyObservers() { TODO("Inventory: notifyObservers") }
}

class LLViewerInventoryItemWrapper(source: Any) {
    fun getName(): String = TODO("Inventory: get item name")
    fun rename(name: String) { TODO("Inventory: rename item") }
    fun updateServer(isNew: Boolean) { TODO("Inventory: updateServer") }
}

object LLInventoryPanel {
    fun getActiveInventoryPanel(createIfNeeded: Boolean): LLInventoryPanel? = TODO("Inventory: get active inventory panel")
    fun getItemByID(id: UUID): Any? = TODO("Inventory: getItemByID")
    fun setSelection(id: UUID, focus: Boolean) { TODO("Inventory: setSelection") }
}

class LLFolderViewFolder {
    fun setOpenArrangeRecursively(open: Boolean, mode: RecurseMode) { TODO("UI: setOpenArrangeRecursively") }
}

object LLViewerAssetUpload {
    fun enqueueInventoryUpload(capsUrl: String, itemId: UUID, buffer: ByteArray, cb: (UUID, UUID, UUID, Any) -> Unit) {
        TODO("APR: use JVM equivalent - HTTP multipart upload to caps URL")
    }
}

object LLCoreHttpUtil {
    fun isHttpOk(response: Any): Boolean { TODO("HTTP: extract HTTP status from LLSD response") }
}

class LLUICtrl {
    fun setCommitCallback(cb: () -> Unit) { TODO("UI: setCommitCallback") }
}

data class LLScrollListRow(val checkValue: Boolean, val nameValue: String, val rowValue: Any)

class LLScrollListItem {
    fun getValue(): String = TODO("UI: get scroll list item string value")
    fun getValueObject(): Any = TODO("UI: get scroll list item value object")
    fun getColumn(index: Int): Any? = TODO("UI: get scroll list column at index")
}

class LLScrollListCheck {
    fun getCheckBox(): LLCheckBoxWidget = TODO("UI: get check box from scroll list check column")
}

class LLCheckBoxWidget {
    fun getValue(): Boolean = TODO("UI: get check box value")
}

fun Any.getID(): UUID = TODO("Agent: getID")
fun Any.getSessionID(): UUID = TODO("Agent: getSessionID")
fun Any.getRegionCapability(cap: String): String = TODO("Agent: getRegionCapability '$cap'")
