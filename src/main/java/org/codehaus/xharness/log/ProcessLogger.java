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

package org.codehaus.xharness.log;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.codehaus.xharness.procutil.BgProcess;
import org.codehaus.xharness.procutil.LoggableProcess;

/**
 * BuildListener, that logs the output of an XHarness Process Task.
 * 
 * @author Gregor Heine
 */
public class ProcessLogger extends TaskLogger implements IDeferredLogger {
    private boolean processLoggingEnabled;
    
    ProcessLogger() {
        // for testing only!
    }
    
    /**
     * Constructs a ProcessLogger.
     * @param registry The XHarness TaskRegistry
     * @param task The Task for this TestLogger.
     * @param name The name for this TestLogger.
     * @param parentName The name of this logger's parent logger.
     */
    public ProcessLogger(TaskRegistry registry, Task task, String name, String parentName) {
        super(registry, task, name, parentName, null);
        processLoggingEnabled = false;
    }
    
    /**
     * Called when this logger's Task has logged a message. Since all output that
     * the logger's process writes to standard out and standard err is logged
     * twice, once by the process' LoggingRedirector and once through the ant
     * eventlog, this output needs to be "merged" in the logger's LineBuffer.
     * 
     * @param eventPrio The priority of the log event.
     * @param message The log message.
     */
    protected void messageLoggedInternal(int eventPrio, String message) {
        if (!processLoggingEnabled) {
            processLoggingEnabled = true;
            Task task = getTask();
            if (task instanceof LoggableProcess) {
                ((LoggableProcess)task).enableLogging(getLineBuffer(), 
                                                      LogPriority.STDOUT, 
                                                      LogPriority.STDERR);
            }
        }
        if (eventPrio == Project.MSG_WARN) {
            getLineBuffer().mergeLine(LogPriority.STDERR, 
                                      mapEventToLogPriority(eventPrio), 
                                      message);
        } else if (eventPrio == Project.MSG_INFO) {
            getLineBuffer().mergeLine(LogPriority.STDOUT, 
                                      mapEventToLogPriority(eventPrio), 
                                      message);
        } else {
            getLineBuffer().logLine(mapEventToLogPriority(eventPrio), message);
        }
    }
    
    /**
     * Called when this logger's Task has finished. Because the Task may be a
     * continue running in the background (XhExecBgTask/XhJavaBgTask), the
     * output is not flushed yet. This is done in {@link #deferredShutdown()}.
     */
    protected void taskFinishedInternal() {
        stopWatch.stop();
    }
    
    /**
     * Kills the logger's process task if it is still running and flushes the output
     * to the ResultFormatter.
     * 
     * @throws BuildException If the Task finished with an error or an error occurs 
     *                        while killing the process.
     */
    public void deferredShutdown() throws BuildException {
        Task task = getTask();
        try {
            if (task instanceof BgProcess) {
                ((BgProcess)task).kill();
            }
        } catch (BuildException err) {
            setFailure(err);
            throw err;
        } finally {
            super.taskFinishedInternal();
        }
    }
}
