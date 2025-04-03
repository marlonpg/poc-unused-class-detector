package com.gambasoftware.alternatives;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassDependencyAnalyzer {

    public static Map<String, Set<String>> findClassDependencies(String jarPath, String packagePrefix) throws Exception {
        Map<String, Set<String>> dependencies = new HashMap<>();
        Set<String> allClasses = AllClassesFromJarDetector.findAllClassesUsingJarFile(jarPath, packagePrefix);

        try (JarFile jarFile = new JarFile(jarPath)) {
            for (String className : allClasses) {
                String classPath = "BOOT-INF/classes/" + className.replace('.', '/') + ".class";
                JarEntry entry = jarFile.getJarEntry(classPath);

                if (entry != null) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        ClassReader reader = new ClassReader(is);
                        Set<String> classDependencies = new HashSet<>();

                        reader.accept(new ClassVisitor(Opcodes.ASM9) {
                            @Override
                            public void visit(int version, int access, String name, String signature,
                                              String superName, String[] interfaces) {
                                addIfInSet(name, allClasses, classDependencies);
                                addIfInSet(superName, allClasses, classDependencies);
                                if (interfaces != null) {
                                    for (String iface : interfaces) {
                                        addIfInSet(iface, allClasses, classDependencies);
                                    }
                                }
                            }

                            // You can override other visit methods to catch more dependencies
                        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

                        dependencies.put(className, classDependencies);
                    }
                }
            }
        }
        return dependencies;
    }

    private static void addIfInSet(String internalName, Set<String> allClasses, Set<String> dependencies) {
        if (internalName == null) return;
        String className = internalName.replace('/', '.');
        if (allClasses.contains(className)) {
            dependencies.add(className);
        }
    }

    public static Set<String> findMutualDependencies(Map<String, Set<String>> dependencies) {
        Set<String> mutualDependencies = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            String classA = entry.getKey();
            for (String classB : entry.getValue()) {
                if (dependencies.containsKey(classB) &&
                        dependencies.get(classB).contains(classA)) {
                    mutualDependencies.add(classA + " <-> " + classB);
                }
            }
        }

        return mutualDependencies;
    }
}