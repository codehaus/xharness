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

package test.intermediary;

import java.io.IOException;

import test.client.ClientBase;
import test.common.Message;
import test.server.ServerBase;

public class Intermediary extends ServerBase {
    private ClientBase client;
    
    public Intermediary(String serverDir, String clientDir) throws IOException {
        super(clientDir);
        client = new ClientBase(serverDir);
    }
    
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: Intermediary serverDir clientDir");
            System.exit(1);
        }
        Intermediary server = new Intermediary(args[0], args[1]);
        server.run();
    }

    public Message unhandledMessage(Message request) throws IOException {
        System.out.println("Forwarding request " + request.getType());
        client.sendMessage(request);
        return client.receiveMessage(request.getId(), 1000);
    }
}
