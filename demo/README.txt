This demo requires junit.jar on the classpath.

When running the demo with ant 1.7 this is done automatically. Just run "ant" 
on the command line. This will download junit jar (if needed) set up the test 
classpath, build the demo and then run it.

When running the demo with ant 1.6, first check that the junit.jar is in the 
"lib" directory. If this is NOT the case, run "ant download.junit.jar" to 
download the jar. You can then either add ./lib/junit.jar to the CLASSPATH 
environment variable or simply run the demo with "ant -lib ./lib/junit.jar".

After the demo has been run, the results can be found in 
./results/latest/html/index.html and viewed in a webbrowser.