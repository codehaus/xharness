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

package org.codehaus.xharness.procutil;

import org.apache.tools.ant.taskdefs.ExecuteWatchdog;

/**
 * Derivate of {@link org.apache.tools.ant.taskdefs.ExecuteWatchdog} that
 * allows killing the process before the timeout has been expired, by
 * manually triggering a timeout. It also gives a handle to the processes
 * {@link java.lang.Process} instance.
 *
 * @author  Gregor Heine
 */
public class KillableExecuteWatchdog extends ExecuteWatchdog {
    private boolean deliberatelyKilled = false;

    /**
     * Constructor. Only calls into the super class.
     * 
     * @param to Timeout for the watchdog.
     */
    public KillableExecuteWatchdog(long to) {
        super(to);
    }

    /**
     * Return true, iff the process has been killed deliberately via calling
     * killProcess() and not due to a timeout.
     * 
     * @return true, if the process was killed deliberately, otherwise false.
     */
    public boolean killedDeliberately() {
        return deliberatelyKilled;
    }

    /**
     * Kills the process, by simulating an expired timeout in the super class.
     * 
     * @param deliberateKill If true, the process was killed deliberately.
     */
    public void killProcess(boolean deliberateKill) {
        deliberatelyKilled = deliberateKill;
        super.timeoutOccured(null);
    }
}
