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
package org.apache.james.queue.activemq;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.BlobMessage;
import org.apache.activemq.pool.PooledSession;
import org.apache.commons.logging.Log;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageInputStream;
import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.jms.JMSMailQueue;
import org.apache.mailet.Mail;

/**
 * *{@link MailQueue} implementation which use an ActiveMQ Queue.
 * 
 * This implementation require at ActiveMQ 5.4.0+.
 * 
 * When a {@link Mail} attribute is found and is not one of the supported
 * primitives, then the toString() method is called on the attribute value to
 * convert it
 * 
 * The implementation support the usage of {@link BlobMessage} for out-of-band
 * transfer of the {@link MimeMessage}
 * 
 * See http://activemq.apache.org/blob-messages.html for more details
 * 
 * 
 * Some other supported feature is handling of priorities. See:
 * 
 * http://activemq.apache.org/how-can-i-support-priority-queues.html
 * 
 * For this just add a {@link Mail} attribute with name {@link #MAIL_PRIORITY}
 * to it. It should use one of the following value {@link #LOW_PRIORITY},
 * {@link #NORMAL_PRIORITY}, {@link #HIGH_PRIORITY}
 * 
 * 
 */
public class ActiveMQMailQueue extends JMSMailQueue {

    private long messageTreshold = -1;

    private final static String JAMES_BLOB_URL = "JAMES_BLOB_URL";

    public final static int NO_DELAY = -1;
    public final static int DISABLE_TRESHOLD = -1;
    public final static int BLOBMESSAGE_ONLY = 0;

    /**
     * Construct a new ActiveMQ based {@link MailQueue}. The messageTreshold is
     * used to calculate if a {@link BytesMessage} or a {@link BlobMessage}
     * should be used when queuing the mail in ActiveMQ. A {@link BlobMessage}
     * is used If the message size is bigger then the messageTreshold. The size
     * if in bytes.
     * 
     * If you want to disable the usage of {@link BlobMessage} just use
     * {@link #DISABLE_TRESHOLD} as value. If you want to use
     * {@link BlobMessage} for every message (not depending of the size) just
     * use {@link #BLOBMESSAGE_ONLY} as value.
     * 
     * For enabling the priority feature in AMQ see:
     * 
     * http://activemq.apache.org/how-can-i-support-priority-queues.html
     * 
     * @param connectionFactory
     * @param queuename
     * @param messageTreshold
     * @param logger
     */
    public ActiveMQMailQueue(final ConnectionFactory connectionFactory, final String queuename, final long messageTreshold, final Log logger) {
        super(connectionFactory, queuename, logger);
        this.messageTreshold = messageTreshold;
    }

    /**
     * ActiveMQ based {@link MailQueue} which just use {@link BytesMessage} for
     * all messages
     * 
     * @see #ActiveMQMailQueue(ConnectionFactory, String, long, Log)
     */
    public ActiveMQMailQueue(final ConnectionFactory connectionFactory, final String queuename, final Log logger) {
        this(connectionFactory, queuename, DISABLE_TRESHOLD, logger);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.queue.jms.JMSMailQueue#populateMailMimeMessage(javax
     * .jms.Message, org.apache.mailet.Mail)
     */
    protected void populateMailMimeMessage(Message message, Mail mail) throws MessagingException {
        if (message instanceof BlobMessage) {
            try {
                BlobMessage blobMessage = (BlobMessage) message;
                try {
                    // store url for later usage. Maybe we can do something
                    // smart for RemoteDelivery here
                    // TODO: Check if this makes sense at all
                    mail.setAttribute(JAMES_BLOB_URL, blobMessage.getURL());
                } catch (MalformedURLException e) {
                    // Ignore on error
                    logger.debug("Unable to get url from blobmessage for mail " + mail.getName());
                }
                mail.setMessage(new MimeMessageCopyOnWriteProxy(new MimeMessageInputStreamSource(mail.getName(), blobMessage.getInputStream())));

            } catch (IOException e) {
                throw new MailQueueException("Unable to populate MimeMessage for mail " + mail.getName(), e);
            } catch (JMSException e) {
                throw new MailQueueException("Unable to populate MimeMessage for mail " + mail.getName(), e);
            }
        } else {
            super.populateMailMimeMessage(message, mail);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.queue.jms.JMSMailQueue#createMessage(javax.jms.Session,
     * org.apache.mailet.Mail, long)
     */
    protected void produceMail(Session session, Map<String,Object> props, int msgPrio, Mail mail) throws JMSException, MessagingException, IOException {
        boolean useBlob = false;
        if (messageTreshold != -1) {
            try {
                if (messageTreshold == 0 || mail.getMessageSize() > messageTreshold) {
                    useBlob = true;
                }
            } catch (MessagingException e) {
                logger.info("Unable to calculate message size for mail " + mail.getName() + ". Use BytesMessage for JMS");
                useBlob = false;
            }
        }
        if (useBlob) {
            MessageProducer producer = null;
            try {
                ActiveMQSession amqSession;
                if (session instanceof PooledSession) {
                    amqSession = ((PooledSession) session).getInternalSession();
                } else {
                    amqSession = (ActiveMQSession) session;
                }
                BlobMessage message = amqSession.createBlobMessage(new MimeMessageInputStream(mail.getMessage()));
                Queue queue = session.createQueue(queuename);

                producer = session.createProducer(queue);
                Iterator<String> keys = props.keySet().iterator();
                while (keys.hasNext()) {
                    String key = keys.next();
                    message.setObjectProperty(key, props.get(key));
                }
                producer.send(message, Message.DEFAULT_DELIVERY_MODE, msgPrio, Message.DEFAULT_TIME_TO_LIVE);
            } finally {

                try {
                    if (producer != null)
                        producer.close();
                } catch (JMSException e) {
                    // ignore here
                }
            }
            
        } else {
            super.produceMail(session, props, msgPrio, mail);
        }

    }

    @Override
    protected MailQueueItem createMailQueueItem(Connection connection, Session session, MessageConsumer consumer, Message message) throws JMSException, MessagingException {
        Mail mail = createMail(message);
        return new ActiveMQMailQueueItem(mail, connection, session, consumer, message, logger);
    }

}