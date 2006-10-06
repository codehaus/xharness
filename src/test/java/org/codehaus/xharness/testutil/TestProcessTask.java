package org.codehaus.xharness.testutil;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.codehaus.xharness.log.LineBuffer;
import org.codehaus.xharness.procutil.BgProcess;
import org.codehaus.xharness.procutil.LoggableProcess;

public class TestProcessTask extends Task implements LoggableProcess, BgProcess {
    private LineBuffer buffer;
    private int outPrio = Integer.MIN_VALUE;
    private int errPrio = Integer.MIN_VALUE;
    private boolean killCalled = false;
    private BuildException killException = null;
    private boolean isRunning = true;
    
    public TestProcessTask() {
    }
    
    public TestProcessTask(BuildException ex) {
        killException = ex;
    }
    
    public TestProcessTask(boolean running) {
        isRunning = running;
    }
    
    public int getReturnValue() {
        return 0;
    }
    
    public void enableLogging(LineBuffer buf, int out, int err) {
        buffer = buf;
        outPrio = out;
        errPrio = err;
    }
    
    public String getCommandline() {
        return "";
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void kill() {
        killCalled = true;
        if (killException != null) {
            throw killException;
        }
    }
    
    public void setKilltimeout(int t) {
        
    }
    
    public void setPrekilltimeout(int t) {
        
    }
    
    public String getProcessName() {
        return "";
    }
    
    public void setProcessName(String s) {
    }
    
    public LineBuffer getBuffer() {
        return buffer;
    }
    
    public int getOutPrio() {
        return outPrio;
    }
    
    public int getErrPrio() {
        return errPrio;
    }
    
    public boolean killCalled() {
        return killCalled;
    }
}
