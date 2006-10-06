package org.codehaus.xharness.tasks;

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
        suite.addTest(AssertTaskTest.suite());
        suite.addTest(IncludeTaskTest.suite());
        suite.addTest(KillTaskTest.suite());
        suite.addTest(XhReportTaskTest.suite());
        suite.addTest(ServiceDefTest.suite());
        suite.addTest(ServiceInstanceTest.suite());
        suite.addTest(ServiceGroupTaskTest.suite());
        suite.addTest(ServiceVerifyTaskTest.suite());
        suite.addTest(SkipTaskTest.suite());
        suite.addTest(TestCaseTaskTest.suite());
        suite.addTest(TestGroupTaskTest.suite());
        suite.addTest(WhichTaskTest.suite());
        suite.addTest(XhExecTaskTest.suite());
        suite.addTest(XhExecBgTaskTest.suite());
        suite.addTest(XhJavaTaskTest.suite());
        suite.addTest(XhJavaBgTaskTest.suite());
        suite.addTest(XharnessTaskTest.suite());

        return suite;
    }

}
