package org.codehaus.xharness.testutil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class TempDir {
    private static CleanupThread cleaner;
    private static final File SYS_TEMP_DIR = new File(System.getProperty("java.io.tmpdir", "."));
    private static final String PREFIX = "xharnesstmp";
        
    private TempDir() {
    }
    
    public static File createTempDir() {
        File tempDir = null;
        long count = System.currentTimeMillis();
        do {
            tempDir = new File(SYS_TEMP_DIR, PREFIX + (count++));
        } while (tempDir.exists());
        tempDir.mkdirs();
        if (cleaner == null) {
            cleaner = new CleanupThread();
            Runtime.getRuntime().addShutdownHook(cleaner);
        }
        cleaner.addDir(tempDir);
        return tempDir;
    }
    
    public static File createTempFile() throws IOException {
        return File.createTempFile(PREFIX, null, createTempDir());
    }
    
    public static synchronized void removeTempFiles() {
        if (cleaner != null) {
            cleaner.run();
        }
    }
    
    public static synchronized void removeTempFile(File fileOrDir) {
        if (cleaner != null) {
            cleaner.removeDir(fileOrDir);
        }
        try {
            removeAll(fileOrDir);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    private static void removeAll(File f) throws IOException {
        if (f.exists()) {
            if (f.isDirectory()) {
                File[] children = f.listFiles();
                for (int i = 0; i < children.length; i++) {
                    removeAll(children[i]);
                }
            }
            if (!f.delete()) {
                throw new IOException("Unable to delete " + f.getAbsolutePath());
            }
        }
    }
    
    private static final class CleanupThread extends Thread {
        private Map dirs = new HashMap();
        
        public void addDir(File dir) {
            String stack = getStack(new Exception("Directory created at:"));
            dirs.put(dir, stack);
        }
        
        public void removeDir(File dir) {
            dirs.remove(dir);
        }
        
        public void run() {
            StringBuffer buf = new StringBuffer();
            for (Iterator iter = dirs.keySet().iterator(); iter.hasNext();) {
                File dir = (File)iter.next();
                try {
                    removeAll(dir);
                } catch (IOException e) {
                    buf.append(getStack(e));
                    buf.append(dirs.get(dir));
                }
            }
            dirs.clear();
            if (buf.length() > 0) {
                throw new RuntimeException(buf.toString());
            }
        }
        
        private String getStack(Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            return sw.toString();
        }
    }
}
