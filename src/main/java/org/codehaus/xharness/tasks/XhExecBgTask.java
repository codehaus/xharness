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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.types.Commandline;

import org.codehaus.xharness.procutil.BgProcess;
import org.codehaus.xharness.procutil.ProcessRegistry;

/**
 * @author Gregor Heine
 */
public class XhExecBgTask extends XhExecTask implements Runnable, BgProcess {
    // 2 hours default timeout - should be long enough ;)
    private static long defaultTimeout = 2 * 60 * 60 * 1000;

    private Object mutex = new Object();
    private BuildException processException = null;
    private String processName = null;
    private int postKillTimeout = 20;
    private int preKillTimeout = 0;
    private boolean isRunning = false;
    private Sequential afterwards;
    private File launcherArgsFile = null;
    private String launchedCommandLine = null;
    private Launcher launcher = null;

    /**
     * Constructor.
     */
    public XhExecBgTask() {
        super();
        setTimeout(new Long(defaultTimeout));
    }

    /**
     * Set the timeout for waiting until the process has been killed.
     * 
     * @param timeout
     *            kill timeout in seconds
     */
    public void setKilltimeout(int timeout) {
        if (timeout > 0) {
            postKillTimeout = timeout;
        } else {
            // must wait at least 1sec. for process to terminate
            postKillTimeout = 1;
        }
    }

    /**
     * Set the timeout to kill a process if it has not been terminated
     * gracefully.
     * 
     * @param timeout
     *            kill timeout in seconds
     */
    public void setPrekilltimeout(int timeout) {
        if (timeout >= 0) {
            preKillTimeout = timeout;

        }
    }

    public String getProcessName() {
        return processName;
    }
    
    public void setProcessName(String pname) {
        processName = pname;
    }
    
    public void setUseLauncher(boolean useLauncher) {
        if (useLauncher) {
            setSearchPath(true);
            launcher = new Launcher(this);
        }
    }

    public String getCommandline() {
        String ret = super.getCommandline();
        if (launchedCommandLine != null) {
            ret +=  " [" + launchedCommandLine + "]";
        }
        return ret;
    }

    public void addAfterwards(Sequential tasks) {
        afterwards = tasks;
    }

    /**
     * Test, if the process is running.
     * 
     * @return true, iff the process is running, otherwise false.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Kill the process if it is running. If the process can not be killed or
     * the execution failed previously with an exception, a BuildException is
     * thrown, containing the information about the cause. The
     * mutex.wait(prekillTimeout) will wait for the specified period for the
     * thread to die. If it has not terminated gracefully before that time it
     * will be killed.
     * 
     * @throws BuildException If an error occurrs while killing the process.
     */
    public void kill() throws BuildException {
        synchronized (mutex) {
            try {
                if (isRunning && getWatchdog() != null) {
                    boolean deliberateKill = true;
                    if (preKillTimeout > 0) {
                        waitForTermination(preKillTimeout, 
                                           "Waiting for Process to die before kill...");
                        deliberateKill = false;
                    }
                    
                    if (launcher != null) {
                        launcher.shutdown();
                        try {
                            mutex.wait(2000); // wait for the launcher to die
                        } catch (Exception e) {
                            // ignore
                        }
                        if (launcherArgsFile != null && launcherArgsFile.exists()) {
                            launcherArgsFile.delete();
                        }
                    }
                    
                    if (isRunning) {
                        getWatchdog().killProcess(deliberateKill);
                        waitForTermination(postKillTimeout, 
                                           "Waiting for Process to die after kill...");
                    }
                    if (isRunning) {
                        log("Process is still running!", Project.MSG_WARN);
                        throw new BuildException("Unable to kill process!");
                    }
                }
            } finally {
                if (processName != null) {
                    try {
                        ProcessRegistry.unregisterProcess(processName);
                    } catch (BuildException be) {
                        // ignore
                    }
                }
            }
            if (processException != null) {
                log(processException.toString(), Project.MSG_ERR);
                throw processException;
            }
        }
        if (afterwards != null) {
            try {
                afterwards.execute();
            } catch (BuildException be) {
                log("Nested task failed: " + be, Project.MSG_WARN);
                throw new BuildException("Nested task failed.");
            }
        }
    }

    /**
     * Do the execution of this Task.
     * 
     * @throws BuildException If an error occurs while executing this task.
     */
    public void execute() throws BuildException {
        log("Running " + getTaskName() + " in background", Project.MSG_VERBOSE);

        if (launcher != null) {
            launcher.start();
        }

        Thread asyncThread = new Thread(this);
        asyncThread.setDaemon(true);

        synchronized (mutex) {
            isRunning = true;
            try {
                asyncThread.start();
            } catch (OutOfMemoryError oome) {
                Runtime rt = Runtime.getRuntime();
                ThreadGroup tg = Thread.currentThread().getThreadGroup();

                log("Fatal error: unable to create new Thread: " + oome + "\n"
                        + "Trying recovery....\n" + "Before: total="
                        + rt.totalMemory() + ",free=" + rt.freeMemory()
                        + ",threads=" + tg.activeCount(), Project.MSG_ERR);

                isRunning = false;
                throw oome;
            }

            // wait 1 second to start process
            //
            try {
                mutex.wait(1000);
            } catch (Exception e) {
                //ignore
            }

            if (launcher != null) {
                try {
                    launcher.join();
                } catch (InterruptedException ie) {
                    // ignore
                }
                BuildException launcherEx = launcher.getException();
                if (launcherEx != null) {
                    throw launcherEx;
                }
            }

            if (processException != null) {
                log("Failed to execute process " + getCommandline()
                    + "\n" + processException.toString(), 
                    Project.MSG_WARN);
                throw processException;
            } else if (processName != null) {
                ProcessRegistry.registerProcess(processName, this);
            }
        }
    }

    /**
     * Runnable implementation. Executes the process in a separate Thread.
     */
    public void run() {
        try {
            super.execute();
        } catch (BuildException ex) {
            if (getWatchdog() == null || !getWatchdog().killedDeliberately()) {
                processException = ex;
            }
        }

        synchronized (mutex) {
            isRunning = false;
            try {
                mutex.notify();
            } catch (Exception e) {
                //ignore
            }
            
            if (processName != null) {
                try {
                    ProcessRegistry.unregisterProcess(processName);
                } catch (BuildException be) {
                    // ignore
                }
            }
        }
    }

    /**
     * Setup the executable name and the directory to run the process in.
     * Prepend the path to the executable if required. Make sure the
     * process control perl script is the executable name.
     */
    protected void setupExecutableAndDir() {
        super.setupExecutableAndDir();
        if (launcher == null) {
            return;
        }

        StringBuffer buffer = new StringBuffer();
        try {
            launcherArgsFile = File.createTempFile("xharness", ".txt");
            launcherArgsFile.deleteOnExit();
            OutputStream os = new BufferedOutputStream(new FileOutputStream(launcherArgsFile));
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, "US-ASCII"));
            pw.println(defaultTimeout / 1000);
            pw.println(launcher.getPort());
            pw.println(getExecutable());
            buffer.append(getExecutable());
            Iterator i = getArguments().iterator();
            while (i.hasNext()) {
                Object obj = i.next();
                String[] parts = null;
                if (obj instanceof Commandline.Argument) {
                    Commandline.Argument arg = (Commandline.Argument)obj;
                    parts = arg.getParts();
                } else if (obj instanceof Commandline) {
                    Commandline cmd = (Commandline)obj;
                    parts = cmd.getArguments();
                }
                if (parts != null) {
                    for (int j = 0; j < parts.length; j++) {
                        pw.println(parts[j]);
                        buffer.append(" ");
                        buffer.append(parts[j]);
                    }
                }
            }
            getArguments().clear();
            pw.close();
            os.close();
        } catch (Exception ex) {
            log("***** unable to create arguments file: " + ex, Project.MSG_ERR);
            throw new BuildException("Unableto create laucher arguments file "
                                   + launcherArgsFile + ": " + ex);
        }

        launchedCommandLine = buffer.toString();
        Commandline.Argument arg1 = new Commandline.Argument();
        getArguments().add(0, arg1);
        arg1.setValue(launcherArgsFile.getAbsolutePath());
        String launcherName = launcher.execName();
        log("Setting executable to " + launcherName, Project.MSG_VERBOSE);
        setExecutable(launcherName);
        setResolveExecutable(true);
    }
    
    private void waitForTermination(int maxSecs, String logMsg) {
        int count = 0;
        while (isRunning && count++ < maxSecs) {
            log(logMsg, Project.MSG_VERBOSE);
            try {
                mutex.wait(1000);
            } catch (Exception e) {
                //ignore
            }
        }
        
    }
}

class Launcher extends Thread {
    private static String scriptFileName = null;

    private BuildException failure = null;
    private ServerSocket ssocket = null;
    private Socket socket = null;
    private XhExecBgTask task;

    public Launcher(XhExecBgTask t) throws BuildException {
        super();
        task = t;
        prepareScripts();
        try {
            InetAddress addr = InetAddress.getByName("127.0.0.1");
            ssocket = new ServerSocket(0, 0, addr);
        } catch (Exception e) {
            if (ssocket != null) {
                try {
                    ssocket.close();
                } catch (Exception ex) {
                    // ignore
                }
                ssocket = null;
            }
            String err = "Failed to establish socket for \"" + execName() + "\"";
            String msg = e.getMessage();
            if (msg != null) {
                err = err + ": " + msg;
            } else {
                err = err + ": " + e;
            }
            task.log(err, Project.MSG_ERR);
            throw new BuildException(err);
        }
    }

    public void run() {
        // wait for the launcher script to talk to us
        //
        try {
            ssocket.setSoTimeout(30 * 1000); // 30 seconds
            socket = ssocket.accept();
            socket.setSoTimeout(3 * 60 * 1000);
            socket.setTcpNoDelay(true);
        } catch (SocketTimeoutException stmo) {
            String msg = "timeout error on initial connection from " + execName();
            task.log(msg, Project.MSG_ERR);
            failure = new BuildException(msg);
        } catch (Exception e) {
            String err = "error establishing communication with " + execName();
            String msg = e.getMessage();
            if (msg != null) {
                err = err + ": " + msg;
            } else {
                err = err + ": " + e;
            }
            task.log(err, Project.MSG_ERR);
            failure = new BuildException(err);
        }
    }

    public int getPort() {
        if (ssocket != null) {
            return ssocket.getLocalPort();
        } else {
            return -1;
        }
    }

    // You can call getException() anytime, but for it to be meaningful,
    // you need to call it after calling start() and then join() on
    // a Launcher object.
    //
    public BuildException getException() {
        return failure;
    }

    public void shutdown() throws BuildException {
        PrintWriter pw = null;
        BufferedReader in = null;
        try {
            pw = new PrintWriter(socket.getOutputStream(), true);
            pw.println("die");
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            in.readLine();
        } catch (Exception ex) {
            // Ignore any exceptions here. Exceptions are typically
            // the result of race conditions between
            // async_exec_launcher shutting down due to child process
            // death and us trying at the same time to tell it to shut
            // down. It would be nice to be able to distinguish
            // between actual comm failures that prevent us from
            // properly interacting with async_exec_launcher and comm
            // failures due to socket shutdown, but it's really not
            // possible given the current design and structure of the
            // code, plus it's not that big of a problem given that
            // we're limited to localhost loopback communications.
            //
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            } catch (Exception e) {
                // ignore
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
                // ignore
            }
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                // ignore
            }
            try {
                if (ssocket != null) {
                    ssocket.close();
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public String execName() {
        return scriptFileName;
    }
    
    private void prepareScripts() {
        synchronized (getClass()) {
            if (scriptFileName == null) {
                File sysTmpDir = new File(System.getProperty("java.io.tmpdir", "."));
                File launcherTempDir = null;
                long count = System.currentTimeMillis();
                do {
                    launcherTempDir = new File(sysTmpDir, "xharness" + (count++) + ".tmp");
                } while (launcherTempDir.exists());
                launcherTempDir.mkdirs();
                Thread cleanUp = new CleanupThread(launcherTempDir);
                Runtime.getRuntime().addShutdownHook(cleanUp);

                File scriptFile = null;
                if (System.getProperty("os.name", "").toLowerCase().startsWith("win")) {
                    copyResource(task, "bin", "async_exec_launcher_win32.pl", launcherTempDir);
                    scriptFile = new File(launcherTempDir, "async_exec_launcher.bat");

                    // create launcher batch file
                    //
                    try {
                        OutputStream os = new FileOutputStream(scriptFile);
                        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, "US-ASCII"));
                        pw.println("@echo off");
                        File f = new File(launcherTempDir, "async_exec_launcher_win32.pl");
                        pw.print("if not \"%CCPERL%\"==\"\" %CCPERL% ");
                        pw.print(f.getAbsolutePath());
                        pw.println(" %*");
                        pw.print("if \"%CCPERL%\"==\"\" perl.exe ");
                        pw.print(f.getAbsolutePath());
                        pw.println(" %*");
                        pw.close();
                        os.close();
                    } catch (IOException ioe) {
                        task.log("Unable to write batch file "
                                 + scriptFile.getAbsolutePath() + ": " + ioe,
                                 Project.MSG_WARN);
                    }
                } else {
                    copyResource(task, "bin", "async_exec_launcher", launcherTempDir);
                    scriptFile = new File(launcherTempDir, "async_exec_launcher");

                    // make script executable
                    //
                    try {
                        Process proc = Runtime.getRuntime().exec(
                            new String[] {"chmod", "775", scriptFile.getAbsolutePath()}
                        );
                        proc.waitFor();
                    } catch (Exception ex) {
                        task.log("Unable to set execute file permission on laucher script "
                                 + scriptFile.getAbsolutePath() + ": " + ex,
                                 Project.MSG_WARN);
                    }
                }

                try {
                    scriptFileName = scriptFile.getCanonicalPath();
                } catch (IOException ioe) {
                    scriptFileName = scriptFile.getAbsolutePath();
                }
            }
        }
    }
    
    private static void copyResource(Task task, String dir, String file, File toDir) {
        InputStream is = null;
        OutputStream os = null;
        String resourceName = "/" + dir + "/" + file;
        task.log("Copying resource " + resourceName + " to " + toDir.getAbsolutePath(),
                Project.MSG_DEBUG);
        try {
            is = new BufferedInputStream(
                    XhExecTask.class.getResourceAsStream(resourceName));
            os = new BufferedOutputStream(new FileOutputStream(new File(toDir, file)));
            for (int data = is.read(); data >= 0; data = is.read()) {
                os.write(data);
            }
        } catch (Exception ex) {
            task.log("Unable to copy resource " + resourceName
                     + " to " + toDir.getAbsolutePath() + ": "  + ex,
                     Project.MSG_WARN);
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException ioe) { /*ignore*/ }
            }
            if (os != null) {
                try { os.close(); } catch (IOException ioe) { /*ignore*/ }
            }
        }
    }
    
    private static final class CleanupThread extends Thread {
        private File dir;
        
        public CleanupThread(File f) {
            dir = f;
        }
        
        public void run() {
            remove(dir);
        }
        
        private void remove(File f) {
            if (f.exists()) {
                if (f.isDirectory()) {
                    File[] children = f.listFiles();
                    for (int i = 0; i < children.length; i++) {
                        remove(children[i]);
                    }
                }
                f.delete();
            }
        }
    }
}
