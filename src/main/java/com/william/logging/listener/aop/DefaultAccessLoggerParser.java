package com.william.logging.listener.aop;


import com.william.logging.annotation.SysLog;
import com.william.logging.context.MethodInterceptorHolder;
import com.william.logging.enums.SysLogType;
import com.william.logging.listener.LoggerDefine;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Stream;


public class DefaultAccessLoggerParser implements AccessLoggerParser {
    @Override
    public boolean support(Class clazz, Method method) {
        SysLog ann = AnnotationUtils.findAnnotation(method, SysLog.class);
        //注解了并且未取消
//        return null != ann && !ann.ignore();
        return null != ann;
    }

    @Override
    public LoggerDefine parse(MethodInterceptorHolder holder) {
        SysLog methodAnn = holder.findMethodAnnotation(SysLog.class);
        SysLog classAnn = holder.findClassAnnotation(SysLog.class);
        String action = Stream.of(classAnn, methodAnn)
                .filter(Objects::nonNull)
                .map(SysLog::value)
                .reduce((c, m) -> c.concat("-").concat(m))
                .orElse("");
        String describe = Stream.of(classAnn, methodAnn)
                .filter(Objects::nonNull)
                .map(SysLog::describe)
                .flatMap(Stream::of)
                .reduce((c, s) -> c.concat("\n").concat(s))
                .orElse("");
        Integer type = SysLogType.AECCESS.getValue();
        if (null != methodAnn){
            type = methodAnn.type().getValue();
        }

        return new LoggerDefine(action, describe, type);
    }
}
