package com.firestorm.llplugin

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

abstract class LLPluginProcessParentOwner {
    abstract fun receivePluginMessage(message: LLPluginMessage)
    open fun receivePluginMessageEarly(message: LLPluginMessage): Boolean = false
    open fun pluginLaunchFailed() {}
    open fun pluginDied() {}
}

/**
 * LLPluginProcessParent manages the parent side of the out-of-process plugin protocol.
 *
 * Platform notes:
 *  - Socket/APR poll machinery → TODO stubs (use java.net or Netty)
 *  - Shared memory regions → TODO stubs (use MappedByteBuffer / memfd_create via JNI)
 *  - Process launching → TODO stubs (use ProcessBuilder)
 *  - Read thread → TODO stubs (use Kotlin coroutines / Executor)
 */
class LLPluginProcessParent private constructor(private var owner: LLPluginProcessParentOwner?) {

    enum class EState {
        STATE_UNINITIALIZED,
        STATE_INITIALIZED,
        STATE_LISTENING,
        STATE_LAUNCHED,
        STATE_CONNECTED,
        STATE_HELLO,
        STATE_LOADING,
        STATE_RUNNING,
        STATE_GOODBYE,
        STATE_LAUNCH_FAILURE,
        STATE_ERROR,
        STATE_CLEANUP,
        STATE_EXITING,
        STATE_DONE,
    }

    private var state: EState = EState.STATE_UNINITIALIZED

    private var sleepTime: Double = 0.0
    private var cpuUsage: Double = 0.0
    var isBlocked: Boolean = false
        private set
    private var disableTimeout: Boolean = false
    private var debug: Boolean = false
    private var polledInput: Boolean = false

    private var pluginFile: String = ""
    private var pluginDir: String = ""
    private var pluginVersionString: String = ""

    private var pluginLaunchTimeout: Float = 60.0f
    private var pluginLockupTimeout: Float = 15.0f

    private val sharedMemoryRegions: MutableMap<String, SharedMemoryStub> = mutableMapOf()
    private val messageClassVersions: MutableMap<String, String> = mutableMapOf()

    private val incomingQueue: LinkedBlockingQueue<LLPluginMessage> = LinkedBlockingQueue()

    private var boundPort: Int = 0
    private var portToBind: Int = 0
    private var bindRetries: Int = 0

    fun getSleepTime(): Double = sleepTime
    fun getCPUUsage(): Double = cpuUsage
    fun getDisableTimeout(): Boolean = disableTimeout
    fun setDisableTimeout(disable: Boolean) { disableTimeout = disable }
    fun setLaunchTimeout(timeout: Float) { pluginLaunchTimeout = timeout }
    fun setLockupTimeout(timeout: Float) { pluginLockupTimeout = timeout }

    fun init(launcherFilename: String, pluginDir: String, pluginFilename: String, debug: Boolean) {
        this.pluginFile = pluginFilename
        this.pluginDir = pluginDir
        this.cpuUsage = 0.0
        this.debug = debug
        portToBind = 0
        bindRetries = 0
        setState(EState.STATE_INITIALIZED)
    }

    fun isLoading(): Boolean = state <= EState.STATE_LOADING
    fun isRunning(): Boolean = state == EState.STATE_RUNNING
    fun isDone(): Boolean = state == EState.STATE_DONE

    fun requestShutdown() {
        setState(EState.STATE_GOODBYE)
        owner = null
        idle()
        removeFromProcessing()
    }

    fun killSockets() {
        System.err.println("LLPluginProcessParent: killSockets not yet implemented")
    }

    fun errorState() {
        if (state < EState.STATE_RUNNING)
            setState(EState.STATE_LAUNCH_FAILURE)
        else
            setState(EState.STATE_ERROR)
    }

    fun setSleepTime(sleepTime: Double, forceSend: Boolean = false) {
        if (forceSend || sleepTime != this.sleepTime) {
            this.sleepTime = sleepTime
            if (isRunning()) {
                val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_INTERNAL, "sleep_time")
                msg.setValueReal("time", sleepTime)
                sendMessage(msg)
            }
        }
    }

    fun sendMessage(message: LLPluginMessage) {
        if (message.hasValue("blocking_response")) {
            isBlocked = false
        }
        val buffer = message.generate()
        System.err.println("LLPluginProcessParent: sendMessage not yet implemented")
    }

    fun receiveMessageRaw(message: String) {
        val parsed = LLPluginMessage()
        if (parsed.parse(message) >= 0) {
            if (parsed.hasValue("blocking_request")) {
                isBlocked = true
            }
            if (polledInput) {
                receiveMessageEarly(parsed)
            } else {
                receiveMessage(parsed)
            }
        }
    }

    fun receiveMessageEarly(message: LLPluginMessage) {
        val handled = if (message.getClass() != LLPLUGIN_MESSAGE_CLASS_INTERNAL) {
            owner?.receivePluginMessageEarly(message) ?: false
        } else false

        if (!handled) {
            incomingQueue.add(message)
        }
    }

    fun receiveMessage(message: LLPluginMessage) {
        val messageClass = message.getClass()
        if (messageClass == LLPLUGIN_MESSAGE_CLASS_INTERNAL) {
            val messageName = message.getName()
            when (messageName) {
                "hello" -> {
                    if (state == EState.STATE_CONNECTED) {
                        setState(EState.STATE_HELLO)
                    } else {
                        errorState()
                    }
                }
                "load_plugin_response" -> {
                    if (state == EState.STATE_LOADING) {
                        pluginVersionString = message.getValue("plugin_version")
                        val versionsLlsd = message.getValueLLSD("versions")
                        if (versionsLlsd is Map<*, *>) {
                            @Suppress("UNCHECKED_CAST")
                            (versionsLlsd as Map<String, Any>).forEach { (k, v) ->
                                messageClassVersions[k] = v.toString()
                            }
                        }
                        check(sleepTime != 0.0) { "sleepTime must be set before STATE_RUNNING" }
                        setSleepTime(sleepTime, forceSend = true)
                        setState(EState.STATE_RUNNING)
                    } else {
                        errorState()
                    }
                }
                "heartbeat" -> {
                    cpuUsage = message.getValueReal("cpu_usage")
                }
                "shm_add_response" -> { /* nothing to do */ }
                "shm_remove_response" -> {
                    val name = message.getValue("name")
                    sharedMemoryRegions.remove(name)?.destroy()
                }
            }
        } else {
            owner?.receivePluginMessage(message)
        }
    }

    fun idle() {
        var idleAgain: Boolean
        do {
            while (incomingQueue.isNotEmpty()) {
                val msg = incomingQueue.poll() ?: break
                receiveMessage(msg)
            }

            idleAgain = false
            when (state) {
                EState.STATE_UNINITIALIZED -> {}
                EState.STATE_INITIALIZED -> {
                    System.err.println("LLPluginProcessParent: STATE_INITIALIZED socket creation not yet implemented")
                }
                EState.STATE_LISTENING -> {
                    System.err.println("LLPluginProcessParent: STATE_LISTENING subprocess spawn not yet implemented")
                }
                EState.STATE_LAUNCHED -> {
                    System.err.println("LLPluginProcessParent: STATE_LAUNCHED connection accept not yet implemented")
                }
                EState.STATE_CONNECTED -> {
                    System.err.println("LLPluginProcessParent: STATE_CONNECTED hello wait not yet implemented")
                }
                EState.STATE_HELLO -> {
                    val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_INTERNAL, "load_plugin")
                    msg.setValue("file", pluginFile)
                    msg.setValue("dir", pluginDir)
                    sendMessage(msg)
                    setState(EState.STATE_LOADING)
                }
                EState.STATE_LOADING -> {
                    System.err.println("LLPluginProcessParent: STATE_LOADING response wait not yet implemented")
                }
                EState.STATE_RUNNING -> {
                    System.err.println("LLPluginProcessParent: STATE_RUNNING socket I/O pump not yet implemented")
                }
                EState.STATE_GOODBYE -> {
                    val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_INTERNAL, "shutdown_plugin")
                    sendMessage(msg)
                    setState(EState.STATE_EXITING)
                }
                EState.STATE_EXITING -> {
                    System.err.println("LLPluginProcessParent: STATE_EXITING process exit check not yet implemented")
                }
                EState.STATE_LAUNCH_FAILURE -> {
                    owner?.pluginLaunchFailed()
                    setState(EState.STATE_CLEANUP)
                }
                EState.STATE_ERROR -> {
                    owner?.pluginDied()
                    setState(EState.STATE_CLEANUP)
                }
                EState.STATE_CLEANUP -> {
                    System.err.println("LLPluginProcessParent: STATE_CLEANUP process/socket/shm teardown not yet implemented")
                    setState(EState.STATE_DONE)
                }
                EState.STATE_DONE -> {}
            }
        } while (idleAgain)
    }

    fun addSharedMemory(size: Long): String {
        System.err.println("LLPluginProcessParent: addSharedMemory not yet implemented")
        return ""
    }

    fun removeSharedMemory(name: String) {
        if (sharedMemoryRegions.containsKey(name)) {
            val msg = LLPluginMessage(LLPLUGIN_MESSAGE_CLASS_INTERNAL, "shm_remove")
            msg.setValue("name", name)
            sendMessage(msg)
        }
    }

    fun getSharedMemorySize(name: String): Long =
        sharedMemoryRegions[name]?.size ?: 0L

    fun getSharedMemoryAddress(name: String): ByteArray? =
        sharedMemoryRegions[name]?.address

    fun getMessageClassVersion(messageClass: String): String =
        messageClassVersions[messageClass] ?: ""

    fun getPluginVersion(): String = pluginVersionString

    private fun setState(newState: EState) {
        state = newState
    }

    private fun removeFromProcessing() {
        synchronized(instancesMutex) {
            instances.remove(this)
        }
    }

    private inner class SharedMemoryStub(val size: Long) {
        var address: ByteArray? = ByteArray(size.toInt())
        fun destroy() { address = null }
    }

    companion object {
        const val LLPLUGIN_MESSAGE_CLASS_INTERNAL = "internal"

        private val instancesMutex = Any()
        private val instances: MutableSet<LLPluginProcessParent> = ConcurrentHashMap.newKeySet()

        private var useReadThread: Boolean = false
        private var pollsetNeedsRebuild: Boolean = false

        fun create(owner: LLPluginProcessParentOwner): LLPluginProcessParent {
            val instance = LLPluginProcessParent(owner)
            synchronized(instancesMutex) { instances.add(instance) }
            return instance
        }

        fun shutdown() {
            synchronized(instancesMutex) {
                for (inst in instances) {
                    val s = inst.state
                    if (s != EState.STATE_CLEANUP && s != EState.STATE_EXITING
                        && s != EState.STATE_DONE && s != EState.STATE_ERROR) {
                        inst.setState(EState.STATE_GOODBYE)
                        inst.owner = null
                    }
                    if (s != EState.STATE_DONE) {
                        inst.idle()
                    }
                }
                instances.clear()
            }
        }

        fun poll(timeout: Double) {
            System.err.println("LLPluginProcessParent: poll not yet implemented")
        }

        fun canPollThreadRun(): Boolean = pollsetNeedsRebuild || useReadThread

        fun setUseReadThread(useReadThread: Boolean) {
            this.useReadThread = useReadThread
            if (useReadThread) {
                System.err.println("LLPluginProcessParent: setUseReadThread background thread not yet implemented")
            }
        }

        fun getUseReadThread(): Boolean = useReadThread
    }
}

private const val LLPLUGIN_MESSAGE_CLASS_INTERNAL = "internal"
