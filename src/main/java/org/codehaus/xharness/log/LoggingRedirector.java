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

import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.Redirector;


/**
 * 
 * 
 * @author Gregor Heine
 */
public class LoggingRedirector extends Redirector {
    private LineBuffer buffer = null;
    private int stdOutPriority;
    private int stdErrPriority;
    
    /**
     * Create a LoggingRedirector instance for the given task.
     *
     * @param managingTask the task for which the redirector is to work.
     */
    public LoggingRedirector(Task managingTask) {
        super(managingTask);
    }
    
    /**
     * Enable logging for this redirector. All messages logged to standard out and err
     * are recorded in the specified LineBuffer with the specified log priorities.
     * 
     * @param buf The LineBuffer that records the messages
     * @param outPrio Logging priority for standard out messages
     * @param errPrio Logging priority for standard err messages
     */
    public void enableLogging(LineBuffer buf, int outPrio, int errPrio) {
        buffer = buf;
        stdOutPriority = outPrio;
        stdErrPriority = errPrio;
    }
    
    /**
     * Pass output sent to System.out to specified output.
     *
     * @param output the data to be output
     */
    protected synchronized void handleOutput(String output) {
        super.handleOutput(output);
        if (buffer != null) {
            buffer.logLine(stdOutPriority, output);
        }
    }

    /**
     * Process data due to a flush operation.
     *
     * @param output the data being flushed.
     */
    protected synchronized void handleFlush(String output) {
        super.handleFlush(output);
        if (buffer != null) {
            buffer.logLine(stdOutPriority, output);
        }
    }

    /**
     * Handle process error output.
     *
     * @param output the error output data.
     */
    protected synchronized void handleErrorOutput(String output) {
        super.handleErrorOutput(output);
        if (buffer != null) {
            buffer.logLine(stdErrPriority, output);
        }
    }

    /**
     * Handle a flush operation on the error stream.
     *
     * @param output the error information being flushed.
     */
    protected synchronized void handleErrorFlush(String output) {
        super.handleErrorFlush(output);
        if (buffer != null) {
            buffer.logLine(stdErrPriority, output);
        }
    }

    /**
     * Override of {@link org.apache.tools.ant.taskdefs.ExecTask#createHandler()}.
     * Create a Stream handler that intercepts data written to stdout and stderr.
     * 
     * @return A LoggingStreamHandler if logging is enabled, otherwise the 
     *         ExecuteStreamHandler from the super implementation.
     * @throws BuildException If the stream handler cannot be created.
     */
    public synchronized ExecuteStreamHandler createHandler() throws BuildException {
        ExecuteStreamHandler esh = super.createHandler();
        if (buffer != null) {
            OutputStream out = new LogOutputStream(buffer, stdOutPriority);
            OutputStream err = new LogOutputStream(buffer, stdErrPriority);
            esh = new LoggingStreamHandler(esh, out, err);
        }
        return esh;
    }
}
