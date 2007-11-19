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

package org.apache.james.mailboxmanager.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MessageResult;

public class MailboxListenerCollector implements MailboxListener {
    
    protected List addedList =new ArrayList();
    protected List expungedList =new ArrayList();
    protected List flaggedList =new ArrayList();

    public void added(MessageResult mr) {
        addedList.add(mr);
    }

    public void expunged(MessageResult mr) {
        expungedList.add(mr);
    }

    public void flagsUpdated(MessageResult mr) {
        flaggedList.add(mr);
    }
    
    public synchronized List getAddedList(boolean reset) {
        List list=addedList;
        if (reset) {
            addedList=new ArrayList();
        }
        return list;
    }

    public synchronized List getExpungedList(boolean reset) {
        List list=expungedList;
        if (reset) {
            expungedList=new ArrayList();
        }
        return list;
    }

    public synchronized List getFlaggedList(boolean reset) {
        List list=flaggedList;
        if (reset) {
            flaggedList=new ArrayList();
        }
        return list;
    }

    public void mailboxDeleted() {
    }

    public void mailboxRenamed(String origName, String newName) {
    }

    public void mailboxRenamed(String newName) {
    }

    public void event(Event event) {
        if (event instanceof MessageEvent) {
            final MessageEvent messageEvent = (MessageEvent) event;
            final MessageResult subject = messageEvent.getSubject();
            if (event instanceof Added) {
                added(subject);
            } else if (event instanceof Expunged) {
                expunged(subject);
            } else if (event instanceof FlagsUpdated) {
                flagsUpdated(subject);
            }
        }
    }

}
