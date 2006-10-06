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

package org.codehaus.xharness.tasks;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;

import org.codehaus.xharness.exceptions.TestSkippedException;


/**
 * Implementation of the <code>skip</code> Task. If the condition contained within the
 * within the SkipTask is true, the TestCase/TestGroup containing the SkipTask is
 * being skipped.
 * The optional description attribute gives a text description of the skip
 * condition.
 */
public class SkipTask extends ConditionBase {
    private String description;

    /**
     * Set the description attribute.
     *
     * @param descr  the description String.
     */
    public void setDescription(String descr) {
        description = descr;
    }

    /**
     *Get the value of the description attribute.
     *
     * @return  the description String.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Execute this Task.
     * 
     * @throws BuildException If the number of nested conditions is not equals 1.
     */
    public void execute() throws BuildException {
        if (countConditions() > 1) {
            throw new BuildException("You must not nest more than one condition into <skip>");
        }
        if (countConditions() < 1) {
            throw new BuildException("You must nest a condition into <skip>");
        }
        Condition c = (Condition)getConditions().nextElement();
        if (c.eval()) {
            throw new TestSkippedException(description, false);
        }
    }
}
