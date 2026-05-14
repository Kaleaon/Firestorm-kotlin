package com.firestorm.newview

import com.firestorm.llmath.Vector3

private const val PATH_TOOL_NAME = "PathfindingPathTool"

object LLPathfindingPathTool : Tool(PATH_TOOL_NAME) {

    enum class EPathStatus {
        kPathStatusUnknown,
        kPathStatusChooseStartAndEndPoints,
        kPathStatusChooseStartPoint,
        kPathStatusChooseEndPoint,
        kPathStatusHasValidPath,
        kPathStatusHasInvalidPath,
        kPathStatusNotEnabled,
        kPathStatusNotImplemented,
        kPathStatusError
    }

    enum class ECharacterType {
        kCharacterTypeNone,
        kCharacterTypeA,
        kCharacterTypeB,
        kCharacterTypeC,
        kCharacterTypeD
    }

    typealias PathEventCallback = () -> Unit

    private val finalPathData: PathingPacket = PathingPacket()
    private val tempPathData: PathingPacket = PathingPacket()
    private var pathResult: LLPLResult = LLPLResult.LLPL_NO_PATH
    private var characterType: ECharacterType = ECharacterType.kCharacterTypeNone
    private val pathEventListeners: MutableList<PathEventCallback> = mutableListOf()
    private var isLeftMouseButtonHeld: Boolean = false
    private var isMiddleMouseButtonHeld: Boolean = false
    private var isRightMouseButtonHeld: Boolean = false

    init {
        setCharacterWidth(1.0f)
        setCharacterType(characterType)
    }

    override fun handleMouseDown(x: Int, y: Int, mask: MASK): Boolean {
        var returnVal = false
        if (!isLeftMouseButtonHeld && !isMiddleMouseButtonHeld && !isRightMouseButtonHeld) {
            if (isAnyPathToolModKeys(mask)) {
                // no-op
                computeFinalPoints(x, y, mask)
                isLeftMouseButtonHeld = true
                setMouseCapture(true)
                returnVal = true
            } else if (!isCameraModKeys(mask)) {
                // no-op
                isLeftMouseButtonHeld = true
                setMouseCapture(true)
                returnVal = true
            }
        }
        isLeftMouseButtonHeld = true
        return returnVal
    }

    override fun handleMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        var returnVal = false
        if (isLeftMouseButtonHeld && !isMiddleMouseButtonHeld && !isRightMouseButtonHeld) {
            computeFinalPoints(x, y, mask)
            setMouseCapture(false)
            returnVal = true
        }
        isLeftMouseButtonHeld = false
        return returnVal
    }

    fun handleMiddleMouseDown(x: Int, y: Int, mask: MASK): Boolean {
        setMouseCapture(true)
        isMiddleMouseButtonHeld = true
        // no-op
        return true
    }

    fun handleMiddleMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        if (!isLeftMouseButtonHeld && isMiddleMouseButtonHeld && !isRightMouseButtonHeld) {
            setMouseCapture(false)
        }
        isMiddleMouseButtonHeld = false
        return true
    }

    override fun handleRightMouseDown(x: Int, y: Int, mask: MASK): Boolean {
        setMouseCapture(true)
        isRightMouseButtonHeld = true
        // no-op
        return true
    }

    override fun handleRightMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        if (!isLeftMouseButtonHeld && !isMiddleMouseButtonHeld && isRightMouseButtonHeld) {
            setMouseCapture(false)
        }
        isRightMouseButtonHeld = false
        return true
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: MASK): Boolean = true

    override fun handleHover(x: Int, y: Int, mask: MASK): Boolean {
        var returnVal = false
        if (!isLeftMouseButtonHeld && !isMiddleMouseButtonHeld && !isRightMouseButtonHeld && !isAnyPathToolModKeys(mask)) {
            // no-op
        }
        if (!isMiddleMouseButtonHeld && !isRightMouseButtonHeld && isAnyPathToolModKeys(mask)) {
            // no-op
            computeTempPoints(x, y, mask)
            returnVal = true
        } else {
            clearTemp()
            computeFinalPath()
        }
        return returnVal
    }

    override fun handleKey(key: KEY, mask: MASK): Boolean {
        // Eat KEY_ESCAPE to prevent the camera tool from resetting the toolset
        // while pathfinding path elements are on screen.
        return key == KEY_ESCAPE
    }

    fun getPathStatus(): EPathStatus {
        val pathingLibAvailable = false
        return when {
            !pathingLibAvailable -> EPathStatus.kPathStatusNotImplemented
            false ->
                EPathStatus.kPathStatusUnknown
            false ->
                EPathStatus.kPathStatusNotEnabled
            !hasFinalA() && !hasFinalB() -> EPathStatus.kPathStatusChooseStartAndEndPoints
            !hasFinalA() -> EPathStatus.kPathStatusChooseStartPoint
            !hasFinalB() -> EPathStatus.kPathStatusChooseEndPoint
            pathResult == LLPLResult.LLPL_PATH_GENERATED_OK -> EPathStatus.kPathStatusHasValidPath
            pathResult == LLPLResult.LLPL_NO_PATH -> EPathStatus.kPathStatusHasInvalidPath
            else -> EPathStatus.kPathStatusError
        }
    }

    fun getCharacterWidth(): Float = finalPathData.characterWidth

    fun setCharacterWidth(pCharacterWidth: Float) {
        finalPathData.characterWidth = pCharacterWidth
        tempPathData.characterWidth = pCharacterWidth
        computeFinalPath()
    }

    fun getCharacterType(): ECharacterType = characterType

    fun setCharacterType(pCharacterType: ECharacterType) {
        characterType = pCharacterType
        val libCharType = when (pCharacterType) {
            ECharacterType.kCharacterTypeNone -> LLPLCharacterType.LLPL_CHARACTER_TYPE_NONE
            ECharacterType.kCharacterTypeA -> LLPLCharacterType.LLPL_CHARACTER_TYPE_A
            ECharacterType.kCharacterTypeB -> LLPLCharacterType.LLPL_CHARACTER_TYPE_B
            ECharacterType.kCharacterTypeC -> LLPLCharacterType.LLPL_CHARACTER_TYPE_C
            ECharacterType.kCharacterTypeD -> LLPLCharacterType.LLPL_CHARACTER_TYPE_D
        }
        finalPathData.characterType = libCharType
        tempPathData.characterType = libCharType
        computeFinalPath()
    }

    fun isRenderPath(): Boolean = hasFinalA() || hasFinalB() || hasTempA() || hasTempB()

    fun clearPath() {
        clearFinal()
        clearTemp()
        computeFinalPath()
    }

    fun registerPathEventListener(pPathEventCallback: PathEventCallback): PathEventCallback {
        pathEventListeners.add(pPathEventCallback)
        return pPathEventCallback
    }

    private fun isAnyPathToolModKeys(mask: MASK): Boolean = (mask and (MASK_CONTROL or MASK_SHIFT)) != 0

    private fun isPointAModKeys(mask: MASK): Boolean = (mask and MASK_CONTROL) != 0

    private fun isPointBModKeys(mask: MASK): Boolean = (mask and MASK_SHIFT) != 0

    private fun isCameraModKeys(mask: MASK): Boolean = (mask and MASK_ALT) != 0

    private fun getRayPoints(x: Int, y: Int): Pair<Vector3, Vector3> {
        // no-op
        return Pair(Vector3.ZERO, Vector3.ZERO)
    }

    private fun computeFinalPoints(x: Int, y: Int, mask: MASK) {
        val (rayStart, rayEnd) = getRayPoints(x, y)
        when {
            isPointAModKeys(mask) -> setFinalA(rayStart, rayEnd)
            isPointBModKeys(mask) -> setFinalB(rayStart, rayEnd)
        }
        computeFinalPath()
    }

    private fun computeTempPoints(x: Int, y: Int, mask: MASK) {
        val (rayStart, rayEnd) = getRayPoints(x, y)
        when {
            isPointAModKeys(mask) -> {
                setTempA(rayStart, rayEnd)
                if (hasFinalB()) setTempB(getFinalBStart(), getFinalBEnd())
            }
            isPointBModKeys(mask) -> {
                if (hasFinalA()) setTempA(getFinalAStart(), getFinalAEnd())
                setTempB(rayStart, rayEnd)
            }
        }
        computeTempPath()
    }

    private fun setFinalA(start: Vector3, end: Vector3) {
        finalPathData.startPointA = start
        finalPathData.endPointA = end
        finalPathData.hasPointA = true
    }

    private fun hasFinalA(): Boolean = finalPathData.hasPointA

    private fun getFinalAStart(): Vector3 = finalPathData.startPointA

    private fun getFinalAEnd(): Vector3 = finalPathData.endPointA

    private fun setTempA(start: Vector3, end: Vector3) {
        tempPathData.startPointA = start
        tempPathData.endPointA = end
        tempPathData.hasPointA = true
    }

    private fun hasTempA(): Boolean = tempPathData.hasPointA

    private fun setFinalB(start: Vector3, end: Vector3) {
        finalPathData.startPointB = start
        finalPathData.endPointB = end
        finalPathData.hasPointB = true
    }

    private fun hasFinalB(): Boolean = finalPathData.hasPointB

    private fun getFinalBStart(): Vector3 = finalPathData.startPointB

    private fun getFinalBEnd(): Vector3 = finalPathData.endPointB

    private fun setTempB(start: Vector3, end: Vector3) {
        tempPathData.startPointB = start
        tempPathData.endPointB = end
        tempPathData.hasPointB = true
    }

    private fun hasTempB(): Boolean = tempPathData.hasPointB

    private fun clearFinal() {
        finalPathData.hasPointA = false
        finalPathData.hasPointB = false
    }

    private fun clearTemp() {
        tempPathData.hasPointA = false
        tempPathData.hasPointB = false
    }

    private fun computeFinalPath() {
        pathResult = LLPLResult.LLPL_NO_PATH
        // no-op
        pathEventListeners.forEach { it() }
    }

    private fun computeTempPath() {
        pathResult = LLPLResult.LLPL_NO_PATH
        // no-op
        pathEventListeners.forEach { it() }
    }
}

// Mirrors LLPathingLib::PathingPacket — holds the two ray endpoints and character parameters
// for a single path query submitted to the native pathing library.
data class PathingPacket(
    var startPointA: Vector3 = Vector3.ZERO,
    var endPointA: Vector3 = Vector3.ZERO,
    var hasPointA: Boolean = false,
    var startPointB: Vector3 = Vector3.ZERO,
    var endPointB: Vector3 = Vector3.ZERO,
    var hasPointB: Boolean = false,
    var characterWidth: Float = 0f,
    var characterType: LLPLCharacterType = LLPLCharacterType.LLPL_CHARACTER_TYPE_NONE
)

enum class LLPLResult {
    LLPL_NO_PATH,
    LLPL_PATH_GENERATED_OK,
    LLPL_ERROR
}

enum class LLPLCharacterType {
    LLPL_CHARACTER_TYPE_NONE,
    LLPL_CHARACTER_TYPE_A,
    LLPL_CHARACTER_TYPE_B,
    LLPL_CHARACTER_TYPE_C,
    LLPL_CHARACTER_TYPE_D
}

private const val KEY_ESCAPE: KEY = 0x1B
