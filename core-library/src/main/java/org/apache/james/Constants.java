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


package org.apache.james;

/**
 * Assorted Constants for use in all James blocks
 * The Software Version, Software Name and Build Date are set by ant during
 * the build process.
 *
 *
 * @version This is $Revision$
 */
public class Constants {

    /**
     * The version of James.
     */
    public static final String SOFTWARE_VERSION = "@@VERSION@@";

    /**
     * The name of the software (i.e. James).
     */
    public static final String SOFTWARE_NAME = "@@NAME@@";

    /**
     * Context key used to store the default domain name
     * serviced by this James instance in the context.
     */
    public static final String DEFAULT_DOMAIN = "DEFAULT_DOMAIN";

    /**
     * Context key used to store the Mailet/SMTP "hello name" for this
     * James instance in the context.
     */
    public static final String HELLO_NAME = "HELLO_NAME";

    /**
     * Key used to store the hostaddress of the localhost
     */
    public static final String HOSTADDRESS = "LOCAL_HOSTADDRESS";
    
    /**
     * Key used to store the hostname of localhost
     */
    public static final String HOSTNAME = "LOCAL_HOSTNAME";

}