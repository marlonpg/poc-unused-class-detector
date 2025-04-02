# Compile
javac Agent.java
jar cmf MANIFEST.MF agent.jar Agent.class MyClassTransformer.class

java -cp out Agent

# Run
java -javaagent:agent.jar -jar simple\poc.jar
