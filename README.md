# logging-aspect
logging-aspect 项目中日志收集实现AOP 和 Listener两种全局实现



# 实现方式

## Base 注解 && 枚举类
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysLog {
    /**
     * 模块
     * @return
     */
    String value();

    /**
     * 功能描述
     *
     * @return
     */
    String describe() default "";

    //	分类 Id 1-登陆 2-访问 3-操作 4-异常
    SysLogType type() default SysLogType.AECCESS;

}
```

-----

```java
public enum SysLogType {

    //分类 Id 1-登录 2-访问 3-操作 4-异常
    LOGIN(1), AECCESS(2), OPER(3), EXCEPTION(4);


    private int value;

    SysLogType(int value){
        this.value = value;
    }


    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}

```


## 1 SysLogAspect

```java
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

```

## 2 AccessLoggerListener

```java
/**
 * 访问日志监听器,实现此接口并注入到spring容器即可获取访问日志信息
 */
public interface AccessLoggerListener {

    /**
     * 当产生访问日志时,将调用此方法.注意,此方法内的操作应尽量设置为异步操作,否则可能影响请求性能
     *
     * @param loggerInfo 产生的日志信息
     */
    void onLogger(LoggerInfo loggerInfo);

    default void onLogBefore(LoggerInfo loggerInfo){}
}
```


# 使用方式

## 1 直接SysLogAspect

## 2 实现AccessLoggerListener
```java
/**
 * Created by sungang on 2017/11/17.
 */
@Component
public class TestLog implements AccessLoggerListener {


    @Override
    public void onLogger(LoggerInfo loggerInfo) {
        System.out.println("AccessLoggerListener{} " + JSON.toJSONString(loggerInfo));
    }

}
```



# 测试使用

```java
@RestController
@RequestMapping("log")
public class TestController {


    @RequestMapping(method = RequestMethod.POST)
    @SysLog(value = "Test Log", describe = "登录", type = SysLogType.LOGIN)
    public String testLogin() {
        return "LOGIN";
    }

    @RequestMapping(method = RequestMethod.GET)
    @SysLog(value = "Test Log", describe = "请求", type = SysLogType.AECCESS)
    public String testAccess() {
        return "AECCESS";
    }


    @RequestMapping(method = RequestMethod.PUT)
    @SysLog(value = "Test Log", describe = "操作", type = SysLogType.OPER)
    public String testOper() {
        return "OPER";
    }
}

```



> * 1 启动 LoggingAspectApplication
> * 2 请求:http://localhost:8080/log

#### 响应:

> * SysLogAspect 响应

```json
SysLogAspect{} {"action":"访问","describe":"访问地址:com.william.logging.TestController.testAccess()","executeResult":1,"httpMethod":"GET","id":"57cccc5c-9e62-4a47-8417-f604c958edb9","method":{"accessible":false,"annotatedExceptionTypes":[],"annotatedParameterTypes":[],"annotatedReceiverType":{"annotations":[],"declaredAnnotations":[],"type":"com.william.logging.TestController"},"annotatedReturnType":{"annotations":[],"declaredAnnotations":[],"type":"java.lang.String"},"annotations":[{},{}],"bridge":false,"declaringClass":"com.william.logging.TestController","default":false,"exceptionTypes":[],"genericExceptionTypes":[],"genericParameterTypes":[],"genericReturnType":"java.lang.String","modifiers":1,"name":"testAccess","parameterAnnotations":[],"parameterCount":0,"parameterTypes":[],"returnType":"java.lang.String","synthetic":false,"typeParameters":[],"varArgs":false},"module":"Test Log","operUserId":"1","operUserName":"admin","requestTime":1510849753705,"response":"Success","responseTime":0,"target":"com.william.logging.TestController","type":2,"url":"/log"}
```

> * AccessLoggerListener 响应
```json
AccessLoggerListener{} {"action":"Test Log","describe":"请求","httpHeaders":{"host":"localhost:8080","connection":"keep-alive","cache-control":"max-age=0","user-agent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36","upgrade-insecure-requests":"1","accept":"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8","accept-encoding":"gzip, deflate, br","accept-language":"zh-CN,zh;q=0.8,en;q=0.6","cookie":"Idea-92744ae7=c77d525f-75f6-4a90-892f-60b49798f026; olfsk=olfsk49921969428008994; hblid=vKo3049kW1NNZ4bW3m39N0H0RE2aDaCZ; shop_id=2; JSESSIONID=47321175CA9DE3494204BACCA95FAA3A"},"httpMethod":"GET","id":"08679c47-57d1-4420-b210-6e9b02061a83","ip":"127.0.0.1","method":{"accessible":false,"annotatedExceptionTypes":[],"annotatedParameterTypes":[],"annotatedReceiverType":{"annotations":[],"declaredAnnotations":[],"type":"com.william.logging.TestController"},"annotatedReturnType":{"annotations":[],"declaredAnnotations":[],"type":"java.lang.String"},"annotations":[{},{}],"bridge":false,"declaringClass":"com.william.logging.TestController","default":false,"exceptionTypes":[],"genericExceptionTypes":[],"genericParameterTypes":[],"genericReturnType":"java.lang.String","modifiers":1,"name":"testAccess","parameterAnnotations":[],"parameterCount":0,"parameterTypes":[],"returnType":"java.lang.String","synthetic":false,"typeParameters":[],"varArgs":false},"parameters":{},"requestTime":1510849753618,"response":"AECCESS","responseTime":1510849753707,"target":"com.william.logging.TestController","url":"http://localhost:8080/log"}
```


#### 自己任选其一