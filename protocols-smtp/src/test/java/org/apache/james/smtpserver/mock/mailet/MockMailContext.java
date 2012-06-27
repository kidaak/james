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
package org.apache.james.smtpserver.mock.mailet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;

public class MockMailContext implements MailetContext {

    HashMap attributes = new HashMap();

    @Override
    public void bounce(Mail mail, String message) throws MessagingException {
        // trivial implementation
    }

    @Override
    public void bounce(Mail mail, String message, MailAddress bouncer) throws MessagingException {
        // trivial implementation
    }

    @Override
    public Collection getMailServers(String host) {
        return null; // trivial implementation
    }

    @Override
    public MailAddress getPostmaster() {
        return null; // trivial implementation
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Iterator getAttributeNames() {
        return attributes.keySet().iterator();
    }

    @Override
    public int getMajorVersion() {
        return 0; // trivial implementation
    }

    @Override
    public int getMinorVersion() {
        return 0; // trivial implementation
    }

    @Override
    public String getServerInfo() {
        return "Mock Server";
    }

    @Override
    public boolean isLocalServer(String serverName) {
        return false; // trivial implementation
    }

    @Override
    public boolean isLocalUser(String userAccount) {
        return false; // trivial implementation
    }

    @Override
    public boolean isLocalEmail(MailAddress mailAddress) {
        return false; // trivial implementation
    }

    @Override
    public void log(String message) {
        System.out.println(message);
    }

    @Override
    public void log(String message, Throwable t) {
        System.out.println(message);
        t.printStackTrace(System.out);
    }

    @Override
    public void removeAttribute(String name) {
        // trivial implementation
    }

    @Override
    public void sendMail(MimeMessage msg) throws MessagingException {
        throw new UnsupportedOperationException("MOCKed method");
    }

    @Override
    public void sendMail(MailAddress sender, Collection recipients, MimeMessage msg) throws MessagingException {
        throw new UnsupportedOperationException("MOCKed method");
    }

    @Override
    public void sendMail(MailAddress sender, Collection recipients, MimeMessage msg, String state) throws
            MessagingException {
        throw new UnsupportedOperationException("MOCKed method");
    }

    @Override
    public void sendMail(Mail mail) throws MessagingException {
        throw new UnsupportedOperationException("MOCKed method");
    }

    @Override
    public void setAttribute(String name, Object object) {
        attributes.put(name, object);
    }

    @Override
    public void storeMail(MailAddress sender, MailAddress recipient, MimeMessage msg) throws MessagingException {
        // trivial implementation
    }

    @Override
    public Iterator getSMTPHostAddresses(String domainName) {
        return null; // trivial implementation
    }
}