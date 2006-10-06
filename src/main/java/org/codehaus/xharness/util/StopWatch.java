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

package org.codehaus.xharness.util;

public class StopWatch {
    private boolean running;
    private long startTime;
    private long cumulativeTime;
    
    public StopWatch(boolean startNow) {
        if (startNow) {
            start();
        }
    }
    
    public boolean start() {
        if (!running) {
            running = true;
            startTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }
    
    public boolean stop() {
        if (running) {
            cumulativeTime += System.currentTimeMillis() - startTime;
            running = false;
            return true;
        }
        return false;
    }

    public long getTime() {
        return cumulativeTime;
    }
}
