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

import java.util.Hashtable;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.EnumeratedAttribute;

import org.codehaus.xharness.log.LogPriority;
import org.codehaus.xharness.log.TaskLogger;
import org.codehaus.xharness.log.TaskRegistry;

public abstract class AbstractOutput extends ProjectComponent implements Condition {
    private Stream stream = null;
    private TaskLogger taskLogger = null;
    private String taskName;
    
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
    
    protected Iterator getOutputIterator() {
        return getTaskLogger().getLineBuffer().iterator(getStream().getPriority());
    }
    
    protected final String logPrefix()  {
        return "Task @" + getTaskLogger().getFullName() 
             + "@ output (" + getStream().getValue() + ") ";
    }
    
    private Stream getStream() {
        if (stream == null) {
            stream = new Stream();
            stream.setValue(stream.getValues()[0]);
        }
        return stream;
    }
    
    private TaskLogger getTaskLogger() {
        if (taskLogger == null) {
            taskLogger = TaskRegistry.getLogger(taskName); 
            if (taskLogger == null) {
                throw new BuildException("Task \"" + taskName + "\" not found!");
            }
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

        private static Hashtable streamTable = new Hashtable();
        
        static {
            streamTable.put(STDOUT,  new Integer(LogPriority.STDOUT));
            streamTable.put(STDERR,  new Integer(LogPriority.STDERR));
            streamTable.put(ERROR,   new Integer(LogPriority.ERROR));
            streamTable.put(WARNING, new Integer(LogPriority.WARNING));
            streamTable.put(INFO,    new Integer(LogPriority.INFO));
            streamTable.put(VERBOSE, new Integer(LogPriority.VERBOSE));
            streamTable.put(DEBUG,   new Integer(LogPriority.DEBUG));
        }

        public int getPriority() {
            String key = getValue().toLowerCase();
            Integer i = (Integer)streamTable.get(key);
            return i.intValue();
        }

        public String[] getValues() {
            return UNITS;
        }
    }
}
