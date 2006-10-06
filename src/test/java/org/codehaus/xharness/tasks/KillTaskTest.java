package org.codehaus.xharness.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.codehaus.xharness.procutil.BgProcess;
import org.codehaus.xharness.procutil.ProcessRegistry;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class KillTaskTest extends TestCase {
    private static final String PROC_NAME = "foo";
    private Project project;

    public KillTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = KillTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = KillTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(KillTaskTest.class);
    }
    
    public void setUp() throws Exception {
        MockControl ctrl = MockClassControl.createControl(Project.class);
        project = (Project)ctrl.getMock();
        try {
            ProcessRegistry.unregisterProcess(PROC_NAME);
        } catch (BuildException be) {
            // ignore
        }
    }
    
    public void testExecute() throws Exception {
        KillTask kill = new KillTask();
        kill.setProject(project);
        kill.setProcessname(PROC_NAME);
        MockControl ctrl = MockControl.createControl(BgProcess.class);
        BgProcess proc = (BgProcess)ctrl.getMock();
        proc.kill();
        
        ctrl.replay();
        ProcessRegistry.registerProcess(PROC_NAME, proc);
        kill.execute();
        ctrl.verify();
    }
    
    public void testExecuteNoProcessName() throws Exception {
        KillTask kill = new KillTask();
        kill.setProject(project);
        try {
            kill.execute();
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "You must provide a process name in order to use the KillProcess task", 
                         be.getMessage());
        }
    }
    
    public void testExecuteFailingKill() throws Exception {
        KillTask kill = new KillTask();
        kill.setProject(project);
        kill.setProcessname(PROC_NAME);
        MockControl ctrl = MockControl.createControl(BgProcess.class);
        BgProcess proc = (BgProcess)ctrl.getMock();
        proc.kill();
        ctrl.setThrowable(new BuildException("blah"));
        
        ctrl.replay();
        ProcessRegistry.registerProcess(PROC_NAME, proc);
        try {
            kill.execute();
        } catch (BuildException be) {
            assertEquals("Wrong message", "blah", be.getMessage());
        }
        ctrl.verify();
    }
    
    public void testExecuteFailOnError() throws Exception {
        KillTask kill = new KillTask();
        kill.setProject(project);
        kill.setProcessname(PROC_NAME);
        kill.setFailonerror(false);
        MockControl ctrl = MockControl.createControl(BgProcess.class);
        BgProcess proc = (BgProcess)ctrl.getMock();
        proc.kill();
        ctrl.setThrowable(new BuildException("blah"));
        
        ctrl.replay();
        ProcessRegistry.registerProcess(PROC_NAME, proc);
        kill.execute();
        ctrl.verify();
    }
}
