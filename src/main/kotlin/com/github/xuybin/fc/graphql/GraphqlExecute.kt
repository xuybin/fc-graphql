package com.github.xuybin.fc.graphql

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQL
import graphql.schema.DataFetcher
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.errors.SchemaProblem
import java.time.Instant

/**
 * Created by xuybin@qq.com  2018/10/11 09:50.
 * 合并graphql模型文件
 */
fun GApp.initGraphqlSchema(): GraphQLSchema {
    val schemaVersion="""
            type Query {
              # 版本信息
              version: [String!]!
            }
            type Mutation {
              # 版本信息
              version: [String!]!
            }
            type Subscription {
              # 版本信息
              version: [String!]!
            }
            """
    
    val typeRegistry = TypeDefinitionRegistry()

    try {
        val schemaParser = SchemaParser()
        typeRegistry.merge(schemaParser.parse(schemaVersion))

        getGSchema().forEach {
            it.logger.trace("merge schema loader from  ${it}")
            typeRegistry.merge(schemaParser.parse(it.schema()))
        }
    } catch (ex: Throwable) {
        when (ex) {
            is GErr -> throw ex
            is GErrExecute -> throw ex
            else -> throw GErrExecute(
                GErrType.UnknownGraphqlSchema,
                "at ${::initGraphqlSchema.name}  ${ex.message} ${ex.cause}"
            )
        }
    }

    val runtimeWiring = graphql.schema.idl.RuntimeWiring.newRuntimeWiring()
        .type("Query") { builder ->
            try {
                builder.dataFetchers(mutableMapOf<String, DataFetcher<*>>(
                   "version" to  DataFetcher<Any?> {
                            return@DataFetcher version()
                    }
                ))
                getGSchema().filter { it is GQuery }.map {
                    it.logger.trace(
                        "merge runtimeWiring loader from  ${it}"
                    )
                    it as GQuery
                }.forEach {
                    builder.dataFetchers(it.dataFetchersMap())
                }
            } catch (ex: Throwable) {
                // 如发生错误,须检查src\main\resources\META-INF\services的内容
                throw GErrExecute(
                    GErrType.UnknownGraphqlSchema,
                    "at ${::initGraphqlSchema.name} QueryTypeRuntimeWiring ${ex.message}"
                )
            }
            builder
        }
        .type("Mutation") { builder ->
            try {
                builder.dataFetchers(mutableMapOf<String, DataFetcher<*>>(
                    "version" to  DataFetcher<Any?> {
                        return@DataFetcher version()
                    }
                ))
                getGSchema().filter { it is GMutation }.map {
                    it.logger.trace(
                        "merge runtimeWiring loader from  ${it}"
                    )
                    it as GMutation
                }.forEach {
                    builder.dataFetchers(it.dataFetchersMap())
                }
            } catch (ex: Throwable) {
                // 如发生错误,须检查src\main\resources\META-INF\services的内容
                throw GErrExecute(
                    GErrType.UnknownGraphqlSchema,
                    "at ${::initGraphqlSchema.name} GMutationRuntimeWiring ${ex.message}"
                )
            }
            builder
        }
        .type("Subscription") { builder ->
            try {
                builder.dataFetchers(mutableMapOf<String, DataFetcher<*>>(
                    "version" to  DataFetcher<Any?> {
                        return@DataFetcher version()
                    }
                ))
                getGSchema().filter { it is GSubscription }.map {
                    it.logger.trace(
                        "merge runtimeWiring loader from  ${it}"
                    )
                    it as GSubscription
                }.forEach {
                    builder.dataFetchers(it.dataFetchersMap())
                }
            } catch (ex: Throwable) {
                // 如发生错误,须检查src\main\resources\META-INF\services的内容
                throw GErrExecute(
                    GErrType.UnknownGraphqlSchema,
                    "at ${::initGraphqlSchema.name} GSubscriptionRuntimeWiring ${ex.message}"
                )
            }
            builder
        }
        .build()
    return graphql.schema.idl.SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring)
}

/**
 * Created by xuybin@qq.com  2018/10/9 17:21.
 * 执行Graphql入口
 */
fun ExecutionInput.execute(graphQL: GraphQL, extStartInstant: Instant? = null): String {
    return try {
        val inputExtensions = mutableMapOf<String, Any>()
        if (extStartInstant != null) {
            inputExtensions.put(
                "initRequest",
                "init Graphql Request consume ${java.time.Duration.between(
                    extStartInstant,
                    java.time.Instant.now()
                ).toMillis()} ms"
            )
        }
        var startInstant = java.time.Instant.now()

        graphQL.execute(this).let {
            if (extStartInstant != null) {
                inputExtensions.put(
                    "executeGraphql",
                    "execute Graphql consume ${java.time.Duration.between(
                        startInstant,
                        java.time.Instant.now()
                    ).toMillis()} ms"
                )
            }
            it.stringify(inputExtensions, (context as GContext).getGApp()::toJson)
        }
    } catch (ex: SchemaProblem) {
        kotlin.collections.mapOf("errors" to ex.errors.map { error -> error.toMap() }).let {
            (context as GContext).getGApp().toJson(it)
        }
    } catch (error: GErrExecute) {
        kotlin.collections.mapOf("errors" to kotlin.collections.listOf(error.toMap())).let {
            (context as GContext).getGApp().toJson(it)
        }
    } catch (error: GErr) {
        kotlin.collections.mapOf("errors" to kotlin.collections.listOf(error.toMap())).let {
            (context as GContext).getGApp().toJson(it)
        }
    } catch (ex: Throwable) {
        mapOf(
            "errors" to listOf(
                GErrExecute(
                    GErrType.Unknown,
                    "at execute ${ex.message}"
                ).toMap()
            )
        ).let {
            (context as GContext).getGApp().toJson(it)
        }
    }
}

/**
 * Created by xuybin@qq.com  2018/10/10 10:42.
 * ExecutionResult序列化为json字符串
 */
fun ExecutionResult.stringify(inputExtensions: Map<String, Any>? = null, toJson: (src: Any?) -> String): String {
    return StringBuilder().also {
        it.append("{")
        it.append("\"data\":${toJson(this.getData<Any?>())}")
        //  this.data!=null && ( this.errors==null || this.errors.size==0) 不输出errors
        if (this.getData<Any?>() == null || (this.errors != null && this.errors!!.size > 0)) {
            it.append(",\"errors\":${toJson(this.errors?.map { error -> error.toMap() })}")
        }
        if (inputExtensions != null) {
            if (this.extensions != null) {
                it.append(",\"extensions\":${toJson(mutableMapOf<String, Any>().also { newMap ->
                    // 追加Graphql运行时传递的扩展
                    this.extensions.forEach {
                        newMap.put("${it.key}", it.value)
                    }
                    // 追加外部业务传递的扩展信息
                    inputExtensions.forEach {
                        newMap.put(it.key, it.value)
                    }
                })}")
            } else it.append(",\"extensions\":${toJson(inputExtensions)}")
        } else if (this.extensions != null) it.append(",\"extensions\":${toJson(this.extensions)}")
        it.append("}")
    }.toString()
}