package com.gambasoftware.poc;

import com.gambasoftware.alternatives.AllClassesFromJarDetector;
import com.gambasoftware.alternatives.ClassUsageAnalyzer;

import java.util.Map;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        String jarPath = "target/poc-0.0.1-SNAPSHOT.jar";
        String packagePrefix = "com.gambasoftware.poc";
        System.out.println("All classes from Jar");
        Set<String> classesFromJar = AllClassesFromJarDetector.findAllClassesUsingJarFile(jarPath, packagePrefix);
        classesFromJar.forEach(System.out::println);

        // Checking class usage using ASM
        Map<String, Set<String>> dependencies = ClassUsageAnalyzer.findClassUsages(jarPath, classesFromJar);

        System.out.println("Class and used classes");
        dependencies.forEach((cl4ss, deps) -> {
            System.out.println(cl4ss + " -> " + deps);
        });
    }
}
