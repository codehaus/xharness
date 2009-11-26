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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.And;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.Not;
import org.apache.tools.ant.taskdefs.condition.Or;
import org.codehaus.xharness.exceptions.FatalException;
import org.codehaus.xharness.log.LineBuffer;
import org.codehaus.xharness.log.LogLine;

/**
 * The SubSection condition defines a subsection of a task output which can then be used to apply 
 * further output validation via a nested condition.
 *
 * @author  Gregor Heine
 */
public class SubSection extends AbstractOutput {
    private Condition condition;
    private String beginRegex;
    private String endRegex;
    private int beginAfter = 0;
    private int repeat = 1;
    private boolean greedy = false;
    
    /**
     * Set the number of subsections that are skipped before the nested condition is evaluated. 
     * The default value is 0. This can be used to strip out a "header" from the output that 
     * matches the same pattern as the main output contents, but can be ignored.
     * 
     * @param skip The number of initial subsections to skip
     */
    public void setBeginAfter(int skip) {
        if (skip < 0) {
            throw new FatalException("subsection: beginAfter value must be >= 0");
        }
        beginAfter = skip;
    }

    /**
     * Set the number of times the subsection is repeated in the output. The default value is 1.
     * If the value is > 1, the nested condition is evaluated multiple times for each of the
     * subsections. If the nested condition evaluates to false for any of the subsections,
     * the subsection will evaluate false, i.e. the nested condition must pass for all 
     * subsections.
     * 
     * @param num The number of times this subsection is repeated.
     */
    public void setRepeat(int num) {
        if (num < 1) {
            throw new FatalException("<subsection> repeat value must be > 0");
        }
        repeat = num;
    }

    /**
     * Set searching for the endRegex to greedy mode. The default value is false. By default,
     * the endRegex will match upon the next occurrence of the expression in the output.
     * In greedy mode, the endRegex will instead match upon the last occurrence of the expression
     * in the output.
     * 
     * @param gr Set true to enable greedy matching of the endRegex
     */
    public void setGreedy(boolean gr) {
        greedy = gr;
    }

    /**
     * Set the regular expression that marks the beginning of this subsection.
     *
     * @param s the regular expression that begins this subsection
     */
    public void setBeginRegex(String s) {
        beginRegex = s;
    }

    /**
     * Set the regular expression that marks the end of this subsection.
     *
     * @param s the regular expression that ends this subsection
     */
    public void setEndRegex(String s) {
        endRegex = s;
    }

    /**
     * Add an arbitrary nested condition.
     * 
     * @param c the nested condition to evaluate for this subsection
     */
    public void add(Condition c) {
        if (c != null) {
            if (condition != null) {
                throw new FatalException("Only one nested condition is supported.");
            }
            condition = c;
        }
    }

    /**
     * Add an &lt;not&gt; condition "container".
     *
     * @param n a Not condition
     * @since 1.1
     */
    public void addNot(Not n) {
        if (n != null) {
            if (condition != null) {
                throw new FatalException("Only one nested condition is supported.");
            }
            condition = n;
        }
    }

    /**
     * Add an &lt;and&gt; condition "container".
     *
     * @param a an And condition
     * @since 1.1
     */
    public void addAnd(And a) {
        if (a != null) {
            if (condition != null) {
                throw new FatalException("Only one nested condition is supported.");
            }
            condition = a;
        }
    }

    /**
     * Add an &lt;or&gt; condition "container".
     *
     * @param o an Or condition
     * @since 1.1
     */
    public void addOr(Or o) {
        if (o != null) {
            if (condition != null) {
                throw new FatalException("Only one nested condition is supported.");
            }
            condition = o;
        }
    }

    /**
     * Evaluate this subsection condition.
     * 
     * @return true if the expected output is found , false otherwise
     * @exception BuildException if an error occurs
     */
    public boolean eval() throws BuildException {
        if (condition == null) {
            throw new FatalException("You must nest a condition into <subsection>");
        }
        if (beginRegex == null && endRegex == null) {
            throw new FatalException("<subsection> requires beginRegex or endRegex attribute");
        }
        if (beginAfter > 0 && beginRegex == null) {
            throw new FatalException("<subsection> beginRegex must be set when beginAfter > 0");
        }
        if (repeat > 1 && (beginRegex == null || endRegex == null)) {
            throw new FatalException("<subsection> use of repeat require "
                                     + "beginRegex and endRegex attribute");
        }
        if (greedy && endRegex == null) {
            throw new FatalException("<subsection> endRegex must be set when greedy=true");
        }
        if (greedy && (repeat + beginAfter) > 1) {
            throw new FatalException(
                    "<subsection> can't use repeat and beginAfter with greedy=true");
        }
        Searcher searcher = new Searcher(getOutputIterator());
        Pattern beginPattern = beginRegex == null ? null : Pattern.compile(beginRegex);
        Pattern endPattern = endRegex == null ? null : Pattern.compile(endRegex);
        int findCount = 0;
        while (findCount < beginAfter) {
            ++findCount;
            LineBuffer buf = searcher.getTo(beginPattern);
            if (buf == null) {
                logEvalResult("Can't find " + printCount(findCount) + "begin of <subsection> \"" 
                              + beginRegex + "\"");
                return false;
            } else {
                log("Skipping " + printCount(findCount) + "<subsection>", 
                    Project.MSG_VERBOSE);
            }
        }
        
        while (findCount < beginAfter + repeat) {
            ++findCount;
            if (beginPattern != null) {
                LineBuffer buf = searcher.getTo(beginPattern);
                if (buf == null) {
                    logEvalResult("Can't find " + printCount(findCount) 
                                  + "begin of <subsection> \"" + beginRegex + "\"");
                    return false;
                }
            }
            LineBuffer buf = greedy ? searcher.getToLast(endPattern) : searcher.getTo(endPattern);
            if (buf == null) {
                logEvalResult("can't find end of <subsection> \"" + endRegex + "\"");
                return false;
            }
            log("Found " + (beginAfter + repeat > 1 ? printCount(findCount) : "") + "<subsection>", 
                Project.MSG_VERBOSE);
            try {
                setLineBuffer(buf);
                boolean ret = condition.eval();
                if (!ret) {
                    return false;
                }
            } finally {
                setLineBuffer(null);
            }
        }
        return true;
    }

    private String printCount(int findCount) {
        int rightmostDigit = findCount % 10;
        if (rightmostDigit == 1 && findCount != 11) {
            return findCount + "st ";
        } else if (rightmostDigit == 2 && findCount != 12) {
            return findCount + "nd ";
        } else if (rightmostDigit == 3 && findCount != 13) {
            return findCount + "rd ";
        } else {
            return findCount + "th ";
        }
    }

    private class Searcher {
        private Iterator lineIter;
        private String currentLine;
        private int skipChars;
        
        public Searcher(Iterator iter) {
            lineIter = iter;
        }
        
        public LineBuffer getTo(Pattern pattern) {
            LineBuffer subSection = new LineBuffer(getStreamPrio());
            if (pattern == null) {
                if (currentLine != null) {
                    subSection.logLine(currentLine);
                    currentLine = null;
                }
                while (lineIter.hasNext()) {
                    subSection.addLine((LogLine)lineIter.next());
                }
            } else {
                boolean patternFound = false;
                while (!patternFound && (currentLine != null || lineIter.hasNext())) {
                    String text;
                    if (currentLine == null) {
                        LogLine line = (LogLine)lineIter.next();
                        text = line.getText();
                    } else {
                        text = currentLine;
                        currentLine = null;
                    }
                    Matcher matcher = pattern.matcher(text);
                    if (matcher.find(skipChars)) {
                        patternFound = true;
                        subSection.logLine(text.substring(0, matcher.start()));
                        currentLine = text.substring(matcher.start());
                        skipChars = matcher.end() - matcher.start();
                    } else {
                        subSection.logLine(text);
                        currentLine = null;
                        skipChars = 0;
                    }
                }
                if (!patternFound) {
                    return null;
                }
            }
            return subSection;
        }
        
        public LineBuffer getToLast(Pattern pattern) {
            LineBuffer subSection = new LineBuffer(getStreamPrio());
            LineBuffer previousSubSection = null;
            if (currentLine != null) {
                Matcher matcher = pattern.matcher(currentLine);
                int lastIdx = -1;
                while (matcher.find()) {
                    lastIdx = matcher.start();
                }
                if (lastIdx >= 0) {
                    previousSubSection = (LineBuffer)subSection.clone();
                    previousSubSection.logLine(currentLine.substring(0, lastIdx));
                }
                subSection.logLine(currentLine);
                currentLine = null;
            }
            while (lineIter.hasNext()) {
                LogLine line = (LogLine)lineIter.next();
                String text = line.getText();
                Matcher matcher = pattern.matcher(text);
                int lastIdx = -1;
                while (matcher.find()) {
                    lastIdx = matcher.start();
                }
                if (lastIdx >= 0) {
                    previousSubSection = (LineBuffer)subSection.clone();
                    previousSubSection.logLine(text.substring(0, lastIdx));
                }
                subSection.addLine(line);
            }
            return previousSubSection;
        }
    }
}
