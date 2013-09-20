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

package mail2printgw;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.activation.MimeType;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;
import mail2printgw.Certificate.CertificateCheck;

/**
 * This class build up the main part of the mail2printGW. Its main function is
 * to manage different mail accounts, fetch mails and return printable Items.
 * Printable Items are Objects containing the Document to print and a printer,
 * which should be used. One Printable Item represent one Document, which should
 * be printed.
 * 
 * @author andre
 */
public class MailFetcher2 {
    private CertificateCheck cc = new CertificateCheck();
    
    public static void clearAllJavaMailProperties() {
        Properties props = System.getProperties();
        
        props.remove("mail.debug");
        props.remove("mail.host");
        
        props.remove("mail.imap.port");
        props.remove("mail.imap.socketFactory.port");
        props.remove("mail.imap.starttls.enable");
        
        props.remove("mail.password");
        props.remove("mail.protocol.port");
        
        props.remove("mail.smtp.auth");
        props.remove("mail.smtp.host");
        props.remove("mail.smtp.port");
        props.remove("mail.smtp.socketFactory.class");
        props.remove("mail.smtp.socketFactory.fallback");
        props.remove("mail.smtp.socketFactory.port");
        props.remove("mail.smtp.starttls.enable");
        props.remove("mail.smtps.auth");
        props.remove("mail.smtps.host");
        props.remove("mail.smtps.quitwait");
        props.remove("mail.store.protocol");
        
        props.remove("mail.transport.protocol");
        
        props.remove("mail.user");
    }
    
    public MailFetcher2() {
        
    }
    
    /**
     * This method set all System-properties, which are used by JavaMail - API
     * to connect to the mail server. 
     * 
     * @param actAcc ImapAcc contain information, which are needed to set the
     *                  right properies.
     */
    private void setMailProperties(MailAcc actAcc){//set mail properties
        clearAllJavaMailProperties();
        
        Properties props = System.getProperties();
        //props.put("mail.debug", "true");
        props.put("mail.host", actAcc.url);
        props.put("mail.user", actAcc.username);
        //props.put("mail.transport.protocol", "smtp");
        props.put("mail.store.protocol", actAcc.protocol);
            
        //Ports
        props.put("mail.protocol.port", actAcc.port);
        //props.put("mail.imap.port", actAcc.port);
        //props.put("mail.imap.socketFactory.port", actAcc.port);
            
        if(actAcc.protocol.equals("imap")){
            if(actAcc.useSTARTTLS){
                props.put("mail.imap.starttls.enable", true);
            } else {
                props.put("mail.imap.starttls.enable", false);
            }
        }
        
        //props.put("mail.debug", "false");
        //props.put("mail.debug", "false");
        //props.put("mail.debug", "false");
        //props.put("mail.debug", "false");
    }
    
    /**
     * This method determine if a certificate is needed and determine if the
     * certificate is already there. If the certificate have to be imported and
     * if the user specify in the config that the certificate should be
     * imported, the method try to import the certificate.
     * 
     * @param actAcc    account, which contain information used to
     *                  check/retrieve certificates.
     * @return  true    if the connection to the server can be established
     *                  (with or without certificate)
     *          false   otherwise
     */
    private boolean handleCertificateIssues(MailAcc actAcc){
        //check certificates
        if((actAcc.protocol.equals("pop3s") ||
            actAcc.protocol.equals("imaps") ||
            actAcc.protocol.equals("imap") && actAcc.useSTARTTLS) &&
                !cc.hasValidCertificate(actAcc.url, actAcc.port, actAcc.useSTARTTLS)){
        
            //need a certificate to create connection but the certificate is not
            //there
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.INFO,
                "no certificate for server " + actAcc.url + ":" + actAcc.port);
            
            if(actAcc.importCert){
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.INFO,
                    "try to import certificate");
                
                if(!cc.importCertificate(actAcc.url, actAcc.port, actAcc.useSTARTTLS)){
                    Logger.getLogger(MailFetcher2.class.getName()).log(Level.INFO,
                        "unable to import certificate.");
                    return false;
                }
                
                return true;
            } else {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.INFO,
                    "certificate-import not enabled by user.");
                
                return false;
            }
        } else {
            //no certificate needed
            return true;
        }
    }
    
    /**
     * This method take information of the given account-object and create a
     * store, which can be used to retrieve e-mails.
     * 
     * @param actAcc    account, which contain information to create and connect
     *                  a mail store.
     * @return  true    if a store can be created and connected to the
     *                  mail-Account specified by the parameter actAcc.
     *          false   otherwise
     */
    private boolean getConnectedStore(MailAcc actAcc){
        Session session = Session.getDefaultInstance(System.getProperties(), null);
        //session.setDebug(false);
        actAcc.mailStore = null;
        try{
            //store = session.getStore(actAcc.javaMailStoreName);
            actAcc.mailStore = session.getStore();
            //System.out.println("DEBUG Store: " + store);
        } catch(NoSuchProviderException ex){
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                "Specified provider is not suported by Javamail.\njavamail." +
                    "providers: " + System.getProperty("javamail.providers") +
                    "\njavamail.default.providers: " +
                    System.getProperty("javamail.default.providers"), ex);
            
            return false;
        }
            
        if(actAcc.mailStore == null){
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                "unable to create a message store. the cause is a problem "
                    + "during retrieving the store from the session.");
            
            return false;
        }
        
        try{
            //System.out.println("try to connect to " + actAcc.username + "@"
            //        + actAcc.url + ":" + actAcc.port);
            actAcc.mailStore.connect(actAcc.url, actAcc.port, actAcc.username, actAcc.password);
        } catch(MessagingException ex){
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                "Unable to connect Mailstore of " + actAcc.protocol + "-acc " +
                actAcc.username + "@" + actAcc.url + ":" + actAcc.port +
                ". May be there is a problem with certificates for the host.", ex);
            
            return false;
        }
            
        if(!actAcc.mailStore.isConnected()){
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                "unable to connect to mail-server \"" + actAcc.url + "\".");
            
            return false;
        }
        return true;
    }
    
    /**
     * Try to close MailStore given by parameter.
     * 
     * @param store store, which should be closed.
     */
    private void closeMailStore(Store store){
        if(store != null){
            try{
                store.close();
            } catch(MessagingException ex){
                Logger.getLogger(MailFetcher2.class.getName())
                    .log(Level.SEVERE, "problem during logout.", ex);
            }
        }
    }
    
    /**
     * This method work like printAllAttachmentsOfAllMailsOfAllAccs(), but
     * expects only a single account.
     * 
     * @param ma    mail-account to connect to
     */
    public void printAllAttachmentsOfAllMailsOfAllAccs(MailAcc ma) {
        if(null == ma) {
            return;
        }
        HashMap<Integer, MailAcc> hm = new HashMap<Integer, MailAcc>();
        hm.put(0, ma);
        printAllAttachmentsOfAllMailsOfAllAccs(hm);
    }
    
    /**
     * Iterate over all mail Accounts, fetch all printable attachments of all
     * mails inside all accounts and return them in one list.
     * 
     * @return a list of printable Items.
     */
    public void printAllAttachmentsOfAllMailsOfAllAccs(HashMap<Integer, MailAcc> imapAccs){
        if(null == imapAccs) {
            return;
        }
        
        Set<Integer> keys = imapAccs.keySet();
        
        for(Integer key: keys){
            //System.out.println("mf - -----------------------------------\n"
            //                 + "----------------------------------------");
            
            MailAcc actAcc = (MailAcc)imapAccs.get(key);
            
            setMailProperties(actAcc);
            
            if(!handleCertificateIssues(actAcc)) {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                    "skip mail account: " + actAcc.username + "@" + actAcc.url);
                continue;
            }
            
            printAllPrinatableItemsOfAcc(actAcc);
            closeMailStore(actAcc.mailStore);
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    //                                                                        //
    //   Here the methods to browse a mailaccount and to fetch all mails in   //
    //   the account starts                                                   //
    //                                                                        //
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /**
     * This method searchers the mailAccount given by the parameter actAcc for
     * mails, put each printable attachment with additional information in
     * Objects of the Type PrintItem and return them. This method return all
     * printable Attachments of all mails found in the mail-account in one
     * ArrayList.
     * 
     * @param actAcc    account with a connected store. This object specify a
     *                  mail account.
     * @return          A list with all printable elements of all mails found in
     *                  the account
     */
    private void printAllPrinatableItemsOfAcc(MailAcc actAcc){
        if(!getConnectedStore(actAcc)){
            closeMailStore(actAcc.mailStore);
            return;
        }
        
        try{
            //System.out.println("getDefaultFolder()");
            Folder f = actAcc.mailStore.getDefaultFolder();
            if(f != null && f.exists()){
                Folder[] flds = f.list();
                for(int i = 0; i<flds.length; i++){
                    if(flds[i] != null && flds[i].exists()){
                        printAllPrintItemsOfFolder(flds[i], actAcc.printer);
                    }
                }
            } else {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "Can't find folders in account: " + actAcc.username + "@" +
                    actAcc.url);
                
                return;
            }
        } catch(MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                "problem during retrieving mails.", ex);
        }
    }
    
    /**
     * This method get a message (an e-mail) and retrieve all e-mail-addresses,
     * which are specified as sender of this email. (the e-mail standard allow
     * multiple senders for one mail)
     * 
     * @param msg   message, where the senders should be retrieved.
     * @return      a list of e-mail addresses, which send the given message.
     */
    private ArrayList<String> getFromAddresses(Message msg){
        //get from address
        ArrayList<String> fromAddr = new ArrayList<String>();
        Address[] from = null;
        try {
            from = msg.getFrom();
        } catch (MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                    "can't retrieve the sender of a message", ex);
        }
        
        if(from != null){
            for(int i = 0; i<from.length; i++){
                if(from[i] != null && !from[i].toString().equals("")){
                    fromAddr.add(from[i].toString());
                    //System.out.println("mf - from: " + from[i].toString());
                }
            }
        } else {
            return null;
        }
        
        if(fromAddr.isEmpty()){
            return null;
        }
        return fromAddr;
    }
    
    /**
     * This method get an InputStream which reads one mail attachment and
     * provide it as input for the program.
     * 
     * @param b64ds Inputstream of one MailAttachment.
     * @return an PDDocument which is an printable PDFDocument.
     */
    /*
    private PDDocument getAttachmentAsPDF(BASE64DecoderStream b64ds){
        int noAvail;
        PDDocument doc = null;
        try {
            noAvail = b64ds.available();
            if(noAvail > 0){
                doc = PDDocument.load(b64ds);
                if(b64ds.available() != 0){
                    Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                        " - a problem during decoding occure.");
                }
            } else {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                    "mf - attachment of 0-length.");
            }
        } catch (IOException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "something went wrong during decoding of mail attachment "
                    + "or creation of pdf.", ex);
        }
        return doc;
    }
    */
    
    /**
     * This method get an InputStream which reads one mail attachment and
     * provide it as input for the program.
     * 
     * @param is Inputstream of one MailAttachment.
     * @return an PDDocument which is an printable PDFDocument.
     */
/*
    private void printAttachment(InputStream is, ArrayList<String> from){
        int noAvail;
        PDDocument doc = null;
        try {
            noAvail = is.available();
            if(noAvail > 0){
                System.out.println("-- mf - try to create document");
                doc = PDDocument.load(is);
                System.out.println("-- mf - document created");
                
                if(is.available() != 0){
                    Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                        " - a problem during decoding occure.");
                }
                
                
            } else {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                    "mf - attachment of 0-length.");
            }
        } catch (IOException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "something went wrong during decoding of mail attachment "
                    + "or creation of pdf.", ex);
        }
    }
  */
    
    /**
     * This method take the name of the printer and the path to the folder to
     * save attachments and create a file where the actual attachment should
     * be saved in.
     * 
     * @param printer   Name of the printer.
     * @return          File - Directory where attachment to print should be
     *                      saved.
     */
    private File getFileToSaveAttachment(String printer, String fileName) {
        //create folders/files for attachment
        //folder specified in config-file
        String path = ConfigFileParser.getInstance().getTmpDirForPDFs();
        File saveTo = new File(path);
        if(!saveTo.exists()) {
            if(!saveTo.mkdirs()) {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "unable to read attachments", new Exception());
            }
        }
        //folder of printer
        path = path + File.separator + printer;
        saveTo = new File(path);
        if(!saveTo.exists()) {
            if(!saveTo.mkdirs()) {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "unable to read attachments", new Exception());
            }
        }
        //folder of print - job
        String[] files = saveTo.list();
        /*
        for(int i = 0; i<files.length; i++) {
            System.out.println("-- mf2 - file: " + files[i]);
        }
        */
        if(files.length > 0) {
            int[] intNames = new int[files.length];
            for(int i = 0; i<files.length; i++) {
                String act = files[i];
                int tmp = Integer.MAX_VALUE;
                try {
                    tmp = Integer.parseInt(act);
                    intNames[i] = tmp;
                } catch(NumberFormatException ex) {
                    intNames[i] = Integer.MAX_VALUE;
                }
            }
            Arrays.sort(intNames);
            int firstFree = -1;
            int count = 0;
            for(count = 0; count<intNames.length; count++) {
                //System.out.println("-- mf2 - intNames: " + intNames[i]);
                if(intNames[count] != count){
                    firstFree = count;
                }
            }
            if(firstFree == -1){
                firstFree = count;
            }
            path = path + File.separator + firstFree;
            saveTo = new File(path);
            if(!saveTo.mkdirs()) {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "unable to read attachments", new Exception());
            }
        } else {
            path = path + File.separator + "0";
            saveTo = new File(path);
            saveTo.mkdir();
        }
        
        //file to save attachemnt
        path = path + File.separator + fileName;
        saveTo = new File(path);
        try {
            if(!saveTo.createNewFile()) {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                    "unable to read attachments", new Exception());
            }
        } catch (IOException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                    "unable to read attachments", ex);
        }
        //System.out.println("-- mf2 - file created: " + path);
        return saveTo;
    }
    
    
    /**
     * This method get some parameters and create a printitem with all needed information.
     * 
     * @param printer       name of printer which should be used
     * @param bp            one attachment of the mail
     * @return              a print-item
     */
    private PrintItem saveAttachmentToFile(String printer, BodyPart bp){
        //read
        File f = null;
        //System.out.println("-- mf2 - read: " + buf[0]);
        String fileName = null;
        try {
            fileName = bp.getFileName();
            int size = bp.getSize();
            byte[] buf = new byte[size];
            //System.out.println("-- mf2 - " + size);
            DataHandler dh = bp.getDataHandler();
            int length2 = dh.getInputStream().read(buf);
            
            /*
            System.out.println("bytes read = " + length2);
            //dethermine real attachment length
            int length = 0;
            for(length = buf.length-1; length>0; length--){
                if(buf[length] != 0) {
                    System.out.println("length: " + length);
                    break;
                }
            }
            */
            
            f = getFileToSaveAttachment(printer, fileName);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buf,0, length2);
            fos.flush();
            fos.close();
            
            
            /*
            BASE64Decoder decoder = new BASE64Decoder();
            decoder.decodeBuffer(dh.getInputStream(), fos);
            fos.flush();
            fos.close();
            */
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "unable to read attachments", ex);
        }
        //System.out.println("-- mf2 - read: " + buf[0] + " " + buf2[0]);
        
        
        PrintItem pi = new PrintItem();
        pi.setFilePath(f.getAbsolutePath());
        pi.setPrinter(printer);
        
        return pi;
    }
    
    /**
     * This method process one BodyPart of a mail which represents one mail
     * attachment. The try to read the attachment, convert the attachment in a
     * pdf, create a PrintItem and save sender of the mail and the document
     * inside the PrintItem.
     * 
     * @param bp    Attachment, which should be processed.
     * @return      A PrintItem containing the document to print or null, if
     *              there is a failure while reading the attachment or the
     *              attachment has a mime-type, which should not be printed
     */
    private void printAttachmentOfBodyPart(BodyPart bp, ArrayList<String> from, String printer){
        //System.out.println("-- mf -         next attachment");
        try {
            MimeType mime = getPrintableMime(bp);
            if(checkPrintable(bp)){
                //System.out.println(bp.getContent());
                if(Part.ATTACHMENT.equalsIgnoreCase(bp.getDisposition()) ||
                        bp.getDisposition() == null) {
                    PrintItem pi = saveAttachmentToFile(printer, bp);
                    pi.setMime(mime);
                    pi.setFrom(from);
                    PrintManager.getInstance().printItem(pi);
                } else {
                    System.out.println("is not an attachment");
                }
            } else {
                //String mime = bp.getContentType();
                //System.out.println("-- mf - mimetype not printable: " + mime);
            }
        } catch (MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "Can't read content from BodyPart/Can't read attachment.", ex);
        }
    }
    
    /**
     * This method extract all printable Documents of the given mail and return
     * them.
     * 
     * @param actMsg    Message, which has at least one attachment.
     * @return          ArrayList with at least one Printable Item inside or
     *                  null, if there are no printable document inside the
     *                  mail.
     */
    private void printAllPrintItemsOfMailWithAttachment(Message actMsg, String printer) {
        try {
            if(actMsg.getContent() instanceof MimeMultipart) {
                MimeMultipart mm = (MimeMultipart)actMsg.getContent();
                ArrayList<String> from = getFromAddresses(actMsg);
                
                for(int i = 0; i<mm.getCount(); i++) {
                    //iterate over all attachments
                    printAttachmentOfBodyPart(mm.getBodyPart(i), from, printer);
                }
            } else {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                    getMessageInformation(actMsg) + " - email data can't be parsed");
            }
        } catch (IOException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * This method process one mail. It expect the mail as parameter and analyze
     * if the the mail has attachments. If the mail has attachments, the mail is
     * processed and all printable elements are retrieved.
     * 
     * @param actMsg    mail to process.
     * @return          ArrayList containing all printable documents of the
     *                  mail.
     */
    private void printAllPrintItemsOfMail(Message actMsg, String printer) {
        //get printable Attachments
        try{
            if(actMsg.isMimeType("text/plain")){
                //System.out.println("-- mf  - mail " + actMsg.getFileName() + " of "
                //        + "mimeType \"text/plain\". The mail only contain text "
                //        + "and should not be printed.");
            } else {
                //mail has another content typ. Till now I don't know what else
                //exist. I expact a message with printable attachments.
                printAllPrintItemsOfMailWithAttachment(actMsg, printer);
            }
        } catch (MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                getMessageInformation(actMsg) + " - email data can't be retrieved");
        }
    }
    
    /**
     * The method fetches all printable Documents of all attachments of all
     * mails insight the given folder. It browses the given folder recursively
     * to find all mails.
     * 
     * @param folder    The folder, which should be searched.
     * @return          A list of all printable elements in this folder.
     */
    private void printAllPrintItemsOfFolder(Folder folder, String printer){
        try{
            if(!folder.isOpen()){
                folder.open(Folder.READ_WRITE);
            }
            
            //System.out.println("actFolder open? " + folder.isOpen());
            //System.out.println("name of actFolder: -" + folder.getFullName() + "-");
            //System.out.println("number of messages: " + folder.getMessageCount());
            
            
            
            
            
            
            Message[] msgs = folder.getMessages();
            
            
            
            
            
            
            for(int i = 0; i < msgs.length; i++){
                //iterate over mails of folder
                
                Message actMsg = msgs[i];
                //ArrayList<String> from = getFromAddresses(actMsg);
                printAllPrintItemsOfMail(actMsg, printer);
                
                
                actMsg.setFlag(Flags.Flag.DELETED, true);
            }
            
            Folder[] subFolders = folder.list();
            //System.out.println("number of subFolders: " + subFolders.length);
            for(int i=0; i<subFolders.length; i++){
                if(subFolders[i].exists()){
                    printAllPrintItemsOfFolder(subFolders[i], printer);
                }
            }
            
            folder.expunge();
        } catch(MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "Error during folder/mail operation.", ex);
        }
    }
    
    /**
     * Helperfunction that return the mime type of the bodypart.
     * 
     * @param   bp BodyPart to check.
     * @return  string with mime-type or
     *          null if it is not a printable mime-type.
     */
    private MimeType getPrintableMime(BodyPart bp){
        if(null == bp) {
            return null;
        }
        
        ArrayList<MimeType> mimes = ConfigFileParser.getInstance().getPrintableMimes();
        if(mimes == null) {
            return null;
        }
        for(MimeType actMime : mimes){
            //System.out.println("mime: " + actMime.toString());
            try {
                if(bp.isMimeType(actMime.toString())){
                    //System.out.println("printable mime: " + actMime.toString() +
                    //        " in Bodypart: " + bp.getContentType());
                    return actMime;
                }
            } catch (MessagingException ex) {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                        "can't check, whether attachment is printable or not.", ex);
            }
        }
        
        /*
        try {
            System.out.println("printable mime: " + null + " in bodypart: " +
                bp.getContentType());
            
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                "Bodypart " + bp.getFileName() + " has an unknown mime-type: " +
                    bp.getContentType(),
                new Exception());
        } catch (MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                "can't check, whether attachment is printable or not.", ex);
        }
        */
        
        return null;
    }
    
    /**
     * Helperfunction that check bodypart if it has a printable mimetype.
     * 
     * @param bp BodyPart to check.
     * @return true if it is printable - else false
     */
    private boolean checkPrintable(BodyPart bp){
        boolean printable = false;
        ArrayList<MimeType> mimes = ConfigFileParser.getInstance().getPrintableMimes();
        if(mimes == null) {
            return false;
        }
        for(MimeType actMime : mimes){
            //System.out.println("mime: " + actMime.toString());
            try {
                if(bp.isMimeType(actMime.toString())){
                    printable = true;
                }
            } catch (MessagingException ex) {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                        "can't check, whether attachment is printable or not.", ex);
            }
        }
        return printable;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //                                                                        //
    //  debug output                                                          //
    //                                                                        //
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Helperfunction that do, what the name suggests.
     * 
     * @param actFolder folder to start from
     */
    private void showFolderNameAndFolderNamesOfAllParents(Folder actFolder){
        try{
            actFolder.open(Folder.READ_WRITE);
            System.out.println("foldername: \"" + actFolder.getFullName() + "\"");
            while(actFolder.getParent() != null &&
                    actFolder.getParent().exists() && 
                    actFolder.getParent().getFullName() != null &&
                    !actFolder.getParent().getFullName().equals("")){
                Folder alt = actFolder;
                actFolder = actFolder.getParent();
                alt.close(true);
                actFolder.open(Folder.READ_WRITE);
                System.out.println("foldername: \"" + actFolder.getFullName() + "\"");
            }
        } catch(MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "can't print foldernames.", ex);
        }
    }
    
    /**
     * Help function that print information about a bodypart.
     * 
     * @param bp 
     */
    private void showBodyPartInformation(BodyPart bp){
        try {
            System.out.println("-- mf - contentType: -" + bp.getContentType() + "-");
            if(checkPrintable(bp)){
                System.out.println("-- mf - printable");
            } else {
                System.out.println("-- mf - not printable");
            }
                            
            System.out.println("-- mf - description: -" + bp.getDescription() + "-");
            System.out.println("-- mf - size:        -" + bp.getSize() + "-");
            System.out.println("-- mf - content:\n-------" + bp.getContent());
            System.out.println("-- mf - inputstream:\n-------" + bp.getInputStream());
            System.out.println("-- mf - header:");
            Enumeration en = bp.getAllHeaders();
            while(en.hasMoreElements()){
                System.out.println("-- mf - elem:" + en.nextElement());
            }
        } catch (MessagingException | IOException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                    "can't print body part information.", ex);
        }
    }
    
    /**
     * Method to get debug output.
     * @param f 
     */
    private void debugMessageCount(Folder f) {
        try {
            Message[] msg = f.getMessages();
            for(int i = 0; i<msg.length; i++) {
                System.out.println("/////");
                System.out.println(getMessageInformation(msg[i]));
                System.out.println("/////");
            }
        } catch (MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Help method simply for debugging. This method get a Message and create a
     * Sting containing some information of this message.
     * 
     * @param actMsg Message to retrieve information from.
     * @return string with information.
     */
    private String getMessageInformation(Message actMsg){
        String ret = null;
        try {
            ret = actMsg.getFolder().getStore().getURLName().getUsername() + "@" +
                    actMsg.getFolder().getStore().getURLName().getHost() + ":\n" +
                    actMsg.getFolder().getName() + ":" + actMsg.getMessageNumber() + " " +
                    actMsg.getSubject();
        } catch (MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                    "can't retrieve message information.", ex);
        }
        return ret;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //                                                                        //
    //  delete all mails in folder                                            //
    //                                                                        //
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Method which delete all mails in the given acc without to print them.
     * 
     * @param actAcc    Acc to connect to.
     */
    public void deleteMailsInAcc(MailAcc actAcc) {
        setMailProperties(actAcc);
        
        if(!handleCertificateIssues(actAcc)) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.INFO,
                "skip mail account: " + actAcc.username + "@" + actAcc.url);
        }
        
        if(!getConnectedStore(actAcc)){
            closeMailStore(actAcc.mailStore);
            return;
        }
        
        try{
            //System.out.println("getDefaultFolder()");
            Folder f = actAcc.mailStore.getDefaultFolder();
            if(f != null && f.exists()){
                Folder[] flds = f.list();
                for(int i = 0; i<flds.length; i++){
                    if(flds[i] != null && flds[i].exists()){
                        deleteAllMailsInFolder(flds[i]);
                    }
                }
            } else {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "Can't find folders in account: " + actAcc.username + "@" +
                    actAcc.url);
            }
        } catch(MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                "problem during retrieving mails.", ex);
        }
        closeMailStore(actAcc.mailStore);
    }
    
    /**
     * Delete all mails in the given folder and all subfolders.
     * 
     * @param f     Folder, where mails should be delete.
     */
    private void deleteAllMailsInFolder(Folder folder) {
        try{
            if(!folder.isOpen()){
                folder.open(Folder.READ_WRITE);
            }
            
            Message[] msgs = folder.getMessages();
            
            for(int i = 0; i < msgs.length; i++){
                msgs[i].setFlag(Flags.Flag.DELETED, true);
            }
            
            Folder[] subFolders = folder.list();
            //System.out.println("number of subFolders: " + subFolders.length);
            for(int i=0; i<subFolders.length; i++){
                if(subFolders[i] != null && subFolders[i].exists()){
                    deleteAllMailsInFolder(subFolders[i]);
                }
            }
            
            folder.expunge();
        } catch(MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "Error during folder/mail operation.", ex);
        }
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    //                                                                        //
    //  number of mails                                                       //
    //                                                                        //
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * This method connect to the given account and count number of messages.
     * 
     * @param ma    account to connect to. 
     * @return      number of mails to account.
     */
    public int getNumberOfMailsInMailAccount(MailAcc actAcc){
        int ret = 0;
        setMailProperties(actAcc);
        
        if(!handleCertificateIssues(actAcc)) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.INFO,
                    "skip mail account: " + actAcc.username + "@" + actAcc.url);
            return 0;
        }
            
        ret = ret + getNumberOfMailsInConnectedAcc(actAcc);
        
        closeMailStore(actAcc.mailStore);
        return ret;
    }
    
    /**
     * This method connect to the given account and count number of messages.
     * The method expects that the right mail properties are already set.
     * 
     * @param ma    account to connect to. 
     * @return      number of mails to account.
     */
    private int getNumberOfMailsInConnectedAcc(MailAcc actAcc){
        int ret = 0;
        
        if(!getConnectedStore(actAcc)){
            closeMailStore(actAcc.mailStore);
            return ret;
        }
        
        try{
            //System.out.println("getDefaultFolder()");
            Folder f = actAcc.mailStore.getDefaultFolder();
            if(f != null && f.exists()){
                Folder[] flds = f.list();
                for(int i = 0; i<flds.length; i++){
                    if(flds[i] != null && flds[i].exists()){
                        ret = ret + getNumberOfMailsInFolder(flds[i], actAcc.protocol);
                    }
                }
            } else {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "Can't find folders in account: " + actAcc.username + "@" +
                    actAcc.url);
            }
        } catch(MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                "problem during retrieving mails.", ex);
        }
        return ret;
    }
    
    /**
     * This method count the number of mails in the given folder and all
     * subfolders recursivly.
     * 
     * @param f folder, where mails should be counted.
     * @return  number of mails in folder.
     */
    private int getNumberOfMailsInFolder(Folder folder, String protocol){
        int ret = 0;
        try{
            if(!folder.isOpen()){
                folder.open(Folder.READ_WRITE);
            }
            
            ret = ret + folder.getMessageCount();
            
            /*
            if(ret > 0) {
                debugMessageCount(folder);
            }
            */
            
            Folder[] subFolders = folder.list();
            //System.out.println("number of subFolders: " + subFolders.length);
            for(int i=0; i<subFolders.length; i++){
                if(subFolders[i].exists()){
                    ret = ret + getNumberOfMailsInFolder(subFolders[i], protocol);
                }
            }
        } catch(MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                    "Error during folder/mail operation.", ex);
        }
        return ret;
    }
}
