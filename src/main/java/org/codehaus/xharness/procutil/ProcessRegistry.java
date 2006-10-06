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

import java.util.HashMap;

import org.apache.tools.ant.BuildException;

/**
 * @author Gregor Heine
 */
public final class ProcessRegistry {
    private static HashMap smMap = new HashMap();
    
    private ProcessRegistry() {
        // static class. No instantiation allowed.
    }
    
    public static synchronized void registerProcess(String name, BgProcess proc) 
        throws BuildException {
        if (smMap.get(name) != null) {
            throw new BuildException("Process " + name + " already registered.");
        }
        smMap.put(name, proc);
    }
    
    public static synchronized BgProcess getProcess(String name) 
        throws BuildException {
        BgProcess proc = (BgProcess)smMap.get(name);
        if (proc == null) {
            throw new BuildException("Process " + name + " not registered.");
        }
        return proc;
    }
    
    public static synchronized void unregisterProcess(String name) 
        throws BuildException {
        if (smMap.remove(name) == null) {
            throw new BuildException("Process " + name + " not registered.");
        }
    }
    
    public static synchronized void reset() {
        smMap.clear();
    }
}
