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


import org.apache.tools.ant.BuildException;

public class OutputContains extends AbstractRepeateableOutput {

    /**
     * Evaluate this output condition.
     * 
     * @return true if the expected output is found, false otherwise
     * @exception BuildException if an error occurs
     */
    public boolean eval() throws BuildException {
        if (getText() == null || "".equals(getText())) {
            return true;
        } 
        Searcher searcher = new LineBufferSearcher(getOutputIterator()) {
            private int textLen = getText().length();
            public int indexIn(String text) {
                int index = text.indexOf(getText());
                return index >= 0 ? index + textLen : -1;
            }
        };
        return super.eval(searcher);
    }
    
    protected void logStartSearch() {
        logEvalResult("searching for string \"" + getText() + "\"");
    }
}
