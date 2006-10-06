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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;

import org.codehaus.xharness.exceptions.ServiceVerifyException;
import org.codehaus.xharness.procutil.BgProcess;
import org.codehaus.xharness.tasks.ServiceDef;
import org.codehaus.xharness.tasks.ServiceGroupTask;
import org.codehaus.xharness.tasks.ServiceVerifyTask;


/**
 * BuildListener, that logs the output of XHarness ServiceDef Tasks and their instances.
 *
 * @author Gregor Heine
 */
public class ServiceLogger extends TestLogger implements IDeferredLogger {
    private TestLogger currentTest;
    private Task currentInstance;
    
    /**
     * Constructs a ServiceLogger.
     * @param registry The XHarness TaskRegistry
     * @param task The Task for this TestLogger.
     * @param name The name for this TestLogger.
     * @param parent The parent of this TestLogger. Must no be <code>null</code>.
     */
    public ServiceLogger(TaskRegistry registry, 
                         Task task, 
                         String name, 
                         TestLogger parent) {
        super(registry, task, name, parent);
    }

    /**
     * Sets the context of a test logger in which child Tasks are executed.
     * Since a ServiceLogger logs both ServiceDef Tasks as well as their instances,
     * the context logger creates a connection between a Service and the
     * point where the start, verify and stop elements of the ServiceDef
     * are actually executed. 
     * 
     * @param context The context logger.
     * @param instanceTask The ServiceInstance Task.
     */
    public void setContext(TestLogger context, Task instanceTask) {
        stopWatch.start();
        currentTest = context;
        currentTest.deactivate(false);
        currentInstance = instanceTask;
        activate();
    }
    
    /**
     * Called when a new Task is started. If the ServiceLogger is active and the
     * child Task is a ServicegroupTask of a start, verify or stop group, it will
     * start a new child Logger.
     * 
     * @param task The started Task.
     */
    protected void taskStartedInternal(Task task) {
        if (isActive()) {
            UnknownElement originalTask = null;
            if (task instanceof UnknownElement) {
                originalTask = (UnknownElement)task;
                originalTask.maybeConfigure();
                task = originalTask.getTask();
            }
            
            TaskLogger newLogger = null;
            if (task instanceof ServiceGroupTask) {
                if ("start".equals(task.getTaskName()) && currentTest != null) {
                    newLogger = handleStart(task);
                } else if ("verify".equals(task.getTaskName()) && currentTest != null) {
                    newLogger = handleVerify(task);
                } else if (ServiceVerifyTask.DUMMY.equals(task.getTaskName()) 
                        && currentTest != null) {
                    newLogger = handleVerifyDummy(task);
                } else if ("stop".equals(task.getTaskName())) {
                    newLogger = handleStop(task);
                } else {
                    getLineBuffer().logLine(
                            LogPriority.WARNING, 
                            "Service has invalid nested Task "
                            + task.getTaskName() + " (" + task + ")");
                }
            } else {
                getLineBuffer().logLine(
                        LogPriority.WARNING, 
                        "Service has invalid nested Task "
                        + task.getTaskName() + " (" + task + ")");
            }
            addChildLogger(newLogger);
        }
    }
    
    /**
     * Called when this logger's Task has finished. Because the child elements
     * of a ServiceDef Task are only executed at a later stage, the logger
     * does not publish it's result until  {@link #deferredShutdown()} is called.
     */
    protected void taskFinishedInternal() {
        if (((ServiceDef)getTask()).wasStopped()) {
            stopDeferredElements();
        }
        if (currentTest != null) {
            deactivate(false);
            currentTest.activate();
            currentTest = null;
        } else {
            deactivate(true);
        }
        stopWatch.stop();
    }
    
    /**
     * Shuts down the service by executing the ServiceDef's stop element and 
     * flushes the service's result and output to the ResultFormatter.
     * 
     * @throws BuildException If the service finished with an error.
     */
    public void deferredShutdown() throws BuildException {
        stopWatch.start();
        activate();
        
        try {
            ((ServiceDef)getTask()).stop();
        } catch (BuildException exc) {
            setFailure(exc);
        }
        super.taskFinishedInternal();
        deactivate(true);
        Throwable taskFailure = getFailure();
        if (taskFailure != null) {
            if (taskFailure instanceof BuildException) {
                throw (BuildException)taskFailure;
            }
            throw new BuildException(taskFailure);
        }
    }
    
    /**
     * Sets a failure that occurred while executing the service.
     * 
     * @param t The failure.
     */
    protected void setFailure(Throwable t) {
        if (!(t instanceof ServiceVerifyException)) {
            super.setFailure(t);
        }
    }

    /**
     * Tests if the given Task is the logger's ServiceDef Task or
     * an instance of it.
     * 
     * @param task The task to test.
     * @return true if the Task is the logger's ServideDef Task or an instance of it,
     *              otherwise false.
     */
    protected boolean isMyTask(Task task) {
        return super.isMyTask(task)
            || sameTask(currentInstance, task);
    }
    
    private TaskLogger handleStart(Task task) {
        SvcsStartLogger logger = new SvcsStartLogger(
                    getRegistry(), 
                    task, 
                    genTaskName("start"),
                    this,
                    currentTest.getFullName());
        addDeferredLogger(logger);            
        new LinkLogger(getRegistry(), 
                task, 
                "Start_" + getName(), 
                currentTest.getFullName(), 
                logger.getFullName());
        return logger;
    }
    
    private TaskLogger handleVerify(Task task) {
        return new TestLogger(
                getRegistry(), 
                task, 
                "verify_" + getName(),
                this, 
                currentTest.getFullName(),
                getFullName());
    }
    
    private TaskLogger handleVerifyDummy(Task task) {
        TaskLogger ret = handleVerify(task);
        SvcsStartLogger startLogger = null;
        try {
            startLogger = (SvcsStartLogger)getDeferredLoggers().get(0);
        } catch (Exception ex) {
            throw new BuildException(ex);
        }
        
        Iterator iter = startLogger.getDeferredLoggers().iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o instanceof ProcessLogger) {
                ProcessLogger pLogger = (ProcessLogger)o;
                if (pLogger.getTask() instanceof BgProcess) {
                    BgProcess proc = (BgProcess)pLogger.getTask();
                    if (!proc.isRunning()) {
                        BuildException be = new BuildException(
                                "Process @" + pLogger.getFullName() + "@ has stopped running");
                        ((ServiceVerifyTask)task).setException(be);
                        return ret;
                    }
                }
            }
        }
        return ret;
    }
    
    private TaskLogger handleStop(Task task) {
        String parentName = currentTest == null ? getParentName() : currentTest.getFullName();
        TaskLogger logger = new TestLogger(
                getRegistry(), 
                task, 
                genTaskName("stop"), 
                this,
                getFullName(),
                parentName);
        new LinkLogger(getRegistry(), task, "Stop_" + getName(), parentName, logger.getFullName());
        return logger;
    }
}
