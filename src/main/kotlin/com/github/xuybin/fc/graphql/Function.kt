package com.github.xuybin.fc.graphql

import com.aliyun.fc.runtime.Context
import com.aliyun.fc.runtime.FunctionInitializer
import java.io.IOException
import com.aliyun.fc.runtime.StreamRequestHandler
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import graphql.GraphQL
import graphql.execution.preparsed.PreparsedDocumentEntry
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.*


class Function : StreamRequestHandler, FunctionInitializer {
    lateinit var appContext: GApp
    lateinit var graphQL: GraphQL

    @Throws(IOException::class)
    override fun initialize(context: Context) {
        try {
            appContext = ServiceLoader.load(GApp::class.java).first().also { it.init() }
            // 缓存1000次不同的query
            val cache: Cache<String, PreparsedDocumentEntry> = Caffeine.newBuilder().maximumSize(1000).build()
            graphQL = GraphQL.newGraphQL(appContext.initGraphqlSchema())
                .preparsedDocumentProvider(cache::get)
                .build()
        } catch (e: Exception) {
            throw IOException(e.message)
        }
    }


    @Throws(IOException::class)
    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        var query = ""
        try {
            val startInstant = Instant.now()
            query = input.bufferedReader().use { it.readText() }
            output.write(
                appContext.getContext().fillFromFContext(context)
                    .getExecutionInput(query).execute(graphQL, startInstant).toByteArray()
            )
        } catch (ex: Exception) {
            "${ex.message} for input $query".let { message ->
                context.logger.error(message)
                output.write(
                    mapOf(
                        "errors" to listOf(
                            GErrExecute(
                                GErrType.UnknownSerializable,
                                "at ${Function::handleRequest.name} $message"
                            ).toMap()
                        )
                    ).let {
                        appContext.toJson(it)
                    }.toByteArray()
                )
            }
        }
    }

}