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

package org.codehaus.xharness.types;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.EnumeratedAttribute;

import org.codehaus.xharness.log.LineBuffer;
import org.codehaus.xharness.log.LogLine;
import org.codehaus.xharness.log.LogPriority;
import org.codehaus.xharness.log.TaskLogger;
import org.codehaus.xharness.log.TaskRegistry;

public abstract class AbstractOutput extends ProjectComponent implements Condition {
    private static LineBuffer lineBuffer;
    private Stream stream = null;
    private String taskName;
    private boolean ignoreANSI = false;

    /**
     * Set Ignore ANSI control characters in stream text.
     *
     * @param ignoreansi ignore ansi control characters.
     */
    public void setIgnoreANSI(boolean ignoreansi) {
        ignoreANSI = ignoreansi;
    }

    /**
     * Set the task to check.
     * 
     * @param task Name or identifier for the Task.
     */
    public void setTask(String task) {
        taskName = task;
    }
    
    /**
     * Set the stream to check.
     * 
     * @param strm The stream identifier.
     */
    public void setStream(Stream strm) {
        stream = strm;
    }
    
    protected boolean isIgnoreANSI() {
        return ignoreANSI;
    }
    
    protected LineBuffer getLineBuffer() {
        return lineBuffer == null ? getTaskLogger().getLineBuffer() : lineBuffer;
    }
    
    protected int getStreamPrio() {
        return lineBuffer == null ? getStream().getPriority() : lineBuffer.getDefaultPriority();
    }

    protected Iterator getOutputIterator() {
        return getLineBuffer().iterator(getStreamPrio());
    }
    
    protected final void logEvalResult(String msg)  {
        LineBuffer subsection = lineBuffer;
        StringBuffer buf = new StringBuffer();
        buf.append("Task @@")
            .append(getTaskLogger().getFullName())
            .append("@@ output (")
            .append(getStream().getValue())
            .append(") ")
            .append(msg);
        if (subsection != null) {
            buf.append(" in subsection");
        }
        log(buf.toString(), Project.MSG_VERBOSE);
        if (subsection != null) {
            logSubsection(subsection);
        }
    }

    protected final void logSubsection(LineBuffer subsection) {
        log("+++ subsection contents in debug output +++", Project.MSG_VERBOSE);
        for (Iterator iter = subsection.iterator(); iter.hasNext();) {
            LogLine line = (LogLine)iter.next();
            log(line.getText(), Project.MSG_DEBUG);
        }
        log("+++ end of subsection contents +++", Project.MSG_VERBOSE);
    }
    
    static void setLineBuffer(LineBuffer buf) {
        lineBuffer = buf;
    }
    
    private Stream getStream() {
        if (stream == null) {
            int prio = getLineBuffer().getDefaultPriority();
            stream = Stream.getStream(prio);
        }
        return stream;
    }
    
    private TaskLogger getTaskLogger() {
        TaskLogger taskLogger = TaskRegistry.getLogger(taskName); 
        if (taskLogger == null) {
            String descr = taskName == null ? "" : "\"" + taskName + "\" ";
            throw new BuildException("Task " + descr + "not found!");
        }
        return taskLogger;
    }
    
    public static class Stream extends EnumeratedAttribute {
        private static final String STDOUT = "stdout";
        private static final String STDERR = "stderr";
        private static final String ERROR = "error";
        private static final String WARNING = "warning";
        private static final String INFO = "info";
        private static final String VERBOSE = "verbose";
        private static final String DEBUG = "debug";

        private static final String[] UNITS = {
            STDOUT, STDERR, ERROR, WARNING, INFO, VERBOSE, DEBUG
        };

        private static Map streamTable = new HashMap();
        
        static {
            streamTable.put(STDOUT,  Integer.valueOf(LogPriority.STDOUT));
            streamTable.put(STDERR,  Integer.valueOf(LogPriority.STDERR));
            streamTable.put(ERROR,   Integer.valueOf(LogPriority.ERROR));
            streamTable.put(WARNING, Integer.valueOf(LogPriority.WARNING));
            streamTable.put(INFO,    Integer.valueOf(LogPriority.INFO));
            streamTable.put(VERBOSE, Integer.valueOf(LogPriority.VERBOSE));
            streamTable.put(DEBUG,   Integer.valueOf(LogPriority.DEBUG));
        }
        
        public Stream() {
            super();
        }
        
        private Stream(String sreamName) {
            super();
            setValue(sreamName);
        }

        public int getPriority() {
            String key = getValue().toLowerCase();
            Integer i = (Integer)streamTable.get(key);
            return i.intValue();
        }

        public String[] getValues() {
            return UNITS;
        }

        public static Stream getStream(int priority) {
            Integer prioObj = Integer.valueOf(priority);
            for (Iterator iter = streamTable.entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry)iter.next();
                if (prioObj.equals(entry.getValue())) {
                    return new Stream((String)entry.getKey());
                }
            }
            return null;
        }
    }
}
