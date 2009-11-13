package org.codehaus.xharness.log;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.UnknownElement;

import org.codehaus.xharness.tasks.IncludeTask;
import org.codehaus.xharness.tasks.TestCaseTask;
import org.codehaus.xharness.tasks.XharnessTask;
import org.codehaus.xharness.testutil.TempDir;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TaskRegistryTest extends TestCase {
    public TaskRegistryTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = TaskRegistryTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = TaskRegistryTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(TaskRegistryTest.class);
    }
    
    public void setUp() throws Exception {
        TaskRegistry.reset();
    }
    
    public void tearDown() throws Exception {
        TaskRegistry.reset();
    }
    
    public void testInit() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        MockControl tkCtrl = MockClassControl.createControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)tkCtrl.getMock();
        task.getPattern();
        tkCtrl.setReturnValue(null);
        task.getName();
        tkCtrl.setReturnValue("foo");
        task.getResultsdir();
        tkCtrl.setReturnValue(null, 2);
        task.getBasedir();
        tkCtrl.setReturnValue(null);
        task.getProject();
        tkCtrl.setReturnValue(project);
        
        prCtrl.replay();
        tkCtrl.replay();
        
        TaskRegistry registry = TaskRegistry.init(task);
        assertEquals("Wrong registry", registry, TaskRegistry.getRegistry());
        assertEquals("Wrong formatter class", 
                     ResultFormatter.class.getName(), 
                     registry.getFormatter().getClass().getName());
        assertEquals("Wrong project", project, registry.getProject());
        assertEquals("Wrong testlogger", task, registry.getCurrentTest().getTask()); 
        prCtrl.verify();
        tkCtrl.verify();
    }
    
    public void testPattern() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        MockControl xhCtrl = MockClassControl.createControl(XharnessTask.class);
        XharnessTask xhTask = (XharnessTask)xhCtrl.getMock();
        xhTask.getPattern();
        xhCtrl.setReturnValue("foo/bar/spam");
        xhTask.getName();
        xhCtrl.setReturnValue("foo");
        xhTask.getResultsdir();
        xhCtrl.setReturnValue(null, 2);
        xhTask.getBasedir();
        xhCtrl.setReturnValue(null, 3);
        xhTask.getProject();
        xhCtrl.setReturnValue(project);
        xhTask.getRuntimeConfigurableWrapper();
        xhCtrl.setReturnValue(new RuntimeConfigurable(new Object(), "xharness"));
        
        MockControl tkCtrl = MockClassControl.createControl(TestCaseTask.class);
        TestCaseTask tcTask = (TestCaseTask)tkCtrl.getMock();
        tcTask.getRuntimeConfigurableWrapper();
        tkCtrl.setReturnValue(new RuntimeConfigurable(new Object(), "testcase"));

        prCtrl.replay();
        xhCtrl.replay();
        tkCtrl.replay();
        
        assertEquals("Incorrect match", true, TaskRegistry.matchesPattern(xhTask));
        TaskRegistry registry = TaskRegistry.init(xhTask);
        assertEquals("Incorrect match", false, TaskRegistry.matchesPattern(null));
        assertEquals("Incorrect match", true, TaskRegistry.matchesPattern(xhTask));
        TestLogger logger = new TestLogger(registry, tcTask, "bar", registry.getCurrentTest());
        registry.setCurrentTest(logger);
        assertEquals("Incorrect match", false, TaskRegistry.matchesPattern(tcTask));
        logger = new TestLogger(registry, tcTask, "spam", logger);
        registry.setCurrentTest(logger);
        assertEquals("Incorrect match", false, TaskRegistry.matchesPattern(xhTask));
        assertEquals("Incorrect match", true, TaskRegistry.matchesPattern(tcTask));

        prCtrl.verify();
        xhCtrl.verify();
        tkCtrl.verify();
    }
    
    public void testGetTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask xhTask = (XharnessTask)xhCtrl.getMock();
        xhTask.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class); 
        TestLogger logger = (TestLogger)tlCtrl.getMock();
        logger.getTask("blah");
        TaskLogger subLogger = new TaskLogger();
        tlCtrl.setReturnValue(subLogger);

        prCtrl.replay();
        xhCtrl.replay();
        tlCtrl.replay();
        
        assertEquals("Incorrect task", null, TaskRegistry.getLogger("blah"));
        TaskRegistry registry = TaskRegistry.init(xhTask);
        assertEquals("Incorrect task", null, TaskRegistry.getLogger("blah"));
        registry.setCurrentTest(logger);
        assertEquals("Incorrect match", subLogger, TaskRegistry.getLogger("blah"));

        prCtrl.verify();
        xhCtrl.verify();
        tlCtrl.verify();
    }
    
    public void testSetErrorProperty() throws Exception {
        MockControl prCtrl = MockClassControl.createControl(Project.class);
        prCtrl.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
        Project project = (Project)prCtrl.getMock();
        project.setNewProperty("prop", "true");
        prCtrl.setVoidCallable(1);
        project.addBuildListener(null);
        prCtrl.setVoidCallable(3);

        MockControl xhCtrl = MockClassControl.createControl(XharnessTask.class);
        XharnessTask xhTask = (XharnessTask)xhCtrl.getMock();
        xhTask.getName();
        xhCtrl.setReturnValue("foo", 3);
        xhTask.getPattern();
        xhCtrl.setReturnValue(null, 3);
        xhTask.getBasedir();
        xhCtrl.setReturnValue(null, 3);
        xhTask.getResultsdir();
        xhCtrl.setReturnValue(null, 6);
        xhTask.getProject();
        xhCtrl.setReturnValue(project, 3);
        xhTask.getErrorProperty();
        xhCtrl.setReturnValue(null);
        xhCtrl.setReturnValue("prop", 2);
        
        prCtrl.replay();
        xhCtrl.replay();
        
        TaskRegistry.setErrorProperty(new Exception());
        TaskRegistry.init(xhTask);
        TaskRegistry.setErrorProperty(null);
        TaskRegistry.init(xhTask);
        TaskRegistry.setErrorProperty(new Exception());
        TaskRegistry.init(xhTask);
        TaskRegistry.setErrorProperty(new Exception());

        prCtrl.verify();
        xhCtrl.verify();
    }
    
    public void testShutDown() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask xhTask = (XharnessTask)xhCtrl.getMock();
        xhTask.getProject();
        xhCtrl.setReturnValue(project);
        
        Exception ex = new Exception();
        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class); 
        TestLogger logger = (TestLogger)tlCtrl.getMock();
        logger.setFailure(null);
        logger.setFailure(ex);
        logger.taskFinishedInternal();
        tlCtrl.setVoidCallable(2);

        prCtrl.replay();
        xhCtrl.replay();
        tlCtrl.replay();
        
        TaskRegistry registry = TaskRegistry.init(xhTask);
        registry.setCurrentTest(logger);
        registry.shutdown(null);
        registry.shutdown(ex);

        prCtrl.verify();
        xhCtrl.verify();
        tlCtrl.verify();
    }
    
    public void testSetCurrentTest() throws Exception {
        File baseDir = new File(".");
        File testDir = new File(baseDir, "foo/bar");
        
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        project.setUserProperty(TaskRegistry.CURRENT_TEST_DIR_PROPERY, baseDir.getAbsolutePath());
        project.setUserProperty(TaskRegistry.CURRENT_TEST_DIR_PROPERY, testDir.getAbsolutePath());

        MockControl xhCtrl = MockClassControl.createControl(XharnessTask.class);
        XharnessTask xhTask = (XharnessTask)xhCtrl.getMock();
        xhTask.getName();
        xhCtrl.setReturnValue(null);
        xhTask.getPattern();
        xhCtrl.setReturnValue(null);
        xhTask.getResultsdir();
        xhCtrl.setReturnValue(null, 2);
        xhTask.getBasedir();
        xhCtrl.setReturnValue(baseDir, 8);
        xhTask.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TestLogger.class); 
        TestLogger logger = (TestLogger)tlCtrl.getMock();
        logger.getFullName();
        tlCtrl.setReturnValue(null);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");

        prCtrl.replay();
        xhCtrl.replay();
        tlCtrl.replay();
        
        TaskRegistry registry = TaskRegistry.init(xhTask);
        TestLogger baseLogger = registry.getCurrentTest();
        registry.setCurrentTest(logger);
        registry.setCurrentTest(baseLogger);
        registry.setCurrentTest(logger);

        prCtrl.verify();
        xhCtrl.verify();
        tlCtrl.verify();
    }
    
    public void testTaskId() throws Exception {
        File tempDir = TempDir.createTempDir();
        
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        MockControl tkCtrl = MockClassControl.createControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)tkCtrl.getMock();
        task.getPattern();
        tkCtrl.setReturnValue(null, 2);
        task.getName();
        tkCtrl.setReturnValue("foo", 2);
        task.getResultsdir();
        tkCtrl.setReturnValue(tempDir, 5);
        task.getBasedir();
        tkCtrl.setReturnValue(null, 2);
        task.getProject();
        tkCtrl.setReturnValue(project, 2);

        prCtrl.replay();
        tkCtrl.replay();

        TaskRegistry registry = TaskRegistry.init(task);
        assertEquals(1, registry.getNextId());
        assertEquals(2, registry.getNextId());
        assertFalse(new File(tempDir, "xharness.properties").exists());
        registry.shutdown(null);
        assertTrue(new File(tempDir, "xharness.properties").isFile());
        assertEquals(3, registry.getNextId());
        assertEquals(4, registry.getNextId());
        registry = TaskRegistry.init(task);
        assertEquals(4, registry.getNextId());
        assertEquals(5, registry.getNextId());
        prCtrl.verify();
        tkCtrl.verify();

        TempDir.removeTempFile(tempDir);
    }
    
    public void testUnwrapComponent() {
        MockControl pcCtrl1 = MockClassControl.createControl(ProjectComponent.class);
        ProjectComponent pc1 = (ProjectComponent)pcCtrl1.getMock();

        MockControl pcCtrl2 = MockClassControl.createControl(TaskAdapter.class);
        TaskAdapter pc2 = (TaskAdapter)pcCtrl2.getMock();
        pc2.getProxy();
        pcCtrl2.setReturnValue(pc1);
        pc2.getProxy();
        pcCtrl2.setReturnValue(null);

        MockControl pcCtrl3 = MockClassControl.createControl(IncludeTask.class);
        IncludeTask pc3 = (IncludeTask)pcCtrl3.getMock();
        pc3.getNestedTask();
        pcCtrl3.setReturnValue(pc2, 2);

        MockControl pcCtrl4 = MockClassControl.createControl(UnknownElement.class);
        UnknownElement pc4 = (UnknownElement)pcCtrl4.getMock();
        pc4.getRealThing();
        pcCtrl4.setReturnValue(pc3);
        pc4.getRealThing();
        pcCtrl4.setReturnValue(new Object());
        
        pcCtrl1.replay();
        pcCtrl2.replay();
        pcCtrl3.replay();
        pcCtrl4.replay();
        
        // 4->3->2->1
        assertEquals(pc1, TaskRegistry.unwrapComponent(pc4));
        // 4 (pc4.getRealThing() returns Object not ProjectComponent)
        assertEquals(pc4, TaskRegistry.unwrapComponent(pc4));
        // 3->2 (pc2.getProxy() returns null)
        assertEquals(pc2, TaskRegistry.unwrapComponent(pc3));
        
        pcCtrl1.verify();
        pcCtrl2.verify();
        pcCtrl3.verify();
        pcCtrl4.verify();
    }
}
