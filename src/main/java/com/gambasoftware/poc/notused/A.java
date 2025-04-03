package com.gambasoftware.poc.notused;

import com.gambasoftware.poc.used.C;

public class A {
    private C c;

    public A(C c) {
        this.c = c;
    }

    public void useC(){
        System.out.println(c);
    }
}
