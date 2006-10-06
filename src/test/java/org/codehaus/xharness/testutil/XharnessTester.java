// @Copyright 2004 IONA Technologies, Plc. All Rights Reserved.
//
package org.codehaus.xharness.testutil;


import java.io.File;
import java.io.IOException;

import java.util.Iterator;
import java.util.LinkedList;


/**
 * Test program that simulates possible behaviour and output of Java and C++
 * CORBA client and server processes.
 *
 * @autor  Gregor Heine
 */
public final class XharnessTester {
    private XharnessTester() {
        //utility class, never constructed
    }


    public static void main(String[] args) {
        File blockFile = null;
        File touchFile = null;
        int sleep = 0;
        int retval = 0;
        LinkedList out1Output = new LinkedList();
        LinkedList err1Output = new LinkedList();
        LinkedList out2Output = new LinkedList();
        LinkedList err2Output = new LinkedList();

        for (int i = 0; i < args.length; i++) {
            if ("-block".equals(args[i])) {
                blockFile = new File(args[++i]);
            } else if ("-delete".equals(args[i])) {
                new File(args[++i]).delete();
            } else if ("-check".equals(args[i])) {
                String filename = args[++i];

                if (!new File(filename).exists()) {
                    System.out.println("File " + filename + " not found!");
                    System.exit(1);
                }
            } else if ("-checknot".equals(args[i])) {
                String filename = args[++i];

                if (new File(filename).exists()) {
                    System.out.println("File " + filename + " found!");
                    System.exit(1);
                }
            } else if ("-touch".equals(args[i])) {
                touchFile = new File(args[++i]);
            } else if ("-sleep".equals(args[i])) {
                sleep = Integer.parseInt(args[++i]);
            } else if ("-return".equals(args[i])) {
                retval = Integer.parseInt(args[++i]);
            } else if ("-out".equals(args[i])) {
                out1Output.add(args[++i]);
            } else if ("-err".equals(args[i])) {
                err1Output.add(args[++i]);
            } else if ("-out2".equals(args[i])) {
                out2Output.add(args[++i]);
            } else if ("-err2".equals(args[i])) {
                err2Output.add(args[++i]);
            }
        }

        Iterator i;

        i = out1Output.iterator();
        while (i.hasNext()) {
            System.out.println((String)i.next());
        }

        i = err1Output.iterator();
        while (i.hasNext()) {
            System.err.println((String)i.next());
        }

        if (sleep > 0) {
            try {
                Thread.sleep(sleep * 1000);
            } catch (Exception e) {
                //ignore
            }
        }

        if (blockFile != null) {
            blockFile.delete();
        }
        if (touchFile != null) {
            try {
                touchFile.createNewFile();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            }
        }
        if (blockFile != null) {
            while (!blockFile.exists()) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    //ignore
                }
            }
        }

        i = out2Output.iterator();
        while (i.hasNext()) {
            System.out.println((String)i.next());
        }

        i = err2Output.iterator();
        while (i.hasNext()) {
            System.err.println((String)i.next());
        }

        System.exit(retval);
    }
}


