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
import org.apache.tools.ant.Task;

/**
 * BuildListener, that logs the output of a Service's start group. 
 * 
 * @author Gregor Heine
 */
public class SvcsStartLogger extends TestLogger implements IDeferredLogger {
    /**
     * Constructs a SvcsStartLogger.
     * @param registry The XHarness TaskRegistry
     * @param task The Task for this TestLogger.
     * @param name The name for this TestLogger.
     * @param parent The parent of this TestLogger. Must no be <code>null</code>.
     * @param reference An optional reference String denoting a fully quailified name of
     *                  another logger or <code>null</code>.
     */
    public SvcsStartLogger(TaskRegistry registry, 
                           Task task, 
                           String name, 
                           TestLogger parent,
                           String reference) {
        super(registry, task, name, parent, parent.getFullName(), reference);
    }

    /**
     * Called when the start group has finished. Because the child elements
     * may include background processes that are only shut down when the service
     * is shut down, the start group does not publish it's result until  
     * {@link #deferredShutdown()} is called.
     */
    protected void taskFinishedInternal() {
        deactivate(true);
        stopWatch.stop();
    }
    
    /**
     * Shuts down all deferred child of the start group and pubished the group's result.
     * 
     * @throws BuildException If an error occurs while shutting down any of 
     *                        the deferred children.
     */
    public void deferredShutdown() throws BuildException {
        super.taskFinishedInternal();
        Throwable taskFailure = getFailure();
        if (taskFailure != null) {
            if (taskFailure instanceof BuildException) {
                throw (BuildException)taskFailure;
            }
            throw new BuildException(taskFailure);
        }
    }
}
