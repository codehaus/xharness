package org.codehaus.xharness.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.codehaus.xharness.log.LineBuffer;
import org.codehaus.xharness.log.TaskLogger;
import org.codehaus.xharness.log.TaskRegistry;
import org.codehaus.xharness.tasks.XharnessTask;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class OutputSizeTest extends TestCase {
    public OutputSizeTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = OutputSizeTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = OutputSizeTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(OutputSizeTest.class);
    }
    
    public void setUp() throws Exception {
        MockTaskRegistry.reset();
    }
    
    public void testEqualsAndLarger() throws Exception {
        OutputSize condition = new OutputSize();
        condition.setEquals(3);
        condition.setLarger(1);
        condition.setProject(new Project());
        try {
            condition.eval();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Can only set one of: equals, larger, smaller", be.getMessage());
        }
    }
    
    public void testEqualsAndSmaller() throws Exception {
        OutputSize condition = new OutputSize();
        condition.setEquals(1);
        condition.setSmaller(3);
        condition.setProject(new Project());
        try {
            condition.eval();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Can only set one of: equals, larger, smaller", be.getMessage());
        }
    }
    
    public void testLargerAndSmaller() throws Exception {
        OutputSize condition = new OutputSize();
        condition.setLarger(1);
        condition.setSmaller(3);
        condition.setProject(new Project());
        try {
            condition.eval();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Can only set one of: equals, larger, smaller", be.getMessage());
        }
    }
    
    public void testNoDimension() throws Exception {
        OutputSize condition = new OutputSize();
        condition.setProject(new Project());
        try {
            condition.eval();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Must set one of: equals, larger, smaller", be.getMessage());
        }
    }
    
    public void testWrongMode() throws Exception {
        OutputSize condition = new OutputSize();
        condition.setProject(new Project());
        condition.setEquals(1);
        condition.setMode("invalid");
        try {
            condition.eval();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Invalid mode attribute: invalid", be.getMessage());
        }
    }
    
    public void testNoTask() throws Exception {
        OutputSize condition = new OutputSize();
        condition.setProject(new Project());
        condition.setEquals(1);
        try {
            condition.eval();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Task not found!", be.getMessage());
        }
    }
    
    public void testLinesEqualsPass() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine("All good things must come to an end");
        
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer, 2);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputSize condition = new OutputSize();
        condition.setProject(project);
        condition.setTask("foo");
        condition.setEquals(1);

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertTrue("Condition evaled incorrectly", condition.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        assertEquals("Task @@foo/bar@@ output (stdout) size is 1 line.", 
                     project.getBuffer().toString(Project.MSG_VERBOSE));
    }
    
    public void testLinesEqualsFails() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine("All good things must come to an end");
        buffer.logLine("Beam me up, Scottie");
        
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer, 2);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputSize condition = new OutputSize();
        condition.setProject(project);
        condition.setTask("foo");
        condition.setEquals(1);

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertFalse("Condition evaled incorrectly", condition.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        assertEquals("Task @@foo/bar@@ output (stdout) size is 2 lines.", 
                     project.getBuffer().toString(Project.MSG_VERBOSE));
    }
    
    public void testCharsEqualsPass() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine("All good things must come to an end");
        
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer, 2);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputSize condition = new OutputSize();
        condition.setProject(project);
        condition.setTask("foo");
        condition.setEquals(35);
        condition.setMode("char");

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertTrue("Condition evaled incorrectly", condition.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        assertEquals("Task @@foo/bar@@ output (stdout) size is 35 characters.", 
                     project.getBuffer().toString(Project.MSG_VERBOSE));
    }
    
    public void testCharsEqualsFails() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine("All good things must come to an end");
        
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer, 2);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputSize condition = new OutputSize();
        condition.setProject(project);
        condition.setTask("foo");
        condition.setEquals(34);
        condition.setMode("char");

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertFalse("Condition evaled incorrectly", condition.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        assertEquals("Task @@foo/bar@@ output (stdout) size is 35 characters.", 
                     project.getBuffer().toString(Project.MSG_VERBOSE));
    }
    
    public void testLinesLargerPass() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine("All good things must come to an end");
        buffer.logLine("Beam me up, Scottie");
        
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer, 2);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputSize condition = new OutputSize();
        condition.setProject(project);
        condition.setTask("foo");
        condition.setLarger(1);

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertTrue("Condition evaled incorrectly", condition.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        assertEquals("Task @@foo/bar@@ output (stdout) size is 2 lines.", 
                     project.getBuffer().toString(Project.MSG_VERBOSE));
    }
    
    public void testLinesLargerFails() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine("All good things must come to an end");
        buffer.logLine("Beam me up, Scottie");
        
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer, 2);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputSize condition = new OutputSize();
        condition.setProject(project);
        condition.setTask("foo");
        condition.setLarger(3);

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertFalse("Condition evaled incorrectly", condition.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        assertEquals("Task @@foo/bar@@ output (stdout) size is 2 lines.", 
                     project.getBuffer().toString(Project.MSG_VERBOSE));
    }
    
    public void testCharsLargerPass() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine("All good things must come to an end");
        
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer, 2);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputSize condition = new OutputSize();
        condition.setProject(project);
        condition.setTask("foo");
        condition.setLarger(34);
        condition.setMode("char");

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertTrue("Condition evaled incorrectly", condition.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        assertEquals("Task @@foo/bar@@ output (stdout) size is 35 characters.", 
                     project.getBuffer().toString(Project.MSG_VERBOSE));
    }
    
    public void testCharsLargerFails() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine("All good things must come to an end");
        
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer, 2);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputSize condition = new OutputSize();
        condition.setProject(project);
        condition.setTask("foo");
        condition.setLarger(35);
        condition.setMode("char");

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertFalse("Condition evaled incorrectly", condition.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        assertEquals("Task @@foo/bar@@ output (stdout) size is 35 characters.", 
                     project.getBuffer().toString(Project.MSG_VERBOSE));
    }
    
    public void testLinesSmallerPass() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine("All good things must come to an end");
        buffer.logLine("Beam me up, Scottie");
        
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer, 2);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputSize condition = new OutputSize();
        condition.setProject(project);
        condition.setTask("foo");
        condition.setSmaller(3);

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertTrue("Condition evaled incorrectly", condition.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        assertEquals("Task @@foo/bar@@ output (stdout) size is 2 lines.", 
                     project.getBuffer().toString(Project.MSG_VERBOSE));
    }
    
    public void testLinesSmallerFails() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine("All good things must come to an end");
        buffer.logLine("Beam me up, Scottie");
        
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer, 2);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputSize condition = new OutputSize();
        condition.setProject(project);
        condition.setTask("foo");
        condition.setSmaller(2);

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertFalse("Condition evaled incorrectly", condition.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        assertEquals("Task @@foo/bar@@ output (stdout) size is 2 lines.", 
                     project.getBuffer().toString(Project.MSG_VERBOSE));
    }
    
    public void testCharsSmallerPass() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine("All good things must come to an end");
        
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer, 2);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputSize condition = new OutputSize();
        condition.setProject(project);
        condition.setTask("foo");
        condition.setSmaller(36);
        condition.setMode("char");

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertTrue("Condition evaled incorrectly", condition.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        assertEquals("Task @@foo/bar@@ output (stdout) size is 35 characters.", 
                     project.getBuffer().toString(Project.MSG_VERBOSE));
    }
    
    public void testCharsSmallerFails() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine("All good things must come to an end");
        
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer, 2);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputSize condition = new OutputSize();
        condition.setProject(project);
        condition.setTask("foo");
        condition.setLarger(35);
        condition.setMode("char");

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertFalse("Condition evaled incorrectly", condition.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        assertEquals("Task @@foo/bar@@ output (stdout) size is 35 characters.", 
                     project.getBuffer().toString(Project.MSG_VERBOSE));
    }
}
