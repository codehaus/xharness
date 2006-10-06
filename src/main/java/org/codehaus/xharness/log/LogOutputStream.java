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


import java.io.ByteArrayOutputStream;
import java.io.OutputStream;


/**
 * OutputStream that stores its data in a LineBuffer.
 */
public class LogOutputStream extends OutputStream {
    private LineBuffer buffer;
    private int priority;
    private ByteArrayOutputStream cache = new ByteArrayOutputStream();
    private boolean skip = false;

    /**
     * Default Constructor. Creates a new LineBuffer to store data written
     * to the LogOutputStream and uses the LineBuffers default priority.
     */
    public LogOutputStream() {
        buffer = new LineBuffer();
        priority = buffer.getDefaultPriority();
    }

    /**
     * Constructor that uses the supplied LineLogger for storage.
     * 
     * @param buf The buffer to store data written to the LogOutputStream.
     * @param prio The priority to use when logging data in the buffer.
     */
    public LogOutputStream(LineBuffer buf, int prio) {
        buffer = buf;
        priority = prio;
    }

    /**
     * Writes the specified byte to this OutputStream.
     * 
     * @param val The byte.
     */
    public synchronized void write(int val) {
        final byte b = (byte)val;

        if (b == '\n' || b == '\r') {
            if (!skip) {
                copyBuffer();
            }
        } else {
            cache.write(val);
        }
        skip = b == '\r';
    }

    /**
     * Flushes this output stream and forces any buffered output bytes to be
     * written out.
     */
    public synchronized void flush() {
        copyBuffer();
    }

    /**
     * Closes this output stream and releases any system resources associated
     * with this stream.
     */
    public void close() {
        flush();
    }

    /**
     * Returns the underlying LineBuffer storage of this OutputStream.
     *
     * @return  The underlying LineBuffer storage.
     */
    public LineBuffer getBuffer() {
        return buffer;
    }

    /**
     * Copies all outstanding data from the temporary cache to the LineLogger.
     */
    private void copyBuffer() {
        String line = cache.toString();

        if (line != null && !"".equals(line)) {
            buffer.logLine(priority, line);
        }
        cache.reset();
    }
}

