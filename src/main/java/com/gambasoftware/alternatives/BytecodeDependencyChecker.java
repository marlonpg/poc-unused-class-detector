package com.gambasoftware.alternatives;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class BytecodeDependencyChecker {

    public static Map<String, Set<String>> findClassDependencies(
            String jarPath,
            Set<String> allClasses
    ) throws Exception {
        Map<String, Set<String>> dependencies = new HashMap<>();

        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (!entryName.endsWith(".class")) continue;

                try (InputStream is = jarFile.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(is);
                    ClassDependencyVisitor visitor = new ClassDependencyVisitor(allClasses);
                    reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

                    String currentClass = entryName
                            .replace(".class", "")
                            .replace("/", ".");

                    if (!visitor.getDependencies().isEmpty()) {
                        dependencies.put(currentClass, visitor.getDependencies());
                    }
                }
            }
        }
        return dependencies;
    }

    static class ClassDependencyVisitor extends ClassVisitor {
        private final Set<String> allClasses;
        private final Set<String> dependencies = new HashSet<>();
        private String currentClass;

        public ClassDependencyVisitor(Set<String> allClasses) {
            super(Opcodes.ASM9);
            this.allClasses = allClasses;
        }

        @Override
        public void visit(
                int version, int access, String name, String signature,
                String superName, String[] interfaces
        ) {
            this.currentClass = name.replace('/', '.');
            addIfInTarget(superName);
            if (interfaces != null) {
                for (String iface : interfaces) addIfInTarget(iface);
            }
        }

        @Override
        public MethodVisitor visitMethod(
                int access, String name, String descriptor,
                String signature, String[] exceptions
        ) {
            // Check method return type + parameter types
            Type returnType = Type.getReturnType(descriptor);
            checkType(returnType);

            Type[] argTypes = Type.getArgumentTypes(descriptor);
            for (Type argType : argTypes) {
                checkType(argType);
            }

            // Check exceptions
            if (exceptions != null) {
                for (String ex : exceptions) addIfInTarget(ex);
            }

            return new MethodDependencyVisitor();
        }

        private void addIfInTarget(String internalName) {
            if (internalName == null) return;
            String className = internalName.replace('/', '.');
            if (allClasses.contains(className) && !className.equals(currentClass)) {
                dependencies.add(className);
            }
        }

        private void checkType(Type type) {
            if (type.getSort() == Type.OBJECT) {
                addIfInTarget(type.getInternalName());
            } else if (type.getSort() == Type.ARRAY) {
                checkType(type.getElementType());
            }
        }

        public Set<String> getDependencies() {
            return dependencies;
        }

        class MethodDependencyVisitor extends MethodVisitor {
            public MethodDependencyVisitor() {
                super(Opcodes.ASM9);
            }

            @Override
            public void visitMethodInsn(
                    int opcode, String owner, String name,
                    String descriptor, boolean isInterface
            ) {
                addIfInTarget(owner);
                Type returnType = Type.getReturnType(descriptor);
                checkType(returnType);
                for (Type argType : Type.getArgumentTypes(descriptor)) {
                    checkType(argType);
                }
            }

            @Override
            public void visitFieldInsn(
                    int opcode, String owner, String name, String descriptor
            ) {
                addIfInTarget(owner);
                checkType(Type.getType(descriptor));
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                addIfInTarget(type);
            }

            @Override
            public void visitLdcInsn(Object value) {
                if (value instanceof Type) {
                    checkType((Type) value);
                }
            }
        }
    }
    public static Set<String> findCircularDependencies(Map<String, Set<String>> dependencies) {
        Set<String> circularDeps = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            String classA = entry.getKey();
            for (String classB : entry.getValue()) {
                if (dependencies.containsKey(classB) &&
                        dependencies.get(classB).contains(classA)) {
                    circularDeps.add(classA + " <-> " + classB);
                }
            }
        }

        return circularDeps;
    }
}