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

public class OutputIsTest extends TestCase {
    public OutputIsTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = OutputIsTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = OutputIsTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(OutputIsTest.class);
    }
    
    public void setUp() throws Exception {
        MockTaskRegistry.reset();
    }
    
    public void testCdataAndString() throws Exception {
        MockControl prCtrl = MockClassControl.createControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        project.replaceProperties("foo");
        prCtrl.setReturnValue("spam");
        project.replaceProperties("bar");
        prCtrl.setReturnValue("eggs");
        
        prCtrl.replay();

        OutputIs cond1 = new OutputIs();
        cond1.setProject(project);
        cond1.setString("foo");
        assertEquals("Wrong text", "spam", cond1.getText());
        try {
            cond1.addText("bar");
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Cannot use string argument and CDATA", be.getMessage());
        }

        OutputIs cond2 = new OutputIs();
        cond2.setProject(project);
        cond2.addText("bar");
        assertEquals("Wrong text", "eggs", cond2.getText());
        try {
            cond2.setString("foo");
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Cannot use string argument and CDATA", be.getMessage());
        }
        
        prCtrl.verify();
    }
    
    public void testNoTask() throws Exception {
        OutputIs condition = new OutputIs();
        condition.setProject(new Project());
        condition.addText("Beam me up, Scottie");
        try {
            condition.eval();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Task \"null\" not found!", be.getMessage());
        }
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
        
        OutputIs condition = new OutputIs();
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
    
    public void testEvalTrueCdata() throws Exception {
        LineBuffer buffer = new LineBuffer(LogPriority.STDOUT);
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
        
        OutputIs condition = new OutputIs();
        condition.setProject(new Project());
        condition.addText("Beam me up, Scottie");
        condition.setTask("foo");

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
    
    
    public void testEvalTrueString() throws Exception {
        LineBuffer buffer = new LineBuffer(LogPriority.STDOUT);
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
        
        OutputIs condition = new OutputIs();
        condition.setProject(new Project());
        condition.setString("Beam me up, Scottie");
        condition.setTask("foo");

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
    public void testEvalFailing() throws Exception {
        LineBuffer buffer = new LineBuffer(LogPriority.STDOUT);
        
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
        
        OutputIs condition = new OutputIs();
        condition.setProject(new Project());
        condition.addText("Beam me up, Scottie");
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
    
    public void testEvalEmpty() throws Exception {
        LineBuffer buffer = new LineBuffer(LogPriority.STDERR);
        
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
        
        OutputIs condition = new OutputIs();
        condition.setProject(new Project());
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
    
    public void testEvalEmptyFailing() throws Exception {
        LineBuffer buffer = new LineBuffer(LogPriority.STDOUT);
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
        
        OutputIs condition = new OutputIs();
        condition.setProject(new Project());
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
