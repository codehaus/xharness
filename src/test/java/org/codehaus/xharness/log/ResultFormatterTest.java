package org.codehaus.xharness.log;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;

import org.codehaus.xharness.testutil.TempDir;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ResultFormatterTest extends TestCase {
    private File resultsDir;

    public ResultFormatterTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ResultFormatterTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ResultFormatterTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(ResultFormatterTest.class);
    }
    
    public void setUp() throws Exception {
        resultsDir = TempDir.createTempDir();
    }
    
    public void tearDown() throws Exception {
        TempDir.removeTempFile(resultsDir);
    }
    
    public void testTask() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine(1, "Beam me up, Scottie!");
        buffer.logLine(2, "All good things must come to an end.");
        buffer.logLine(2, "Must you be so linear, Jean-Luc?");
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getTaskType();
        tlCtrl.setReturnValue(Result.OTHER_TASK);
        logger.getFullName();
        tlCtrl.setReturnValue("", 2);
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer);
        logger.getId();
        tlCtrl.setReturnValue(1);
        logger.getParentName();
        tlCtrl.setReturnValue("foo");
        logger.getName();
        tlCtrl.setReturnValue("bar");
        logger.getReference();
        tlCtrl.setReturnValue("spam");
        
        tlCtrl.replay();
        
        ResultFormatter formatter = new ResultFormatter(resultsDir);
        formatter.writeResults(logger, Result.PASSED, "description", 12345L);
        Element elem = getDoc("TASK_.xml");
        verifyResult(elem, "task", "Passed");
        assertEquals("Wrong order id", "1", elem.getAttribute(XMLConstants.ATTR_ORDERID));
        assertEquals("Wrong time", "12.345", elem.getAttribute(XMLConstants.ATTR_TIME));
        assertEquals("Wrong parent", "foo", elem.getAttribute(XMLConstants.ATTR_PARENT));
        assertEquals("Wrong name", "bar", elem.getAttribute(XMLConstants.ATTR_TASK_NAME));
        assertEquals("Wrong fullname", "", elem.getAttribute(XMLConstants.ATTR_FULL_NAME));
        assertEquals("Wrong reference", "spam", elem.getAttribute(XMLConstants.ATTR_REFERENCE));
        assertEquals("Wrong ret val", "", elem.getAttribute(XMLConstants.ATTR_RETVAL));
        assertNull("Wrong command", elem.getElementsByTagName(XMLConstants.COMMAND).item(0));
        assertEquals("Wrong owner", "", elem.getAttribute(XMLConstants.ATTR_OWNER));
        tlCtrl.verify();
    }
    
    public void testTwoTasks() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine(1, "Beam me up, Scottie!");
        buffer.logLine(2, "All good things must come to an end.");
        buffer.logLine(2, "Must you be so linear, Jean-Luc?");
        
        MockControl tlCtrl1 = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger1 = (TaskLogger)tlCtrl1.getMock();
        logger1.getTaskType();
        tlCtrl1.setReturnValue(Result.OTHER_TASK);
        logger1.getFullName();
        tlCtrl1.setReturnValue("foo/bar", 2);
        logger1.getLineBuffer();
        tlCtrl1.setReturnValue(buffer);
        logger1.getId();
        tlCtrl1.setReturnValue(1);
        logger1.getParentName();
        tlCtrl1.setReturnValue("bar");
        logger1.getName();
        tlCtrl1.setReturnValue("spam");
        logger1.getReference();
        tlCtrl1.setReturnValue("eggs");
        
        
        MockControl tlCtrl2 = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger2 = (TaskLogger)tlCtrl2.getMock();
        logger2.getTaskType();
        tlCtrl2.setReturnValue(Result.OTHER_TASK);
        logger2.getFullName();
        tlCtrl2.setReturnValue("foo/bar", 2);
        logger2.getLineBuffer();
        tlCtrl2.setReturnValue(buffer);
        logger2.getId();
        tlCtrl2.setReturnValue(2);
        logger2.getParentName();
        tlCtrl2.setReturnValue("c");
        logger2.getName();
        tlCtrl2.setReturnValue("d");
        logger2.getReference();
        tlCtrl2.setReturnValue("e");

        tlCtrl1.replay();
        tlCtrl2.replay();

        ResultFormatter formatter = new ResultFormatter(resultsDir);
        formatter.writeResults(logger1, Result.PASSED, "description1", 12345L);
        formatter.writeResults(logger2, Result.FAILED, "description2", 54321L);
        Element elem = getDoc("TASK_foo_bar.xml");
        verifyResult(elem, "task", "Passed");
        assertEquals("Wrong order id", "1", elem.getAttribute(XMLConstants.ATTR_ORDERID));
        assertEquals("Wrong time", "12.345", elem.getAttribute(XMLConstants.ATTR_TIME));
        assertEquals("Wrong parent", "bar", elem.getAttribute(XMLConstants.ATTR_PARENT));
        assertEquals("Wrong name", "spam", elem.getAttribute(XMLConstants.ATTR_TASK_NAME));
        assertEquals("Wrong fullname", "foo/bar", elem.getAttribute(XMLConstants.ATTR_FULL_NAME));
        assertEquals("Wrong reference", "eggs", elem.getAttribute(XMLConstants.ATTR_REFERENCE));
        elem = getDoc("TASK_foo_bar_1.xml");
        verifyResult(elem, "task", "Failed");
        assertEquals("Wrong order id", "2", elem.getAttribute(XMLConstants.ATTR_ORDERID));
        assertEquals("Wrong time", "54.321", elem.getAttribute(XMLConstants.ATTR_TIME));
        assertEquals("Wrong parent", "c", elem.getAttribute(XMLConstants.ATTR_PARENT));
        assertEquals("Wrong name", "d", elem.getAttribute(XMLConstants.ATTR_TASK_NAME));
        assertEquals("Wrong fullname", "foo/bar", elem.getAttribute(XMLConstants.ATTR_FULL_NAME));
        assertEquals("Wrong reference", "e", elem.getAttribute(XMLConstants.ATTR_REFERENCE));

        tlCtrl1.verify();
        tlCtrl2.verify();
    }

    public void testProcessTask() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine(1, "Beam me up, Scottie!");
        buffer.logLine(2, "All good things must come to an end.");
        buffer.logLine(2, "Must you be so linear, Jean-Luc?");
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getTaskType();
        tlCtrl.setReturnValue(Result.PROCESS_TASK);
        logger.getFullName();
        tlCtrl.setReturnValue("_foo/bar", 2);
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer);
        logger.getId();
        tlCtrl.setReturnValue(1);
        logger.getParentName();
        tlCtrl.setReturnValue("foo");
        logger.getName();
        tlCtrl.setReturnValue("bar");
        logger.getReference();
        tlCtrl.setReturnValue(null);
        logger.getRetVal();
        tlCtrl.setReturnValue(0);
        logger.getCommand();
        tlCtrl.setReturnValue("spam");
        
        tlCtrl.replay();
        
        ResultFormatter formatter = new ResultFormatter(resultsDir);
        formatter.writeResults(logger, Result.WARNING, "description", 12345L);
        Element elem = getDoc("TASK_foo_bar.xml");
        verifyResult(elem, "task", "Warning");
        assertEquals("Wrong parent", "foo", elem.getAttribute(XMLConstants.ATTR_PARENT));
        assertEquals("Wrong name", "bar", elem.getAttribute(XMLConstants.ATTR_TASK_NAME));
        assertEquals("Wrong fullname", "_foo/bar", elem.getAttribute(XMLConstants.ATTR_FULL_NAME));
        assertEquals("Wrong reference", "", elem.getAttribute(XMLConstants.ATTR_REFERENCE));
        assertEquals("Wrong ret val", "0", elem.getAttribute(XMLConstants.ATTR_RETVAL));
        Node child = elem.getElementsByTagName(XMLConstants.COMMAND).item(0).getFirstChild();
        assertEquals("Wrong command", "spam", child.getNodeValue());
        assertEquals("Wrong owner", "", elem.getAttribute(XMLConstants.ATTR_OWNER));
        
        tlCtrl.verify();
    }
    
    public void testService() throws Exception {
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getTaskType();
        tlCtrl.setReturnValue(Result.SERVICE);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar", 2);
        logger.getLineBuffer();
        tlCtrl.setReturnValue(new LineBuffer());
        logger.getId();
        tlCtrl.setReturnValue(1);
        logger.getParentName();
        tlCtrl.setReturnValue("foo");
        logger.getName();
        tlCtrl.setReturnValue("bar");
        logger.getReference();
        tlCtrl.setReturnValue(null);
        
        tlCtrl.replay();
        
        ResultFormatter formatter = new ResultFormatter(resultsDir);
        formatter.writeResults(logger, Result.FAILED, "description", 12345L);
        verifyResult(getDoc("SVCS_foo_bar.xml"), "service", "Failed");
        tlCtrl.verify();
    }
    
    public void testStart() throws Exception {
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getTaskType();
        tlCtrl.setReturnValue(Result.START);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar", 2);
        logger.getLineBuffer();
        tlCtrl.setReturnValue(new LineBuffer());
        logger.getId();
        tlCtrl.setReturnValue(1);
        logger.getParentName();
        tlCtrl.setReturnValue("foo");
        logger.getName();
        tlCtrl.setReturnValue("bar");
        logger.getReference();
        tlCtrl.setReturnValue(null);
        
        tlCtrl.replay();
        
        ResultFormatter formatter = new ResultFormatter(resultsDir);
        formatter.writeResults(logger, 12345, "description", 12345L);
        verifyResult(getDoc("SVCS_foo_bar.xml"), "start", "Invalid");
        tlCtrl.verify();
    }
    
    public void testVerify() throws Exception {
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getTaskType();
        tlCtrl.setReturnValue(Result.VERIFY);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar", 2);
        logger.getLineBuffer();
        tlCtrl.setReturnValue(new LineBuffer());
        logger.getId();
        tlCtrl.setReturnValue(1);
        logger.getParentName();
        tlCtrl.setReturnValue("foo");
        logger.getName();
        tlCtrl.setReturnValue("bar");
        logger.getReference();
        tlCtrl.setReturnValue(null);
        
        tlCtrl.replay();
        
        ResultFormatter formatter = new ResultFormatter(resultsDir);
        formatter.writeResults(logger, Result.SKIPPED, "description", 12345L);
        verifyResult(getDoc("SVCS_foo_bar.xml"), "verify", "Skipped");
        tlCtrl.verify();
    }
    
    public void testStop() throws Exception {
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getTaskType();
        tlCtrl.setReturnValue(Result.STOP);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar", 2);
        logger.getLineBuffer();
        tlCtrl.setReturnValue(new LineBuffer());
        logger.getId();
        tlCtrl.setReturnValue(1);
        logger.getParentName();
        tlCtrl.setReturnValue("foo");
        logger.getName();
        tlCtrl.setReturnValue("bar");
        logger.getReference();
        tlCtrl.setReturnValue(null);
        
        tlCtrl.replay();
        
        ResultFormatter formatter = new ResultFormatter(resultsDir);
        formatter.writeResults(logger, Result.PASSED, "description", 12345L);
        verifyResult(getDoc("SVCS_foo_bar.xml"), "stop", "Passed");
        tlCtrl.verify();
    }
    
    public void testTestgroup() throws Exception {
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getTaskType();
        tlCtrl.setReturnValue(Result.TESTGROUP);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar", 2);
        logger.getLineBuffer();
        tlCtrl.setReturnValue(new LineBuffer());
        logger.getId();
        tlCtrl.setReturnValue(1);
        logger.getParentName();
        tlCtrl.setReturnValue("foo");
        logger.getName();
        tlCtrl.setReturnValue("bar");
        logger.getReference();
        tlCtrl.setReturnValue(null);
        
        tlCtrl.replay();
        
        ResultFormatter formatter = new ResultFormatter(resultsDir);
        formatter.writeResults(logger, Result.PASSED, "description", 12345L);
        verifyResult(getDoc("GROUP_foo_bar.xml"), "group", "Passed");
        tlCtrl.verify();
    }
    
    public void testTestcase() throws Exception {
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getTaskType();
        tlCtrl.setReturnValue(Result.TESTCASE);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar", 2);
        logger.getLineBuffer();
        tlCtrl.setReturnValue(new LineBuffer());
        logger.getId();
        tlCtrl.setReturnValue(1);
        logger.getParentName();
        tlCtrl.setReturnValue("foo");
        logger.getName();
        tlCtrl.setReturnValue("bar");
        logger.getReference();
        tlCtrl.setReturnValue(null);
        logger.getOwner();
        tlCtrl.setReturnValue("John Doe");
        
        tlCtrl.replay();
        
        ResultFormatter formatter = new ResultFormatter(resultsDir);
        formatter.writeResults(logger, Result.PASSED, "description", 12345L);
        Element doc = getDoc("TEST_foo_bar.xml");
        verifyResult(doc, "test", "Passed");
        assertEquals("Wrong owner", "John Doe", doc.getAttribute(XMLConstants.ATTR_OWNER));
        tlCtrl.verify();
    }
    
    public void testXHarness() throws Exception {
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getTaskType();
        tlCtrl.setReturnValue(Result.XHARNESS);
        logger.getFullName();
        tlCtrl.setReturnValue("foo", 2);
        logger.getLineBuffer();
        tlCtrl.setReturnValue(new LineBuffer());
        logger.getId();
        tlCtrl.setReturnValue(1);
        logger.getName();
        tlCtrl.setReturnValue("bar");
        logger.getReference();
        tlCtrl.setReturnValue("spam");
        
        tlCtrl.replay();
        
        ResultFormatter formatter = new ResultFormatter(resultsDir);
        formatter.writeResults(logger, Result.PASSED, "description", 12345L);
        Element elem = getDoc("XHARNESS_foo.xml");
        verifyResult(elem, "xharness", "Passed");
        assertEquals("Wrong parent", "", elem.getAttribute(XMLConstants.ATTR_PARENT));
        assertEquals("Wrong name", "bar", elem.getAttribute(XMLConstants.ATTR_TASK_NAME));
        assertEquals("Wrong fullname", "foo", elem.getAttribute(XMLConstants.ATTR_FULL_NAME));
        assertEquals("Wrong reference", "spam", elem.getAttribute(XMLConstants.ATTR_REFERENCE));
        tlCtrl.verify();
    }
    
    public void testLink() throws Exception {
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getTaskType();
        tlCtrl.setReturnValue(Result.LINK);
        logger.getFullName();
        tlCtrl.setReturnValue("foo/bar", 2);
        logger.getLineBuffer();
        tlCtrl.setReturnValue(new LineBuffer());
        logger.getId();
        tlCtrl.setReturnValue(1);
        logger.getParentName();
        tlCtrl.setReturnValue("foo");
        logger.getName();
        tlCtrl.setReturnValue("bar");
        logger.getReference();
        tlCtrl.setReturnValue(null);
        
        tlCtrl.replay();
        
        ResultFormatter formatter = new ResultFormatter(resultsDir);
        formatter.writeResults(logger, Result.PASSED, "description", 12345L);
        Element doc = getDoc("LNK_foo_bar.xml");
        verifyResult(doc, "link", "Passed");
        tlCtrl.verify();
    }
    
    public void testInvalidType() throws Exception {
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getTaskType();
        tlCtrl.setReturnValue(55555);
        
        tlCtrl.replay();
        
        ResultFormatter formatter = new ResultFormatter(resultsDir);
        try {
            formatter.writeResults(logger, Result.PASSED, "description", 12345L);
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Invalid Task Type: 55555", be.getMessage());
        }

        tlCtrl.verify();
    }
    
    public void testInvalidResultsdir() throws Exception {
        LineBuffer buffer = new LineBuffer();
        buffer.logLine(1, "Beam me up, Scottie!");
        buffer.logLine(2, "All good things must come to an end.");
        buffer.logLine(2, "Must you be so linear, Jean-Luc?");
        
        MockControl tlCtrl = MockClassControl.createControl(TaskLogger.class);
        TaskLogger logger = (TaskLogger)tlCtrl.getMock();
        logger.getTaskType();
        tlCtrl.setReturnValue(Result.OTHER_TASK);
        logger.getFullName();
        tlCtrl.setReturnValue(null, 2);
        logger.getLineBuffer();
        tlCtrl.setReturnValue(buffer);
        logger.getId();
        tlCtrl.setReturnValue(1);
        logger.getParentName();
        tlCtrl.setReturnValue("foo");
        logger.getName();
        tlCtrl.setReturnValue("bar");
        logger.getReference();
        tlCtrl.setReturnValue("spam");
        
        tlCtrl.replay();
        
        ResultFormatter formatter = new ResultFormatter(new File("/non/existant/321.123/"));
        try {
            formatter.writeResults(logger, Result.PASSED, "description", 12345L);
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertTrue("Wrong message: " + be.getMessage(), 
                       be.getMessage().startsWith("Unable to write results file TASK_.xml: "));
        }

        tlCtrl.verify();
    }
    
    private Element getDoc(String fileName) throws Exception {
        File f = new File(resultsDir, fileName);
        assertTrue("File " + f.getAbsolutePath() + " does not exist", f.exists());
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse("file:///" + f.getAbsolutePath());
        return doc.getDocumentElement();
    }
    
    private void verifyResult(Element elem, String elemName, String result) throws Exception {
        assertEquals("Wrong tag name", elemName, elem.getNodeName());
        assertEquals("Wrong result", result, elem.getAttribute(XMLConstants.ATTR_RESULT));
    }
}
