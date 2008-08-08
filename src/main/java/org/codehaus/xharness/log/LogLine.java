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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A LogLine is an entry in a LineBuffer. It consists of a line of text (String)
 * and an associated log level/priority. An ANSI code filter is provided to 
 * allow for reading of text without ANSI control characters.
 * 
 * @author Gregor Heine
 */
public class LogLine {
    private String textLine;
    private int priority;
    
    /**
     * Constructs a LogLine with the given priority and text.
     * 
     * @param prio The priority of the log line.
     * @param text The text string of the log line.
     */
    protected LogLine(int prio, String text) {
        this.priority = prio;
        this.textLine = text;
    }
    
    /**
     * Returns the text of the log line.
     * 
     * @return The text String.
     */
    public String getText() {
        return this.textLine;
    }

    /**
     * Returns the text of the log line excluding ANSI 
     * escape code if true, returns unfiltered text
     * otherwise.
     *
     * @param ignoreANSI retrieve all text excluding ANSI code.
     * @return The text String with or without ANSI codes.
     */
    public String getText(boolean ignoreANSI) {
        if (ignoreANSI) {
            String txt = getText();

            Pattern pattern = Pattern.compile("\\u001B\\[\\d+m");
            Matcher matcher = pattern.matcher(txt);
            txt = matcher.replaceAll("");

            return txt; //Stripped line.
        }
        return getText();
    }
    
    /**
     * Returns the priority (log level) of the log line.
     * 
     * @return The line's priority.
     */
    public int getPriority() {
        return this.priority;
    }
    
    /**
     * Sets the priority to a new value.
     * 
     * @param prio The new priority value.
     */
    protected void setPriority(int prio) {
        this.priority = prio;
    }
}
