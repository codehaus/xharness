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

public class OutputIs extends AbstractOutput {
    private String text;
    
    /**
     * Add Text to be matched upon.
     *
     * @param txt The text to match.
     */
    public void addText(String txt) {
        if (text == null) {
            text = getProject().replaceProperties(txt);
        } else {
            throw new FatalException("Cannot use string argument and CDATA");
        }
    }

    /**
     * Add Text to be matched upon.
     *
     * @param txt The text to match.
     */
    public void setString(String txt) {
        if (text == null) {
            text = getProject().replaceProperties(txt);
        } else {
            throw new FatalException("Cannot use string argument and CDATA");
        }
    }

    /**
     * Evaluate this output condition.
     * 
     * @return true if the expected output is found, false otherwise
     * @exception BuildException if an error occurs
     */
    public boolean eval() throws BuildException {
        Iterator iter = getOutputIterator();
        if (getText() == null || "".equals(getText())) {
            boolean ret = !iter.hasNext();
            if (ret) {
                logEvalResult("empty");
            } else {
                logEvalResult("not empty");
            }
            return ret;
        } else if (iter.hasNext()) {
            LogLine line = (LogLine)iter.next();
            if (!iter.hasNext() && getText().equals(line.getText(isIgnoreANSI()))) {
                logEvalResult("is \"" + getText() + "\"");
                return true;
            }
        }
        logEvalResult("not \"" + getText() + "\"");
        return false;
    }
    
    protected final String getText() {
        return text;
    }
}
