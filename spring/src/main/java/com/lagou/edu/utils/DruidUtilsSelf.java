package com.lagou.edu.utils;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * @author 应癫
 */
public class DruidUtilsSelf {

    private DruidUtilsSelf(){
    }

    private static DruidDataSource druidDataSource = new DruidDataSource();


    static {
        druidDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        druidDataSource.setUrl("jdbc:mysql://mysql-aliyun.mysql.rds.aliyuncs.com:3306/mybatics?useUnicode=true&characterEncoding=utf-8&useSSL=false");
        druidDataSource.setUsername("mysql_aliyun");
        druidDataSource.setPassword("dyq528020501");

    }

    public static DruidDataSource getInstance() {
        return druidDataSource;
    }

}
