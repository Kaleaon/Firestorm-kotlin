package com.firestorm.llcorehttp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LLCoreHttpTest {

    @Test
    fun testLLHttpRequestBuildersAndFactories() {
        val reqGet = LLHttpRequest.get("https://example.com/api/test")
        assertEquals(HttpMethod.GET, reqGet.method)
        assertEquals("https://example.com/api/test", reqGet.url.toString())

        val reqPost = LLHttpRequest.post("https://example.com/api/post", "hello".toByteArray())
        assertEquals(HttpMethod.POST, reqPost.method)
        assertEquals("hello", String(reqPost.body!!))

        val reqPut = LLHttpRequest.put("https://example.com/api/put")
        assertEquals(HttpMethod.PUT, reqPut.method)

        val reqDelete = LLHttpRequest.delete("https://example.com/api/delete")
        assertEquals(HttpMethod.DELETE, reqDelete.method)

        val reqPatch = LLHttpRequest.patch("https://example.com/api/patch")
        assertEquals(HttpMethod.PATCH, reqPatch.method)

        val reqHead = LLHttpRequest.head("https://example.com/api/head")
        assertEquals(HttpMethod.HEAD, reqHead.method)

        val reqOptions = LLHttpRequest.options("https://example.com/api/options")
        assertEquals(HttpMethod.OPTIONS, reqOptions.method)
    }

    @Test
    fun testLLHttpRequestFluentSetters() {
        val req = LLHttpRequest.get("https://example.com/data")
            .setHeader("X-Custom-Header", "Value123")
            .setJsonBody("""{"key":"value"}""")
            .setByteRange(100L, 500L)

        assertEquals("Value123", req.headers.find("X-Custom-Header"))
        assertEquals("application/json", req.headers.find("Content-Type"))
        assertEquals("bytes=100-599", req.headers.find("Range"))
        assertEquals("""{"key":"value"}""", String(req.body!!))
    }

    @Test
    fun testLLHttpHeadersCaseInsensitiveAndMultiValues() {
        val headers = LLHttpHeaders()
        headers.append("Accept", "application/json")
        headers.append("accept", "text/plain")

        assertTrue(headers.contains("ACCEPT"))
        assertEquals("application/json", headers.find("accept"))
        assertEquals(listOf("application/json", "text/plain"), headers.findAll("Accept"))

        headers.set("Authorization", "Bearer token123")
        assertEquals("Bearer token123", headers.find("authorization"))

        val map = headers.toMap()
        assertEquals("application/json, text/plain", map["Accept"])

        headers.remove("ACCEPT")
        assertFalse(headers.contains("Accept"))
        assertNull(headers.find("Accept"))

        headers.clear()
        assertTrue(headers.isEmpty())
    }

    @Test
    fun testLLHttpResponseStatusAndHelpers() {
        val headers = LLHttpHeaders()
        headers.set("Content-Type", "text/html; charset=utf-8")
        headers.set("Content-Length", "12")

        val body = "Hello World!".toByteArray()
        val response = LLHttpResponse(status = 200, headers = headers, body = body)

        assertTrue(response.isSuccess)
        assertFalse(response.isRedirect)
        assertFalse(response.isClientError)
        assertFalse(response.isServerError)

        assertEquals("text/html; charset=utf-8", response.contentType())
        assertEquals(12L, response.contentLength())
        assertEquals("Hello World!", response.bodyAsString())

        val errResp = LLHttpResponse(status = 404)
        assertTrue(errResp.isClientError)
        assertFalse(errResp.isSuccess)

        val serverErrResp = LLHttpResponse(status = 500)
        assertTrue(serverErrResp.isServerError)

        val redirectResp = LLHttpResponse(status = 302)
        assertTrue(redirectResp.isRedirect)
    }

    @Test
    fun testLLCoreHttpHandlerTypealias() {
        var handledResponse: LLHttpResponse? = null
        var handledError: Throwable? = null

        val handler: HttpHandler = { resp, err ->
            handledResponse = resp
            handledError = err
        }

        val dummyResp = LLHttpResponse(200)
        handler(dummyResp, null)

        assertNotNull(handledResponse)
        assertEquals(200, handledResponse?.status)
        assertNull(handledError)
    }
}
