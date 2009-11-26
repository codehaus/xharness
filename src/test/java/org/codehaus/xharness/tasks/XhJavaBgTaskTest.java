package org.codehaus.xharness.tasks;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.types.Path;

import org.codehaus.xharness.procutil.KillableExecuteWatchdog;
import org.codehaus.xharness.procutil.ProcessRegistry;
import org.codehaus.xharness.testutil.ProcessTester;
import org.codehaus.xharness.testutil.TestProject;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class XhJavaBgTaskTest extends TestCase {
    private static final String PROC_NAME = "myProcessName";

    public XhJavaBgTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = XhJavaBgTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = XhJavaBgTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(XhJavaBgTaskTest.class);
    }
    
    public void setUp() throws Exception {
        ProcessRegistry.reset();
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

    public void testProcessName() throws Exception {
        XhJavaBgTask task = new XhJavaBgTask();
        task.setProcessName(PROC_NAME);
        assertEquals("Wrong process name", PROC_NAME, task.getProcessName());
    }
    
    public void testExecute() throws Exception {
        TestProject project = new TestProject();
        
        ProcessTester server = new ProcessTester();
        
        XhJavaBgTask task = new XhJavaBgTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.setProcessName(PROC_NAME);
        
        try {
            ProcessRegistry.getProcess(PROC_NAME);
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "Process " + PROC_NAME + " not registered.", 
                         be.getMessage());
        }
        task.execute();
        assertEquals("Process not registered", task, ProcessRegistry.getProcess(PROC_NAME));
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Process is not running", server.passed());
        assertEqualsIgnoreCase("Wrong user dir", 
                               System.getProperty("user.dir"), 
                               server.getReceivedData());
        server.getSocket().close();
        // wait for process to terminate
        for (int i = 100; i > 0 && task.isRunning(); i--) {
            Thread.sleep(100);
        }
        assertTrue("Process is still running", !task.isRunning());
        try {
            ProcessRegistry.getProcess(PROC_NAME);
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "Process " + PROC_NAME + " not registered.", 
                         be.getMessage());
        }
        String[] output = project.getBuffer().toStringArray(Project.MSG_WARN);
        assertEquals(Arrays.toString(output), 1, output.length);
        output = project.getBuffer().toStringArray(Project.MSG_INFO);
        assertEquals(Arrays.toString(output), 5, output.length);
    }
    
    public void testExecuteNoFork() throws Exception {
        TestProject project = new TestProject();
        
        ProcessTester server = new ProcessTester();
        
        XhJavaBgTask task = new XhJavaBgTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.setProcessName(PROC_NAME);
        task.setFork(false);
        
        try {
            ProcessRegistry.getProcess(PROC_NAME);
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "Process " + PROC_NAME + " not registered.", 
                         be.getMessage());
        }
        try {
            project.enableConsoleCapturing(100, 101);
            task.execute();
            assertEquals("Process not registered", task, ProcessRegistry.getProcess(PROC_NAME));
            assertTrue("Process is not running", task.isRunning());
            assertTrue("Process is not running", server.passed());
            assertEqualsIgnoreCase("Wrong user dir", 
                                   System.getProperty("user.dir"), 
                                   server.getReceivedData());
            server.getSocket().close();
            // wait for process to terminate
            for (int i = 100; i > 0 && task.isRunning(); i--) {
                Thread.sleep(100);
            }
            assertTrue("Process is still running", !task.isRunning());
        } finally {
            project.disableConsoleCapturing();
        }
        
        try {
            ProcessRegistry.getProcess(PROC_NAME);
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "Process " + PROC_NAME + " not registered.", 
                         be.getMessage());
        }
        
        String[] output = project.getBuffer().toStringArray(101);
        assertEquals(Arrays.toString(output), 1, output.length);
        output = project.getBuffer().toStringArray(100);
        assertEquals(Arrays.toString(output), 5, output.length);
    }
    
    public void testKill() throws Exception {
        TestProject project = new TestProject();
        
        ProcessTester server = new ProcessTester();
        
        XhJavaBgTask task = new XhJavaBgTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.setProcessName(PROC_NAME);
        
        try {
            ProcessRegistry.getProcess(PROC_NAME);
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "Process " + PROC_NAME + " not registered.", 
                         be.getMessage());
        }
        task.execute();
        assertEquals("Process not registered", task, ProcessRegistry.getProcess(PROC_NAME));
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Process is not running", server.passed());
        assertEqualsIgnoreCase("Wrong user dir", 
                               System.getProperty("user.dir"), 
                               server.getReceivedData());
        task.kill();
        assertTrue("Process is still running", !task.isRunning());
        try {
            ProcessRegistry.getProcess(PROC_NAME);
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "Process " + PROC_NAME + " not registered.", 
                         be.getMessage());
        }
        String[] output = project.getBuffer().toStringArray(Project.MSG_WARN);
        assertEquals(Arrays.toString(output), 1, output.length);
        output = project.getBuffer().toStringArray(Project.MSG_INFO);
        assertEquals(Arrays.toString(output), 3, output.length);
        output = project.getBuffer().toStringArray(Project.MSG_VERBOSE);
        assertEquals("Waiting for Process to die after kill...", output[output.length - 1]);
    }

    public void testKillNotRunning() throws Exception {
        TestProject project = new TestProject();
        
        ProcessTester server = new ProcessTester();
        
        XhJavaBgTask task = new XhJavaBgTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        
        task.execute();
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Process is not running", server.passed());
        assertEqualsIgnoreCase("Wrong user dir", 
                               System.getProperty("user.dir"), 
                               server.getReceivedData());
        server.getSocket().close();
        // wait for process to terminate
        for (int i = 100; i > 0 && task.isRunning(); i--) {
            Thread.sleep(100);
        }
        task.kill();
        assertTrue("Process is still running", !task.isRunning());

        String[] output = project.getBuffer().toStringArray(Project.MSG_WARN);
        assertEquals(Arrays.toString(output), 1, output.length);
        output = project.getBuffer().toStringArray(Project.MSG_INFO);
        assertEquals(Arrays.toString(output), 5, output.length);
    }

    public void testPrekillTimeout() throws Exception {
        TestProject project = new TestProject();
        
        ProcessTester server = new ProcessTester();
        
        XhJavaBgTask task = new XhJavaBgTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.setPrekilltimeout(1);
        
        task.execute();
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Process is not running", server.passed());
        assertEqualsIgnoreCase("Wrong user dir", 
                               System.getProperty("user.dir"), 
                               server.getReceivedData());
        try {
            task.kill();
            fail("Expected BuildExcecption");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Timeout: killed the sub-process", be.getMessage());
        }
        assertTrue("Process is still running", !task.isRunning());

        String[] output = project.getBuffer().toStringArray(Project.MSG_WARN);
        assertEquals(Arrays.toString(output), 1, output.length);
        output = project.getBuffer().toStringArray(Project.MSG_INFO);
        assertEquals(Arrays.toString(output), 3, output.length);
        output = project.getBuffer().toStringArray(Project.MSG_ERR);
        assertEquals(Arrays.toString(output), 1, output.length);
        assertEquals("Timeout: killed the sub-process", output[0]);
    }

    public void testNegativePrekillTimeout() throws Exception {
        TestProject project = new TestProject();
        
        ProcessTester server = new ProcessTester();
        
        XhJavaBgTask task = new XhJavaBgTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.setPrekilltimeout(-1);
        
        task.execute();
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Process is not running", server.passed());
        assertEqualsIgnoreCase("Wrong user dir", 
                               System.getProperty("user.dir"), 
                               server.getReceivedData());
        task.kill();
        assertTrue("Process is still running", !task.isRunning());

        String[] output = project.getBuffer().toStringArray(Project.MSG_WARN);
        assertEquals(Arrays.toString(output), 1, output.length);
        output = project.getBuffer().toStringArray(Project.MSG_INFO);
        assertEquals(Arrays.toString(output), 3, output.length);
        output = project.getBuffer().toStringArray(Project.MSG_VERBOSE);
        assertEquals("Waiting for Process to die after kill...", output[output.length - 1]);
    }
    
    public void testKillTimeout() throws Exception {
        TestProject project = new TestProject();
        
        ProcessTester server = new ProcessTester();
        
        MockControl wdCtrl = MockClassControl.createControl(KillableExecuteWatchdog.class);
        KillableExecuteWatchdog wd = (KillableExecuteWatchdog)wdCtrl.getMock();
        wd.killProcess(true);

        XhJavaBgTask task = new TestXhJavaBgTask(wd);
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.setKilltimeout(1);
        
        wdCtrl.replay();
        task.execute();
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Process is not running", server.passed());
        assertEqualsIgnoreCase("Wrong user dir", 
                               System.getProperty("user.dir"), 
                               server.getReceivedData());
        try {
            task.kill();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Unable to kill process!", be.getMessage());
        }
        assertTrue("Process is not running", task.isRunning());
        server.getSocket().close();
        // wait for process to terminate
        for (int i = 100; i > 0 && task.isRunning(); i--) {
            Thread.sleep(100);
        }
        assertTrue("Process is still running", !task.isRunning());
        wdCtrl.verify();

        String[] output = project.getBuffer().toStringArray(Project.MSG_WARN);
        assertEquals(Arrays.toString(output), 2, output.length);
        assertEquals("Process is still running!", output[1]);
        output = project.getBuffer().toStringArray(Project.MSG_INFO);
        assertEquals(Arrays.toString(output), 5, output.length);
        output = project.getBuffer().toStringArray(Project.MSG_VERBOSE);
        assertEquals("Waiting for Process to die after kill...", output[output.length - 1]);
    }
    
    public void testAfterwards() throws Exception {
        TestProject project = new TestProject();
        
        ProcessTester server = new ProcessTester();
        
        MockControl ctrl = MockClassControl.createControl(Sequential.class);
        Sequential afterwards = (Sequential)ctrl.getMock();
        afterwards.execute();

        XhJavaBgTask task = new XhJavaBgTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.addAfterwards(afterwards);
        
        ctrl.replay();
        task.execute();
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Process is not running", server.passed());
        assertEqualsIgnoreCase("Wrong user dir", 
                               System.getProperty("user.dir"), 
                               server.getReceivedData());
        task.kill();
        assertTrue("Process is still running", !task.isRunning());
        ctrl.verify();

        String[] output = project.getBuffer().toStringArray(Project.MSG_WARN);
        assertEquals(Arrays.toString(output), 1, output.length);
        output = project.getBuffer().toStringArray(Project.MSG_INFO);
        assertEquals(Arrays.toString(output), 3, output.length);
        output = project.getBuffer().toStringArray(Project.MSG_VERBOSE);
        assertEquals("Waiting for Process to die after kill...", output[output.length - 1]);
    }
    
    public void testAfterwardsFails() throws Exception {
        TestProject project = new TestProject();
        
        ProcessTester server = new ProcessTester();
        
        MockControl ctrl = MockClassControl.createControl(Sequential.class);
        Sequential afterwards = (Sequential)ctrl.getMock();
        afterwards.execute();
        ctrl.setThrowable(new BuildException());

        XhJavaBgTask task = new XhJavaBgTask();
        task.setProject(project);
        task.setClassname(ProcessTester.class.getName());
        task.setClasspath(new Path(project, getClassPath()));
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.addAfterwards(afterwards);
        
        ctrl.replay();
        task.execute();
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Process is not running", server.passed());
        assertEqualsIgnoreCase("Wrong user dir", 
                               System.getProperty("user.dir"), 
                               server.getReceivedData());
        try {
            task.kill();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Nested task failed.", be.getMessage());
        }
        assertTrue("Process is still running", !task.isRunning());
        ctrl.verify();

        String[] output = project.getBuffer().toStringArray(Project.MSG_WARN);
        assertEquals(Arrays.toString(output), 2, output.length);
        assertEquals("Nested task failed: null", output[1]);
        output = project.getBuffer().toStringArray(Project.MSG_INFO);
        assertEquals(Arrays.toString(output), 3, output.length);
        output = project.getBuffer().toStringArray(Project.MSG_VERBOSE);
        assertEquals("Waiting for Process to die after kill...", output[output.length - 1]);
    }
    
    public void testExecuteFails() throws Exception {
        TestProject project = new TestProject();
        
        XhJavaBgTask task = new XhJavaBgTask();
        task.setProject(project);
        task.setClassname("org.bogus.NonExist");
        
        try {
            // depending how quick the java process starts, either execute will throw an
            // exception or it will be re-thrown in kill() after the process has terminated.
            task.execute();
            for (int i = 600; i > 0 && task.isRunning(); i--) {
                Thread.sleep(100);
            }
            task.kill();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Java returned: 1", be.getMessage());
        }

        String[] output = project.getBuffer().toStringArray(Project.MSG_WARN);
        assertEquals(Arrays.toString(output), 2, output.length);
        assertEquals("java.lang.NoClassDefFoundError: org/bogus/NonExist", output[0]);
        assertEquals("Exception in thread \"main\" ", output[1]);
        output = project.getBuffer().toStringArray(Project.MSG_INFO);
        assertEquals(Arrays.toString(output), 0, output.length);
    }
    
    public void testExecuteFailsNoFork() throws Exception {
        TestProject project = new TestProject();
        
        XhJavaBgTask task = new XhJavaBgTask();
        task.setProject(project);
        task.setClassname("org.bogus.NonExist");
        task.setFork(false);
        
        try {
            project.enableConsoleCapturing(100, 101);
            task.execute();
            for (int i = 6000; i > 0 && task.isRunning(); i--) {
                Thread.sleep(100);
            }
            task.kill();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                    "Could not find org.bogus.NonExist. Make sure you have it in your classpath", 
                    be.getMessage());
        } finally {
            project.disableConsoleCapturing();
        }
        
        //System.out.println(project.getBuffer().toString('\n', null));
        String[] output = project.getBuffer().toStringArray(101);
        assertEquals(Arrays.toString(output), 0, output.length);
        output = project.getBuffer().toStringArray(100);
        assertEquals(Arrays.toString(output), 0, output.length);
    }
    
    private static void assertEqualsIgnoreCase(String msg, String s1, String s2) {
        assertEquals(msg, s1.toLowerCase(), s2.toLowerCase());
    }
    
    private static class TestXhJavaBgTask extends XhJavaBgTask {
        private KillableExecuteWatchdog watchdog;
        public TestXhJavaBgTask(KillableExecuteWatchdog wd) {
            super();
            watchdog = wd;
        }
        protected KillableExecuteWatchdog getWatchdog() {
            return watchdog;
        }
    }
}
