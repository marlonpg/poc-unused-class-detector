package com.gambasoftware.alternatives;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassUsageAnalyzer {

    public static Map<String, Set<String>> findClassUsages(String jarPath, Set<String> targetClasses) throws Exception {
        Map<String, Set<String>> usages = new HashMap<>();

        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;

                try (InputStream is = jarFile.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(is);
                    UsageVisitor visitor = new UsageVisitor(targetClasses);
                    reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

                    if (!visitor.getUsedClasses().isEmpty()) {
                        String currentClass = entry.getName()
                                .replace(".class", "")
                                .replace("/", ".");
                        usages.put(currentClass, visitor.getUsedClasses());
                    }
                }
            }
        }
        return usages;
    }

    static class UsageVisitor extends ClassVisitor {
        private final Set<String> targetClasses;
        private final Set<String> usedClasses = new HashSet<>();
        private String currentClass;

        public UsageVisitor(Set<String> targetClasses) {
            super(Opcodes.ASM9);
            this.targetClasses = targetClasses;
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.currentClass = name.replace('/', '.');
            checkClass(superName);
            if (interfaces != null) {
                for (String iface : interfaces) checkClass(iface);
            }
        }

        @Override
        public org.objectweb.asm.FieldVisitor visitField(int access, String name, String descriptor,
                                                         String signature, Object value) {
            checkType(Type.getType(descriptor));
            return null;
        }

        @Override
        public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor,
                                                           String signature, String[] exceptions) {
            checkMethodType(Type.getMethodType(descriptor));
            if (exceptions != null) {
                for (String ex : exceptions) checkClass(ex);
            }
            return new MethodUsageVisitor();
        }

        private void checkClass(String internalName) {
            if (internalName == null) return;
            String className = internalName.replace('/', '.');
            if (targetClasses.contains(className) && !className.equals(currentClass)) {
                usedClasses.add(className);
            }
        }

        private void checkType(Type type) {
            if (type.getSort() == Type.OBJECT) {
                checkClass(type.getInternalName());
            } else if (type.getSort() == Type.ARRAY) {
                checkType(type.getElementType());
            }
        }

        private void checkMethodType(Type methodType) {
            checkType(methodType.getReturnType());
            for (Type arg : methodType.getArgumentTypes()) {
                checkType(arg);
            }
        }

        public Set<String> getUsedClasses() {
            return usedClasses;
        }

        class MethodUsageVisitor extends org.objectweb.asm.MethodVisitor {
            public MethodUsageVisitor() {
                super(Opcodes.ASM9);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                checkClass(owner);
                checkType(Type.getType(descriptor));
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name,
                                        String descriptor, boolean isInterface) {
                checkClass(owner);
                checkMethodType(Type.getMethodType(descriptor));
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                checkClass(type);
            }

            @Override
            public void visitLdcInsn(Object value) {
                if (value instanceof Type) {
                    checkType((Type) value);
                }
            }
        }
    }
}