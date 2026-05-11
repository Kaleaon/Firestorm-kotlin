package com.firestorm.llcorehttp

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest as JHttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture

typealias HttpHandler = (response: LLHttpResponse?, error: Throwable?) -> Unit

object LLCoreHttp {
    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun request(req: LLHttpRequest, handler: HttpHandler): CompletableFuture<Void> {
        val builder = JHttpRequest.newBuilder()
            .uri(URI.create(req.url.toString()))
            .timeout(Duration.ofMillis(req.timeoutMs))

        req.headers.toMap().forEach { (k, v) -> builder.header(k, v) }

        val bodyPublisher = req.body?.let {
            JHttpRequest.BodyPublishers.ofByteArray(it)
        } ?: JHttpRequest.BodyPublishers.noBody()

        when (req.method) {
            HttpMethod.GET     -> builder.GET()
            HttpMethod.POST    -> builder.POST(bodyPublisher)
            HttpMethod.PUT     -> builder.PUT(bodyPublisher)
            HttpMethod.DELETE  -> builder.DELETE()
            HttpMethod.PATCH   -> builder.method("PATCH", bodyPublisher)
            HttpMethod.HEAD    -> builder.method("HEAD", JHttpRequest.BodyPublishers.noBody())
            HttpMethod.OPTIONS -> builder.method("OPTIONS", JHttpRequest.BodyPublishers.noBody())
        }

        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
            .thenAccept { resp ->
                val responseHeaders = LLHttpHeaders()
                resp.headers().map().forEach { (k, vs) -> vs.forEach { v -> responseHeaders.append(k, v) } }
                handler(LLHttpResponse(resp.statusCode(), responseHeaders, resp.body()), null)
            }
            .exceptionally { err -> handler(null, err); null }
    }

    fun requestSync(req: LLHttpRequest): LLHttpResponse {
        val builder = JHttpRequest.newBuilder()
            .uri(URI.create(req.url.toString()))
            .timeout(Duration.ofMillis(req.timeoutMs))

        req.headers.toMap().forEach { (k, v) -> builder.header(k, v) }

        val bodyPublisher = req.body?.let {
            JHttpRequest.BodyPublishers.ofByteArray(it)
        } ?: JHttpRequest.BodyPublishers.noBody()

        when (req.method) {
            HttpMethod.GET     -> builder.GET()
            HttpMethod.POST    -> builder.POST(bodyPublisher)
            HttpMethod.PUT     -> builder.PUT(bodyPublisher)
            HttpMethod.DELETE  -> builder.DELETE()
            else               -> builder.method(req.method.name, bodyPublisher)
        }

        val resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
        val responseHeaders = LLHttpHeaders()
        resp.headers().map().forEach { (k, vs) -> vs.forEach { v -> responseHeaders.append(k, v) } }
        return LLHttpResponse(resp.statusCode(), responseHeaders, resp.body())
    }
}
