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

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.AccessControlException;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;
import org.apache.james.imapserver.MailboxException;

import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.List;

/**
 * List Command for listing some mailboxes. 
 * See RFC 2060 for more details.
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.2 on 04 Aug 2002
 */

class ListCommand extends AuthenticatedSelectedStateCommand
{
    public ListCommand()
    {
        this.commandName = "LIST";

        this.getArgs().add( new AstringArgument( "reference name" ) );
        this.getArgs().add( new AstringArgument( "mailbox" ) );
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String command = this.commandName;

        boolean subscribeOnly;
        if ( command.equalsIgnoreCase( "LIST" ) ) {
            subscribeOnly = false;
        }
        else {
            subscribeOnly = true;
        }

        String reference = (String) argValues.get( 0 );
        String folder = (String) argValues.get( 1 );

        Collection list = null;
        try {
        System.out.println("getImapHost: "+session.getImapHost().getClass().getName());
            list = session.getImapHost().listMailboxes( session.getCurrentUser(), reference, folder,
                                                        subscribeOnly );
            if ( list == null ) {
                session.noResponse( command, " unable to interpret mailbox" );
            }
            else if ( list.size() == 0 ) {
                getLogger().debug( "List request matches zero mailboxes: " + request.getCommandRaw() );
                session.okResponse( command );
            }
            else {
                Iterator it = list.iterator();
                while ( it.hasNext() ) {
                    String listResponse = (String) it.next();
                    session.getOut().println( UNTAGGED + SP + command.toUpperCase()
                                               + SP + listResponse );
                    getLogger().debug( UNTAGGED + SP + command.toUpperCase()
                                       + SP + listResponse );
                }
                session.okResponse( command );
            }
        }
        catch ( MailboxException mbe ) {
            if ( mbe.isRemote() ) {
                session.noResponse( command, "[REFERRAL "
                                           + mbe.getRemoteServer() + "]"
                                           + SP + "Wrong server. Try remote." );
            }
            else {
                session.noResponse( command, "No such mailbox" );
            }
            return true;
        }
        catch ( AccessControlException ace ) {
            session.noResponse( command, "No such mailbox" );
            session.logACE( ace );
            return true;
        }

        if ( session.getState() == ImapSessionState.SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        return true;
    }
}
