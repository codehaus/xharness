package org.codehaus.xharness;

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
        suite.addTest(org.codehaus.xharness.log.AllTests.suite());
        suite.addTest(org.codehaus.xharness.tasks.AllTests.suite());
        suite.addTest(org.codehaus.xharness.types.AllTests.suite());

        return suite;
    }

}
