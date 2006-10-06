package org.codehaus.xharness.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.codehaus.xharness.log.LineBuffer;
import org.codehaus.xharness.log.LogPriority;
import org.codehaus.xharness.log.TaskLogger;
import org.codehaus.xharness.log.TaskRegistry;
import org.codehaus.xharness.log.TestLogger;
import org.codehaus.xharness.tasks.XharnessTask;
import org.codehaus.xharness.types.AbstractOutput.Stream;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class OutputContainsTest extends TestCase {
    public OutputContainsTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = OutputContainsTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = OutputContainsTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(OutputContainsTest.class);
    }
    
    public void setUp() throws Exception {
        MockTaskRegistry.reset();
    }
    
    public void testNoTask() throws Exception {
        OutputContains condition = new OutputContains();
        condition.setProject(new Project());
        condition.addText("Beam me up, Scottie");
        try {
            condition.eval();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Task \"null\" not found!", be.getMessage());
        }
    }
    
    public void testNoCdata() throws Exception {
        OutputContains condition = new OutputContains();
        assertTrue("Condition evaled incorrectly", condition.eval());
    }
    
    public void testEvalFalse() throws Exception {
        LineBuffer buffer = new LineBuffer(LogPriority.STDOUT);
        buffer.logLine("All good things must come to an end");
        
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
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
        
        OutputContains condition = new OutputContains();
        condition.setProject(new Project());
        condition.addText("Scottie");
        condition.setTask("foo");

        xhCtrl.replay();
        tlCtrl.replay();
        prCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertTrue("Condition evaled incorrectly", !condition.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();
        prCtrl.verify();
    }
    
    
    public void testEvalTrue() throws Exception {
        LineBuffer buffer = new LineBuffer(LogPriority.STDERR);
        buffer.logLine("All good things must come to an end");
        buffer.logLine("Beam me up, Scottie");
        
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
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
        
        OutputContains condition = new OutputContains();
        condition.setProject(new Project());
        condition.addText("Scottie");
        condition.setTask("foo");
        Stream stream = new Stream();
        stream.setValue("stderr");
        condition.setStream(stream);

        xhCtrl.replay();
        tlCtrl.replay();
        prCtrl.replay();
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
        prCtrl.verify();
    }
    
    private static class MockTestLogger extends TestLogger {
        private TaskLogger taskLogger;
        
        public MockTestLogger(TaskRegistry registry, TaskLogger logger) {
            super(registry, null, null, null, null, null);
            taskLogger = logger;
        }
        
        public TaskLogger getTask(String name) {
            return taskLogger;
        }
    }
    
    private static class MockTaskRegistry extends TaskRegistry {
        public MockTaskRegistry() {
            super();
        }
        
        protected static void reset() {
            TaskRegistry.reset();
        }
    }
}
