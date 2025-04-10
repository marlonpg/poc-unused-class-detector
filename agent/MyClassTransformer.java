import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class MyClassTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        System.out.println("Loaded class: " + className);
        return classfileBuffer; // Return unmodified bytecode (for now)
    }
}