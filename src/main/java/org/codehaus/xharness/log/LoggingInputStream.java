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

package org.codehaus.xharness.log;


import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * InpuStream that writes a copy of all data read from it's underlying InputStream 
 * to a (logging) OutputStream.
 * 
 * @author Gregor Heine
 */
public class LoggingInputStream extends FilterInputStream {
    private OutputStream log;
    private boolean locked = false;

    /**
     * Constructor.
     *
     * @param parent  The underlying InputStream that is being read from.
     * @param os      The OutputStream that all data read from the InputStream 
     *                is written to.
     */
    public LoggingInputStream(InputStream parent, OutputStream os) {
        super(parent);
        log = os;
    }

    /**
     * Closes the parent InputStream and the logging OutputStream and releases
     * any system resources associated with the streams.
     * 
     * @throws IOException If the underlying InputStream throws an IOException.
     */
    public void close() throws IOException {
        super.close();
        log.close();
    }

    /**
     * Reads the next byte of data from the parent InputStream and writes it to
     * the logging OutputStream.
     * 
     * @return The byte value read from the underlying InputStream.
     * @throws IOException If the underlying InputStream throws an IOException.
     */
    public synchronized int read() throws IOException {
        boolean writeOk = !locked;

        locked = true;
        try {
            int val = super.read();

            if (val > 0 && writeOk) {
                log.write(val);
            }
            return val;
        } finally {
            locked = !writeOk;
        }
    }

    /**
     * Reads a number of bytes of data from the parent InputStream into an array
     * of bytes and also writes the data to the logging OutputStream.
     * 
     * @param b The he buffer into which the data is read from the underlying InputStream.
     * @return the total number of bytes read into the buffer, or -1 if there is no 
     *         more data because the end of the stream has been reached.
     * @throws IOException If the underlying InputStream throws an IOException.
     */
    public synchronized int read(byte[] b) throws IOException {
        boolean writeOk = !locked;

        locked = true;
        try {
            int val = super.read(b);

            if (val > 0 && writeOk) {
                log.write(b, 0, val);
            }
            return val;
        } finally {
            locked = !writeOk;
        }
    }

    /**
     * Reads a number of bytes of data from the parent InputStream into an array
     * of bytes and also writes the data to the logging OutputStream.
     * 
     * @param b The he buffer into which the data is read from the underlying InputStream.
     * @param off The start offset in array b  at which the data is written.
     * @param len The maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or -1 if there is no 
     *         more data because the end of the stream has been reached.
     * @throws IOException If the underlying InputStream throws an IOException.
     */
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        boolean writeOk = !locked;

        locked = true;
        try {
            int val = super.read(b, off, len);

            if (val > 0 && writeOk) {
                log.write(b, off, val);
            }
            return val;
        } finally {
            locked = !writeOk;
        }
    }
    
    public boolean equals(Object other) {
        if (other instanceof LoggingInputStream) {
            LoggingInputStream lis = (LoggingInputStream)other;
            return in.equals(lis.in) && log.equals(lis.log);
        }
        return false;
    }
    
    public int hashCode() {
        return super.hashCode();
    }
}
