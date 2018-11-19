package com.github.xuybin.fc.graphql

import graphql.ErrorType
import graphql.GraphQLError
import graphql.language.SourceLocation
import java.util.LinkedHashMap

/**
 * Created by xuybin@qq.com  2018/10/9 11:58.
 * 内部使用的桥接异常
 * 以利用graphql.ExceptionWhileDataFetching.mkExtensions
 * 把数据提取异常(ErrorType.DataFetchingException)
 * 拆分为更详细的业务错误 GErrType
 */
internal class GErrExecute(private val errorType_: GErrType, @JvmField override var message: String?) : GraphQLError,
    Throwable(message) {
    companion object {
        const val ERRORTYPE = "errorType"
    }

    override fun getMessage(): String? {
        return message
    }

    override fun getErrorType(): ErrorType {
        return ErrorType.DataFetchingException
    }

    override fun getLocations(): MutableList<SourceLocation> {
        return mutableListOf()
    }

    override fun getExtensions(): Map<String, Any>? {
        return mapOf(ERRORTYPE to errorType_.name)
    }
}

/**
 * Created by xuybin@qq.com  2018/10/9 11:58.
 * 做GError->GErrorExtensions的转换,以屏蔽编译时需引入GraphQLError的问题
 */
internal fun GErr.toGErrExecute(): GErrExecute {
    return GErrExecute(this.errorType, this.message)
}

/**
 * Created by xuybin@qq.com  2018/10/9 17:20.
 * GraphQLError按照规范转成map
 */
internal fun GErr.toMap(): Map<String, Any> {
    return this.toGErrExecute().toMap()
}

/**
 * Created by xuybin@qq.com  2018/10/9 17:20.
 * GraphQLError按照规范转成map
 */
internal fun GraphQLError.toMap(): Map<String, Any> {
    val error = this
    return LinkedHashMap<String, Any>().apply {
        put("message", error.message)
        if (error.locations != null) {
            put("locations", error.locations.map {
                val map = LinkedHashMap<String, Int>()
                map["line"] = it.getLine()
                map["column"] = it.getColumn()
                map
            })
        }
        if (error.path != null) {
            put("path", error.path)
        }
        if (error.extensions != null) {
            if (error.extensions.containsKey(GErrExecute.ERRORTYPE)) {
                // 已经提取到业务错误类型GraphqlErrorType(详见graphql.ExceptionWhileDataFetching.mkExtensions)
                put("extensions", error.extensions)
            } else {
                // 未获取到业务错误类型，则使用GraphQLError.ErrorType
                put("extensions", error.extensions.toMutableMap().apply {
                    put(GErrExecute.ERRORTYPE, error.errorType.name)
                })
            }
        } else put("extensions", mapOf(GErrExecute.ERRORTYPE to error.errorType.name))
    }
}
