package com.gambasoftware.poc.used;

public class MainUsingA {
    public static void main(String[] args) {
        A a = new A(new C());
        a.useC();
    }
}
