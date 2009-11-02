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

import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.codehaus.xharness.exceptions.FatalException;
import org.codehaus.xharness.log.LogLine;

public class OutputRegex extends OutputIs {
    public boolean eval() throws BuildException {
        if (getText() == null) {
            throw new FatalException("Missing regular expression");
        } 
        Pattern pattern = Pattern.compile(getText());
        Iterator iter = getOutputIterator();
        while (iter.hasNext()) {
            LogLine line = (LogLine)iter.next();
            if (pattern.matcher(line.getText(filterANSI())).find()) {
                log(logPrefix() + "matches pattern \"" + getText() + "\"", Project.MSG_VERBOSE);
                return true;
            }
        }
        log(logPrefix() + "does not match pattern \"" + getText() + "\"", Project.MSG_VERBOSE);
        return false;
    }
}
