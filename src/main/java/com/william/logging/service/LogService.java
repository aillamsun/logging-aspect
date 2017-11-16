package com.william.logging.service;

import com.alibaba.fastjson.JSON;
import com.william.logging.model.LoggerInfo;
import org.springframework.stereotype.Service;

/**
 * Created by sungang on 2017/11/16.
 */
@Service
public class LogService {

    public void insert(LoggerInfo loggerInfo){
        System.out.println("SysLogAspect{} " + JSON.toJSONString(loggerInfo));
    }
}
