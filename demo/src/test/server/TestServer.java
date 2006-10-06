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

import java.io.IOException;

import test.common.Message;

public class TestServer extends ServerBase {
    public TestServer(String baseDir) throws IOException {
        super(baseDir);
        registerMessageHandler(new EchoMessageHandler());
        registerMessageHandler(new PingMessageHandler());
        registerMessageHandler(new ShutdownMessageHandler(this));
    }
    
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: TestServer serverDir");
            System.exit(1);
        }
        TestServer server = new TestServer(args[0]);
        server.run();
    }

    private static class PingMessageHandler implements MessageHandler {
        public String getOperation() {
            return "ping";
        }
        
        public Message execute(String[] msgBody) {
            System.out.println("Ping");
            return new Message("pong");
        }
    }

    private static class ShutdownMessageHandler implements MessageHandler {
        private ServerBase server;
        public ShutdownMessageHandler(ServerBase svr) {
            server = svr;
        }
        public String getOperation() {
            return "shutdown";
        }
        
        public Message execute(String[] msgBody) {
            System.out.println("Server shutting down");
            server.shutdown();
            return null;
        }
    }

    private static class EchoMessageHandler implements MessageHandler {
        public static final String OPERATION = "echo";
        
        public String getOperation() {
            return OPERATION;
        }
        
        public Message execute(String[] msgBody) {
            System.out.println("Received echo message:");
            for (int i = 0; i < msgBody.length; i++) {
                System.out.println(msgBody[i]);
            }
            return new Message(OPERATION, msgBody);
        }
    }

}
