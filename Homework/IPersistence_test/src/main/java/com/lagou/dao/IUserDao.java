package com.lagou.dao;

import com.lagou.pojo.User;

import java.util.List;

public interface IUserDao {

    //查询所有用户
    public List<User> findAll() throws Exception;

    public void updateById(User user) throws Exception;

    public void insertUser(User user) throws Exception;

    public void delUser(int id) throws Exception;
}
