package org.puregxl.framework.web;

import org.puregxl.framework.errorcode.BaseErrorCode;
import org.puregxl.framework.exception.AbstractException;
import org.puregxl.framework.result.Result;

import java.util.Optional;

/**
 * 构造全局返回对象
 */
public class Results {

    /**
     * 直接返回成功
     * @return
     */
    public static Result<Void> success(){
        return new Result<Void>()
                .setCode(Result.SUCCESS_CODE);
    }

    /**
     * 带有数据的成功
     * @param data
     * @return
     * @param <T>
     */
    public static <T> Result<T> success(T data){
        return new Result<T>()
                .setCode(Result.SUCCESS_CODE)
                .setData(data);
    }

    /**
     * 构建服务端失败响应
     * @return
     */
    public static Result<Void> failure(){
        return new Result<Void>()
                .setCode(BaseErrorCode.SERVICE_ERROR.code())
                .setMessage(BaseErrorCode.SERVICE_ERROR.message());
    }


    /**
     * 使用AbstractException 构建
     * @param abstractException
     * @return
     */
    public static Result<Void> failure(AbstractException abstractException) {
        String code = Optional.ofNullable(abstractException.errorCode)
                .orElse(BaseErrorCode.SERVICE_ERROR.code());

        String message = Optional.ofNullable(abstractException.errorMessage)
                .orElse(BaseErrorCode.SERVICE_ERROR.message());

        return new Result<Void>()
                .setCode(abstractException.errorCode)
                .setMessage(abstractException.errorMessage);
    }

    /**
     * 通过 errorCode、errorMessage 构建失败响应
     */
    protected static Result<Void> failure(String errorCode, String errorMessage) {
        return new Result<Void>()
                .setCode(errorCode)
                .setMessage(errorMessage);
    }

}
