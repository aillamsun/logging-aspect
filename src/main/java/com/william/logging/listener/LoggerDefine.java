package com.william.logging.listener;


public class LoggerDefine {
    private String module;

    private String describe;

    private Integer type;

    public LoggerDefine(String module, String describe,Integer type){
        this.module=module;
        this.describe=describe;
        this.type = type;
    }

    public String getDescribe() {
        return describe;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }
}

