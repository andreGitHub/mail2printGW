   /*
    * Copyright 2013 André Schütze
    * 
    * This file is part of mail2printGW.
    * 
    * Mail2printGW is free software: you can redistribute it and/or modify
    * it under the terms of the GNU General Public License as published by
    * the Free Software Foundation, either version 3 of the License, or
    * (at your option) any later version.
    * 
    * Mail2printGW is distributed in the hope that it will be useful,
    * but WITHOUT ANY WARRANTY; without even the implied warranty of
    * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    * GNU General Public License for more details.
    * 
    * You should have received a copy of the GNU General Public License
    * along with mail2printGW.  If not, see <http://www.gnu.org/licenses/>.
    */


package mail2printgw.Tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import mail2printgw.MailAcc;
import mail2printgw.MailFetcher2;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author eddi
 */
public class TestFetchingMails extends mail2printgw.Tests.Test{
    private static PrintStream origOut = System.out;
    private static PrintStream origErr = System.err;
    
    private static ByteArrayOutputStream baosOut = null;
    private static ByteArrayOutputStream baosErr = null;
    
    public TestFetchingMails() {
        super();
        System.out.println("constructor");
    }

    @AfterClass
    public static void tearDownClass() {
        System.out.println("tearDownClass");
    }

    @Test
    public void countMails() {
        System.out.println("countMails");

        MailAcc ma = TestToolbox.getMailAcc_myImap();
        MailFetcher2 mf2 = new MailFetcher2();

        TestToolbox.clearAcc(ma);

        assertTrue("count messages.", mf2.getNumberOfMailsInMailAccount(ma) == 0);

        ArrayList<String> fts = new ArrayList<String>();
        fts.add("test/testPDFs/pdf2.pdf");
        TestToolbox.sendMessageOverWebDe(
                TestToolbox.getCorrectMailAddress(ma.username, ma.url), fts);

        TestToolbox.waitTillXMessagesFoundInAccount(ma, 1);
        assertTrue(mf2.getNumberOfMailsInMailAccount(ma) == 1);
        TestToolbox.clearAcc(ma);
    }

    @Test
    public void printMailsInEmptyAcc() {
        System.out.println("printMailsInEmptyAcc");
        MailAcc ma = TestToolbox.getMailAcc_myImap();

        MailFetcher2 mf = new MailFetcher2();
        int no = mf.getNumberOfMailsInMailAccount(ma);
        if (no != 0) {
            System.out.println("clear " + no + " mail(s) in acc");
            TestToolbox.clearAcc(ma);
        }
        System.out.println("print all attachments of empty acc.");
        mf.printAllAttachmentsOfAllMailsOfAllAccs(ma);
    }
    
    @Test
    public void putOneMailWithWebDeInMailAcc() {
        MailAcc ma = TestToolbox.getMailAcc_aschuetze();
        TestToolbox.clearAcc(ma);
        assertTrue(0 == TestToolbox.getNumberOfMessages(ma));
        
        File f = new File("test/testPDFs/pdf2.pdf");
        ArrayList<String> fts = new ArrayList<String>();
        fts.add("test/testPDFs/pdf2.pdf");
        TestToolbox.sendMessageOverWebDe("aschuetze@net.t-labs.tu-berlin.de", fts);
        
        TestToolbox.waitTillXMessagesFoundInAccount(ma, 1);
        assertTrue(1 == TestToolbox.getNumberOfMessages(ma));
    }
    /*
    @Test
    public void putOneMailWithYahooSeInMailAcc() {
        MailAcc ma = TestToolbox.getMailAcc_aschuetze();
        TestToolbox.clearAcc(ma);
        assertTrue(0 == TestToolbox.getNumberOfMessages(ma));
        
        File f = new File("test/testPDFs/pdf2.pdf");
        ArrayList<String> fts = new ArrayList<String>();
        fts.add("test/testPDFs/pdf2.pdf");
        TestToolbox.sendMessageOverYahooDe("aschuetze@net.t-labs.tu-berlin.de", fts);
        
        TestToolbox.waitTillXMessagesFoundInAccount(ma, 1);
        assertTrue(1 == TestToolbox.getNumberOfMessages(ma));
    }
    */
    /*
    @Test
    public void putOneMailInInetAcc() {
        File f = new File("test/testPDFs/pdf2.pdf");
        ArrayList<String> fts = new ArrayList<String>();
        TestToolbox.sendMessageOverWebDe("aschuetze@net.t-labs.tu-berlin.de", fts);
        origOut.println("path " + f.getAbsolutePath());
    }
    */
    /*
    @Test
    public void putOneMailInMyImapAcc() {
        File f = new File("test/testPDFs/pdf2.pdf");
        ArrayList<String> fts = new ArrayList<String>();
        TestToolbox.sendMessageOverWebDe("myImap@yahoo.de", fts);
        origOut.println("path " + f.getAbsolutePath());
    }
    */
}