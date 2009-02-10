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
 * Defines XML constants used in the XML Result output of XHarness testcases
 * and processes.
 *
 * @author Gregor Heine
 */
public interface XMLConstants {

    /** the testcase element. */
    String RESULTS = "results";

    /** the task element. */
    String TASK = "task";

    /** the testgroup element. */
    String GROUP = "group";

    /** the testcase element. */
    String TEST = "test";

    /** the service element. */
    String SERVICE = "service";

    /** the service start element. */
    String START = "start";

    /** the service verify element. */
    String VERIFY = "verify";

    /** the service stop element. */
    String STOP = "stop";

    /** the top-level xharness element. */
    String XHARNESS = "xharness";

    /** the link element. */
    String LINK = "link";

    /** the testdescription element. */
    String DESCRIPTION = "description";

    /** the command element. */
    String COMMAND = "command";

    /** the system-out element. */
    String OUTPUT = "output";

    /** name attribute. */
    String ATTR_ORDERID = "orderid";

    /** task name attribute. */
    String ATTR_TASK_NAME = "name";

    /** task full name attribute. */
    String ATTR_FULL_NAME = "fullname";

    /** task parent attribute. */
    String ATTR_PARENT = "parent";

    /** task reference attribute. */
    String ATTR_REFERENCE = "reference";

    /** result attribute. */
    String ATTR_RESULT = "result";

    /** owner attribute. */
    String ATTR_OWNER = "owner";

    /** returnvalue attribute. */
    String ATTR_RETVAL = "retval";

    /** time attribute. */
    String ATTR_TIME = "time";

    /** raw output attribute. */
    String ATTR_RAWDATA = "rawdata";

    /** raw output attribute. */
    String ATTR_LOGLEVEL = "level";
}
