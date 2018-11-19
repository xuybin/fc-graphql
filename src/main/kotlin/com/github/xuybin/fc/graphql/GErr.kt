package com.github.xuybin.fc.graphql

/**
 * Created by xuybin@qq.com  2018/10/9 11:57.
 * 业务错误类型
 * 利用graphql.ExceptionWhileDataFetching.mkExtensions
 * 把数据提取异常(ErrorType.DataFetchingException)
 * 拆分为更详细的业务错误
 */
enum class GErrType {
    // 未身份认证(需登陆)
    Unauthenticated,
    // 未授权(需提示权限不足)
    Unauthorized,
    // 无效参数
    InvalidParam,
    // 模型定义返回带!,不能为null未找到则报此异常
    NotFound,
    // 后端配置错误
    BackendConfigError,
    // DB存储异常(超时,主键或唯一约束冲突等)
    StorageException,
    // RestTemplate调用API异常
    RestApiException,
    // 未知错误(运行中不应出现,如出现，需改正后重新部署)
    Unknown,
    // 未知的序列化异常(运行中不应出现,如出现，需改正后重新部署)
    UnknownSerializable,
    // 未知的模型异常(运行中不应出现,如出现，需改正后重新部署)
    UnknownGraphqlSchema
}

/**
 * Created by xuybin@qq.com  2018/10/9 11:58.
 * 业务异常(必须指定业务错误类型)
 */

class GErr(val errorType: GErrType, @JvmField override var message: String?) : Throwable(message) {
    companion object {
        fun Unauthorized(msg: String): GErr {
            return GErr(GErrType.Unauthorized, msg)
        }

        fun Unauthenticated(msg: String): GErr {
            return GErr(GErrType.Unauthenticated, msg)
        }

        fun InvalidParam(msg: String): GErr {
            return GErr(GErrType.InvalidParam, msg)
        }

        fun NotFound(msg: String): GErr {
            return GErr(GErrType.NotFound, msg)
        }

        fun BackendConfigError(msg: String): GErr {
            return GErr(GErrType.BackendConfigError, msg)
        }

        fun StorageException(msg: String): GErr {
            return GErr(GErrType.StorageException, msg)
        }

        fun RestApiException(msg: String): GErr {
            return GErr(GErrType.RestApiException, msg)
        }

        fun Unknown(msg: String): GErr {
            return GErr(GErrType.Unknown, msg)
        }

        fun UnknownSerializable(msg: String): GErr {
            return GErr(GErrType.UnknownSerializable, msg)
        }

        fun UnknownGraphqlSchema(msg: String): GErr {
            return GErr(GErrType.UnknownGraphqlSchema, msg)
        }
    }
}