package com.firestorm.newview

import java.util.UUID

private const val NEW_LSL_NAME = "New Script"

private fun fixNewScriptDefaultName(scriptName: String): String {
    return if (scriptName == NEW_LSL_NAME) scriptName.replace(' ', '_') else scriptName
}

// ============================================================================
// LLFloaterScriptRecover
//

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

    override fun postBuild(): Boolean {
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
            val sdFile = item.getValue()
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

    private fun <T> findChild(name: String): T? = TODO("UI: findChild '$name'")
    private fun closeFloater() { TODO("UI: close this floater") }
}

// ============================================================================
// LLScriptRecoverQueue
//

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
                    } else {
                        strName = strName.substring(agentDelimOffset + 1)
                    }
                }

                val offset = strName.lastIndexOf('-')
                if (offset != -1 && offset != 0 && offset == strName.length - 9) {
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
        val idFNF = gInventory.findCategoryUUIDForType(LLFolderType.LOST_AND_FOUND)

        val itFile = fileQueue.entries.firstOrNull { entry ->
            val fileData = entry.value as? Map<*, *>
            !(fileData?.containsKey("item") == true && fileData["item"] != null)
        }

        if (itFile == null) {
            val pInvPanel = LLInventoryPanel.getActiveInventoryPanel(true)
            val pFVF = pInvPanel?.getItemByID(idFNF) as? LLFolderViewFolder
            if (pFVF != null) {
                pFVF.setOpenArrangeRecursively(true, RecurseMode.UP)
                pInvPanel?.setSelection(idFNF, true)
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
        val pItem = gInventory.getItem(idItem) ?: return

        var strFileName = ""
        var strFilePath = ""
        for ((path, value) in fileQueue) {
            val fileData = value as? Map<*, *> ?: continue
            if (fixNewScriptDefaultName(fileData["name"]?.toString() ?: "") != pItem.getName()) continue
            strFileName = fileData["path"]?.toString() ?: ""
            (fileQueue as MutableMap<String, Any>)[path] = buildFileDataWithItem(fileData, idItem)
            strFilePath = path
            break
        }

        val strCapsUrl = gAgent.getRegionCapability("UpdateScriptAgent")
        if (strCapsUrl.isNotEmpty()) {
            val buffer = TODO("APR: use JVM equivalent - read file bytes from '$strFilePath' using java.io.File")
            @Suppress("UNREACHABLE_CODE")
            LLViewerAssetUpload.enqueueInventoryUpload(strCapsUrl, idItem, buffer as ByteArray) { itemId, newAssetId, newItemId, response ->
                onSavedScript(itemId, newAssetId, newItemId, response)
            }
        }
    }

    fun onSavedScript(itemId: UUID, newAssetId: UUID, newItemId: UUID, response: Any) {
        val httpOk = LLCoreHttpUtil.isHttpOk(response)

        val itFile = fileQueue.entries.firstOrNull { entry ->
            val fileData = entry.value as? Map<*, *>
            fileData?.get("item")?.let { it as? UUID } == itemId
        }

        if (itFile == null) {
            return
        }

        if (httpOk) {
            val pItem = gInventory.getItem(itemId)
            if (pItem != null) {
                val fileData = itFile.value as? Map<*, *>
                val strScriptName = fileData?.get("name")?.toString() ?: ""
                if (strScriptName == NEW_LSL_NAME) {
                    // Rename back scripts that were created with a sanitised default name
                    val pNewItem = LLViewerInventoryItem(pItem)
                    pNewItem.rename(strScriptName)
                    pNewItem.updateServer(false)
                    gInventory.updateItem(pNewItem)
                    gInventory.notifyObservers()
                }
                LLFile.remove(itFile.key)
                fileQueue.remove(itFile.key)
            }
        } else {
            val pItem = gInventory.getItem(itemId)
            if (pItem != null) {
                gInventory.changeItemParent(pItem, gInventory.findCategoryUUIDForType(LLFolderType.TRASH), false)
            }
            fileQueue.remove(itFile.key)
        }
        recoverNext()
    }

    private fun buildFileDataWithItem(original: Map<*, *>, idItem: UUID): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        for ((k, v) in original) { if (k != null && v != null) result[k.toString()] = v }
        result["item"] = idItem
        return result
    }
}

// ============================================================================
// Stub types for referenced platform / inventory APIs
//

object LLFile {
    fun remove(path: String) { TODO("APR: use JVM equivalent - java.io.File(path).delete()") }
    fun isFile(path: String): Boolean { TODO("APR: use JVM equivalent - java.io.File(path).isFile") }
    fun tmpdir(): String { TODO("APR: use JVM equivalent - System.getProperty(\"java.io.tmpdir\")") }
    fun listFiles(dir: String, glob: String): List<String> { TODO("APR: use JVM equivalent - list files matching glob") }
    fun getBaseFileName(path: String, stripExtension: Boolean): String { TODO("APR: derive base filename from path") }
}

object LLViewerAssetType {
    fun generateDescriptionFor(assetType: LLAssetType): String { TODO("Asset: generate description for $assetType") }
}

enum class LLAssetType { LSL_TEXT }

enum class LLFolderType { LOST_AND_FOUND, TRASH }

enum class AddPosition { BOTTOM }

enum class RecurseMode { UP }

val gAgentID: UUID get() = TODO("Agent: global agent UUID")
val gInventory: Any get() = TODO("Inventory: global inventory model")

fun createInventoryItem(
    agentId: UUID,
    sessionId: UUID,
    parentId: UUID,
    name: String,
    description: String,
    assetType: LLAssetType,
    callback: (UUID) -> Unit
) { TODO("Inventory: create_inventory_item") }

object LLViewerAssetUpload {
    fun enqueueInventoryUpload(capsUrl: String, itemId: UUID, buffer: ByteArray, cb: (UUID, UUID, UUID, Any) -> Unit) {
        TODO("APR: use JVM equivalent - HTTP upload to caps URL")
    }
}

object LLCoreHttpUtil {
    fun isHttpOk(response: Any): Boolean { TODO("HTTP: extract status from response") }
}

class LLScrollListCtrl {
    fun clearRows() { TODO("UI: clear scroll list rows") }
    fun addElement(row: LLScrollListRow, pos: AddPosition) { TODO("UI: add row to scroll list") }
    fun getAllData(): List<LLScrollListItem> { TODO("UI: get all scroll list items") }
}

data class LLScrollListRow(val checkValue: Boolean, val nameValue: String, val rowValue: Any)

class LLScrollListItem {
    fun getValue(): String = TODO("UI: get scroll list item value")
    fun getColumn(index: Int): Any? = TODO("UI: get scroll list item column")
}

class LLScrollListCheck {
    fun getCheckBox(): LLCheckBox = TODO("UI: get check box from scroll list check column")
}

class LLCheckBox {
    fun getValue(): Boolean = TODO("UI: get check box value")
}

class LLUICtrl {
    fun setCommitCallback(cb: () -> Unit) { TODO("UI: setCommitCallback") }
}

object LLInventoryPanel {
    fun getActiveInventoryPanel(create: Boolean): LLInventoryPanel? = TODO("Inventory: get active inventory panel")
    fun getItemByID(id: UUID): Any? = TODO("Inventory: get item by UUID")
    fun setSelection(id: UUID, focus: Boolean) { TODO("Inventory: setSelection") }
}

class LLFolderViewFolder {
    fun setOpenArrangeRecursively(open: Boolean, mode: RecurseMode) { TODO("UI: setOpenArrangeRecursively") }
}

class LLViewerInventoryItem(source: Any) {
    fun getName(): String = TODO("Inventory: get item name")
    fun rename(name: String) { TODO("Inventory: rename item") }
    fun updateServer(isNew: Boolean) { TODO("Inventory: updateServer") }
}

fun Any.getID(): UUID = TODO("Agent: getID")
fun Any.getSessionID(): UUID = TODO("Agent: getSessionID")
fun Any.getRegionCapability(cap: String): String = TODO("Agent: getRegionCapability '$cap'")
fun Any.getItem(id: UUID): LLViewerInventoryItem? = TODO("Inventory: getItem")
fun Any.findCategoryUUIDForType(type: LLFolderType): UUID = TODO("Inventory: findCategoryUUIDForType")
fun Any.changeItemParent(item: LLViewerInventoryItem, parentId: UUID, restamp: Boolean) { TODO("Inventory: changeItemParent") }
fun Any.updateItem(item: LLViewerInventoryItem) { TODO("Inventory: updateItem") }
fun Any.notifyObservers() { TODO("Inventory: notifyObservers") }
