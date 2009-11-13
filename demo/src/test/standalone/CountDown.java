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

package test.standalone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public final class CountDown {
    private final static String KEY = "polval";
    private CountDown() {
    }
    
    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        File pollerFile = new File("countdown.properties");
        if (pollerFile.isFile()) {
            InputStream is = new FileInputStream(pollerFile);
            properties.load(is);
            is.close();
            System.out.println("CountDown continuing");
        } else {
            if (args.length != 1) {
                System.err.println("Invalid usage: CountDown <num>");
                System.exit(1);
            }
            int val = parseInt(args[0]);
            properties.setProperty(KEY, Integer.toString(val));
            System.out.println("CountDown started");
        }
        int val = parseInt(properties.getProperty(KEY)) - 1;
        System.out.println("CountDown value: " + val);
        if (val == 0) {
            pollerFile.delete();
            System.out.println("CountDown reached");
        } else {
            properties.setProperty(KEY, Integer.toString(val));
            OutputStream os = new FileOutputStream(pollerFile);
            properties.store(os, null);
            os.close();
            System.out.println("CountDown incomplete");
        }
    }
    
    private static int parseInt(String val) {
        int ret = 0;
        try {
            ret = Integer.parseInt(val);
        } catch (NumberFormatException nfe) {
            System.err.println("Invalid usage: CountDown <num>");
            System.exit(2);
        }
        return ret;
    }
}
