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
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;

import org.codehaus.xharness.exceptions.AssertionWarningException;
import org.codehaus.xharness.exceptions.FatalException;

/**
 * Performs a repeated check on an ant condition.
 */
public class AssertTask extends ConditionBase {
    private boolean erroronfail = false;
    private int timeout = 0;
    private String message = "Assertion failed";
    private Task nestedTask;
    
    /**
     * Add a nested task to this TaskContainer.
     *
     * @param task  Nested task to execute sequentially
     * @throws FatalException If there is more than 1 nested task.
     */
    public void add(Task task) throws FatalException {
        if (task != null) {
            if (nestedTask != null) {
                throw new FatalException("Only one nested task is suppoted.");
            }
            nestedTask = task;
        }
    }

    /**
     * Define a timeout how long to wait for the expected condition to be true.
     *
     * @param to The timeout in seconds.
     */
    public void setTimeout(int to) {
        if (to >= 0) {
            timeout = to;
        }
    }

    public void setErroronfail(boolean err) {
        erroronfail = err;
    }
    
    public void setMessage(String msg) {
        message = msg;
    }

    /**
     * Execute this Assert Task.
     *
     * @throws FatalException If the number of nested conditions is not exactly 1.
     * @throws  AssertionWarningException If the expected condition was not true, 
     *         after a specified timeout (and erroronfail=false)
     * @throws BuildException if the nested task fails or if the expected condition was not true
               and erroronfail=true
     */
    public void execute() throws BuildException {
        if (countConditions() > 1) {
            throw new FatalException("You must not nest more than one condition into <assert>");
        }
        if (countConditions() < 1) {
            throw new FatalException("You must nest a condition into <assert>");
        }

        int to = timeout;

        do {
            if (nestedTask != null) {
                log("Executing nested task " + nestedTask.getTaskName(), Project.MSG_VERBOSE);
                nestedTask.perform();
            }
            boolean eval = ((Condition)getConditions().nextElement()).eval();
            if (eval) {
                log("Condition true; Assertion passed.", Project.MSG_VERBOSE);
                break;
            } else if ((to--) > 0) {
                log(message + "\nRetrying for another " + to + " seconds",
                        Project.MSG_VERBOSE);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
            } else {
                String msg = message;
                if (timeout > 0) {
                    msg += ", after " + timeout + " seconds";
                }
                log(msg, Project.MSG_ERR);
                if (erroronfail) {
                    throw new BuildException(message);
                }
                throw new AssertionWarningException(message);
            }
        } while (true);
    }
}
