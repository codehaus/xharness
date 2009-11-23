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
package org.codehaus.xharness.types;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.codehaus.xharness.log.LineBuffer;

public class TestProject extends Project {
    private LineBuffer buffer = new LineBuffer();
    
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
    
    LineBuffer getBuffer() {
        return buffer;
    }
}
