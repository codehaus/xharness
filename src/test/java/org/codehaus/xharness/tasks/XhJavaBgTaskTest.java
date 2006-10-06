package org.codehaus.xharness.tasks;

import java.net.URL;
import java.net.URLClassLoader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.types.Path;

import org.codehaus.xharness.procutil.KillableExecuteWatchdog;
import org.codehaus.xharness.procutil.ProcessRegistry;
import org.codehaus.xharness.testutil.ProcessTester;

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
        Project project = new Project();
        
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
        // wait 500ms for process to terminate
        Thread.sleep(500);
        assertTrue("Process is still running", !task.isRunning());
        try {
            ProcessRegistry.getProcess(PROC_NAME);
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "Process " + PROC_NAME + " not registered.", 
                         be.getMessage());
        }
    }
    
    public void testKill() throws Exception {
        Project project = new Project();
        
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
    }

    public void testKillNotRunning() throws Exception {
        Project project = new Project();
        
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
        // wait 500 for process to terminate
        Thread.sleep(500);
        task.kill();
        assertTrue("Process is still running", !task.isRunning());
    }

    public void testPrekillTimeout() throws Exception {
        Project project = new Project();
        
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
    }

    public void testNegativePrekillTimeout() throws Exception {
        Project project = new Project();
        
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
    }
    
    public void testKillTimeout() throws Exception {
        Project project = new Project();
        
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
        // wait 500ms for process to terminate
        Thread.sleep(500);
        assertTrue("Process is still running", !task.isRunning());
        wdCtrl.verify();
    }
    
    public void testAfterwards() throws Exception {
        Project project = new Project();
        
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
    }
    
    public void testAfterwardsFails() throws Exception {
        Project project = new Project();
        
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
    }
    
    public void testExecuteFails() throws Exception {
        Project project = new Project();
        
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
            assertContains("Wrong message", 
                           "Java returned: 1", 
                           be.getMessage());
        }
    }
    
    private static void assertEqualsIgnoreCase(String msg, String s1, String s2) {
        assertEquals(msg, s1.toLowerCase(), s2.toLowerCase());
    }
    
    private static void assertContains(String msg, String s1, String s2) {
        assertTrue(msg + " <" + s1 + "> not found in <" + s2 + ">", s2.indexOf(s1) >= 0);
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
