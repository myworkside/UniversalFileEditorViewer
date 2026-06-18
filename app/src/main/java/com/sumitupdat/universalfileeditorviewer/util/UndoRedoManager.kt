package com.sumitupdat.universalfileeditorviewer.util

import java.util.*

class UndoRedoManager<T>(private val maxStackSize: Int = 50) {
    private val undoStack = Stack<T>()
    private val redoStack = Stack<T>()

    fun push(state: T) {
        if (undoStack.isNotEmpty() && undoStack.peek() == state) return
        undoStack.push(state)
        if (undoStack.size > maxStackSize) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun undo(currentState: T): T? {
        if (undoStack.isEmpty()) return null
        redoStack.push(currentState)
        return undoStack.pop()
    }

    fun redo(currentState: T): T? {
        if (redoStack.isEmpty()) return null
        undoStack.push(currentState)
        return redoStack.pop()
    }

    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()
}
