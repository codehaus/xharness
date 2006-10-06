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

package test.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;


public abstract class AbstractEndpoint {
    private static final FileFilter MSG_FILTER = new MsgFileFilter();
    private File inDir;
    private File outDir;

    public AbstractEndpoint(String inDirName, String outDirName) throws IOException {
        this(new File(inDirName), new File(outDirName));
    }

    public AbstractEndpoint(File in, File out) throws IOException {
        inDir = in;
        outDir = out;
        
        if (!inDir.exists()) {
            inDir.mkdirs();
        } else if (!inDir.isDirectory()) {
            throw new IOException(inDir.getAbsolutePath() + " is not a directory");
        }
        
        if (!outDir.exists()) {
            outDir.mkdirs();
        } else if (!outDir.isDirectory()) {
            throw new IOException(outDir.getAbsolutePath() + " is not a directory");
        }
    }
    
    public synchronized void sendMessage(Message msg) throws IOException {
        File tmpFile = File.createTempFile("msg", ".writing", outDir);
        PrintStream out = new PrintStream(new FileOutputStream(tmpFile));
        out.println(msg.getType());
        for (int i = 0; i < msg.getBody().length; i++) {
            out.println(msg.getBody()[i]);
        }
        out.flush();
        out.close();
        
        if (msg.getId() == null) {
            int id = (int)(System.currentTimeMillis() & 0xffff)  ^ hashCode();
            while (!tmpFile.renameTo(new File(outDir, Integer.toString(id) + ".txt"))) {
                id++;
            }
            msg.setId(Integer.toString(id) + ".txt");
        } else {
            if (!tmpFile.renameTo(new File(outDir, msg.getId()))) {
                throw new IOException("Unable to write file: " + msg.getId());
            }
        }
    }
    
    public synchronized Message receiveMessage(String msgId, int timeout) throws IOException {
        File msgFile = null;
        long start = System.currentTimeMillis();
        do {
            File[] files = inDir.listFiles(MSG_FILTER);
            if (msgId != null) {
                for (int i = 0; i < files.length; i++) {
                    if (msgId.equals(files[i].getName())) {
                        msgFile = files[i];
                        break;
                    }
                }
            } else if (files.length > 0) {
                msgFile = files[0];
            }
            if (msgFile != null) {
                break;
            }
            if (timeout >= 0 && System.currentTimeMillis() - start > timeout) {
                throw new IOException("Message not received within timeout");
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException ie) {
                // ignore
            }
        } while (true);
        
        BufferedReader reader = new BufferedReader(new FileReader(msgFile));
        List msgContent = new LinkedList();
        String line;
        while ((line = reader.readLine()) != null) {
            msgContent.add(line);
        }
        reader.close();
        msgFile.delete();
        Message msg = new Message((String[])msgContent.toArray(new String[0]));
        msg.setId(msgFile.getName());
        return msg;
    }
    
    private static class MsgFileFilter implements FileFilter {
        public boolean accept(File f) {
            return f.getName().endsWith(".txt");
        }
    }

}
