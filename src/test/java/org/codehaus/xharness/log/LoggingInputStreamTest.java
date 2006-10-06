package org.codehaus.xharness.log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class LoggingInputStreamTest extends TestCase {
    public LoggingInputStreamTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = LoggingInputStreamTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = LoggingInputStreamTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(LoggingInputStreamTest.class);
    }
    
    public void testRead() throws Exception {
        byte[] data = "foobar".getBytes();
        byte[] batch1 = new byte[2];
        byte[] batch2 = new byte[5];
        InputStream is = new ByteArrayInputStream(data);
        MockControl osCtrl = MockClassControl.createControl(OutputStream.class);
        OutputStream os = (OutputStream)osCtrl.getMock();
        os.write(data[0]);
        os.write(batch1, 0, 2);
        os.write(batch2, 1, 3);
        os.close();

        osCtrl.replay();
        
        LoggingInputStream lis = new LoggingInputStream(is, os);
        assertEquals("Wrong data read", data[0], lis.read());
        assertEquals("Wrong data read", 2, lis.read(batch1));
        assertEqualsArray("Wrong data read", "oo".getBytes(), batch1);
        
        assertEquals("Wrong data read", 3, lis.read(batch2, 1, 4));
        assertEqualsArray("Wrong data read", "\0bar\0".getBytes(), batch2);
        
        assertEquals("Wrong data read", -1, lis.read());
        assertEquals("Wrong data read", -1, lis.read(batch1));

        lis.close();
        
        osCtrl.verify();
    }
    
    public void testEquals() throws Exception {
        InputStream is1 = new ByteArrayInputStream(new byte[0]);
        InputStream is2 = new ByteArrayInputStream(new byte[0]);
        OutputStream os1 = new ByteArrayOutputStream();
        OutputStream os2 = new ByteArrayOutputStream();
        LoggingInputStream lis = new LoggingInputStream(is1, os1);
        
        assertTrue("Objects should be equal", lis.equals(lis));
        assertTrue("Objects should be equal", lis.equals(new LoggingInputStream(is1, os1)));
        assertTrue("Objects shouldn't be equal", !lis.equals(new LoggingInputStream(is2, os1)));
        assertTrue("Objects shouldn't be equal", !lis.equals(new LoggingInputStream(is1, os2)));
        assertTrue("Objects shouldn't be equal", !lis.equals(new Object()));
        
        assertTrue("Wrong hashCode", lis.hashCode() != 0);
        
    }
    
    private static void assertEqualsArray(String msg, byte[] o1, byte[] o2) {
        StringBuffer s1 = new StringBuffer();
        s1.append("[ ");
        for (int i = 0; i < o1.length; i++) {
            if (i != 0) {
                s1.append(", ");
            }
            s1.append(o1[i]);
        }
        s1.append(" ]");
        
        StringBuffer s2 = new StringBuffer();
        s2.append("[ ");
        for (int i = 0; i < o2.length; i++) {
            if (i != 0) {
                s2.append(", ");
            }
            s2.append(o2[i]);
        }
        s2.append(" ]");
        
        assertEquals(msg, s1.toString(), s2.toString());
    }
}
