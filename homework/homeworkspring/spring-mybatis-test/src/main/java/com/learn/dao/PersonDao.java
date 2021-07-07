package com.learn.dao;

import com.learn.annotation.Autowired;
import com.learn.annotation.Service;
import com.learn.mybatis.SqlSessionTemplate;

/**
 * @author T00032266
 * @DateTime 2021/7/6
 */
@Service
public class PersonDao {

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;


    public void tranfer() {

    }
}
