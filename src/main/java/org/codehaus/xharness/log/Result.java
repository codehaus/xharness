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

/**
 * Result base class. Contains information about the type of result, the
 * actual result, the test name and group, owner, error description and
 * an order Id used in the result processing to place this result in the right
 * place.
 *
 * @author  Gregor Heine
 */
public interface Result {
    int PROCESS_TASK = 0;
    int OTHER_TASK = 1;
    int SERVICE = 2;
    int START = 3;
    int VERIFY = 4;
    int STOP = 5;
    int TESTCASE = 6;
    int TESTGROUP = 7;
    int XHARNESS = 8;
    int LINK = 9;

    int SKIPPED = 0;
    int PASSED = 1;
    int WARNING = 2;
    int FAILED = 3;
}
