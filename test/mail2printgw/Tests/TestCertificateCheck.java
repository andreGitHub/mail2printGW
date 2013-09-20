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
import java.util.logging.Level;
import java.util.logging.Logger;
import mail2printgw.Certificate.CertificateCheck;
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
        System.out.println("-- tcc - setUp");
        //System.out.println("constructor");
        System.setProperty("javax.net.ssl.keyStore", pathToCertificateFile);
        //System.setProperty("javax.net.ssl.trustStore", "falsch");
        clearFile(pathToCertificateFile);
    }
    
    @After
    public void tearDown() {
        //System.out.println("tearDown");
        clearFile(pathToCertificateFile);
    }
    
    @Test
    public void connectToLdapsHost() {
        //ssh -L 636:intserv-new.net.t-labs.tu-berlin.de:636 -l aschuetze roadrunner.net.t-labs.tu-berlin.de
        System.out.println("-- tcc - connectToLdapsHost");
        CertificateCheck cc = new CertificateCheck();
        
        boolean ret = cc.hasValidCertificate("127.0.0.1", 636, false);
        assertFalse(ret);
        ret = cc.importCertificate("127.0.0.1", 636, false);
        assertTrue(ret);
        assertFalse(cc.importCertificate("127.0.0.1", 636, false));
        assertTrue(cc.hasValidCertificate("127.0.0.1", 636, false));
    }
    
    @Test
    public void connectToImapsAndLdapsHost() {
        //ssh -L 636:intserv-new.net.t-labs.tu-berlin.de:636 -l aschuetze roadrunner.net.t-labs.tu-berlin.de
        System.out.println("-- tcc - connectToImapsAndLdapsHost");
        CertificateCheck cc = new CertificateCheck();
        
        assertFalse(cc.hasValidCertificate("www.google.de", 443, false));
        assertTrue(cc.importCertificate("www.google.de", 443, false));
        assertFalse(cc.importCertificate("www.google.de", 443, false));
        assertTrue(cc.hasValidCertificate("www.google.de", 443, false));
        
        boolean ret = cc.hasValidCertificate("127.0.0.1", 636, false);
        assertFalse(ret);
        ret = cc.importCertificate("127.0.0.1", 636, false);
        assertTrue(ret);
        assertFalse(cc.importCertificate("127.0.0.1", 636, false));
        assertTrue(cc.hasValidCertificate("127.0.0.1", 636, false));
    }
    
    @Test
    public void connectToHostWithNoCertificateImapCommon(){
        System.out.println("-- tcc - connectToHostWithNoCertificateImapCommon");
        //clearFile(pathToCertificateFile);
        CertificateCheck cc = new CertificateCheck();
        assertFalse(cc.hasValidCertificate("www.google.de", 443, false));
    }
    
    @Test
    public void connectToHostWithNoCertificateImapStarttls(){
        System.out.println("-- tcc - connectToHostWithNoCertificateImapStarttls");
        //clearFile(pathToCertificateFile);
        CertificateCheck cc = new CertificateCheck();
        assertFalse(cc.hasValidCertificate("mail.net.t-labs.tu-berlin.de.", 143, false));
    }
    
    @Test
    public void importCertOfHostImapCommon(){
        System.out.println("-- tcc - importCertOfHostImapCommon");
        //clearFile(pathToCertificateFile);
        CertificateCheck cc = new CertificateCheck();
        assertFalse(cc.hasValidCertificate("www.google.de", 443, false));
        assertTrue(cc.importCertificate("www.google.de", 443, false));
        assertFalse(cc.importCertificate("www.google.de", 443, false));
        assertTrue(cc.hasValidCertificate("www.google.de", 443, false));
    }
    
    @Test
    public void importCertOfHostImapStarttls(){
        System.out.println("-- tcc - importCertOfHostImapStarttls");
        //clearFile(pathToCertificateFile);
        CertificateCheck cc = new CertificateCheck();
        assertFalse(cc.hasValidCertificate("mail.net.t-labs.tu-berlin.de.", 143, true));
        assertFalse(cc.hasValidCertificate("mail.net.t-labs.tu-berlin.de.", 143, true));
        assertTrue(cc.importCertificate("mail.net.t-labs.tu-berlin.de.", 143, true));
        assertFalse(cc.importCertificate("mail.net.t-labs.tu-berlin.de.", 143, true));
        assertTrue(cc.hasValidCertificate("mail.net.t-labs.tu-berlin.de.", 143, true));
    }

    @Test
    public void testWithMoreThanOneCert(){
        System.out.println("-- tcc - testWithMoreThanOneCert");
        //clearFile(pathToCertificateFile);
        CertificateCheck cc = new CertificateCheck();
        assertFalse(cc.hasValidCertificate("mail.net.t-labs.tu-berlin.de.", 143, true));
        assertTrue(cc.importCertificate("mail.net.t-labs.tu-berlin.de.", 143, true));
        assertFalse(cc.importCertificate("mail.net.t-labs.tu-berlin.de.", 143, true));
        assertTrue(cc.hasValidCertificate("mail.net.t-labs.tu-berlin.de.", 143, true));
        
        assertFalse(cc.hasValidCertificate("www.google.de", 443, false));
        assertTrue(cc.importCertificate("www.google.de", 443, false));
        assertFalse(cc.importCertificate("www.google.de", 443, false));
        assertTrue(cc.hasValidCertificate("www.google.de", 443, false));
        
        assertFalse(cc.hasValidCertificate("imap.mail.yahoo.com", 993, false));
        assertFalse(cc.hasValidCertificate("imap.web.de", 993, false));
        assertTrue(cc.importCertificate("imap.mail.yahoo.com", 993, false));
        assertTrue(cc.importCertificate("imap.web.de", 993, false));
        assertFalse(cc.importCertificate("imap.mail.yahoo.com", 993, false));
        assertFalse(cc.importCertificate("imap.web.de", 993, false));
        assertTrue(cc.hasValidCertificate("imap.mail.yahoo.com", 993, false));
        assertTrue(cc.hasValidCertificate("imap.web.de", 993, false));
    }
    
    @Test
    public void connectToHostWithExsistingCertificate() {
        System.out.println("-- tcc - connectToHostWithExsistingCertificate");
        System.clearProperty("javax.net.ssl.keyStore");
        CertificateCheck cc = new CertificateCheck();
        assertTrue(cc.hasValidCertificate("www.google.de", 443, false));
        assertFalse(cc.importCertificate("www.google.de", 443, false));
        assertTrue(cc.hasValidCertificate("www.google.de", 443, false));
    }
    
    /* skip pop3 test - support follow later
    @Test
    public void importCertOfPop3Host() {
        System.out.println("-- tcc - importCertOfPop3Host");
        //clearFile(pathToCertificateFile);
        CertificateCheck cc = new CertificateCheck();
        assertFalse("perform certificate check with pop3 server",
                cc.hasValidCertificate("pop3.web.de", 110, false));
        assertFalse("perform certificate check with pop3 server using ",
                cc.hasValidCertificate("pop3.web.de", 110, true));
        assertTrue("import certificate of pop3 server",
                cc.importCertificate("pop3.web.de", 110, false));
    }
    */
}
