package org.codehaus.xharness.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;

import org.codehaus.xharness.TestHelper;
import org.codehaus.xharness.exceptions.AssertionWarningException;
import org.codehaus.xharness.exceptions.TestSkippedException;
import org.codehaus.xharness.log.TaskRegistry;
import org.codehaus.xharness.log.TestLogger;
import org.codehaus.xharness.testutil.TempDir;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestGroupTaskTest extends TestCase {
    private Project project;
    
    public TestGroupTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = TestGroupTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = TestGroupTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(TestGroupTaskTest.class);
    }
    
    public void setUp() throws Exception {
        MockControl ctrl = MockClassControl.createControl(Project.class);
        project = (Project)ctrl.getMock();
    }
    
    public void testAttributes() throws Exception {
        TestGroupTask group = new TestGroupTask();
        
        assertEquals("Invalid Name", null, group.getName());
        group.setName("Foo");
        assertEquals("Invalid Name", "Foo", group.getName());
        assertEquals("Invalid toString", "testgroup Foo", group.toString());
        assertEquals("Invalid failOnError", false, group.failOnError());
        
        assertNotNull("Children null", group.getNestedTasks());
        assertEquals("Invalid number of children", 0, group.getNestedTasks().size());
        
        MockControl ctrl = MockClassControl.createControl(Task.class);
        Task mock1 = (Task)ctrl.getMock();
        Task mock2 = (Task)ctrl.getMock();
        
        ctrl.replay();
        group.addTask(mock1);
        group.addTask(null);
        group.addTask(mock2);
        assertEquals("Invalid number of children", 2, group.getNestedTasks().size());
        assertEquals("Invalid Child", mock1, group.getNestedTasks().get(0));
        assertEquals("Invalid Child", mock2, group.getNestedTasks().get(1));
        ctrl.verify();
    }
    
    public void testExecutePatternMismatch() throws Exception {
        File resultsDir = TempDir.createTempDir();
        XharnessTask xhTask = new XharnessTask();
        xhTask.setName("bar");
        xhTask.setPattern("spam");
        xhTask.setProject(project);
        xhTask.setResultsdir(resultsDir);
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(xhTask);
            TestGroupTask group = new TestGroupTask();
            group.setProject(project);
            group.setName("foo");
            
            TestLogger logger = new TestLogger(registry, group, "foo", registry.getCurrentTest());
            registry.setCurrentTest(logger);
            
            MockControl ctrl = MockClassControl.createControl(Task.class);
            Task mock = (Task)ctrl.getMock();
            mock.getProject();
            ctrl.setReturnValue(project, 2);
            mock.maybeConfigure();
            ctrl.setVoidCallable();
            mock.execute();
            ctrl.setVoidCallable();

            ctrl.replay();
            group.addTask(mock);
            group.execute();
            ctrl.verify();
        } finally {
            if (registry != null) {
                registry.shutdown(null);
            }
            TempDir.removeTempFile(resultsDir);
        }
        
    }
    
    public void testExecuteSkipped() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        
        Task mockTask = (Task)taskCtrl.getMock();
        mockTask.getProject();
        taskCtrl.setReturnValue(project, 2);
        mockTask.maybeConfigure();
        mockTask.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        mockTask.execute();
        taskCtrl.setThrowable(new TestSkippedException("foo", true));

        taskCtrl.replay();
        group.addTask(mockTask);
        try {
            group.execute();
            fail("Expected TestSkippedException to be thrown");
        } catch (TestSkippedException tse) {
            assertEquals("Wrong message", "foo", tse.getMessage());
        }
        taskCtrl.verify();
    }
    
    public void testExecuteSkippedGroup() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        MockControl taskCtrl = MockClassControl.createControl(TestGroupTask.class);
        
        Task mockTask = (Task)taskCtrl.getMock();
        mockTask.getProject();
        taskCtrl.setReturnValue(project, 2);
        mockTask.maybeConfigure();
        mockTask.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        mockTask.execute();
        taskCtrl.setThrowable(new TestSkippedException("foo", true));

        taskCtrl.replay();
        group.addTask(mockTask);
        group.execute();
        taskCtrl.verify();
    }
    
    public void testExecute() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        
        Task mockTask1 = (Task)taskCtrl.getMock();
        mockTask1.getProject();
        taskCtrl.setReturnValue(project, 2);
        mockTask1.maybeConfigure();
        mockTask1.execute();

        Task mockTask2 = (Task)taskCtrl.getMock();
        mockTask2.getProject();
        taskCtrl.setReturnValue(project, 2);
        mockTask2.maybeConfigure();
        mockTask2.execute();

        taskCtrl.replay();
        group.addTask(mockTask1);
        group.addTask(mockTask2);
        group.execute();
        taskCtrl.verify();
    }
    
    public void testExecuteWarningMultipleTasks() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        MockControl taskCtrl = MockClassControl.createControl(Task.class);

        Task mockTask1 = (Task)taskCtrl.getMock();
        mockTask1.getProject();
        taskCtrl.setReturnValue(project, 2);
        mockTask1.maybeConfigure();
        mockTask1.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        mockTask1.getTaskName();
        taskCtrl.setReturnValue("foo");
        mockTask1.execute();
        taskCtrl.setThrowable(new AssertionWarningException("XXX"));

        Task mockTask2 = (Task)taskCtrl.getMock();
        mockTask2.getProject();
        taskCtrl.setReturnValue(project, 2);
        mockTask2.maybeConfigure();
        mockTask2.execute();

        Task mockTask3 = (Task)taskCtrl.getMock();
        mockTask3.getProject();
        taskCtrl.setReturnValue(project, 2);
        mockTask3.maybeConfigure();
        mockTask3.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        mockTask3.execute();
        taskCtrl.setThrowable(new AssertionWarningException("XXX"));

        taskCtrl.replay();
        group.addTask(mockTask1);
        group.addTask(mockTask2);
        group.addTask(mockTask3);
        try {
            group.execute();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", "Warning in Task foo", awe.getMessage());
        }
        taskCtrl.verify();
    }
    
    public void testExecuteWarningNestedTask() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task nested = (Task)taskCtrl.getMock();
        nested.getTaskName();
        taskCtrl.setReturnValue("foo");

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement mockUke = getMockUnknownElement(ukeCtrl, 
                                                       nested, 
                                                       new AssertionWarningException());

        taskCtrl.replay();
        ukeCtrl.replay();
        group.addTask(mockUke);
        try {
            group.execute();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", "Warning in Task foo", awe.getMessage());
        }
        taskCtrl.verify();
        ukeCtrl.verify();
    }
    
    public void testExecuteWarningNestedServiceDef() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        ServiceDef nested = new ServiceDef();
        nested.setName("myService");

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement mockUke = getMockUnknownElement(ukeCtrl, 
                                                       nested, 
                                                       new AssertionWarningException());

        ukeCtrl.replay();
        group.addTask(mockUke);
        try {
            group.execute();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", "Warning in Service myService", awe.getMessage());
        }
        ukeCtrl.verify();
    }
    
    public void testExecuteWarningNestedServiceInstance() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        ServiceInstance nested = new ServiceInstance();
        nested.setTaskName("myService");

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement mockUke = getMockUnknownElement(ukeCtrl, 
                                                       nested, 
                                                       new AssertionWarningException());

        ukeCtrl.replay();
        group.addTask(mockUke);
        try {
            group.execute();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", "Warning in Service myService", awe.getMessage());
        }
        ukeCtrl.verify();
    }
    
    public void testExecuteWarningNestedTestCase() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        TestCaseTask nested = new TestCaseTask();
        nested.setName("foo");

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement mockUke = getMockUnknownElement(ukeCtrl, 
                                                       nested, 
                                                       new AssertionWarningException());

        ukeCtrl.replay();
        group.addTask(mockUke);
        try {
            group.execute();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", "Warning in Testcase foo", awe.getMessage());
        }
        ukeCtrl.verify();
    }
    
    
    public void testExecuteWarningNestedTestGroup() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        TestGroupTask nested = new TestGroupTask();
        nested.setName("foo");

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement mockUke = getMockUnknownElement(ukeCtrl, 
                                                       nested, 
                                                       new AssertionWarningException());

        ukeCtrl.replay();
        group.addTask(mockUke);
        group.execute();
        ukeCtrl.verify();
    }
    
    public void testExecuteErrorMultipleTasks() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        MockControl taskCtrl = MockClassControl.createControl(Task.class);

        Task mockTask1 = (Task)taskCtrl.getMock();
        mockTask1.getProject();
        taskCtrl.setReturnValue(project, 2);
        mockTask1.maybeConfigure();
        mockTask1.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        mockTask1.getTaskName();
        taskCtrl.setReturnValue("foo");
        mockTask1.execute();
        taskCtrl.setThrowable(new BuildException("XXX"));

        Task mockTask2 = (Task)taskCtrl.getMock();
        mockTask2.getProject();
        taskCtrl.setReturnValue(project, 2);
        mockTask2.maybeConfigure();
        mockTask2.execute();

        Task mockTask3 = (Task)taskCtrl.getMock();
        mockTask3.getProject();
        taskCtrl.setReturnValue(project, 2);
        mockTask3.maybeConfigure();
        mockTask3.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        mockTask3.execute();
        taskCtrl.setThrowable(new BuildException("XXX"));

        taskCtrl.replay();
        group.addTask(mockTask1);
        group.addTask(mockTask2);
        group.addTask(mockTask3);
        try {
            group.execute();
            fail("Expected AssertionWarningException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Task foo failed", be.getMessage());
        }
        taskCtrl.verify();
    }
    
    public void testExecuteErrorNestedTask() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task nested = (Task)taskCtrl.getMock();
        nested.getTaskName();
        taskCtrl.setReturnValue("foo");

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement mockUke = getMockUnknownElement(ukeCtrl, nested, new BuildException());

        taskCtrl.replay();
        ukeCtrl.replay();
        group.addTask(mockUke);
        try {
            group.execute();
            fail("Expected AssertionWarningException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Task foo failed", be.getMessage());
        }
        taskCtrl.verify();
        ukeCtrl.verify();
    }
    
    public void testExecuteErrorNestedServiceDef() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        ServiceDef nested = new ServiceDef();
        nested.setName("myService");

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement mockUke = getMockUnknownElement(ukeCtrl, nested, new BuildException());

        ukeCtrl.replay();
        group.addTask(mockUke);
        try {
            group.execute();
            fail("Expected AssertionWarningException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Service myService failed", be.getMessage());
        }
        ukeCtrl.verify();
    }
    
    public void testExecuteErrorNestedServiceInstance() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        ServiceInstance nested = new ServiceInstance();
        nested.setTaskName("myService");

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement mockUke = getMockUnknownElement(ukeCtrl, nested, new BuildException());

        ukeCtrl.replay();
        group.addTask(mockUke);
        try {
            group.execute();
            fail("Expected AssertionWarningException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Service myService failed", be.getMessage());
        }
        ukeCtrl.verify();
    }
    
    public void testExecuteErrorNestedTestCase() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        TestCaseTask nested = new TestCaseTask();
        nested.setName("foo");

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement mockUke = getMockUnknownElement(ukeCtrl, nested, new BuildException());

        ukeCtrl.replay();
        group.addTask(mockUke);
        try {
            group.execute();
            fail("Expected AssertionWarningException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Testcase foo failed", be.getMessage());
        }
        ukeCtrl.verify();
    }
    
    
    public void testExecuteErrorNestedTestGroup() throws Exception {
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        TestGroupTask nested = new TestGroupTask();
        nested.setName("foo");

        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement mockUke = getMockUnknownElement(ukeCtrl, nested, new BuildException());

        ukeCtrl.replay();
        group.addTask(mockUke);
        group.execute();
        ukeCtrl.verify();
    }

    public void testExecuteErrorNullTask() throws Exception {
        
        // For XHARNESS-9
        
        TestGroupTask group = new TestGroupTask();
        group.setProject(project);
        
        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement mockUke = (UnknownElement)ukeCtrl.getMock();
        mockUke.getProject();
        ukeCtrl.setReturnValue(project, 2);
        mockUke.maybeConfigure();
        mockUke.getLocation();
        ukeCtrl.setReturnValue(new Location(""));
        mockUke.getTaskName();
        ukeCtrl.setReturnValue("foo");
        mockUke.getRealThing();
        if (!TestHelper.isAnt16()) {
            ukeCtrl.setReturnValue(null, 2);
        } else {
            ukeCtrl.setReturnValue(null, 1);
        }
        mockUke.execute();
        ukeCtrl.setThrowable(new BuildException());

        ukeCtrl.replay();
        group.addTask(mockUke);
        try {
            group.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Task foo failed", be.getMessage());
        }
        ukeCtrl.verify();        
    }
    
    
    public void testSimilar() throws Exception {
        RuntimeConfigurable rc1 = new RuntimeConfigurable(null, "mocktask1");
        RuntimeConfigurable rc2 = new RuntimeConfigurable(null, "mocktask2");
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task task1 = (Task)taskCtrl.getMock();
        Task task2 = (Task)taskCtrl.getMock();
        task1.getRuntimeConfigurableWrapper();
        taskCtrl.setReturnValue(rc1);
        taskCtrl.setReturnValue(rc2, 7);
        MockControl ukeCtrl = MockClassControl.createControl(UnknownElement.class);
        UnknownElement uke1 = (UnknownElement)ukeCtrl.getMock();
        UnknownElement uke2 = (UnknownElement)ukeCtrl.getMock();
        uke1.similar(uke2);
        ukeCtrl.setReturnValue(false);
        ukeCtrl.setReturnValue(true);

        taskCtrl.replay();
        ukeCtrl.replay();
        
        TestGroupTask group = new TestGroupTask();
        group.setName("foo");
        group.addTask(task1);
        
        TestGroupTask other = new TestGroupTask();
        other.setName("bar");
        assertEquals("Groups shouldn't be similar", false, group.similar(other));
        
        other.setName("foo");
        assertEquals("Groups shouldn't be similar", false, group.similar(other));

        other.addTask(task2);
        assertEquals("Groups shouldn't be similar", false, group.similar(other));
        
        other = new TestGroupTask();
        other.setName("foo");
        other.addTask(task1);
        assertEquals("Groups should be similar", true, group.similar(other));
        
        group.addTask(uke1);
        other.addTask(uke2);
        assertEquals("Groups shouldn't be similar", false, group.similar(other));
        
        other = new TestGroupTask();
        other.setName("foo");
        other.addTask(task1);
        other.addTask(uke1);
        assertEquals("Groups should be similar", true, group.similar(other));

        taskCtrl.verify();
        ukeCtrl.verify();
    }
    
    private UnknownElement getMockUnknownElement(MockControl ctrl, 
                                                 Object nested, 
                                                 Throwable exception) {
        UnknownElement mockUke = (UnknownElement)ctrl.getMock();
        mockUke.getProject();
        ctrl.setReturnValue(project, 2);
        mockUke.maybeConfigure();
        mockUke.getLocation();
        ctrl.setReturnValue(new Location(""));
        mockUke.getRealThing();
        if (!TestHelper.isAnt16()) {
            ctrl.setReturnValue(nested, 2);
        } else {
            ctrl.setReturnValue(nested, 1);
        }
        mockUke.execute();
        if (exception != null) {
            ctrl.setThrowable(exception);
        }
        return mockUke;
    }
}
