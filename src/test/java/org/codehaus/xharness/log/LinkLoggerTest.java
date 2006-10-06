package org.codehaus.xharness.log;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class LinkLoggerTest extends TestCase {
    public LinkLoggerTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = LinkLoggerTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = LinkLoggerTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(LinkLoggerTest.class);
    }
    
    public void testGetTaskType() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project);

        prCtrl.replay();
        trCtrl.replay();

        LinkLogger logger = new LinkLogger(registry, null, null, null, null);
        assertEquals("Wrong task type", Result.LINK, logger.getTaskType());

        prCtrl.verify();
        trCtrl.verify();
    }
    
    public void testMessageLogged() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        
        MockControl trCtrl = MockClassControl.createControl(TaskRegistry.class);
        TaskRegistry registry = (TaskRegistry)trCtrl.getMock();
        registry.getNextId();
        trCtrl.setReturnValue(101);
        registry.getProject();
        trCtrl.setReturnValue(project);
        
        MockControl tkCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)tkCtrl.getMock();

        MockControl evCtrl = MockClassControl.createControl(BuildEvent.class);
        BuildEvent event = (BuildEvent)evCtrl.getMock();
        event.getMessage();
        evCtrl.setReturnValue("blah", 2);
        event.getTask();
        evCtrl.setReturnValue(task, 3);
        event.getPriority();
        evCtrl.setReturnValue(2);
        
        prCtrl.replay();
        trCtrl.replay();
        tkCtrl.replay();
        evCtrl.replay();
        
        LinkLogger logger = new LinkLogger(registry, task, null, null, null);
        logger.messageLogged(event);
        assertEquals("Wrong log", "", logger.getLineBuffer().toString());
        
        prCtrl.verify();
        trCtrl.verify();
        tkCtrl.verify();
        evCtrl.verify();
    }
}
