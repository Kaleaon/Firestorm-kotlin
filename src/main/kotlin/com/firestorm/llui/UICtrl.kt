package com.firestorm.llui

const val TAKE_FOCUS_YES = true
const val TAKE_FOCUS_NO = false
const val DROP_SHADOW_FLOATER = 5

abstract class UICtrl(params: Params = Params()) : View(params) {

    // --- type aliases replacing boost::signals2 signal types ---
    typealias CommitCallback = (ctrl: UICtrl, param: LLSD) -> Unit
    typealias EnableCallback = (ctrl: UICtrl, param: LLSD) -> Boolean
    typealias MouseCallback  = (ctrl: UICtrl, x: Int, y: Int, mask: Int) -> Unit

    // --- parameter / config data classes ---

    data class CallbackParam(
        var functionName: String = "",
        var parameter: LLSD = LLSD(),
        var controlName: String = ""
    )

    data class CommitCallbackParam(
        var base: CallbackParam = CallbackParam(),
        var function: CommitCallback? = null
    )

    data class EnableCallbackParam(
        var base: CallbackParam = CallbackParam(),
        var function: EnableCallback? = null
    )

    data class EnableControls(
        var enabled: String = "",
        var disabled: String = ""
    )

    data class ControlVisibility(
        var visible: String = "",
        var invisible: String = ""
    )

    open class Params : View.Params() {
        var label: String = ""
        var tabStop: Boolean = true
        var chrome: Boolean = false
        var requestsFront: Boolean = false
        var initialValue: LLSD = LLSD()
        var initialValueProvided: Boolean = false
        var initCallback: CommitCallbackParam? = null
        var commitCallback: CommitCallbackParam? = null
        var validateCallback: EnableCallbackParam? = null
        var mouseenterCallback: CommitCallbackParam? = null
        var mouseleaveCallback: CommitCallbackParam? = null
        var controlName: String = ""
        var enabledControls: EnableControls? = null
        var controlsVisibility: ControlVisibility? = null
    }

    enum class TypeTransparency {
        TT_DEFAULT,
        TT_ACTIVE,
        TT_INACTIVE,
        TT_FADING,
        TT_FORCE_OPAQUE
    }

    // --- signals (lists of lambdas replacing boost::signals2) ---
    protected val commitSignal:          MutableList<CommitCallback> = mutableListOf()
    protected val validateSignal:        MutableList<EnableCallback> = mutableListOf()
    protected val mouseEnterSignal:      MutableList<CommitCallback> = mutableListOf()
    protected val mouseLeaveSignal:      MutableList<CommitCallback> = mutableListOf()
    protected val mouseDownSignal:       MutableList<MouseCallback>  = mutableListOf()
    protected val mouseUpSignal:         MutableList<MouseCallback>  = mutableListOf()
    protected val rightMouseDownSignal:  MutableList<MouseCallback>  = mutableListOf()
    protected val rightMouseUpSignal:    MutableList<MouseCallback>  = mutableListOf()
    protected val doubleClickSignal:     MutableList<MouseCallback>  = mutableListOf()

    // --- view-model ---
    protected var viewModel: ViewModel = ViewModel()

    // --- control variables (opaque handles; real wiring happens at a higher layer) ---
    var controlVariable: ControlVariable? = null
        protected set
    var enabledControlVariable: ControlVariable? = null
        protected set
    var disabledControlVariable: ControlVariable? = null
        protected set
    var makeVisibleControlVariable: ControlVariable? = null
        protected set
    var makeInvisibleControlVariable: ControlVariable? = null
        protected set

    protected var functionName: String = ""

    // --- layout / chrome state ---
    private var isChrome: Boolean = false
    private var requestsFront: Boolean = params.requestsFront
    private var tabStop: Boolean = false
    private var tentative: Boolean = false
    var transparencyType: TypeTransparency = TypeTransparency.TT_DEFAULT
        private set

    var transparencyOverrideCallback: ((TypeTransparency, Float) -> Float)? = null

    companion object {
        var activeControlTransparency: Float = 1.0f
        var inactiveControlTransparency: Float = 1.0f

        fun controlListener(newValue: LLSD, ctrl: UICtrl?, type: String): Boolean {
            ctrl ?: return false
            return when (type) {
                "value" -> {
                    ctrl.setValue(newValue)
                    true
                }
                "enabled" -> {
                    ctrl.setEnabled(newValue.asBoolean())
                    true
                }
                "disabled" -> {
                    ctrl.setEnabled(!newValue.asBoolean())
                    true
                }
                "visible", "invisible" -> {
                    ctrl.decideVisibility()
                    true
                }
                else -> false
            }
        }
    }

    // --- init ---

    init {
        initFromParams(params)
    }

    open fun initFromParams(p: Params) {
        requestsFront = p.requestsFront
        setIsChrome(p.chrome)
        setControlName(p.controlName)

        p.enabledControls?.let { ec ->
            if (ec.enabled.isNotEmpty()) {
                val ctrl = findControl(ec.enabled)
                if (ctrl != null) setEnabledControlVariable(ctrl)
            } else if (ec.disabled.isNotEmpty()) {
                val ctrl = findControl(ec.disabled)
                if (ctrl != null) setDisabledControlVariable(ctrl)
            }
        }

        p.controlsVisibility?.let { cv ->
            if (cv.visible.isNotEmpty()) {
                val ctrl = findControl(cv.visible)
                if (ctrl != null) setMakeVisibleControlVariable(ctrl)
            }
            if (cv.invisible.isNotEmpty()) {
                val ctrl = findControl(cv.invisible)
                if (ctrl != null) setMakeInvisibleControlVariable(ctrl)
            }
        }

        setTabStop(p.tabStop)

        if (p.initialValueProvided && p.controlName.isEmpty()) {
            setValue(p.initialValue)
        }

        p.commitCallback?.let { addCommitCallback(initCommitCallback(it)) }
        p.validateCallback?.let { addValidateCallback(initEnableCallback(it)) }

        p.initCallback?.let { ic ->
            if (ic.function != null) {
                ic.function!!(this, ic.base.parameter)
            }
        }

        p.mouseenterCallback?.let { addMouseEnterCallback(initCommitCallback(it)) }
        p.mouseleaveCallback?.let { addMouseLeaveCallback(initCommitCallback(it)) }
    }

    protected fun initCommitCallback(cb: CommitCallbackParam): CommitCallback {
        if (cb.function != null) return cb.function!!
        val name = cb.base.functionName
        if (name.isNotEmpty()) setFunctionName(name)
        return { _, _ -> }
    }

    protected fun initEnableCallback(cb: EnableCallbackParam): EnableCallback {
        if (cb.function != null) return cb.function!!
        return { _, _ -> true }
    }

    // --- View overrides ---

    override fun isCtrl(): Boolean = true

    override fun onMouseEnter(x: Int, y: Int, mask: Int) {
        val v = getValue()
        mouseEnterSignal.forEach { it(this, v) }
    }

    override fun onMouseLeave(x: Int, y: Int, mask: Int) {
        val v = getValue()
        mouseLeaveSignal.forEach { it(this, v) }
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val handled = super.handleMouseDown(x, y, mask)
        mouseDownSignal.forEach { it(this, x, y, mask) }
        return handled
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        val handled = super.handleMouseUp(x, y, mask)
        mouseUpSignal.forEach { it(this, x, y, mask) }
        return handled
    }

    override fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val handled = super.handleRightMouseDown(x, y, mask)
        rightMouseDownSignal.forEach { it(this, x, y, mask) }
        return handled
    }

    override fun handleRightMouseUp(x: Int, y: Int, mask: Int): Boolean {
        val handled = super.handleRightMouseUp(x, y, mask)
        rightMouseUpSignal.forEach { it(this, x, y, mask) }
        return handled
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        val handled = super.handleDoubleClick(x, y, mask)
        doubleClickSignal.forEach { it(this, x, y, mask) }
        return handled
    }

    override fun canFocusChildren(): Boolean = hasTabStop()

    override fun postBuild(): Boolean {
        if (childCount > 0) {
            val toFront = children.filterIsInstance<UICtrl>().filter { it.requestsFront }
            toFront.forEach { sendChildToFront(it) }
        }
        return super.postBuild()
    }

    override fun setLabelArg(key: String, text: String): Boolean = false

    // --- focus ---

    override fun hasFocus(): Boolean = TODO("APR: use JVM equivalent for focus manager")

    override fun setFocus(b: Boolean) {
        if (!isEnabled()) return
        TODO("APR: use JVM equivalent for focus manager")
    }

    // --- interfaces (return null by default; subclasses override) ---

    open fun getSelectionInterface(): CtrlSelectionInterface? = null
    open fun getListInterface(): CtrlListInterface? = null
    open fun getScrollInterface(): CtrlScrollInterface? = null

    // --- control variable wiring ---

    fun setControlValue(value: LLSD): Boolean {
        controlVariable?.set(value) ?: return false
        return true
    }

    fun setControlVariable(control: ControlVariable?) {
        controlVariable = null
        if (control != null) {
            controlVariable = control
            control.signal.add { newVal -> controlListener(newVal, this, "value") }
            setValue(control.getValue())
        }
    }

    fun removeControlVariable() {
        controlVariable = null
    }

    open fun setControlName(controlName: String, context: View? = null) {
        if (controlName.isEmpty()) return
        val ctx = context ?: this
        val control = ctx.findControl(controlName)
        setControlVariable(control)
    }

    fun setEnabledControlVariable(control: ControlVariable?) {
        enabledControlVariable = null
        if (control != null) {
            enabledControlVariable = control
            control.signal.add { newVal -> controlListener(newVal, this, "enabled") }
            setEnabled(control.getValue().asBoolean())
        }
    }

    fun setDisabledControlVariable(control: ControlVariable?) {
        disabledControlVariable = null
        if (control != null) {
            disabledControlVariable = control
            control.signal.add { newVal -> controlListener(newVal, this, "disabled") }
            setEnabled(!control.getValue().asBoolean())
        }
    }

    fun setMakeVisibleControlVariable(control: ControlVariable?) {
        makeVisibleControlVariable = null
        if (control != null) {
            makeVisibleControlVariable = control
            control.signal.add { newVal -> controlListener(newVal, this, "visible") }
            decideVisibility()
        }
    }

    fun setMakeInvisibleControlVariable(control: ControlVariable?) {
        makeInvisibleControlVariable = null
        if (control != null) {
            makeInvisibleControlVariable = control
            control.signal.add { newVal -> controlListener(newVal, this, "invisible") }
            decideVisibility()
        }
    }

    fun setFunctionName(name: String) { functionName = name }

    // --- value ---

    open fun setValue(value: LLSD) { viewModel.setValue(value) }
    open fun getValue(): LLSD = viewModel.getValue()
    open fun shareViewModelFrom(other: UICtrl) { viewModel = other.viewModel }
    open fun getViewModel(): ViewModel = viewModel

    open fun setTextArg(key: String, text: String): Boolean = false
    open fun setIsChrome(isChrome: Boolean) { this.isChrome = isChrome }

    fun getIsChrome(): Boolean {
        if (isChrome) return true
        var parentCtrl: View? = getParent()
        while (parentCtrl != null) {
            if (parentCtrl.isCtrl()) return (parentCtrl as UICtrl).getIsChrome()
            parentCtrl = parentCtrl.getParent()
        }
        return false
    }

    open fun acceptsTextInput(): Boolean = false

    open fun isDirty(): Boolean = viewModel.isDirty()
    open fun resetDirty() { viewModel.resetDirty() }

    open fun onCommit() {
        val v = getValue()
        commitSignal.forEach { it(this, v) }
    }

    open fun onTabInto() { onUpdateScrollToChild(this) }
    open fun clear() {}
    open fun setColor(color: UIColor) {}

    open fun getCurrentTransparency(): Float {
        val alpha = when (transparencyType) {
            TypeTransparency.TT_DEFAULT      -> getDrawContext().alpha
            TypeTransparency.TT_ACTIVE       -> activeControlTransparency
            TypeTransparency.TT_INACTIVE     -> inactiveControlTransparency
            TypeTransparency.TT_FADING       -> inactiveControlTransparency / 2.0f
            TypeTransparency.TT_FORCE_OPAQUE -> 1.0f
        }
        return transparencyOverrideCallback?.invoke(transparencyType, alpha) ?: alpha
    }

    fun setTransparencyType(type: TypeTransparency) { transparencyType = type }

    open fun setTentative(b: Boolean) { tentative = b }
    open fun getTentative(): Boolean = tentative

    // --- tab stop ---

    open fun setTabStop(b: Boolean) { tabStop = b }
    open fun hasTabStop(): Boolean = tabStop

    // --- focus traversal ---

    open fun focusFirstItem(preferTextFields: Boolean = false, focusFlash: Boolean = true): Boolean {
        TODO("APR: use JVM equivalent for focus/tab-order traversal")
    }

    fun focusNextItem(textFieldsOnly: Boolean): Boolean {
        TODO("APR: use JVM equivalent for focus/tab-order traversal")
    }

    fun focusPrevItem(textFieldsOnly: Boolean): Boolean {
        TODO("APR: use JVM equivalent for focus/tab-order traversal")
    }

    fun findRootMostFocusRoot(): UICtrl? {
        var focusRoot: UICtrl? = null
        var next: UICtrl? = this
        while (next != null && next.hasTabStop()) {
            if (next.isFocusRoot()) focusRoot = next
            next = next.getParentUICtrl()
        }
        return focusRoot
    }

    fun getParentUICtrl(): UICtrl? {
        var parent: View? = getParent()
        while (parent != null) {
            if (parent.isCtrl()) return parent as UICtrl
            parent = parent.getParent()
        }
        return null
    }

    fun findHelpTopic(): String? {
        var ctrl: UICtrl? = this
        while (ctrl != null) {
            val panel = ctrl as? Panel
            if (panel != null) {
                val sub = panel.findVisibleChildPanelWithHelpTopic()
                if (sub != null) return sub
                val active = panel.findActiveTabHelpTopic()
                if (active != null) return active
                if (panel.helpTopic.isNotEmpty()) return panel.helpTopic
            }
            ctrl = ctrl.getParentUICtrl()
        }
        return null
    }

    // --- signal subscription ---

    fun addCommitCallback(cb: CommitCallback)         { commitSignal.add(cb) }
    fun addValidateCallback(cb: EnableCallback)        { validateSignal.add(cb) }
    fun addMouseEnterCallback(cb: CommitCallback)      { mouseEnterSignal.add(cb) }
    fun addMouseLeaveCallback(cb: CommitCallback)      { mouseLeaveSignal.add(cb) }
    fun addMouseDownCallback(cb: MouseCallback)        { mouseDownSignal.add(cb) }
    fun addMouseUpCallback(cb: MouseCallback)          { mouseUpSignal.add(cb) }
    fun addRightMouseDownCallback(cb: MouseCallback)   { rightMouseDownSignal.add(cb) }
    fun addRightMouseUpCallback(cb: MouseCallback)     { rightMouseUpSignal.add(cb) }
    fun addDoubleClickCallback(cb: MouseCallback)      { doubleClickSignal.add(cb) }

    // --- visibility decision (visible/invisible control variables may both be set) ---

    internal fun decideVisibility() {
        var visible = true
        if (makeVisibleControlVariable != null &&
            !makeVisibleControlVariable!!.getValue().asBoolean()) {
            visible = false
        } else if (makeInvisibleControlVariable != null &&
            makeInvisibleControlVariable!!.getValue().asBoolean()) {
            visible = false
        }
        setVisible(visible)
    }

    override fun addInfo(info: LLSD) {
        super.addInfo(info)
        info["value"] = getValue()
    }
}

// --- stub interfaces (subclasses may implement) ---
interface CtrlSelectionInterface
interface CtrlListInterface
interface CtrlScrollInterface
