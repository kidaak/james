/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.mailet;

import java.util.*;

/**
 * GenericMailet makes writing mailets easier. It provides simple
 * versions of the lifecycle methods init and destroy and of the methods
 * in the MailetConfig interface. GenericMailet also implements the log
 * method, declared in the MailetContext interface.
 * <p>
 * To write a generic mailet, you need only override the abstract service
 * method.
 *
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri   <scoobie@pop.systemy.it>
 * @author  Stefano Mazzocchi   <stefano@apache.org>
 * @author  Pierpaolo Fumagalli <pier@apache.org>
 * @author  Serge Knystautas    <sergek@lokitech.com>
 */
public abstract class GenericMailet implements Mailet, MailetConfig {
    private MailetConfig config = null;

    /**
     * Called by the mailer container to indicate to a mailet that the
     * mailet is being taken out of service.
     */
    public void destroy() {
        //Do nothing
    }

    /**
     * Returns a String containing the value of the named initialization
     * parameter, or null if the parameter does not exist.
     * <p>
     * This method is supplied for convenience. It gets the value of the
     * named parameter from the mailet's MailetConfig object.
     *
     * @param name - a String specifying the name of the initialization parameter
     * @return String a String containing the value of the initalization parameter
     */
    public String getInitParameter(String name) {
        return config.getInitParameter(name);
    }

    /**
     * Returns the names of the mailet's initialization parameters as an
     * Iterator of String objects, or an empty Iterator if the mailet has no
     * initialization parameters.
     * <p>
     * This method is supplied for convenience. It gets the parameter names from
     * the mailet's MailetConfig object.
     *
     * @return Iterator an iterator of String objects containing the names of
     *          the mailet's initialization parameters
     */
    public Iterator getInitParameterNames() {
        return config.getInitParameterNames();
    }

    /**
     * Returns this matcher's MailetConfig object.
     *
     * @return MailetConfig the MailetConfig object that initialized this mailet
     */
    public MailetConfig getMailetConfig() {
        return config;
    }

    /**
     * Returns a reference to the MailetContext in which this mailet is
     * running.
     *
     * @return MailetContext the MailetContext object passed to this mailet by the init method
     */
    public MailetContext getMailetContext() {
        return getMailetConfig().getMailetContext();
    }

    /**
     * Returns information about the mailet, such as author, version, and
     * copyright.  By default, this method returns an empty string. Override
     * this method to have it return a meaningful value.
     *
     * @return String information about this mailet, by default an empty string
     */
    public String getMailetInfo() {
        return "";
    }

    /**
     * Returns the name of this mailet instance.
     *
     * @return the name of this mailet instance
     */
    public String getMailetName() {
        return config.getMailetName();
    }


    /**
     * Called by the mailet container to indicate to a mailet that the
     * mailet is being placed into service.
     *
     * This implementation stores the MailetConfig object it receives from
     * the mailet container for alter use. When overriding this form of the
     * method, call super.init(config).
     *
     * @param MailetConfig config - the MailetConfig object that contains
     *          configutation information for this mailet
     * @throws MailetException
     *          if an exception occurs that interrupts the mailet's normal operation
     */
    public void init(MailetConfig newConfig) throws MailetException {
        config = newConfig;
        init();
    }

    /**
     * A convenience method which can be overridden so that there's no
     * need to call super.init(config).
     *
     * Instead of overriding init(MailetConfig), simply override this
     * method and it will be called by GenericMailet.init(MailetConfig config).
     * The MailetConfig object can still be retrieved via getMailetConfig().
     *
     * @throws MailetException
     *          if an exception occurs that interrupts the mailet's normal operation
     */
    public void init() throws MailetException {
        //Do nothing... can be overriden
    }

    /**
     * Writes the specified message to a mailet log file, prepended by
     * the mailet's name.
     *
     * @param msg - a String specifying the message to be written to the log file
     */
    public void log(String message) {
        getMailetContext().log(config.getMailetName() + ": " + message);
    }

    /**
     * Writes an explanatory message and a stack trace for a given Throwable
     * exception to the mailet log file, prepended by the mailet's name.
     *
     * @param message - a String that describes the error or exception
     * @param t - the java.lang.Throwable error or exception
     */
    public void log(String message, Throwable t) {
        getMailetContext().log(config.getMailetName() + ": " + message, t);
    }

    /**
     * Called by the mailet container to allow the mailet to process a
     * message.
     *
     * This method is declared abstract so subclasses must override it.
     *
     * @param mail - the Mail object that contains the MimeMessage and
     *          routing information
     * @throws MailetException - if an exception occurs that interferes with the mailet's normal operation
     *          occurred
     */
    public abstract void service(Mail mail) throws MailetException, javax.mail.MessagingException;
}


