package org.codehaus.xharness.log;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Task;

import org.codehaus.xharness.tasks.ServiceDef;
import org.codehaus.xharness.testutil.ResultFormatterMatcher;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SvcsStartLoggerTest extends TestCase {
    public SvcsStartLoggerTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = SvcsStartLoggerTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = SvcsStartLoggerTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(SvcsStartLoggerTest.class);
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
        
        SvcsStartLogger logger = new SvcsStartLogger(registry, task, "foo", parent, "spam");
        logger.taskFinished(event);
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        evCtrl.verify();
    }
    
    public void testDeferredShutdown() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl rfCtrl = MockClassControl.createControl(ResultFormatter.class);
        rfCtrl.setDefaultMatcher(new ResultFormatterMatcher());
        ResultFormatter formatter = (ResultFormatter)rfCtrl.getMock();

        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 2);
        registry.getFormatter();
        trCtrl.setReturnValue(formatter);
        
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
        
        SvcsStartLogger logger = new SvcsStartLogger(registry, task, "foo", parent, "spam");
        formatter.writeResults(logger, Result.PASSED, "", 0L);
        rfCtrl.setVoidCallable();
        rfCtrl.replay();

        logger.deferredShutdown();
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        rfCtrl.verify();
    }
    
    public void testDeferredShutdownBuildException() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl rfCtrl = MockClassControl.createControl(ResultFormatter.class);
        rfCtrl.setDefaultMatcher(new ResultFormatterMatcher());
        ResultFormatter formatter = (ResultFormatter)rfCtrl.getMock();

        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 2);
        registry.getFormatter();
        trCtrl.setReturnValue(formatter);
        
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
        
        SvcsStartLogger logger = new SvcsStartLogger(registry, task, "foo", parent, "spam");
        formatter.writeResults(logger, Result.FAILED, "bang", 0L);
        rfCtrl.setVoidCallable();
        rfCtrl.replay();

        logger.setFailure(new BuildException("bang"));
        try {
            logger.deferredShutdown();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "bang", be.getMessage());
        }
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        rfCtrl.verify();
    }
    
    public void testDeferredShutdownException() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl rfCtrl = MockClassControl.createControl(ResultFormatter.class);
        rfCtrl.setDefaultMatcher(new ResultFormatterMatcher());
        ResultFormatter formatter = (ResultFormatter)rfCtrl.getMock();

        MockControl trCtrl = MockClassControl.createNiceControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getProject();
        trCtrl.setReturnValue(project, 2);
        registry.getFormatter();
        trCtrl.setReturnValue(formatter);
        
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
        
        SvcsStartLogger logger = new SvcsStartLogger(registry, task, "foo", parent, "spam");
        formatter.writeResults(logger, Result.FAILED, "bang", 0L);
        rfCtrl.setVoidCallable();
        rfCtrl.replay();

        logger.setFailure(new RuntimeException("bang"));
        try {
            logger.deferredShutdown();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertNotNull("Should have nested Excecption", be.getCause());
            assertEquals("Wrong nested Exception", 
                         RuntimeException.class.getName(), 
                         be.getCause().getClass().getName());
            assertEquals("Wrong message", "bang", be.getCause().getMessage());
        }
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        tlCtrl.verify();
        rfCtrl.verify();
    }
}
