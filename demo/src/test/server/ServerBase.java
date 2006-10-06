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

package test.server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import test.common.AbstractEndpoint;
import test.common.ErrorMessage;
import test.common.Message;
import test.common.VoidMessage;

public class ServerBase extends AbstractEndpoint {
    private Map handlerMap = new HashMap();
    private boolean shutdown = false;
    
    public ServerBase(String baseDir) throws IOException {
        this(new File(baseDir));
    }

    public ServerBase(File baseDir) throws IOException {
        super(new File(baseDir, "in"), new File(baseDir, "out"));
        cleanDir(new File(baseDir, "in"));
        cleanDir(new File(baseDir, "out"));
        
    }
    
    public void shutdown() {
        shutdown = true;
    }
    
    public void run() {
        do {
            Message request = null;
            try {
                request = receiveMessage(null, -1);
            } catch (IOException ioe) {
                System.err.println("Exception while receiving message: " + ioe);
                continue;
            }
            MessageHandler handler = getMessageHandler(request);
            Throwable error = null;
            
            if (handler == null) {
                try {
                    String id = request.getId();
                    Message reply = unhandledMessage(request);
                    sendReply(reply, id);
                } catch (Throwable t) {
                    error = t;
                }
            } else {
                try {
                    Message reply = handler.execute(request.getBody());
                    sendReply(reply, request.getId());
                } catch (Throwable t) {
                    error = t;
                }
            }
            
            if (error != null) {
                System.err.println(error);
                try {
                    sendReply(new ErrorMessage(error), request.getId());
                } catch (IOException ioe) {
                    System.err.println("Failure while sending Error message:" + ioe);
                }
                
            }
        } while (!shutdown);
    }
    
    public Message unhandledMessage(Message request) throws Exception {
        throw new RuntimeException("No handler registered for message type " + request.getType());
    }
    
    public void registerMessageHandler(MessageHandler handler) {
        synchronized (handlerMap) {
            handlerMap.put(handler.getOperation(), handler);
        }
    }
    
    public MessageHandler getMessageHandler(Message msg) {
        synchronized (handlerMap) {
            return (MessageHandler)handlerMap.get(msg.getType());
        }
    }
    
    public interface MessageHandler {
        String getOperation();
        Message execute(String[] msgBody);
        
    }
    
    private void sendReply(Message reply, String msgId) throws IOException {
        if (reply == null) {
            reply = new VoidMessage();
        }
        reply.setId(msgId);
        sendMessage(reply);
    }

    private static void cleanDir(File dir) {
        File[] children = dir.listFiles();
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory()) {
                cleanDir(children[i]);
            }
            children[i].delete();
        }
    }
}
