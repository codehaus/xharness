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

import org.apache.tools.ant.BuildException;

/**
 * Interface implemented by Tasks that can run asynchronously in the background 
 * (i.e. a separate Thread).
 * 
 * @author Gregor Heine
 */
public interface BgProcess {
    boolean isRunning();
    void kill() throws BuildException;
    void setKilltimeout(int timeout);
    void setPrekilltimeout(int timeout);
    String getProcessName();
    void setProcessName(String pname);
}
