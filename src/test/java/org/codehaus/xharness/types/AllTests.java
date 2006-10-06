package org.codehaus.xharness.types;

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
        suite.addTest(EnvironmentVariableTest.suite());
        suite.addTest(EnvSetTest.suite());
        suite.addTest(FileContainsTest.suite());
        suite.addTest(OutputContainsTest.suite());
        suite.addTest(OutputIsTest.suite());
        suite.addTest(OutputRegexTest.suite());
        suite.addTest(OutputSizeTest.suite());

        return suite;
    }

}
