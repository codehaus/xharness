package org.codehaus.xharness.tasks;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.types.Path;

import org.codehaus.xharness.log.LineBuffer;
import org.codehaus.xharness.procutil.KillableExecuteWatchdog;
import org.codehaus.xharness.procutil.ProcessRegistry;
import org.codehaus.xharness.testutil.ProcessTester;
import org.codehaus.xharness.types.EnvSet;
import org.codehaus.xharness.types.EnvironmentVariable;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class XhExecBgTaskTest extends TestCase {
    private static final String PROC_NAME = "myProcessName";
    private static final String JVM_DIR = new String(
            (System.getProperty("java.home") + "/bin")
            .replace('/', File.separatorChar).replace(
                     '\\', File.separatorChar));
    private static final String JVM_CMD = new String(
            "java" + (System.getProperty("os.name").toLowerCase().startsWith("win") ? ".exe" : ""));

    private static final String JVM = new String(
            JVM_DIR + File.separator + JVM_CMD);


    public XhExecBgTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = XhExecBgTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = XhExecBgTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(XhExecBgTaskTest.class);
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
        XhExecBgTask task = new XhExecBgTask();
        task.setProcessName(PROC_NAME);
        assertEquals("Wrong process name", PROC_NAME, task.getProcessName());
    }

    public void testExecute() throws Exception {
        Project project = new Project();

        ProcessTester server = new ProcessTester();

        XhExecBgTask task = new XhExecBgTask();
        task.setProject(project);
        task.setExecutable(JVM);
        task.createArg().setValue("-cp");
        task.createArg().setPath(new Path(project, getClassPath()));
        task.createArg().setLine(ProcessTester.class.getName());
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
        assertTrue("Server has not passed", server.passed());
        assertEquals("Wrong user dir", System.getProperty("user.dir"), server.getReceivedData());
        server.getSocket().close();
        // wait 1sec for process to terminate
        Thread.sleep(1000);
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

        XhExecBgTask task = new XhExecBgTask();
        task.setProject(project);
        task.setExecutable(JVM);
        task.createArg().setValue("-cp");
        task.createArg().setPath(new Path(project, getClassPath()));
        task.createArg().setLine(ProcessTester.class.getName());
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

        // wait 950ms for process to start
        Thread.sleep(950);
        assertEquals("Process not registered", task, ProcessRegistry.getProcess(PROC_NAME));
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Process is not running", server.passed());
        assertEquals("Wrong user dir", System.getProperty("user.dir"), server.getReceivedData());
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

        XhExecBgTask task = new XhExecBgTask();
        task.setProject(project);
        task.setExecutable(JVM);
        task.createArg().setValue("-cp");
        task.createArg().setPath(new Path(project, getClassPath()));
        task.createArg().setLine(ProcessTester.class.getName());
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");

        task.execute();
        // wait 250ms for process to start
        Thread.sleep(250);
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Server has not passed", server.passed());
        assertEquals("Wrong user dir", System.getProperty("user.dir"), server.getReceivedData());
        server.getSocket().close();
        // wait 500ms for process to terminate
        Thread.sleep(500);
        task.kill();
        assertTrue("Process is still running", !task.isRunning());
    }

    public void testPrekillTimeout() throws Exception {
        Project project = new Project();

        ProcessTester server = new ProcessTester();

        XhExecBgTask task = new XhExecBgTask();
        task.setProject(project);
        task.setExecutable(JVM);
        task.createArg().setValue("-cp");
        task.createArg().setPath(new Path(project, getClassPath()));
        task.createArg().setLine(ProcessTester.class.getName());
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.setPrekilltimeout(1);

        task.execute();
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Server has not passed", server.passed());
        assertEquals("Wrong user dir", System.getProperty("user.dir"), server.getReceivedData());
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

        XhExecBgTask task = new XhExecBgTask();
        task.setProject(project);
        task.setExecutable(JVM);
        task.createArg().setValue("-cp");
        task.createArg().setPath(new Path(project, getClassPath()));
        task.createArg().setLine(ProcessTester.class.getName());
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.setPrekilltimeout(-1);

        task.execute();
        // wait 250ms for process to start
        Thread.sleep(250);
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Server has not passed", server.passed());
        assertEquals("Wrong user dir", System.getProperty("user.dir"), server.getReceivedData());
        task.kill();
        assertTrue("Process is still running", !task.isRunning());
    }

    public void testKillTimeout() throws Exception {
        Project project = new Project();

        ProcessTester server = new ProcessTester();

        MockControl wdCtrl = MockClassControl.createControl(KillableExecuteWatchdog.class);
        KillableExecuteWatchdog wd = (KillableExecuteWatchdog)wdCtrl.getMock();
        wd.killProcess(true);

        XhExecBgTask task = new TestXhExecBgTask(wd);
        task.setProject(project);
        task.setExecutable(JVM);
        task.createArg().setValue("-cp");
        task.createArg().setPath(new Path(project, getClassPath()));
        task.createArg().setLine(ProcessTester.class.getName());
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.setKilltimeout(1);

        wdCtrl.replay();
        task.execute();
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Server has not passed", server.passed());
        assertEquals("Wrong user dir", System.getProperty("user.dir"), server.getReceivedData());
        try {
            task.kill();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Unable to kill process!", be.getMessage());
        }
        assertTrue("Process is not running", task.isRunning());
        server.getSocket().close();
        // wait 900ms for process to terminate
        Thread.sleep(900);
        assertTrue("Process is still running", !task.isRunning());
        wdCtrl.verify();
    }

    public void testAfterwards() throws Exception {
        Project project = new Project();

        ProcessTester server = new ProcessTester();

        MockControl ctrl = MockClassControl.createControl(Sequential.class);
        Sequential afterwards = (Sequential)ctrl.getMock();
        afterwards.execute();

        XhExecBgTask task = new XhExecBgTask();
        task.setProject(project);
        task.setExecutable(JVM);
        task.createArg().setValue("-cp");
        task.createArg().setPath(new Path(project, getClassPath()));
        task.createArg().setLine(ProcessTester.class.getName());
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.addAfterwards(afterwards);

        ctrl.replay();
        task.execute();
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Server has not passed", server.passed());
        assertEquals("Wrong user dir", System.getProperty("user.dir"), server.getReceivedData());
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

        XhExecBgTask task = new XhExecBgTask();
        task.setProject(project);
        task.setExecutable(JVM);
        task.createArg().setValue("-cp");
        task.createArg().setPath(new Path(project, getClassPath()));
        task.createArg().setLine(ProcessTester.class.getName());
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.addAfterwards(afterwards);

        ctrl.replay();
        task.execute();
        assertTrue("Process is not running", task.isRunning());
        assertTrue("Server has not passed", server.passed());
        assertEquals("Wrong user dir", System.getProperty("user.dir"), server.getReceivedData());
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

        XhExecBgTask task = new XhExecBgTask();
        task.setProject(project);
        task.setExecutable("bogus");

        try {
            // depending how quick the java process starts, either execute will throw an
            // exception or it will be re-thrown in kill() after the process has terminated.
            task.execute();
            for (int i = 100; i > 0 && task.isRunning(); i--) {
                Thread.sleep(100);
            }
            task.kill();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertContains("Wrong message",
                           "Execute failed: java.io.IOException: ",
                           be.getMessage());
        }
    }

    public void testUseNoLauncher() throws Exception {
        Project project = new Project();
        LineBuffer buffer = new LineBuffer();

        ProcessTester server = new ProcessTester();

        XhExecBgTask task = new XhExecBgTask();
        task.setProject(project);
        task.setExecutable(JVM);
        task.createArg().setValue("-cp");
        Path path = new Path(project, getClassPath());
        task.createArg().setPath(path);
        task.createArg().setLine(ProcessTester.class.getName());
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.setUseLauncher(false);
        task.enableLogging(buffer, 0, 1);

        try {
            task.execute();
            // wait 250ms for process to start
            Thread.sleep(250);
            assertTrue("Process is not running", task.isRunning());
            assertTrue("Process is not running", server.passed());
            assertEquals("Wrong user dir",
                         System.getProperty("user.dir"),
                         server.getReceivedData());
            task.kill();
            assertTrue("Process is still running", !task.isRunning());
            String cmdl = task.getCommandline();
            assertTrue("Wrong commandline: " + cmdl, cmdl.indexOf("async_exec_launcher") < 0);
            assertContains("Wrong commandline", JVM, cmdl);
            assertContains("Wrong commandline", path.toString(), cmdl);
            assertContains("Wrong commandline", ProcessTester.class.getName(), cmdl);
            assertContains("Wrong commandline", " -s user.dir", cmdl);
            assertEquals("Wrong stderr", "Welcome stderr!", buffer.toString(1));
            String stdout = buffer.toString(0);
            assertTrue("Wrong stdout", stdout.startsWith("Welcome stdout!"));
        } finally {
//            System.out.println(task.getCommandline());
//            System.out.println(buffer.toString('\n', null));
        }
    }

    public void testUseLauncher() throws Exception {
        Project project = new Project();
        project.addBuildListener(new BuildListener() {
            public final void buildStarted(BuildEvent event) {
            }
            public final void buildFinished(BuildEvent event) {
            }
            public final void targetStarted(BuildEvent event) {
            }
            public final void targetFinished(BuildEvent event) {
            }
            public final void taskStarted(BuildEvent event) {
            }
            public final void taskFinished(BuildEvent event) {
            }
            public final void messageLogged(BuildEvent event) {
//                System.out.println("> " + event.getMessage());
            }
        });
        LineBuffer buffer = new LineBuffer();

        EnvironmentVariable var = new EnvironmentVariable();
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            var.setKey("Path");
        } else {
            var.setKey("PATH");
        }
        var.setPath(new Path(project, new File("./bin").getCanonicalPath()));
        var.setPrepend(true);

//        EnvironmentVariable var2 = new EnvironmentVariable();
//        var2.setKey("ASYNC_EXEC_DEBUG");
//        var2.setValue("abc.txt");

        EnvSet envset = new EnvSet();
        envset.setLoadenvironment(true);
        envset.addEnv(var);
//        envset.addEnv(var2);

        ProcessTester server = new ProcessTester();

        XhExecBgTask task = new XhExecBgTask();
        task.setProject(project);
        task.setExecutable(JVM);
        task.createArg().setValue("-cp");
        Path classpath = new Path(project, getClassPath());
        task.createArg().setPath(classpath);
        task.createArg().setLine(ProcessTester.class.getName());
        task.createArg().setLine("-p " + server.getPort());
        task.createArg().setLine("-s user.dir");
        task.createArg().setLine("-t 60");
        task.setUseLauncher(true);
        task.addEnvset(envset);
        task.enableLogging(buffer, 0, 1);

        try {
            task.execute();
            // wait 2sec for process to start
            Thread.sleep(2000);
            assertTrue("Process is not running", task.isRunning());
            assertTrue("Process is not running", server.passed());
            assertEquals("Wrong user dir",
                         System.getProperty("user.dir"),
                         server.getReceivedData());
            Thread.sleep(500);
            task.kill();
            assertTrue("Process is still running", !task.isRunning());
//            assertContains("Wrong commandline", "async_exec_launcher", task.getCommandline());
//            assertEquals("Wrong stderr", "Welcome stderr!", buffer.toString(1));
//            String stdout = buffer.toString(0);
//            assertTrue("Wrong stdout", stdout.startsWith("Welcome stdout!"));
        } finally {
//            System.out.println(task.getCommandline());
//            System.out.println(buffer.toString('\n', null));
        }
    }

    private static void assertContains(String msg, String s1, String s2) {
        assertTrue(msg + " <" + s1 + "> not found in <" + s2 + ">", s2.indexOf(s1) >= 0);
    }

    private static class TestXhExecBgTask extends XhExecBgTask {
        private KillableExecuteWatchdog watchdog;
        public TestXhExecBgTask(KillableExecuteWatchdog wd) {
            super();
            watchdog = wd;
        }
        protected KillableExecuteWatchdog getWatchdog() {
            return watchdog;
        }
    }
}
