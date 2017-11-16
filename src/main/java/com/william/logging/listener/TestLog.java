package com.william.logging.listener;

import com.alibaba.fastjson.JSON;
import com.william.logging.model.LoggerInfo;
import org.springframework.stereotype.Component;
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
