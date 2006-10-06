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

import java.io.IOException;

public class Message {
    private String msgId;
    private String msgType;
    private String[] msgBody;
    
    public Message(String type) {
        this (type, new String[0]);
    }
    
    public Message(String type, String body) {
        this (type, new String[] {body});
    }
    
    public Message(String type, String[] body) {
        msgType = type;
        msgBody = body;
    }
    
    public Message(String[] content) throws IOException {
        if (content.length == 0) {
            throw new IOException("Invalid message format");
        }
        msgType = content[0];
        msgBody = new String[content.length - 1];
        System.arraycopy(content, 1, msgBody, 0, msgBody.length);
    }
    
    public String getType() {
        return msgType;
    }
    
    public String[] getBody() {
        return msgBody;
    }
    
    public String getId() {
        return msgId;
    }
    
    public void setId(String id) {
        msgId = id;
    }
}
