package org.codehaus.xharness.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.optional.junit.AggregateTransformer;
import org.apache.tools.ant.types.FileSet;

import org.codehaus.xharness.TestHelper;
import org.codehaus.xharness.tasks.XhAggregateTransformer;
import org.codehaus.xharness.testutil.TempDir;

import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import org.w3c.dom.Element;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class XhReportTaskTest extends TestCase {
    private File tempDir;

    public XhReportTaskTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = XhReportTaskTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = XhReportTaskTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(XhReportTaskTest.class);
    }
    
    public void setUp() throws Exception {
        tempDir = TempDir.createTempDir(new File("."));
    }
    
    public void tearDown() throws Exception {
        TempDir.removeFiles(tempDir);
    }
    
    public void testFailOnError() throws Exception {
        XhReportTask task = new XhReportTask();
        task.setFailOnError(true);
    }
    
    public void testGetDestinationFile() throws Exception {
        MockControl ctrl = MockClassControl.createControl(Project.class);
        ctrl.getMock();

        XhReportTask task = new XhReportTask();
        task.setTodir(tempDir);
        task.setTofile("file");

        ctrl.replay();
        assertEquals("Incorrect Destination", new File(tempDir, "file"), task.getDestinationFile());
        ctrl.verify();
    }
    
    public void testGetDestinationFileNoTodir() throws Exception {
        MockControl ctrl = MockClassControl.createControl(Project.class);
        Project project = (Project)ctrl.getMock();
        project.resolveFile(XhReportTask.DEFAULT_DIR);
        ctrl.setReturnValue(tempDir);

        XhReportTask task = new XhReportTask();
        task.setProject(project);
        task.setTofile("file");

        ctrl.replay();
        assertEquals("Incorrect Destination", new File(tempDir, "file"), task.getDestinationFile());
        ctrl.verify();
    }
    
    public void testGetDestinationFileNoTofile() throws Exception {
        MockControl ctrl = MockClassControl.createControl(Project.class);
        ctrl.getMock();

        XhReportTask task = new XhReportTask();
        task.setTodir(tempDir);

        ctrl.replay();
        assertEquals("Incorrect Destination", 
                     new File(tempDir, XhReportTask.DEFAULT_FILENAME), task.getDestinationFile());
        ctrl.verify();
    }
    
    public void testCreateReport() throws Exception {
        XhReportTask task = new XhReportTask();
        
        AggregateTransformer transformer = task.createReport();
        assertTrue(transformer instanceof XhAggregateTransformer);
        String ssi = ((XhAggregateTransformer)transformer).getStylesheetSystemId();
        assertTrue("Wrong stylesheet", ssi.endsWith("/org/codehaus/xharness/xsl/frames.xsl"));
    }
    
    public void testSetStyledir() throws Exception {
        XhReportTask task = new XhReportTask();
        MockControl ctrl = MockClassControl.createControl(Project.class);
        Project project = (Project)ctrl.getMock();
        project.log(task, "This task doesn't support the styledir attribute. It is ignored.", Project.MSG_WARN);
        ctrl.getMock();
        task.setProject(project);
        AggregateTransformer transformer = task.createReport();
        assertTrue(transformer instanceof XhAggregateTransformer);
        ctrl.replay();
        transformer.setStyledir(new File("."));
        ctrl.verify();
    }
    
    public void testGetFiles() throws Exception {
        MockControl dsCtrl = MockClassControl.createControl(DirectoryScanner.class);
        DirectoryScanner scanner = (DirectoryScanner)dsCtrl.getMock();
        MockControl fsCtrl = MockClassControl.createControl(FileSet.class);
        FileSet fileset = (FileSet)fsCtrl.getMock();
        fileset.getDirectoryScanner(null);
        fsCtrl.setDefaultMatcher(MockClassControl.ALWAYS_MATCHER);
        fsCtrl.setReturnValue(scanner);
        scanner.scan();
        scanner.getIncludedFiles();
        dsCtrl.setReturnValue(new String[]{"foo.xml", "bar", "spam.xml", "eggs"});
        scanner.getBasedir();
        dsCtrl.setReturnValue(tempDir, 2);

        MockControl prCtrl = MockClassControl.createControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        project.resolveFile(new File(tempDir, "foo.xml").getPath());
        prCtrl.setReturnValue(new File("foo.resolved"));
        project.resolveFile(new File(tempDir, "spam.xml").getPath());
        prCtrl.setReturnValue(new File("spam.resolved"));
        
        XhReportTask task = new XhReportTask();
        task.setProject(project);
        task.addFileSet(fileset);
        
        fsCtrl.replay();
        dsCtrl.replay();
        prCtrl.replay();
        File[] resolved = task.getFiles();
        assertEquals("Wrong number of files", 2, resolved.length);
        assertEquals("Wrong file resolved", "foo.resolved", resolved[0].getName());
        assertEquals("Wrong file resolved", "spam.resolved", resolved[1].getName());
        fsCtrl.verify();
        dsCtrl.verify();
        prCtrl.verify();
    }
    
    public void testCreateDocument() throws Exception {
        MockControl dsCtrl = MockClassControl.createControl(DirectoryScanner.class);
        DirectoryScanner scanner = (DirectoryScanner)dsCtrl.getMock();
        MockControl fsCtrl = MockClassControl.createControl(FileSet.class);
        FileSet fileset = (FileSet)fsCtrl.getMock();
        fileset.getDirectoryScanner(null);
        fsCtrl.setDefaultMatcher(MockClassControl.ALWAYS_MATCHER);
        fsCtrl.setReturnValue(scanner);
        scanner.scan();
        scanner.getIncludedFiles();
        dsCtrl.setReturnValue(new String[]{"foo.xml", "bar", "spam.xml", "eggs"});
        scanner.getBasedir();
        dsCtrl.setReturnValue(new File("."), 2);

        MockControl prCtrl = MockClassControl.createControl(Project.class);
        prCtrl.setDefaultMatcher(MockClassControl.ALWAYS_MATCHER);
        Project project = (Project)prCtrl.getMock();
        project.log((Task)null, null, 0);
        prCtrl.setVoidCallable(4);
//        MockControl f1Ctrl = MockClassControl.createControl(File.class);
//        File file1 = (File)f1Ctrl.getMock();
//        file1.getAbsolutePath();
//        f1Ctrl.setReturnValue("foo.xml");
//        MockControl f2Ctrl = MockClassControl.createControl(File.class);
//        File file2 = (File)f2Ctrl.getMock();
//        file2.getAbsolutePath();
//        f2Ctrl.setReturnValue("spam.xml");
        project.resolveFile(null);
//        prCtrl.setReturnValue(file1);
        prCtrl.setReturnValue(new File("nonexist1"));
//      prCtrl.setReturnValue(file2);
        prCtrl.setReturnValue(new File("nonexist2"));
        
        XhReportTask task = new XhReportTask();
        task.setProject(project);
        task.addFileSet(fileset);
        
        fsCtrl.replay();
        dsCtrl.replay();
        prCtrl.replay();
//        f1Ctrl.replay();
//        f2Ctrl.replay();
        Element doc = task.createDocument();
        assertEquals("Wrong document name", "results", doc.getNodeName());
        fsCtrl.verify();
        dsCtrl.verify();
        prCtrl.verify();
//        f1Ctrl.verify();
//        f2Ctrl.verify();
    }
    
    public void testExecute() throws Exception {
        MockControl dsCtrl = MockClassControl.createControl(DirectoryScanner.class);
        DirectoryScanner scanner = (DirectoryScanner)dsCtrl.getMock();
        MockControl fsCtrl = MockClassControl.createControl(FileSet.class);
        FileSet fileset = (FileSet)fsCtrl.getMock();
        fileset.getDirectoryScanner(null);
        fsCtrl.setDefaultMatcher(MockClassControl.ALWAYS_MATCHER);
        fsCtrl.setReturnValue(scanner);
        scanner.scan();
        scanner.getIncludedFiles();
        dsCtrl.setReturnValue(new String[]{"foo.xml", "bar", "spam.xml", "eggs"});
        scanner.getBasedir();
        dsCtrl.setReturnValue(new File("."), 2);

        MockControl prCtrl = MockClassControl.createNiceControl(Project.class);
        prCtrl.setDefaultMatcher(MockClassControl.ALWAYS_MATCHER);
        Project project = (Project)prCtrl.getMock();
        project.resolveFile(null);
        prCtrl.setReturnValue(new File("nonexist"));
        prCtrl.setReturnValue(new File("."));
        
        XhReportTask task = new XhReportTask();
        task.setProject(project);
        task.addFileSet(fileset);
        task.createReport();
        task.setTodir(tempDir);
        
        fsCtrl.replay();
        dsCtrl.replay();
        prCtrl.replay();
        File[] outFiles = null;
        try {
            task.execute();
            fail("Expected BuildException");
        } catch (NullPointerException npe) {
            assertTrue(!TestHelper.isAnt16());
        } catch (BuildException be) {
            assertTrue(TestHelper.isAnt16());
        }
        outFiles = tempDir.listFiles();
        assertEquals("Invalid no. files in outout dir", 1, outFiles.length);
        assertEquals("Output file wrong name", 
                     XhReportTask.DEFAULT_FILENAME, 
                     outFiles[0].getName());
        assertTrue("Output file doesn't exist", outFiles[0].exists());
        fsCtrl.verify();
        dsCtrl.verify();
        prCtrl.verify();
    }
}
