package com.gambasoftware.poc;

import com.gambasoftware.alternatives.AllClassesFromJarDetector;
import com.gambasoftware.alternatives.BytecodeDependencyChecker;
import com.gambasoftware.alternatives.ClassUsageAnalyzer;

import java.util.Map;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        String jarPath = "target/poc-0.0.1-SNAPSHOT.jar";
        String packagePrefix = "com.gambasoftware.poc";
        System.out.println("Classes from Jar");
        Set<String> classesFromJar = AllClassesFromJarDetector.findAllClassesUsingJarFile(jarPath, packagePrefix);
        classesFromJar.forEach(System.out::println);

        // 2. Find dependencies
        Map<String, Set<String>> dependencies = BytecodeDependencyChecker.findClassDependencies(jarPath, classesFromJar);

        // 3. Detect circular dependencies
        Set<String> circularDeps = BytecodeDependencyChecker.findCircularDependencies(dependencies);

        // Print results
        System.out.println("=== Circular Dependencies ===");
        circularDeps.forEach(System.out::println);

        System.out.println("\n=== All Dependencies ===");
        dependencies.forEach((cls, deps) -> {
            System.out.println(cls + " -> " + deps);
        });
        ClassUsageAnalyzer.findClassUsages(jarPath, classesFromJar);
    }
}
