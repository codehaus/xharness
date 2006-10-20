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

import java.io.File;
import java.util.regex.Pattern;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.codehaus.xharness.tasks.TestCaseTask;
import org.codehaus.xharness.tasks.XharnessTask;

/**
 * The TaskRegistry is a central entity in the XHarness test cycle. It implements the 
 * singleton pattern and serves the purpose of conecting tasks and loggers awith 
 * other loggers, is responsible to set the "current.test.directory" property
 * and perform test pattern matching.
 * 
 * @author Gregor Heine
 */
public class TaskRegistry {
    public static final String CURRENT_TEST_DIR_PROPERY = "current.test.directory";

    private static TaskRegistry singleton;

    private int currentTaskId;
    private ResultFormatter formatter;
    private TestLogger currentTestLogger;
    private Pattern pattern;
    private Project project;
    private File baseDir;
    private String errorProperty;
    
    protected TaskRegistry() {
        // for testing only!
    }
    
    private TaskRegistry(XharnessTask task) {
        project = task.getProject();
        baseDir = task.getBasedir();
        errorProperty = task.getErrorProperty();
        currentTaskId = 0;
        String patStr = task.getPattern();
        if (patStr != null && !"".equals(patStr)) {
            pattern = Pattern.compile(patStr);
        }
        new TestLogger(this, task, task.getName(), null, "", null);
        formatter = new ResultFormatter(task.getResultsdir());
    }

    /**
     * Initializes a TaskRegistry singleton for the given XHarness base task.
     * @param base The XHarness root task.
     * @return A new TaskRegistry instance.
     */
    public static TaskRegistry init(XharnessTask base) {
        singleton = new TaskRegistry(base);
        return singleton;
    }
    
    /**
     * Returns the current singleton instance.
     * 
     * @return The current TaskRegistry singleton or <code>null</code> if the registry 
     *         hasn't been initialized.
     */
    public static TaskRegistry getRegistry() {
        return singleton; 
    }
    
    /**
     * Returns a child logger of the current test logger (testcase/testgroup/etc.) with
     * the given id. The id can be either a logging name or a positive or negative integer
     * value. See {link org.codehaus.xharness.log.TestLogger#getTask(String)}.
     * 
     * @param id The logger Id.
     * @return The TaskLogger eith the given Id or <code>null</code> if there is no
     *         such logger.
     */
    public static TaskLogger getLogger(String id) {
        if (singleton != null && singleton.currentTestLogger != null) {
            return singleton.currentTestLogger.getTask(id);
        }
        return null;
    }
    
    /**
     * Test if the (logger-)name of the given TestCaseTask matches the pattern
     * specified in the top-level XHarnessTask.
     * @param task The Task to test.
     * @return true, if the registry singelton hasn't been initialized, the task has 
     *         an associated logger and the task is either not a TestCaseTask or
     *         the testcase matches the pattern, otherwise false.
     */
    public static boolean matchesPattern(Task task) {
        if (singleton == null || singleton.pattern == null) {
            return true;
        } else {
            Pattern pattern = singleton.pattern;
            TestLogger logger = singleton.currentTestLogger;
            if (logger != null && logger.isMyTask(task)) {
                if (task instanceof TestCaseTask) {
                    return pattern.matcher(logger.getFullName()).find();
                } else {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Sets the error property sepecified in the top level XHarness task, when
     * an error has occurred in a Task.
     * 
     * @param error The occurred error.
     */
    public static void setErrorProperty(Throwable error) {
        if (error != null && singleton != null && singleton.errorProperty != null) {
            Project proj = singleton.getProject();
            proj.setNewProperty(singleton.errorProperty, "true");
        }
    }
    
    /**
     * Shuts down and resets the current TaskRegistry singleton. 
     * 
     * @param failure The failure that has occurred in the top-level XHarness task, or null.
     */
    public void shutdown(Throwable failure) {
        if (currentTestLogger != null) {
            currentTestLogger.setFailure(failure);
            currentTestLogger.taskFinishedInternal();
        }
        singleton = null;
    }
    
    /**
     * Returns a unique, sequential logger id.
     * 
     * @return The next logger Id.
     */
    public int getNextId() {
        synchronized (this) {
            return currentTaskId++;
        }
    }
    
    /**
     * Sets the specified TestLogger to be the current test's logger.
     * 
     * @param testLogger The current test logger.
     */
    public void setCurrentTest(TestLogger testLogger) {
        if (testLogger != null && currentTestLogger != testLogger) {
            currentTestLogger = testLogger;
            
            if (baseDir != null) {
                String testName = currentTestLogger.getFullName();
                if (testName == null) {
                    testName = "";
                }
                
                File testDir = new File(baseDir, testName);
                String absPath = testDir.getAbsolutePath();

                if (!testDir.isDirectory()) {
                    getProject().log("Unknown test directory detected: " + absPath, 
                                     Project.MSG_DEBUG);
                }
                getProject().log("Setting property " + CURRENT_TEST_DIR_PROPERY + " to " + absPath, 
                            Project.MSG_DEBUG);
                getProject().setUserProperty(CURRENT_TEST_DIR_PROPERY, absPath);
            }
        }
    }
    
    /**
     * Returns the current test logger.
     * 
     * @return the current test logger.
     */
    public TestLogger getCurrentTest() {
        return currentTestLogger;
    }
     
    /**
     * Returns the Project associated with the top-level XHarness task.
     * 
     * @return The Project.
     */
    public Project getProject() {
        return project;
    }
    
    /**
     * Returns the result formatter for the current XHarness suite.
     * 
     * @return The result formatter.
     */
    public ResultFormatter getFormatter() {
        return formatter;
    }

    /** 
     * Initializes the Taskregistry singleton without a top-level XHarness task. 
     * For testing purposes only!
     * 
     * @param project The Project
     * @return A new TaskRegistry instance.
     */
    protected static TaskRegistry init(Project project) {
        singleton = new TaskRegistry();
        singleton.project = project;
        return singleton;
    }
    
    /**
     * Resets the current TaskRegistry singleton. For testing purposes only!
     */
    protected static void reset() {
        singleton = null;
    }
}
