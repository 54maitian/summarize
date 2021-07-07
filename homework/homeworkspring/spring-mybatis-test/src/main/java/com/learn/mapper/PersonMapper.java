package com.learn.mapper;

import com.learn.model.Person;

import java.util.List;

public interface PersonMapper {
    /**
     * 根据条件查询一个人
     * @param person
     * @return
     */
    Person selectOne(Person person);

    /**
     * 查询所有人
     * @return
     */
    List<Person> selectList();

    int updatePerson(Person person);

    int deletePerson(Person person);

    int insertPerson(Person person);
}
