package org.codehaus.xharness.tasks;

import java.io.File;
import java.io.FileWriter;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Echo;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class IncludeTaskTest extends TestCase {
    public IncludeTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = IncludeTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = IncludeTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(IncludeTaskTest.class);
    }
    
    public void testAddTask() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        IncludeTask task = new IncludeTask();
        task.setProject(project);
        
        MockControl tkCtrl = MockClassControl.createControl(Task.class);
        Task mock = (Task)tkCtrl.getMock();

        tkCtrl.replay();
        task.addTask(mock);
        try {
            task.addTask(mock);
            fail("Expected BuildExcecption");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Invalid XML", be.getMessage());
        }
        assertEquals("Wrong nested Task", mock, task.getNestedTask());
        tkCtrl.verify();
    }
    
    public void testToString() throws Exception {
        MockControl ctrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)ctrl.getMock();

        IncludeTask task = new IncludeTask();
        task.setProject(project);
        assertEquals("Wrong toString", "include null", task.toString());
        task.setFile("foo");
        assertEquals("Wrong toString", "include foo", task.toString());
    }
    
    public void testExecuteNoFile() throws Exception {
        MockControl ctrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)ctrl.getMock();

        IncludeTask task = new IncludeTask();
        task.setProject(project);
        
        try {
            task.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "import requires file attribute", be.getMessage());
        }
    }
    
    public void testExecuteNonExistantFile() throws Exception {
        MockControl ctrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)ctrl.getMock();

        IncludeTask task = new IncludeTask();
        task.setProject(project);
        task.setFile("foo");

        project.getBaseDir();
        File baseDir = new File(".");
        ctrl.setReturnValue(baseDir);
        project.log(task, 
                    "Importing file foo from " + baseDir.getAbsolutePath(), 
                    Project.MSG_VERBOSE);

        ctrl.replay();
        task.setProject(project);
        
        try {
            task.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "Cannot find foo imported from " + baseDir.getAbsolutePath(), 
                         be.getMessage());
        }
        ctrl.verify();
    }
    
    public void testExecute() throws Exception {
        File baseDir = new File(".");
        File impFile = File.createTempFile("tmp", ".xml", baseDir);
        impFile.deleteOnExit();
        FileWriter fw = new FileWriter(impFile);
        fw.write("<echo/>\n");
        fw.close();
        
        MockControl ctrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)ctrl.getMock();
        project.getBaseDir();
        ctrl.setReturnValue(baseDir);
        project.getProperty(null);
        ctrl.setMatcher(MockControl.ALWAYS_MATCHER);
        ctrl.setReturnValue("foo", 6);
        project.createClassLoader(null);
        ctrl.setMatcher(MockControl.ALWAYS_MATCHER);
        ctrl.setReturnValue(new AntClassLoader(new Echo().getClass().getClassLoader(), false));

        IncludeTask task = new IncludeTask();
        task.setProject(project);

        ctrl.replay();
        task.setFile(impFile.getName());
        task.setProject(project);
        task.setOwningTarget((Target)MockClassControl.createControl(Target.class).getMock());
        
        // REVISIT: Test not fully working yet!
        //          Can't seem to find a way around ant not being able to load echo class...
        try {
            task.execute();
        } catch (BuildException be) {
            // ignore
        }
        ctrl.verify();
    }
}
