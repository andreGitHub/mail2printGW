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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import mail2printgw.Certificate.CertificateCheck;
import mail2printgw.ConfigFileParser;
import mail2printgw.ImapAcc;
import mail2printgw.MailFetcher2;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andre
 */
public class TestCertificateCheck {
    private String pathToCertificateFile = "/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.25-2.3.12.3.fc19.i386/jre/lib/security/cacertsTEMP";
    public TestCertificateCheck() {
        //System.out.println("constructor");
        System.setProperty("javax.net.ssl.keyStore", pathToCertificateFile);
        //System.setProperty("javax.net.ssl.trustStore", "falsch");
        clearFile(pathToCertificateFile);

         
    }
    
    private void clearFile(String path){
        try {
            FileOutputStream fos = new FileOutputStream(new File(path));
            fos.write((new String()).getBytes());
            fos.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TestCertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TestCertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @BeforeClass
    public static void setUpClass() {
        //System.out.println("setUpClass");
    }
    
    @AfterClass
    public static void tearDownClass() {
        //System.out.println("tearDownClass");
    }
    
    @Before
    public void setUp() {
        //System.out.println("setUp");
    }
    
    @After
    public void tearDown() {
        //System.out.println("tearDown");
        clearFile(pathToCertificateFile);
    }
    
    @Test
    public void connectToHostWithNoCertificateImapCommon(){
        clearFile(pathToCertificateFile);
        CertificateCheck cc = new CertificateCheck();
        assertFalse(cc.hasValidCertificate("www.google.de", 443));
    }
    
    @Test
    public void connectToHostWithNoCertificateImapStarttls(){
        clearFile(pathToCertificateFile);
        CertificateCheck cc = new CertificateCheck();
        assertFalse(cc.hasValidCertificate("mail.net.t-labs.tu-berlin.de.", 143));
    }
    
    @Test
    public void importCertOfHostImapCommon(){
        clearFile(pathToCertificateFile);
        CertificateCheck cc = new CertificateCheck();
        assertFalse(cc.hasValidCertificate("www.google.de", 443));
        assertTrue(cc.importCertificate("www.google.de", 443));
        assertFalse(cc.importCertificate("www.google.de", 443));
        assertTrue(cc.hasValidCertificate("www.google.de", 443));
    }
    
    @Test
    public void importCertOfHostImapStarttls(){
        clearFile(pathToCertificateFile);
        CertificateCheck cc = new CertificateCheck();
        assertFalse(cc.hasValidCertificate("mail.net.t-labs.tu-berlin.de.", 143));
        assertTrue(cc.importCertificate("mail.net.t-labs.tu-berlin.de.", 143));
        assertFalse(cc.importCertificate("mail.net.t-labs.tu-berlin.de.", 143));
        assertTrue(cc.hasValidCertificate("mail.net.t-labs.tu-berlin.de.", 143));
    }

    @Test
    public void testWithMoreThanOneCert(){
        clearFile(pathToCertificateFile);
        CertificateCheck cc = new CertificateCheck();
        assertFalse(cc.hasValidCertificate("mail.net.t-labs.tu-berlin.de.", 143));
        assertTrue(cc.importCertificate("mail.net.t-labs.tu-berlin.de.", 143));
        assertFalse(cc.importCertificate("mail.net.t-labs.tu-berlin.de.", 143));
        assertTrue(cc.hasValidCertificate("mail.net.t-labs.tu-berlin.de.", 143));
        
        assertFalse(cc.hasValidCertificate("www.google.de", 443));
        assertTrue(cc.importCertificate("www.google.de", 443));
        assertFalse(cc.importCertificate("www.google.de", 443));
        assertTrue(cc.hasValidCertificate("www.google.de", 443));
        
        assertFalse(cc.hasValidCertificate("imap.mail.yahoo.com", 993));
        assertFalse(cc.hasValidCertificate("imap.web.de", 993));
        assertTrue(cc.importCertificate("imap.mail.yahoo.com", 993));
        assertTrue(cc.importCertificate("imap.web.de", 993));
        assertFalse(cc.importCertificate("imap.mail.yahoo.com", 993));
        assertFalse(cc.importCertificate("imap.web.de", 993));
        assertTrue(cc.hasValidCertificate("imap.mail.yahoo.com", 993));
        assertTrue(cc.hasValidCertificate("imap.web.de", 993));
    }
    
    @Test
    public void connectToHostWithExsistingCertificate() {
        //System.out.println("ConnectToHostWithExsistingCertificate");
        System.clearProperty("javax.net.ssl.keyStore");
        CertificateCheck cc = new CertificateCheck();
        assertFalse(cc.hasValidCertificate("www.google.de", 443));
        assertTrue(cc.importCertificate("www.google.de", 443));
        assertTrue(cc.hasValidCertificate("www.google.de", 443));
    }
}
