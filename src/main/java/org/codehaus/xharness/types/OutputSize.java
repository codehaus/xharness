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

package org.codehaus.xharness.types;

import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.codehaus.xharness.log.LogLine;

public class OutputSize extends AbstractOutput {
    private String mode = "line";
    private int equals = -1;
    private int larger = -1;
    private int smaller = -1;
    
    public void setMode(String mod) {
        mode = mod;
    }
    
    public void setEquals(int value) {
        equals = value;
    }
    
    public void setLarger(int value) {
        larger = value;
    }
    
    public void setSmaller(int value) {
        smaller = value;
    }
    
    public boolean eval() throws BuildException {
        if (equals >= 0 && (larger >= 0 || smaller >= 0) || larger >= 0 && smaller >= 0) {
            throw new BuildException("Can only set one of: equals, larger, smaller");
        }
        if (equals < 0 && larger < 0 && smaller < 0) {
            throw new BuildException("Must set one of: equals, larger, smaller");
        }
        boolean lineMode;
        if (mode.toLowerCase().startsWith("line")) {
            lineMode = true;
        } else if (mode.toLowerCase().startsWith("char")) {
            lineMode = false;
        } else {
            throw new BuildException("Invalid mode attribute: " + mode);
        }
        
        int numLines = 0;
        int numChars = 0;
        
        Iterator iter = getOutputIterator();
        while (iter.hasNext()) {
            LogLine line = (LogLine)iter.next();
            numLines++;
            numChars += line.getText().length();
        }
        
        log(logPrefix() + "size is "
            + (lineMode ? (numLines + " Lines.") : (numChars + " Characters.")), 
            Project.MSG_VERBOSE);

        if (equals >= 0) {
            return lineMode ? numLines == equals : numChars == equals;
        } else if (larger >= 0) {
            return lineMode ? numLines > larger : numChars > larger;
        } else {
            return lineMode ? numLines < smaller : numChars < smaller;
        }
    }

}
