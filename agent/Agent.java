import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Agent {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent started!");
        inst.addTransformer(new MyClassTransformer());
    }
}
