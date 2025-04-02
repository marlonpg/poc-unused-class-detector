package com.gambasoftware.poc;

import com.gambasoftware.poc.alternatives.AccessingAllClassesInPackage;
import com.gambasoftware.poc.alternatives.AllClassesFromJarDetector;

import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        String jarPath = "target/poc-0.0.1-SNAPSHOT.jar";
        String packagePrefix = "com.gambasoftware.poc";

        Set<String> classesFromJar = AllClassesFromJarDetector.findAllClassesUsingJarFile(jarPath, packagePrefix);
        Set<Class> classesFromMemory = AccessingAllClassesInPackage.findAllClassesUsingClassLoader(packagePrefix);

        System.out.println(classesFromJar);
        System.out.println(classesFromMemory);
    }
}
