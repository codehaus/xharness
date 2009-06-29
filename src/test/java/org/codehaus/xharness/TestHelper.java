package org.codehaus.xharness;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class TestHelper {
    private static String ANT_VERSION;
    private TestHelper() {
    }
    
    public static boolean isAnt16() {
        return getAntVersion().startsWith("1.6.");
    }
    
    public static boolean isAnt17() {
        return getAntVersion().startsWith("1.7.");
    }
    
    public static String getAntVersion() {
        if (ANT_VERSION == null) {
            InputStream is = TestHelper.class.getResourceAsStream("/org/apache/tools/ant/version.txt");
            Properties props = new Properties();
            try {
                props.load(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ANT_VERSION = props.getProperty("VERSION", "0.0.0");
        }
        return ANT_VERSION;
    }
}
