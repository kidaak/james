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



package org.apache.james.mailetcontainer;
import javax.mail.MessagingException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.mailetcontainer.MatcherLoader;
import org.apache.mailet.Matcher;
/**
 * Loads Matchers for use inside James.
 *
 */
public class JamesMatcherLoader extends AbstractLoader implements MatcherLoader {
    private static final String DISPLAY_NAME = "matcher";
    private final String MATCHER_PACKAGE = "matcherpackage";


    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration conf) throws ConfigurationException {
           getPackages(conf.configurationAt("matcherpackages"),MATCHER_PACKAGE);
    }

    /**
     * @see org.apache.james.mailetcontainer.MatcherLoader#getMatcher(java.lang.String)
    */
    public Matcher getMatcher(String matchName) throws MessagingException {
        try {
            String condition = (String) null;
            int i = matchName.indexOf('=');
            if (i != -1) {
                condition = matchName.substring(i + 1);
                matchName = matchName.substring(0, i);
            }
            for (final String packageName: packages) {
                final String className = packageName + matchName;
                try {
                    final Matcher matcher = (Matcher) factory.newInstance(Thread.currentThread().getContextClassLoader().loadClass(className));
                    
                    final MatcherConfigImpl configImpl = new MatcherConfigImpl();
                    configImpl.setMatcherName(matchName);
                    configImpl.setCondition(condition);
                    configImpl.setMailetContext(mailetContext);

                    matcher.init(configImpl);
                    
                    return matcher;
                } catch (ClassNotFoundException cnfe) {
                    //do this so we loop through all the packages
                }
            }
            throw classNotFound(matchName);
        } catch (MessagingException me) {
            throw me;
        } catch (Exception e) {
            throw loadFailed(matchName, e);
        }
    }

    /**
     * @see AbstractLoader#getDisplayName()
     */
    @Override
    protected String getDisplayName() {
        return DISPLAY_NAME;
    }
}