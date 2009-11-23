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


import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;


/**
 * A LineBuffer is used to store lines of text, associated with an integer priority level.
 *
 * @author Gregor Heine
 */
public class LineBuffer implements Cloneable {
    
    private List log = new LinkedList();;
    private int defaultPriority;
    private int minPriority = Integer.MAX_VALUE;
    private int maxPriority = Integer.MIN_VALUE;
    
    
    /**
     * Constructs a LineBuffer with the default priority 0.
     */
    public LineBuffer() {
        this(0);
    }
    
    /**
     * Constructs a LineBuffer with the specified default priority.
     * 
     * @param defaultPrio The default priority for this LineBuffer.
     */
    public LineBuffer(int defaultPrio) {
        this.defaultPriority = defaultPrio;
    }
    
    private LineBuffer(LineBuffer lineBuffer) {
        defaultPriority = lineBuffer.getDefaultPriority();
        minPriority = lineBuffer.getMinPriority();
        maxPriority = lineBuffer.getMaxPriority();
        for (Iterator iter = lineBuffer.iterator(); iter.hasNext();) {
            LogLine line = (LogLine)iter.next();
            log.add(line);
        }
    }

    /**
     * Clears the internal text buffer.
     */
    public void clear() {
        synchronized (log) {
            log.clear();
        }
    }
    
    /**
     * Returns the default priority of the LineBuffer.
     * 
     * @return The default priority.
     */
    public int getDefaultPriority() {
        return defaultPriority;
    }

    /**
     * Appends a line of text with the default priority to the Buffer. 
     * If the String contains line breaks ('\n', '\r' or '\f'), 
     * it is broken down into multiple lines.
     *
     * @param line The line(s) of text.
     */
    public void logLine(String line) {
        logLine(defaultPriority, line);
    }
    
    /**
     * Appends a line of text with the specified priority to the Buffer. 
     * If the String contains line breaks ('\n', '\r' or '\f'), 
     * it is broken down into multiple lines.
     *
     * @param priority The priority of the logged text.
     * @param text The line(s) of text.
     */
    public void logLine(int priority, String text) {
        if (text != null) {
            StringTokenizer tok = tokenize(text);
            while (tok.hasMoreTokens()) {
                addLine(new LogLine(priority, tok.nextToken()));
            }
        }
    }
    
    /**
     * Merges the text with the specified priority prio2 into the buffer,
     * only if it isn't already contained in the Buffer with priority prio1.
     * If the String contains line breaks ('\n', '\r' or '\f'), 
     * it is broken down into multiple lines.
     *
     * @param prio1 The priority under which the text may already exist in the buffer.
     * @param prio2 The priority of the logged text if hasn't already been logged.
     * @param text The line(s) of text.
     */
    public void mergeLine(int prio1, int prio2, String text) {
        if (text != null) {
            StringTokenizer tok = tokenize(text);
            synchronized (log) {
                while (tok.hasMoreTokens()) {
                    String newLine = tok.nextToken();
                    ListIterator iter = log.listIterator(log.size());
                    boolean found = false;
                    while (iter.hasPrevious()) {
                        LogLine scanLine = (LogLine)iter.previous();
                        if (scanLine.getPriority() == prio1
                            && scanLine.getText().equals(newLine)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        addLine(new LogLine(prio2, newLine));
                    }
                }
            }
        }
    }
    
    /**
     * Appends a log line to the Buffer. 
     *
     * @param logLine The log line.
     */
    public void addLine(LogLine logLine) {
        synchronized (log) {
            log.add(logLine);
            int priority = logLine.getPriority();
            if (priority < minPriority) {
                minPriority = priority;
            }
            if (priority > maxPriority) {
                maxPriority = priority;
            }
        }
    }
    
    /**
     * Represent all lines of this buffer as a String.
     * The lines are separated by '\n' line breaks.
     *
     * @return The contents of this buffer as a String.
     */
    public String toString() {
        return toString('\n', "", Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Represent the lines with the given priority as a String.
     * The lines are separated by '\n' line breaks.
     *
     * @param prio The priority of the returned lines.
     * @return The contents of this buffer as a String.
     */
    public String toString(int prio) {
        return toString('\n', "", prio, prio);
    }

    /**
     * Represent the lines in the range between the given minimum and maximum 
     * priority as a String.
     * The lines are separated by '\n' line breaks.
     *
     * @param minPrio The minimum priority of the returned lines.
     * @param maxPrio The maximum priority of the returned lines.
     * @return The contents of this buffer as a String.
     */
    public String toString(int minPrio, int maxPrio) {
        return toString('\n', "", minPrio, maxPrio);
    }

    /**
     * Represent all lines of this buffer as a String.
     * The lines are separated by the given sepeatator character and  
     * may be prefixed with an arbitrary String.
     *
     * @param lineSeparator The line separator char.
     * @param linePrefix The String every line is prefixed with. If <code>null</code>, every 
     *        line is prefixed with the line's priority followed by a colon, e.g. "3: " 
     * @return The contents of this buffer as a String.
     */
    public String toString(char lineSeparator, String linePrefix) {
        return toString(lineSeparator, linePrefix, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    
    /**
     * Represent all lines of this buffer as a String.
     * The lines are separated by the given sepeatator character and  
     * may be prefixed with an arbitrary String.
     *
     * @param lineSeparator The line separator char.
     * @param linePrefix The String every line is prefixed with. If <code>null</code>, every 
     *        line is prefixed with the line's priority followed by a colon, e.g. "3: " 
     * @param ignoreAnsi Strip ANSI characters from output
     * @return The contents of this buffer as a String.
     */
    public String toString(char lineSeparator, String linePrefix, boolean ignoreAnsi) {
        return toString(lineSeparator, linePrefix, 
                        Integer.MIN_VALUE, Integer.MAX_VALUE, ignoreAnsi);
    }
    
    /**
     * Represent the lines with the given priority as a String.
     * The lines are separated by the given sepeatator character and  
     * may be prefixed with an arbitrary String.
     *
     * @param lineSeparator The line separator char.
     * @param linePrefix The String every line is prefixed with. If <code>null</code>, every 
     *        line is prefixed with the line's priority followed by a colon, e.g. "3: " 
     * @param prio The priority of the returned lines.
     * @return The contents of this buffer as a String.
     */
    public String toString(char lineSeparator, String linePrefix, int prio) {
        return toString(lineSeparator, linePrefix, prio, prio);
    }
    
    /**
     * Represent the lines with the given priority as a String.
     * The lines are separated by the given sepeatator character and  
     * may be prefixed with an arbitrary String.
     *
     * @param lineSeparator The line separator char.
     * @param linePrefix The String every line is prefixed with. If <code>null</code>, every 
     *        line is prefixed with the line's priority followed by a colon, e.g. "3: " 
     * @param prio The priority of the returned lines.
     * @param ignoreAnsi Strip ANSI characters from output
     * @return The contents of this buffer as a String.
     */
    public String toString(char lineSeparator, String linePrefix, int prio, boolean ignoreAnsi) {
        return toString(lineSeparator, linePrefix, prio, prio, ignoreAnsi);
    }
    
    /**
     * Represent the lines in the range between the given minimum and maximum 
     * priority as a String.
     * The lines are separated by '\n' line breaks.
     * The lines are separated by the given sepeatator character and  
     * may be prefixed with an arbitrary String.
     *
     * @param lineSeparator The line separator char.
     * @param linePrefix The String every line is prefixed with. If <code>null</code>, every 
     *        line is prefixed with the line's priority followed by a colon, e.g. "3: " 
     * @param minPrio The minimum priority of the returned lines.
     * @param maxPrio The maximum priority of the returned lines.
     * @return The contents of this buffer as a String.
     */
    public String toString(char lineSeparator, String linePrefix, int minPrio, int maxPrio) {
        return toString(lineSeparator, linePrefix, minPrio, maxPrio, false);
    }
        
    /**
     * Represent the lines in the range between the given minimum and maximum 
     * priority as a String.
     * The lines are separated by '\n' line breaks.
     * The lines are separated by the given sepeatator character and  
     * may be prefixed with an arbitrary String.
     *
     * @param lineSeparator The line separator char.
     * @param linePrefix The String every line is prefixed with. If <code>null</code>, every 
     *        line is prefixed with the line's priority followed by a colon, e.g. "3: " 
     * @param minPrio The minimum priority of the returned lines.
     * @param maxPrio The maximum priority of the returned lines.
     * @param ignoreAnsi Strip ANSI characters from output
     * @return The contents of this buffer as a String.
     */
    public String toString(char lineSeparator, String linePrefix, 
                               int minPrio, int maxPrio, boolean ignoreAnsi) {
        StringBuffer ret = new StringBuffer();
        boolean first = true;

        synchronized (log) {
            Iterator iter = log.iterator();
            while (iter.hasNext()) {
                LogLine line = (LogLine)iter.next();
                if (line.getPriority() >= minPrio && line.getPriority() <= maxPrio) {
                    if (!first && lineSeparator != 0) {
                        ret.append(lineSeparator);
                    }
                    first = false;
                    if (linePrefix == null) {
                        ret.append(line.getPriority());
                        ret.append(": ");
                    } else {
                        ret.append(linePrefix);
                    }
                    ret.append(line.getText(ignoreAnsi));
                }
            }
        }
        return ret.toString();
    }
        
    /**
     * Represent the lines of this LineBuffer as an array of LogLine.
     *
     * @return The contents of this buffer as a LogLine array.
     */
    public LogLine[] toArray() {
        synchronized (log) {
            LogLine[] lines = new LogLine[log.size()];
            log.toArray(lines);
            return lines;
        }
    }

    
    /**
     * Represent the lines of this LineBuffer as an array of Strings.
     *
     * @return The contents of this buffer as a String array.
     */
    public String[] toStringArray() {
        return toStringArray(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    
    /**
     * Represent the lines of this LineBuffer as an array of Strings.
     *
     * @param priority The priority of the returned lines.
     * @return The contents of this buffer as a String array.
     */
    public String[] toStringArray(int priority) {
        return toStringArray(priority, priority);
    }
    
    /**
     * Represent the lines of this LineBuffer as an array of Strings.
     *
     * @param minPrio The minimum priority of the returned lines.
     * @param maxPrio The maximum priority of the returned lines.
     * @return The contents of this buffer as a String array.
     */
    public String[] toStringArray(int minPrio, int maxPrio) {
        LogLine[] lines = toArray();
        String[] strings = new String[lines.length];
        int count = 0;
        for (int i = 0; i < lines.length; i++) {
            LogLine line = (LogLine)lines[i];
            if (line.getPriority() >= minPrio && line.getPriority() <= maxPrio) {
                strings[count++] = line.getText();
            }
        }
        String[] ret = new String[count];
        System.arraycopy(strings, 0, ret, 0, count);
        return ret;
    }

    /**
     * Get the minimum priority of all logged text lines.
     * 
     * @return The minimum priority value of lines contained in this buffer. 
     */
    public int getMinPriority() {
        return minPriority;
    }

    /**
     * Get the maximum priority of all logged text lines.
     * 
     * @return The maximum priority value of lines contained in this buffer. 
     */
    public int getMaxPriority() {
        return maxPriority;
    }
    
    /**
     * Get an Iterator over {@link org.codehaus.xharness.log.LogLine} instances that 
     * represent all lines in this buffer. This Iterator operates on a snapshot of data
     * to avoid ConcurrentModificationException, using a copy-on-write technique. Thus it
     * does not support remove() operation
     * 
     * @see org.codehaus.xharness.log.LogLine
     * @return An Iterator over {@link org.codehaus.xharness.log.LogLine} instances. 
     */
    public Iterator iterator() {
        return new LineIterator(minPriority, maxPriority);
    }
    
    /**
     * Get an Iterator over {@link org.codehaus.xharness.log.LogLine} instances that 
     * represent all lines of the given priority in this buffer. This Iterator operates on
     * a snapshot of data to avoid ConcurrentModificationException, using a copy-on-write 
     * technique. Thus it does not support remove() operation
     * 
     * @see org.codehaus.xharness.log.LogLine
     * @param priority The priority of the lines in the returned Iterator.
     * @return An Iterator over {@link org.codehaus.xharness.log.LogLine} instances. 
     */
    public Iterator iterator(int priority) {
        return new LineIterator(priority, priority);
    }
    
    /**
     * Get an Iterator over {@link org.codehaus.xharness.log.LogLine} instances that 
     * represent all lines of the given priority in this buffer. This Iterator operates on
     * a snapshot of data to avoid ConcurrentModificationException, using a copy-on-write
     * technique. Thus it does not support remove() operation 
     * 
     * @see org.codehaus.xharness.log.LogLine
     * @param minPrio The minimum priority of the lines in the returned Iterator.
     * @param maxPrio The maximum priority of the lines in the returned Iterator.
     * @return An Iterator over {@link org.codehaus.xharness.log.LogLine} instances. 
     */
    public Iterator iterator(int minPrio, int maxPrio) {
        return new LineIterator(minPrio, maxPrio);
    }
    
    public Object clone() {
        return new LineBuffer(this);
    }
    
    private StringTokenizer tokenize(String text) {
        while (text.startsWith("\n") || text.startsWith("\r") || text.startsWith("\f")) {
            text = text.substring(1);
        }
        while (text.endsWith("\n") || text.endsWith("\r") || text.endsWith("\f")) {
            text = text.substring(0, text.length() - 1);
        }
        return new StringTokenizer(text, "\n\r\f");
    }
    
    private class LineIterator implements Iterator {
        private Iterator iter;
        private LogLine nextLine;
        private int minPriority;
        private int maxPriority;
        
        public LineIterator(int minPrio, int maxPrio) {
            minPriority = minPrio;
            maxPriority = maxPrio;
            synchronized (log) {
                iter = Arrays.asList(log.toArray()).iterator();
            }
            nextLine = getNext();
        }
        
        public boolean hasNext() {
            return nextLine != null;
        }
        
        public Object next() {
            LogLine ret = nextLine;
            if (nextLine != null) {
                nextLine = getNext();
            }
            return ret;
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        private LogLine getNext() {
            while (iter.hasNext()) {
                LogLine line = (LogLine)iter.next();
                if (line.getPriority() >= minPriority
                    && line.getPriority() <= maxPriority) {
                    return line;
                }
            }
            return null;
        }
    }
}
