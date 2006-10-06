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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;


/**
 * ExecuteStreamHandler that logs any output written to a Process'es stdout and
 * stderr streams to the supplied OutputStreams before passing in on to the
 * child ExecuteStreamHandler.
 */
public class LoggingStreamHandler implements ExecuteStreamHandler {
    private ExecuteStreamHandler delegate;
    private OutputStream stdout;
    private OutputStream stderr;

    /*
     * Constructor.
     *
     * @param esh The delegate StreamHandler, that performs any further processing
     *            of the output.
     * @param stdout The OutputStream that will receive a copy of the process'es
     *               stdout data.
     * @param stderr The OutputStream that will receive a copy of the process'es
     *               stderr data.
     *
     */
    public LoggingStreamHandler(ExecuteStreamHandler c,
            OutputStream so,
            OutputStream se) {
        delegate = c;
        stdout = so;
        stderr = se;
    }

    /**
     * Install a handler for the input stream of the subprocess.
     *
     * @param os output stream to write to the standard input stream of the
     *           subprocess. Directly passed onto child StreamHandler.
     * @throws IOException If the delegate stream handler throws in IOException.
     */
    public void setProcessInputStream(OutputStream os) throws IOException {
        delegate.setProcessInputStream(os);
    }

    /**
     * Install a handler for the error stream of the subprocess.
     *
     * @param is input stream to read from the error stream from the subprocess
     * @throws IOException If the delegate stream handler throws in IOException.
     */
    public void setProcessErrorStream(InputStream is) throws IOException {
        // install LoggingInputStream between the Process'es InputStream and
        // the child StreamHandler, that'll intercept and log all data written
        // to stdout.
        InputStream childIs = new LoggingInputStream(is, stderr);

        delegate.setProcessErrorStream(childIs);
    }

    /**
     * Install a handler for the output stream of the subprocess.
     *
     * @param is input stream to read from the error stream from the subprocess
     * @throws IOException If the delegate stream handler throws in IOException.
     */
    public void setProcessOutputStream(InputStream is) throws IOException {
        // install LoggingInputStream between the Process'es InputStream and
        // the child StreamHandler, that'll intercept and log all data written
        // to stderr.
        InputStream childIs = new LoggingInputStream(is, stdout);

        delegate.setProcessOutputStream(childIs);
    }

    /**
     * Start handling of the streams.
     * 
     * @throws IOException If the delegate stream handler throws in IOException.
     */
    public void start() throws IOException {
        delegate.start();
    }

    /**
     * Stop handling of the streams - will not be restarted.
     */
    public void stop() {
        delegate.stop();
        try {
            stdout.flush();
        } catch (IOException ioe) {
            //ignore
        }
        try {
            stderr.flush();
        } catch (IOException ioe) {
            //ignore
        }
    }
}
