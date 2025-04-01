package com.gambasoftware.poc;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AllClassesFromJarDetector {
    public static Set<String> findAllClassesInSpringBootJar(String jarPath, String packagePrefix) throws Exception {
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