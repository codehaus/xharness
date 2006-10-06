package org.codehaus.xharness.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class WhichTaskTest extends TestCase {
    public WhichTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = WhichTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = WhichTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(WhichTaskTest.class);
    }
    
    public void testExecuteNoExecutable() throws Exception {
        WhichTask which = new WhichTask();
        
        try {
            which.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "The executable attribute is required.", be.getMessage());
        }
    }
    
    public void testExecuteNoPath() throws Exception {
        WhichTask which = new WhichTask();
        which.setExecutable("foo");
        
        try {
            which.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "The path attribute is required.", be.getMessage());
        }
    }
    
    public void testExecuteNoProperty() throws Exception {
        WhichTask which = new WhichTask();
        which.setExecutable("foo");
        which.setPath("spam;eggs");
        
        try {
            which.execute();
            fail("Expected BuildException");
        } catch (BuildException be) {
            assertEquals("Wrong message", "The property attribute is required.", be.getMessage());
        }
    }
    
    public void testExecuteWindowsExists() throws Exception {
        doTest("", true, true, true, false);
    }
    
    public void testExecuteWindowsComExists() throws Exception {
        doTest(".com", true, true, true, true);
    }
    
    public void testExecuteWindowsExeExists() throws Exception {
        doTest(".exe", true, true, false, true);
    }
    
    public void testExecuteWindowsBatExists() throws Exception {
        doTest(".bat", true, true, true, true);
    }
    
    public void testExecuteWindowsShExists() throws Exception {
        doTest(".sh", true, true, false, true);
    }
    
    public void testExecuteWindowsNonexist() throws Exception {
        doTest("", true, false, true, false);
    }
    
    public void testExecuteWindowsComNonexist() throws Exception {
        doTest(".com", true, false, true, false);
    }
    
    public void testExecuteWindowsExeNonexist() throws Exception {
        doTest(".exe", true, false, false, false);
    }
    
    public void testExecuteWindowsBatNonexist() throws Exception {
        doTest(".bat", true, false, true, false);
    }
    
    public void testExecuteWindowsShNonexist() throws Exception {
        doTest(".sh", true, false, false, false);
    }
    
    public void testExecuteWindowsOther() throws Exception {
        doTest(".xml", true, true, false, false);
    }
    
    public void testExecuteUnixExists() throws Exception {
        doTest("", false, true, true, true);
    }
    
    public void testExecuteUnixComExists() throws Exception {
        doTest(".com", false, true, true, false);
    }
    
    public void testExecuteUnixExeExists() throws Exception {
        doTest(".exe", false, true, false, false);
    }
    
    public void testExecuteUnixBatExists() throws Exception {
        doTest(".bat", false, true, true, false);
    }
    
    public void testExecuteUnixShExists() throws Exception {
        doTest(".sh", false, true, false, false);
    }
    
    public void testExecuteUnixNonexist() throws Exception {
        doTest("", false, false, true, false);
    }
    
    public void testExecuteUnixComNonexist() throws Exception {
        doTest(".com", false, false, true, false);
    }
    
    public void testExecuteUnixExeNonexist() throws Exception {
        doTest(".exe", false, false, false, false);
    }
    
    public void testExecuteUnixBatNonexist() throws Exception {
        doTest(".bat", false, false, true, false);
    }
    
    public void testExecuteUnixShNonexist() throws Exception {
        doTest(".sh", false, false, false, false);
    }
    
    public void testExecuteUnixOther() throws Exception {
        doTest(".xml", false, true, false, false);
    }
    
    private void doTest(String fileExt, 
                        boolean onWindows, 
                        boolean createFile, 
                        boolean failOnError,
                        boolean expectSuccess) throws Exception {
        WhichTask which = new WhichTask();
        which.setExecutable("foo");
        File currDir = new File(".");
        String path = "/bogus/path;" + currDir.getCanonicalPath() + ";/non/existent";
        which.setPath(path);
        which.setProperty("result.property");
        
        
        File executable = new File(currDir, "foo" + fileExt);
        MockControl ctrl = MockClassControl.createControl(Project.class);
        ctrl.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
        Project project = (Project)ctrl.getMock();
        project.log((Task)null, null, 0);
        int logCalls = 5;
        if (onWindows) {
            if (!expectSuccess) {
                logCalls = 25;
            } else if (".com".equals(fileExt)) {
                logCalls = 11;
            } else if (".exe".equals(fileExt)) {
                logCalls = 13;
            } else if (".bat".equals(fileExt)) {
                logCalls = 15;
            } else if (".sh".equals(fileExt)) {
                logCalls = 17;
            } else {
                logCalls = 25;
            }
        } else if (!expectSuccess) {
            logCalls = 7;
        }
        ctrl.setVoidCallable(logCalls);
        which.setProject(project);
        which.setFailOnError(failOnError);
        if (expectSuccess) {
            project.setNewProperty("result.property", executable.getCanonicalPath());
        }
        
        String savedProperty = System.getProperty("os.name");
        try {
            if (createFile) {
                executable.createNewFile();
            }
            System.setProperty("os.name", onWindows ? "windows" : "bogus");
            ctrl.replay();
            try {
                which.execute();
                if (!expectSuccess && failOnError) {
                    fail("Expected BuildException");
                }
            } catch (BuildException be) {
                if (expectSuccess || !failOnError) {
                    fail("Unexpected BuildException: " + be);
                }
                assertEquals("Wrong message", 
                             "File foo not found in path " + path, 
                             be.getMessage());
            }
            ctrl.verify();
        } finally {
            System.setProperty("os.name", savedProperty);
            if (executable.exists()) {
                executable.delete();
            }
        }
    }
}
