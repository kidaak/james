package org.apache.james.imapserver;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.net.Socket;
import java.io.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;

import org.apache.james.test.SimpleFileProtocolTest;
import org.apache.james.remotemanager.UserManagementTest;

public class TestNonAuthenticated
        extends SimpleFileProtocolTest
{
    public TestNonAuthenticated( String name )
    {
        super( name );
        _port = 143;
    }

    public void setUp() throws Exception
    {
        super.setUp();
        addTestFile( "Welcome.test", _preElements );
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new TestNonAuthenticated( "Capability" ) );
        suite.addTest( new TestNonAuthenticated( "Authenticate" ) );
        suite.addTest( new TestAuthenticated( "Login" ) );
        suite.addTest( new TestNonAuthenticated( "Logout" ) );

        return suite;
    }

}
