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

import org.apache.tools.ant.Task;

/**
 * Mock-logger, that does nothing but create a link between one logger and another one.
 * Used to link from a Service start/verify/stop Task to the ServiceDef Task.
 * 
 * @author Gregor Heine
 */
public class LinkLogger extends TaskLogger {
    public LinkLogger(TaskRegistry reg, Task task, String name, String parent, String reference) {
        super(reg, task, name, parent, reference);
    }

    /**
     * Get the type of this logger.
     * 
     * @return Return the link result type Id.
     */
    protected int getTaskType() {
        return Result.LINK;
    }
    
    /**
     * Called when the logger's Task logs an event message. Does nothing.
     * 
     * @param eventPrio The event priority.
     * @param message The event message.
     */
    protected void messageLoggedInternal(int eventPrio, String message) {
    }
}
