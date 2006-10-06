package org.codehaus.xharness.log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class LoggingRedirectorTest extends TestCase {
    public LoggingRedirectorTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = LoggingRedirectorTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = LoggingRedirectorTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(LoggingRedirectorTest.class);
    }
    
    public void testLoggingDisabled() throws Exception {
        MockControl tkCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)tkCtrl.getMock();
        task.log("foobar", 2);

        tkCtrl.replay();
        
        LoggingRedirector redirector = new LoggingRedirector(task);
        ExecuteStreamHandler handler = redirector.createHandler();
        assertEquals("Wrong handler class", 
                     PumpStreamHandler.class.getName(), 
                     handler.getClass().getName());
        redirector.handleOutput("foo");
        redirector.handleFlush("bar");
        redirector.handleErrorOutput("spam");
        redirector.handleErrorFlush("eggs");
        
        tkCtrl.verify();
    }
    
    public void testLoggingEnabled() throws Exception {
        MockControl tkCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)tkCtrl.getMock();
        task.log("foobar", 2);
        
        LineBuffer buffer = new LineBuffer();

        tkCtrl.replay();
        
        LoggingRedirector redirector = new LoggingRedirector(task);
        redirector.enableLogging(buffer, 1, 2);
        ExecuteStreamHandler handler = redirector.createHandler();
        assertEquals("Wrong handler class", 
                     LoggingStreamHandler.class.getName(), 
                     handler.getClass().getName());
        redirector.handleOutput("foo");
        redirector.handleFlush("bar");
        redirector.handleErrorOutput("spam");
        redirector.handleErrorFlush("eggs");
        
        assertEquals("Wrong log", "foo\nbar", buffer.toString(1));
        assertEquals("Wrong log", "spam\neggs", buffer.toString(2));
        
        tkCtrl.verify();
    }
    
    public void testStreamHandler() throws Exception {
        MockControl tkCtrl = MockClassControl.createControl(Task.class);
        Task task = (Task)tkCtrl.getMock();
        
        LineBuffer buffer = new LineBuffer();

        tkCtrl.replay();
        
        LoggingRedirector redirector = new LoggingRedirector(task);
        redirector.enableLogging(buffer, 1, 2);
        ExecuteStreamHandler handler = redirector.createHandler();
        handler.setProcessOutputStream(new ByteArrayInputStream("\nfoo\n\nbar\n".getBytes()));
        handler.setProcessErrorStream(new ByteArrayInputStream("\nspam\n\neggs\n".getBytes()));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        handler.setProcessInputStream(os);
        assertEquals("Wrong log", "", buffer.toString(1));
        assertEquals("Wrong log", "", buffer.toString(2));
        assertEquals("Wrong log", "", os.toString());

        handler.start();
        handler.stop();
        
        assertEquals("Wrong log", "foo\nbar", buffer.toString(1));
        assertEquals("Wrong log", "spam\neggs", buffer.toString(2));
        assertEquals("Wrong log", "", os.toString());
        
        tkCtrl.verify();
    }
}
