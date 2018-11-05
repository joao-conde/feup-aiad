mkdir bin;
cd src/utilities;
javac -d '../../bin' *.java;
cd ..;
cd main;
javac  -classpath '../utilities/Utils.java' -d '../../bin'  -cp '../../jade/lib/jade.jar' *.java;
java -cp .:./jade/lib/jade.jar Market;