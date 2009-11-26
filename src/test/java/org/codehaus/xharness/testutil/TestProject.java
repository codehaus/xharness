/*
 * Copyright 2009 Progress Software
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
package org.codehaus.xharness.testutil;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.codehaus.xharness.log.LineBuffer;

public class TestProject extends Project {
    private LineBuffer buffer = new LineBuffer();
    private LogPrintStream stdOut;
    private LogPrintStream stdErr;
    private PrintStream stdOutBak;
    private PrintStream stdErrBak;
    
    /**
     * Ant 1.6.x log method override.
     */
    protected void fireMessageLogged(Project project, String message, int priority) {
        buffer.logLine(priority, message);
    }
    
    /**
     * Ant 1.7.x log method override.
     */
    protected void fireMessageLogged(Project project, String message,
                                     Throwable throwable, int priority) {
        buffer.logLine(priority, message);
    }
    
    /**
     * Ant 1.6.x log method override.
     */
    protected void fireMessageLogged(Target target, String message, int priority) {
        buffer.logLine(priority, message);
    }

    /**
     * Ant 1.7.x log method override.
     */
    protected void fireMessageLogged(Target target, String message,
                                     Throwable throwable, int priority) {
        buffer.logLine(priority, message);
    }
    
    /**
     * Ant 1.6.x log method override.
     */
    protected void fireMessageLogged(Task task, String message, int priority) {
        buffer.logLine(priority, message);
    }
    
    /**
     * Ant 1.7.x log method override.
     */
    protected void fireMessageLogged(Task task, String message, 
                                     Throwable throwable, int priority) {
        buffer.logLine(priority, message);
    }
    
    public LineBuffer getBuffer() {
        return buffer;
    }
    
    public synchronized void enableConsoleCapturing(int stdoutPrio, int stdErrPrio) {
        if (stdOutBak == null) {
            stdOut = new LogPrintStream(new ByteArrayOutputStream(), stdoutPrio);
            stdErr = new LogPrintStream(new ByteArrayOutputStream(), stdErrPrio);
            synchronized (System.out) {
                stdOutBak = System.out;
                System.setOut(stdOut);
            }
            synchronized (System.err) {
                stdErrBak = System.err;
                System.setErr(stdErr);
            }
        }
    }
 
    public synchronized void disableConsoleCapturing() {
        if (stdOutBak != null) {
            synchronized (System.out) {
                System.out.flush();
                System.setOut(stdOutBak);
            }
            synchronized (System.err) {
                System.err.flush();
                System.setErr(stdErrBak);
            }
            stdOut.flushToBuffer();
            stdErr.flushToBuffer();
            stdOut = null;
            stdErr = null;
            stdOutBak = null;
            stdErrBak = null;
        }
    }
    
    private class LogPrintStream extends PrintStream {
        private ByteArrayOutputStream baos;
        private int priority;
        
        public LogPrintStream(ByteArrayOutputStream os, int prio) {
            super(os);
            baos = os;
            priority = prio;
        }
        
        public void println() {
            synchronized (baos) {
                super.println();
                flushToBuffer();
            }
        }
        
        public void println(String x) {
            synchronized (baos) {
                super.println(x);
                flushToBuffer();
            }
        }
        
        public void println(Object x) {
            synchronized (baos) {
                super.println(x);
                flushToBuffer();
            }
        }
        
        public void flushToBuffer() {
            buffer.logLine(priority, baos.toString());
            baos.reset();
        }
    }
}
