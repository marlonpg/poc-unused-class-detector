package com.gambasoftware.alternatives;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
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
                if (!entry.getName().endsWith(".class"))
                    continue;

                try (InputStream is = jarFile.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(is);
                    UsageVisitor visitor = new UsageVisitor(targetClasses);
                    reader.accept(visitor, 0);

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
            // Call parent constructor with ASM API version ASM9 most recent
            super(Opcodes.ASM9);
            this.targetClasses = targetClasses;
        }

        /**
         * Called when a class is visited.
         * @param name The internal name of the class (e.g., "com/example/MyClass").
         * @param superName The internal name of the superclass.
         * @param interfaces The internal names of implemented interfaces.
         */
        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.currentClass = name.replace('/', '.');

            // Check if superclass is in the target classes
            checkClass(superName);

            // Check if any implemented interfaces are in the target classes
            if (interfaces != null) {
                for (String iface : interfaces) {
                    checkClass(iface);
                }
            }
        }

        /**
         * Called when a field is visited in the class.
         * @param descriptor Type descriptor (e.g., "Ljava/lang/String;").
         */
        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            // Check if the field type is a tracked class
            checkType(Type.getType(descriptor));
            return null;
        }

        /**
         * Called when a method is visited in the class.
         * @param descriptor Method descriptor (e.g., "(Ljava/lang/String;)V").
         * @param exceptions Exceptions thrown by the method (nullable).
         * @return MethodVisitor to visit method internals.
         */
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            // Check parameter and return types
            checkMethodType(Type.getMethodType(descriptor));
            // Check exceptions thrown by the method
            if (exceptions != null) {
                for (String ex : exceptions) {
                    checkClass(ex);
                }
            }
            // Return a visitor for method instructions
            return new MethodUsageVisitor();
        }

        /**
         * Checks if a class should be marked as used.
         *
         * @param internalName Internal class name (e.g., "java/lang/String").
         */
        private void checkClass(String internalName) {
            if (internalName == null) return;
            String className = internalName.replace('/', '.');
            if (targetClasses.contains(className) && !className.equals(currentClass)) {
                usedClasses.add(className);
            }
        }

        /**
         * Checks if a type is a class or array and processes it accordingly.
         *
         * @param type The ASM Type object.
         */
        private void checkType(Type type) {
            if (type.getSort() == Type.OBJECT) {
                // Process object types
                checkClass(type.getInternalName());
            } else if (type.getSort() == Type.ARRAY) {
                // Recursively process array elements
                checkType(type.getElementType());
            }
        }

        /**
         * Checks parameter and return types of a method.
         *
         * @param methodType The ASM Type representing the method.
         */
        private void checkMethodType(Type methodType) {
            // Check return type
            checkType(methodType.getReturnType());
            // Check parameter types
            for (Type arg : methodType.getArgumentTypes()) {
                checkType(arg);
            }
        }

        /**
         * Returns the set of detected used classes.
         *
         * @return A set of used class names.
         */
        public Set<String> getUsedClasses() {
            return usedClasses;
        }

        /**
         * Inner class to track method instructions and detect class usage.
         */
        class MethodUsageVisitor extends MethodVisitor {
            public MethodUsageVisitor() {
                // Use the latest ASM API version ASM9 which is the most recent
                super(Opcodes.ASM9);
            }

            /**
             * Called when a field is accessed in the method.
             * @param owner      The internal class name where the field is defined.
             * @param descriptor The field type descriptor.
             */
            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                // Check the class that owns the field
                checkClass(owner);
                // Check the field type
                checkType(Type.getType(descriptor));
            }

            /**
             * Called when a method is invoked in the method.
             *
             * @param owner       The internal class name of the method owner.
             * @param descriptor  The method descriptor.
             */
            @Override
            public void visitMethodInsn(int opcode, String owner, String name,
                                        String descriptor, boolean isInterface) {
                // Check the class where the method is defined
                checkClass(owner);
                // Check method parameter and return types
                checkMethodType(Type.getMethodType(descriptor));
                // Check if Reflection methods are called
                if (owner.equals("java/lang/Class") && name.equals("forName")) {
                    System.out.println("Reflection detected: Class.forName");
                    System.out.println(descriptor);
                }
                if (owner.equals("java/lang/reflect/Method") && name.equals("invoke")) {
                    System.out.println("Reflection detected: Method.invoke");
                    System.out.println(descriptor);
                }
            }

            /**
             * Called when a type instruction is encountered (e.g., NEW, ANEWARRAY, CHECKCAST, INSTANCEOF).
             * @param type   The internal class name being used.
             */
            @Override
            public void visitTypeInsn(int opcode, String type) {
                // Check the referenced class
                checkClass(type);
            }

            /**
             * Called when a constant (LDC) is encountered in the method.
             *
             * @param value The loaded constant (could be a string, number, or class type).
             */
            @Override
            public void visitLdcInsn(Object value) {

                //TODO: Continue looking HERE
                if (value instanceof Type) {
                    // Check if the constant is a class reference
                    checkType((Type) value);
                }
            }
        }
    }
}