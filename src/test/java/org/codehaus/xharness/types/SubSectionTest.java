package org.codehaus.xharness.types;

import junit.framework.TestCase;

import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.UnknownElement;
import org.codehaus.xharness.exceptions.FatalException;
import org.codehaus.xharness.log.LineBuffer;
import org.codehaus.xharness.log.LogPriority;
import org.codehaus.xharness.log.TaskLogger;
import org.codehaus.xharness.log.TaskRegistry;
import org.codehaus.xharness.tasks.XharnessTask;
import org.codehaus.xharness.testutil.TestProject;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

public class SubSectionTest extends TestCase {
    private LineBuffer buffer1;
    private LineBuffer buffer2;
    public void setUp() {
        buffer1 = new LineBuffer(LogPriority.STDOUT);
        buffer1.logLine("First I was afraid");
        buffer1.logLine("I was petrified");
        buffer1.logLine("Kept thinking I could never live");
        buffer1.logLine("without you by my side");
        buffer1.logLine("But I spent so many nights");
        buffer1.logLine("thinking how you did me wrong");
        buffer1.logLine("I grew strong");
        buffer1.logLine("I learned how to carry on");
        
        buffer2 = new LineBuffer(LogPriority.STDOUT);
        buffer2.logLine("first start foo end bar");
        buffer2.logLine("second start");
        buffer2.logLine("boo end");
        buffer2.logLine("third start loo");
        buffer2.logLine("end spam");
    }
    
    public void testSetterFatalExceptions() {
        SubSection subSection = new SubSection();
        try {
            subSection.setBeginAfter(-1);
            fail("Expected FatalException");
        } catch (FatalException fe) {
            assertEquals("subsection: beginAfter value must be >= 0", fe.getMessage());
        }
        try {
            subSection.setRepeat(0);
            fail("Expected FatalException");
        } catch (FatalException fe) {
            assertEquals("<subsection> repeat value must be > 0", fe.getMessage());
        }
        subSection.add(new OutputIs());
        try {
            subSection.add(new OutputIs());
            fail("Expected FatalException");
        } catch (FatalException fe) {
            assertEquals("Only one nested condition is supported.", fe.getMessage());
        }
    }

    public void testEvalFatalExceptions() {
        SubSection subSection = new SubSection();
        try {
            subSection.eval();
            fail("Expected FatalException");
        } catch (FatalException fe) {
            assertEquals("You must nest a condition into <subsection>", fe.getMessage());
        }
        subSection.add(new OutputIs());
        try {
            subSection.eval();
            fail("Expected FatalException");
        } catch (FatalException fe) {
            assertEquals("<subsection> requires beginRegex or endRegex attribute", 
                         fe.getMessage());
        }
        subSection.setEndRegex("");
        subSection.setBeginAfter(1);
        try {
            subSection.eval();
            fail("Expected FatalException");
        } catch (FatalException fe) {
            assertEquals("<subsection> beginRegex must be set when beginAfter > 0", 
                         fe.getMessage());
        }
        subSection.setBeginAfter(0);
        subSection.setRepeat(2);
        try {
            subSection.eval();
            fail("Expected FatalException");
        } catch (FatalException fe) {
            assertEquals("<subsection> use of repeat require beginRegex and endRegex attribute", 
                         fe.getMessage());
        }
        subSection.setRepeat(1);
        subSection.setBeginRegex("");
        subSection.setEndRegex(null);
        subSection.setGreedy(true);
        try {
            subSection.eval();
            fail("Expected FatalException");
        } catch (FatalException fe) {
            assertEquals("<subsection> endRegex must be set when greedy=true", 
                         fe.getMessage());
        }
        subSection.setRepeat(2);
        subSection.setEndRegex("");
        try {
            subSection.eval();
            fail("Expected FatalException");
        } catch (FatalException fe) {
            assertEquals("<subsection> can't use repeat and beginAfter with greedy=true", 
                         fe.getMessage());
        }
    }
    
    public void testStartRegex() {
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer1, 4);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar", 2);
        
        OutputContains condition  = new OutputContains();
        condition.setProject(project);
        condition.addText("learned");
        
        SubSection subSection1 = new SubSection();
        subSection1.setProject(project);
        subSection1.setBeginRegex("thinking");
        subSection1.add(condition);
        
        SubSection subSection2 = new SubSection();
        subSection2.setProject(project);
        subSection2.setBeginRegex("foobaring");
        subSection2.add(condition);

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertTrue("Condition evaled incorrectly", subSection1.eval());
            assertFalse("Condition evaled incorrectly", subSection2.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();
        
        String[] logLines = project.getBuffer().toStringArray();
        assertEquals(13, logLines.length);
        assertEquals("Adding reference: ant.PropertyHelper", logLines[0]);
        assertEquals("Found <subsection>", logLines[1]);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for string \"learned\" " 
                     + "in subsection", logLines[2]);
        assertEquals("+++ subsection contents in debug output +++", logLines[3]);
        assertEquals("thinking I could never live", logLines[4]); // ignore the next 4 lines
        assertEquals("I learned how to carry on", logLines[9]);
        assertEquals("+++ end of subsection contents +++", logLines[10]);
        assertEquals("Condition passed: found at least 1 occurrence", logLines[11]);
        assertEquals("Task @@foo/bar@@ output (stdout) Can't find 1st begin of <subsection> " 
                     + "\"foobaring\"", logLines[12]);
    }
    
    public void testEndRegex() {
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer1, 4);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar", 2);
        
        OutputContains condition  = new OutputContains();
        condition.setProject(project);
        condition.addText("thinking");
        
        SubSection subSection1 = new SubSection();
        subSection1.setProject(project);
        subSection1.setEndRegex("many");
        subSection1.add(condition);

        SubSection subSection2 = new SubSection();
        subSection2.setProject(project);
        subSection2.setEndRegex("foobar");
        subSection2.add(condition);

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertTrue("Condition evaled incorrectly", subSection1.eval());
            assertFalse("Condition evaled incorrectly", subSection2.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();
        
        String[] logLines = project.getBuffer().toStringArray();
        assertEquals(12, logLines.length);
        assertEquals("Adding reference: ant.PropertyHelper", logLines[0]);
        assertEquals("Found <subsection>", logLines[1]);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for string \"thinking\" " 
                     + "in subsection", logLines[2]);
        assertEquals("+++ subsection contents in debug output +++", logLines[3]);
        assertEquals("First I was afraid", logLines[4]); // ignore the next 3 lines
        assertEquals("But I spent so ", logLines[8]);
        assertEquals("+++ end of subsection contents +++", logLines[9]);
        assertEquals("Condition passed: found at least 1 occurrence", logLines[10]);
        assertEquals("Task @@foo/bar@@ output (stdout) can't find end of <subsection> \"foobar\"", 
                     logLines[11]);
    }
    
    public void testStartAndEndRegex() {
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer1, 2);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar");
        
        OutputContains condition  = new OutputContains();
        condition.setProject(project);
        condition.addText("thinking");
        
        SubSection subSection = new SubSection();
        subSection.setProject(project);
        subSection.setBeginRegex("afraid");
        subSection.setEndRegex("without");
        subSection.add(condition);

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertTrue("Condition evaled incorrectly", subSection.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();
        
        String[] logLines = project.getBuffer().toStringArray();
        assertEquals(9, logLines.length);
        assertEquals("Adding reference: ant.PropertyHelper", logLines[0]);
        assertEquals("Found <subsection>", logLines[1]);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for string \"thinking\" in subsection", logLines[2]);
        assertEquals("+++ subsection contents in debug output +++", logLines[3]);
        assertEquals("afraid", logLines[4]); // ignore the next 2 lines
        assertEquals("Kept thinking I could never live", logLines[6]);
        assertEquals("+++ end of subsection contents +++", logLines[7]);
        assertEquals("Condition passed: found at least 1 occurrence", logLines[8]);
    }
    
    public void testGreedy() {
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer1, 4);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar", 2);
        
        OutputContains condition  = new OutputContains();
        condition.setProject(project);
        condition.addText("I");
        condition.setMin(5);
        
        SubSection subSection1 = new SubSection();
        subSection1.setProject(project);
        subSection1.setEndRegex("I");
        subSection1.setGreedy(true);
        subSection1.add(condition);
        
        SubSection subSection2 = new SubSection();
        subSection2.setProject(project);
        subSection2.setBeginRegex("how");
        subSection2.setEndRegex("I");
        subSection2.setGreedy(true);
        subSection2.add(condition);

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertTrue("Condition evaled incorrectly", subSection1.eval());
            assertFalse("Condition evaled incorrectly", subSection2.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();
        
        String[] logLines = project.getBuffer().toStringArray();
        //System.out.println(project.getBuffer().toString());
        assertEquals(20, logLines.length);
        assertEquals("Adding reference: ant.PropertyHelper", logLines[0]);
        assertEquals("Found <subsection>", logLines[1]);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for string \"I\" in subsection", 
                     logLines[2]);
        assertEquals("+++ subsection contents in debug output +++", logLines[3]);
        assertEquals("First I was afraid", logLines[4]); // ignore the next 5 lines
        assertEquals("I grew strong", logLines[10]);
        assertEquals("+++ end of subsection contents +++", logLines[11]);
        assertEquals("Condition passed: found at least 5 occurrences", logLines[12]);
        assertEquals("Found <subsection>", logLines[13]);
        assertEquals("Condition failed: found 1 occurrence, required  at least 5", logLines[19]);
    }
    
    public void testBeginAfter() {
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer1, 4);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar", 2);
        
        OutputContains condition  = new OutputContains();
        condition.setProject(project);
        condition.addText("I");
        condition.setMin(3);
        
        SubSection subSection1 = new SubSection();
        subSection1.setProject(project);
        subSection1.setBeginRegex("I");
        subSection1.setBeginAfter(2);
        subSection1.add(condition);
        
        SubSection subSection2 = new SubSection();
        subSection2.setProject(project);
        subSection2.setBeginRegex("I");
        subSection2.setBeginAfter(7);
        subSection2.add(condition);

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertTrue("Condition evaled incorrectly", subSection1.eval());
            assertFalse("Condition evaled incorrectly", subSection2.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();
        
        String[] logLines = project.getBuffer().toStringArray();
        assertEquals(21, logLines.length);
        assertEquals("Adding reference: ant.PropertyHelper", logLines[0]);
        assertEquals("Skipping 1st <subsection>", logLines[1]);
        assertEquals("Skipping 2nd <subsection>", logLines[2]);
        assertEquals("Found 3rd <subsection>", logLines[3]);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for string \"I\" in subsection", 
                     logLines[4]);
        assertEquals("+++ subsection contents in debug output +++", logLines[5]);
        assertEquals("I could never live", logLines[6]); // ignore the next 4 lines
        assertEquals("I learned how to carry on", logLines[11]);
        assertEquals("+++ end of subsection contents +++", logLines[12]);
        assertEquals("Condition passed: found at least 3 occurrences", logLines[13]);
        assertEquals("Skipping 1st <subsection>", logLines[14]);
        assertEquals("Skipping 2nd <subsection>", logLines[15]);
        assertEquals("Skipping 3rd <subsection>", logLines[16]);
        assertEquals("Skipping 4th <subsection>", logLines[17]);
        assertEquals("Skipping 5th <subsection>", logLines[18]);
        assertEquals("Skipping 6th <subsection>", logLines[19]);
        assertEquals("Task @@foo/bar@@ output (stdout) Can't find 7th begin of <subsection> \"I\"", 
                     logLines[20]);
    }
    
    public void testRepeat() {
        TestProject project = new TestProject();
        
        MockControl xhCtrl = MockClassControl.createNiceControl(XharnessTask.class);
        XharnessTask task = (XharnessTask)xhCtrl.getMock();
        task.getProject();
        xhCtrl.setReturnValue(project);
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer2, 4);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar", 7);
        
        OutputRegex condition  = new OutputRegex();
        condition.setProject(project);
        condition.addText("[a-z]oo");
        
        SubSection subSection1 = new SubSection();
        subSection1.setProject(project);
        subSection1.setBeginRegex("star");
        subSection1.setEndRegex("end");
        subSection1.setRepeat(3);
        subSection1.add(condition);
        
        SubSection subSection2 = new SubSection();
        subSection2.setProject(project);
        subSection2.setBeginRegex("star");
        subSection2.setEndRegex("end");
        subSection2.setRepeat(4);
        subSection2.add(condition);

        xhCtrl.replay();
        tlCtrl.replay();
        TaskRegistry registry = null;
        try {
            registry = TaskRegistry.init(task);
            registry.setCurrentTest(new MockTestLogger(registry, logger));
            assertTrue("Condition evaled incorrectly", subSection1.eval());
            assertFalse("Condition evaled incorrectly", subSection2.eval());
        } finally {
            MockTaskRegistry.reset();
        }
        xhCtrl.verify();
        tlCtrl.verify();
        
        String[] logLines = project.getBuffer().toStringArray();
        //System.out.println(project.getBuffer().toString());
        assertEquals(40, logLines.length);
        assertEquals("Adding reference: ant.PropertyHelper", logLines[0]);
        assertEquals("Found 1st <subsection>", logLines[1]);
        assertEquals("Task @@foo/bar@@ output (stdout) searching for regex pattern " 
                     + "\"[a-z]oo\" in subsection", logLines[2]);
        assertEquals("start foo ", logLines[4]);
        assertEquals("Condition passed: found at least 1 occurrence", logLines[6]);
        assertEquals("Found 2nd <subsection>", logLines[7]);
        assertEquals("start", logLines[10]);
        assertEquals("boo ", logLines[11]);
        assertEquals("Found 3rd <subsection>", logLines[14]);
        assertEquals("start loo", logLines[17]);
        assertEquals("Found 1st <subsection>", logLines[20]);
        assertEquals("Found 2nd <subsection>", logLines[26]);
        assertEquals("Found 3rd <subsection>", logLines[33]);
        assertEquals("Task @@foo/bar@@ output (stdout) Can't find 4th begin of " 
                     + "<subsection> \"star\"", logLines[39]);
    }
    
    public void testNestedConditionTypes() {
        TestProject project = new TestProject();
        SubSection subSection = new SubSection();
        
        ComponentHelper ch = ComponentHelper.getComponentHelper(project);
        IntrospectionHelper ih = IntrospectionHelper.getHelper(SubSection.class);
        
        try {
            ih.getElementCreator(project, "", subSection, "outputis", 
                                 new UnknownElement("outputis"));
        } catch (Exception ex) {
            assertEqualsIgnoreCase("class org.codehaus.xharness.types.SubSection doesn't support "
                         + "the nested \"outputis\" element.", ex.getMessage());
        }
        
        ch.addDataTypeDefinition("outputis", OutputIs.class);
        assertNotNull(ih.getElementCreator(project, "", subSection, "outputis", 
                                           new UnknownElement("outputis")));
        
        assertNotNull(ih.getElementCreator(project, "", subSection, "and", 
                                           new UnknownElement("and")));
        
        assertNotNull(ih.getElementCreator(project, "", subSection, "or", 
                                           new UnknownElement("or")));
        
        assertNotNull(ih.getElementCreator(project, "", subSection, "not", 
                                           new UnknownElement("not")));
        
        try {
            ih.getElementCreator(project, "", subSection, "available", 
                                 new UnknownElement("available"));
        } catch (Exception ex) {
            assertEqualsIgnoreCase("class org.codehaus.xharness.types.SubSection doesn't support "
                         + "the nested \"available\" element.", ex.getMessage());
        }
    }
    
    private static void assertEqualsIgnoreCase(String s1, String s2) {
        assertEquals(s1.toLowerCase(), s2.toLowerCase());
    }
}
