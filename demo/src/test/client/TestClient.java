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

import java.io.IOException;

import test.common.Message;

public class TestClient extends ClientBase {
    public TestClient(String baseDir) throws IOException {
        super(baseDir);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: TestClient serverDir operation [message]*");
            System.exit(1);
        }
        TestClient client = new TestClient(args[0]);
        String[]  msg = new String[0];
        if (args.length > 1) {
            msg = new String[args.length - 2];
            System.arraycopy(args, 2, msg, 0, msg.length);
        }
        try {
            Message reply = client.sendRequest(args[1], msg, 1000);
            if (reply != null) {
                if (reply.getBody().length == 0) {
                    System.out.println("Received " + reply.getType() + " from server.");
                } else {
                    System.out.println("Received " + reply.getType() + " from server:");
                    for (int i = 0; i < reply.getBody().length; i++) {
                        System.out.println(reply.getBody()[i]);
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Operation " + args[1] + " failed:");
            ex.printStackTrace();
        }
    }
}
