package com.github.xuybin.fc.graphql

import com.aliyun.fc.runtime.FunctionComputeLogger
import graphql.ExecutionInput
import graphql.schema.DataFetchingEnvironment
import java.lang.reflect.Type

/**
 * Created by xuybin@qq.com  2018/11/15 9:41.
 * 封装上下文的接口
 */
class GContext {
    /**
     * Created by xuybin@qq.com  2018/11/15 9:41.
     * 封装函数计算传递和配置读取的认证信息
     */
    class Token(
        var accessKeyId: String = "",
        var accessKeySecret: String = "",
        var securityToken: String? = null
    )
    // 请求的唯一标识
    var requestId: String = ""
        private set

    // 从函数计算传递或配置读取认证信息
    var token: Token? = null
        private set

    // 从FunctionComputeLogger获取日志
    private var fLog: FunctionComputeLogger? = null

    fun getFLogger(): Logger? {
        return fLog?.let {
            object : Logger {
                override fun trace(string: String) {
                    it.trace(string)
                }

                override fun debug(string: String) {
                    it.debug(string)
                }

                override fun info(string: String) {
                    it.info(string)
                }

                override fun warn(string: String) {
                    it.warn(string)
                }

                override fun error(string: String) {
                    it.error(string)
                }
            }
        }
    }

    fun fillFromFContext(fContext: com.aliyun.fc.runtime.Context): GContext {
        token = fContext.executionCredentials.let {
            Token(it.accessKeyId, it.accessKeySecret, it.securityToken)
        }
        requestId = fContext.requestId
        fLog = fContext.logger
        return this
    }

    // 参数名称和值的map
    public var arguments: Map<String, Any?> = emptyMap()
    // 参数的定义顺序List
    public var argumentNames: List<String> = emptyList()

    // 从DataFetchingEnvironment通过顺序获取参数值
    inline operator fun <reified T : Any?> get(index: Int): T {
        val arg= arguments.get(argumentNames[index])
        return when(T::class.java){
            Int::class.java,
            Boolean::class.java,
            String::class.java,
            Float::class.java,
            Double::class.java->arg as T
            else-> {
                if (arg==null) arg as T
                else {
                    val jsonStr=getGApp().toJson(arg)
                    getGApp().fromJson(jsonStr,T::class.java)
                }
            }
        }
    }

    // T本身是泛型试,需要在外部传递构造和传递TypeToken<List<Object>>(){}.getType()
    operator fun <T : Any?> get(index: Int,typeOfT: Type): T {
        val arg= arguments.get(argumentNames[index])
        return getGApp().fromJson(getGApp().toJson(arg),typeOfT)
    }

    private var appContext: GApp? = null
    fun getGApp(): GApp {
        return appContext ?: throw GErrExecute(GErrType.UnknownGraphqlSchema, "GApp must initialization")
    }

    companion object {
        private val instance by lazy {
            GContext()
        }

        fun fromGApp(appContext: GApp): GContext {
            instance.appContext = appContext
            return instance
        }

        fun fromDataFetchingEnvironment(dfe: DataFetchingEnvironment): GContext {
            return dfe.getContext<GContext>().also {
                it.arguments = dfe.arguments
                it.argumentNames = dfe.fieldDefinition.arguments.map { it.name }
                if (it.requestId.isEmpty()) it.requestId = dfe.executionId.toString()
            }
        }
    }

    // build ExecutionInput
    fun getExecutionInput(queryJson: String): ExecutionInput {
        return getGApp().fromJson<GRequest>(queryJson,GRequest::class.java).let {
            ExecutionInput.newExecutionInput().query(it.query)
                .operationName(it.operationName)
                .context(this)
                .variables(it.variables)
                .build()
        }
    }

}


/**
 * Created by xuybin@qq.com  2018/11/15 9:42.
 * 封装函数计算传递和org.slf4j.LoggerFactory初始的日志接口
 */
interface Logger {
    fun trace(string: String)

    fun debug(string: String)

    fun info(string: String)

    fun warn(string: String)

    fun error(string: String)

    enum class Level {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    }
}
