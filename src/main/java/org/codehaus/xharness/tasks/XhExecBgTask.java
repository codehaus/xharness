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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.types.Commandline;

import org.codehaus.xharness.procutil.BgProcess;
import org.codehaus.xharness.procutil.ProcessRegistry;
import org.codehaus.xharness.procutil.ScriptLauncher;

/**
 * @author Gregor Heine
 */
public class XhExecBgTask extends XhExecTask implements Runnable, BgProcess {
    // 2 hours default timeout - should be long enough ;)
    private static long defaultTimeout = 2 * 60 * 60 * 1000;

    private Object mutex = new Object();
    private BuildException processException = null;
    private String processName = null;
    private int postKillTimeout = 20;
    private int preKillTimeout = 0;
    private boolean isRunning = false;
    private Sequential afterwards;
    private File launcherArgsFile = null;
    private String launchedCommandLine = null;
    private ScriptLauncher launcher = null;

    /**
     * Constructor.
     */
    public XhExecBgTask() {
        super();
        setTimeout(new Long(defaultTimeout));
    }

    /**
     * Set the timeout for waiting until the process has been killed.
     * 
     * @param timeout
     *            kill timeout in seconds
     */
    public void setKilltimeout(int timeout) {
        if (timeout > 0) {
            postKillTimeout = timeout;
        } else {
            // must wait at least 1sec. for process to terminate
            postKillTimeout = 1;
        }
    }

    /**
     * Set the timeout to kill a process if it has not been terminated
     * gracefully.
     * 
     * @param timeout
     *            kill timeout in seconds
     */
    public void setPrekilltimeout(int timeout) {
        if (timeout >= 0) {
            preKillTimeout = timeout;

        }
    }

    public String getProcessName() {
        return processName;
    }
    
    public void setProcessName(String pname) {
        processName = pname;
    }
    
    public void setUseLauncher(boolean useLauncher) {
        if (useLauncher) {
            setSearchPath(true);
            launcher = new ScriptLauncher(this);
        }
    }

    public String getCommandline() {
        String ret = super.getCommandline();
        if (launchedCommandLine != null) {
            ret +=  " [" + launchedCommandLine + "]";
        }
        return ret;
    }

    public void addAfterwards(Sequential tasks) {
        afterwards = tasks;
    }

    /**
     * Test, if the process is running.
     * 
     * @return true, iff the process is running, otherwise false.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Kill the process if it is running. If the process can not be killed or
     * the execution failed previously with an exception, a BuildException is
     * thrown, containing the information about the cause. The
     * mutex.wait(prekillTimeout) will wait for the specified period for the
     * thread to die. If it has not terminated gracefully before that time it
     * will be killed.
     * 
     * @throws BuildException If an error occurrs while killing the process.
     */
    public void kill() throws BuildException {
        synchronized (mutex) {
            try {
                if (isRunning && getWatchdog() != null) {
                    boolean deliberateKill = true;
                    if (preKillTimeout > 0) {
                        waitForTermination(preKillTimeout, 
                                           "Waiting for Process to die before kill...");
                        deliberateKill = false;
                    }
                    
                    if (launcher != null) {
                        launcher.shutdown();
                        try {
                            mutex.wait(2000); // wait for the launcher to die
                        } catch (Exception e) {
                            // ignore
                        }
                        if (launcherArgsFile != null && launcherArgsFile.exists()) {
                            launcherArgsFile.delete();
                        }
                    }
                    
                    if (isRunning) {
                        getWatchdog().killProcess(deliberateKill);
                        waitForTermination(postKillTimeout, 
                                           "Waiting for Process to die after kill...");
                    }
                    if (isRunning) {
                        log("Process is still running!", Project.MSG_WARN);
                        throw new BuildException("Unable to kill process!");
                    }
                }
            } finally {
                if (processName != null) {
                    try {
                        ProcessRegistry.unregisterProcess(processName);
                    } catch (BuildException be) {
                        // ignore
                    }
                }
            }
            if (processException != null) {
                log(processException.toString(), Project.MSG_ERR);
                throw processException;
            }
        }
        if (afterwards != null) {
            try {
                afterwards.execute();
            } catch (BuildException be) {
                log("Nested task failed: " + be, Project.MSG_WARN);
                throw new BuildException("Nested task failed.");
            }
        }
    }

    /**
     * Do the execution of this Task.
     * 
     * @throws BuildException If an error occurs while executing this task.
     */
    public void execute() throws BuildException {
        log("Running " + getTaskName() + " in background", Project.MSG_VERBOSE);

        if (launcher != null) {
            launcher.start();
        }

        Thread asyncThread = new Thread(this);
        asyncThread.setDaemon(true);

        synchronized (mutex) {
            isRunning = true;
            try {
                asyncThread.start();
            } catch (OutOfMemoryError oome) {
                Runtime rt = Runtime.getRuntime();
                ThreadGroup tg = Thread.currentThread().getThreadGroup();

                log("Fatal error: unable to create new Thread: " + oome + "\n"
                        + "Trying recovery....\n" + "Before: total="
                        + rt.totalMemory() + ",free=" + rt.freeMemory()
                        + ",threads=" + tg.activeCount(), Project.MSG_ERR);

                isRunning = false;
                throw oome;
            }

            // wait 1 second to start process
            //
            try {
                mutex.wait(1000);
            } catch (Exception e) {
                //ignore
            }

            if (launcher != null) {
                try {
                    launcher.join();
                } catch (InterruptedException ie) {
                    // ignore
                }
                BuildException launcherEx = launcher.getException();
                if (launcherEx != null) {
                    throw launcherEx;
                }
            }

            if (processException != null) {
                log("Failed to execute process " + getCommandline()
                    + "\n" + processException.toString(), 
                    Project.MSG_WARN);
                throw processException;
            } else if (processName != null) {
                ProcessRegistry.registerProcess(processName, this);
            }
        }
    }

    /**
     * Runnable implementation. Executes the process in a separate Thread.
     */
    public void run() {
        try {
            super.execute();
        } catch (BuildException ex) {
            if (getWatchdog() == null || !getWatchdog().killedDeliberately()) {
                processException = ex;
            }
        }

        synchronized (mutex) {
            isRunning = false;
            try {
                mutex.notify();
            } catch (Exception e) {
                //ignore
            }
            
            if (processName != null) {
                try {
                    ProcessRegistry.unregisterProcess(processName);
                } catch (BuildException be) {
                    // ignore
                }
            }
        }
    }

    /**
     * Setup the executable name and the directory to run the process in.
     * Prepend the path to the executable if required. Make sure the
     * process control perl script is the executable name.
     */
    protected void setupExecutableAndDir() {
        super.setupExecutableAndDir();
        if (launcher == null) {
            return;
        }

        StringBuffer buffer = new StringBuffer();
        try {
            launcherArgsFile = File.createTempFile("xharness", ".txt");
            launcherArgsFile.deleteOnExit();
            OutputStream os = new BufferedOutputStream(new FileOutputStream(launcherArgsFile));
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, "US-ASCII"));
            pw.println(defaultTimeout / 1000);
            pw.println(launcher.getPort());
            pw.println(getExecutable());
            buffer.append(getExecutable());
            Iterator i = getArguments().iterator();
            while (i.hasNext()) {
                Object obj = i.next();
                String[] parts = null;
                if (obj instanceof Commandline.Argument) {
                    Commandline.Argument arg = (Commandline.Argument)obj;
                    parts = arg.getParts();
                } else if (obj instanceof Commandline) {
                    Commandline cmd = (Commandline)obj;
                    parts = cmd.getArguments();
                }
                if (parts != null) {
                    for (int j = 0; j < parts.length; j++) {
                        pw.println(parts[j]);
                        buffer.append(" ");
                        buffer.append(parts[j]);
                    }
                }
            }
            getArguments().clear();
            pw.close();
            os.close();
        } catch (Exception ex) {
            log("***** unable to create arguments file: " + ex, Project.MSG_ERR);
            throw new BuildException("Unableto create laucher arguments file "
                                   + launcherArgsFile + ": " + ex);
        }

        launchedCommandLine = buffer.toString();
        Commandline.Argument arg1 = new Commandline.Argument();
        getArguments().add(0, arg1);
        arg1.setValue(launcherArgsFile.getAbsolutePath());
        String launcherName = launcher.execName();
        log("Setting executable to " + launcherName, Project.MSG_VERBOSE);
        setExecutable(launcherName);
        setResolveExecutable(true);
    }
    
    private void waitForTermination(int maxSecs, String logMsg) {
        int count = 0;
        while (isRunning && count++ < maxSecs) {
            log(logMsg, Project.MSG_VERBOSE);
            try {
                mutex.wait(1000);
            } catch (Exception e) {
                //ignore
            }
        }
        
    }
}
