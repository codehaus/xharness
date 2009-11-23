package org.codehaus.xharness.log;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.Java;

import org.codehaus.xharness.exceptions.AssertionWarningException;
import org.codehaus.xharness.exceptions.TestSkippedException;
import org.codehaus.xharness.tasks.ServiceDef;
import org.codehaus.xharness.tasks.ServiceGroupTask;
import org.codehaus.xharness.tasks.ServiceInstance;
import org.codehaus.xharness.tasks.TestCaseTask;
import org.codehaus.xharness.tasks.TestGroupTask;
import org.codehaus.xharness.tasks.XhExecTask;
import org.codehaus.xharness.tasks.XharnessTask;
import org.codehaus.xharness.testutil.ResultFormatterMatcher;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TaskLoggerTest extends TestCase {
    public TaskLoggerTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = TaskLoggerTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = TaskLoggerTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(TaskLoggerTest.class);
    }
    
    public void testCtorAndAcors() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101, 2);
        registry.getProject();
        trCtrl.setReturnValue(project, 2);
        
        UnknownElement uke = new UnknownElement("eggs");

        prCtrl.replay();
        trCtrl.replay();
        
        TaskLogger logger = new TaskLogger(registry, uke, "foo", "bar", "spam");
        
        assertEquals("Wrong registry", registry, logger.getRegistry());
        assertEquals("Wrong task", uke, logger.getTask());
        assertEquals("Wrong name", "foo", logger.getName());
        assertEquals("Wrong parent", "bar", logger.getParentName());
        assertEquals("Wrong full name", "bar/foo", logger.getFullName());
        assertEquals("Wrong reference", "spam", logger.getReference());
        assertEquals("Wrong Id", 101, logger.getId());
        assertEquals("Wrong Uke", null, logger.getUnknownElement());
        logger.setUnknownElement(uke);
        assertEquals("Wrong Uke", uke, logger.getUnknownElement());
        assertTrue("Buffer shouldn't be null", logger.getLineBuffer() != null);
        assertEquals(LogPriority.INFO, logger.getLineBuffer().getDefaultPriority());
        assertEquals("Wrong owner", "", logger.getOwner());
        assertEquals("Wrong command", "", logger.getCommand());
        assertEquals("Wrong retval", 0, logger.getRetVal());
        assertEquals("Wrong Failure", null, logger.getFailure());
        Exception failure = new Exception("blah");
        logger.setFailure(failure);
        assertEquals("Wrong Failure", failure, logger.getFailure());
        assertEquals("Wrong Failure", failure.getMessage(), logger.getFailure().getMessage());
        
        TaskLogger logger2 = new TaskLogger(registry, uke, "foo", null, null, 123);
        
        assertEquals("Wrong name", "foo", logger2.getName());
        assertEquals("Wrong parent", null, logger2.getParentName());
        assertEquals("Wrong full name", "foo", logger2.getFullName());
        assertEquals("Wrong reference", null, logger2.getReference());
        assertTrue("Buffer shouldn't be null", logger2.getLineBuffer() != null);
        assertEquals(123, logger2.getLineBuffer().getDefaultPriority());

        prCtrl.verify();
        trCtrl.verify();
    }
    
    public void testGetOwner() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(TestCaseTask.class);
        TestCaseTask task = (TestCaseTask)tkCtrl.getMock();
        task.getOwner();
        tkCtrl.setReturnValue("Che Guevara");

        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        
        TaskLogger logger = new TaskLogger(registry, task, "foo", "bar", "spam");
        assertEquals("Wrong Owner", "Che Guevara", logger.getOwner());
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
    }
    
    public void testGetCommand() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(XhExecTask.class);
        XhExecTask task = (XhExecTask)tkCtrl.getMock();
        task.getCommandline();
        tkCtrl.setReturnValue("Mahatma Ghandi");

        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        
        TaskLogger logger = new TaskLogger(registry, task, "foo", "bar", "spam");
        assertEquals("Wrong Commandline", "Mahatma Ghandi", logger.getCommand());
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
    }
    
    public void testGetRetVal() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(XhExecTask.class);
        XhExecTask task = (XhExecTask)tkCtrl.getMock();
        task.getReturnValue();
        tkCtrl.setReturnValue(321);

        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        
        TaskLogger logger = new TaskLogger(registry, task, "foo", "bar", "spam");
        assertEquals("Wrong Commandline", 321, logger.getRetVal());
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
    }
    
    public void testEmptyBuildListenerImplMethods() throws Exception {
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
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();
        
        TaskLogger logger = new TaskLogger(registry, task, "foo", "bar", "spam");
        logger.buildStarted(null);
        logger.buildFinished(null);
        logger.targetStarted(null);
        logger.targetFinished(null);
        logger.taskStarted(null);
        logger.taskFinished(null);
        logger.messageLogged(null);
        logger.buildStarted(event);
        logger.buildFinished(event);
        logger.targetStarted(event);
        logger.targetFinished(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskStarted() throws Exception {
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
        event.getTask();
        evCtrl.setReturnValue(task, 2);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();
        
        TaskLogger logger = new TaskLogger(registry, task, "foo", "bar", "spam");
        logger.taskStarted(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskFinishedPassed() throws Exception {
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
        
        TaskLogger logger1 = new TaskLogger(registry, task, "foo", "bar", "spam");
        formatter.writeResults(logger1, Result.PASSED, "", 0L);
        rfCtrl.setVoidCallable();
        rfCtrl.replay();

        logger1.taskFinished(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
        rfCtrl.verify();
    }
    
    public void testTaskFinishedSkipped() throws Exception {
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
        task.getRuntimeConfigurableWrapper();
        tkCtrl.setReturnValue(new RuntimeConfigurable(new Object(), "foo"));

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 3);
        event.getException();
        evCtrl.setReturnValue(new TestSkippedException("blah", false));
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();
        
        TaskLogger logger1 = new TaskLogger(registry, task, "foo", "bar", "spam");
        formatter.writeResults(logger1, Result.SKIPPED, "blah", 0L);
        rfCtrl.setVoidCallable();
        rfCtrl.replay();

        logger1.taskFinished(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
        rfCtrl.verify();
    }
    
    public void testTaskFinishedSkippedByPattern() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project, 2);
        
        MockControl tkCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)tkCtrl.getMock();
        task.getRuntimeConfigurableWrapper();
        tkCtrl.setReturnValue(new RuntimeConfigurable(new Object(), "foo"));

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 3);
        event.getException();
        evCtrl.setReturnValue(new TestSkippedException("blah", true));
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();
        
        TaskLogger logger1 = new TaskLogger(registry, task, "foo", "bar", "spam");

        logger1.taskFinished(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskFinishedWarning() throws Exception {
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
        task.getRuntimeConfigurableWrapper();
        tkCtrl.setReturnValue(new RuntimeConfigurable(new Object(), "foo"));

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 3);
        event.getException();
        evCtrl.setReturnValue(new AssertionWarningException());
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();
        
        TaskLogger logger1 = new TaskLogger(registry, task, "foo", "bar", "spam");
        formatter.writeResults(logger1, Result.WARNING, "Warning (unknown reason)", 0L);
        rfCtrl.setVoidCallable();
        rfCtrl.replay();

        logger1.taskFinished(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
        rfCtrl.verify();
    }
    
    public void testTaskFinishedError1() throws Exception {
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
        task.getRuntimeConfigurableWrapper();
        tkCtrl.setReturnValue(new RuntimeConfigurable(new Object(), "foo"));

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 3);
        event.getException();
        evCtrl.setReturnValue(new BuildException("arse"));
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();
        
        TaskLogger logger1 = new TaskLogger(registry, task, "foo", "bar", "spam");
        formatter.writeResults(logger1, Result.FAILED, "arse", 0L);
        rfCtrl.setVoidCallable();
        rfCtrl.replay();

        logger1.taskFinished(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
        rfCtrl.verify();
    }
    
    public void testTaskFinishedError2() throws Exception {
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
        task.getRuntimeConfigurableWrapper();
        tkCtrl.setReturnValue(new RuntimeConfigurable(new Object(), "foo"));

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 3);
        event.getException();
        evCtrl.setReturnValue(new Exception());
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();
        
        TaskLogger logger1 = new TaskLogger(registry, task, "foo", "bar", "spam");
        formatter.writeResults(logger1, Result.FAILED, "Failed (unknown reason)", 0L);
        rfCtrl.setVoidCallable();
        rfCtrl.replay();

        logger1.taskFinished(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
        rfCtrl.verify();
    }
    
    public void testTaskFinishedDetermineActualTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl rfCtrl = MockClassControl.createControl(ResultFormatter.class);
        rfCtrl.setDefaultMatcher(MockClassControl.ALWAYS_MATCHER);
        ResultFormatter formatter = (ResultFormatter)rfCtrl.getMock();
        formatter.writeResults(null, 0, null, 0L);
        rfCtrl.setVoidCallable(2);

        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project, 3);
        registry.getFormatter();
        trCtrl.setReturnValue(formatter, 2);
        
        Task task1 = new TestTask();

        RuntimeConfigurable wrapper = new RuntimeConfigurable(task1, "foo");

        MockControl tkCtrl2 = MockClassControl.createControl(Task.class);
        Task task2 = (Task)tkCtrl2.getMock();
        task2.getRuntimeConfigurableWrapper();
        tkCtrl2.setReturnValue(wrapper, 2);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task2, 5);
        event.getException();
        evCtrl.setReturnValue(new Exception("eggs"), 2);
        
        prCtrl.replay();
        rfCtrl.replay();
        trCtrl.replay();
        tkCtrl2.replay();
        evCtrl.replay();
        
        TaskLogger logger = new TaskLogger(registry, task2, "foo", "bar", "spam");
        logger.taskFinished(event);
        assertEquals("Wrong task", task1, logger.getTask());
        logger.taskFinished(event);

        prCtrl.verify();
        rfCtrl.verify();
        trCtrl.verify();
        tkCtrl2.verify();
        evCtrl.verify();
    }

    public void testTaskFinishedNotMyTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        RuntimeConfigurable wrapper1 = new RuntimeConfigurable(new Object(), "foo");
        MockControl tkCtrl1 = MockClassControl.createControl(Task.class);
        Task task1 = (Task)tkCtrl1.getMock();
        task1.getRuntimeConfigurableWrapper();
        tkCtrl1.setReturnValue(wrapper1);

        RuntimeConfigurable wrapper2 = new RuntimeConfigurable(new Object(), "bar");
        MockControl tkCtrl2 = MockClassControl.createControl(Task.class);
        Task task2 = (Task)tkCtrl2.getMock();
        task2.getRuntimeConfigurableWrapper();
        tkCtrl2.setReturnValue(wrapper2);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task1, 2);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl1.replay();
        tkCtrl2.replay();
        evCtrl.replay();
        
        TaskLogger logger = new TaskLogger(registry, task2, "foo", "bar", "spam");
        logger.taskFinished(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl1.verify();
        tkCtrl2.verify();
        evCtrl.verify();
    }
    
    public void testMessageLoggedNoMessage() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getMessage();
        evCtrl.setReturnValue(null);
        
        prCtrl.replay();
        trCtrl.replay();
        evCtrl.replay();
        
        TaskLogger logger = new TaskLogger(registry, new TestTask(), "foo", "bar", "spam");
        logger.messageLogged(event);
        
        prCtrl.verify();
        trCtrl.verify();
        evCtrl.verify();
    }
    
    
    public void testMessageLoggedNoTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getMessage();
        evCtrl.setReturnValue("blah");
        event.getTask();
        evCtrl.setReturnValue(null);
        
        prCtrl.replay();
        trCtrl.replay();
        evCtrl.replay();
        
        TaskLogger logger = new TaskLogger(registry, new TestTask(), "foo", "bar", "spam");
        logger.messageLogged(event);
        
        prCtrl.verify();
        trCtrl.verify();
        evCtrl.verify();
    }

    public void testMessageLoggedNotMyTask() throws Exception {
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
        event.getMessage();
        evCtrl.setReturnValue("blah");
        event.getTask();
        evCtrl.setReturnValue(task);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();
        
        TaskLogger logger = new TaskLogger(registry, new TestTask(), "foo", "bar", "spam");
        logger.messageLogged(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
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
        evCtrl.setReturnValue("warning", 2);
        evCtrl.setReturnValue("info", 2);
        evCtrl.setReturnValue("verbose", 2);
        evCtrl.setReturnValue("debug", 2);
        evCtrl.setReturnValue("debug", 2);
        event.getTask();
        evCtrl.setReturnValue(task, 8);
        event.getPriority();
        evCtrl.setReturnValue(Project.MSG_ERR);
        evCtrl.setReturnValue(Project.MSG_WARN);
        evCtrl.setReturnValue(Project.MSG_INFO);
        evCtrl.setReturnValue(Project.MSG_VERBOSE);
        evCtrl.setReturnValue(Project.MSG_DEBUG);
        evCtrl.setReturnValue(1234);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();
        
        TaskLogger logger = new TaskLogger(registry, task, "foo", "bar", "spam");
        logger.messageLogged(event);
        logger.messageLogged(event);
        logger.messageLogged(event);
        logger.messageLogged(event);
        logger.messageLogged(event);
        logger.messageLogged(event);
        
        assertEquals("Wrong log", "error", logger.getLineBuffer().toString(LogPriority.ERROR));
        assertEquals("Wrong log", "warning", logger.getLineBuffer().toString(LogPriority.WARNING));
        assertEquals("Wrong log", "info", logger.getLineBuffer().toString(LogPriority.INFO));
        assertEquals("Wrong log", "verbose", logger.getLineBuffer().toString(LogPriority.VERBOSE));
        assertEquals("Wrong log", 
                     "debug\ndebug", 
                     logger.getLineBuffer().toString(LogPriority.DEBUG));
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }
    
    public void testGetTaskType() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101, 11);
        registry.getProject();
        trCtrl.setReturnValue(project, 11);
        
        prCtrl.replay();
        trCtrl.replay();
        
        TaskLogger logger;
        logger = new TaskLogger(registry, new TestTask(), "foo", "bar", "spam");
        assertEquals("Wrong Result", Result.OTHER_TASK, logger.getTaskType());

        logger = new TaskLogger(registry, new ExecTask(), "foo", "bar", "spam");
        assertEquals("Wrong Result", Result.PROCESS_TASK, logger.getTaskType());

        logger = new TaskLogger(registry, new Java(), "foo", "bar", "spam");
        assertEquals("Wrong Result", Result.PROCESS_TASK, logger.getTaskType());

        logger = new TaskLogger(registry, new ServiceDef(), "foo", "bar", "spam");
        assertEquals("Wrong Result", Result.SERVICE, logger.getTaskType());

        logger = new TaskLogger(registry, new ServiceInstance(), "foo", "bar", "spam");
        assertEquals("Wrong Result", Result.SERVICE, logger.getTaskType());

        logger = new TaskLogger(registry, new TestGroupTask(), "foo", "bar", "spam");
        assertEquals("Wrong Result", Result.TESTGROUP, logger.getTaskType());

        logger = new TaskLogger(registry, new TestCaseTask(), "foo", "bar", "spam");
        assertEquals("Wrong Result", Result.TESTCASE, logger.getTaskType());

        logger = new TaskLogger(registry, new XharnessTask(), "foo", "bar", "spam");
        assertEquals("Wrong Result", Result.XHARNESS, logger.getTaskType());

        Task task = new ServiceGroupTask();
        task.setTaskName("start");
        logger = new TaskLogger(registry, task, "foo", "bar", "spam");
        assertEquals("Wrong Result", Result.START, logger.getTaskType());

        task.setTaskName("verify");
        logger = new TaskLogger(registry, task, "foo", "bar", "spam");
        assertEquals("Wrong Result", Result.VERIFY, logger.getTaskType());

        task.setTaskName("stop");
        logger = new TaskLogger(registry, task, "foo", "bar", "spam");
        assertEquals("Wrong Result", Result.STOP, logger.getTaskType());

        prCtrl.verify();
        trCtrl.verify();
    }

    private static class TestTask extends Task {
    }
}
