package com.william.logging;

import com.william.logging.annotation.SysLog;
import com.william.logging.enums.SysLogType;
import org.springframework.web.bind.annotation.*;

/**
 * Created by sungang on 2017/11/16.
 */
@RestController
@RequestMapping("log")
public class TestController {


    @RequestMapping(method = RequestMethod.POST)
    @SysLog(value = "测试日志模块", describe = "测试登录", type = SysLogType.LOGIN)
    public String testLogin() {
        return "LOGIN";
    }

    @RequestMapping(method = RequestMethod.GET)
    @SysLog(value = "测试日志模块", describe = "测试访问", type = SysLogType.AECCESS)
    public String testAccess() {
        return "AECCESS";
    }


    @RequestMapping(method = RequestMethod.PUT)
    @SysLog(value = "测试日志模块", describe = "测试操作", type = SysLogType.OPER)
    public String testOper() {
        return "OPER";
    }
}
