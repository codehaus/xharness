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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;

import org.codehaus.xharness.exceptions.FatalException;

public class OutputRegex extends AbstractRepeateableOutput {
    private boolean docMode = false;
    
    /**
     * Enable doc mode. In doc mode, the task/process output is treated as a multi-line 
     * document, allowing the regular expression to contain line breaks (\n).
     * If disabled (default) the regular expression is applied to each line of the output 
     * separately, allowing the use of start-of-line and end-of-line patterns (^ and $).
     * By default doc mode is disabled.
     *
     * @param mode true to enable doc mode, false to disable doc mode
     */
    public void setDocMode(boolean mode) {
        docMode = mode;
    }
    
    /**
     * Evaluate this output condition.
     * 
     * @return true if the expected output is found, false otherwise
     * @exception BuildException if an error occurs
     */
    public boolean eval() throws BuildException {
        if (getText() == null) {
            throw new FatalException("Missing regular expression");
        } 
        final Pattern pattern = Pattern.compile(getText());
        Searcher searcher;
        if (docMode) {
            final Matcher matcher = pattern.matcher(
                    getLineBuffer().toString('\n', "", getStreamPrio(), isIgnoreANSI()));
            searcher = new Searcher() {
                public boolean findAgain() {
                    return matcher.matches();
                }
            };
        } else {
            searcher = new LineBufferSearcher(getOutputIterator()) {
                public int indexIn(String text) {
                    Matcher matcher = pattern.matcher(text);
                    return matcher.find() ? matcher.end() : -1;
                }
            };
        }
        return super.eval(searcher);
    }
    
    protected void logStartSearch() {
        logEvalResult("searching for regex pattern \"" + getText() + "\"");
    }
}
