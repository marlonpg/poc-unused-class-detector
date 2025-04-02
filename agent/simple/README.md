# Compile
javac -d out used\A.java used\C.java used\MainUsingA.java notused\B.java

jar cmf MANIFEST.MF poc.jar -C out .

# Run test on MainUsingA
java -cp out com.gambasoftware.poc.used.MainUsingA


java -javaagent:agent.jar -jar poc.jar



