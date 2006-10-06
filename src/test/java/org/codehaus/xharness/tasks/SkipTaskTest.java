package org.codehaus.xharness.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;

import org.codehaus.xharness.exceptions.TestSkippedException;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SkipTaskTest extends TestCase {
    public SkipTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = SkipTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = SkipTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(SkipTaskTest.class);
    }
    
    public void testDescription() throws Exception {
        SkipTask skip = new SkipTask();
        skip.setDescription("foo");
        assertEquals("Wrong description", "foo", skip.getDescription());
    }
    
    public void testExecuteDoSkip() throws Exception {
        SkipTask skip = new SkipTask();
        
        MockControl ctrl = MockClassControl.createControl(Condition.class);
        Condition condition = (Condition)ctrl.getMock();
        condition.eval();
        ctrl.setReturnValue(true);

        ctrl.replay();
        skip.add(condition);
        try {
            skip.execute();
            fail("Expected TestSkippedException");
        } catch (TestSkippedException tse) {
            assertEquals("Wrong message", null, tse.getMessage());
        }
        ctrl.verify();
    }

    public void testExecuteNoSkip() throws Exception {
        SkipTask skip = new SkipTask();
        
        MockControl ctrl = MockClassControl.createControl(Condition.class);
        Condition condition = (Condition)ctrl.getMock();
        condition.eval();
        ctrl.setReturnValue(false);

        ctrl.replay();
        skip.add(condition);
        skip.execute();
        ctrl.verify();
    }

    public void testExecuteNoCondition() throws Exception {
        SkipTask skip = new SkipTask();
        
        try {
            skip.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "You must nest a condition into <skip>", be.getMessage());
        }
    }
    
    public void testExecuteMultipleConditions() throws Exception {
        SkipTask skip = new SkipTask();
        
        MockControl ctrl = MockClassControl.createControl(Condition.class);
        Condition condition = (Condition)ctrl.getMock();

        ctrl.replay();
        skip.add(condition);
        skip.add(condition);
        try {
            skip.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "You must not nest more than one condition into <skip>", 
                         be.getMessage());
        }
        ctrl.verify();
    }
}
