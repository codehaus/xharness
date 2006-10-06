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

package test.client;

import java.io.File;
import java.io.IOException;

import test.common.AbstractEndpoint;
import test.common.ErrorMessage;
import test.common.Message;
import test.common.VoidMessage;

public class ClientBase extends AbstractEndpoint {
    private static final int DEFAULT_TIMEOUT = 1000;
    public ClientBase(String baseDir) throws IOException {
        this(new File(baseDir));
    }

    public ClientBase(File baseDir) throws IOException {
        super(new File(baseDir, "out"), new File(baseDir, "in"));
    }
    
    public Message sendRequest(String operation) throws IOException {
        return sendRequest(operation, new String[0], DEFAULT_TIMEOUT);
    }
    
    public Message sendRequest(String operation, int timeout) throws IOException {
        return sendRequest(operation, new String[0], timeout);
    }
    
    public Message sendRequest(String operation, String body) throws IOException {
        return sendRequest(operation, new String[] {body}, DEFAULT_TIMEOUT);
    }
    
    public Message sendRequest(String operation, String body, int timeout) throws IOException {
        return sendRequest(operation, new String[] {body}, timeout);
    }
    
    public Message sendRequest(String operation, String[] body) throws IOException {
        return sendRequest(operation, body, DEFAULT_TIMEOUT);
    }
    
    public Message sendRequest(String operation, String[] body, int timeout) throws IOException {
        Message request = new Message(operation, body);
        sendMessage(request);
        Message reply = receiveMessage(request.getId(), timeout);
        
        if (VoidMessage.OPERATION.equals(reply.getType())) {
            return null;
        } else if (ErrorMessage.OPERATION.equals(reply.getType())) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < reply.getBody().length; i++) {
                buf.append(reply.getBody()[i]);
            }
            throw new RuntimeException(buf.toString());
        }
        
        return reply;
    }
}
