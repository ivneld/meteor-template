package com.meteor.support.async;

import java.lang.reflect.Method;

import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import com.meteor.support.web.ErrorCodeLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

/**
 * {@code @Async} 메서드에서 전파되지 못한 예외를 에러 코드의 로그 레벨에 맞춰 기록한다.
 */
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void handleUncaughtException(Throwable e, Method method, Object... params) {
        ErrorCode errorCode = (e instanceof CoreException coreException) ? coreException.getErrorCode()
                : ErrorCode.INTERNAL_ERROR;
        ErrorCodeLogger.log(log, errorCode, "Async failed [" + errorCode.getCode() + "] method=" + method.getName(), e);
    }

}
