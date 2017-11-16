package com.william.logging.listener.aop;

import com.william.logging.annotation.SysLog;
import com.william.logging.context.MethodInterceptorHolder;
import com.william.logging.listener.AccessLoggerListener;
import com.william.logging.listener.LoggerDefine;
import com.william.logging.model.LoggerInfo;
import com.william.logging.utils.AopUtils;
import com.william.logging.utils.WebUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 使用AOP记录访问日志,并触发{@link AccessLoggerListener#onLogger(LoggerInfo)}
 *
 */
public class AopAccessLoggerSupport extends StaticMethodMatcherPointcutAdvisor {

    private final List<AccessLoggerListener> listeners = new ArrayList<>();

    private final List<AccessLoggerParser> loggerParsers = new ArrayList<>();

    public AopAccessLoggerSupport addListener(AccessLoggerListener loggerListener) {
        listeners.add(loggerListener);
        return this;
    }

    public AopAccessLoggerSupport addParser(AccessLoggerParser parser) {
        loggerParsers.add(parser);
        return this;
    }

    public AopAccessLoggerSupport() {
        setAdvice((MethodInterceptor) methodInvocation -> {
            MethodInterceptorHolder methodInterceptorHolder = MethodInterceptorHolder.create(methodInvocation);
            LoggerInfo info = createLogger(methodInterceptorHolder);
            Object response;
            try {
                listeners.forEach(listener -> listener.onLogBefore(info));
                response = methodInvocation.proceed();
                info.setResponse(response);
                info.setResponseTime(System.currentTimeMillis());
            } catch (Throwable e) {
                info.setException(e);
                throw e;
            } finally {
                //触发监听
                listeners.forEach(listener -> listener.onLogger(info));
            }
            return response;
        });
    }

    protected LoggerInfo createLogger(MethodInterceptorHolder holder) {
        LoggerInfo info = new LoggerInfo();
        info.setId(UUID.randomUUID().toString());

        info.setRequestTime(System.currentTimeMillis());


        LoggerDefine define = loggerParsers.stream()
                .filter(parser -> parser.support(ClassUtils.getUserClass(holder.getTarget()), holder.getMethod()))
                .findAny()
                .map(parser -> parser.parse(holder))
                .orElse(null);

        if (define != null) {
            info.setAction(define.getAction());
            info.setDescribe(define.getDescribe());
        }
        info.setParameters(holder.getArgs());
        info.setTarget(holder.getTarget().getClass());
        info.setMethod(holder.getMethod());

        HttpServletRequest request = WebUtil.getHttpServletRequest();
        if (null != request) {
            info.setHttpHeaders(WebUtil.getHeaders(request));
            info.setIp(WebUtil.getIpAddr(request));
            info.setHttpMethod(request.getMethod());
            info.setUrl(request.getRequestURL().toString());
        }
        return info;

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public boolean matches(Method method, Class<?> aClass) {
        SysLog ann = AopUtils.findAnnotation(aClass, method, SysLog.class);
//        if(ann!=null&&ann.ignore()){
//            return false;
//        }
        RequestMapping mapping = AopUtils.findAnnotation(aClass, method, RequestMapping.class);
        return mapping != null;

//        //注解了并且未取消
//        return null != ann && !ann.ignore();
    }
}
