package org.codehaus.xharness.testutil;

import java.io.File;
import java.io.IOException;

public final class TempDir {
    private TempDir() {
    }
    
    public static File createTempDir(File baseDir) throws IOException {
        if (!baseDir.exists() && baseDir.isDirectory()) {
            throw new IOException("Invalid basedir");
        }
        File ret = null;
        long time = System.currentTimeMillis();
        do {
            ret = new File(baseDir, Long.toString(time++) + ".tmp");
        } while (ret.exists());
        ret.mkdir();
        return ret;
    }
    
    public static void removeFiles(File base) {
        if (base.exists()) {
            if (base.isDirectory()) {
                File[] children = base.listFiles();
                for (int i = 0; i < children.length; i++) {
                    removeFiles(children[i]);
                }
            }
            base.delete();
        }
    }

}
