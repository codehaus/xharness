package org.codehaus.xharness.tasks;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.codehaus.xharness.exceptions.AssertionWarningException;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ServiceDefTest extends TestCase {
    public ServiceDefTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ServiceDefTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ServiceDefTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(ServiceDefTest.class);
    }
    
    public void testName() throws Exception {
        ServiceDef service = new ServiceDef();
        service.setName("foo");
        assertEquals("Wrong service name", "foo", service.getName());
    }

    public void testCreateStart() throws Exception {
        ServiceDef service = new ServiceDef();
        ServiceGroupTask start = service.createStart();
        assertEquals("Wrong start name", "start", start.getName());
        try {
            service.createStart();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Only one start allowed", be.getMessage());
        }
    }

    public void testCreateVerify() throws Exception {
        ServiceDef service = new ServiceDef();
        assertEquals("Wrong verify task", null, service.getVerifyTask());
        ServiceVerifyTask verify = service.createVerify();
        assertEquals("Wrong start name", "verify", verify.getName());
        try {
            service.createVerify();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Only one verify allowed", be.getMessage());
        }
        assertEquals("Wrong verify task", verify, service.getVerifyTask());
    }
    
    public void testCreateStop() throws Exception {
        ServiceDef service = new ServiceDef();
        ServiceGroupTask stop = service.createStop();
        assertEquals("Wrong start name", "stop", stop.getName());
        try {
            service.createStop();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Only one stop allowed", be.getMessage());
        }
    }
    
    public void testStart() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        ServiceGroupTask start = service.createStart();
        start.setProject(project);
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)taskCtrl.getMock();
        start.addTask(task);
        task.getProject();
        taskCtrl.setReturnValue(project, 2);
        task.maybeConfigure();
        task.execute();

        projectCtrl.replay();
        taskCtrl.replay();
        service.start();
        service.start();
        assertTrue("Service stopped", !service.wasStopped());
        projectCtrl.verify();
        taskCtrl.verify();
    }
    
    public void testStartWarning() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        ServiceGroupTask start = service.createStart();
        start.setProject(project);
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)taskCtrl.getMock();
        start.addTask(task);
        task.getProject();
        taskCtrl.setReturnValue(project, 2);
        task.maybeConfigure();
        task.execute();
        taskCtrl.setThrowable(new AssertionWarningException());
        task.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        task.getTaskName();
        taskCtrl.setReturnValue("foo");

        projectCtrl.replay();
        taskCtrl.replay();
        try {
            service.start();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", 
                         "Assertion failed during startup of Service.", 
                         awe.getMessage());
        }
        service.start();
        assertTrue("Service stopped", !service.wasStopped());
        projectCtrl.verify();
        taskCtrl.verify();
    }
    
    public void testStartError() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        ServiceGroupTask start = service.createStart();
        start.setProject(project);
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)taskCtrl.getMock();
        start.addTask(task);
        task.getProject();
        taskCtrl.setReturnValue(project, 2);
        task.maybeConfigure();
        task.execute();
        taskCtrl.setThrowable(new BuildException());
        task.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        task.getTaskName();
        taskCtrl.setReturnValue("foo");

        projectCtrl.replay();
        taskCtrl.replay();
        try {
            service.start();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Startup of Service failed.", be.getMessage());
        }
        service.start();
        assertTrue("Service stopped", !service.wasStopped());
        projectCtrl.verify();
        taskCtrl.verify();
    }
    
    public void testStop() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        ServiceGroupTask start = service.createStart();
        start.setProject(project);
        ServiceGroupTask stop = service.createStop();
        stop.setProject(project);
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)taskCtrl.getMock();
        stop.addTask(task);
        task.getProject();
        taskCtrl.setReturnValue(project, 2);
        task.maybeConfigure();
        task.execute();

        projectCtrl.replay();
        taskCtrl.replay();
        service.start();
        assertTrue("Service stopped", !service.wasStopped());
        service.stop();
        assertTrue("Service not stopped", service.wasStopped());
        projectCtrl.verify();
        taskCtrl.verify();
    }
    
    public void testStopUnstarted() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        ServiceGroupTask stop = service.createStop();
        stop.setProject(project);
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)taskCtrl.getMock();
        stop.addTask(task);

        projectCtrl.replay();
        taskCtrl.replay();
        assertTrue("Service stopped", !service.wasStopped());
        service.stop();
        assertTrue("Service not stopped", service.wasStopped());
        projectCtrl.verify();
        taskCtrl.verify();
    }
    
    public void testStopNoStoptask() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        ServiceGroupTask start = service.createStart();
        start.setProject(project);

        projectCtrl.replay();
        assertTrue("Service stopped", !service.wasStopped());
        service.stop();
        assertTrue("Service not stopped", service.wasStopped());
        projectCtrl.verify();
    }
    
    public void testStopWarning() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        ServiceGroupTask start = service.createStart();
        start.setProject(project);
        ServiceGroupTask stop = service.createStop();
        stop.setProject(project);
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)taskCtrl.getMock();
        stop.addTask(task);
        task.getProject();
        taskCtrl.setReturnValue(project, 2);
        task.maybeConfigure();
        task.execute();
        taskCtrl.setThrowable(new AssertionWarningException());
        task.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        task.getTaskName();
        taskCtrl.setReturnValue("foo");

        projectCtrl.replay();
        taskCtrl.replay();
        service.start();
        assertTrue("Service stopped", !service.wasStopped());
        try {
            service.stop();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", 
                         "Assertion failed during shutdown of Service.", 
                         awe.getMessage());
        }
        assertTrue("Service not stopped", service.wasStopped());
        projectCtrl.verify();
        taskCtrl.verify();
    }
    
    public void testStopError() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        ServiceGroupTask start = service.createStart();
        start.setProject(project);
        ServiceGroupTask stop = service.createStop();
        stop.setProject(project);
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)taskCtrl.getMock();
        stop.addTask(task);
        task.getProject();
        taskCtrl.setReturnValue(project, 2);
        task.maybeConfigure();
        task.execute();
        taskCtrl.setThrowable(new BuildException());
        task.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        task.getTaskName();
        taskCtrl.setReturnValue("foo");

        projectCtrl.replay();
        taskCtrl.replay();
        service.start();
        assertTrue("Service stopped", !service.wasStopped());
        try {
            service.stop();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Shutdown of Service failed.", be.getMessage());
        }
        assertTrue("Service not stopped", service.wasStopped());
        projectCtrl.verify();
        taskCtrl.verify();
    }
    
    public void testStopWarningInStart() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        ServiceGroupTask start = service.createStart();
        start.setProject(project);
        ServiceGroupTask stop = service.createStop();
        stop.setProject(project);
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)taskCtrl.getMock();
        start.addTask(task);
        task.getProject();
        taskCtrl.setReturnValue(project, 2);
        task.maybeConfigure();
        task.execute();
        taskCtrl.setThrowable(new AssertionWarningException());
        task.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        task.getTaskName();
        taskCtrl.setReturnValue("foo");

        projectCtrl.replay();
        taskCtrl.replay();
        try {
            service.start();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", 
                         "Assertion failed during startup of Service.", 
                         awe.getMessage());
        }
        assertTrue("Service stopped", !service.wasStopped());
        try {
            service.stop();
            fail("Expected AssertionWarningException");
        } catch (AssertionWarningException awe) {
            assertEquals("Wrong message", 
                         "Assertion failed during startup of Service.", 
                         awe.getMessage());
        }
        assertTrue("Service not stopped", service.wasStopped());
        projectCtrl.verify();
        taskCtrl.verify();
    }
    
    public void testStopErrorInStart() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        ServiceGroupTask start = service.createStart();
        start.setProject(project);
        ServiceGroupTask stop = service.createStop();
        stop.setProject(project);
        MockControl taskCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)taskCtrl.getMock();
        start.addTask(task);
        task.getProject();
        taskCtrl.setReturnValue(project, 2);
        task.maybeConfigure();
        task.execute();
        taskCtrl.setThrowable(new BuildException());
        task.getLocation();
        taskCtrl.setReturnValue(new Location(""));
        task.getTaskName();
        taskCtrl.setReturnValue("foo");

        projectCtrl.replay();
        taskCtrl.replay();
        try {
            service.start();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Startup of Service failed.", be.getMessage());
        }
        assertTrue("Service stopped", !service.wasStopped());
        try {
            service.stop();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Startup of Service failed.", be.getMessage());
        }
        assertTrue("Service not stopped", service.wasStopped());
        projectCtrl.verify();
        taskCtrl.verify();
    }
    
    public void testExecute() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        projectCtrl.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        service.setName("foo");
        service.createStart();
        service.createVerify();
        projectCtrl.replay();
        service.execute();
        projectCtrl.verify();
    }
    
    public void testExecuteNoName() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        try {
            service.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Name not specified", be.getMessage());
        }
    }
    
    public void testExecuteNoStart() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        service.setName("foo");
        try {
            service.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Missing start element", be.getMessage());
        }
    }
    
    public void testExecuteNoVerify() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        projectCtrl.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
        Project project = (Project)projectCtrl.getMock();

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        service.setName("foo");
        service.createStart();
        projectCtrl.replay();
        service.execute();
        projectCtrl.verify();
    }
    
    
    public void testSimilar() throws Exception {
        ServiceDef service = new ServiceDef();
        assertTrue("Services should be similar", service.similar(service));
        assertTrue("Services shouldn't be similar", !service.similar(new ServiceDef()));
        assertTrue("Services shouldn't be similar", !service.similar(new Object()));
    }

    public void testMyAntTypeDefinition() throws Exception {
        MockControl projectCtrl = MockClassControl.createNiceControl(Project.class);
        projectCtrl.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
        Project project = (Project)projectCtrl.getMock();
        MyComponentHelper ch = new MyComponentHelper();
        ch.setProject(project);
        project.getReference(null);
        projectCtrl.setReturnValue(ch);

        ServiceDef service = new ServiceDef();
        service.setProject(project);
        service.setName("foo");
        service.createStart();
        
        projectCtrl.replay();
        service.execute();
        
        assertNotNull("No AntTypeDefinition", ch.getDefinition());
        assertEquals("Invalid AntTypeDefinition class", 
                     "org.codehaus.xharness.tasks.ServiceDef$MyAntTypeDefinition", 
                     ch.getDefinition().getClass().getName());
        Object instance = ch.getDefinition().create(project);
        assertTrue("Wrong instance", instance instanceof ServiceInstance);
        assertEquals("Wrong ServiceDef", service, ((ServiceInstance)instance).getServiceDef());
        
        AntTypeDefinition otherDef = new AntTypeDefinition();
        assertTrue("TypeDefinition should be same", 
                   ch.getDefinition().sameDefinition(ch.getDefinition(), project));
        assertTrue("TypeDefinition shouldn't be same", 
                   !ch.getDefinition().sameDefinition(otherDef, project));
        assertTrue("TypeDefinition should be similar", 
                   ch.getDefinition().similarDefinition(ch.getDefinition(), project));
        assertTrue("TypeDefinition shouldn't be similar", 
                   !ch.getDefinition().similarDefinition(otherDef, project));
        projectCtrl.verify();
    }
    
    private class MyComponentHelper extends ComponentHelper {
        private AntTypeDefinition atd;
        
        public MyComponentHelper() {
            super();
        }
        
        public void addDataTypeDefinition(AntTypeDefinition def) {
            super.addDataTypeDefinition(def);
            atd = def;
        }
        
        public AntTypeDefinition getDefinition() {
            return atd;
        }
    }
}
