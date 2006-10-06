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

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.codehaus.xharness.log.TaskRegistry;

/**
 * {@link org.apache.tools.ant.TaskContainer} implementation for Tasks within
 * the XHarness framework. Implements reference infrastructure for the retrieval
 * of (service) task references and initializes child tasks.
 *
 * @author  Gregor Heine
 */
public class XharnessTask extends TestGroupTask {
    private File resultsdir = null;
    private File basedir = null;
    private String pattern = null;
    private String errorProperty = null;

    /**
     * Sets the directory for result output. In this directory, the results of
     * every test and process are stored in XML format. Called by the ant runtime.
     *
     * @param dir The directory for the XML result files.
     */
    public void setResultsdir(File dir) {
        resultsdir = dir;
    }

    /**
     * Get the base directory for test processes.
     *
     * @return The test base directory
     */
    public File getResultsdir() {
        return resultsdir;
    }

    /**
     * Sets the base directory for tests. If set, test processes calculate the
     * directory to run in from the base dir and the "object path" they're in
     * realtive to the &lt;xharness&gt; top-level object. Called by the ant runtime.
     *
     * @param dir The base directory for test processes.
     */
    public void setBasedir(File dir) {
        basedir = dir;
    }

    /**
     * Get the base directory for test processes.
     *
     * @return The test base directory
     */
    public File getBasedir() {
        return basedir;
    }

    /**
     * Sets the pattern used to select which tests to run. Called by the ant
     * runtime.
     *
     * @param p The pattern string.
     */
    public void setPattern(String p) {
        pattern = p;
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public void setErrorProperty(String property) {
        errorProperty = property;
    }
    
    public String getErrorProperty() {
        return errorProperty;
    }

    /**
     * Do the execution of this Task.
     * 
     * @throws BuildException If an exception occurs while executing a child Task.
     */
    public void execute() throws BuildException {
        if (resultsdir == null) {
            throw new BuildException("Required attribute \"resultsdir\" not set!");
        }

        TaskRegistry registry = TaskRegistry.init(this);
        BuildException exception = null;
        try {
            super.execute();
        } catch (BuildException be) {
            exception = be;
        } finally {
            registry.shutdown(exception);
            log("Completed " + toString(), Project.MSG_INFO);
        }
    }

    public String toString() {
        if (getName() == null) {
            return "xharness";
        }
        return "xharness " + getName();
    }

}
