package org.codehaus.xharness.types;

import org.codehaus.xharness.log.TaskRegistry;

public class MockTaskRegistry extends TaskRegistry {
    protected static void reset() {
        TaskRegistry.reset();
    }
}
