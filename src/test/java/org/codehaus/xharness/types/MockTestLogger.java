package org.codehaus.xharness.types;

import org.codehaus.xharness.log.TaskLogger;
import org.codehaus.xharness.log.TaskRegistry;
import org.codehaus.xharness.log.TestLogger;

public class MockTestLogger extends TestLogger {
    private TaskLogger taskLogger;
    
    public MockTestLogger(TaskRegistry registry, TaskLogger logger) {
        super(registry, null, null, null, null, null);
        taskLogger = logger;
    }
    
    public TaskLogger getTask(String name) {
        return taskLogger;
    }
}
