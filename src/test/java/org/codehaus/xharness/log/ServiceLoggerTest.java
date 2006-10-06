package org.codehaus.xharness.log;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.UnknownElement;

import org.codehaus.xharness.exceptions.ServiceVerifyException;
import org.codehaus.xharness.tasks.ServiceDef;
import org.codehaus.xharness.tasks.ServiceGroupTask;
import org.codehaus.xharness.tasks.ServiceVerifyTask;
import org.codehaus.xharness.tasks.XharnessTask;
import org.codehaus.xharness.testutil.ResultFormatterMatcher;
import org.codehaus.xharness.testutil.TestProcessTask;

import org.easymock.AbstractMatcher;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ServiceLoggerTest extends TestCase {
    public ServiceLoggerTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ServiceLoggerTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ServiceLoggerTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(ServiceLoggerTest.class);
    }
    
    public void testTaskStartedInactive() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef task = (ServiceDef)tkCtrl.getMock();

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar");
        parent.deactivate(false);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.deactivate(false);
        logger.taskStarted(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskStartedInvalidTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef task = (ServiceDef)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blah");

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar");
        parent.deactivate(false);
        tlCtrl.setVoidCallable(3);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.setContext(parent, null);
        logger.taskStarted(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskStartedUnknownElement() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef task = (ServiceDef)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blah");

        MockControl ueCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement uke = (UnknownElement)ueCtrl.getMock();
        uke.maybeConfigure();
        uke.getTask();
        ueCtrl.setReturnValue(task);

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar");
        parent.deactivate(false);
        tlCtrl.setVoidCallable(3);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(uke, 2);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        ueCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, uke, "foo", parent);
        logger.setContext(parent, null);
        logger.taskStarted(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        ueCtrl.verify();
        tlCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskStartedStartTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 3);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceGroupTask.class);
        ServiceGroupTask task = (ServiceGroupTask)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("start");

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar", 3);
        parent.deactivate(false);
        tlCtrl.setVoidCallable(3);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.setContext(parent, null);
        logger.taskStarted(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskStartedVerifyTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 2);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceVerifyTask.class);
        ServiceVerifyTask task = (ServiceVerifyTask)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("verify", 2);

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar", 2);
        parent.deactivate(false);
        tlCtrl.setVoidCallable(3);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.setContext(parent, null);
        logger.taskStarted(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskStartedVerifyDummyTaskBeforeStart() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 2);

        MockControl tkCtrl = MockClassControl.createControl(ServiceVerifyTask.class);
        ServiceVerifyTask task = (ServiceVerifyTask)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue(ServiceVerifyTask.DUMMY, 3);

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar", 2);
        parent.deactivate(false);
        tlCtrl.setVoidCallable(3);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.setContext(parent, null);
        try {
            logger.taskStarted(event);
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong nested Excecption", 
                         ArrayIndexOutOfBoundsException.class.getName(), 
                         be.getCause().getClass().getName());
        }
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskStartedVerifyDummyTaskPasses() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask xhTask = (XharnessTask)xhCtrl.getMock();
        xhTask.getPattern();
        xhCtrl.setReturnValue(null);
        xhTask.getName();
        xhCtrl.setReturnValue("foo");
        xhTask.getResultsdir();
        xhCtrl.setReturnValue(null);
        xhTask.getBasedir();
        xhCtrl.setReturnValue(null);
        xhTask.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceVerifyTask.class);
        ServiceVerifyTask task = (ServiceVerifyTask)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("start");
        tkCtrl.setReturnValue(ServiceVerifyTask.DUMMY, 3);

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar", 4);
        parent.deactivate(false);
        tlCtrl.setVoidCallable(4);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 4);
        
        prCtrl.replay();
        xhCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();
        
        TaskRegistry registry = TaskRegistry.init(xhTask);
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.setContext(parent, null);
        logger.taskStarted(event);
        TestLogger child = registry.getCurrentTest();
        assertEquals("Wrong child logger",
                     SvcsStartLogger.class.getName(),
                     child.getClass().getName());
        child.getDeferredLoggers().add(new TaskLogger());
        child.getDeferredLoggers().add(new ProcessLogger());
        child.getDeferredLoggers().add(new ProcessLogger(registry, 
                                                         new TestProcessTask(true), 
                                                         "a", 
                                                         "b"));
        logger.activate();
        logger.taskStarted(event);
        
        prCtrl.verify();
        xhCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskStartedVerifyDummyTaskFails() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask xhTask = (XharnessTask)xhCtrl.getMock();
        xhTask.getPattern();
        xhCtrl.setReturnValue(null);
        xhTask.getName();
        xhCtrl.setReturnValue("foo");
        xhTask.getResultsdir();
        xhCtrl.setReturnValue(null);
        xhTask.getBasedir();
        xhCtrl.setReturnValue(null);
        xhTask.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceVerifyTask.class);
        tkCtrl.setDefaultMatcher(new ExceptionMatcher());
        ServiceVerifyTask task = (ServiceVerifyTask)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("start");
        tkCtrl.setReturnValue(ServiceVerifyTask.DUMMY, 3);
        task.setException(new BuildException("Process @d/c@ has stopped running"));

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar", 4);
        parent.deactivate(false);
        tlCtrl.setVoidCallable(4);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 4);
        
        prCtrl.replay();
        xhCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();
        
        TaskRegistry registry = TaskRegistry.init(xhTask);
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.setContext(parent, null);
        logger.taskStarted(event);
        TestLogger child = registry.getCurrentTest();
        assertEquals("Wrong child logger",
                     SvcsStartLogger.class.getName(),
                     child.getClass().getName());
        child.getDeferredLoggers().add(new TaskLogger());
        child.getDeferredLoggers().add(new ProcessLogger());
        child.getDeferredLoggers().add(new ProcessLogger(registry, 
                                                         new TestProcessTask(false), 
                                                         "c", 
                                                         "d"));
        logger.activate();
        logger.taskStarted(event);
        
        prCtrl.verify();
        xhCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskStartedStopTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 3);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceGroupTask.class);
        ServiceGroupTask task = (ServiceGroupTask)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("stop", 4);

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar", 2);
        parent.deactivate(false);
        tlCtrl.setVoidCallable(3);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.setContext(parent, null);
        logger.taskStarted(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskStartedStopTaskNoContext() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 3);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceGroupTask.class);
        ServiceGroupTask task = (ServiceGroupTask)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("stop", 4);

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar");
        parent.deactivate(false);
        tlCtrl.setVoidCallable(1);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.taskStarted(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskStartedInvalidServiceGroupTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceGroupTask.class);
        ServiceGroupTask task = (ServiceGroupTask)tkCtrl.getMock();
        task.getTaskName();
        tkCtrl.setReturnValue("blah", 5);

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar");
        parent.deactivate(false);
        tlCtrl.setVoidCallable(3);

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 2);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.setContext(parent, null);
        logger.taskStarted(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskFinished() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef task = (ServiceDef)tkCtrl.getMock();
        task.getRuntimeConfigurableWrapper();
        tkCtrl.setReturnValue(new RuntimeConfigurable(new Object(), "foo"));
        task.wasStopped();
        tkCtrl.setReturnValue(false);

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar");
        parent.deactivate(false);
        parent.activate();

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 3);
        event.getException();
        evCtrl.setReturnValue(null);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        evCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.taskFinished(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        evCtrl.verify();
    }
    
    public void testTaskFinishedWithContext() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef task = (ServiceDef)tkCtrl.getMock();
        task.getRuntimeConfigurableWrapper();
        tkCtrl.setReturnValue(new RuntimeConfigurable(new Object(), "foo"));
        task.wasStopped();
        tkCtrl.setReturnValue(true);

        MockControl tlCtrl1 = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl1.getMock();
        parent.getFullName();
        tlCtrl1.setReturnValue("bar");
        parent.deactivate(false);
        tlCtrl1.setVoidCallable(3);

        MockControl tlCtrl2 = MockClassControl.createControl(TestLogger.class);
        TestLogger context = (TestLogger)tlCtrl2.getMock();
        context.deactivate(false);
        context.activate();

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getTask();
        evCtrl.setReturnValue(task, 3);
        event.getException();
        evCtrl.setReturnValue(null);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl1.replay();
        tlCtrl2.replay();
        evCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.setContext(context, null);
        logger.taskFinished(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl1.verify();
        tlCtrl2.verify();
        evCtrl.verify();
    }

    public void testDeferredShutdown() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl rfCtrl = MockClassControl.createNiceControl(ResultFormatter.class);
        rfCtrl.setDefaultMatcher(new ResultFormatterMatcher());
        ResultFormatter formatter = (ResultFormatter)rfCtrl.getMock();

        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 2);
        registry.getFormatter();
        trCtrl.setReturnValue(formatter);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef task = (ServiceDef)tkCtrl.getMock();
        task.stop();

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar");
        parent.deactivate(false);
        tlCtrl.setVoidCallable(3);
        parent.activate();
        tlCtrl.setVoidCallable(2);

        prCtrl.replay();
        rfCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.deferredShutdown();
        
        prCtrl.verify();
        rfCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
    }

    public void testDeferredShutdownFailing1() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl rfCtrl = MockClassControl.createNiceControl(ResultFormatter.class);
        rfCtrl.setDefaultMatcher(new ResultFormatterMatcher());
        ResultFormatter formatter = (ResultFormatter)rfCtrl.getMock();

        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 2);
        registry.getFormatter();
        trCtrl.setReturnValue(formatter);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef task = (ServiceDef)tkCtrl.getMock();
        task.stop();
        tkCtrl.setThrowable(new BuildException("foo"));

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar");
        parent.deactivate(false);
        tlCtrl.setVoidCallable(3);
        parent.activate();
        tlCtrl.setVoidCallable(2);

        prCtrl.replay();
        rfCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        try {
            logger.deferredShutdown();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "foo", be.getMessage());
        }
        
        prCtrl.verify();
        rfCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
    }

    public void testDeferredShutdownFailing2() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl rfCtrl = MockClassControl.createNiceControl(ResultFormatter.class);
        rfCtrl.setDefaultMatcher(new ResultFormatterMatcher());
        ResultFormatter formatter = (ResultFormatter)rfCtrl.getMock();

        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 2);
        registry.getFormatter();
        trCtrl.setReturnValue(formatter);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef task = (ServiceDef)tkCtrl.getMock();
        task.stop();
        tkCtrl.setThrowable(new ServiceVerifyException(new BuildException("foo")));

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar");
        parent.deactivate(false);
        tlCtrl.setVoidCallable(3);
        parent.activate();
        tlCtrl.setVoidCallable(2);

        prCtrl.replay();
        rfCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.deferredShutdown();
        
        prCtrl.verify();
        rfCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
    }

    public void testDeferredShutdownFailing3() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl rfCtrl = MockClassControl.createNiceControl(ResultFormatter.class);
        rfCtrl.setDefaultMatcher(new ResultFormatterMatcher());
        ResultFormatter formatter = (ResultFormatter)rfCtrl.getMock();

        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 2);
        registry.getFormatter();
        trCtrl.setReturnValue(formatter);
        
        MockControl tkCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef task = (ServiceDef)tkCtrl.getMock();
        task.stop();

        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class);
        TestLogger parent = (TestLogger)tlCtrl.getMock();
        parent.getFullName();
        tlCtrl.setReturnValue("bar");
        parent.deactivate(false);
        tlCtrl.setVoidCallable(3);
        parent.activate();
        tlCtrl.setVoidCallable(2);

        prCtrl.replay();
        rfCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        tlCtrl.replay();
        
        ServiceLogger logger = new ServiceLogger(registry, task, "foo", parent);
        logger.setFailure(new RuntimeException("blah"));
        try {
            logger.deferredShutdown();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertNotNull("Should have nested Excecption", be.getCause());
            assertEquals("Wrong nested Exception", 
                         RuntimeException.class.getName(), 
                         be.getCause().getClass().getName());
            assertEquals("Wrong message", "blah", be.getCause().getMessage());
        }
        
        prCtrl.verify();
        rfCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
    }

    private static class ExceptionMatcher extends AbstractMatcher {
        protected boolean argumentMatches(Object first, Object second) {
            if (first instanceof Exception && second instanceof Exception) {
                Exception ex1 = (Exception)first;
                Exception ex2 = (Exception)second;
                return ex1.getClass().getName().equals(ex2.getClass().getName())
                       && ex1.getMessage().equals(ex2.getMessage());
            }
            return super.argumentMatches(first, second);
        }
    }
}
