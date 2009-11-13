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



import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.UnsupportedAttributeException;
import org.apache.tools.ant.UnsupportedElementException;

import org.codehaus.xharness.exceptions.AssertionWarningException;
import org.codehaus.xharness.exceptions.FatalException;
import org.codehaus.xharness.exceptions.TestSkippedException;
import org.codehaus.xharness.log.TaskRegistry;

/**
 * {@link org.apache.tools.ant.TaskContainer} implementation for Tasks within
 * the XHarness framework. Implements reference infrastructure for the retrieval
 * of (service) task references and initializes child tasks.
 *
 * @author  Gregor Heine
 */
public class TestGroupTask extends Task implements TaskContainer {
    private List children = new LinkedList();
    private String groupName;

    //
    // ---- TaskContainer implementation
    //

    /**
     * Add a nested task to this TaskContainer.
     *
     * @param nestedTask  Nested task to execute sequentially
     */
    public void addTask(Task nestedTask) {
        if (nestedTask != null) {
            children.add(nestedTask);
        }
    }

    public List getNestedTasks() {
        return children;
    }

    //
    // ---- Resultable implementation
    //

    /**
     * Set the name of this Task, used for Result reporting.
     *
     * @param name  The name this Task
     */
    public void setName(String name) {
        groupName = name;
    }

    /**
     * Get the name of this Task.
     *
     * @return  This Task's name.
     */
    public String getName() {
        return groupName;
    }

    //
    // ---- Task overrides
    //

    /**
     * Do the execution of this Task.
     * 
     * @throws BuildException If an exception occurs while executing a child Task.
     * throws  TestSkippedException If the test doesn't match the test pattern.
     */
    public void execute() throws BuildException {
        if (!TaskRegistry.matchesPattern(this)) {
            String msg = toString() + " doesn't match pattern. Skipped.";
            log(msg, Project.MSG_INFO);
            throw new TestSkippedException(msg, true);
        }
        log("Performing " + toString(), Project.MSG_INFO);
        BuildException error = null;
        AssertionWarningException warning = null;
        Iterator iter = children.iterator();
        while (iter.hasNext()) {
            Task currentTask = (Task)iter.next();

            try {
                currentTask.perform();
            } catch (FatalException ex) {
                log("Fatal: " + ex.getMessage(), Project.MSG_ERR);
                throw ex;
            } catch (UnsupportedElementException ex) {
                log("Fatal: " + ex.getMessage(), Project.MSG_ERR);
                throw ex;
            } catch (UnsupportedAttributeException ex) {
                log("Fatal: " + ex.getMessage(), Project.MSG_ERR);
                throw ex;
            } catch (TestSkippedException ex) {
                currentTask = unwrapTask(currentTask);
                if (!(currentTask instanceof TestGroupTask)) {
                    throw ex;
                }
            } catch (AssertionWarningException ex) {
                if (ex.getMessage() != null) {
                    log(ex.getMessage(), Project.MSG_ERR);
                } else {
                    StringWriter sw = new StringWriter();
                    ex.printStackTrace(new PrintWriter(sw, true));
                    log(sw.toString(), Project.MSG_ERR);
                    
                }
                if (warning == null) {
                    TaskRegistry.setErrorProperty(ex);
                    currentTask = unwrapTask(currentTask);
                    if (currentTask instanceof ServiceDef) {
                        String name = ((ServiceDef)currentTask).getName();
                        warning = new AssertionWarningException("Warning in Service " + name, ex);
                    } else if (currentTask instanceof ServiceInstance) {
                        String name = currentTask.getTaskName();
                        warning = new AssertionWarningException("Warning in Service " + name, ex);
                    } else if (currentTask instanceof TestGroupTask) {
                        String name = ((TestGroupTask)currentTask).getName();
                        if (currentTask instanceof TestCaseTask) {
                            warning = new AssertionWarningException("Warning in Testcase " + name, 
                                                                    ex);
                        } else if (failOnError()) {
                            warning = new AssertionWarningException("Warning in Testgroup " + name, 
                                                                    ex);
                        }
                    } else {
                        String name = currentTask.getTaskName();
                        warning = new AssertionWarningException("Warning in Task " + name, ex);
                    }
                }
            } catch (Exception ex) {
                if (ex instanceof BuildException && ex.getMessage() != null) {
                    log(ex.getMessage(), Project.MSG_ERR);
                } else {
                    StringWriter sw = new StringWriter();
                    ex.printStackTrace(new PrintWriter(sw, true));
                    log(sw.toString(), Project.MSG_ERR);
                    
                }
                if (error == null) {
                    TaskRegistry.setErrorProperty(ex);
                    currentTask = unwrapTask(currentTask);
                    if (currentTask instanceof ServiceDef) {
                        String name = ((ServiceDef)currentTask).getName();
                        error = new BuildException("Service " + name + " failed", ex);
                    } else if (currentTask instanceof ServiceInstance) {
                        String name = currentTask.getTaskName();
                        error = new BuildException("Service " + name + " failed", ex);
                    } else if (currentTask instanceof TestGroupTask) {
                        String name = ((TestGroupTask)currentTask).getName();
                        if (currentTask instanceof TestCaseTask) {
                            error = new BuildException("Testcase " + name + " failed", ex);
                        } else if (failOnError()) {
                            error = new BuildException("Testgroup " + name + " failed", ex);
                        }
                    } else {
                        String name = "<unknown>";
                        if (currentTask != null) {
                            name = currentTask.getTaskName();
                        }
                        error = new BuildException("Task " + name + " failed", ex);
                    }
                    if (failOnError()) {
                        throw error;
                    }
                }
            }
        }
        if (error != null) {
            throw error;
        }
        if (warning != null) {
            throw warning;
        }
    }
    
    public String toString() {
        return "testgroup " + getName();
    }

    /**
     * Determines if the execution of this Task's children is interrupted, if
     * one of the children fails.
     *
     * @return  false. TestGroup's continue to execute children even if they fail.
     */
    protected boolean failOnError() {
        return false;
    }

    /**
     * A compare function to compare this with another
     * NestedSequential.
     * It calls similar on the nested unknown elements.
     *
     * @param other the nested sequential to compare with.
     * @return true if they are similar, false otherwise
     */
    public boolean similar(TestGroupTask other) {
        if (groupName == null && other.getName() != null || !groupName.equals(other.getName())) {
            return false;
        }
        if (children.size() != other.children.size()) {
            return false;
        }
        Iterator iter1 = children.iterator();
        Iterator iter2 = other.children.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
            Task t1 = (Task)iter1.next();
            Task t2 = (Task)iter2.next();
            if (t1 instanceof UnknownElement && t2 instanceof UnknownElement) {
                if (!((UnknownElement)t1).similar((UnknownElement)t2)) {
                    return false;
                }
            } else {
                if (t1.getRuntimeConfigurableWrapper() != t2.getRuntimeConfigurableWrapper()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private Task unwrapTask(Task task) {
        ProjectComponent comp = TaskRegistry.unwrapComponent(task);
        if (comp != task && comp instanceof Task) {
            return (Task)comp;
        }
        return task;
    }
}
