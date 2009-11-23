package org.codehaus.xharness.procutil;

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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.codehaus.xharness.tasks.XhExecBgTask;
import org.codehaus.xharness.tasks.XhExecTask;

public class ScriptLauncher extends Thread {
    private static String scriptFileName = null;

    private BuildException failure = null;
    private ServerSocket ssocket = null;
    private Socket socket = null;
    private XhExecBgTask task;

    public ScriptLauncher(XhExecBgTask t) throws BuildException {
        super();
        task = t;
        prepareScripts();
        try {
            InetAddress addr = InetAddress.getByName(null);
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
