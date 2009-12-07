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

import org.codehaus.xharness.exceptions.FatalException;
import org.codehaus.xharness.log.LogLine;

public class OutputSize extends AbstractOutput {
    private String mode = "line";
    private int equals = -1;
    private int larger = -1;
    private int smaller = -1;
    
    /**
     * Set the size mode. Allowed values are "line" to count the number of lines in the output
     * and "char" to count the number of characters in the output. In "char" mode, line breaks
     * (cr/lf) are ignored.
     *
     * @param mod the size mode
     */
    public void setMode(String mod) {
        mode = mod;
    }
    
    /**
     * Match exactly the number of lines/characters as specified in the parameter.
     * The condition will pass if the output contains the exact number of lines/characters
     * (depending on the size mode), otherwise it will fail.  
     *
     * @param value the exact number of lines to expect
     */
    public void setEquals(int value) {
        equals = value;
    }
    
    /**
     * Match at least one more number of lines/characters than specified in the parameter.
     * The condition will pass if the output contains more lines/characters
     * (depending on the size mode) than specified, otherwise it will fail.  
     *
     * @param value the minimum number of lines to expect minus 1
     */
    public void setLarger(int value) {
        larger = value;
    }
    
    /**
     * Match less number of lines/characters than specified in the parameter.
     * The condition will pass if the output contains less lines/characters
     * (depending on the size mode) than specified, otherwise it will fail.  
     *
     * @param value the maximum number of lines to expect plus 1
     */
    public void setSmaller(int value) {
        smaller = value;
    }
    
    /**
     * Evaluate this output condition.
     * 
     * @return true if the expected output is found, false otherwise
     * @exception BuildException if an error occurs
     */
    public boolean eval() throws BuildException {
        if (equals >= 0 && (larger >= 0 || smaller >= 0) || larger >= 0 && smaller >= 0) {
            throw new FatalException("Can only set one of: equals, larger, smaller");
        }
        if (equals < 0 && larger < 0 && smaller < 0) {
            throw new FatalException("Must set one of: equals, larger, smaller");
        }
        boolean lineMode;
        if (mode.toLowerCase().startsWith("line")) {
            lineMode = true;
        } else if (mode.toLowerCase().startsWith("char")) {
            lineMode = false;
        } else {
            throw new FatalException("Invalid mode attribute: " + mode);
        }
        
        int numLines = 0;
        int numChars = 0;
        
        for (Iterator iter = getOutputIterator(); iter.hasNext();) {
            LogLine line = (LogLine)iter.next();
            numLines++;
            numChars += line.getText().length();
        }
        
        final int expectVal;
        final int isVal = lineMode ? numLines : numChars;
        final String unit = lineMode ? "line" : "character";
        final boolean result;

        StringBuffer buf = new StringBuffer();
        buf.append("expected ");
        if (equals >= 0) {
            buf.append("exactly ");
            expectVal = equals;
            result = (isVal == expectVal);
        } else if (larger >= 0) {
            buf.append("more than ");
            expectVal = larger;
            result = (isVal > expectVal);
        } else {
            buf.append("less than ");
            expectVal = smaller;
            result = (isVal < expectVal);
        }
        buf.append(expectVal);
        buf.append(" ");
        buf.append(unit);
        buf.append(expectVal == 1 ? ", found " : "s, found ");
        buf.append(isVal);
        buf.append(" ");
        buf.append(unit);
        buf.append(isVal == 1 ? "." : "s.");
        logEvalResult(buf.toString());
        
        return result;
    }

}
