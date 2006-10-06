/*
 * Created on 01-Feb-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.xharness.testutil;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author greheine
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ProcessTester {
    private static final int MAX_PASSED_WAIT_SECS = 6;
    private boolean passed;
    private int port;
    private String receivedData;
    private Socket sock;
    private Object mutex = new Object();

    public ProcessTester() throws Exception {
        passed = false;
        ServerSocket ssock = new ServerSocket(0);
        port = ssock.getLocalPort();
        Thread t = new SocketThread(ssock);
        t.setDaemon(true);
        t.start();
    }

    public static void main(String[] args) {
        System.out.println("Welcome stdout!");
        System.err.println("Welcome stderr!");
        int port = 0;
        int timeout = 0;
        boolean fail = false;
        String sendProperty = null;
        for (int i = 0; i < args.length - 1; i++) {
            if ("-p".equals(args[i])) {
                port = Integer.parseInt(args[++i]);
            } else if ("-t".equals(args[i])) {
                timeout = Integer.parseInt(args[++i]);
            } else if ("-s".equals(args[i])) {
                sendProperty = args[++i];
            } else if ("-f".equals(args[i])) {
                fail = true;
            }
        }
        if (port <= 0) {
            System.err.println("Wrong or missing port.");
        }
        try {
            System.out.println("Connecting to port " + port);
            final Socket sock = new Socket("localhost", port);
            if (sendProperty != null) {
                String propVal = System.getProperty(sendProperty);
                if (propVal != null) {
                    sock.getOutputStream().write(propVal.getBytes());
                }
            }
            if (timeout > 0) {
                System.out.println("...connected! Wating now for " + timeout + " seconds.");
                Thread t = new Thread() {
                    public void run() {
                        try {
                            sock.getInputStream().read(new byte[100]);
                        } catch (Exception ex) {
                            // ignore
                        }
                        System.out.println("Socket closed");
                        synchronized (sock) {
                            sock.notify();
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
                synchronized (sock) {
                    sock.wait(timeout * 1000);
                    sock.close();
                }
            }
            System.out.println("...done. Exiting.");
        } catch (Exception e) {
            System.err.println("Caught unexecpted exception: " + e.toString());
            e.printStackTrace();
            System.exit(1);
        }
        if (fail) {
            System.exit(1);
        }
    }

    public boolean passed() {

        synchronized (mutex) {
            int count = 0;
            while (!passed && count++ < MAX_PASSED_WAIT_SECS) {
                try {
                    mutex.wait(count * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            boolean ret = passed;
            passed = false;
            return ret;
        }
    }

    public int getPort() {
        return port;
    }

    public Socket getSocket() {
        return sock;
    }

    public String getReceivedData() {
        return receivedData;
    }

    private class SocketThread extends Thread {
        private ServerSocket ssocket;

        public SocketThread(ServerSocket ssock) {
            ssocket = ssock;
        }

        public void run() {
            while (true) {
                try {
                    sock = ssocket.accept();
                } catch (Exception e) {
                    return;
                }
                try {
                    byte[] data = new byte[1000];
                    int bytes = sock.getInputStream().read(data);
                    if (bytes > 0) {
                        receivedData = new String(data, 0, bytes);
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                }
                synchronized (mutex) {
                    passed = true;
                    mutex.notify();
                }
            }
        }
    }
}
