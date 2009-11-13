package org.codehaus.xharness.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;

import org.codehaus.xharness.exceptions.AssertionWarningException;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AssertTaskTest extends TestCase {
    public AssertTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = AssertTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = AssertTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(AssertTaskTest.class);
    }
    
    public void testExecutePass() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        AssertTask task = new AssertTask();
        task.setProject(project);
        
        MockControl conCtrl = MockClassControl.createControl(Condition.class);
        Condition condition = (Condition)conCtrl.getMock();
        condition.eval();
        conCtrl.setReturnValue(true);

        prCtrl.replay();
        conCtrl.replay();
        task.add(condition);
        task.execute();
        prCtrl.verify();
        conCtrl.verify();
    }

    public void testExecuteFail() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        AssertTask task = new AssertTask();
        task.setProject(project);
        
        MockControl conCtrl = MockClassControl.createControl(Condition.class);
        Condition condition = (Condition)conCtrl.getMock();
        condition.eval();
        conCtrl.setReturnValue(false);

        prCtrl.replay();
        conCtrl.replay();
        task.add(condition);
        try {
            task.execute();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", "Assertion failed", awe.getMessage());
        }
        prCtrl.verify();
        conCtrl.verify();
    }

    public void testExecuteNoCondition() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        AssertTask task = new AssertTask();
        task.setProject(project);
        
        try {
            task.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "You must nest a condition into <assert>", 
                         be.getMessage());
        }
    }
    
    public void testExecuteMultipleConditions() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        AssertTask task = new AssertTask();
        task.setProject(project);
        
        MockControl conCtrl = MockClassControl.createControl(Condition.class);
        Condition condition = (Condition)conCtrl.getMock();

        prCtrl.replay();
        conCtrl.replay();
        task.add(condition);
        task.add(condition);
        try {
            task.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "You must not nest more than one condition into <assert>", 
                         be.getMessage());
        }
        prCtrl.verify();
        conCtrl.verify();
    }
    
    public void testExecuteTimeout() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        AssertTask task = new AssertTask();
        task.setProject(project);
        
        MockControl conCtrl = MockClassControl.createControl(Condition.class);
        Condition condition = (Condition)conCtrl.getMock();
        condition.eval();
        conCtrl.setReturnValue(false, 2);

        prCtrl.replay();
        conCtrl.replay();
        task.add(condition);
        task.setTimeout(1);
        try {
            task.execute();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", "Assertion failed", awe.getMessage());
        }
        prCtrl.verify();
        conCtrl.verify();
    }
    
    public void testExecuteTimeoutNegative() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        AssertTask task = new AssertTask();
        task.setProject(project);
        
        MockControl conCtrl = MockClassControl.createControl(Condition.class);
        Condition condition = (Condition)conCtrl.getMock();
        condition.eval();
        conCtrl.setReturnValue(true);

        prCtrl.replay();
        conCtrl.replay();
        task.add(condition);
        task.setTimeout(-1);
        task.execute();
        prCtrl.verify();
        conCtrl.verify();
    }

    public void testExecuteMessage() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        AssertTask task = new AssertTask();
        task.setProject(project);
        
        MockControl conCtrl = MockClassControl.createControl(Condition.class);
        Condition condition = (Condition)conCtrl.getMock();
        condition.eval();
        conCtrl.setReturnValue(false);

        prCtrl.replay();
        conCtrl.replay();
        task.add(condition);
        task.setMessage("foo");
        try {
            task.execute();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", "foo", awe.getMessage());
        }
        prCtrl.verify();
        conCtrl.verify();
    }
    
    public void testExecuteErrorOnFail() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        AssertTask task = new AssertTask();
        task.setProject(project);
        
        MockControl conCtrl = MockClassControl.createControl(Condition.class);
        Condition condition = (Condition)conCtrl.getMock();
        condition.eval();
        conCtrl.setReturnValue(false);

        prCtrl.replay();
        conCtrl.replay();
        
        task.add(condition);
        task.setErroronfail(true);
        try {
            task.execute();
            fail("Expected BuildException");
        } catch (AssertionWarningException awe) {
            fail("Expected BuildException instead of AssertionWarningException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Assertion failed", be.getMessage());
        }
        
        prCtrl.verify();
        conCtrl.verify();
    }
    
    public void testNestedTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl taskCtrl = MockClassControl.createNiceControl(Task.class);
        Task nestedTask = (Task)taskCtrl.getMock();
        nestedTask.getProject();
        taskCtrl.setReturnValue(project, 4);
        nestedTask.getLocation();
        taskCtrl.setReturnValue(new Location("123"));
        nestedTask.execute();
        nestedTask.execute();
        taskCtrl.setThrowable(new BuildException("foo"));

        AssertTask task = new AssertTask();
        task.setProject(project);
        
        MockControl conCtrl = MockClassControl.createControl(Condition.class);
        Condition condition = (Condition)conCtrl.getMock();
        condition.eval();
        conCtrl.setReturnValue(true);

        prCtrl.replay();
        taskCtrl.replay();
        conCtrl.replay();
        
        task.add(condition);
        task.add(nestedTask);
        try {
            task.add(nestedTask);
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Only one nested task is suppoted.", be.getMessage());
        }
        task.add((Task)null);
        task.execute();
        try {
            task.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("123: foo", be.toString());
        }
        
        prCtrl.verify();
        taskCtrl.verify();
        conCtrl.verify();
    }
}
