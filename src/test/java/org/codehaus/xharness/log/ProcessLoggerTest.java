package org.codehaus.xharness.log;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;

import org.codehaus.xharness.testutil.ResultFormatterMatcher;
import org.codehaus.xharness.testutil.TestProcessTask;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ProcessLoggerTest extends TestCase {
    public ProcessLoggerTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ProcessLoggerTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ProcessLoggerTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(ProcessLoggerTest.class);
    }
    
    public void testCtorAndAcors() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        UnknownElement uke = new UnknownElement("eggs");

        prCtrl.replay();
        trCtrl.replay();
        
        ProcessLogger logger = new ProcessLogger(registry, uke, "foo", "bar");
        
        assertEquals("Wrong registry", registry, logger.getRegistry());
        assertEquals("Wrong task", uke, logger.getTask());
        assertEquals("Wrong name", "foo", logger.getName());
        assertEquals("Wrong parent", "bar", logger.getParentName());
        assertEquals("Wrong full name", "bar/foo", logger.getFullName());
        assertNull("Wrong reference", logger.getReference());
        assertEquals("Wrong Id", 101, logger.getId());
        assertEquals("Wrong Uke", null, logger.getUnknownElement());
        logger.setUnknownElement(uke);
        assertEquals("Wrong Uke", uke, logger.getUnknownElement());
        assertTrue("Buffer shouldn't be null", logger.getLineBuffer() != null);
        assertEquals(LogPriority.STDOUT, logger.getLineBuffer().getDefaultPriority());
        assertEquals("Wrong owner", "", logger.getOwner());
        assertEquals("Wrong command", "", logger.getCommand());
        assertEquals("Wrong retval", 0, logger.getRetVal());
        assertEquals("Wrong Failure", null, logger.getFailure());
        Exception failure = new Exception("blah");
        logger.setFailure(failure);
        assertEquals("Wrong Failure", failure, logger.getFailure());
        assertEquals("Wrong Failure", failure.getMessage(), logger.getFailure().getMessage());

        prCtrl.verify();
        trCtrl.verify();
    }
    
    public void testMessageLogged() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)tkCtrl.getMock();

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getMessage();
        evCtrl.setReturnValue("error", 2);
        evCtrl.setReturnValue("foo", 2);
        evCtrl.setReturnValue("stdout", 2);
        evCtrl.setReturnValue("stderr", 2);
        evCtrl.setReturnValue("bar", 2);
        event.getTask();
        evCtrl.setReturnValue(task, 7);
        event.getPriority();
        evCtrl.setReturnValue(Project.MSG_ERR);
        evCtrl.setReturnValue(Project.MSG_INFO);
        evCtrl.setReturnValue(Project.MSG_INFO);
        evCtrl.setReturnValue(Project.MSG_WARN);
        evCtrl.setReturnValue(Project.MSG_WARN);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();
        
        ProcessLogger logger = new ProcessLogger(registry, task, "foo", "bar");
        logger.getLineBuffer().logLine(LogPriority.STDOUT, "stdout");
        logger.getLineBuffer().logLine(LogPriority.STDERR, "stderr");
        logger.messageLogged(event);
        logger.messageLogged(event);
        logger.messageLogged(event);
        logger.messageLogged(event);
        logger.messageLogged(event);
        
        assertEquals("Wrong log", "error", logger.getLineBuffer().toString(LogPriority.ERROR));
        assertEquals("Wrong log", "foo", logger.getLineBuffer().toString(LogPriority.INFO));
        assertEquals("Wrong log", "bar", logger.getLineBuffer().toString(LogPriority.WARNING));
        assertEquals("Wrong log", "stdout", logger.getLineBuffer().toString(LogPriority.STDOUT));
        assertEquals("Wrong log", "stderr", logger.getLineBuffer().toString(LogPriority.STDERR));
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }
    
    public void testMessageLoggedLoggableProcess() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        TestProcessTask task = new TestProcessTask();

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getMessage();
        evCtrl.setReturnValue("foo", 2);
        event.getTask();
        evCtrl.setReturnValue(task, 3);
        event.getPriority();
        evCtrl.setReturnValue(Project.MSG_INFO);
        
        prCtrl.replay();
        trCtrl.replay();
        evCtrl.replay();
        
        ProcessLogger logger = new ProcessLogger(registry, task, "foo", "bar");
        logger.messageLogged(event);
        
        assertEquals("Wrong LineBuffer passed to Task", logger.getLineBuffer(), task.getBuffer());
        assertEquals("Wrong Out prio passed to Task", LogPriority.STDOUT, task.getOutPrio());
        assertEquals("Wrong Err prio passed to Task", LogPriority.STDERR, task.getErrPrio());
        assertEquals("Wrong log", "foo", logger.getLineBuffer().toString(LogPriority.INFO));
        
        prCtrl.verify();
        trCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskFinished() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)tkCtrl.getMock();
        task.getRuntimeConfigurableWrapper();
        tkCtrl.setReturnValue(new RuntimeConfigurable(new Object(), "foo"));

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 3);
        event.getException();
        evCtrl.setReturnValue(null);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();
        
        ProcessLogger logger = new ProcessLogger(registry, task, "foo", "bar");
        logger.taskFinished(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }
    
    public void testDeferredShutdown() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl rfCtrl = MockClassControl.createControl(ResultFormatter.class);
        rfCtrl.setDefaultMatcher(new ResultFormatterMatcher());
        ResultFormatter formatter = (ResultFormatter)rfCtrl.getMock();

        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project, 2);
        registry.getFormatter();
        trCtrl.setReturnValue(formatter);
        
        MockControl tkCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)tkCtrl.getMock();

        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        
        ProcessLogger logger = new ProcessLogger(registry, task, "foo", "bar");
        formatter.writeResults(logger, Result.PASSED, "", 0L);
        rfCtrl.setVoidCallable();
        rfCtrl.replay();

        logger.deferredShutdown();
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        rfCtrl.verify();
    }

    public void testDeferredShutdownBgProcess() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl rfCtrl = MockClassControl.createControl(ResultFormatter.class);
        rfCtrl.setDefaultMatcher(new ResultFormatterMatcher());
        ResultFormatter formatter = (ResultFormatter)rfCtrl.getMock();

        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project, 2);
        registry.getFormatter();
        trCtrl.setReturnValue(formatter);
        
        TestProcessTask task = new TestProcessTask();

        prCtrl.replay();
        trCtrl.replay();
        
        ProcessLogger logger = new ProcessLogger(registry, task, "foo", "bar");
        formatter.writeResults(logger, Result.PASSED, "", 0L);
        rfCtrl.setVoidCallable();
        rfCtrl.replay();

        logger.deferredShutdown();
        assertTrue("Kill not called on BgProcess", task.killCalled());
        
        prCtrl.verify();
        trCtrl.verify();
        rfCtrl.verify();
    }

    public void testDeferredShutdownBgProcessWithException() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl rfCtrl = MockClassControl.createControl(ResultFormatter.class);
        rfCtrl.setDefaultMatcher(new ResultFormatterMatcher());
        ResultFormatter formatter = (ResultFormatter)rfCtrl.getMock();

        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project, 2);
        registry.getFormatter();
        trCtrl.setReturnValue(formatter);
        
        TestProcessTask task = new TestProcessTask(new BuildException("foo"));

        prCtrl.replay();
        trCtrl.replay();
        
        ProcessLogger logger = new ProcessLogger(registry, task, "foo", "bar");
        formatter.writeResults(logger, Result.FAILED, "foo", 0L);
        rfCtrl.setVoidCallable();
        rfCtrl.replay();

        try {
            logger.deferredShutdown();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "foo", be.getMessage());
        }
        assertTrue("Kill not called on BgProcess", task.killCalled());
        
        prCtrl.verify();
        trCtrl.verify();
        rfCtrl.verify();
    }
}
