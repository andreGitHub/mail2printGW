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


package mail2printgw.Tests.configFileParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.After;
import org.junit.AfterClass;
import static java.nio.file.StandardCopyOption.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.activation.MimeType;
import mail2printgw.ConfigFileParser;
import mail2printgw.MailAcc;
import mail2printgw.Tests.Test;
import static org.junit.Assert.*;


/**
 * JUnit test for the Class ConfigFileParser.
 * 
 * @author eddi
 */
public class TestConfigFileParser extends Test{
    /*
    @BeforeClass
    public static void setUpClass() {
        //System.out.println("TestConfigFileParser.setUpClass()");
        File configFile = new File("gwTEST.conf");
        if(configFile != null && configFile.exists() && configFile.isFile()) {
            File backupFile = new File("./test/testConfigFileParser/backup/gw.conf");
            try {
                Files.move(configFile.toPath(), backupFile.toPath(), REPLACE_EXISTING);
            } catch (IOException ex) {
                Logger.getLogger(TestConfigFileParser.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
        
        Test.setUpClass();
    }
    */
    
    
    @AfterClass
    public static void tearDownClass() {
        File configFile = new File("gwTEST.conf");
        configFile.delete();
    }
    
    @org.junit.Test
    public void testEmptyConfigFile() {
        origOut.println("-- tcc - testEmptyConfigFile");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        assertTrue(null == cfp.getImapAccs());
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        assertTrue(null == cfp.getUrlToLdapServer());
        
        checkOutput();
    }
    
    @org.junit.Test
    public void testConfigFileWithComments() {
        origOut.println("-- tcc - testConfigFileWithOneComment");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("#comment\n");
            bw.write("    #comment");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        assertTrue(null == cfp.getImapAccs());
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        assertTrue(null == cfp.getUrlToLdapServer());
        assertTrue(null == cfp.getConvertCmd());
        assertTrue(null == cfp.getPrintCmd());
        
        checkOutput();
    }
    
    @org.junit.Test
    public void testConfigFileWithOneOptionAndACommentInTheSameLine() {
        origOut.println("-- tcc - testConfigFileWithOneOptionAndACommentInTheSameLine");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("cacertFilePath = /usr/lib/jvm/java-1.7.0-openjdk-1.7.0"
                    + ".25-2.3.12.3.fc19.i386/jre/lib/security/cacertsTEMP "
                    + "#it's only a path\n");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        assertTrue(null == cfp.getImapAccs());
        assertTrue(cfp.getPathToCertFile().equals("/usr/lib/jvm/java-1.7.0-"
                + "openjdk-1.7.0.25-2.3.12.3.fc19.i386/jre/lib/security/cacerts"
                + "TEMP"));
        assertTrue(null == cfp.getPrintableMimes());
        assertTrue(null == cfp.getUrlToLdapServer());
        
        checkOutput();
    }
    
    @org.junit.Test
    public void testWithWrongSectionNameOfMailAccount() {
        origOut.println("-- tcc - testWithWrongSectionNameOfMailAccount");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("[section]\n");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        assertTrue(null == cfp.getUrlToLdapServer());
        assertTrue(null == cfp.getImapAccs());
        
        assertTrue(baosOut.toString().equals(""));
        
        assertTrue(baosErr.toString().contains("SEVERE: Parser does not recognize line type"));
        
        baosOut.reset();
        baosErr.reset();
    }
    
    @org.junit.Test
    public void testWithAValidAnInvalidAndAValidSectionName() {
        origOut.println("-- tcc - testWithAValidAnInvalidAndAValidSectionName");
        //System.out.println("look:");
        
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("[imapAcc0]\n");
            bw.write("    url = mail.net.t-labs.tu-berlin.de\n" +
                     "    port = 143\n" +
                     "    importCert = true\n" +
                     "    protocol = imap\n" +
                     "    useSTARTTLS = true\n" +
                     "    username = aschuetze\n" +
                     "    password = mypass\n");
            bw.write("[falsch]\n");
            bw.write("    url = mail.net.t-labs.tu-berlin.de\n" +
                     "    port = 143\n" +
                     "    importCert = true\n" +
                     "    protocol = imap\n" +
                     "    useSTARTTLS = true\n" +
                     "    username = aschuetze\n" +
                     "    password = mypass\n");
            bw.write("[imapAcc2]\n");
            bw.write("    url = mail.net.t-labs.tu-berlin.de\n" +
                     "    port = 143\n" +
                     "    importCert = true\n" +
                     "    protocol = imap\n" +
                     "    useSTARTTLS = true\n" +
                     "    username = aschuetze\n" +
                     "    password = mypass\n");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        assertTrue(null == cfp.getUrlToLdapServer());
        assertTrue(null != cfp.getImapAccs());
        
        HashMap<Integer, MailAcc> accs = cfp.getImapAccs();
        assertFalse(accs.isEmpty());
        assertTrue(2 == accs.size());
        
        Set<Integer> keys = accs.keySet();
        for(Integer key : keys) {
            MailAcc actAcc = accs.get(key);
            checkAcc(actAcc);
        }
        
        String strOut = baosOut.toString();
        assertTrue("".equals(strOut));
        String strErr = baosErr.toString();
        
        //origOut.println(">" + strErr + "<");
        //showConfig();
        
        
        assertTrue(strErr.matches("^(([^(\\r?\\n)]*)(\\r?\\n)){17}$"));
        assertTrue(strErr.contains("Parser does not recognize line type"));
        assertTrue(strErr.contains("[falsch]"));
        assertTrue(strErr.contains("skip option of faulty section"));
        baosOut.reset();
        baosErr.reset();
    }
    
    @org.junit.Test
    public void testForEmptySection() {
        origOut.println("-- tcc - testForRightDefaultValuesOfMailAccount1");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("[imapAcc1]\n");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        assertTrue(null == cfp.getUrlToLdapServer());
        assertTrue(null == cfp.getImapAccs());
        
        checkOutput();
    }
    
    @org.junit.Test
    public void testForRightDefaultValuesOfMailAccount1() {
        origOut.println("-- tcc - testForRightDefaultValuesOfMailAccount1");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("[imapAcc1]\n");
            bw.write("username=aschuetze");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        assertTrue(null == cfp.getUrlToLdapServer());
        
        HashMap<Integer, MailAcc> accs = cfp.getImapAccs();
        assertTrue(1 == accs.size());
        int key = (Integer)accs.keySet().toArray()[0];
        MailAcc ma = accs.get(key);
        assertTrue(null != ma);
        assertTrue(ma.url == null);
        assertTrue(ma.port == 0);
        assertTrue(ma.importCert == false);
        assertTrue(ma.protocol == null);
        assertTrue(ma.useSTARTTLS == false);
        assertTrue("aschuetze".equals(ma.username));
        assertTrue(ma.password == null);
        
        checkOutput();
    }
    
    @org.junit.Test
    public void testForRightDefaultValuesOfMailAccount2() {
        origOut.println("-- tcc - testForRightDefaultValuesOfMailAccount2");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("[imapAcc1]\n");
            bw.write("password=mypass");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        assertTrue(null == cfp.getUrlToLdapServer());
        
        HashMap<Integer, MailAcc> accs = cfp.getImapAccs();
        assertTrue(1 == accs.size());
        int key = (Integer)accs.keySet().toArray()[0];
        MailAcc ma = accs.get(key);
        assertTrue(null != ma);
        assertTrue(ma.url == null);
        assertTrue(ma.port == 0);
        assertTrue(ma.importCert == false);
        assertTrue(ma.protocol == null);
        assertTrue(ma.useSTARTTLS == false);
        assertTrue(ma.username == null);
        assertTrue("mypass".equals(ma.password));
        
        checkOutput();
    }
    
    @org.junit.Test
    public void testSectionIncludingAnOptionLineOfGeneralPart() {
        origOut.println("-- tcc - testSectionIncludingAnOptionLineOfGeneralPart");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("ldapURL = ldaps://127.0.0.1:636\n");
            bw.write("[imapAcc0]\n");
            bw.write("    url = mail.net.t-labs.tu-berlin.de\n" +
                     "    port = 143\n" +
                     "    importCert = true\n" +
                     "ldapURL = ldaps://127.0.0.1:389\n" +
                     "    protocol = imap\n" +
                     "    useSTARTTLS = true\n" +
                     "    username = aschuetze\n" +
                     "    password = mypass\n");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        
        assertTrue("ldaps://127.0.0.1:636".equals(cfp.getUrlToLdapServer()));
        
        HashMap<Integer, MailAcc> accs = cfp.getImapAccs();
        assertTrue(1 == accs.size());
        int key = (Integer)accs.keySet().toArray()[0];
        MailAcc ma = accs.get(key);
        checkAcc(ma);
        
        String strOut = baosOut.toString();
        assertTrue(null != strOut);
        assertTrue(0 == strOut.length());
        assertTrue("".equals(strOut));
        
        String strErr = baosErr.toString();
        assertTrue(null != strErr);
        assertTrue(0 < strErr.length());
        assertTrue(strErr.contains("wrong Syntax in config-File."));
        
        baosOut.reset();
        baosErr.reset();
    }
    
    @org.junit.Test
    public void testConfigFileWithDuplicatedOptionWithSameValueInSection() {
        origOut.println("-- tcc - testConfigFileWithDuplicatedOptionWithSameValueInSection");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("[imapAcc0]\n");
            bw.write("    protocol = imap\n" +
                     "    url = mail.net.t-labs.tu-berlin.de\n" +
                     "    port = 143\n" +
                     "    importCert = true\n" +
                     "    protocol = imap\n" +
                     "    useSTARTTLS = true\n" +
                     "    username = aschuetze\n" +
                     "    password = mypass\n");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        assertTrue(null == cfp.getUrlToLdapServer());
        
        HashMap<Integer, MailAcc> accs = cfp.getImapAccs();
        assertTrue(1 == accs.size());
        int key = (Integer)accs.keySet().toArray()[0];
        MailAcc ma = accs.get(key);
        checkAcc(ma);
        
        checkOutput();
    }
    
    @org.junit.Test
    public void testConfigFileWithDuplicatedOptionWithDifferentValuesInSection() {
        origOut.println("-- tcc - testConfigFileWithDuplicatedOptionWithDifferentValuesInSection");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("[imapAcc5681]\n");
            bw.write("    protocol = imaps\n" +
                     "    url = mail.net.t-labs.tu-berlin.de\n" +
                     "    port = 143\n" +
                     "    importCert = true\n" +
                     "    protocol = imap\n" +
                     "    useSTARTTLS = true\n" +
                     "    username = aschuetze\n" +
                     "    password = mypass\n");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        assertTrue(null == cfp.getUrlToLdapServer());
        
        HashMap<Integer, MailAcc> accs = cfp.getImapAccs();
        assertTrue(1 == accs.size());
        int key = (Integer)accs.keySet().toArray()[0];
        MailAcc ma = accs.get(key);
        checkAcc(ma);
        
        checkOutput();
    }
    
    @org.junit.Test
    public void testConfigFileWithACommentBehindAMailAccountOption() {
        origOut.println("-- tcc - testConfigFileWithACommentBehindAMailAccountOption");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("[imapAcc7]\n");
            bw.write("    url = mail.net.t-labs.tu-berlin.de\n" +
                     "    port = 143#'whatever\n" +
                     "    importCert = true'it' is\n" +
                     "    protocol = imap\n" +
                     "    useSTARTTLS = true\n" +
                     "    username = aschuetze\n" +
                     "    password = mypass\n");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        assertTrue(null == cfp.getUrlToLdapServer());
        
        HashMap<Integer, MailAcc> accs = cfp.getImapAccs();
        assertTrue(1 == accs.size());
        int key = (Integer)accs.keySet().toArray()[0];
        MailAcc ma = accs.get(key);
        checkAcc(ma);
        
        checkOutput();
    }
    
    @org.junit.Test
    public void testConfigFileWithoutMailAccounts() {
        origOut.println("-- tcc - testConfigFileWithACommentBehindAMailAccountOption");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("printableMimes = application/pdf;application/octet-stream\n");
            bw.write("ldapURL = ldaps://127.0.0.1:636\n");
            bw.write("cacertFilePath = /usr/lib/jvm/java-1.7.0-openjdk-1.7.0"
                    + ".25-2.3.12.3.fc19.i386/jre/lib/security/cacertsTEMP\n");
            bw.write("tempDir = /home/eddi/NetBeansProjects/mail2printGW/test/testPDFs/send\n");
            bw.write("printCmd = shellCmdPrint\n");
            bw.write("convertCmd = shellCmdConvert");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        
        //origOut.println(">" + cfp.getUrlToLdapServer() + "\n" + baosOut.toString());
        
        assertTrue("ldaps://127.0.0.1:636".equalsIgnoreCase(cfp.getUrlToLdapServer()));
        assertTrue("/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.25-2.3.12.3.fc19.i386/jre/lib/security/cacertsTEMP"
                .equalsIgnoreCase(cfp.getPathToCertFile()));
        assertTrue("/home/eddi/NetBeansProjects/mail2printGW/test/testPDFs/send"
                .equalsIgnoreCase(cfp.getTmpDirForPDFs()));
        assertTrue("shellCmdPrint".equalsIgnoreCase(cfp.getPrintCmd()));
        assertTrue("shellCmdConvert".equalsIgnoreCase(cfp.getConvertCmd()));
        
        assertTrue(null != cfp.getPrintableMimes());
        ArrayList<MimeType> mimes =  cfp.getPrintableMimes();
        assertFalse(mimes.isEmpty());
        assertTrue(2 == mimes.size());
        MimeType m1 = mimes.get(0);
        MimeType m2 = mimes.get(1);
        assertTrue(null != m1);
        assertTrue(null != m2);
        
        if(m1.getBaseType().equals("application/pdf")) {
            assertTrue(m1.getBaseType().equals("application/pdf"));
            assertTrue(m2.getBaseType().equals("application/octet-stream"));
        } else {
            assertTrue(m1.getBaseType().equals("application/octet-stream"));
            assertTrue(m2.getBaseType().equals("application/pdf"));
        }
        
        HashMap<Integer, MailAcc> accs = cfp.getImapAccs();
        assertTrue(null == accs);
        
        checkOutput();
    }
    
    @org.junit.Test
    public void testValidConfigFileWithAllEntriesAndTwoAccounts() {
        origOut.println("-- tcc - testValidConfigFileWithAllEntriesAndTwoAccounts");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("#comment\n");
            bw.write("printableMimes = application/pdf\n");
            bw.write("ldapURL = ldaps://127.0.0.1:636\n");
            bw.write("cacertFilePath = /usr/lib/jvm/java-1.7.0-openjdk-1.7.0"
                    + ".25-2.3.12.3.fc19.i386/jre/lib/security/cacertsTEMP\n\n\n");
            
            bw.write("[imapAcc568]\n");
            bw.write("    url = mail.net.t-labs.tu-berlin.de\n" +
                     "    port = 143\n" +
                     "    importCert = true\n" +
                     "    protocol = imap\n" +
                     "    useSTARTTLS = true\n" +
                     "    username = aschuetze\n" +
                     "    password = mypass\n");
            
            bw.write("[imapAcc5681]\n");
            bw.write("    url = imap.yahoo.de\n" +
                     "    port = 993\n" +
                     "    importCert = false\n" +
                     "    protocol = imap\n" +
                     "    useSTARTTLS = true\n" +
                     "    username = asta\n" +
                     "    password = what");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        
        ConfigFileParser cfp = getNewConfigFileParser();
        
        assertTrue("ldaps://127.0.0.1:636".equalsIgnoreCase(cfp.getUrlToLdapServer()));
        assertTrue("/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.25-2.3.12.3.fc19.i386/jre/lib/security/cacertsTEMP"
                .equalsIgnoreCase(cfp.getPathToCertFile()));
        
        assertTrue(null != cfp.getPrintableMimes());
        ArrayList<MimeType> mimes =  cfp.getPrintableMimes();
        assertFalse(mimes.isEmpty());
        assertTrue(1 == mimes.size());
        MimeType mi = mimes.get(0);
        assertTrue(null != mi);
        assertTrue(mi.getBaseType().equals("application/pdf"));
        
        HashMap<Integer, MailAcc> accs = cfp.getImapAccs();
        assertTrue(2 == accs.size());
        
        Object[] keys = accs.keySet().toArray();
        MailAcc ma1 = accs.get(keys[0]);
        MailAcc ma2 = accs.get(keys[1]);
        assertTrue(ma1 != null);
        assertTrue(ma2 != null);
        
        if("mail.net.t-labs.tu-berlin.de".equalsIgnoreCase(ma1.url)) {
            checkAcc(ma1);
            ma1 = ma2;
        } else {
            checkAcc(ma2);
        }
        
        assertTrue("imap.yahoo.de".equals(ma1.url));
        assertTrue(993 == ma1.port);
        assertTrue(false == ma1.importCert);
        assertTrue("imap".equals(ma1.protocol));
        assertTrue(true == ma1.useSTARTTLS);
        assertTrue("asta".equals(ma1.username));
        assertTrue("what".equals(ma1.password));
        
        checkOutput();
    }
    
    @org.junit.Test
    public void testInvalidConfigFileWithAllEntriesButDoublicatedAccountSectionNames() {
        origOut.println("-- tcc - testInvalidConfigFileWithAllEntriesButDoublicatedAccountSectionNames");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("#comment\n");
            bw.write("printableMimes = application/pdf\n");
            bw.write("ldapURL = ldaps://127.0.0.1:636\n");
            bw.write("cacertFilePath = /usr/lib/jvm/java-1.7.0-openjdk-1.7.0"
                    + ".25-2.3.12.3.fc19.i386/jre/lib/security/cacertsTEMP\n\n\n");
            
            bw.write("[imapAcc5681]\n");
            bw.write("    url = mail.net.t-labs.tu-berlin.de\n" +
                     "    port = 143\n" +
                     "    importCert = true\n" +
                     "    protocol = imap\n" +
                     "    useSTARTTLS = true\n" +
                     "    username = aschuetze\n" +
                     "    password = mypass\n");
            
            bw.write("[imapAcc5681]\n");
            bw.write("    url = imap.yahoo.de\n" +
                     "    port = 993\n" +
                     "    importCert = false\n" +
                     "    protocol = imap\n" +
                     "    useSTARTTLS = true\n" +
                     "    username = asta\n" +
                     "    password = what");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        //showConfig();
        
        ConfigFileParser cfp = getNewConfigFileParser();
        
        assertTrue("ldaps://127.0.0.1:636".equalsIgnoreCase(cfp.getUrlToLdapServer()));
        assertTrue("/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.25-2.3.12.3.fc19.i386/jre/lib/security/cacertsTEMP"
                .equalsIgnoreCase(cfp.getPathToCertFile()));
        
        assertTrue(null != cfp.getPrintableMimes());
        ArrayList<MimeType> mimes =  cfp.getPrintableMimes();
        assertFalse(mimes.isEmpty());
        assertTrue(1 == mimes.size());
        MimeType mi = mimes.get(0);
        assertTrue(null != mi);
        assertTrue(mi.getBaseType().equals("application/pdf"));
        
        HashMap<Integer, MailAcc> accs = cfp.getImapAccs();
        assertTrue(1 == accs.size());
        
        Object[] keys = accs.keySet().toArray();
        MailAcc ma1 = accs.get(keys[0]);
        assertTrue(ma1 != null);
        
        checkAcc(ma1);
        
        String strOut = baosOut.toString();
        String strErr = baosErr.toString();
        
        assertTrue(strOut.length() == 0);
        
        //origOut.println(">" + strErr + "<");
        
        assertTrue(strErr.length() > 0);
        assertTrue(strOut.isEmpty());
        assertFalse(strErr.isEmpty());
        assertTrue(strOut.equals(""));
        assertTrue(strErr.contains("wrong Syntax in config-File. Section-Name "
                + "\"imapAcc5681\" allready exists."));
        assertTrue(strErr.contains("skip option of faulty section"));
        
        assertTrue(strErr.matches("^(([^(\\r?\\n)]*)(\\r?\\n)){16}$"));
        
        baosOut.reset();
        baosErr.reset();
    }
    
    @org.junit.Test
    public void testInvalidConfigFileWithOneOptionValueMissing() {
        origOut.println("-- tcc - testInvalidConfigFileWithOneOptionValueMissing");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("#comment\n");
            bw.write("printableMimes = \n");
            bw.write("ldapURL = \n");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        
        assertTrue(null == cfp.getUrlToLdapServer());
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        assertTrue(null == cfp.getImapAccs());
        
        String strOut = baosOut.toString();
        String strErr = baosErr.toString();
        
        assertTrue(strOut.length() == 0);
        assertTrue(strErr.length() > 0);
        assertTrue(strOut.isEmpty());
        assertFalse(strErr.isEmpty());
        assertTrue(strOut.equals(""));
        assertTrue(strErr.contains("Parser does not recognize line type"));
        assertTrue(strErr.matches("^(([^(\\r?\\n)]*)(\\r?\\n)){6}$"));
        
        baosOut.reset();
        baosErr.reset();
    }
    
    @org.junit.Test
    public void testForInvalidSectionNames() {
        origOut.println("-- tcc - testForInvalidSectionNames");
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("#comment\n");
            bw.write("[falsch]\n");
            bw.write("    url = mail.net.t-labs.tu-berlin.de\n" +
                     "    port = 143\n" +
                     "    importCert = true\n" +
                     "    protocol = imap\n" +
                     "    useSTARTTLS = true\n" +
                     "    username = aschuetze\n" +
                     "    password = mypass\n");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        
        assertTrue(null == cfp.getUrlToLdapServer());
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        
        HashMap<Integer, MailAcc> accs = cfp.getImapAccs();
        assertTrue(null == accs);
        
        String strOut = baosOut.toString();
        String strErr = baosErr.toString();
        
        assertTrue(strOut.length() == 0);
        assertTrue(strErr.length() > 0);
        assertTrue(strOut.isEmpty());
        assertFalse(strErr.isEmpty());
        assertTrue(strOut.equals(""));
        assertTrue(strErr.contains("Parser does not recognize line type"));
        assertTrue(strErr.matches("^(([^(\\r?\\n)]*)(\\r?\\n)){17}$"));
        
        baosOut.reset();
        baosErr.reset();
    }

    @org.junit.Test
    public void testInvalidConfigFileWithFaultyMimesOptionValue() {
        origOut.println("-- tcc - testInvalidConfigFileWithFaultyMimesOptionValue");
        
        BufferedWriter bw = getConfigFileWriter();
        try {
            bw.write("#comment\n");
            bw.write("printableMimes = ; ;");
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        ConfigFileParser cfp = getNewConfigFileParser();
        
        assertTrue(null == cfp.getUrlToLdapServer());
        assertTrue(null == cfp.getPathToCertFile());
        assertTrue(null == cfp.getPrintableMimes());
        assertTrue(null == cfp.getImapAccs());
        
        String strOut = baosOut.toString();
        String strErr = baosErr.toString();
        
        //origOut.println("out: " + strOut);
        //origOut.println("err: " + strErr);
        
        assertTrue(strOut.length() == 0);
        assertTrue(strErr.length() > 0);
        assertTrue(strOut.isEmpty());
        assertFalse(strErr.isEmpty());
        assertTrue(strOut.equals(""));
        assertTrue(strErr.contains("mimetypes are not specified correctly"));
        assertTrue(strErr.matches("^(([^(\\r?\\n)]*)(\\r?\\n)){2}$"));
        
        baosOut.reset();
        baosErr.reset();
    }
    
    private void checkAcc(MailAcc ma) {
/*
        bw.write("    url = mail.net.t-labs.tu-berlin.de\n" +
                 "    port = 143\n" +
                 "    importCert = true\n" +
                 "    protocol = imap\n" +
                 "    useSTARTTLS = true\n" +
                 "    username = aschuetze\n" +
                 "    password = mypass\n");
 */
        assertTrue("mail.net.t-labs.tu-berlin.de".equals(ma.url));
        assertTrue(143 == ma.port);
        assertTrue(true == ma.importCert);
        assertTrue("imap".equals(ma.protocol));
        assertTrue(true == ma.useSTARTTLS);
        assertTrue("aschuetze".equals(ma.username));
        assertTrue("mypass".equals(ma.password));
    }

    private BufferedWriter getConfigFileWriter() {
        File config = new File("gwTEST.conf");
        if(config != null && config.exists()) {
            config.delete();
        }
        
        if(config.exists()) {
            origOut.println("-- tcfp - ERROR: unable to delete config-file.");
        }
        
        FileWriter fw = null;
        try {
            fw = new FileWriter(config);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        return new BufferedWriter(fw);
    }
    
    private void showConfig() {
        File f = new File("gwTEST.conf");
        
        FileReader fr = null;;
        try {
            fr = new FileReader(f);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        BufferedReader br = new BufferedReader(fr);
        
        origOut.println("---------------------------------------------------");
        try {
            while(br.ready()) {
                origOut.println(br.readLine());
            }
        } catch (IOException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        origOut.println("---------------------------------------------------");
    }
    
    private ConfigFileParser getNewConfigFileParser() {
        Constructor<ConfigFileParser> constructor = null;
        Field configFilePath = null;
        
        try {
            configFilePath = ConfigFileParser.class.getDeclaredField("configFilePath");
            constructor = ConfigFileParser.class.getDeclaredConstructor();
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        constructor.setAccessible(true);
        configFilePath.setAccessible(true);
        ConfigFileParser cfp = null;
        /*
        try {
            cfp = constructor.newInstance();
        } catch (InstantiationException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
        try {
            configFilePath.set(null, "./gwTEST.conf");
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            cfp = constructor.newInstance();
        } catch (InstantiationException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(TestConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        origOut.println("-- tcfp - return new cfp");
        return cfp;
    }
}
