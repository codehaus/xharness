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

package org.codehaus.xharness.exceptions;

import org.apache.tools.ant.BuildException;


/**
 * Exception that signals a skipped TestCase.
 * 
 * @author Gregor Heine
 */
public class TestSkippedException extends BuildException {
    private static final long serialVersionUID = 
        TestSkippedException.class.getName().hashCode();
    private boolean skippedByPattern;

    /**
     * Constructs an exception with the given descriptive message and
     * and indicator whether it originated from a SkipTask or a mattern mismatch.
     * 
     * @param msg The exception message.
     * @param byPattern true, if it originated from a pattern mismatch, otherwise false.
     */
    public TestSkippedException(String msg, boolean byPattern) {
        super(msg);
        skippedByPattern = byPattern;
    }
    
    /**
     * Test, if the exception was thrown due to a pattern mismatch.
     * 
     * @return true, if it originated from a pattern mismatch, otherwise false.
     */
    public boolean skippedByPattern() {
        return skippedByPattern;
    }
}
