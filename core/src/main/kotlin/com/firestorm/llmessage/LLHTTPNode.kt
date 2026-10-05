package com.firestorm.llmessage

import com.firestorm.llcommon.LLSD

abstract class HTTPNode {
    private val children: MutableMap<String, HTTPNode> = mutableMapOf()
    private var wildcardChild: HTTPNode? = null
    private var wildcardKey: String? = null

    abstract fun get(context: LLSD): LLSD

    open fun put(context: LLSD, input: LLSD): LLSD = LLSD.Undefined

    open fun post(context: LLSD, input: LLSD): LLSD = LLSD.Undefined

    open fun del(context: LLSD): LLSD = LLSD.Undefined

    open fun options(context: LLSD): LLSD {
        val methods = buildList {
            add("GET")
            if (this@HTTPNode.javaClass.methods.any { it.name == "put" && it.declaringClass != HTTPNode::class.java }) add("PUT")
            if (this@HTTPNode.javaClass.methods.any { it.name == "post" && it.declaringClass != HTTPNode::class.java }) add("POST")
            if (this@HTTPNode.javaClass.methods.any { it.name == "del" && it.declaringClass != HTTPNode::class.java }) add("DELETE")
        }
        return LLSD.fromList(methods.map { LLSD.fromString(it) })
    }

    open fun validate(name: String): Boolean = false

    fun getChild(name: String): HTTPNode? {
        children[name]?.let { return it }
        wildcardChild?.takeIf { it.validate(name) }?.let { return it }
        return null
    }

    fun traverse(path: List<String>): HTTPNode? {
        if (path.isEmpty()) return this
        val child = getChild(path.first()) ?: return null
        return child.traverse(path.drop(1))
    }

    fun addNode(path: String, node: HTTPNode) {
        val parts = path.trimStart('/').split('/')
        addNodeParts(parts, node)
    }

    private fun addNodeParts(parts: List<String>, node: HTTPNode) {
        if (parts.isEmpty()) return
        val head = parts.first()
        val tail = parts.drop(1)
        if (tail.isEmpty()) {
            if (head.startsWith('<') && head.endsWith('>')) {
                wildcardKey = head.removeSurrounding("<", ">")
                wildcardChild = node
            } else {
                children[head] = node
            }
        } else {
            val child = children.getOrPut(head) { object : HTTPNode() {
                override fun get(context: LLSD): LLSD = LLSD.Undefined
            }}
            child.addNodeParts(tail, node)
        }
    }

    fun allNodePaths(): List<String> {
        val result = mutableListOf<String>()
        for ((name, child) in children) {
            result.add(name)
            child.allNodePaths().forEach { result.add("$name/$it") }
        }
        wildcardChild?.let { wc ->
            val key = "<${wildcardKey ?: "wildcard"}>"
            result.add(key)
            wc.allNodePaths().forEach { result.add("$key/$it") }
        }
        return result
    }
}

class HTTPNodeTree : HTTPNode() {

    override fun get(context: LLSD): LLSD = LLSD.Undefined

    fun route(method: String, path: String, body: LLSD = LLSD.Undefined): LLSD {
        val parts = path.trimStart('/').split('/').filter { it.isNotEmpty() }
        val node = traverse(parts) ?: return LLSD.Undefined
        val context = LLSD.fromMap(mapOf(
            "request" to LLSD.fromMap(mapOf("path" to LLSD.fromString(path)))
        ))
        return when (method.uppercase()) {
            "GET"    -> node.get(context)
            "PUT"    -> node.put(context, body)
            "POST"   -> node.post(context, body)
            "DELETE" -> node.del(context)
            "OPTIONS"-> node.options(context)
            else     -> LLSD.Undefined
        }
    }
}
