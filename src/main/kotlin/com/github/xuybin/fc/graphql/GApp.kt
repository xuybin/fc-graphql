package com.github.xuybin.fc.graphql

import kotlin.reflect.KClass

/**
 * Created by xuybin@qq.com  2018/11/18 14:41.
 * 从Inversion of Control框架获取single Bean
 * 获取GSchema  GQuery GMutation GSubscription的实现类
 */
interface GApp {

    fun init(args: Array<String> =emptyArray())

    fun <T : Any> getBean(clazz: KClass<T>): T

    fun <T : Any> getBean(clazz: KClass<T>, name: String): T

    fun toJson(src: Any?): String

    fun fromJson(queryJson: String): GRequest

    fun getGSchema(): List<GSchema>

    fun getContext(): GContext {
        return GContext.fromGApp(this)
    }

    fun version(): List<String> {
        val versions = mutableListOf<String>()
        try {
            loadProperties("/com/github/xuybin/fc/graphql/defaults.properties").forEach {
                if (it.first.startsWith("version")) versions.add(it.second)
            }
        } catch (ex: Throwable) { }
        try {
            loadProperties("bootstrap.properties").forEach {
                if (it.first.startsWith("version")) versions.add(it.second)
            }
        } catch (ex: Throwable) { }
        try {
            loadProperties("application.properties").forEach {
                if (it.first.startsWith("version")) versions.add(it.second)
            }
        } catch (ex: Throwable) { }
        return versions
    }

    fun serverPort():Int{
        return 8080
    }
}

fun loadProperties(resPath: String): List<Pair<String, String>> {
    val properties = mutableListOf<Pair<String, String>>()
    GRequest::class.java.getResourceAsStream(resPath).bufferedReader().use {
        it.readLines().forEach {
            "(\\S+)[ \\f\\t]*=[ \\f\\t]*(\\S*)[ \\f\\t]*".toRegex().matchEntire(it)?.run {
                if (groupValues.size == 3) {
                    properties.add(Pair(groupValues[1], groupValues[2]))
                }
            }
        }
    }
    return properties
}

class GRequest(
    var query: String = ""
    , var variables: Map<String, Any>? = null
    , var operationName: String? = null
)
