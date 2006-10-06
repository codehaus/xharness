package org.codehaus.xharness.types;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FileContainsTest extends TestCase {
    public FileContainsTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = FileContainsTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = FileContainsTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(FileContainsTest.class);
    }
    
    public void testCdataAndString() throws Exception {
        MockControl prCtrl = MockClassControl.createControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        project.replaceProperties("foo");
        prCtrl.setReturnValue("spam");
        project.replaceProperties("bar");
        prCtrl.setReturnValue("eggs");
        
        prCtrl.replay();

        FileContains cond1 = new FileContains();
        cond1.setProject(project);
        cond1.setString("foo");
        assertEquals("Wrong text", "spam", cond1.getText());
        try {
            cond1.addText("bar");
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Cannot use string argument and CDATA", be.getMessage());
        }

        FileContains cond2 = new FileContains();
        cond2.setProject(project);
        cond2.addText("bar");
        assertEquals("Wrong text", "eggs", cond2.getText());
        try {
            cond2.setString("foo");
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "Cannot use string argument and CDATA", be.getMessage());
        }
        
        prCtrl.verify();
    }
    
    public void testNoText() throws Exception {
        FileContains condition = new FileContains();
        try {
            condition.eval();
            fail("Expected BuildExcption");
        } catch (BuildException be) {
            assertEquals("Wrong message", "text not defined", be.getMessage());
        }
    }

    public void testNoFile() throws Exception {
        MockControl prCtrl = MockClassControl.createControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        project.replaceProperties("foo");
        prCtrl.setReturnValue("foo");
        
        prCtrl.replay();

        FileContains condition = new FileContains();
        condition.setProject(project);
        condition.setString("foo");
        try {
            condition.eval();
            fail("Expected BuildExcption");
        } catch (BuildException be) {
            assertEquals("Wrong message", "file not defined", be.getMessage());
        }
        
        prCtrl.verify();
    }

    public void testBadFile() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        project.replaceProperties("foo");
        prCtrl.setReturnValue("foo");
        
        prCtrl.replay();

        FileContains condition = new FileContains();
        condition.setProject(project);
        condition.setString("foo");
        condition.setFile(new File("nonexist"));
        
        assertTrue("Condition evaluated wrong", !condition.eval());
        
        prCtrl.verify();
    }
    
    public void testEvalTrue() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        project.replaceProperties("foo");
        prCtrl.setReturnValue("foo");
        
        prCtrl.replay();

        File f = File.createTempFile("test", ".tmp");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write("Hello\n");
            bw.write("foo\n");
            bw.write("world\n");
            bw.close();

            FileContains condition = new FileContains();
            condition.setProject(project);
            condition.setString("foo");
            condition.setFile(f);
            
            assertTrue("Condition evaluated wrong", condition.eval());
            
        } finally {
            f.delete();
        }

        prCtrl.verify();
    }
    
    public void testEvalFalse() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        project.replaceProperties("foow");
        prCtrl.setReturnValue("foow");
        
        prCtrl.replay();

        File f = File.createTempFile("test", ".tmp");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write("Hello\n");
            bw.write("foo\n");
            bw.write("world\n");
            bw.close();

            FileContains condition = new FileContains();
            condition.setProject(project);
            condition.setString("foow");
            condition.setFile(f);
            
            assertTrue("Condition evaluated wrong", !condition.eval());
            
        } finally {
            f.delete();
        }

        prCtrl.verify();
    }
    
    public void testEncodingFails() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        project.replaceProperties("foo");
        prCtrl.setReturnValue("foo");
        
        prCtrl.replay();

        File f = File.createTempFile("test", ".tmp");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, "UTF-16"));
            bw.write("Hello\n");
            bw.write("foo\n");
            bw.write("world\n");
            bw.close();

            FileContains condition = new FileContains();
            condition.setProject(project);
            condition.setString("foo");
            condition.setFile(f);
            
            assertTrue("Condition evaluated wrong", !condition.eval());
            
        } finally {
            f.delete();
        }

        prCtrl.verify();
    }
    
    public void testEncodingPasses() throws Exception {
        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        project.replaceProperties("foo");
        prCtrl.setReturnValue("foo");
        
        prCtrl.replay();

        File f = File.createTempFile("test", ".tmp");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, "UTF-16"));
            bw.write("Hello\n");
            bw.write("foo\n");
            bw.write("world\n");
            bw.close();

            FileContains condition = new FileContains();
            condition.setProject(project);
            condition.setString("foo");
            condition.setFile(f);
            condition.setEncoding("UTF-16");
            
            assertTrue("Condition evaluated wrong", condition.eval());
            
        } finally {
            f.delete();
        }

        prCtrl.verify();
    }
}
