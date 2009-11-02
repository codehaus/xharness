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
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.types.Commandline;

import org.codehaus.xharness.exceptions.FatalException;
import org.codehaus.xharness.log.LineBuffer;
import org.codehaus.xharness.log.LoggingRedirector;
import org.codehaus.xharness.log.TaskRegistry;
import org.codehaus.xharness.procutil.KillableExecuteWatchdog;
import org.codehaus.xharness.procutil.LoggableProcess;
import org.codehaus.xharness.types.EnvSet;
import org.codehaus.xharness.types.EnvironmentVariable;

/**
 * Extension of the standard ant Exec task that adds the following:
 * - support for nested EnvSet types.
 * - output capturing (stdout/stderr). 
 * - Watchdog that allows process killing.
 * - Intelligent executable and dir setting depneding on current.test.directory property.
 * 
 * @author Gregor Heine
 * @see org.apache.tools.ant.taskdefs.ExecTask
 */
public class XhExecTask extends ExecTask implements LoggableProcess {
    private static final boolean ON_WINDOWS = 
        System.getProperty("os.name", "").toLowerCase().startsWith("win");
    private KillableExecuteWatchdog watchDog;
    private EnvSet envSet = null;
    private List arguments = new LinkedList();
    private File dir;
    private long procTimeout = 60 * 60 * 1000; // 1 hour
    private int retVal = 0;

    public XhExecTask() {
        this.redirector = new LoggingRedirector(this);
        setFailonerror(true);
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

    /**
     * Adds a command-line argument.
     *
     * @return  the command line argument
     * @see org.apache.tools.ant.taskdefs.ExecTask#createArg()
     */
    public Commandline.Argument createArg() {
        Commandline.Argument argument = new Commandline.Argument();
        getArguments().add(argument);
        return argument;
    }
    
    public List getArguments() {
        return arguments;
    }

    public void execute() throws BuildException {
        setupExecutableAndDir();

        for (Iterator iter = getArguments().iterator(); iter.hasNext();) {
            Commandline.Argument arg = (Commandline.Argument)iter.next();
            cmdl.addArguments(arg.getParts());
        }
        
        if (envSet != null) {
            EnvironmentVariable[] vars = envSet.getVariables(getProject());

            for (int i = 0; i < vars.length; i++) {
                addEnv(vars[i]);
            }
        }
        
        super.execute();
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

    public String getExecutable() {
        return cmdl.getExecutable();
    }

    public String getCommandline() {
        return cmdl.toString();
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
     * Override of
     * {@link org.apache.tools.ant.taskdefs.ExecTask#maybeSetResultPropertyValue(int)}.
     * Captures the return value for later processing.
     * 
     * @param result the return value of the process
     */
    protected void maybeSetResultPropertyValue(int result) {
        super.maybeSetResultPropertyValue(result);
        retVal = result;
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
    
    /**
     * Setup the executable name and the directory to run the process in.
     * Prepend the path to the executable if required.
     */
    protected void setupExecutableAndDir() {
        String executableName = getExecutable();
        if (executableName == null) {
            throw new FatalException("executable not set!");
        }

        String ctdProperty = getProject().getProperty(TaskRegistry.CURRENT_TEST_DIR_PROPERY);
        if (ctdProperty != null) {
            File testDir = new File(ctdProperty);
            File executable = null;
            try {
                executable = new File(testDir, executableName).getCanonicalFile();
            } catch (IOException ioe) {
                executable = new File(testDir, executableName);
            }
            if (!isAbsoluteFilename(executableName) && executable.exists() && executable.isFile()) {
                executableName = executable.getAbsolutePath();
                log("Setting executable to " + executableName, Project.MSG_DEBUG);
                setExecutable(executableName);
                if (dir == null) {
                    dir = executable.getParentFile();
                    log("Setting dir to " + dir, Project.MSG_DEBUG);
                    super.setDir(dir);
                }
            }
            if (dir == null && testDir.exists() && testDir.isDirectory()) {
                log("Setting dir to " + testDir, Project.MSG_DEBUG);
                super.setDir(testDir);
            }
        }
    }

    private boolean isAbsoluteFilename(String filename) {
        // The following test is for a windows executable and checks whether or not
        // the filename begins with a drive-letter, followed
        // by colon, followed by (back)slash e.g. C:\
        //
        if (ON_WINDOWS && filename.length() > 2 
            && Character.isLetter(filename.charAt(0))
            && filename.charAt(1) == ':' 
            && (filename.charAt(2) == '\\' || filename.charAt(2) == '/')) {
            return true;
        } else if (filename.length() > 0 
                   && (filename.charAt(0) == '\\' || filename.charAt(0) == '/')) {
            return true;
        }
        return false;
    }
}
