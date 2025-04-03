# poc-unused-class-detector


- https://www.baeldung.com/java-find-all-classes-in-package
- https://foojay.io/today/instrumenting-java-code-to-find-and-handle-unused-classes/

- reflection
- configuration
- access directly
- is it in memory (checking runtime)


Bytecode contains fully qualified class names, so imports are irrelevant there
Catches all class references, including:
- Fields, method parameters, return types
- Local variables, method calls
- Dynamic class loading (Class.forName())
- Annotations, superclasses, interfaces