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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Sequential;

import org.codehaus.xharness.procutil.BgProcess;
import org.codehaus.xharness.procutil.ProcessRegistry;

/**
 * @author Gregor Heine
 */
public class XhJavaBgTask extends XhJavaTask implements Runnable, BgProcess {
    private Object mutex = new Object();
    private BuildException processException = null;
    private String processName = null;
    private long postKillTimeout = 10;
    private long preKillTimeout = 0;
    private boolean isRunning = false;
    private Sequential afterwards;

    /**
     * Constructor.
     */
    public XhJavaBgTask() {
        super();
        setTimeout(new Long(2 * 60 * 60 * 1000)); // 2 hours default - should be
                                                  // long enough ;)
    }

    /**
     * Set the timeout for waiting until the process has been killed.
     * 
     * @param timeout Kill timeout in seconds
     */
    public void setKilltimeout(int timeout) {
        if (timeout >= 0) {
            postKillTimeout = (long)timeout;
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
            preKillTimeout = (long)timeout;

        }
    }

    public String getProcessName() {
        return processName;
    }
    
    public void setProcessName(String pname) {
        processName = pname;
    }
    
    public void addAfterwards(Sequential tasks) {
        afterwards = tasks;
    }

    /**
     * Test, if the process is running.
     * 
     * @return true, if the process is running, otherwise false.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Kill the process if it is running. If the process can not be killed or
     * the excecution failed previously with an exception, a BuildException is
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
                    if (preKillTimeout > 0) {
                        try {
                            log("Waiting for Process to die before kill...",
                                    Project.MSG_VERBOSE);
                            mutex.wait(preKillTimeout * 1000);
                        } catch (Exception e) {
                            //ignore
                        }
                        getWatchdog().killProcess(false);
                    } else {
                        getWatchdog().killProcess(true);
                    }
                    int count = 0;
                    
                    while (isRunning && count++ < postKillTimeout) {
                        log("Waiting for Process to die after kill...",
                                Project.MSG_VERBOSE);
                        try {
                            mutex.wait(1000);
                        } catch (Exception e) {
                            //ignore
                        }
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
        Thread asyncThread = new Thread(this);
        asyncThread.setDaemon(true);

        synchronized (mutex) {
            isRunning = true;
            try {
                asyncThread.start();
            } catch (OutOfMemoryError oome) {
                Runtime rt = Runtime.getRuntime();
                log("Fatal error: unable to create new Thread: " + oome + "\n"
                        + "Trying recovery....\n" + "Before: total="
                        + rt.totalMemory() + ",free=" + rt.freeMemory()
                        + ",threads=" + Thread.activeCount(), Project.MSG_ERR);

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

            if (processException != null) {
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
        BuildException exc = null;

        try {
            super.execute();
        } catch (BuildException e) {
            exc = e;
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

            if (processException == null && exc != null
                    && !getWatchdog().killedDeliberately()) {
                processException = exc;
            }
        }
    }

}
