package org.codehaus.xharness.tasks;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;

import org.codehaus.xharness.log.LineBuffer;
import org.codehaus.xharness.log.TaskRegistry;
import org.codehaus.xharness.procutil.KillableExecuteWatchdog;
import org.codehaus.xharness.testutil.ProcessTester;
import org.codehaus.xharness.types.EnvSet;
import org.codehaus.xharness.types.EnvironmentVariable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class XhJavaTaskTest extends TestCase {
    private static final String JVM_DIR = new String(
            (System.getProperty("java.home") + "/bin")
            .replace('/', File.separatorChar).replace(
                     '\\', File.separatorChar));
    private static final String JVM = new String(
              JVM_DIR + File.separator + "java" 
            + (System.getProperty("os.name").toLowerCase().startsWith("win") ? ".exe" : ""));

    public XhJavaTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = XhJavaTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = XhJavaTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(XhJavaTaskTest.class);
    }
    public void testExecuteNoClassname() throws Exception {
        Project project = new Project();
        
        XhJavaTask task = new XhJavaTask();
        task.setProject(project);
        task.createArg().setLine("blah");
        
        try {
            task.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Classname must not be null.", be.getMessage());
        }
    }
    
    public void testExecute() throws Exception {
        Project project = new Project();
        
        ProcessTester server = new ProcessTester();
        
        XhJavaTask task = new XhJavaTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        
        task.execute();
        assertEquals("Wrong return value", 0, task.getReturnValue());
        assertTrue("Client did not run", server.passed());
        assertEquals("Wrong user dir", System.getProperty("user.dir"), server.getReceivedData());
    }
    
    public void testExecuteWithDir() throws Exception {
        Project project = new Project();
        
        ProcessTester server = new ProcessTester();
        File execDir = new File("temp");
        
        XhJavaTask task = new XhJavaTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.setDir(execDir);
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        
        try {
            execDir.mkdir();
            task.execute();
        } finally {
            execDir.delete();
        }
        assertEquals("Wrong return value", 0, task.getReturnValue());
        assertTrue("Client did not run", server.passed());
        assertEquals("Wrong user dir", execDir.getAbsolutePath(), server.getReceivedData());
    }
    
    public void testExecuteTimeout() throws Exception {
        Project project = new Project();
        
        ProcessTester server = new ProcessTester();
        
        XhJavaTask task = new XhJavaTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.setTimeout(new Long(2000));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 10");
        
        try {
            task.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Timeout: killed the sub-process", be.getMessage());
        }
        assertEquals("Wrong return value", 0, task.getReturnValue());
        assertTrue("Client did not run", server.passed());
        assertEquals("Wrong user dir", System.getProperty("user.dir"), server.getReceivedData());
    }
    
    public void testExecuteCurrentTestDirProperty() throws Exception {
        Project project = new Project();
        project.setProperty(TaskRegistry.CURRENT_TEST_DIR_PROPERY, JVM_DIR);
        
        ProcessTester server = new ProcessTester();
        
        XhJavaTask task = new XhJavaTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        
        task.execute();
        assertEquals("Wrong return value", 0, task.getReturnValue());
        assertTrue("Client did not run", server.passed());
        assertEquals("Wrong user dir", JVM_DIR, server.getReceivedData());
    }
    
    public void testExecuteCurrentTestDirPropertyDirOverride() throws Exception {
        Project project = new Project();
        project.setProperty(TaskRegistry.CURRENT_TEST_DIR_PROPERY, JVM_DIR);
        
        ProcessTester server = new ProcessTester();
        File execDir = new File("temp");
        
        XhJavaTask task = new XhJavaTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.setDir(execDir);
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        
        try {
            execDir.mkdir();
            task.execute();
        } finally {
            execDir.delete();
        }
        
        assertEquals("Wrong return value", 0, task.getReturnValue());
        assertTrue("Client did not run", server.passed());
        assertEquals("Wrong user dir", execDir.getAbsolutePath(), server.getReceivedData());
    }
    
    public void testExecuteCurrentTestDirPropertyNonExistent() throws Exception {
        Project project = new Project();
        project.setProperty(TaskRegistry.CURRENT_TEST_DIR_PROPERY, "bogus");
        
        ProcessTester server = new ProcessTester();
        
        XhJavaTask task = new XhJavaTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        
        task.execute();
        assertEquals("Wrong return value", 0, task.getReturnValue());
        assertTrue("Client did not run", server.passed());
        assertEquals("Wrong temp dir", System.getProperty("user.dir"), server.getReceivedData());
    }
    
    public void testExecuteEnvSet() throws Exception {
        Project project = new Project();
        
        EnvironmentVariable var = new EnvironmentVariable();
        var.setKey("CLASSPATH");
        var.setPath(new Path(project, getClassPath() + ";\\marker"));

        EnvSet envset = new EnvSet();
        envset.setLoadenvironment(true);
        envset.addEnv(var);
        
        ProcessTester server = new ProcessTester();
        
        XhJavaTask task = new XhJavaTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.addEnvset(envset);
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s java.class.path");
        
        task.execute();
        assertEquals("Wrong return value", 0, task.getReturnValue());
        assertTrue("Client did not run", server.passed());
        assertEndsWith("Wrong classpath", 
                       "marker", 
                       server.getReceivedData());
    }

    public void testExecuteLogging() throws Exception {
        Project project = new Project();
        LineBuffer buffer = new LineBuffer();
        
        ProcessTester server = new ProcessTester();
        
        XhJavaTask task = new XhJavaTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.enableLogging(buffer, 0, 1);
        
        task.execute();
        assertEquals("Wrong return value", 0, task.getReturnValue());
        assertTrue("Client did not run", server.passed());
        assertEquals("Wrong user dir", System.getProperty("user.dir"), server.getReceivedData());
        assertEquals("Wrong stderr", "Welcome stderr!", buffer.toString(1));
        String stdout = buffer.toString(0);
        assertTrue("Wrong stdout", stdout.startsWith("Welcome stdout!"));
        assertTrue("Wrong stdout", stdout.endsWith("...done. Exiting."));
    }
    
    public void testGetCommandline() throws Exception {
        Project project = new Project();
        
        ProcessTester server = new ProcessTester();
        
        XhJavaTask task = new XhJavaTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        Path path = new Path(project, getClassPath());
        task.setClasspath(path);
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        
        task.execute();
        assertContains("Wrong commandline", JVM.toLowerCase(), task.getCommandline().toLowerCase());
        assertContains("Wrong commandline", " -classpath ", task.getCommandline());
        assertContains("Wrong commandline", ProcessTester.class.getName(), task.getCommandline());
        assertContains("Wrong commandline", " -p " + server.getPort(), task.getCommandline());
        assertContains("Wrong commandline", " -s user.dir", task.getCommandline());
    }

    public void testCreateWatchdog() throws Exception {
        Project project = new Project();
        XhJavaTask task = new XhJavaTask();
        task.setProject(project);
        ExecuteWatchdog dog = task.createWatchdog();
        assertEquals("Wrong watchdog", 
                     KillableExecuteWatchdog.class.getName(), 
                     dog.getClass().getName());
        assertEquals("Wrong watchdog", dog, task.getWatchdog());
    }

    public void testOverrideFailonerror() throws Exception {
        Project project = new Project();
        
        ProcessTester server = new ProcessTester();
        
        XhJavaTask task = new XhJavaTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        Field f1 = XhJavaTask.class.getDeclaredField("overrideFailOnError");
        Field f2 = Java.class.getDeclaredField("spawn");
        Field f3 = Java.class.getDeclaredField("failOnError");
        Field f4 = Java.class.getDeclaredField("incompatibleWithSpawn");
        // override Java language access checks to be able to read&write private members
        Field.setAccessible(new Field[] {f1, f2, f3, f4}, true);
        
        assertEquals(Boolean.TRUE, f1.get(task));
        assertEquals(Boolean.FALSE, f2.get(task));
        assertEquals(Boolean.FALSE, f3.get(task));
        assertEquals(Boolean.FALSE, f4.get(task));
        
        task.execute();
        
        assertEquals(Boolean.TRUE, f1.get(task));
        assertEquals(Boolean.FALSE, f2.get(task));
        assertEquals(Boolean.TRUE, f3.get(task));  // failOnError -> true
        assertEquals(Boolean.TRUE, f4.get(task));  // incompatibleWithSpawn -> true
        f3.set(task, Boolean.FALSE); // reset
        f4.set(task, Boolean.FALSE); // reset
        
        task.setFailonerror(false);
        task.execute();
        
        assertEquals(Boolean.FALSE, f1.get(task)); // overrideFailOnError -> false
        assertEquals(Boolean.FALSE, f2.get(task));
        assertEquals(Boolean.FALSE, f3.get(task)); 
        assertEquals(Boolean.FALSE, f4.get(task));
        f1.set(task, Boolean.TRUE); // reset
        
        task.setFailonerror(true);
        task.execute();
        
        assertEquals(Boolean.FALSE, f1.get(task)); // overrideFailOnError -> false
        assertEquals(Boolean.FALSE, f2.get(task));
        assertEquals(Boolean.TRUE, f3.get(task));  // failOnError -> true
        assertEquals(Boolean.TRUE, f4.get(task));  // incompatibleWithSpawn -> true
        f1.set(task, Boolean.TRUE); // reset
        f3.set(task, Boolean.FALSE); // reset
        f4.set(task, Boolean.FALSE); // reset
        
        task.setSpawn(false);
        task.execute();
        
        assertEquals(Boolean.FALSE, f1.get(task)); // overrideFailOnError -> false
        assertEquals(Boolean.FALSE, f2.get(task));
        assertEquals(Boolean.FALSE, f3.get(task)); 
        assertEquals(Boolean.FALSE, f4.get(task));
        f1.set(task, Boolean.TRUE); // reset
        
        task.setSpawn(true);
        task.execute();
        
        assertEquals(Boolean.FALSE, f1.get(task)); // overrideFailOnError -> false
        assertEquals(Boolean.TRUE, f2.get(task)); // spawn -> true
        assertEquals(Boolean.FALSE, f3.get(task));
        assertEquals(Boolean.FALSE, f4.get(task));
        f1.set(task, Boolean.TRUE); // reset
        f2.set(task, Boolean.FALSE); // reset
        
        task.setSpawn(true);
        task.setFailonerror(true);
        try {
            task.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("You have used an attribute or nested element which is not compatible with spawn", be.toString());
        }
        
        assertEquals(Boolean.FALSE, f1.get(task)); // overrideFailOnError -> false
        assertEquals(Boolean.TRUE, f2.get(task));  // spawn -> true
        assertEquals(Boolean.TRUE, f3.get(task));  // failOnError -> true
        assertEquals(Boolean.TRUE, f4.get(task)); // incompatibleWithSpawn -> true
        f1.set(task, Boolean.TRUE); // reset
        f2.set(task, Boolean.FALSE); // reset
        f3.set(task, Boolean.FALSE); // reset
        f4.set(task, Boolean.FALSE); // reset
    }

    protected String getClassPath() {
        ClassLoader loader = getClass().getClassLoader();
        StringBuffer classPath = new StringBuffer();
        if (loader instanceof URLClassLoader) {
            URLClassLoader urlLoader = (URLClassLoader)loader;
            URL[] urls = urlLoader.getURLs();
            for (int x = 0; x < urls.length; x++) {
                String file = urls[x].getFile().replaceAll("%20", " ");
                if (file.indexOf("junit") == -1) {
                    classPath.append(file);
                    classPath.append(System.getProperty("path.separator"));
                }
            }
        }
        return classPath.toString();
    }
    
    private static void assertContains(String msg, String s1, String s2) {
        assertTrue(msg + " <" + s1 + "> not found in <" + s2 + ">", s2.indexOf(s1) >= 0);
    }
    
    private static void assertEndsWith(String msg, String s1, String s2) {
        assertTrue(msg + " <" + s2 + "> doesn't end with <" + s1 + ">", s2.endsWith(s1));
    }
}
