package org.codehaus.xharness.tasks;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ServiceGroupTaskTest extends TestCase {
    public ServiceGroupTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ServiceGroupTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ServiceGroupTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(ServiceGroupTaskTest.class);
    }
    
    public void testToString() throws Exception {
        ServiceGroupTask task = new ServiceGroupTask();
        assertEquals("Wrong toString", "service null", task.toString());
        task.setTaskName("foo");
        assertEquals("Wrong toString", "service foo", task.toString());
    }
    
    public void testFailonerror() throws Exception {
        ServiceGroupTask task = new ServiceGroupTask();
        assertTrue("Service group should fail on error", task.failOnError());
    }
}
