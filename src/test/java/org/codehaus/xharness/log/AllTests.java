package org.codehaus.xharness.log;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase {
    public AllTests(String name) throws Exception {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.main(new String[] {AllTests.class.getName()});
    }

    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite();

        // Add package tests to list
        //
        suite.addTest(LineBufferTest.suite());
        suite.addTest(LinkLoggerTest.suite());
        suite.addTest(LoggingInputStreamTest.suite());
        suite.addTest(LogOutputStreamTest.suite());
        suite.addTest(LoggingRedirectorTest.suite());
        suite.addTest(LoggingStreamHandlerTest.suite());
        suite.addTest(ProcessLoggerTest.suite());
        suite.addTest(ResultFormatterTest.suite());
        suite.addTest(ServiceLoggerTest.suite());
        suite.addTest(SvcsStartLoggerTest.suite());
        suite.addTest(TaskLoggerTest.suite());
        suite.addTest(TaskRegistryTest.suite());
        suite.addTest(TestLoggerTest.suite());

        return suite;
    }

}
