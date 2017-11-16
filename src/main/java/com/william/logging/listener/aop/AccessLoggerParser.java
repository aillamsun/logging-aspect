package com.william.logging.listener.aop;


import com.william.logging.context.MethodInterceptorHolder;
import com.william.logging.listener.LoggerDefine;

import java.lang.reflect.Method;

public interface AccessLoggerParser {
    boolean support(Class clazz, Method method);

    LoggerDefine parse(MethodInterceptorHolder holder);
}
