package com.gambasoftware.poc.used;

import com.gambasoftware.poc.notused.A;

import java.lang.reflect.Method;

public class MainUsingA {
    public static void main(String[] args) {
        A a = new A(new C());
        a.useC();
        try {
            //Adding scenario where a class is instantiated using Reflection
            Class<?> clazzB = Class.forName("com.gambasoftware.poc.notused.B");
            Object obj = clazzB.getDeclaredConstructor().newInstance();
            Method method = clazzB.getMethod("test");
            method.invoke(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
