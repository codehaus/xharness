package org.codehaus.xharness.types;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class EnvironmentVariableTest extends TestCase {
    public EnvironmentVariableTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = EnvironmentVariableTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = EnvironmentVariableTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(EnvironmentVariableTest.class);
    }
    
    public void testCtor1() throws Exception {
        EnvironmentVariable var = new EnvironmentVariable();
        assertNull("Wrong var key", var.getKey());
        assertNull("Wrong var value", var.getValue());
        try {
            var.getContent();
            fail("Expected BuildException");
        } catch (BuildException be) {
            // key and value must be specified
        }
    }
    
    public void testCtor2() throws Exception {
        EnvironmentVariable var = new EnvironmentVariable("foo", "bar");
        assertEquals("Wrong var key", "foo", var.getKey());
        assertEquals("Wrong var value", "bar", var.getValue());
        assertEquals("Wrong var", "foo=bar", var.getContent());
    }
    
    public void testCtor3() throws Exception {
        File f = new File("bar");
        EnvironmentVariable var = new EnvironmentVariable("foo", f);
        assertEquals("Wrong var key", "foo", var.getKey());
        assertEquals("Wrong var value", f.getAbsolutePath(), var.getValue());
        assertEquals("Wrong var", "foo=" + f.getAbsolutePath(), var.getContent());
    }
    
    public void testCtor4() throws Exception {
        Path p = new Path(new Project(),  "/foo/bar:/spam/eggs/");
        EnvironmentVariable var = new EnvironmentVariable("foo", p);
        assertEquals("Wrong var key", "foo", var.getKey());
        assertEquals("Wrong var value", p.toString(), var.getValue());
        assertEquals("Wrong var", "foo=" + p.toString(), var.getContent());
    }
    
    public void testPrepend() throws Exception {
        EnvironmentVariable var1 = new EnvironmentVariable("foo", "bar");
        EnvironmentVariable var2 = new EnvironmentVariable("spam", "eggs");
        EnvironmentVariable var3 = new EnvironmentVariable("foo", "eggs");

        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(null);
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(var2);
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(var3);
        assertEquals("Wrong var", "foo=bar", var1.getContent());

        var1.setPrepend(false);
        
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(null);
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(var2);
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(var3);
        assertEquals("Wrong var", "foo=bar", var1.getContent());

        var1.setPrepend(true);
        
        var1.combineWith(null);
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(var2);
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(var3);
        assertEquals("Wrong var", "foo=bareggs", var1.getContent());
    }
    
    public void testPrependPath() throws Exception {
        Path p1 = new Path(new Project(),  "spam");
        EnvironmentVariable var1 = new EnvironmentVariable("foo", p1);
        Path p2 = new Path(new Project(),  "eggs");
        EnvironmentVariable var2 = new EnvironmentVariable("foo", p2);

        var1.setPrepend(true);
        var1.combineWith(var2);
        assertEquals("Wrong var", 
                "foo=" + p1.toString() + File.pathSeparator + p2.toString(), 
                var1.getContent());
    }
    
    public void testAppend() throws Exception {
        EnvironmentVariable var1 = new EnvironmentVariable("foo", "bar");
        EnvironmentVariable var2 = new EnvironmentVariable("spam", "eggs");
        EnvironmentVariable var3 = new EnvironmentVariable("foo", "eggs");

        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(null);
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(var2);
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(var3);
        assertEquals("Wrong var", "foo=bar", var1.getContent());

        var1.setAppend(false);
        
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(null);
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(var2);
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(var3);
        assertEquals("Wrong var", "foo=bar", var1.getContent());

        var1.setAppend(true);
        
        var1.combineWith(null);
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(var2);
        assertEquals("Wrong var", "foo=bar", var1.getContent());
        var1.combineWith(var3);
        assertEquals("Wrong var", "foo=eggsbar", var1.getContent());
    }
    
    public void testAppendPath() throws Exception {
        Path p1 = new Path(new Project(),  "spam");
        EnvironmentVariable var1 = new EnvironmentVariable("foo", p1);
        Path p2 = new Path(new Project(),  "eggs");
        EnvironmentVariable var2 = new EnvironmentVariable("foo", p2);

        var1.setAppend(true);
        var1.combineWith(var2);
        assertEquals("Wrong var", 
                     "foo=" + p2.toString() + File.pathSeparator + p1.toString(), 
                     var1.getContent());
    }
    
    public void testPrependAndAppend() throws Exception {
        EnvironmentVariable var = new EnvironmentVariable("foo", "bar");
        var.setPrepend(true);
        var.setPrepend(false);
        var.setAppend(true);
        var.setAppend(false);
        var.setPrepend(true);
        try {
            var.setAppend(true);
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", 
                         "Only \"prepend\" OR \"append\" equals \"true\" allowed!", 
                         be.getMessage());
        }
    }
}
