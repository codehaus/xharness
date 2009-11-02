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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.codehaus.xharness.exceptions.FatalException;

public class FileContains extends ProjectComponent implements Condition {
    private File file;
    private String encoding;
    private String text;
    
    public void setFile(File f) {
        file = f;
    }
    
    public void setEncoding(String e) {
        encoding = e;
    }
    
    /**
     * Add Text to be matched upon.
     *
     * @param txt The text to match.
     */
    public void addText(String txt) {
        if (text == null) {
            text = getProject().replaceProperties(txt);
        } else {
            throw new FatalException("Cannot use string argument and CDATA");
        }
    }

    /**
     * Add Text to be matched upon.
     *
     * @param txt The text to match.
     */
    public void setString(String txt) {
        if (text == null) {
            text = getProject().replaceProperties(txt);
        } else {
            throw new FatalException("Cannot use string argument and CDATA");
        }
    }

    public boolean eval() throws BuildException {
        if (getText() == null) {
            throw new FatalException("text not defined");
        }
        try {
            BufferedReader reader = getFileReader();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {    
                if (line.indexOf(getText()) >= 0) {
                    log("File " + file.getAbsolutePath() + " contains \"" + getText() + "\"", 
                        Project.MSG_VERBOSE);
                    return true;
                }
                
            }
            log("File " + file.getAbsolutePath() + " does not contain \"" + getText() + "\"", 
                Project.MSG_VERBOSE);
        } catch (IOException ioe) {
            log("Unable to read file " + file.getAbsolutePath(), 
                Project.MSG_VERBOSE);
        }
        return false;
    }
    
    protected BufferedReader getFileReader() throws IOException {
        if (file == null) {
            throw new BuildException("file not defined");
        }
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr;
        if (encoding == null) {
            isr = new InputStreamReader(fis);
        } else {
            isr = new InputStreamReader(fis, encoding);
        }
        return new BufferedReader(isr);
    }

    protected String getText() {
        return text;
    }
}
