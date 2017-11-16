package com.william.logging.annotation;


import com.william.logging.enums.SysLogType;

import java.lang.annotation.*;

/**
 * 系统日志注解
 */
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
