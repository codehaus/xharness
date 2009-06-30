package org.codehaus.xharness.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.codehaus.xharness.testutil.TempDir;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class XharnessTaskTest extends TestCase {
    private File resultsDir;
    
    public XharnessTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = XharnessTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = XharnessTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(XharnessTaskTest.class);
    }
    
    public void setUp() throws Exception {
        resultsDir = TempDir.createTempDir();
    }
    
    public void tearDown() throws Exception {
        TempDir.removeTempFile(resultsDir);
    }
    
    public void testAttributes() throws Exception {
        XharnessTask xhTask = new XharnessTask();
        
        assertEquals("Invalid Name", null, xhTask.getName());
        assertEquals("Invalid toString", "xharness", xhTask.toString());
        xhTask.setName("Foo");
        assertEquals("Invalid Name", "Foo", xhTask.getName());
        assertEquals("Invalid toString", "xharness Foo", xhTask.toString());
        assertEquals("Invalid failOnError", false, xhTask.failOnError());
        
        assertEquals("Invalid results dir", null, xhTask.getResultsdir());
        File f = new File("");
        xhTask.setResultsdir(f);
        assertEquals("Invalid results dir", f, xhTask.getResultsdir());
        
        assertEquals("Invalid base dir", null, xhTask.getBasedir());
        File f1 = new File("");
        xhTask.setBasedir(f1);
        assertEquals("Invalid base dir", f, xhTask.getBasedir());
        
        assertEquals("Invalid pattern", null, xhTask.getPattern());
        xhTask.setPattern("Hello!");
        assertEquals("Invalid pattern", "Hello!", xhTask.getPattern());
        
        assertEquals("Invalid error property", null, xhTask.getErrorProperty());
        xhTask.setErrorProperty("error.property");
        assertEquals("Invalid error property", "error.property", xhTask.getErrorProperty());
        
        assertNotNull("Children null", xhTask.getNestedTasks());
        assertEquals("Invalid number of children", 0, xhTask.getNestedTasks().size());
        
        MockControl ctrl = MockClassControl.createControl(Task.class);
        Task mock1 = (Task)ctrl.getMock();
        Task mock2 = (Task)ctrl.getMock();
        
        ctrl.replay();
        xhTask.addTask(mock1);
        xhTask.addTask(null);
        xhTask.addTask(mock2);
        assertEquals("Invalid number of children", 2, xhTask.getNestedTasks().size());
        assertEquals("Invalid Child", mock1, xhTask.getNestedTasks().get(0));
        assertEquals("Invalid Child", mock2, xhTask.getNestedTasks().get(1));
        ctrl.verify();
    }
    
    public void testExecuteNoResultsDir() throws Exception {
        XharnessTask xhTask = new XharnessTask();
        try {
            xhTask.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "Required attribute \"resultsdir\" not set!", 
                         be.getMessage());
        }
        
    }
    
    public void testExecute() throws Exception {
        MockControl ctrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)ctrl.getMock();
        ctrl.replay();
        
        XharnessTask xhTask = new XharnessTask();
        xhTask.setProject(project);
        xhTask.setResultsdir(resultsDir);
        
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        
        Task mockTask = (Task)taskCtrl.getMock();
        mockTask.getProject();
        taskCtrl.setReturnValue(project, 2);
        mockTask.maybeConfigure();
        mockTask.execute();
        
        taskCtrl.replay();
        xhTask.addTask(mockTask);
        xhTask.execute();
        taskCtrl.verify();
        ctrl.verify();
    }
    
    public void testExecuteFailing() throws Exception {
        MockControl ctrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)ctrl.getMock();
        ctrl.replay();
        
        XharnessTask xhTask = new XharnessTask();
        xhTask.setProject(project);
        xhTask.setResultsdir(resultsDir);
        
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        
        Task mockTask = (Task)taskCtrl.getMock();
        mockTask.getProject();
        taskCtrl.setReturnValue(project, 2);
        mockTask.getTaskName();
        taskCtrl.setReturnValue("mocktask");
        mockTask.maybeConfigure();
        mockTask.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        mockTask.execute();
        taskCtrl.setThrowable(new BuildException());
        
        taskCtrl.replay();
        xhTask.addTask(mockTask);
        xhTask.execute();
        taskCtrl.verify();
        ctrl.verify();
    }
}
