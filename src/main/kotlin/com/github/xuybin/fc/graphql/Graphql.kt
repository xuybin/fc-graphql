package com.github.xuybin.fc.graphql

import graphql.schema.DataFetcher
import org.slf4j.LoggerFactory

/**
 * Created by xuybin@qq.com  2018/11/1 22:48.
 * 重新封装,简化和避免暴露graphql-java的对象
 */
interface GResolver {
    // 业务解析器 Map(模型对应实现的方法名,调用模型对应实现的方法)
    fun resolverMap(): Map<String, (GContext) -> Any?>

    /**
     * Created by xuybin@qq.com  2018/11/1 22:48.
     * 封装的简化转graphql-java需要的对象
     */
    fun dataFetchersMap(): Map<String, DataFetcher<*>> {
        val dataFetchers = mutableMapOf<String, DataFetcher<*>>()
        this.resolverMap().forEach {
            dataFetchers.put(it.key, DataFetcher<Any?> { dfe ->
                // 做GErr->GErrorExtensions的转换,以屏蔽编译时需引入GraphQLError的问题
                try {
                    val gcontext = GContext.fromDataFetchingEnvironment(dfe)
                    if (this is GSchema) {
                        initContext(gcontext)
                    } else throw GErrExecute(
                        GErrType.UnknownGraphqlSchema,
                        "${this.javaClass.canonicalName} must extends GSchema"
                    )
                    // 调用该解析方法
                    it.value(gcontext.arguments)
                } catch (ex: GErr) {
                    throw ex.toGErrExecute()
                } catch (ex: GErrExecute) {
                    throw ex
                } catch (ex: Throwable) {
                    throw GErrExecute(GErrType.Unknown, ex.message)
                }
            })
        }
        return dataFetchers
    }
}


/**
 * Created by xuybin@qq.com  2018/10/12 18:00.
 * 所有Graphql Query必须实现的接口
 * 实现后记得追加完成的名称到src\main\resources\META-INF\services\com.github.xuybin.fc.graphql.GQuery
 */
interface GQuery : GResolver

/**
 * Created by xuybin@qq.com  2018/10/12 18:00.
 * 所有Graphql Mutation必须实现的接口
 * 实现后记得追加完成的名称到src\main\resources\META-INF\services\com.github.xuybin.fc.graphql.GMutation
 */
interface GMutation : GResolver

/**
 * Created by xuybin@qq.com  2018/10/12 18:00.
 * 所有Graphql Subscription必须实现的接口
 * 实现后记得追加完成的名称到src\main\resources\META-INF\services\com.github.xuybin.fc.graphql.GSubscription
 */
interface GSubscription : GResolver

/**
 * Created by xuybin  2018/11/1 14:12.
 * 所有Graphql Schema必须实现的接口
 * 实现后记得追加完成的名称到src\main\resources\META-INF\services\com.github.xuybin.fc.graphql.GSchema
 */
abstract class GSchema : GResolver {
    private var gcontext: GContext? = null
    private val log = LoggerFactory.getLogger(this@GSchema.javaClass)

    fun initContext(context: GContext) {
        gcontext = context
    }

    val context: GContext by lazy {
        gcontext
            ?: throw GErr.UnknownGraphqlSchema("at ${this.javaClass.canonicalName}.context must initContext befor use.")
    }

    val logger: Logger by lazy {
        object : Logger {
            override fun trace(string: String) {
                if (log.isTraceEnabled) gcontext?.getFLogger()?.trace(string) ?: log.trace(string)
            }

            override fun debug(string: String) {
                if (log.isDebugEnabled) gcontext?.getFLogger()?.debug(string) ?: log.debug(string)
            }

            override fun info(string: String) {
                if (log.isInfoEnabled) gcontext?.getFLogger()?.info(string) ?: log.info(string)
            }

            override fun warn(string: String) {
                if (log.isWarnEnabled) gcontext?.getFLogger()?.warn(string) ?: log.warn(string)
            }

            override fun error(string: String) {
                if (log.isErrorEnabled) gcontext?.getFLogger()?.error(string) ?: log.error(string)
            }
        }
    }

    open fun schema(): String {
        val schemaFile = "/schema/${this.javaClass.simpleName}.graphqls"
        return GSchema::class.java.getResourceAsStream(schemaFile).let {
            it
                ?: throw GErr.UnknownGraphqlSchema("at ${this.javaClass.canonicalName}.${::schema.name} getResourceAsStream $schemaFile is null")
            it.bufferedReader().use { it.readText() }
        }
    }
}
