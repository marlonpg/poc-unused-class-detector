package com.gambasoftware.poc;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class UnusedClassDetector {

    public static void main(String[] args) throws Exception {
        String jarPath = "target/poc-0.0.1-SNAPSHOT.jar";
        String packagePrefix = "com.gambasoftware.poc";

        Set<String> allClassesV2 = findAllClassesInSpringBootJar(jarPath, packagePrefix);

        System.out.println(allClassesV2);
    }

    private static Set<String> findAllClassesInSpringBootJar(String jarPath, String packagePrefix) throws Exception {
        Set<String> classes = new HashSet<>();
        String packagePath = "BOOT-INF/classes/" + packagePrefix.replace('.', '/');

        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.endsWith(".class") && name.startsWith(packagePath)) {
                    String className = name
                            .replace("BOOT-INF/classes/", "")
                            .replace("/", ".")
                            .replace(".class", "");
                    classes.add(className);
                }
            }
        }
        return classes;
    }
}