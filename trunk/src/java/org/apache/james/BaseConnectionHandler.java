/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Different connection handlers extend this class
 * Common Connection Handler code could be factored into this class.
 * At present(April 28' 2001) there is not much in this class
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
public class BaseConnectionHandler extends AbstractLogEnabled implements Configurable {

    /**
     * The timeout for the connection
     */
    protected int timeout;

    /**
     * The hello name for the connection
     */
    protected String helloName;

    /**
     * Get the hello name for this server
     *
     * @param configuration a configuration object containing server name configuration info
     * @return the hello name for this server
     */
    public static String configHelloName(final Configuration configuration)
        throws ConfigurationException {
        String hostName = null;

        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch  (UnknownHostException ue) {
            // Default to localhost if we can't get the local host name.
            hostName = "localhost";
        }

        Configuration helloConf = configuration.getChild("helloName");
        boolean autodetect = helloConf.getAttributeAsBoolean("autodetect", true);

        return autodetect ? hostName : helloConf.getValue("localhost");
    }


    /**
     * Pass the <code>Configuration</code> to the instance.
     *
     * @param configuration the class configurations.
     * @throws ConfigurationException if an error occurs
     */
    public void configure( final Configuration configuration )
        throws ConfigurationException {

        timeout = configuration.getChild( "connectiontimeout" ).getValueAsInteger( 1800000 );
        helloName = configHelloName(configuration);
        getLogger().info("Hello Name is: " + helloName);
    }

    /**
     * Release a previously created ConnectionHandler e.g. for spooling.
     *
     * @param connectionHandler the ConnectionHandler to be released
     */
    public void releaseConnectionHandler(ConnectionHandler connectionHandler) {
    }
}
