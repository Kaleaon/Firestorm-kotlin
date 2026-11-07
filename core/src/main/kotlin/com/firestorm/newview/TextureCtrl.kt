package com.firestorm.newview

import com.firestorm.ui.UICtrl
import com.firestorm.ui.UIColor
import com.firestorm.ui.UIImage
import com.firestorm.ui.Floater
import com.firestorm.ui.FloaterHandle
import com.firestorm.ui.TextBox
import com.firestorm.ui.ViewBorder
import com.firestorm.ui.View
import com.firestorm.ui.Button
import com.firestorm.ui.RadioGroup
import com.firestorm.ui.ScrollListCtrl
import com.firestorm.ui.LineEditor
import com.firestorm.ui.FilterEditor
import com.firestorm.ui.InventoryPanel
import com.firestorm.ui.FolderView
import com.firestorm.inventory.InventoryItem
import com.firestorm.inventory.ViewerInventoryItem
import com.firestorm.render.ViewerFetchedTexture
import com.firestorm.render.ViewerTexture
import com.firestorm.render.FetchedGLTFMaterial
import com.firestorm.types.UUID
import com.firestorm.types.LLSD
import java.util.UUID as JavaUUID

typealias DragNDropCallback = (UICtrl, InventoryItem) -> Boolean
typealias TextureSelectedCallback = (InventoryItem) -> Unit
typealias FloaterCommitCallback = (TextureCtrl.TexturePickOp, PickerSource, UUID, UUID, UUID) -> Unit
typealias FloaterCloseCallback = () -> Unit
typealias SetImageAssetIdCallback = (UUID) -> Unit
typealias SetOnUpdateImageStatsCallback = (ViewerTexture?) -> Unit

enum class PickInventoryType {
    PICK_TEXTURE_MATERIAL,
    PICK_TEXTURE,
    PICK_MATERIAL,
}

enum class PickerSource {
    INVENTORY,
    LOCAL,
    BAKE,
    UNKNOWN,
}

fun getIsPredefinedTexture(assetId: UUID): Boolean {
    return assetId == UUID.DEFAULT_OBJECT_TEXTURE
        || assetId == UUID.DEFAULT_OBJECT_SPECULAR
        || assetId == UUID.DEFAULT_OBJECT_NORMAL
        || assetId == UUID.BLANK_OBJECT_NORMAL
        || assetId == UUID.IMG_WHITE
        || assetId == UUID.SCULPT_DEFAULT_TEXTURE
        || assetId == UUID.BLANK_MATERIAL_ASSET_ID
}

fun getCopyFreeItemByAssetId(imageId: UUID, noTransPerm: Boolean = false): UUID {
    System.err.println("TextureCtrl: getCopyFreeItemByAssetId not yet implemented")
    return UUID.NULL
}

fun getCanCopyTexture(imageId: UUID): Boolean {
    return getIsPredefinedTexture(imageId) || getCopyFreeItemByAssetId(imageId) != UUID.NULL
}

open class TextureCtrl(params: Params) : UICtrl(params) {

    enum class TexturePickOp {
        TEXTURE_CHANGE,
        TEXTURE_SELECT,
        TEXTURE_CANCEL,
    }

    data class Params(
        val imageId: UUID = UUID.NULL,
        val defaultImageId: UUID = UUID.NULL,
        val defaultImageName: String = "",
        val pickType: PickInventoryType = PickInventoryType.PICK_TEXTURE,
        val allowNoTexture: Boolean = false,
        val canApplyImmediately: Boolean = false,
        val noCommitOnSelection: Boolean = false,
        val labelWidth: Int = -1,
        val borderColor: UIColor? = null,
        val fallbackImage: UIImage? = null,
        val showCaption: Boolean = true,
    ) : UICtrl.Params()

    var dragCallback: DragNDropCallback? = null
    var dropCallback: DragNDropCallback? = null
    var onCancelCallback: ((UICtrl) -> Unit)? = null
    var onSelectCallback: ((UICtrl) -> Unit)? = null
    var onCloseCallback: ((UICtrl) -> Unit)? = null
    var onTextureSelectedCallback: TextureSelectedCallback? = null

    var texturep: ViewerFetchedTexture? = null
        private set
    var gltfMaterial: FetchedGLTFMaterial? = null
        private set
    private var gltfPreview: ViewerTexture? = null

    private var borderColor: UIColor = params.borderColor ?: UIColor.DEFAULT
    var imageItemID: UUID = params.imageId
        private set
    var imageAssetID: UUID = params.imageId
        private set
    var defaultImageAssetID: UUID = params.defaultImageId
        private set
    var blankImageAssetID: UUID = UUID.NULL
    private var localTrackingID: UUID = UUID.NULL
    private var fallbackImage: UIImage? = params.fallbackImage
    var defaultImageName: String = params.defaultImageName
        private set
    private var floaterHandle: FloaterHandle = FloaterHandle.DEAD
    private var tentativeLabel: TextBox? = null
    private var caption: TextBox? = null
    private var captionHeight: Int = 0
    var label: String = ""
        private set
    var allowNoTexture: Boolean = params.allowNoTexture
    var allowLocalTexture: Boolean = true
    private var immediateFilterPermMask: Int = 0
    private var dndFilterPermMask: Int = 0
    private var canApplyImmediately: Boolean = params.canApplyImmediately
    private var commitOnSelection: Boolean = !params.noCommitOnSelection
    private var needsRawImageData: Boolean = false
    private var border: ViewBorder? = null
    var valid: Boolean = true
    private var showLoadingPlaceholder: Boolean = false
    private var loadingPlaceholderString: String = ""
    private var labelWidth: Int = params.labelWidth
    private var openTexPreview: Boolean = false
    var bakeTextureEnabled: Boolean = false
    var inventoryPickType: PickInventoryType = params.pickType
    var isMasked: Boolean = false
    var isPreviewDisabled: Boolean = false
    private var textEnabledColor: UIColor = UIColor.DEFAULT
    private var textDisabledColor: UIColor = UIColor.DEFAULT

    fun isPickerShown(): Boolean = !floaterHandle.isDead()
    fun isImageLocal(): Boolean = localTrackingID != UUID.NULL
    fun getLocalTrackingID(): UUID = localTrackingID

    fun setLabel(label: String) { this.label = label }
    fun setLabelWidth(labelWidth: Int) { this.labelWidth = labelWidth }
    fun setAllowLocalTexture(b: Boolean) { allowLocalTexture = b }
    fun setOpenTexPreview(openPreview: Boolean) { openTexPreview = openPreview }
    fun setCanApplyImmediately(b: Boolean) { canApplyImmediately = b }

    open fun setImageAssetName(name: String) {
        System.err.println("TextureCtrl: setImageAssetName not yet implemented")
    }

    fun setImageAssetID(imageAssetId: UUID) {
        this.imageAssetID = imageAssetId
        System.err.println("TextureCtrl: setImageAssetID not yet implemented")
    }

    fun setDefaultImageAssetID(id: UUID) { defaultImageAssetID = id }
    fun setBlankImageAssetID(id: UUID) { blankImageAssetID = id }

    fun setCaption(caption: String) {
        this.caption?.setText(caption)
    }

    fun setCanApply(canPreview: Boolean, canApply: Boolean) {
        System.err.println("TextureCtrl: setCanApply not yet implemented")
    }

    fun setImmediateFilterPermMask(mask: Int) { immediateFilterPermMask = mask }
    fun setDnDFilterPermMask(mask: Int) { dndFilterPermMask = mask }
    fun getImmediateFilterPermMask(): Int = immediateFilterPermMask
    fun setFilterPermissionMasks(mask: Int) {
        immediateFilterPermMask = mask
        dndFilterPermMask = mask
    }

    fun closeDependentFloater() {
        System.err.println("TextureCtrl: closeDependentFloater not yet implemented")
    }

    fun showPicker(takeFocus: Boolean) {
        System.err.println("TextureCtrl: showPicker not yet implemented")
    }

    fun onFloaterClose() {
        onCloseCallback?.invoke(this)
    }

    fun onFloaterCommit(
        op: TexturePickOp,
        source: PickerSource,
        localId: UUID,
        invId: UUID,
        trackingId: UUID,
    ) {
        when (op) {
            TexturePickOp.TEXTURE_CANCEL -> onCancelCallback?.invoke(this)
            TexturePickOp.TEXTURE_SELECT -> onSelectCallback?.invoke(this)
            TexturePickOp.TEXTURE_CHANGE -> onSelectCallback?.invoke(this)
        }
    }

    fun setOnTextureSelectedCallback(cb: TextureSelectedCallback) {
        onTextureSelectedCallback = cb
    }

    fun setShowLoadingPlaceholder(showLoadingPlaceholder: Boolean) {
        this.showLoadingPlaceholder = showLoadingPlaceholder
    }

    fun setBakeTextureEnabled(enabled: Boolean) { bakeTextureEnabled = enabled }
    fun setInventoryPickType(type: PickInventoryType) { inventoryPickType = type }

    fun setLabelColor(c: UIColor) { textEnabledColor = c; updateLabelColor() }
    fun setDisabledLabelColor(c: UIColor) { textDisabledColor = c; updateLabelColor() }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        if (!visible) closeDependentFloater()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        updateLabelColor()
    }

    override fun setValue(value: LLSD) { setImageAssetID(value.asUUID()) }
    override fun getValue(): LLSD = LLSD.fromUUID(imageAssetID)

    override fun draw() {
        // no-op
    }

    override fun clear() {
        setImageAssetID(UUID.NULL)
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        showPicker(true)
        return true
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int,
        drop: Boolean, cargoType: Int, cargoData: Any?,
        accept: IntArray, tooltipMsg: StringBuilder,
    ): Boolean {
        val item = cargoData as? InventoryItem ?: return false
        return if (!drop) {
            dragCallback?.invoke(this, item) ?: allowDrop(item, cargoType, tooltipMsg)
        } else {
            dropCallback?.invoke(this, item) ?: doDrop(item)
        }
    }

    private fun allowDrop(item: InventoryItem, cargoType: Int, tooltipMsg: StringBuilder): Boolean {
        System.err.println("TextureCtrl: allowDrop not yet implemented")
        return false
    }

    private fun doDrop(item: InventoryItem): Boolean {
        System.err.println("TextureCtrl: doDrop not yet implemented")
        return false
    }

    private fun updateLabelColor() {
        System.err.println("TextureCtrl: updateLabelColor not yet implemented")
    }
}

open class FloaterTexturePicker(
    private var owner: View?,
    imageAssetId: UUID,
    defaultImageAssetId: UUID,
    blankImageAssetId: UUID,
    tentative: Boolean,
    allowNoTexture: Boolean,
    private val label: String,
    immediateFilterPermMask: Int,
    dndFilterPermMask: Int,
    canApplyImmediately: Boolean,
    private val fallbackImage: UIImage?,
    private var inventoryPickType: PickInventoryType,
) : Floater() {

    companion object {
        var lastPickerMode: Int = 0
    }

    var imageAssetID: UUID = imageAssetId
        protected set
    val defaultImageAssetID: UUID = defaultImageAssetId
    val blankImageAssetID: UUID = blankImageAssetId
    private val originalImageAssetID: UUID = imageAssetId
    private var allowNoTexture: Boolean = allowNoTexture
    private var active: Boolean = true
    private var canApplyImmediately: Boolean = canApplyImmediately
    private var immediateFilterPermMask: Int = immediateFilterPermMask
    private var dndFilterPermMask: Int = dndFilterPermMask
    private var noCopyTextureSelected: Boolean = false
    private var contextConeOpacity: Float = 0f
    private var selectedItemPinned: Boolean = false
    private var canApply: Boolean = true
    private var canPreview: Boolean = true
    private var previewSettingChanged: Boolean = false
    private var limitsSet: Boolean = false
    private var maxDim: Int = Int.MAX_VALUE
    private var minDim: Int = 0
    private var bakeTextureEnabled: Boolean = false
    private var localTextureEnabled: Boolean = false

    protected var texturep: ViewerFetchedTexture? = null
    protected var gltfMaterial: FetchedGLTFMaterial? = null
    protected var gltfPreview: ViewerTexture? = null
    private var tentativeLabel: TextBox? = null
    private var resolutionLabel: TextBox? = null
    private var resolutionWarning: TextBox? = null
    private var pendingName: String = ""
    private var filterEdit: FilterEditor? = null
    private var inventoryPanel: InventoryPanel? = null
    protected var modeSelector: RadioGroup? = null
    private var localScrollCtrl: ScrollListCtrl? = null
    private var defaultBtn: Button? = null
    private var noneBtn: Button? = null
    private var blankBtn: Button? = null
    private var pipetteBtn: Button? = null
    private var selectBtn: Button? = null
    private var cancelBtn: Button? = null
    private var previewWidget: View? = null
    private var uuidEditor: LineEditor? = null
    private var uuidBtn: Button? = null
    private var transparentBtn: Button? = null

    var textureSelectedCallback: TextureSelectedCallback? = null
    var onFloaterCloseCallback: FloaterCloseCallback? = null
    var onFloaterCommitCallback: FloaterCommitCallback? = null
    var setImageAssetIDCallback: SetImageAssetIdCallback? = null
    var onUpdateImageStatsCallback: SetOnUpdateImageStatsCallback? = null

    init {
        setTentative(tentative)
    }

    fun setImageID(imageId: UUID, setSelection: Boolean = true) {
        if ((imageAssetID != imageId || isTentative()) && active) {
            noCopyTextureSelected = false
            imageAssetID = imageId
            System.err.println("FloaterTexturePicker: setImageID not yet implemented")
        }
    }

    fun updateImageStats(): Boolean {
        System.err.println("FloaterTexturePicker: updateImageStats not yet implemented")
        return false
    }

    fun getAssetID(): UUID = imageAssetID

    fun findItemID(assetId: UUID, copyableOnly: Boolean, ignoreLibrary: Boolean = false): UUID {
        System.err.println("FloaterTexturePicker: findItemID not yet implemented")
        return UUID.NULL
    }

    fun setCanApplyImmediately(b: Boolean) {
        canApplyImmediately = b
        System.err.println("FloaterTexturePicker: setCanApplyImmediately not yet implemented")
    }

    fun setActive(active: Boolean) {
        if (!active && isUsingPipette()) stopUsingPipette()
        this.active = active
    }

    fun getOwner(): View? = owner
    fun setOwner(owner: View?) { this.owner = owner }

    fun stopUsingPipette() {
        System.err.println("FloaterTexturePicker: stopUsingPipette not yet implemented")
    }

    private fun isUsingPipette(): Boolean {
        System.err.println("FloaterTexturePicker: isUsingPipette not yet implemented")
        return false
    }

    fun commitIfImmediateSet() {
        if (canApplyImmediately) commitCallback(TextureCtrl.TexturePickOp.TEXTURE_CHANGE)
    }

    fun commitCallback(op: TextureCtrl.TexturePickOp) {
        val source = PickerSource.INVENTORY
        onFloaterCommitCallback?.invoke(op, source, imageAssetID, UUID.NULL, UUID.NULL)
    }

    fun commitCancel() {
        commitCallback(TextureCtrl.TexturePickOp.TEXTURE_CANCEL)
    }

    fun onFilterEdit(searchString: String) {
        System.err.println("FloaterTexturePicker: onFilterEdit not yet implemented")
    }

    fun setCanApply(canPreview: Boolean, canApply: Boolean, inworldImage: Boolean = true) {
        this.canPreview = canPreview
        this.canApply = canApply
    }

    fun setMinDimensionsLimits(minDim: Int) {
        limitsSet = true
        this.minDim = minDim
    }

    fun setLocalTextureEnabled(enabled: Boolean) { localTextureEnabled = enabled }
    fun setBakeTextureEnabled(enabled: Boolean) { bakeTextureEnabled = enabled }
    fun setInventoryPickType(type: PickInventoryType) { inventoryPickType = type }
    fun setImmediateFilterPermMask(mask: Int) { immediateFilterPermMask = mask }

    override fun postBuild(): Boolean {
        System.err.println("FloaterTexturePicker: postBuild not yet implemented")
        return false
    }

    override fun onOpen(key: LLSD) {
        if (lastPickerMode != 0) {
            System.err.println("FloaterTexturePicker: onOpen not yet implemented")
        }
    }

    override fun onClose(appSettings: Boolean) {
        if (owner != null) onFloaterCloseCallback?.invoke()
        stopUsingPipette()
        lastPickerMode = modeSelector?.getSelectedIndex() ?: 0
        gltfPreview = null
    }

    override fun draw() {
        // no-op
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int,
        drop: Boolean, cargoType: Int, cargoData: Any?,
        accept: IntArray, tooltipMsg: StringBuilder,
    ): Boolean {
        System.err.println("FloaterTexturePicker: handleDragAndDrop not yet implemented")
        return false
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        System.err.println("FloaterTexturePicker: handleKeyHere not yet implemented")
        return false
    }

    fun onSelectionChange(items: List<Any>, userAction: Boolean) {
        System.err.println("FloaterTexturePicker: onSelectionChange not yet implemented")
    }

    fun onTextureSelect(te: Any) {
        System.err.println("FloaterTexturePicker: onTextureSelect not yet implemented")
    }

    protected fun changeMode() {
        System.err.println("FloaterTexturePicker: changeMode not yet implemented")
    }

    protected fun refreshLocalList() {
        System.err.println("FloaterTexturePicker: refreshLocalList not yet implemented")
    }

    protected fun refreshInventoryFilter() {
        System.err.println("FloaterTexturePicker: refreshInventoryFilter not yet implemented")
    }

    protected fun setImageIDFromItem(itemp: InventoryItem, setSelection: Boolean = true) {
        System.err.println("FloaterTexturePicker: setImageIDFromItem not yet implemented")
    }

    protected fun findInvItem(assetId: UUID, copyableOnly: Boolean, ignoreLibrary: Boolean = false): ViewerInventoryItem? {
        System.err.println("FloaterTexturePicker: findInvItem not yet implemented")
        return null
    }

    companion object {
        fun onBtnSetToDefault(picker: FloaterTexturePicker) {
            picker.setImageID(picker.defaultImageAssetID)
            picker.commitIfImmediateSet()
        }

        fun onBtnApplyTexture(picker: FloaterTexturePicker) {
            System.err.println("FloaterTexturePicker: onBtnApplyTexture not yet implemented")
        }

        fun onBtnSelect(picker: FloaterTexturePicker) {
            picker.commitCallback(TextureCtrl.TexturePickOp.TEXTURE_SELECT)
            picker.closeFloater()
        }

        fun onBtnCancel(picker: FloaterTexturePicker) {
            picker.commitCancel()
            picker.closeFloater()
        }

        fun onBtnBlank(picker: FloaterTexturePicker) {
            picker.setImageID(picker.blankImageAssetID)
            picker.commitIfImmediateSet()
        }

        fun onBtnTransparent(picker: FloaterTexturePicker) {
            System.err.println("FloaterTexturePicker: onBtnTransparent not yet implemented")
        }

        fun onBtnNone(picker: FloaterTexturePicker) {
            picker.setImageID(UUID.NULL)
            picker.commitIfImmediateSet()
        }

        fun onApplyImmediateCheck(ctrl: UICtrl, picker: FloaterTexturePicker) {
            System.err.println("FloaterTexturePicker: onApplyImmediateCheck not yet implemented")
        }

        fun onModeSelect(ctrl: UICtrl, picker: FloaterTexturePicker) {
            picker.changeMode()
        }

        fun onBtnAdd(picker: FloaterTexturePicker) {
            System.err.println("FloaterTexturePicker: onBtnAdd not yet implemented")
        }

        fun onBtnRemove(picker: FloaterTexturePicker) {
            System.err.println("FloaterTexturePicker: onBtnRemove not yet implemented")
        }

        fun onBtnUpload(picker: FloaterTexturePicker) {
            System.err.println("FloaterTexturePicker: onBtnUpload not yet implemented")
        }

        fun onLocalScrollCommit(ctrl: UICtrl, picker: FloaterTexturePicker) {
            System.err.println("FloaterTexturePicker: onLocalScrollCommit not yet implemented")
        }

        fun onBakeTextureSelect(ctrl: UICtrl, picker: FloaterTexturePicker) {
            System.err.println("FloaterTexturePicker: onBakeTextureSelect not yet implemented")
        }

        fun onPickerCallback(filenames: List<String>, handle: FloaterHandle) {
            System.err.println("FloaterTexturePicker: onPickerCallback not yet implemented")
        }
    }
}
