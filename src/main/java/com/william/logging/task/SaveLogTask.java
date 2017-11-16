package com.william.logging.task;

import com.william.logging.model.LoggerInfo;
import com.william.logging.service.LogService;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by sungang on 2017/11/16.
 */
public class SaveLogTask implements Runnable {


    private LogService logService;
    private LoggerInfo log;
    HttpServletRequest request;

    public SaveLogTask(LogService logService, LoggerInfo log) {
        this.logService = logService;
        this.log = log;
    }


    @Override
    public void run() {
//        //保存系统日志
        logService.insert(log);
    }
}
