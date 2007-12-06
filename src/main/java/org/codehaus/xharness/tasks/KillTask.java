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

import org.codehaus.xharness.procutil.BgProcess;
import org.codehaus.xharness.procutil.ProcessRegistry;

public class KillTask extends Task {
    private String procName;
    private boolean failonerror = true;

    // The method executing the task
    public void execute() throws BuildException {
        if (procName == null) {
            throw new BuildException(
                    "You must provide a process name in order to use the KillProcess task");
        }

        try {
            BgProcess proc = ProcessRegistry.getProcess(procName);
            log("KillProcess: killing processname " + procName, Project.MSG_VERBOSE);
            proc.kill();
        } catch (BuildException b) {
            if (failonerror) {
                throw b;
            }
        }
    }

    // The setter for the "processname" attribute
    public void setProcessname(String pn) {
        procName = pn;
    }

    // The setter for the "failonerror" attribute
    public void setFailonerror(boolean fe) {
        failonerror = fe;
    }

}
