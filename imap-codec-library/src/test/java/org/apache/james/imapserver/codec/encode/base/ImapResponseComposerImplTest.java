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

package org.apache.james.imapserver.codec.encode.base;

import java.util.List;

import org.apache.james.imapserver.codec.encode.AbstractTestImapResponseComposer;

public class ImapResponseComposerImplTest extends
        AbstractTestImapResponseComposer {

    ImapResponseComposerImpl composer;
    ByteImapResponseWriter writer;
    
    protected void setUp() throws Exception {
        super.setUp();
        writer = new ByteImapResponseWriter();
        composer = new ImapResponseComposerImpl(writer);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    protected byte[] encodeListResponse(String typeName, List attributes, String hierarchyDelimiter, String name) throws Exception {
        composer.listResponse(typeName, attributes, hierarchyDelimiter, name);
        return writer.getBytes();
    }

    protected void clear() throws Exception {
        writer.clear();
    }

}
