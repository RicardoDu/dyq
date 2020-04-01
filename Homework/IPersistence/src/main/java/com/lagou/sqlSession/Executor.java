package com.lagou.sqlSession;

import com.lagou.pojo.Configuration;
import com.lagou.pojo.MappedStatement;

import java.sql.SQLException;
import java.util.List;

public interface Executor {

    public <E> List<E> query(Configuration configuration, MappedStatement mappedStatement, Object... params) throws Exception;

    // 更新 or 插入 or 删除，由传入的 MappedStatement 的 SQL 所决定
    public int update(Configuration configuration, MappedStatement ms, Object... params) throws SQLException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException;
}
