package org.codehaus.xharness.log;

import junit.framework.TestCase;

public class LogLineTest extends TestCase {
    public void testGetPriority() {
        LogLine line = new LogLine(5, "hello world");
        assertEquals(5, line.getPriority());
    }

    public void testToString() {
        LogLine line = new LogLine(5, "hello world");
        assertEquals("5: hello world", line.toString());
    }

    public void testGetText() {
        LogLine line = new LogLine(5, "hello world");
        assertEquals("hello world", line.getText());
        
        line = new LogLine(5, "hel\u001B[123;456mlo");
        assertEquals("hel\u001B[123;456mlo", line.getText());
        assertEquals("hel\u001B[123;456mlo", line.getText(false));
        assertEquals("hello", line.getText(true));
        
        line = new LogLine(5, "hel\u001B[123mlo");
        assertEquals("hello", line.getText(true));
        
        line = new LogLine(5, "hel\u001B[123;mlo");
        assertEquals("hello", line.getText(true));
        
        line = new LogLine(5, "hel\u001B[123;456m\u001B[7mlo");
        assertEquals("hello", line.getText(true));
    }
}
