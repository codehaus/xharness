package org.codehaus.xharness.log;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.MacroDef;
import org.apache.tools.ant.taskdefs.MacroInstance;
import org.apache.tools.ant.taskdefs.Parallel;
import org.apache.tools.ant.taskdefs.Sequential;

import org.codehaus.xharness.tasks.AssertTask;
import org.codehaus.xharness.tasks.IncludeTask;
import org.codehaus.xharness.tasks.ServiceDef;
import org.codehaus.xharness.tasks.ServiceInstance;
import org.codehaus.xharness.tasks.SkipTask;
import org.codehaus.xharness.tasks.TestGroupTask;
import org.codehaus.xharness.testutil.ResultFormatterMatcher;
import org.codehaus.xharness.testutil.TestProcessTask;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestLoggerTest extends TestCase {
    public TestLoggerTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = TestLoggerTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = TestLoggerTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(TestLoggerTest.class);
    }

    public void testDeActivation() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        trCtrl.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project);
        registry.setCurrentTest(null);
        trCtrl.setVoidCallable(2);

        MockControl tkCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)tkCtrl.getMock();

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar");
        parent.deactivate(false);
        tlCtrl.setVoidCallable(2);
        parent.activate();

        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", parent);
        assertTrue("Logger not active", logger.isActive());
        logger.deactivate(false);
        assertTrue("Logger still active", !logger.isActive());
        logger.activate();
        assertTrue("Logger not active", logger.isActive());
        logger.deactivate(true);
        assertTrue("Logger still active", !logger.isActive());
        assertEquals("Wrong parent", parent, logger.getParent());

        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
    }

    public void testDeActivationNoParent() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        trCtrl.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project);
        registry.setCurrentTest(null);
        trCtrl.setVoidCallable(2);

        MockControl tkCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)tkCtrl.getMock();

        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", null, null, null);
        assertTrue("Logger not active", logger.isActive());
        logger.deactivate(false);
        assertTrue("Logger still active", !logger.isActive());
        logger.activate();
        assertTrue("Logger not active", logger.isActive());
        logger.deactivate(true);
        assertTrue("Logger still active", !logger.isActive());
        assertNull("Parent should be null", logger.getParent());

        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
    }

    public void testTaskStartedInactive() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)tkCtrl.getMock();

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);

        prCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", null, null, null);
        logger.deactivate(false);
        logger.taskStarted(event);

        prCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedUnknownElement() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blah", 2);

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement uke = (UnknownElement)ukeCtrl.getMock();
        uke.maybeConfigure();
        uke.getTask();
        ukeCtrl.setReturnValue(task, 2);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(uke, 2);

        prCtrl.replay();
        tkCtrl.replay();
        ukeCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, uke, "foo", null, null, null);
        logger.taskStarted(event);
        TaskLogger child = logger.getTask("blah");
        assertEquals("Wrong child logger",
                     TaskLogger.class.getName(),
                     child.getClass().getName());
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());
        assertEquals("Wrong UnknownElement", uke, child.getUnknownElement());
        assertEquals("Wrong Task", task, child.getTask());

        prCtrl.verify();
        tkCtrl.verify();
        ukeCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedUnknownElementWithException() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl = MockClassControl.createNiceControl(Task.class);
        Task task = (Task)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blah", 2);

        tkCtrl.replay();

        RuntimeConfigurable wrapper = new RuntimeConfigurable(task, "blubb");

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement uke = (UnknownElement)ukeCtrl.getMock();
        uke.maybeConfigure();
        ukeCtrl.setThrowable(new BuildException("bang"));
        uke.getWrapper();
        ukeCtrl.setReturnValue(wrapper);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(uke, 2);

        prCtrl.replay();
        ukeCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, uke, "foo", null, null, null);
        logger.taskStarted(event);
        TaskLogger child = logger.getTask("blah");
        assertEquals("Wrong child logger",
                     TaskLogger.class.getName(),
                     child.getClass().getName());
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());
        assertEquals("Wrong UnknownElement", uke, child.getUnknownElement());
        assertEquals("Wrong Task", task, child.getTask());

        prCtrl.verify();
        tkCtrl.verify();
        ukeCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedUnknownElementWithExceptionAndNoWrappedTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        RuntimeConfigurable wrapper = new RuntimeConfigurable(new Object(), "blubb");

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement uke = (UnknownElement)ukeCtrl.getMock();
        uke.maybeConfigure();
        ukeCtrl.setThrowable(new BuildException("bang"));
        uke.getWrapper();
        ukeCtrl.setReturnValue(wrapper);
        uke.getTaskName();
        ukeCtrl.setReturnValue("blah", 2);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(uke, 2);

        prCtrl.replay();
        ukeCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, uke, "foo", null, null, null);
        logger.taskStarted(event);
        TaskLogger child = logger.getTask("blah");
        assertEquals("Wrong child logger",
                     TaskLogger.class.getName(),
                     child.getClass().getName());
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());
        assertEquals("Wrong UnknownElement", uke, child.getUnknownElement());
        assertEquals("Wrong Task", uke, child.getTask());

        prCtrl.verify();
        ukeCtrl.verify();
        evCtrl.verify();
    }


    public void testTaskStartedUnknownElementWithNullTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement uke = (UnknownElement)ukeCtrl.getMock();
        uke.maybeConfigure();
        uke.getTask();
        ukeCtrl.setReturnValue(null);
        uke.getTaskName();
        ukeCtrl.setReturnValue("blah", 2);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(uke, 2);

        prCtrl.replay();
        ukeCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, uke, "foo", null, null, null);
        logger.taskStarted(event);
        TaskLogger child = logger.getTask("blah");
        assertEquals("Wrong child logger",
                     TaskLogger.class.getName(),
                     child.getClass().getName());
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());
        assertEquals("Wrong UnknownElement", uke, child.getUnknownElement());
        assertEquals("Wrong Task", uke, child.getTask());

        prCtrl.verify();
        ukeCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedIncludeTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl = MockClassControl.createControl(IncludeTask.class);
        IncludeTask task = (IncludeTask)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blah");

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);

        prCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", null, null, null);
        logger.taskStarted(event);
        assertNull("Wrong child logger", logger.getTask("blah"));
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());

        prCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedSkipTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl = MockClassControl.createControl(SkipTask.class);
        SkipTask task = (SkipTask)tkCtrl.getMock();

        TaskAdapter adapter = new TaskAdapter();
        adapter.setProxy(task);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(adapter, 2);

        prCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, adapter, "foo", null, null, null);
        logger.taskStarted(event);
        assertNull("Wrong child logger", logger.getTask("blah"));
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());

        prCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedParallel() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl = MockClassControl.createControl(Parallel.class);
        Parallel task = (Parallel)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blah");

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);

        prCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", null, null, null);
        logger.taskStarted(event);
        assertNull("Wrong child logger", logger.getTask("blah"));
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());

        prCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedSequential() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl = MockClassControl.createControl(Sequential.class);
        Sequential task = (Sequential)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blah");

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);

        prCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", null, null, null);
        logger.taskStarted(event);
        assertNull("Wrong child logger", logger.getTask("blah"));
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());

        prCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedMacroDef() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl = MockClassControl.createControl(MacroDef.class);
        MacroDef task = (MacroDef)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blah");

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);

        prCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", null, null, null);
        logger.taskStarted(event);
        assertNull("Wrong child logger", logger.getTask("blah"));
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());

        prCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedMacroInstance() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl = MockClassControl.createControl(MacroInstance.class);
        MacroInstance task = (MacroInstance)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blah");

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);

        prCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", null, null, null);
        logger.taskStarted(event);
        assertNull("Wrong child logger", logger.getTask("blah"));
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());

        prCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedTestGroupTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl = MockClassControl.createControl(TestGroupTask.class);
        TestGroupTask task = (TestGroupTask)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blah");
        task.getName();
        tkCtrl.setReturnValue("blubb");

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);

        prCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", null, null, null);
        logger.taskStarted(event);
        assertNull("Wrong child logger", logger.getTask("blah"));
        TaskLogger child = logger.getTask("blubb");
        assertNotNull("Wrong child logger", child);
        assertEquals("Wrong child logger",
                     TestLogger.class.getName(),
                     child.getClass().getName());
        assertEquals("Wrong current logger", child, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child.getUnknownElement());
        assertEquals("Wrong Task", task, child.getTask());

        prCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedTestGroupTaskWithoutName() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl = MockClassControl.createControl(TestGroupTask.class);
        TestGroupTask task = (TestGroupTask)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blah", 2);
        task.getName();
        tkCtrl.setReturnValue(null);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);

        prCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", null, null, null);
        logger.taskStarted(event);
        assertNull("Wrong child logger", logger.getTask("blubb"));
        TaskLogger child = logger.getTask("blah");
        assertNotNull("Wrong child logger", child);
        assertEquals("Wrong child logger",
                     TestLogger.class.getName(),
                     child.getClass().getName());
        assertEquals("Wrong current logger", child, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child.getUnknownElement());
        assertEquals("Wrong Task", task, child.getTask());

        prCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedLoggableProcess() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        TestProcessTask task = new TestProcessTask();
        task.setTaskName("blah");

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);

        prCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", null, null, null);
        logger.taskStarted(event);
        TaskLogger child = logger.getTask("blah");
        assertNotNull("Wrong child logger", child);
        assertEquals("Wrong child logger",
                     ProcessLogger.class.getName(),
                     child.getClass().getName());
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child.getUnknownElement());
        assertEquals("Wrong Task", task, child.getTask());

        prCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedServiceDef() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef task = (ServiceDef)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blah");
        task.getName();
        tkCtrl.setReturnValue("blubb");

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);

        prCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", null, null, null);
        logger.taskStarted(event);
        assertNull("Wrong child logger", logger.getTask("blah"));
        TaskLogger child = logger.getTask("blubb");
        assertNotNull("Wrong child logger", child);
        assertEquals("Wrong child logger",
                     ServiceLogger.class.getName(),
                     child.getClass().getName());
        assertEquals("Wrong current logger", child, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child.getUnknownElement());
        assertEquals("Wrong Task", task, child.getTask());

        prCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedServiceInstance() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl1 = MockClassControl.createControl(ServiceDef.class);
        ServiceDef task1 = (ServiceDef)tkCtrl1.getMock();
        task1.getTaskName();
        tkCtrl1.setReturnValue("blah");
        task1.getName();
        tkCtrl1.setReturnValue("blubb");

        MockControl tkCtrl2 = MockClassControl.createControl(ServiceInstance.class);
        ServiceInstance task2 = (ServiceInstance)tkCtrl2.getMock();
        task2.getTaskName();
        tkCtrl2.setReturnValue("blubb", 2);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task1, 2);
        evCtrl.setReturnValue(task2, 2);

        prCtrl.replay();
        tkCtrl1.replay();
        tkCtrl2.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task1, "foo", null, null, null);
        logger.taskStarted(event);
        registry.getCurrentTest().deactivate(true);
        logger.taskStarted(event);
        assertNull("Wrong child logger", logger.getTask("blah"));
        TaskLogger child = logger.getTask("blubb");
        assertNotNull("Wrong child logger", child);
        assertEquals("Wrong child logger",
                     ServiceLogger.class.getName(),
                     child.getClass().getName());
        assertEquals("Wrong current logger", child, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child.getUnknownElement());
        assertEquals("Wrong Task", task1, child.getTask());

        prCtrl.verify();
        tkCtrl1.verify();
        tkCtrl2.verify();
        evCtrl.verify();
    }

    public void testTaskStartedServiceInstanceAsServiceTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl1 = MockClassControl.createControl(ServiceDef.class);
        ServiceDef task1 = (ServiceDef)tkCtrl1.getMock();
        task1.getTaskName();
        tkCtrl1.setReturnValue("blah");
        task1.getName();
        tkCtrl1.setReturnValue("blubb");

        MockControl tkCtrl2 = MockClassControl.createControl(ServiceInstance.class);
        ServiceInstance task2 = (ServiceInstance)tkCtrl2.getMock();
        task2.getTaskName();
        tkCtrl2.setReturnValue("service", 2);
        task2.getReference();
        tkCtrl2.setReturnValue("blubb");

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task1, 2);
        evCtrl.setReturnValue(task2, 2);

        prCtrl.replay();
        tkCtrl1.replay();
        tkCtrl2.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task1, "foo", null, null, null);
        logger.taskStarted(event);
        registry.getCurrentTest().deactivate(true);
        logger.taskStarted(event);
        assertNull("Wrong child logger", logger.getTask("blah"));
        TaskLogger child = logger.getTask("blubb");
        assertNotNull("Wrong child logger", child);
        assertEquals("Wrong child logger",
                     ServiceLogger.class.getName(),
                     child.getClass().getName());
        assertEquals("Wrong current logger", child, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child.getUnknownElement());
        assertEquals("Wrong Task", task1, child.getTask());

        prCtrl.verify();
        tkCtrl1.verify();
        tkCtrl2.verify();
        evCtrl.verify();
    }

    public void testTaskStartedServiceInstanceWithServiceInParent() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl slCtrl = MockClassControl.createControl(ServiceLogger.class);
        slCtrl.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
        ServiceLogger service = (ServiceLogger)slCtrl.getMock();
        service.setContext(null, null);

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.deactivate(false);
        parent.getService("blubb");
        tlCtrl.setReturnValue(service);

        MockControl tkCtrl = MockClassControl.createControl(ServiceInstance.class);
        ServiceInstance task = (ServiceInstance)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blubb", 2);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);

        prCtrl.replay();
        slCtrl.replay();
        tlCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", parent, null, null);
        logger.taskStarted(event);
        assertNull("Wrong child logger", logger.getTask("blubb"));

        prCtrl.verify();
        slCtrl.verify();
        tlCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskStartedServiceInstanceNoParent() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl = MockClassControl.createControl(ServiceInstance.class);
        ServiceInstance task = (ServiceInstance)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blubb", 2);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);

        prCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", null, null, null);

        logger.taskStarted(event);
        assertNull("Wrong child logger", logger.getTask("blubb"));

        prCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }

    public void testTaskFinished() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        MockControl rfCtrl = MockClassControl.createControl(ResultFormatter.class);
        rfCtrl.setDefaultMatcher(new ResultFormatterMatcher());
        ResultFormatter formatter = (ResultFormatter)rfCtrl.getMock();

        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 4);
        registry.getFormatter();
        trCtrl.setReturnValue(formatter, 2);

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.deactivate(false);
        tlCtrl.setVoidCallable(2);
        parent.getFullName();
        tlCtrl.setReturnValue("bar");
        parent.activate();

        TestProcessTask task = new TestProcessTask();
        task.setTaskName("blah");

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 5);
        event.getException();
        evCtrl.setReturnValue(null);

        prCtrl.replay();
        trCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", parent);
        logger.taskStarted(event);

        assertEquals("Wrong number of deferred loggers", 1, logger.getDeferredLoggers().size());
        IDeferredLogger deferredLogger = (IDeferredLogger)logger.getDeferredLoggers().get(0);
        assertEquals("Wrong deferred logger class",
                     ProcessLogger.class.getName(),
                     deferredLogger.getClass().getName());

        formatter.writeResults((TaskLogger)deferredLogger, Result.PASSED, "", 0L);
        formatter.writeResults(logger, Result.PASSED, "", 0L);
        rfCtrl.replay();

        logger.taskFinished(event);

        prCtrl.verify();
        trCtrl.verify();
        evCtrl.verify();
        tlCtrl.verify();
        rfCtrl.verify();
    }

    public void testTaskFinishedFailing() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        MockControl rfCtrl = MockClassControl.createControl(ResultFormatter.class);
        rfCtrl.setDefaultMatcher(new ResultFormatterMatcher());
        ResultFormatter formatter = (ResultFormatter)rfCtrl.getMock();

        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 4);
        registry.getFormatter();
        trCtrl.setReturnValue(formatter, 2);

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.deactivate(false);
        tlCtrl.setVoidCallable(2);
        parent.getFullName();
        tlCtrl.setReturnValue("bar");
        parent.activate();

        TestProcessTask task = new TestProcessTask(new BuildException("bang"));
        task.setTaskName("blah");

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 5);
        event.getException();
        evCtrl.setReturnValue(null);

        prCtrl.replay();
        trCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task, "foo", parent);
        logger.taskStarted(event);

        assertEquals("Wrong number of deferred loggers", 1, logger.getDeferredLoggers().size());
        IDeferredLogger deferredLogger = (IDeferredLogger)logger.getDeferredLoggers().get(0);
        assertEquals("Wrong deferred logger class",
                     ProcessLogger.class.getName(),
                     deferredLogger.getClass().getName());

        formatter.writeResults((TaskLogger)deferredLogger, Result.FAILED, "bang", 0L);
        formatter.writeResults(logger, Result.FAILED, "bang", 0L);
        rfCtrl.replay();

        logger.taskFinished(event);

        prCtrl.verify();
        trCtrl.verify();
        evCtrl.verify();
        tlCtrl.verify();
        rfCtrl.verify();
    }

    public void testGetTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl1 = MockClassControl.createControl(Task.class);
        Task task1 = (Task)tkCtrl1.getMock();
        task1.getTaskName();
        tkCtrl1.setReturnValue("blah", 2);

        MockControl tkCtrl2 = MockClassControl.createControl(Task.class);
        Task task2 = (Task)tkCtrl2.getMock();
        task2.getTaskName();
        tkCtrl2.setReturnValue("blah", 2);

        MockControl tkCtrl3 = MockClassControl.createControl(Task.class);
        Task task3 = (Task)tkCtrl3.getMock();
        task3.getTaskName();
        tkCtrl3.setReturnValue("blah", 2);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task1, 2);
        evCtrl.setReturnValue(task2, 2);
        evCtrl.setReturnValue(task3, 2);

        prCtrl.replay();
        tkCtrl1.replay();
        tkCtrl2.replay();
        tkCtrl3.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task1, "foo", null, null, null);
        logger.taskStarted(event);
        TaskLogger child1 = logger.getTask("blah");
        assertNotNull("Wrong child logger", child1);
        assertEquals("Wrong child logger",
                     TaskLogger.class.getName(),
                     child1.getClass().getName());
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child1.getUnknownElement());
        assertEquals("Wrong Task", task1, child1.getTask());

        logger.taskStarted(event);
        TaskLogger child2 = logger.getTask("blah_1");
        assertNotNull("Wrong child logger", child2);
        assertTrue("Should be different TaskLoggers", child1 != child2);
        assertEquals("Wrong child logger",
                     TaskLogger.class.getName(),
                     child2.getClass().getName());
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child2.getUnknownElement());
        assertEquals("Wrong Task", task2, child2.getTask());

        logger.taskStarted(event);
        TaskLogger child3 = logger.getTask("blah_2");
        assertNotNull("Wrong child logger", child3);
        assertTrue("Should be different TaskLoggers", child1 != child3);
        assertTrue("Should be different TaskLoggers", child2 != child3);
        assertEquals("Wrong child logger",
                     TaskLogger.class.getName(),
                     child3.getClass().getName());
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child3.getUnknownElement());
        assertEquals("Wrong Task", task3, child3.getTask());

        assertEquals("Wrong Task", child1, logger.getTask("1"));
        assertEquals("Wrong Task", child2, logger.getTask("2"));
        assertEquals("Wrong Task", child1, logger.getTask("-2"));
        assertEquals("Wrong Task", child2, logger.getTask("-1"));
        assertEquals("Wrong Task", child3, logger.getTask(""));
        assertEquals("Wrong Task", child3, logger.getTask(null));
        assertEquals("Wrong Task", null, logger.getTask("0"));
        assertEquals("Wrong Task", null, logger.getTask("-3"));
        assertEquals("Wrong Task", child3, logger.getTask("3"));

        prCtrl.verify();
        tkCtrl1.verify();
        tkCtrl2.verify();
        tkCtrl3.verify();
        evCtrl.verify();
    }

    public void testGetTaskFromAssert() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl1 = MockClassControl.createControl(Task.class);
        Task task1 = (Task)tkCtrl1.getMock();
        task1.getTaskName();
        tkCtrl1.setReturnValue("blah", 2);

        MockControl tkCtrl2 = MockClassControl.createControl(Task.class);
        Task task2 = (Task)tkCtrl2.getMock();
        task2.getTaskName();
        tkCtrl2.setReturnValue("blah", 2);

        MockControl tkCtrl3 = MockClassControl.createControl(TaskAdapter.class);
        TaskAdapter task3 = (TaskAdapter)tkCtrl3.getMock();
        task3.getTaskName();
        tkCtrl3.setReturnValue("blah", 2);
        AssertTask assertTask = new AssertTask();
        task3.getProxy();
        tkCtrl3.setReturnValue(assertTask, 4);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task1, 2);
        evCtrl.setReturnValue(task2, 2);
        evCtrl.setReturnValue(task3, 2);

        prCtrl.replay();
        tkCtrl1.replay();
        tkCtrl2.replay();
        tkCtrl3.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task1, "foo", null, null, null);
        logger.taskStarted(event);
        TaskLogger child1 = logger.getTask("blah");
        assertNotNull("Wrong child logger", child1);
        assertEquals("Wrong child logger",
                     TaskLogger.class.getName(),
                     child1.getClass().getName());
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child1.getUnknownElement());
        assertEquals("Wrong Task", task1, child1.getTask());

        logger.taskStarted(event);
        TaskLogger child2 = logger.getTask("blah_1");
        assertNotNull("Wrong child logger", child2);
        assertTrue("Should be different TaskLoggers", child1 != child2);
        assertEquals("Wrong child logger",
                     TaskLogger.class.getName(),
                     child2.getClass().getName());
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child2.getUnknownElement());
        assertEquals("Wrong Task", task2, child2.getTask());

        logger.taskStarted(event);
        TaskLogger child3 = logger.getTask("blah_2");
        assertNotNull("Wrong child logger", child3);
        assertTrue("Should be different TaskLoggers", child1 != child3);
        assertTrue("Should be different TaskLoggers", child2 != child3);
        assertEquals("Wrong child logger",
                     TaskLogger.class.getName(),
                     child3.getClass().getName());
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child3.getUnknownElement());
        assertEquals("Wrong Task", task3, child3.getTask());

        assertEquals("Wrong Task", child1, logger.getTask("1"));
        assertEquals("Wrong Task", child2, logger.getTask("2"));
        assertEquals("Wrong Task", child1, logger.getTask("-2"));
        assertEquals("Wrong Task", child2, logger.getTask("-1"));
        assertEquals("Wrong Task", child2, logger.getTask(""));
        assertEquals("Wrong Task", child2, logger.getTask(null));
        assertEquals("Wrong Task", null, logger.getTask("0"));
        assertEquals("Wrong Task", null, logger.getTask("-3"));
        assertEquals("Wrong Task", child3, logger.getTask("3"));

        prCtrl.verify();
        tkCtrl1.verify();
        tkCtrl2.verify();
        tkCtrl3.verify();
        evCtrl.verify();
    }

    public void testGetTaskDuringShutdown() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        TaskRegistry registry = TaskRegistry.init(project);

        MockControl tkCtrl1 = MockClassControl.createControl(Task.class);
        Task task1 = (Task)tkCtrl1.getMock();
        task1.getTaskName();
        tkCtrl1.setReturnValue("blah", 2);

        MockControl tkCtrl2 = MockClassControl.createControl(Task.class);
        Task task2 = (Task)tkCtrl2.getMock();
        task2.getTaskName();
        tkCtrl2.setReturnValue("blah", 2);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task1, 2);
        evCtrl.setReturnValue(task2, 2);

        prCtrl.replay();
        tkCtrl1.replay();
        tkCtrl2.replay();
        evCtrl.replay();

        TestLogger logger = new TestLogger(registry, task1, "foo", null, null, null);
        logger.taskStarted(event);
        TaskLogger child1 = logger.getTask("blah");
        assertNotNull("Wrong child logger", child1);
        assertEquals("Wrong child logger",
                     TaskLogger.class.getName(),
                     child1.getClass().getName());
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child1.getUnknownElement());
        assertEquals("Wrong Task", task1, child1.getTask());

        logger.taskStarted(event);
        TaskLogger child2 = logger.getTask("blah_1");
        assertNotNull("Wrong child logger", child2);
        assertTrue("Should be different TaskLoggers", child1 != child2);
        assertEquals("Wrong child logger",
                     TaskLogger.class.getName(),
                     child2.getClass().getName());
        assertEquals("Wrong current logger", logger, registry.getCurrentTest());
        assertNull("Wrong UnknownElement", child2.getUnknownElement());
        assertEquals("Wrong Task", task2, child2.getTask());

        IDeferredLogger deferred = new TestDeferredTaskLogger(logger, child1, child2);
        logger.addDeferredLogger(deferred);
        logger.stopDeferredElements();

        prCtrl.verify();
        tkCtrl1.verify();
        tkCtrl2.verify();
        evCtrl.verify();
    }

    class TestDeferredTaskLogger extends TaskLogger implements IDeferredLogger {
        private TestLogger parentLogger;
        private TaskLogger childLogger1;
        private TaskLogger childLogger2;
        public TestDeferredTaskLogger(TestLogger tl, TaskLogger cl1, TaskLogger cl2) {
            parentLogger = tl;
            childLogger1 = cl1;
            childLogger2 = cl2;
        }
        public void deferredShutdown() {
            assertEquals("Wrong Task", this, parentLogger.getTask(""));
            assertEquals("Wrong Task", this, parentLogger.getTask(null));
            assertEquals("Wrong Task", childLogger1, parentLogger.getTask("1"));
            assertEquals("Wrong Task", childLogger1, parentLogger.getTask("-1"));
            assertEquals("Wrong Task", null, parentLogger.getTask("0"));
            assertEquals("Wrong Task", null, parentLogger.getTask("-2"));
            assertEquals("Wrong Task", childLogger2, parentLogger.getTask("2"));
        }
    }
}
