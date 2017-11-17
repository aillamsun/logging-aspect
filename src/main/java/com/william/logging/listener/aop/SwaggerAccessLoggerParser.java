package com.william.logging.listener.aop;

import com.william.logging.context.MethodInterceptorHolder;
import com.william.logging.listener.LoggerDefine;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

public class SwaggerAccessLoggerParser implements AccessLoggerParser {
    @Override
    public boolean support(Class clazz, Method method) {

//        Api api = AnnotationUtils.findAnnotation(clazz, Api.class);
//        ApiOperation operation = AnnotationUtils.findAnnotation(method, ApiOperation.class);

        return true;
    }

    @Override
    public LoggerDefine parse(MethodInterceptorHolder holder) {
//        ApiOperation operation = holder.findAnnotation(ApiOperation.class);
//        String action = "";
//        if (null != operation) {
//            action = operation.value();
//        }
        return new LoggerDefine("", "",1);
    }
}
