package com.william.logging.enums;

/**
 * Created by sungang on 2017/11/2.
 */
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
