package com.learn.model;

import lombok.Getter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author T00032266
 * @DateTime 2021/6/30
 */
@Getter
public class InjectionMetadata {
    private List<Method> methodList = new ArrayList<>();
    private List<Field> fieldList = new ArrayList<>();

    public void addMethod(Method method) {
        methodList.add(method);
    }

    public void addField(Field field) {
        fieldList.add(field);
    }
}
