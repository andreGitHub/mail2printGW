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

import java.util.ArrayList;
import java.util.HashMap;
import mail2printgw.Certificate.CertificateCheck;
import mail2printgw.MailAcc;
import mail2printgw.MailFetcher2;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author eddi
 */
public class TestDifferentMailAccs extends mail2printgw.Tests.Test{
    
    public TestDifferentMailAccs() {
        TestToolbox.createConfigFileParserWithCupsPdfPrinter();
        TestToolbox.createPrintManagerWithDebugModeOn();
    }

    @Test
    public void connectToImapsAcc() {
        origOut.println("-- tdm - connectToImapsAcc");
        
        //Set Information of Mail - account
        MailAcc ma = TestToolbox.getMailAcc_myImap();
        
        CertificateCheck cc = new CertificateCheck();
        if(!cc.hasValidCertificate(ma.url, ma.port, ma.useSTARTTLS)) {
            assertTrue("-- tdm - Please correct certificate probelems  with \""
                    + ma.url + ":" + ma.port + "\" at first.", false);
            return;
        }
        performAccTestWithAnalisationOfOutput(ma);
    }
    
    @Test
    public void connectToStarttlsImapAccWithStarttls() {
        origOut.println("-- tdm - connectToImapAccWithStarttls");
        
        //Set Information of Mail - account
        MailAcc ma = TestToolbox.getMailAcc_aschuetze();
        
        CertificateCheck cc = new CertificateCheck();
        if(!cc.hasValidCertificate(ma.url, ma.port, ma.useSTARTTLS)) {
            assertTrue("-- tdm - Please correct certificate probelems  with \""
                    + ma.url + ":" + ma.port + "\" at first.", false);
            return;
        }
        performAccTestWithAnalisationOfOutput(ma);
    }
    
    @Test
    public void connectToStarttlsImapAccWithoutStarttls_FAIL() {
        origOut.println("-- tdm - connectToImapAccWithoutStarttls");
        
        //Set Information of Mail - account
        MailAcc ma = TestToolbox.getMailAcc_aschuetze();
        ma.useSTARTTLS = false;
        
        CertificateCheck cc = new CertificateCheck();
        if(!cc.hasValidCertificate(ma.url, ma.port, true)) {
            assertTrue("-- tdm - Please correct certificate probelems  with \""
                    + ma.url + ":" + ma.port + "\" at first.", false);
            return;
        }
        
        //create test somehow different than other tests
        origOut.println("-- tdm - run test-method");
        HashMap<Integer, MailAcc> accs = new HashMap<Integer, MailAcc>();
        accs.put(0, ma);
        
        origOut.println("-- tdm - clear acc");
        TestToolbox.clearAcc(ma);
        assertTrue(baosErr.toString().contains("No login methods supported!"));
        baosErr.reset();
        
        origOut.println("-- tdm - count mails");
        MailFetcher2 mf = new MailFetcher2();
        mf.getNumberOfMailsInMailAccount(ma);
        assertTrue(baosErr.toString().contains("No login methods supported!"));
        baosErr.reset();
        
        origOut.println("-- tdm - print");
        mf.printAllAttachmentsOfAllMailsOfAllAccs(accs);
        assertTrue(baosErr.toString().contains("No login methods supported!"));
        baosErr.reset();
    }
    
    private void performAccTestWithAnalisationOfOutput(MailAcc acc) {
        origOut.println("-- tdm - run test-method");
        HashMap<Integer, MailAcc> accs = new HashMap<Integer, MailAcc>();
        accs.put(0, acc);
        
        origOut.println("-- tdm - clear acc");
        TestToolbox.clearAcc(acc);
        checkOutput();
        
        
        origOut.println("-- tdm - count mails");
        MailFetcher2 mf = new MailFetcher2();
        assertTrue(0 == mf.getNumberOfMailsInMailAccount(acc));
        checkOutput();
        
        
        origOut.println("-- tdm - print");
        mf.printAllAttachmentsOfAllMailsOfAllAccs(accs);
        checkOutput();
        
        
        origOut.println("-- tdm - send mail");
        ArrayList<String> fts = new ArrayList<String>();
        fts.add("test/testPDFs/pdf2.pdf");
        TestToolbox.sendMessageOverWebDe(
                TestToolbox.getCorrectMailAddress(acc.username, acc.url), fts);
        checkOutput();
        
        assertTrue(baosOut != null);//
        origOut.println("-- tdm - wait till mail arrive");
        TestToolbox.waitTillXMessagesFoundInAccount(acc, 1);
        assertTrue(baosOut != null); //
        checkOutput();
        
        assertTrue(baosOut != null);//
        
        
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
        }
        
        this.getNewFilesInCupsPdfPrintFolder();
        origOut.println("-- tdm - print");
        mf.printAllAttachmentsOfAllMailsOfAllAccs(accs);
        
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
        }
        
        
        ArrayList<String> newFiles = this.getNewFilesInCupsPdfPrintFolder();
        assertTrue(2 == newFiles.size());
        assertTrue(baosOut != null);//
        assertTrue(baosErr != null);//
        
        checkOutput();
        //origOut.println("strOut:\n" + baosOut.toString() + "\nstrErr:\n" + baosErr.toString());
        origOut.println("-- tdm - finished test-method");
    }
}