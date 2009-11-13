/*
 * Copyright 2006 IONA Technologies
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.codehaus.xharness.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Environment;

import org.codehaus.xharness.log.LineBuffer;
import org.codehaus.xharness.log.LoggingRedirector;
import org.codehaus.xharness.log.TaskRegistry;
import org.codehaus.xharness.procutil.KillableExecuteWatchdog;
import org.codehaus.xharness.procutil.LoggableProcess;
import org.codehaus.xharness.types.EnvSet;
import org.codehaus.xharness.types.EnvironmentVariable;

/**
 * Extension of the standard ant Java task that adds the following: 
 * - support for nested EnvSet types - output capturing (stdout/stderr). 
 * - Watchdog that allows process killing.
 * - override the JVM, via java system property "java.vm"
 * - adds the VM argument "-XdoCloseWithReadPending" on the HP-UX platform. 
 * - adds the system property "java.net.preferIPv4Stack=true" on AIX/JDK 1.4.
 * 
 * @author Gregor Heine
 * @see org.apache.tools.ant.taskdefs.Java
 */
public class XhJavaTask extends Java implements LoggableProcess {
    private KillableExecuteWatchdog watchDog;
    private EnvSet envSet = null;
    private File dir;
    private long procTimeout = 60 * 60 * 1000; // 1 hour
    private int retVal = 0;
    private boolean overrideFailOnError = true;

    public XhJavaTask() {
        this.redirector = new LoggingRedirector(this);
        setFork(true);
    }

    /**
     * Set the working directory of the process.
     *
     * @param d  the directory the process ist run in.
     */
    public void setDir(File d) {
        super.setDir(d);
        dir = d;
    }

    public void setTimeout(Long value) {
        procTimeout = value.longValue();
        super.setTimeout(value);
    }
    
    public void setSpawn(boolean b) {
        overrideFailOnError = false;
        super.setSpawn(b);
    }
    
    public void setFailonerror(boolean f) {
        overrideFailOnError = false;
        super.setFailonerror(f);
    }

    public void execute() throws BuildException {
        // Setup the executable name and the directory to run the process in.
        // Prepend the path to the executable if required.
        String ctdProperty = getProject().getProperty(TaskRegistry.CURRENT_TEST_DIR_PROPERY);
        if (dir == null && ctdProperty != null) {
            File testDir = new File(ctdProperty);
            if (testDir.exists() && testDir.isDirectory()) {
                log("Setting dir to " + testDir, Project.MSG_DEBUG);
                super.setDir(testDir);
            }
        }

        patchBrokenVMs();
        
        if (envSet != null) {
            EnvironmentVariable[] vars = envSet.getVariables(getProject());

            for (int i = 0; i < vars.length; i++) {
                addEnv(vars[i]);
            }
        }
        
        if (overrideFailOnError) {
            super.setFailonerror(true);
        }
        super.execute();
    }

    public int executeJava() throws BuildException {
        retVal = super.executeJava();
        return retVal;
    }

    /**
     * Adds a set of environment variables.
     * 
     * @param set
     *            the environment variable set.
     * @see org.codehaus.xharness.types.EnvSet
     */
    public void addEnvset(EnvSet set) {
        envSet = set;
    }

    public String getCommandline() {
        return getCommandLine().toString();
    }

    public void enableLogging(LineBuffer buffer, int outPrio, int errPrio) {
        if (redirector instanceof LoggingRedirector) {
            ((LoggingRedirector)redirector).enableLogging(buffer, outPrio, errPrio);
        }
    }

    public int getReturnValue() {
        return retVal;
    }
    
    /**
     * Creates a process watchdog that can be killed.
     * 
     * @see org.apache.tools.ant.taskdefs.ExecuteWatchdog
     * @return the ExecuteWatchdog
     */
    protected ExecuteWatchdog createWatchdog() {
        watchDog = new KillableExecuteWatchdog(procTimeout);
        return watchDog;
    }

    protected KillableExecuteWatchdog getWatchdog() {
        return watchDog;
    }

    private void patchBrokenVMs() {
        String osName = System.getProperties().getProperty("os.name");
        String jdkVer = System.getProperties().getProperty("java.specification.version");

        if ("HP-UX".equalsIgnoreCase(osName)) {
            log("Adding VM argument: -XdoCloseWithReadPending");
            createJvmarg().setValue("-XdoCloseWithReadPending");
        } else if ("AIX".equalsIgnoreCase(osName) && "1.4".equals(jdkVer)) {
            log("Adding System property: -Djava.net.preferIPv4Stack=true");
            Environment.Variable var = new Environment.Variable();
            var.setKey("java.net.preferIPv4Stack");
            var.setValue("true");
            addSysproperty(var);
        }
    }
}
