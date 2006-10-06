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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.Java;

import org.codehaus.xharness.exceptions.AssertionWarningException;
import org.codehaus.xharness.exceptions.TestSkippedException;
import org.codehaus.xharness.procutil.LoggableProcess;
import org.codehaus.xharness.tasks.ServiceDef;
import org.codehaus.xharness.tasks.ServiceGroupTask;
import org.codehaus.xharness.tasks.ServiceInstance;
import org.codehaus.xharness.tasks.TestCaseTask;
import org.codehaus.xharness.tasks.TestGroupTask;
import org.codehaus.xharness.tasks.XharnessTask;
import org.codehaus.xharness.util.StopWatch;

/**
 * BuildListener, that logs the output of one single Task and upon completion of the
 * Task passes the result to the {@link ResultFormatter}.
 * 
 * @author Gregor Heine
 */
public class TaskLogger implements BuildListener {
    protected final StopWatch stopWatch = new StopWatch(true);

    private TaskRegistry registry;
    private Task myTask; 
    private UnknownElement originalTask;
    private String taskName;
    private String parentName;
    private String taskReference;

    private LineBuffer lineBuffer = new LineBuffer();

    private int logId;
    private boolean actualTaskDetermined;
    private Throwable taskFailure;
    
    TaskLogger() {
        // for testing only!
    }
    
    /**
     * Constructs a TaskLogger instance.
     * 
     * @param reg The XHarness TaskRegistry
     * @param task The Task that is to be logged by this Logger or a placeholder Task 
     *             retrieved from an {@link org.apache.tools.ant.UnknownElement} if 
     *             the actual Task is not available yet.
     * @param name The name of the TaskLogger (usually some form of the Task name).
     * @param parent The fully wualified name of the Logger's parent logger (or <code>null</code>).
     * @param reference An optional reference String denoting a fully quailified name of 
     *                  another logger or <code>null</code>.
     */
    public TaskLogger(TaskRegistry reg, Task task, String name, String parent, String reference) {
        registry = reg;
        myTask = task;
        actualTaskDetermined = false;
        taskName = name;
        parentName = parent;
        taskReference = reference;
        
        logId = registry.getNextId();
        registry.getProject().addBuildListener(this);
    }

    // -- Accessors
    
    /**
     * Get the XHarness TaskRegistry for this test suite.
     * 
     * @return the TaskRegistry for this test suite.
     */
    public TaskRegistry getRegistry() {
        return registry;
    }
    
    /**
     * Get the Task for this TaskLogger. This may be the original Task passed into the
     * constructor, or (if the original Task was a placeholder and the actual Task
     * has been determined in the meantime) the retrieved actual Task. 
     * 
     * @return the Task of this TaskLogger.
     */
    public Task getTask() {
        return myTask;
    }
    
    /**
     * Get the {@link org.apache.tools.ant.UnknownElement} Task for the logged Task.
     * 
     * @return The UnknownElement of the logged Task.
     */
    public UnknownElement getUnknownElement() {
        return originalTask;
    }
    
    /**
     * Set the {@link org.apache.tools.ant.UnknownElement} Task for the logged Task.
     * 
     * @param ue The UnknownElement of the logged Task.
     */
    public void setUnknownElement(UnknownElement ue) {
        originalTask = ue;
    }
    
    /**
     * Get the name of the TaskLogger.
     * 
     * @return The name if this TaskLogger.
     */
    public String getName() {
        return taskName;
    }
    
    /**
     * Get the fully qualified name of the parent logger (e.g. "foo/bar").
     * 
     * @return The logger's parent name or <code>null</code>. 
     */
    public String getParentName() {
        return parentName;
    }
    
    /**
     * Get the fully qualified name of this TaskLogger. Returns the logger's name,
     * if the parent name is <code>null</code> or &lt;parent-name&gt;/&lt;logger-name&gt;
     * otherwise.
     * 
     * @return The fully qualified name of this logger.
     */
    public String getFullName() {
        if (parentName == null || "".equals(parentName)) {
            return taskName;
        }
        return parentName + "/" + taskName;
    }
    
    /**
     * Get the LineBuffer used to store output of the logger's Task.
     * 
     * @return The LineBuffer containing the Task's output.
     */
    public LineBuffer getLineBuffer() {
        return lineBuffer;
    }
    
    /**
     * Get the unique identifier of this logger.
     * 
     * @return the logger's Id.
     */
    public int getId() {
        return logId;
    }
    
    /**
     * Get the reference String.
     * 
     * @return The reference String or <code>null</code> if not set.
     */
    public String getReference() {
        return taskReference;
    }
    
    // -- BuildListener Implementation
    
    /**
     * BuildListener Implementation. Empty.
     * 
     * @param event The BuildEvent.
     */
    public final void buildStarted(BuildEvent event) {
    }

    /**
     * BuildListener Implementation. Empty.
     * 
     * @param event The BuildEvent.
     */
    public final void buildFinished(BuildEvent event) {
    }

    /**
     * BuildListener Implementation. Empty.
     * 
     * @param event The BuildEvent.
     */
    public final void targetStarted(BuildEvent event) {
    }

    /**
     * BuildListener Implementation. Empty.
     * 
     * @param event The BuildEvent.
     */
    public final void targetFinished(BuildEvent event) {
    }

    /**
     * BuildListener Implementation. Notification that a Task has started.
     * 
     * @param event The BuildEvent.
     */
    public final void taskStarted(BuildEvent event) {
        if (event != null && event.getTask() != null) {
            taskStartedInternal(event.getTask());
        }
    }

    /**
     * BuildListener Implementation. Notification that a Task has finished.
     * 
     * @param event The BuildEvent.
     */
    public final void taskFinished(BuildEvent event) {
        if (event != null && event.getTask() != null && isMyTask(event.getTask())) {
            if (!actualTaskDetermined) {
                Object o = event.getTask().getRuntimeConfigurableWrapper().getProxy();
                if (o instanceof Task) {
                    myTask = (Task)o;
                    actualTaskDetermined = true;
                }
            }
            setFailure(event.getException());
            taskFinishedInternal();
        }
    }

   /**
    * BuildListener Implementation. Notification that a Task has logged a message.
    * 
    * @param event The BuildEvent.
    */
    public final void messageLogged(BuildEvent event) {
        if (event != null 
            && event.getMessage() != null
            && isMyTask(event.getTask())
        ) {
            if (!actualTaskDetermined && event.getTask() != null) {
                myTask = event.getTask();
                actualTaskDetermined = true;
            }
            messageLoggedInternal(event.getPriority(), event.getMessage());
        }
    }
    
    // --- Logging internal impl
    
    /**
     * Called when a new Task is started. This implementation does nothing.
     * May be overridden in subclasses.
     * 
     * @param task The Task that has been started.
     */
    protected void taskStartedInternal(Task task) {
        // Do nothing
    }
    
    /**
     * Called when this logger's Task has logged a message. 
     * 
     * @param eventPrio The priority of the log event.
     * @param message The log message.
     */
    protected void messageLoggedInternal(int eventPrio, String message) {
        lineBuffer.logLine(mapEventToLogPriority(eventPrio), message);
    }
    
    /**
     * Called when this logger's Task has finished. Passes the result of the Task
     * (output, possible Exception, return value, execution time, etc.) to
     * the ResultFormatter.
     */
    protected void taskFinishedInternal() {
        getRegistry().getProject().removeBuildListener(this);
        stopWatch.stop();
        
        int result;
        String resultDescription;
        if (taskFailure == null) {
            result = Result.PASSED;
            resultDescription = "";
        } else if (taskFailure instanceof TestSkippedException) {
            if (((TestSkippedException)taskFailure).skippedByPattern()) {
                // don't write log
                return;
            }
            result = Result.SKIPPED;
            resultDescription = getFailureDescription("Skipped");
        } else if (taskFailure instanceof AssertionWarningException) {
            result = Result.WARNING;
            resultDescription = getFailureDescription("Warning");
            logFailure();
        } else {
            result = Result.FAILED;
            resultDescription = getFailureDescription("Failed");
            logFailure();
        }
        getRegistry().getFormatter().writeResults(this, 
                                                  result, 
                                                  resultDescription, 
                                                  stopWatch.getTime());
    }
    
    /**
     * Creates a full stack trace of an Exception.
     * 
     * @param failure The exception.
     * @return The stack trace.
     */
    protected static String getStackTrace(Throwable failure) {
        if (failure != null) {
            StringWriter sw = new StringWriter();
            failure.printStackTrace(new PrintWriter(sw, true));
            return sw.toString();
        }
        return "";
    }

    // --- protected accessors and setters
    
    /**
     * Returns a result identifier for the logger's Task.
     * 
     * @see Result
     * @return The result identifier for the Task.
     */
    protected int getTaskType() {
        if (myTask instanceof TestCaseTask) {
            return Result.TESTCASE;
        } else if (myTask instanceof XharnessTask) {
            return Result.XHARNESS;
        } else if (myTask instanceof ServiceGroupTask) {
            if ("start".equals(myTask.getTaskName())) {
                return Result.START;
            } else if ("stop".equals(myTask.getTaskName())) {
                return Result.STOP;
            } else {
                return Result.VERIFY;
            }
        } else if (myTask instanceof TestGroupTask) {
            return Result.TESTGROUP;
        } else if (myTask instanceof ServiceDef || myTask instanceof ServiceInstance) {
            return Result.SERVICE;
        } else if (myTask instanceof ExecTask || myTask instanceof Java) {
            return Result.PROCESS_TASK;
        } else {
            return Result.OTHER_TASK;
        }
    }

    /**
     * Returns the owner string, if the logger's Task is a TestCase, otherwise
     * an empty String.
     * 
     * @return The Owner of the TestCase or "".
     */
    protected String getOwner() {
        if (myTask instanceof TestCaseTask) {
            return ((TestCaseTask)myTask).getOwner();
        }
        return "";
    }

    /**
     * Returns the process command, if the logger's Task is a LoggableProcess, otherwise
     * an empty String.
     * 
     * @see XsExecTask
     * @see XsJavaTask
     * @return The process command or "".
     */
    protected String getCommand() {
        if (myTask instanceof LoggableProcess) {
            return ((LoggableProcess)myTask).getCommandline();
        }
        return "";
    }
    
    /**
     * Returns the process return value, if the logger's Task is a LoggableProcess, 
     * otherwise an empty String.
     * 
     * @see XsExecTask
     * @see XsJavaTask
     * @return The process return value or "".
     */
    protected int getRetVal() {
        if (myTask instanceof LoggableProcess) {
            return ((LoggableProcess)myTask).getReturnValue();
        }
        return 0;
    }
    
    /**
     * Get the Exception caused by this logger's Task.
     * 
     * @return The Task Exception or <code>null</code>.
     */
    protected Throwable getFailure() {
        return taskFailure;
    }
    
    /**
     * Set the Exception caused by this logger's Task.
     * 
     * @param t The Task Exception.
     */
    protected void setFailure(Throwable t) {
        if (t !=  null && taskFailure == null) {
            taskFailure = t;
        }
    }
    
    // --- other protected methods

    /**
     * Maps the ant event priority value to an XHarness log priority.
     * 
     * @param eventPrio The event priority value.
     * @return The log priority value.
     */
    protected int mapEventToLogPriority(int eventPrio) {
        switch (eventPrio) {
            case Project.MSG_ERR:     
                return LogPriority.ERROR;   
            case Project.MSG_WARN:    
                return LogPriority.WARNING; 
            case Project.MSG_INFO:    
                return LogPriority.INFO;    
            case Project.MSG_VERBOSE: 
                return LogPriority.VERBOSE; 
            case Project.MSG_DEBUG:   
            default:
                return LogPriority.DEBUG;   
        }
    }
    
    /**
     * Tests, if the specified Task is the Task of this logger.
     * 
     * @param task The Task to test.
     * @return true, if the Task is this logger's Task, otherwise false.
     */
    protected boolean isMyTask(Task task) {
        if (task == null) {
            return myTask instanceof TaskAdapter;
        } 
        return sameTask(myTask, task) || sameTask(originalTask, task);
    }
    
    protected boolean sameTask(Task t1, Task t2) {
        return t1 == t2 
            || t1 != null 
            && t2 != null 
            && t1.getRuntimeConfigurableWrapper() == t2.getRuntimeConfigurableWrapper();
    }
    
    private String getFailureDescription(String defaultMsg) {
        if (taskFailure.getMessage() == null) {
            return defaultMsg + " (unknown reason)";
        } else {
            return taskFailure.getMessage();
            
        }
    }
    
    private void logFailure() {
        if (taskFailure.getMessage() == null) {
            lineBuffer.logLine(LogPriority.ERROR, getStackTrace(taskFailure));
        } else {
            lineBuffer.logLine(LogPriority.ERROR, taskFailure.getMessage());
        }
    }
}
