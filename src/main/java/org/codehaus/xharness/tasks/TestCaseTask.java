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

/**
 * Implementation of the <code>testcase</code> task. Extends {@link TestGroupTask} 
 * and adds the owner attribute.
 *
 * @author  Gregor Heine
 */
public class TestCaseTask extends TestGroupTask {
    private String owner = "unknown";

    /**
     * Sets the owner attribute. Called by the ant runtime.
     *
     * @param o The owner of this testcase.
     */
    public void setOwner(String o) {
        this.owner = o;
    }

    /**
     * Get the owner of this TestCase.
     *
     * @return  the owner of this testcase.
     */
    public String getOwner() {
        return owner;
    }

    public String toString() {
        return "testcase " + getName();
    }

    /**
     * Determines if the execution of this Task's children is interrupted, if
     * one of the children fails.
     *
     * @return  true. TestCase's don't continue to execute children if one has failed.
     */
    protected boolean failOnError() {
        return true;
    }
}
