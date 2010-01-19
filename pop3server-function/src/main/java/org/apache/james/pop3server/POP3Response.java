/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.pop3server;

import java.util.LinkedList;
import java.util.List;

import org.apache.james.api.protocol.RetCodeResponse;

/**
 * Contains an SMTP result
 */
public class POP3Response implements RetCodeResponse{


    // POP3 response prefixes
    /** OK response.  Requested content will follow */
    public final static String OK_RESPONSE = "+OK";
    
    /** 
     * Error response.  
     * Requested content will not be provided.  
     * This prefix is followed by a more detailed error message.
     */
    public final static String ERR_RESPONSE = "-ERR";  
    
    private String retCode = null;
    private List<CharSequence> lines = null;
    private String rawLine = null;
    private boolean endSession = false;
    
    
    /**
     * Construct a new POP3Response. The given code and description can not be null, if null an IllegalArgumentException
     * get thrown
     * 
     * @param code the returnCode
     * @param description the description 
     */
    public POP3Response(String code, CharSequence description) {
        if (code == null) throw new IllegalArgumentException("POP3Response code can not be null");
    
        this.setRetCode(code);
        if (description == null) {
        	description = "";
        }
        this.rawLine = code + " " + description;

        this.appendLine(description);

        
    }
    
    public POP3Response(String code) {
        this(code,null);
    }
    
    /**
     * Append the responseLine to the SMTPResponse
     * 
     * @param line the responseLine to append
     */
    public void appendLine(CharSequence line) {
        if (lines == null) {
            lines = new LinkedList<CharSequence>();
        }
        lines.add(line);
    }

    public String getRetCode() {
        return retCode;
    }


    public void setRetCode(String retCode) {
        this.retCode = retCode;
    }

    /**
     * Return a List of all responseLines stored in this POP3Response
     * 
     * @return all responseLines
     */
    public List<CharSequence> getLines() {
        return lines;
    }

    /**
     * Return the raw representation of the Stored POP3Response
     * 
     * @return rawLine the raw POP3Response
     */
    public String getRawLine() {
        return rawLine;
    }

    /**
     * Return true if the session is ended
     * 
     * @return true if session is ended
     */
    public boolean isEndSession() {
        return endSession;
    }

    /**
     * Set to true to end the session
     * 
     * @param endSession
     */
    public void setEndSession(boolean endSession) {
        this.endSession = endSession;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getRetCode() + " " + getLines();
    }
}
