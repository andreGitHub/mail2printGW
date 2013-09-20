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

import com.sun.mail.smtp.SMTPSenderFailedException;
import com.sun.mail.smtp.SMTPTransport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import mail2printgw.ConfigFileParser;
import mail2printgw.MailAcc;
import mail2printgw.MailFetcher2;
import mail2printgw.PrintManager;

/**
 *
 * @author eddi
 */
public class TestToolbox {
    public static MailAcc getMailAcc_myImap() {
        MailAcc ma = new MailAcc();
        ma.url = "imap.mail.yahoo.com";
        ma.port = 993;
        ma.importCert = false;
        ma.protocol = "imaps";
        ma.username = "myImap@yahoo.de";
        ma.password = TestToolbox.readPW(new File("myImap.txt"));
        ma.printer = "Cups-PDF";
        ma.useSTARTTLS = false;
        return ma;
    }
    
    public static MailAcc getMailAcc_aschuetze() {
        MailAcc ma = new MailAcc();
        ma.url = "mail.net.t-labs.tu-berlin.de";
        ma.port = 143;
        ma.importCert = false;
        ma.protocol = "imap";
        ma.username = "aschuetze";
        ma.password = TestToolbox.readPW(new File("ASchuetzePW.txt"));
        ma.printer = "Cups-PDF";
        ma.useSTARTTLS = true;
        return ma;
    }
    
    public static void clearAcc(MailAcc ma) {
        MailFetcher2 mf2 = new MailFetcher2();
        mf2.deleteMailsInAcc(ma);
    }
    
    public static String readPW(File f){
        if(f == null || !f.exists()){
            return "";
        }
        //System.out.println("-- tdm - path: " + f.getAbsolutePath());
        
        BufferedReader br = null;
        try {
            br = new BufferedReader(
                 new InputStreamReader(
                 new FileInputStream(f)));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TestToolbox.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        
        String ret = null;
        try {
            ret = br.readLine();
        } catch (IOException ex) {
            Logger.getLogger(TestToolbox.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    public static int getNumberOfMessages(MailAcc acc) {
        MailFetcher2 mf2 = new MailFetcher2();
        return mf2.getNumberOfMailsInMailAccount(acc);
    }
    
    public static void sendMessageOverWebDe(String to, ArrayList<String> filesToSend){
        MailFetcher2.clearAllJavaMailProperties();
        
        if(to == null || to.length() == 0){
            return;
        }
        
        Properties properties = System.getProperties();
        //properties.put("mail.debug", "true");
        
        // 587 - useSTARTTLS
        String username = "account.2011";
        String password = readPW(new File("account.2011.txt"));
        String smtpServer = "smtp.web.de";
        int port = 587;
        properties.put("mail.smtp.host", smtpServer);
        properties.remove("mail.smtp.socketFactory.class");
        properties.remove("mail.smtp.socketFactory.fallback");
        properties.remove("mail.smtp.socketFactory.port");
        properties.put("mail.smtp.port", "" + port);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.user", username);
        properties.put("mail.password", readPW(new File("account.2011.txt")));

        
        properties.put("mail.transport.protocol", "smtp");
        
        
        sendMail(properties, filesToSend, username, password, username + "@web.de",
                to, smtpServer, port, "smtp");
    }
    
    public static void sendMessageOverYahooDe(String to, ArrayList<String> filesToSend){
        MailFetcher2.clearAllJavaMailProperties();
        
        if(to == null || to.length() == 0){
            return;
        }
        
        Properties properties = System.getProperties();
        //properties.put("mail.debug", "true");
        
        // 465 use SSL/TLS
        String username = "myImapAcc@yahoo.de";
        String password = readPW(new File("myImapAcc.txt"));
        String smtpServer = "smtp.mail.yahoo.com";
        int port = 465;
        String protocol = "smtps";
        properties.put("mail.smtps.host", smtpServer);
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.fallback", "false");
        properties.put("mail.smtp.port", "" + port);
        properties.put("mail.smtp.socketFactory.port", "" + port);
        
        properties.put("mail.transport.protocol", protocol);
        //properties.put("mail.smtps.auth", "false");
        properties.put("mail.smtps.auth", "true");
        //properties.put("mail.smtps.quitwait"."false");
        //properties.put("mail.smtp.starttls.enable", "true");
        
        properties.put("mail.user", username);
        properties.put("mail.password", password);
        
        sendMail(properties, filesToSend, username, password, username, to,
                smtpServer, port, protocol);
    }
    
    private static void sendMail(Properties properties,
            ArrayList<String> filesToSend, String username, String password,
            String from, String to, String smtpServer, int port, String protocol) {
        Session session = Session.getDefaultInstance(properties, null);
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("hey whats up?");
            
            //printable attachment
            BodyPart bp1 = new MimeBodyPart();
            bp1.setText("some information");
            Multipart mp = new MimeMultipart();
            mp.addBodyPart(bp1);
            
            if(filesToSend != null) {
                for(String fileToSend : filesToSend) {
                    //System.out.println("add attachment: " + fileToSend);
                    String[] path = fileToSend.split("/");
                    String filename = path[path.length-1]; 
                    
                    BodyPart bp = new MimeBodyPart();
                    DataSource source = new FileDataSource(fileToSend);
                    bp.setDataHandler(new DataHandler(source));
                    bp.setFileName(filename);
                    mp.addBodyPart(bp);
                }
            }
            
            message.setContent(mp);
            message.saveChanges();
            SMTPTransport transp = (SMTPTransport)session.getTransport(protocol);
            transp.connect(smtpServer, port, username, password);
            transp.sendMessage(message, message.getAllRecipients());
            transp.close();
        } catch (SMTPSenderFailedException ex) {
            ex.printStackTrace();
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }
    }
    
    public static String getCorrectMailAddress(String user, String url) {
        //System.out.println("Parms: " + user + " " + url);
        
        if(url.equals("imap.mail.yahoo.com")) {
            return user;
        }
        if(url.equals("mail.net.t-labs.tu-berlin.de")) {
            return user + "@net.t-labs.tu-berlin.de";
        }
        
        return user;
    }
    
    public static void waitTillXMessagesFoundInAccount(MailAcc ma, int x) {
        while(TestToolbox.getNumberOfMessages(ma) < x) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestDifferentMailAccs.class.getName())
                        .log(Level.SEVERE, "unable to wait", ex);
            }
        }
    }
    
    protected static void createConfigFileParserWithCupsPdfPrinter() {
        Constructor<ConfigFileParser> constructor = null;
        Field printCmd = null;
        Field instance = null;
        
        try {
            constructor = ConfigFileParser.class.getDeclaredConstructor();
            printCmd = ConfigFileParser.class.getDeclaredField("printCmd");
            instance = ConfigFileParser.class.getDeclaredField("instance");
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        constructor.setAccessible(true);
        printCmd.setAccessible(true);
        instance.setAccessible(true);
        ConfigFileParser cfp = null;
        try {
            cfp = constructor.newInstance();
        } catch (InstantiationException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            printCmd.set(cfp, "lpr -P$printer $inputPS");
            instance.set(cfp, cfp);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //origOut.println("-- tcfp - return new cfp");
    }
    
    protected static void createPrintManagerWithDebugModeOn() {
        Constructor<PrintManager> constructor = null;
        Field debugMode = null;
        Field instance = null;
        
        try {
            constructor = PrintManager.class.getDeclaredConstructor();
            debugMode = PrintManager.class.getDeclaredField("debugMode");
            instance = PrintManager.class.getDeclaredField("instance");
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        constructor.setAccessible(true);
        debugMode.setAccessible(true);
        instance.setAccessible(true);
        PrintManager pm = null;
        try {
            pm = constructor.newInstance();
        } catch (InstantiationException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            debugMode.set(pm, true);
            instance.set(pm, pm);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //origOut.println("-- tcfp - return new cfp");
    }
}
