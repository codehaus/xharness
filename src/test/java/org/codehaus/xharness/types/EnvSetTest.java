package org.codehaus.xharness.types;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class EnvSetTest extends TestCase {
    public EnvSetTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("gui") != null) {
            String[] newArgs = new String[args.length + 2];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = EnvSetTest.class.getName();
            newArgs[args.length + 1] = "-noloading";
            junit.swingui.TestRunner.main(newArgs);
        } else {
            String[] newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[args.length] = EnvSetTest.class.getName();
            junit.textui.TestRunner.main(newArgs);
        }
    }

    public static Test suite() {
        return new TestSuite(EnvSetTest.class);
    }
    
    public void testAddEnv() throws Exception {
        EnvSet set = new EnvSet();
        EnvironmentVariable var = new EnvironmentVariable("foo", "bar");
        set.addEnv(var);
        EnvironmentVariable[] vars = set.getVariables(new Project());
        assertEquals("Wrong number of variables", 1, vars.length);
        assertEquals("Wrong env var", var, vars[0]);

        vars = set.getVariables(new Project());
        assertEquals("Wrong number of variables", 1, vars.length);
        assertEquals("Wrong env var", var, vars[0]);
    }
    
    public void testReference() throws Exception {
        EnvSet set1 = new EnvSet();
        EnvironmentVariable var = new EnvironmentVariable("foo", "bar");
        set1.addEnv(var);

        MockControl prCtrl = MockClassControl.createControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        project.getReference("ref");
        prCtrl.setReturnValue(set1, 2);
        
        
        EnvSet set2 = new EnvSet();
        set2.setRefid(new Reference(null, "ref"));
        prCtrl.replay();
        
        try {
            set2.addEnv(new EnvironmentVariable("spam", "eggs"));
            fail("Expected BuildException");
        } catch (BuildException be) {
            // reference mustn't have child elements
        }
        
        EnvironmentVariable[] vars = set2.getVariables(project);
        assertEquals("Wrong number of variables", 1, vars.length);
        assertEquals("Wrong env var", var, vars[0]);
        
        prCtrl.verify();
    }
    
    public void testBadReference() throws Exception {
        MockControl prCtrl = MockClassControl.createControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        project.getReference("ref");
        prCtrl.setReturnValue(new Object(), 2);
        
        
        EnvSet set = new EnvSet();
        set.setRefid(new Reference(null, "ref"));
        prCtrl.replay();
        
        try {
            set.getVariables(project);
            fail("Expected BuildException");
        } catch (BuildException be) {
            // reference mustn't have child elements
        }
        
        prCtrl.verify();
    }
    
    public void testLoadEnvironment() throws Exception {
        MockControl prCtrl = MockClassControl.createControl(Project.class);
        Project project = (Project)prCtrl.getMock();

        prCtrl.replay();

        EnvSet set1 = new EnvSet();
        set1.setLoadenvironment(false);
        assertEquals("Wrong number of variables", 0, set1.getVariables(project).length);
        
        EnvSet set2 = new EnvSet();
        set2.setLoadenvironment(true);
        EnvironmentVariable[] vars = set2.getVariables(project);
        assertTrue("Wrong number of variables", vars.length > 0);
        
        prCtrl.verify();
    }
    
    public void testLoadEnvironmentNoDefault() throws Exception {
        MockControl prCtrl = MockClassControl.createControl(Project.class);
        Project project = (Project)prCtrl.getMock();
        project.resolveFile("/foo/bar");
        prCtrl.setReturnValue(new File("/foo/bar"));
        

        prCtrl.replay();

        String pathVar = System.getProperty("os.name").toLowerCase().startsWith("win") 
                         ? "Path" 
                         : "PATH";
        Path pathVal = new Path(project, "/foo/bar");
        EnvironmentVariable var = new EnvironmentVariable(pathVar, pathVal);
        var.setAppend(true);

        EnvSet set = new EnvSet();
        set.setLoadenvironment(true);
        set.setNodefault(true);
        set.addEnv(var);
        EnvironmentVariable[] vars = set.getVariables(project);
        assertEquals("Wrong number of variables", 1, vars.length);
        boolean found = false;
        for (int i = 0; i < vars.length; i++) {
            if (pathVar.equals(vars[i].getKey())) {
                assertTrue("Path not appended: " + vars[i].getContent(), 
                           vars[i].getContent().endsWith("foo" + File.separator + "bar"));
                found = true;
                break;
            }
        }
        assertTrue("Path variable not found", found);
        
        prCtrl.verify();
    }
    
    public void testAddConfiguredEnvset() throws Exception {
        EnvSet set1 = new EnvSet();
        EnvironmentVariable var = new EnvironmentVariable("foo", "bar");
        set1.addEnv(var);

        EnvSet set2 = new EnvSet();
        set2.setRefid(new Reference(null, "ref"));
        try {
            set2.addConfiguredEnvset(set1);
            fail("Expected BuildException");
        } catch (BuildException be) {
            // reference mustn't have child elements
        }

        EnvSet set3 = new EnvSet();
        set3.addConfiguredEnvset(set1);
        EnvironmentVariable[] vars = set3.getVariables(new Project());
        assertEquals("Wrong number of variables", 1, vars.length);
        assertEquals("Wrong env var", var, vars[0]);
    }
}
