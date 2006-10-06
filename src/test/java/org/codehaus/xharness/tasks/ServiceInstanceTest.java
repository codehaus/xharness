package org.codehaus.xharness.tasks;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;

import org.codehaus.xharness.exceptions.ServiceVerifyException;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ServiceInstanceTest extends TestCase {
    public ServiceInstanceTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ServiceInstanceTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = ServiceInstanceTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(ServiceInstanceTest.class);
    }
    
    public void testReference() throws Exception {
        ServiceInstance instance = new ServiceInstance();
        instance.setReference("foo");
        assertEquals("Wrong reference", "foo", instance.getReference());
    }

    public void testServiceDef() throws Exception {
        ServiceInstance instance = new ServiceInstance();
        ServiceDef service = new ServiceDef();
        instance.setServiceDef(service);
        assertEquals("Wrong Service", service, instance.getServiceDef());
    }
    
    public void testSetAction() throws Exception {
        ServiceInstance instance = new ServiceInstance();
        instance.setAction("start");
        instance.setAction("verify");
        instance.setAction("stop");
        instance.setAction("Start");
        instance.setAction("veRIFy");
        instance.setAction("STOP");
        try {
            instance.setAction("foo");
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "foo is not a legal value for this attribute", 
                         be.toString());
            
        }
        try {
            instance.setAction("starts");
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "starts is not a legal value for this attribute", 
                         be.toString());
            
        }
    }
    
    public void testExecuteNoDef() throws Exception {
        ServiceInstance instance = new ServiceInstance();
        try {
            instance.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "No Service Definition found!", be.getMessage());
        }
    }
    
    public void testExecute() throws Exception {
        MockControl prjCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prjCtrl.getMock();

        MockControl vfyCtrl = MockClassControl.createNiceControl(ServiceVerifyTask.class);
        ServiceVerifyTask verify = (ServiceVerifyTask)vfyCtrl.getMock();
        verify.getProject();
        vfyCtrl.setReturnValue(project, 2);
        verify.execute();
        vfyCtrl.setVoidCallable();

        MockControl svcCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef service = (ServiceDef)svcCtrl.getMock();
        service.getVerifyTask();
        svcCtrl.setReturnValue(verify);
        service.start();
        svcCtrl.setVoidCallable();

        ServiceInstance instance = new ServiceInstance();
        instance.setProject(project);
        instance.setServiceDef(service);
        
        prjCtrl.replay();
        vfyCtrl.replay();
        svcCtrl.replay();
        instance.execute();
        prjCtrl.verify();
        vfyCtrl.verify();
        svcCtrl.verify();
    }
    
    public void testExecuteNoDefWithGoodReference() throws Exception {
        MockControl chCtrl = MockClassControl.createNiceControl(ComponentHelper.class);
        ComponentHelper cHelper = (ComponentHelper)chCtrl.getMock();
        
        MockControl prjCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prjCtrl.getMock();
        project.getReference("ant.ComponentHelper");
        prjCtrl.setReturnValue(cHelper);

        MockControl vfyCtrl = MockClassControl.createNiceControl(ServiceVerifyTask.class);
        ServiceVerifyTask verify = (ServiceVerifyTask)vfyCtrl.getMock();
        verify.getProject();
        vfyCtrl.setReturnValue(project, 2);
        verify.execute();
        vfyCtrl.setVoidCallable();

        MockControl svcCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef service = (ServiceDef)svcCtrl.getMock();
        service.getVerifyTask();
        svcCtrl.setReturnValue(verify);
        service.start();
        svcCtrl.setVoidCallable();

        ServiceInstance instance = new ServiceInstance();
        instance.setProject(project);
        instance.setReference("foo");
        
        MockControl atdCtrl = MockClassControl.createControl(AntTypeDefinition.class);
        AntTypeDefinition typeDef = (AntTypeDefinition)atdCtrl.getMock();
        typeDef.create(project);
        ServiceInstance helperInstance = new ServiceInstance();
        helperInstance.setServiceDef(service);
        atdCtrl.setReturnValue(helperInstance);
        cHelper.getDefinition("foo");
        chCtrl.setReturnValue(typeDef);

        chCtrl.replay();
        prjCtrl.replay();
        vfyCtrl.replay();
        svcCtrl.replay();
        atdCtrl.replay();
        instance.execute();
        chCtrl.verify();
        prjCtrl.verify();
        vfyCtrl.verify();
        svcCtrl.verify();
        atdCtrl.verify();
    }
    
    public void testExecuteNoDefWithBadReference() throws Exception {
        MockControl chCtrl = MockClassControl.createNiceControl(ComponentHelper.class);
        ComponentHelper cHelper = (ComponentHelper)chCtrl.getMock();
        
        MockControl prjCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prjCtrl.getMock();
        project.getReference("ant.ComponentHelper");
        prjCtrl.setReturnValue(cHelper);

        ServiceInstance instance = new ServiceInstance();
        instance.setProject(project);
        instance.setReference("foo");
        
        MockControl atdCtrl = MockClassControl.createControl(AntTypeDefinition.class);
        AntTypeDefinition typeDef = (AntTypeDefinition)atdCtrl.getMock();
        typeDef.create(project);
        atdCtrl.setReturnValue(new Object()); // bad reference!
        cHelper.getDefinition("foo");
        chCtrl.setReturnValue(typeDef);

        chCtrl.replay();
        prjCtrl.replay();
        atdCtrl.replay();
        try {
            instance.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "No Service Definition found!", be.getMessage());
        }
        chCtrl.verify();
        prjCtrl.verify();
        atdCtrl.verify();
    }
    
    public void testExecuteStartFailure() throws Exception {
        MockControl prjCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prjCtrl.getMock();

        BuildException startExc = new BuildException("foo");
        MockControl vfyCtrl = MockClassControl.createControl(ServiceVerifyTask.class);
        ServiceVerifyTask verify = (ServiceVerifyTask)vfyCtrl.getMock();
        verify.setException(startExc);
        vfyCtrl.setVoidCallable();

        MockControl svcCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef service = (ServiceDef)svcCtrl.getMock();
        service.getVerifyTask();
        svcCtrl.setReturnValue(verify);
        service.start();
        svcCtrl.setThrowable(startExc);

        ServiceInstance instance = new ServiceInstance();
        instance.setProject(project);
        instance.setServiceDef(service);
        
        prjCtrl.replay();
        vfyCtrl.replay();
        svcCtrl.replay();
        try {
            instance.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "foo", be.getMessage());
        }
        prjCtrl.verify();
        vfyCtrl.verify();
        svcCtrl.verify();
    }
    
    public void testExecuteVerifyFailure() throws Exception {
        MockControl prjCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prjCtrl.getMock();

        BuildException verifyExc = new BuildException("foo", new Location("bar"));
        MockControl vfyCtrl = MockClassControl.createNiceControl(ServiceVerifyTask.class);
        ServiceVerifyTask verify = (ServiceVerifyTask)vfyCtrl.getMock();
        verify.getProject();
        vfyCtrl.setReturnValue(project, 2);
        verify.execute();
        vfyCtrl.setThrowable(verifyExc);

        MockControl svcCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef service = (ServiceDef)svcCtrl.getMock();
        service.getVerifyTask();
        svcCtrl.setReturnValue(verify);
        service.start();
        svcCtrl.setVoidCallable();

        ServiceInstance instance = new ServiceInstance();
        instance.setProject(project);
        instance.setServiceDef(service);
        
        prjCtrl.replay();
        vfyCtrl.replay();
        svcCtrl.replay();
        try {
            instance.execute();
            fail("Expected ServiceVerifyException");
        } catch (ServiceVerifyException sve) {
            assertNotNull("No nested exception", sve.getCause());
            Object o = sve.getCause();
            assertEquals("Wrong nested exception", verifyExc, o);
            assertEquals("Wrong message", "foo", sve.getCause().getMessage());
        }
        prjCtrl.verify();
        vfyCtrl.verify();
        svcCtrl.verify();
    }
    
    public void testExecuteStop() throws Exception {
        MockControl prjCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prjCtrl.getMock();

        MockControl vfyCtrl = MockClassControl.createControl(ServiceVerifyTask.class);
        ServiceVerifyTask verify = (ServiceVerifyTask)vfyCtrl.getMock();

        MockControl svcCtrl = MockClassControl.createControl(ServiceDef.class);
        ServiceDef service = (ServiceDef)svcCtrl.getMock();
        service.getVerifyTask();
        svcCtrl.setReturnValue(verify);
        service.stop();
        svcCtrl.setVoidCallable();

        ServiceInstance instance = new ServiceInstance();
        instance.setProject(project);
        instance.setServiceDef(service);
        instance.setAction("stop");
        
        prjCtrl.replay();
        vfyCtrl.replay();
        svcCtrl.replay();
        instance.execute();
        prjCtrl.verify();
        vfyCtrl.verify();
        svcCtrl.verify();
    }
}
