/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.mailrepository;

import org.apache.james.core.MailImpl;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * Implementation of a MailRepository on a FileSystem.
 *
 * Requires a configuration element in the .conf.xml file of the form:
 *  &lt;repository destinationURL="file://path-to-root-dir-for-repository"
 *              type="MAIL"
 *              model="SYNCHRONOUS"/&gt;
 * Requires a logger called MailRepository.
 *
 * @version 1.0.0, 24/04/1999
 */
public class AvalonSpoolRepository
    extends AvalonMailRepository
    implements SpoolRepository {

    /**
     * <p>Returns an arbitrarily selected mail deposited in this Repository.
     * Usage: SpoolManager calls accept() to see if there are any unprocessed 
     * mails in the spool repository.</p>
     *
     * <p>Synchronized to ensure thread safe access to the underlying spool.</p>
     *
     * @return the mail
     */
    public synchronized Mail accept() throws InterruptedException {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Method accept() called");
        }
        while (!Thread.currentThread().isInterrupted()) {
            try {
                for(Iterator it = list(); it.hasNext(); ) {

                    String s = it.next().toString();
                    if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                        StringBuffer logBuffer =
                            new StringBuffer(64)
                                    .append("Found item ")
                                    .append(s)
                                    .append(" in spool.");
                        getLogger().debug(logBuffer.toString());
                    }
                    if (lock(s)) {
                        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                            getLogger().debug("accept() has locked: " + s);
                        }
                        try {
                            MailImpl mail = retrieve(s);
                            // Retrieve can return null if the mail is no longer on the spool
                            // (i.e. another thread has gotten to it first).
                            // In this case we simply continue to the next key
                            if (mail == null) {
                                continue;
                            }
                            return mail;
                        } catch (javax.mail.MessagingException e) {
                            getLogger().error("Exception during retrieve -- skipping item " + s, e);
                        }
                    }
                }

                wait();
            } catch (InterruptedException ex) {
                throw ex;
            } catch (ConcurrentModificationException cme) {
               // Should never get here now that list methods clones keyset for iterator
                getLogger().error("CME in spooler - please report to http://james.apache.org", cme);
            }
        }
        throw new InterruptedException();
    }

    /**
     * <p>Returns an arbitrarily selected mail deposited in this Repository that
     * is either ready immediately for delivery, or is younger than it's last_updated plus
     * the number of failed attempts times the delay time.
     * Usage: RemoteDeliverySpool calls accept() with some delay and should block until an
     * unprocessed mail is available.</p>
     *
     * <p>Synchronized to ensure thread safe access to the underlying spool.</p>
     *
     * @return the mail
     */
    public synchronized Mail accept(final long delay) throws InterruptedException
    {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Method accept(delay) called");
        }
        return accept(new SpoolRepository.AcceptFilter () {
            long youngest = 0;
                
                public boolean accept (String key, String state, long lastUpdated, String errorMessage) {
                    if (state.equals(Mail.ERROR)) {
                        //Test the time...
                        long timeToProcess = delay + lastUpdated;
                
                        if (System.currentTimeMillis() > timeToProcess) {
                            //We're ready to process this again
                            return true;
                        } else {
                            //We're not ready to process this.
                            if (youngest == 0 || youngest > timeToProcess) {
                                //Mark this as the next most likely possible mail to process
                                youngest = timeToProcess;
                            }
                            return false;
                        }
                    } else {
                        //This mail is good to go... return the key
                        return true;
                    }
                }
        
                public long getWaitTime () {
                    if (youngest == 0) {
                        return 0;
                    } else {
                        long duration = youngest - System.currentTimeMillis();
                        youngest = 0; //get ready for next round
                        return duration <= 0 ? 1 : duration;
                    }
                }
            });
    }


    /**
     * Returns an arbitrarily select mail deposited in this Repository for
     * which the supplied filter's accept method returns true.
     * Usage: RemoteDeliverySpool calls accept(filter) with some a filter which determines
     * based on number of retries if the mail is ready for processing.
     * If no message is ready the method will block until one is, the amount of time to block is
     * determined by calling the filters getWaitTime method.
     *
     * <p>Synchronized to ensure thread safe access to the underlying spool.</p>
     *
     * @return  the mail
     */
    public synchronized Mail accept(SpoolRepository.AcceptFilter filter) throws InterruptedException
    {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Method accept(Filter) called");
        }
        while (!Thread.currentThread().isInterrupted()) {
            for (Iterator it = list(); it.hasNext(); ) {
                String s = it.next().toString();
                if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                    StringBuffer logBuffer =
                        new StringBuffer(64)
                                .append("Found item ")
                                .append(s)
                                .append(" in spool.");
                    getLogger().debug(logBuffer.toString());
                }
                if (lock(s)) {
                    if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                        getLogger().debug("accept(delay) has locked: " + s);
                    }
                    MailImpl mail = null;
                    try {
                        mail = retrieve(s);
                    } catch (javax.mail.MessagingException e) {
                        getLogger().error("Exception during retrieve -- skipping item " + s, e);
                    }
                    // Retrieve can return null if the mail is no longer on the spool
                    // (i.e. another thread has gotten to it first).
                    // In this case we simply continue to the next key
                    if (mail == null) {
                        continue;
                    }
                    if (filter.accept (mail.getName(),
                                       mail.getState(),
                                       mail.getLastUpdated().getTime(),
                                       mail.getErrorMessage())) {
                        return mail;
                    }
                    
                }
            }
            //We did not find any... let's wait for a certain amount of time
            try {
                wait (filter.getWaitTime());
            } catch (InterruptedException ex) {
                throw ex;
            } catch (ConcurrentModificationException cme) {
               // Should never get here now that list methods clones keyset for iterator
                getLogger().error("CME in spooler - please report to http://james.apache.org", cme);
            }
        }
        throw new InterruptedException();
    }
    
}
