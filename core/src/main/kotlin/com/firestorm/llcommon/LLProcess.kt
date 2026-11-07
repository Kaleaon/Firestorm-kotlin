package com.firestorm.llcommon

import java.io.InputStream
import java.io.OutputStream

class LLProcess private constructor(private val process: Process) {

    data class Params(
        val executable: String,
        val args: List<String> = emptyList(),
        val cwd: String = "",
        val env: Map<String, String> = emptyMap()
    )

    enum class FILESLOT { STDIN, STDOUT, STDERR }

    enum class State { UNSTARTED, RUNNING, EXITED, KILLED }

    data class Status(val state: State = State.UNSTARTED, val data: Int = 0)

    companion object {
        fun create(params: Params): LLProcess? {
            return try {
                val cmd = buildList {
                    add(params.executable)
                    addAll(params.args)
                }
                val builder = ProcessBuilder(cmd)
                if (params.cwd.isNotEmpty()) {
                    builder.directory(java.io.File(params.cwd))
                }
                if (params.env.isNotEmpty()) {
                    builder.environment().putAll(params.env)
                }
                LLProcess(builder.start())
            } catch (_: Exception) {
                null
            }
        }
    }

    fun isRunning(): Boolean = process.isAlive

    fun getProcessID(): Long = process.pid()

    fun kill(graceful: Boolean = true) {
        if (graceful) process.destroy() else process.destroyForcibly()
    }

    fun getReadPipe(): InputStream? = process.inputStream

    fun getWritePipe(): OutputStream? = process.outputStream

    fun getErrorPipe(): InputStream? = process.errorStream

    fun getStatus(): Status {
        if (process.isAlive) return Status(State.RUNNING)
        val exit = process.exitValue()
        return if (exit == 0 || exit > 0) Status(State.EXITED, exit)
        else Status(State.KILLED, exit)
    }

    fun waitFor(): Status {
        process.waitFor()
        return getStatus()
    }
}
