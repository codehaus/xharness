package org.codehaus.xharness.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.codehaus.xharness.exceptions.FatalException;
import org.codehaus.xharness.log.LineBuffer;
import org.codehaus.xharness.log.LogPriority;
import org.codehaus.xharness.log.TaskLogger;
import org.codehaus.xharness.log.TaskRegistry;
import org.codehaus.xharness.tasks.XharnessTask;
import org.codehaus.xharness.testutil.TestProject;
import org.codehaus.xharness.types.AbstractOutput.Stream;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class OutputRegexTest extends TestCase {
    public OutputRegexTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = OutputRegexTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = OutputRegexTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(OutputRegexTest.class);
    }
    
    public void setUp() throws Exception {
        MockTaskRegistry.reset();
    }
    
    public void testNoCdata() throws Exception {
        OutputRegex condition = new OutputRegex();
        try {
            condition.eval();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Missing regular expression", be.getMessage());
        }
    }
    
    public void testNoTask() throws Exception {
        OutputRegex condition = new OutputRegex();
        condition.setProject(new Project());
        condition.addText("Beam me up, Scottie");
        try {
            condition.eval();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Task not found!", be.getMessage());
        }
    }
    
    public void testEvalFalse() throws Exception {
        LineBuffer buffer = new LineBuffer(LogPriority.STDOUT);
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
        
        OutputRegex condition = new OutputRegex();
        condition.setProject(project);
        condition.addText("Scottie");
        condition.setTask("foo");

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
        
        String[] logLines = project.getBuffer().toStringArray(Project.MSG_VERBOSE);
        assertEquals(2, logLines.length);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for regex pattern \"Scottie\"", 
                     logLines[0]);
        assertEquals("Condition failed: found 0 occurrences, required  at least 1", logLines[1]);
    }
    
    public void testEvalTrue() throws Exception {
        LineBuffer buffer = new LineBuffer(LogPriority.STDERR);
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
        tlCtrl.setReturnValue(buffer);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputRegex condition = new OutputRegex();
        condition.setProject(project);
        condition.addText("[a-z],\\s[A-Z]");
        condition.setTask("foo");
        Stream stream = new Stream();
        stream.setValue("stderr");
        condition.setStream(stream);

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
        
        String[] logLines = project.getBuffer().toStringArray(Project.MSG_VERBOSE);
        assertEquals(2, logLines.length);
        assertEquals("Task @@foo/bar@@ output (stderr) searching for regex pattern " 
                     + "\"[a-z],\\s[A-Z]\"", logLines[0]);
        assertEquals("Condition passed: found at least 1 occurrence", logLines[1]);
    }

    public void testMinAndMax() throws Exception {
        LineBuffer buffer = new LineBuffer(0);
        buffer.logLine("one two three xxxx four");
        buffer.logLine("four three xxxxx three");
        buffer.logLine("two one zero xxxxxx");
        
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer, 6);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar", 5);
        
        OutputRegex condition0 = new OutputRegex();
        condition0.setProject(project);
        condition0.addText("foo");
        condition0.setTask("foo");
        condition0.setStream(Stream.getStream(0));
        try {
            condition0.setMin(0);
            fail("Expected FatalException");
        } catch(FatalException fe) {
            assertEquals("<OutputRegex> min value must be > 0", fe.getMessage());
        }
        try {
            condition0.setMax(0);
            fail("Expected FatalException");
        } catch(FatalException fe) {
            assertEquals("<OutputRegex> max value must be > 0", fe.getMessage());
        }
        condition0.setMin(2);
        condition0.setMax(1);

        OutputRegex condition1 = new OutputRegex();
        condition1.setProject(project);
        condition1.addText("three");
        condition1.setTask("foo");
        condition1.setStream(Stream.getStream(0));
        condition1.setMin(3);
        
        OutputRegex condition2 = new OutputRegex();
        condition2.setProject(project);
        condition2.addText("three");
        condition2.setTask("foo");
        condition2.setStream(Stream.getStream(0));
        condition2.setMin(4);
        
        OutputRegex condition3 = new OutputRegex();
        condition3.setProject(project);
        condition3.addText("three");
        condition3.setTask("foo");
        condition3.setStream(Stream.getStream(0));
        condition3.setMax(3);
        
        OutputRegex condition4 = new OutputRegex();
        condition4.setProject(project);
        condition4.addText("three");
        condition4.setTask("foo");
        condition4.setStream(Stream.getStream(0));
        condition4.setMax(2);
        
        OutputRegex condition5 = new OutputRegex();
        condition5.setProject(project);
        condition5.addText("xxx");
        condition5.setTask("foo");
        condition5.setStream(Stream.getStream(0));
        condition5.setMin(3);
        condition5.setMax(5);

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            try {
                condition0.eval();
                fail("Expected FatalException");
            } catch(FatalException fe) {
                assertEquals("<OutputRegex> min value must be <= max value", fe.getMessage());
            }
            assertTrue("Condition evaled incorrectly", condition1.eval());
            assertFalse("Condition evaled incorrectly", condition2.eval());
            assertTrue("Condition evaled incorrectly", condition3.eval());
            assertFalse("Condition evaled incorrectly", condition4.eval());
            assertTrue("Condition evaled incorrectly", condition5.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        String[] logLines = project.getBuffer().toStringArray(Project.MSG_VERBOSE);
        assertEquals(10, logLines.length);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for regex pattern \"three\"", 
                     logLines[0]);
        assertEquals("Condition passed: found at least 3 occurrences", logLines[1]);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for regex pattern \"three\"", 
                     logLines[2]);
        assertEquals("Condition failed: found 3 occurrences, required  at least 4", logLines[3]);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for regex pattern \"three\"", 
                     logLines[4]);
        assertEquals("Condition passed: found 3 occurrences", logLines[5]);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for regex pattern \"three\"", 
                     logLines[6]);
        assertEquals("Condition failed: found more than 2 occurrences", logLines[7]);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for regex pattern \"xxx\"", 
                     logLines[8]);
        assertEquals("Condition passed: found 4 occurrences", logLines[9]);
    }
    

    public void testDocMode() throws Exception {
        LineBuffer buffer = new LineBuffer(0);
        buffer.logLine("one two three");
        buffer.logLine("four five six");
        
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
        tlCtrl.setReturnValue("foo/bar", 2);
        
        OutputRegex condition1 = new OutputRegex();
        condition1.setProject(project);
        condition1.addText(".*e\\nf.*");
        condition1.setTask("foo");
        condition1.setStream(Stream.getStream(0));
        
        OutputRegex condition2 = new OutputRegex();
        condition2.setProject(project);
        condition2.addText(".*e\\nf.*");
        condition2.setTask("foo");
        condition2.setStream(Stream.getStream(0));
        condition2.setDocMode(true);
        
        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertFalse("Condition evaled incorrectly", condition1.eval());
            assertTrue("Condition evaled incorrectly", condition2.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();

        String[] logLines = project.getBuffer().toStringArray(Project.MSG_VERBOSE);
        assertEquals(4, logLines.length);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for regex pattern \".*e\\nf.*\"", 
                     logLines[0]);
        assertEquals("Condition failed: found 0 occurrences, required  at least 1", logLines[1]);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for regex pattern \".*e\\nf.*\"", 
                     logLines[2]);
        assertEquals("Condition passed: found at least 1 occurrence", logLines[3]);
    }
}
