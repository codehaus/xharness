package org.codehaus.xharness.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ServiceVerifyTaskTest extends TestCase {
    public ServiceVerifyTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ServiceVerifyTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ServiceVerifyTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(ServiceVerifyTaskTest.class);
    }
    
    public void testExecute() throws Exception {
        MockControl prjCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prjCtrl.getMock();

        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task mockTask = (Task)taskCtrl.getMock();
        mockTask.getProject();
        taskCtrl.setReturnValue(project, 2);
        mockTask.maybeConfigure();
        mockTask.execute();
        
        ServiceVerifyTask verify = new ServiceVerifyTask();
        verify.setProject(project);
        verify.addTask(mockTask);
        
        prjCtrl.replay();
        taskCtrl.replay();
        verify.execute();
        prjCtrl.verify();
        taskCtrl.verify();
    }
    
    public void testExecuteDummy() throws Exception {
        MockControl prjCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prjCtrl.getMock();

        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task mockTask = (Task)taskCtrl.getMock();
        
        ServiceVerifyTask verify = new ServiceVerifyTask();
        verify.setProject(project);
        verify.addTask(mockTask);
        verify.setTaskName(ServiceVerifyTask.DUMMY);
        
        prjCtrl.replay();
        taskCtrl.replay();
        verify.execute();
        prjCtrl.verify();
        taskCtrl.verify();
    }
    
    public void testExecuteException() throws Exception {
        MockControl prjCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prjCtrl.getMock();

        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task mockTask = (Task)taskCtrl.getMock();
        
        ServiceVerifyTask verify = new ServiceVerifyTask();
        verify.setProject(project);
        verify.addTask(mockTask);
        verify.setException(new BuildException("foo"));
        
        prjCtrl.replay();
        taskCtrl.replay();
        try {
            verify.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "foo", be.getMessage());
        }
        prjCtrl.verify();
        taskCtrl.verify();
    }
}
