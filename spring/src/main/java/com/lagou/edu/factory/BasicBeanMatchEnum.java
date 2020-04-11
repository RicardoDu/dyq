package com.lagou.edu.factory;

import com.lagou.edu.anno.Component;
import com.lagou.edu.anno.Repository;
import com.lagou.edu.anno.Service;

import java.lang.annotation.Annotation;

public enum BasicBeanMatchEnum {

    /**
     * 注解配置
     */
    Service("com.lagou.edu.service.impl", Service.class),
    Component("com.lagou.edu.utils", Component.class),
    Transfer("com.lagou.edu.transaction", Component.class),
    Repository("com.lagou.edu.dao.impl", Repository .class);
    // 成员变量
    private String name;
    private Class<? extends Annotation> value;
    // 构造方法
    private BasicBeanMatchEnum(String name, Class<? extends Annotation> value) {
        this.name = name;
        this.value = value;
    }

    public String getName(){
        return this.name;
    }

    public Class<? extends Annotation> getValue(){
        return this.value;
    }
}