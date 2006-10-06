package org.codehaus.xharness.log;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class LogOutputStreamTest extends TestCase {
    public LogOutputStreamTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = LogOutputStreamTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = LogOutputStreamTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(LogOutputStreamTest.class);
    }
    
    public void testWrite() throws Exception {
        LogOutputStream los = new LogOutputStream();
        los.write('f');
        los.write("oo".getBytes());
        assertEquals("Wrong stream contents", "", los.getBuffer().toString());
        los.flush();
        assertEquals("Wrong stream contents", "foo", los.getBuffer().toString());
        los.write("xbarx".getBytes(), 1, 3);
        assertEquals("Wrong stream contents", "foo", los.getBuffer().toString());
        los.flush();
        assertEquals("Wrong stream contents", "foo\nbar", los.getBuffer().toString());
        los.write('\r');
        los.write('\n');
        los.write("spam".getBytes());
        los.flush();
        los.write("eggs".getBytes());
        los.write("!!!".getBytes());
        assertEquals("Wrong stream contents", "foo\nbar\nspam", los.getBuffer().toString());
        los.close();
        assertEquals("Wrong stream contents", 
                     "foo\nbar\nspam\neggs!!!", 
                     los.getBuffer().toString());
        
    }
}
