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



package org.apache.james.nntpserver.repository;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.services.FileSystem;
import org.apache.james.util.Lock;
import org.apache.james.util.io.IOUtil;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Processes entries and sends to appropriate groups.
 * Eats up inappropriate entries.
 *
 */
public class NNTPSpooler {

    private ArticleIDRepository idRepos;
    
    private NNTPRepository nntpRepos;
    
    /**
     * The array of spooler runnables, each associated with a Worker thread
     */
    private SpoolerRunnable[] worker;

    /**
     * The directory containing entries to be spooled.
     */
    private File spoolPath;

    /**
     * The String form of the spool directory.
     */
    private String spoolPathString;

    /**
     * The time the spooler threads sleep between processing
     */
    private int threadIdleTime = 0;

    /**
     * The filesystem service
     */
    private FileSystem fileSystem;

    private Log logger;

    private HierarchicalConfiguration config;

    @PostConstruct
    public void init() throws Exception {
        int threadCount = config.getInt("threadCount", 1);
        threadIdleTime = config.getInt("threadIdleTime", 60 * 1000);
        spoolPathString = config.getString("spoolPath");
        worker = new SpoolerRunnable[threadCount];
        
        
        try {
            spoolPath = fileSystem.getFile(spoolPathString);
            if ( spoolPath.exists() == false ) {
                spoolPath.mkdirs();
            } else if (!(spoolPath.isDirectory())) {
                StringBuffer errorBuffer =
                    new StringBuffer(128)
                        .append("Spool directory is improperly configured.  The specified path ")
                        .append(spoolPathString)
                        .append(" is not a directory.");
                throw new ConfigurationException(errorBuffer.toString());
            }
        } catch (Exception e) {
            logger.fatal(e.getMessage(), e);
            throw e;
        }

        for ( int i = 0 ; i < worker.length ; i++ ) {
            worker[i] = new SpoolerRunnable(threadIdleTime,spoolPath, logger);
            worker[i].setRepository(nntpRepos);
            worker[i].setArticleIDRepository(idRepos);
        }
        

        // TODO: Replace this with a standard Avalon thread pool
        for ( int i = 0 ; i < worker.length ; i++ ) {
            new Thread(worker[i],"NNTPSpool-"+i).start();
        }
    }

    /**
     * Sets the repository used by this spooler.
     *
     * @param repo the repository to be used
     */
    @Resource(name="org.apache.james.nntpserver.repository.NNTPRepository")
    void setNNTPRepository(NNTPRepository nntpRepos) {
        this.nntpRepos = nntpRepos;
    }

    /**
     * Sets the article id repository used by this spooler.
     *
     * @param articleIDRepo the article id repository to be used
     */
    @Resource(name="org.apache.james.nntpserver.repository.ArticleIDRepository")
    void setArticleIDRepository(ArticleIDRepository idRepos) {
        this.idRepos = idRepos;
    }

    /**
     * Returns (and creates, if the directory doesn't already exist) the
     * spool directory
     *
     * @return the spool directory
     */
    File getSpoolPath() {
        return spoolPath;
    }

    /**
     * A static inner class that provides the body for the spool
     * threads.
     */
    static class SpoolerRunnable implements Runnable {

        private static final Lock lock = new Lock();

        /**
         * The directory containing entries to be spooled.
         */
        private final File spoolPath;

        /**
         * The time the spooler thread sleeps between processing
         */
        private final int threadIdleTime;

        /**
         * The article ID repository used by this spooler thread
         */
        private ArticleIDRepository articleIDRepo;

        /**
         * The NNTP repository used by this spooler thread
         */
        private NNTPRepository repo;

        private Log logger;
        
        SpoolerRunnable(int threadIdleTime,File spoolPath, Log logger) {
            this.threadIdleTime = threadIdleTime;
            this.spoolPath = spoolPath;
            this.logger = logger;
        }

        /**
         * Sets the article id repository used by this spooler thread.
         *
         * @param articleIDRepo the article id repository to be used
         */
        void setArticleIDRepository(ArticleIDRepository articleIDRepo) {
            this.articleIDRepo = articleIDRepo;
        }

        /**
         * Sets the repository used by this spooler thread.
         *
         * @param repo the repository to be used
         */
        void setRepository(NNTPRepository repo) {
            this.repo = repo;
        }

        /**
         * The threads race to grab a lock. if a thread wins it processes the article,
         * if it loses it tries to lock and process the next article.
         */
        public void run() {
            logger.debug(Thread.currentThread().getName() + " is the NNTP spooler thread.");
            try {
                while ( Thread.interrupted() == false ) {
                    String[] list = spoolPath.list();
                    if (list.length > 0) logger.debug("Files to process: "+list.length);
                    for ( int i = 0 ; i < list.length ; i++ ) {
                        if ( lock.lock(list[i]) ) {
                            File f = new File(spoolPath,list[i]).getAbsoluteFile();
                            logger.debug("Processing file: "+f.getAbsolutePath());
                            try {
                                process(f);
                            } catch(Throwable ex) {
                                logger.debug("Exception occured while processing file: "+
                                                  f.getAbsolutePath(),ex);
                            } finally {
                                lock.unlock(list[i]);
                            }
                        }
                        list[i] = null; // release the string entry;
                    }
                    list = null; // release the array;
                    // this is good for other non idle threads
                    try {
                        Thread.sleep(threadIdleTime);
                    } catch(InterruptedException ex) {
                        // Ignore and continue
                    }
                }
            } finally {
                Thread.interrupted();
            }
        }

        /**
         * Process a file stored in the spool.
         *
         * @param spoolFile the spool file being processed
         */
        private void process(File spoolFile) throws Exception {
            StringBuffer logBuffer =
                new StringBuffer(160)
                        .append("process: ")
                        .append(spoolFile.getAbsolutePath())
                        .append(",")
                        .append(spoolFile.getCanonicalPath());
            logger.debug(logBuffer.toString());
            final MimeMessage msg;
            String articleID;
            // TODO: Why is this a block?
            {   // Get the message for copying to destination groups.
                FileInputStream fin = new FileInputStream(spoolFile);
                try {
                    msg = new MimeMessage(null,fin);
                } finally {
                    IOUtil.shutdownStream(fin);
                }

                String lineCount = null;
                String[] lineCountHeader = msg.getHeader("Lines");
                if (lineCountHeader == null || lineCountHeader.length == 0) {
                    BufferedReader rdr = new BufferedReader(new InputStreamReader(msg.getDataHandler().getInputStream()));
                    int lines = 0;
                    while (rdr.readLine() != null) {
                        lines++;
                    }

                    lineCount = Integer.toString(lines);
                    rdr.close();

                    msg.setHeader("Lines", lineCount);
                }

                // ensure no duplicates exist.
                String[] idheader = msg.getHeader("Message-Id");
                articleID = ((idheader != null && (idheader.length > 0))? idheader[0] : null);
                if ((articleID != null) && ( articleIDRepo.isExists(articleID))) {
                    logger.debug("Message already exists: "+articleID);
                    if (spoolFile.delete() == false)
                        logger.error("Could not delete duplicate message from spool: " + spoolFile.getAbsolutePath());
                    return;
                }
                if ( articleID == null || lineCount != null) {
                    if (articleID == null) {
                        articleID = articleIDRepo.generateArticleID();
                        msg.setHeader("Message-Id", articleID);
                    }
                    FileOutputStream fout = new FileOutputStream(spoolFile);
                    try {
                        msg.writeTo(fout);
                    } finally {
                        IOUtil.shutdownStream(fout);
                    }
                }
            }

            String[] headers = msg.getHeader("Newsgroups");
            Properties prop = new Properties();
            if (headers != null) {
                for ( int i = 0 ; i < headers.length ; i++ ) {
                    StringTokenizer tokenizer = new StringTokenizer(headers[i],",");
                    while ( tokenizer.hasMoreTokens() ) {
                        String groupName = tokenizer.nextToken().trim();
                        logger.debug("Copying message to group: "+groupName);
                        NNTPGroup group = repo.getGroup(groupName);
                        if ( group == null ) {
                            logger.error("Couldn't add article with article ID " + articleID + " to group " + groupName + " - group not found.");
                            continue;
                        }

                        FileInputStream newsStream = new FileInputStream(spoolFile);
                        try {
                            NNTPArticle article = group.addArticle(newsStream);
                            prop.setProperty(group.getName(),article.getArticleNumber() + "");
                        } finally {
                            IOUtil.shutdownStream(newsStream);
                        }
                    }
                }
            }
            articleIDRepo.addArticle(articleID,prop);
            boolean delSuccess = spoolFile.delete();
            if ( delSuccess == false ) {
                logger.error("Could not delete file: " + spoolFile.getAbsolutePath());
            }
        }
    } // class SpoolerRunnable

    /**
     * Setter for the fileSystem service
     * 
     * @param fileSystem fs
     */
    @Resource(name="org.apache.james.services.FileSystem")
    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    
    
    @Resource(name="org.apache.commons.configuration.Configuration")
    public void configure(HierarchicalConfiguration config) {
        this.config = config;
    }

    @Resource(name="org.apache.commons.logging.Log")
    public void setLog(Log log) {
        this.logger = log;
    }
}
