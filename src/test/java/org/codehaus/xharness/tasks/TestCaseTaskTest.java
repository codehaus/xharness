package org.codehaus.xharness.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

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

public class TestCaseTaskTest extends TestCase {
    private Project project;
    
    public TestCaseTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = TestCaseTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = TestCaseTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(TestCaseTaskTest.class);
    }
    
    public void setUp() throws Exception {
        MockControl ctrl = MockClassControl.createControl(Project.class);
        project = (Project)ctrl.getMock();
    }
    
    public void testAttributes() throws Exception {
        TestCaseTask test = new TestCaseTask();
        
        assertEquals("Invalid Name", null, test.getName());
        test.setName("Foo");
        assertEquals("Invalid Name", "Foo", test.getName());
        assertEquals("Invalid toString", "testcase Foo", test.toString());
        assertEquals("Invalid failOnError", true, test.failOnError());
        assertEquals("Invalid Owner", "unknown", test.getOwner());
        test.setOwner("Bar");
        assertEquals("Invalid Owner", "Bar", test.getOwner());
        
        assertNotNull("Children null", test.getNestedTasks());
        assertEquals("Invalid number of children", 0, test.getNestedTasks().size());
        
        MockControl ctrl = MockClassControl.createControl(Task.class);
        Task mock1 = (Task)ctrl.getMock();
        Task mock2 = (Task)ctrl.getMock();
        
        ctrl.replay();
        test.addTask(mock1);
        test.addTask(null);
        test.addTask(mock2);
        assertEquals("Invalid number of children", 2, test.getNestedTasks().size());
        assertEquals("Invalid Child", mock1, test.getNestedTasks().get(0));
        assertEquals("Invalid Child", mock2, test.getNestedTasks().get(1));
        ctrl.verify();
    }
    
    public void testExecuteWarningMultipleTasks() throws Exception {
        TestCaseTask test = new TestCaseTask();
        test.setProject(project);
        
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
        test.addTask(mockTask1);
        test.addTask(mockTask2);
        test.addTask(mockTask3);
        try {
            test.execute();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", "Warning in Task foo", awe.getMessage());
        }
        taskCtrl.verify();
    }
    
    public void testExecuteWarningNestedTestGroup() throws Exception {
        TestCaseTask test = new TestCaseTask();
        test.setProject(project);
        
        TestGroupTask nested = new TestGroupTask();
        nested.setName("foo");

        MockControl ctrl = MockClassControl.createControl(IncludeTask.class);
        IncludeTask include = getMockIncludeTask(ctrl, nested, new AssertionWarningException());

        ctrl.replay();
        test.addTask(include);
        try {
            test.execute();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", "Warning in Testgroup foo", awe.getMessage());
        }
        ctrl.verify();
    }
    
    public void testExecuteErrorMultipleTasks() throws Exception {
        TestCaseTask test = new TestCaseTask();
        test.setProject(project);
        
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
        mockTask2.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        mockTask2.getTaskName();
        taskCtrl.setReturnValue("foo");
        mockTask2.execute();
        taskCtrl.setThrowable(new BuildException("XXX"));

        Task mockTask3 = (Task)taskCtrl.getMock();

        taskCtrl.replay();
        test.addTask(mockTask1);
        test.addTask(mockTask2);
        test.addTask(mockTask3);
        try {
            test.execute();
            fail("Expected AssertionWarningException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Task foo failed", be.getMessage());
        }
        taskCtrl.verify();
    }
    
    public void testExecuteErrorNestedTestGroup() throws Exception {
        TestCaseTask test = new TestCaseTask();
        test.setProject(project);
        
        TestGroupTask nested = new TestGroupTask();
        nested.setName("foo");

        MockControl ctrl = MockClassControl.createControl(IncludeTask.class);
        IncludeTask include = getMockIncludeTask(ctrl, nested, new BuildException());

        ctrl.replay();
        test.addTask(include);
        try {
            test.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Testgroup foo failed", be.getMessage());
        }
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
            TestCaseTask test = new TestCaseTask();
            test.setProject(project);
            test.setName("foo");
            
            TestLogger logger = new TestLogger(registry, test, "foo", registry.getCurrentTest());
            registry.setCurrentTest(logger);
            
            MockControl ctrl = MockClassControl.createControl(Task.class);
            Task mock = (Task)ctrl.getMock();

            ctrl.replay();
            test.addTask(mock);
            try {
                test.execute();
                fail("Expected TestSkippedException");
            } catch (TestSkippedException tse) {
                assertEquals("Wrong message", 
                             "testcase foo doesn't match pattern. Skipped.", 
                             tse.getMessage());
            }
            ctrl.verify();
        } finally {
            if (registry != null) {
                registry.shutdown(null);
            }
            TempDir.removeTempFile(resultsDir);
        }
        
    }
   
    private IncludeTask getMockIncludeTask(MockControl ctrl, Object nested, Throwable exception) {
        IncludeTask include = (IncludeTask)ctrl.getMock();
        include.getProject();
        ctrl.setReturnValue(project, 2);
        include.maybeConfigure();
        include.getLocation();
        ctrl.setReturnValue(new Location(""));
        include.getNestedTask();
        ctrl.setReturnValue(nested, 1);
        include.execute();
        if (exception != null) {
            ctrl.setThrowable(exception);
        }
        return include;
    }
}
