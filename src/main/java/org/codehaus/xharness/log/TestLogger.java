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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.MacroDef;
import org.apache.tools.ant.taskdefs.MacroInstance;
import org.apache.tools.ant.taskdefs.Parallel;
import org.apache.tools.ant.taskdefs.Sequential;

import org.codehaus.xharness.procutil.LoggableProcess;
import org.codehaus.xharness.tasks.IncludeTask;
import org.codehaus.xharness.tasks.ServiceDef;
import org.codehaus.xharness.tasks.ServiceInstance;
import org.codehaus.xharness.tasks.SkipTask;
import org.codehaus.xharness.tasks.TestGroupTask;

/**
 * BuildListener, that logs the output of a TestCase/TestGroup and upon completion of the
 * Task passes the result to the {@link ResultFormatter}.
 * A TestLogger can be active or inctive. It is initially active and remains so, until
 * a child TestLogger is created. When the Task of the child TestLogger is completed, the
 * child TestLogger re-activates it's parent. When a TestLogger is active, it creates
 * new child loggers whenever a child Task is executed. Upon completion of the
 * TestCase/TestGroup, the it is deactivated and it's parent is re-activated.
 *
 * @author Gregor Heine
 */
public class TestLogger extends TaskLogger {
    private Stack deferredTasks = new Stack();
    private LinkedList childLoggers = new LinkedList();
    private TestLogger parentLogger;
    private boolean active;
    private IDeferredLogger taskInDeferredShutdown;

    /**
     * Constructs a TestLogger with no reference.
     * @param registry The XHarness TaskRegistry
     * @param task The Task for this TestLogger.
     * @param name The name for this TestLogger.
     * @param parent The parent of this TestLogger. Must no be <code>null</code>.
     */
    public TestLogger(TaskRegistry registry,
            Task task,
            String name,
            TestLogger parent) {
        this(registry, task, name, parent, parent.getFullName(), null);
    }

    /**
     * Constructs a TestLogger with an optional reference.
     * @param registry The XHarness TaskRegistry
     * @param task The Task for this TestLogger.
     * @param name The name for this TestLogger.
     * @param parent The parent of this TestLogger. May be <code>null</code>.
     * @param parentName The (log-)name of the parent of this TestLogger.
     * @param reference An optional reference String denoting a fully quailified name of
     *                  another logger or <code>null</code>.
     */
    public TestLogger(TaskRegistry registry,
                      Task task,
                      String name,
                      TestLogger parent,
                      String parentName,
                      String reference) {
        super(registry, task, name, parentName, reference);
        this.parentLogger = parent;
        activate();
    }

    /**
     * Activates this TestLogger.
     */
    protected void activate() {
        this.active = true;
        if (getParent() != null) {
            getParent().deactivate(false);
        }
        getRegistry().setCurrentTest(this);
    }

    /**
     * Deactivates this TestLogger.
     *
     * @param activateParent If true, activates the logger's parent.
     */
    protected void deactivate(boolean activateParent) {
        this.active = false;
        if (activateParent && getParent() != null) {
            getParent().activate();
        }
    }

    /**
     * Get this logger's parent logger.
     *
     * @return The parent logger or <code>null</code>.
     */
    protected TestLogger getParent() {
        return parentLogger;
    }

    /**
     * Test if this TestLogger is currently active.
     *
     * @return true, if the logger is active, otherwise false.
     */
    protected final boolean isActive() {
        return active;
    }

    /**
     * Called when a new Task is started. If the TestLogger is active, it
     * creates a new child Logger, depending on the type of the specified Task.
     *
     * @param task The started Task.
     */
    protected void taskStartedInternal(Task task) {
        if (isActive()) {
            UnknownElement originalTask = null;
            if (task instanceof UnknownElement) {
                originalTask = (UnknownElement)task;
                try {
                    originalTask.maybeConfigure();
                    if (originalTask.getTask() != null) {
                        task = originalTask.getTask();
                    }
                } catch (Exception ex) {
                    Object obj = originalTask.getWrapper().getProxy();
                    if (obj instanceof Task && !(obj instanceof UnknownElement)) {
                        task = (Task)obj;
                    } else {
                        getLineBuffer().logLine(LogPriority.ERROR, getStackTrace(ex));
                    }
                }
            }
            getLineBuffer().logLine(LogPriority.VERBOSE, "Logging new Task " + task.getTaskName());

            TaskLogger newLogger = null;
            if (task instanceof IncludeTask) {
                // Don't log <include>
            } else if (task instanceof TaskAdapter
                    && ((TaskAdapter)task).getProxy() instanceof SkipTask) {
                // Don't log <skip>
            } else if (task instanceof Parallel || task instanceof Sequential) {
                // Don't log <parallel> and <sequential>
            } else if (task instanceof MacroDef || task instanceof MacroInstance) {
                // Don't log <macrodef> and macro instances
            } else if (task instanceof TestGroupTask) {
                String groupName = ((TestGroupTask)task).getName();
                if (groupName == null) {
                    groupName = task.getTaskName();
                }
                String taskName = genTaskName(groupName);
                newLogger = new TestLogger(getRegistry(), task, taskName, this);
            } else if (task instanceof LoggableProcess) {
                String taskName = genTaskName(task.getTaskName());
                newLogger = new ProcessLogger(getRegistry(), task, taskName, getFullName());
            } else if (task instanceof ServiceDef) {
                String serviceName = ((ServiceDef)task).getName();
                String taskName = genTaskName(serviceName);
                newLogger = new ServiceLogger(getRegistry(), task, taskName, this);
            } else if (task instanceof ServiceInstance) {
                String taskName = task.getTaskName();
                String serviceName;
                if ("service".equalsIgnoreCase(taskName)) {
                    serviceName = ((ServiceInstance)task).getReference();
                } else {
                    serviceName = taskName;
                }
                ServiceLogger serviceLogger = getService(serviceName);
                if (serviceLogger != null) {
                    serviceLogger.setContext(this, task);
                }
            } else {
                String taskName = genTaskName(task.getTaskName());
                newLogger = new TaskLogger(getRegistry(), task, taskName, getFullName(), null);
            }
            if (newLogger != null) {
                newLogger.setUnknownElement(originalTask);
                if (newLogger instanceof IDeferredLogger) {
                    addDeferredLogger((IDeferredLogger)newLogger);
                }
                addChildLogger(newLogger);
            }
        }
    }

    /**
     * Called when this logger's Task has finished. Stops all deferred elements
     * (process Tasks) that were started in the context of this logger, before
     * calling <code>taskFinishedInternal()</code> in the super class.
     */
    protected void taskFinishedInternal() {
        stopWatch.start();
        stopDeferredElements();
        super.taskFinishedInternal();
        childLoggers.clear();
        deactivate(true);
    }

    /**
     * Adds a new child logger to this TestLogger.
     *
     * @param child The child logger.
     */
    protected void addChildLogger(TaskLogger child) {
        if (child != null) {
            childLoggers.add(child);
        }
    }

    /**
     * Adds a new deferred logger (Logger of a process Task) to this TestLogger.
     *
     * @param logger The deferred logger.
     */
    protected void addDeferredLogger(IDeferredLogger logger) {
        deferredTasks.push(logger);
    }

    /**
     * Returns a list of all deferred loggers that were started in the context
     * of this logger.
     *
     * @return A List of {@link IDeferredLogger} instances.
     */
    protected List getDeferredLoggers() {
        return deferredTasks;
    }

    /**
     * Returns a ServiceLogger that was started in the context of this logger.
     *
     * @param name The name of the service.
     * @return The ServiceLogger or <code>null</code>.
     */
    protected ServiceLogger getService(String name) {
        if (name == null) {
            return null;
        }
        TaskLogger logger = getLogger(name);
        if (logger instanceof ServiceLogger) {
            return (ServiceLogger)logger;
        }
        if (getParent() != null) {
            return getParent().getService(name);
        }
        return null;
    }

    /**
     * Returns a TestLogger that wass started in the context of this logger.
     *
     * @param name The name of the TestLogger. May be an actual name or a decimal value
     *             (positive or negative) denoting the position of the TestLogger in
     *             the list of child loggers of this logger.
     * @return The TaskLogger or <code>null</code>.
     */
    protected TaskLogger getTask(String name) {
        if (name == null || "".equals(name)) {
            if (taskInDeferredShutdown != null && taskInDeferredShutdown instanceof TaskLogger) {
                return (TaskLogger)taskInDeferredShutdown;
            }
            name = "-1";
        }

        TaskLogger ret = null;
        try {
            int numId = Integer.parseInt(name);
            if (numId < 0) {
                numId = childLoggers.size() + numId;
            }
            numId--;
            if (numId >= 0 && numId < childLoggers.size() - 1) {
                ret = (TaskLogger)childLoggers.get(numId);
            }
        } catch (NumberFormatException nfe) {
            ret = getLogger(name);
        }
        return ret;
    }

    /**
     * Stops all deferred processes that were started in the context of this TestLogger.
     */
    protected void stopDeferredElements() {
        activate();
        while (!deferredTasks.empty()) {
            try {
                taskInDeferredShutdown = (IDeferredLogger)deferredTasks.pop();
                taskInDeferredShutdown.deferredShutdown();
            } catch (BuildException err) {
                setFailure(err);
            }
        }
        taskInDeferredShutdown = null;
    }

    /**
     * Generates a unique log-name for a given Task name.
     * If multiple Tasks with the same name are started in the context of this logger,
     * the task name is appended with a unique integer value, e.g. "_3".
     *
     * @param actualName The actual name of a Task.
     * @return The unique log name.
     */
    protected String genTaskName(String actualName) {
        if (getLogger(actualName) == null) {
            return actualName;
        }
        int iter = 1;
        String newName = null;
        do {
            newName = actualName + "_" + (iter++);
        } while (getLogger(newName) != null);
        return newName;
    }

    private TaskLogger getLogger(String name) {
        Iterator iter = childLoggers.iterator();
        while (iter.hasNext()) {
            TaskLogger logger = (TaskLogger)iter.next();
            if (name.equalsIgnoreCase(logger.getName())) {
                return logger;
            }
        }
        return null;
    }
}
