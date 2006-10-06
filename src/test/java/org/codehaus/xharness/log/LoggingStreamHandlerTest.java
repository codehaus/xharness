package org.codehaus.xharness.log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class LoggingStreamHandlerTest extends TestCase {
    public LoggingStreamHandlerTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = LoggingStreamHandlerTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = LoggingStreamHandlerTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(LoggingStreamHandlerTest.class);
    }
    
    public void test() throws Exception {
        InputStream is1 = new ByteArrayInputStream("\nfoo\n\nbar\n".getBytes());
        InputStream is2 = new ByteArrayInputStream("\nspam\n\neggs\n".getBytes());
        
        LogOutputStream os1 = new LogOutputStream();
        LogOutputStream os2 = new LogOutputStream();
        OutputStream os3 = new ByteArrayOutputStream();

        MockControl eshCtrl = MockClassControl.createControl(ExecuteStreamHandler.class);
        ExecuteStreamHandler esHandler = (ExecuteStreamHandler)eshCtrl.getMock();
        esHandler.setProcessOutputStream(new LoggingInputStream(is1, os1));
        esHandler.setProcessErrorStream(new LoggingInputStream(is2, os2));
        esHandler.setProcessInputStream(os3);
        esHandler.start();
        esHandler.stop();
        
        eshCtrl.replay();
        
        LoggingStreamHandler lsHandler = new LoggingStreamHandler(esHandler, os1, os2);
        lsHandler.setProcessOutputStream(is1);
        lsHandler.setProcessErrorStream(is2);
        lsHandler.setProcessInputStream(os3);
 
        lsHandler.start();
        lsHandler.stop();
        
        eshCtrl.verify();
    }
}
