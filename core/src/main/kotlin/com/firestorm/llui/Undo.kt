package com.firestorm.llui

abstract class UndoAction {
    internal var clusterId: Int = 0

    abstract fun undo()
    abstract fun redo()
    open fun cleanup() {}
}

class UndoBuffer(private val factory: () -> UndoAction, initialCount: Int) {
    private val actions: Array<UndoAction> = Array(initialCount) {
        factory() ?: error("Unable to create action for undo buffer")
    }
    private val numActions: Int = initialCount
    private var nextAction: Int = 0
    private var lastAction: Int = 0
    private var firstAction: Int = 0
    private var operationId: Int = 0

    fun getNextAction(setClusterBegin: Boolean = true): UndoAction {
        val action = actions[nextAction]

        if (setClusterBegin) {
            operationId++
        }
        actions[nextAction].clusterId = operationId

        nextAction = (nextAction + 1) % numActions
        lastAction = nextAction

        if (nextAction == firstAction) {
            actions[firstAction].cleanup()
            firstAction = (firstAction + 1) % numActions
        }

        return action
    }

    fun canUndo(): Boolean = nextAction != firstAction
    fun canRedo(): Boolean = nextAction != lastAction

    fun undoAction(): Boolean {
        if (!canUndo()) return false

        var prevAction = (nextAction + numActions - 1) % numActions

        while (actions[prevAction].clusterId == operationId) {
            nextAction = prevAction
            actions[nextAction].undo()

            if (nextAction == firstAction) {
                operationId--
                return false
            }

            prevAction = (nextAction + numActions - 1) % numActions
        }

        operationId--
        return true
    }

    fun redoAction(): Boolean {
        if (!canRedo()) return false

        operationId++

        while (actions[nextAction].clusterId == operationId) {
            if (nextAction == lastAction) return false

            actions[nextAction].redo()
            nextAction = (nextAction + 1) % numActions
        }

        return true
    }

    fun flushActions() {
        for (action in actions) action.cleanup()
        nextAction = 0
        lastAction = 0
        firstAction = 0
        operationId = 0
    }
}
