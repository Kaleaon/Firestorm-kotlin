package com.firestorm.newview

import com.firestorm.agent.Agent
import com.firestorm.app.AppViewer
import com.firestorm.camera.ViewerCamera
import com.firestorm.control.ViewerControl
import com.firestorm.math.Vector3
import com.firestorm.math.Vector3d
import com.firestorm.math.Quaternion
import com.firestorm.math.Matrix3
import com.firestorm.timer.Timer
import kotlin.math.max

class AgentPilot {

    enum class ActionType {
        STRAIGHT,
        TURN,
    }

    private data class Action(
        var type: ActionType = ActionType.STRAIGHT,
        var target: Vector3d = Vector3d(),
        var time: Double = 0.0,
        var cameraView: Float = 0f,
        var cameraOrigin: Vector3 = Vector3(),
        var cameraXAxis: Vector3 = Vector3(),
        var cameraYAxis: Vector3 = Vector3(),
        var cameraZAxis: Vector3 = Vector3(),
    )

    var loop: Boolean = true
    var replaySession: Boolean = false
    var numRuns: Int = -1
    var quitAfterRuns: Boolean = false

    private var recording: Boolean = false
    private var lastRecordTime: Float = 0f
    private var started: Boolean = false
    private var playing: Boolean = false
    private var currentAction: Int = 0
    private var overrideCamera: Boolean = false

    private val actions: MutableList<Action> = mutableListOf()
    private val timer: Timer = Timer()

    fun isRecording(): Boolean = recording
    fun isPlaying(): Boolean = playing
    fun getOverrideCamera(): Boolean = overrideCamera

    fun load() {
        val txtFilename = ViewerControl.getString("StatsPilotFile")
        val xmlFilename = ViewerControl.getString("StatsPilotXMLFile")
        when {
            java.io.File(xmlFilename).isFile -> loadXML(xmlFilename)
            java.io.File(txtFilename).isFile -> loadTxt(txtFilename)
        }
    }

    fun loadTxt(filename: String) {
        if (filename.isEmpty()) return
        val file = java.io.File(filename)
        if (!file.exists()) return

        actions.clear()
        val lines = file.readLines()
        if (lines.isEmpty()) return
        val numActionCount = lines[0].trim().toIntOrNull() ?: return
        for (i in 1..numActionCount) {
            if (i >= lines.size) break
            val parts = lines[i].trim().split("\\s+".toRegex())
            if (parts.size < 5) continue
            val action = Action(
                time = parts[0].toDoubleOrNull() ?: 0.0,
                type = ActionType.entries.getOrElse(parts[1].toIntOrNull() ?: 0) { ActionType.STRAIGHT },
                target = Vector3d(
                    parts[2].toDoubleOrNull() ?: 0.0,
                    parts[3].toDoubleOrNull() ?: 0.0,
                    parts[4].toDoubleOrNull() ?: 0.0,
                ),
            )
            actions.add(action)
        }
        overrideCamera = false
    }

    fun loadXML(filename: String) {
        if (filename.isEmpty()) return
        val file = java.io.File(filename)
        if (!file.exists()) return

        actions.clear()
        TODO("APR: use JVM equivalent for LLSD XML parsing of $filename")
        // Each parsed record should produce an Action and be appended to actions.
        // overrideCamera = true after loading.
    }

    fun save() {
        val txtFilename = ViewerControl.getString("StatsPilotFile")
        val xmlFilename = ViewerControl.getString("StatsPilotXMLFile")
        saveTxt(txtFilename)
        saveXML(xmlFilename)
    }

    fun saveTxt(filename: String) {
        if (filename.isEmpty()) return
        val sb = StringBuilder()
        sb.appendLine(actions.size)
        for (action in actions) {
            sb.append(action.time).append('\t')
                .append(action.type.ordinal).append('\t')
                .append(action.target.x).append('\t')
                .append(action.target.y).append('\t')
                .append(action.target.z).appendLine()
        }
        java.io.File(filename).writeText(sb.toString())
    }

    fun saveXML(filename: String) {
        if (filename.isEmpty()) return
        TODO("APR: use JVM equivalent for LLSD XML serialization to $filename")
    }

    fun startRecord() {
        actions.clear()
        timer.reset()
        addAction(ActionType.STRAIGHT)
        recording = true
    }

    fun stopRecord() {
        addAction(ActionType.STRAIGHT)
        save()
        recording = false
    }

    fun addAction(actionType: ActionType) {
        val cam = ViewerCamera.instance
        val action = Action(
            type = actionType,
            target = Agent.instance.getPositionGlobal(),
            time = timer.getElapsedTimeF32().toDouble(),
            cameraView = cam.getView(),
            cameraOrigin = cam.getOrigin(),
            cameraXAxis = cam.getXAxis(),
            cameraYAxis = cam.getYAxis(),
            cameraZAxis = cam.getZAxis(),
        )
        lastRecordTime = action.time.toFloat()
        actions.add(action)
    }

    fun startPlayback() {
        if (playing) return
        playing = true
        currentAction = 0
        timer.reset()
        if (actions.isNotEmpty()) {
            Agent.instance.startAutoPilotGlobal(actions[0].target)
            moveCamera()
            started = false
        } else {
            playing = false
        }
    }

    fun stopPlayback() {
        if (playing) {
            playing = false
            currentAction = 0
            timer.reset()
            Agent.instance.stopAutoPilot()
        }
        if (replaySession) {
            AppViewer.instance.forceQuit()
        }
    }

    fun addWaypoint() {
        addAction(ActionType.STRAIGHT)
    }

    fun moveCamera() {
        if (!overrideCamera) return
        if (currentAction >= actions.size) return

        val startIndex = max(currentAction - 1, 0)
        val endIndex = currentAction
        val timeDelta = (actions[endIndex].time - actions[startIndex].time).toFloat()
        val tickElapsed = timer.getElapsedTimeF32() - actions[startIndex].time.toFloat()
        val t = if (timeDelta > 0f) tickElapsed / timeDelta else 0f

        if (t < 0f || t > 1f) return

        val start = actions[startIndex]
        val end = actions[endIndex]

        val view = lerp(start.cameraView, end.cameraView, t)
        val origin = lerp(start.cameraOrigin, end.cameraOrigin, t)
        val startQuat = Quaternion(start.cameraXAxis, start.cameraYAxis, start.cameraZAxis)
        val endQuat = Quaternion(end.cameraXAxis, end.cameraYAxis, end.cameraZAxis)
        val quat = nlerp(t, startQuat, endQuat)
        val mat = Matrix3(quat)

        ViewerCamera.instance.setView(view)
        ViewerCamera.instance.setOrigin(origin)
        ViewerCamera.instance.setAxes(mat)
    }

    fun updateTarget() {
        if (playing) {
            if (currentAction < actions.size) {
                if (currentAction == 0) {
                    if (Agent.instance.getAutoPilot()) {
                        return
                    } else {
                        if (!started) {
                            timer.reset()
                            started = true
                        }
                    }
                }
                if (timer.getElapsedTimeF32() > actions[currentAction].time.toFloat()) {
                    currentAction++
                    if (currentAction < actions.size) {
                        Agent.instance.startAutoPilotGlobal(actions[currentAction].target)
                        moveCamera()
                    } else {
                        stopPlayback()
                        numRuns--
                        if (loop) {
                            when {
                                numRuns < 0 || numRuns > 0 -> startPlayback()
                                quitAfterRuns -> AppViewer.instance.forceQuit()
                                else -> stopPlayback()
                            }
                        }
                    }
                }
            } else {
                stopPlayback()
            }
        } else if (recording) {
            if (timer.getElapsedTimeF32() - lastRecordTime > 1f) {
                addAction(ActionType.STRAIGHT)
            }
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
    private fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 = Vector3(
        a.x + (b.x - a.x) * t,
        a.y + (b.y - a.y) * t,
        a.z + (b.z - a.z) * t,
    )
    private fun nlerp(t: Float, a: Quaternion, b: Quaternion): Quaternion = TODO("GPU: nlerp quaternions")

    companion object {
        val instance: AgentPilot = AgentPilot()
    }
}
