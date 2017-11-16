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
