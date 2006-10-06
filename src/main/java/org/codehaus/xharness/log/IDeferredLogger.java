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

/**
 * Interface implemented by loggers that do not stop logging after the logger's
 * Task has completed. Used to log e.g. background processes or services.
 * 
 * @author Gregor Heine
 */
public interface IDeferredLogger {
    /**
     * Shut down the logger. May perform necessary clean-up work, like killing
     * the process of a background task, etc. and publish the logger's result. 
     */
    void deferredShutdown();
}
