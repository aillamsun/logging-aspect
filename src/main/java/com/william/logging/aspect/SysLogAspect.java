package com.william.logging.aspect;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.william.logging.annotation.SysLog;
import com.william.logging.model.LoggerInfo;
import com.william.logging.service.LogService;
import com.william.logging.service.UserService;
import com.william.logging.task.SaveLogTask;
import com.william.logging.utils.HttpContextUtils;
import com.william.logging.utils.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 系统日志，切面处理类
 * <p>
 * Created by sungang on 2017/11/2.
 */
@Aspect
@Component
@Slf4j
public class SysLogAspect {


    @Autowired
    private LogService logService;

    @Autowired
    private UserService userService;


    /**
     * 保存日志到数据库的线程池
     */
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("SLogAspect-Thread-%d").build();
    ExecutorService executor = new ThreadPoolExecutor(5, 200, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1024), threadFactory, new ThreadPoolExecutor.AbortPolicy());


    @Pointcut("@annotation(com.william.logging.annotation.SysLog)")
    public void logPointCut() {
    }

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        //执行方法
        Object result = point.proceed();
        //执行时长(毫秒)
        long time = System.currentTimeMillis() - beginTime;
        LoggerInfo log = new LoggerInfo();
        //执行结果状态 默认成功
        log.setExecuteResult(1);
        log.setResponse("Success");
        //保存日志
        saveSysLog(point, log, time);
        return result;
    }

    /**
     * 执行结果
     *
     * @param joinPoint
     * @param result
     */
//    @AfterReturning(value = "logPointCut()", returning = "result")
//    public void afterReturning(JoinPoint joinPoint, Object result) {
//        String methodName = joinPoint.getSignature().getName();
//        System.out.println("The method " + methodName + " ends with " + result);
//    }

    /**
     * 异常处理
     *
     * @param joinPoint
     * @param e
     */
    @AfterThrowing(value = "logPointCut()", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, Exception e) {


        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        //请求的方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        SysLog syslog = method.getAnnotation(SysLog.class);

        LoggerInfo log = new LoggerInfo();
        if (syslog != null) {
            //注解上的功能描述
            log.setDescribe(syslog.value());
            //操作类型
            int type = syslog.type().getValue();
            log.setType(type);
            if (1 == type) {
                log.setAction("登录");
            } else if (2 == type) {
                log.setAction("访问");
//                log.setExecuteResultJson("访问地址:" + className + "." + methodName + "()");
            } else if (3 == type) {
                log.setAction("操作");
            }
            //操作模块
            log.setModule(syslog.value());
        }

        long beginTime = System.currentTimeMillis();
        //执行时长(毫秒)
        long time = System.currentTimeMillis() - beginTime;

        //执行结果状态 默认成功
        log.setExecuteResult(-1);
        if (e instanceof Exception) {
            saveSysLog(joinPoint, log, time);
        } else { //系统内部程序异常
            saveExceptionSysLog(joinPoint, log, time, e);
        }
    }

    /**
     * 保存系统日志
     *
     * @param joinPoint
     * @param time      耗时
     **/
    private void saveSysLog(JoinPoint joinPoint, LoggerInfo log, long time) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        log.setMethod(method);
        log.setTarget(joinPoint.getTarget().getClass());
        //请求的方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();


        SysLog syslog = method.getAnnotation(SysLog.class);
        if (syslog != null) {
            //注解上的功能描述
            log.setDescribe(syslog.value());
            //操作类型
            int type = syslog.type().getValue();
            log.setType(type);
            if (1 == type) {
                log.setAction("登录");
            } else if (2 == type) {
                log.setAction("访问");
                log.setDescribe("访问地址:" + className + "." + methodName + "()");
            } else if (3 == type) {
                log.setAction("操作");
            }
            //操作模块
            log.setModule(syslog.value());
        }
        //请求的参数
        Object[] args = joinPoint.getArgs();
        try {
            if (args.length >= 1) {
                Object arg = args[0];
                if (null != arg) {
                    String params = JSON.toJSONString(arg);
//                    log.setParameters(params);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        /**
         * 保存
         */
        save(log);
    }

    private void saveExceptionSysLog(JoinPoint joinPoint, LoggerInfo log, long time, Exception ex) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //请求的方法名
        log.setType(4);
        log.setAction("异常");
        //请求的参数
        try {
            Object[] args = joinPoint.getArgs();
            if (args.length >= 1) {
                String params = JSON.toJSONString(args);
                log.setResponse("参数==" + params + " >> 异常提示== " + ex.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.setResponse("异常提示== " + e.getMessage());
        }
        /**
         * 保存
         */
        save(log);
    }

    private void save(LoggerInfo log) {
        //获取request
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
//        //设置IP地址
        log.setId(IPUtils.getIpAddr(request));
        //获取用户
//        JSONObject userObj = (JSONObject) request.getSession().getAttribute(Constants.CURRENT_USER);
//        if (null != userObj) {
//            User user = userService.selectByKey(userObj.getString("id"));
//            if (null != user) {
//                log.setOperateUserId(String.valueOf(user.getId()));
//                log.setOperateAccount(user.getUsername());
//            }
//        }
        log.setUrl(request.getRequestURI());
        log.setHttpMethod(request.getMethod());
        log.setId(UUID.randomUUID().toString());
        log.setOperUserId("1");
        log.setOperUserName("admin");
        log.setRequestTime(System.currentTimeMillis());
        //多线程保存
        executor.execute(new SaveLogTask(logService, log));
    }
}
