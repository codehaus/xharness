/*
 * Copyright 2009 Progress Software
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
import org.codehaus.xharness.exceptions.FatalException;
import org.codehaus.xharness.log.LogLine;

public abstract class AbstractRepeateableOutput extends OutputIs {
    private int minOccurr = 1;
    private int maxOccurr = -1;
    
    /**
     * Set the minimum number of expected occurrences of this condition in the task output.
     * The default value is 1, i.e. at least 1 occurrence/match is expected.
     * The condition will pass if the output matches the text/expression at least the minimum
     * number of times, otherwise it will fail. 
     *
     * @param min the minimum number of occurrences to expect. Value must be >=1.
     */
    public void setMin(int min) {
        if (min < 1) {
            throw new FatalException("<" + getClass().getSimpleName() + "> min value must be > 0");
        }
        minOccurr = min;
    }
    
    /**
     * Set the maximum number of expected occurrences of this condition in the task output.
     * By default, the maximum is not set, i.e. the maxiumum numner of occurrences is unbound.
     * The condition will pass if the output matches the text/expression at most the maximum
     * number of times, otherwise it will fail. 
     *
     * @param max the maximum number of occurrences to expect. Value must be >=1.
     */
    public void setMax(int max) {
        if (max < 1) {
            throw new FatalException("<" + getClass().getSimpleName() + "> max value must be > 0");
        }
        maxOccurr = max;
    }
    
    protected int getMin() {
        return minOccurr;
    }
    
    protected int getMax() {
        return maxOccurr;
    }

    protected boolean eval(Searcher searcher) throws BuildException {
        if (maxOccurr >= 0 && getMin() > getMax()) {
            throw new FatalException("<" + getClass().getSimpleName() 
                                     + "> min value must be <= max value");
        }
        logStartSearch();
        int findCount = 0;
        while (findCount < getMin()) {
            if (!searcher.findAgain()) {
                log("Condition failed: found " + printNumOccur(findCount) 
                              + ", required  at least " + getMin(), Project.MSG_VERBOSE);
                return false;
            }
            ++findCount;
        }
        if (getMax() < 0) {
            log("Condition passed: found at least " + printNumOccur(findCount), 
                Project.MSG_VERBOSE);
            return true;
        }
        while (findCount < getMax()) {
            if (!searcher.findAgain()) {
                log("Condition passed: found " + printNumOccur(findCount), Project.MSG_VERBOSE);
                return true;
            }
            ++findCount;
        }
        boolean ret =  !searcher.findAgain();
        if (ret) {
            log("Condition passed: found " + printNumOccur(findCount), Project.MSG_VERBOSE);
        } else {
            log("Condition failed: found more than " + printNumOccur(getMax()), 
                Project.MSG_VERBOSE);
        }
        return ret;
    }
    
    private String printNumOccur(int occurrences) {
        return Integer.toString(occurrences) + (occurrences == 1 ? " occurrence" : " occurrences");
    }
    
    protected abstract void logStartSearch();
    
    protected interface Searcher {
        boolean findAgain();
    }

    protected abstract class LineBufferSearcher implements Searcher {
        private Iterator lineIter;
        private String currentLine;
        
        public LineBufferSearcher(Iterator iter) {
            lineIter = iter;
        }
        
        public boolean findAgain() {
            boolean patternFound = false;
            while (!patternFound && (currentLine != null || lineIter.hasNext())) {
                String text;
                if (currentLine == null) {
                    LogLine line = (LogLine)lineIter.next();
                    text = line.getText(isIgnoreANSI());
                } else {
                    text = currentLine;
                    currentLine = null;
                }
                int index = indexIn(text);
                if (index >= text.length()) {
                    patternFound = true;
                } else if (index > 0) {
                    currentLine = text.substring(index);
                    patternFound = true;
                }
            }
            return patternFound;
        }
        
        protected abstract int indexIn(String text);
    }
}
