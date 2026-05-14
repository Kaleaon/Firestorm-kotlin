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
    private fun <T> findChild(name: String): T? = null
    private fun closeFloater() {
        System.err.println("LLFloaterScriptRecover: close this floater not yet implemented")
    }
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
        System.err.println("LLScriptRecoverQueue: use JVM equivalent - java.io.File(path).readBytes() not yet implemented")
        return ByteArray(0)
    }
}

// ============================================================================
// Stubs not defined elsewhere in the package
// ============================================================================

object LLFile {
    fun remove(path: String) {
        System.err.println("LLFile: use JVM equivalent - java.io.File(path).delete() not yet implemented")
    }
    fun isFile(path: String): Boolean {
        System.err.println("LLFile: use JVM equivalent - java.io.File(path).isFile not yet implemented")
        return false
    }
    fun tmpdir(): String {
        System.err.println("LLFile: use JVM equivalent - System.getProperty(\"java.io.tmpdir\") not yet implemented")
        return ""
    }
    fun listFiles(dir: String, glob: String): List<String> {
        System.err.println("LLFile: use JVM equivalent - list files matching glob in dir not yet implemented")
        return emptyList()
    }
    fun getBaseFileName(path: String, stripExtension: Boolean): String {
        System.err.println("LLFile: derive base filename from path not yet implemented")
        return ""
    }
}

enum class LLFolderType { LOST_AND_FOUND, TRASH }
enum class AddPosition { BOTTOM }
enum class RecurseMode { UP }

object LLViewerAssetType {
    fun generateDescriptionFor(assetType: LLAssetType): String {
        System.err.println("LLViewerAssetType: generate description for $assetType not yet implemented")
        return ""
    }
}

val gAgentID: UUID get() {
    System.err.println("LLFloaterScriptRecover: global agent UUID not yet implemented")
    return UUID(0L, 0L)
}

object gInventoryModel {
    fun getItem(id: UUID): Any? {
        System.err.println("gInventoryModel: getItem not yet implemented")
        return null
    }
    fun findCategoryUUIDForType(type: LLFolderType): UUID {
        System.err.println("gInventoryModel: findCategoryUUIDForType not yet implemented")
        return UUID(0L, 0L)
    }
    fun changeItemParent(item: Any, parentId: UUID, restamp: Boolean) {
        System.err.println("gInventoryModel: changeItemParent not yet implemented")
    }
    fun updateItem(item: Any) {
        System.err.println("gInventoryModel: updateItem not yet implemented")
    }
    fun notifyObservers() {
        System.err.println("gInventoryModel: notifyObservers not yet implemented")
    }
}

class LLViewerInventoryItemWrapper(source: Any) {
    fun getName(): String {
        System.err.println("LLViewerInventoryItemWrapper: get item name not yet implemented")
        return ""
    }
    fun rename(name: String) {
        System.err.println("LLViewerInventoryItemWrapper: rename item not yet implemented")
    }
    fun updateServer(isNew: Boolean) {
        System.err.println("LLViewerInventoryItemWrapper: updateServer not yet implemented")
    }
}

object LLInventoryPanel {
    fun getActiveInventoryPanel(createIfNeeded: Boolean): LLInventoryPanel? {
        System.err.println("LLInventoryPanel: get active inventory panel not yet implemented")
        return null
    }
    fun getItemByID(id: UUID): Any? {
        System.err.println("LLInventoryPanel: getItemByID not yet implemented")
        return null
    }
    fun setSelection(id: UUID, focus: Boolean) {
        System.err.println("LLInventoryPanel: setSelection not yet implemented")
    }
}

class LLFolderViewFolder {
    fun setOpenArrangeRecursively(open: Boolean, mode: RecurseMode) {
        System.err.println("LLFolderViewFolder: setOpenArrangeRecursively not yet implemented")
    }
}

object LLViewerAssetUpload {
    fun enqueueInventoryUpload(capsUrl: String, itemId: UUID, buffer: ByteArray, cb: (UUID, UUID, UUID, Any) -> Unit) {
        System.err.println("LLViewerAssetUpload: use JVM equivalent - HTTP multipart upload to caps URL not yet implemented")
    }
}

object LLCoreHttpUtil {
    fun isHttpOk(response: Any): Boolean {
        System.err.println("LLCoreHttpUtil: extract HTTP status from LLSD response not yet implemented")
        return false
    }
}

class LLUICtrl {
    fun setCommitCallback(cb: () -> Unit) {
        System.err.println("LLUICtrl: setCommitCallback not yet implemented")
    }
}

data class LLScrollListRow(val checkValue: Boolean, val nameValue: String, val rowValue: Any)

class LLScrollListItem {
    fun getValue(): String {
        System.err.println("LLScrollListItem: get scroll list item string value not yet implemented")
        return ""
    }
    fun getValueObject(): Any {
        System.err.println("LLScrollListItem: get scroll list item value object not yet implemented")
        return Any()
    }
    fun getColumn(index: Int): Any? {
        System.err.println("LLScrollListItem: get scroll list column at index not yet implemented")
        return null
    }
}

class LLScrollListCheck {
    fun getCheckBox(): LLCheckBoxWidget {
        System.err.println("LLScrollListCheck: get check box from scroll list check column not yet implemented")
        return LLCheckBoxWidget()
    }
}

class LLCheckBoxWidget {
    fun getValue(): Boolean {
        System.err.println("LLCheckBoxWidget: get check box value not yet implemented")
        return false
    }
}

fun Any.getID(): UUID {
    System.err.println("LLFloaterScriptRecover: getID not yet implemented")
    return UUID(0L, 0L)
}
fun Any.getSessionID(): UUID {
    System.err.println("LLFloaterScriptRecover: getSessionID not yet implemented")
    return UUID(0L, 0L)
}
fun Any.getRegionCapability(cap: String): String {
    System.err.println("LLFloaterScriptRecover: getRegionCapability '$cap' not yet implemented")
    return ""
}
