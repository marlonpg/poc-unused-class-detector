package com.gambasoftware.poc.used;

public class A {
    private C c;

    public A(C c) {
        this.c = c;
    }

    public void useC(){
        System.out.println(c);
    }
}
