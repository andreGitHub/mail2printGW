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

import com.sun.mail.util.BASE64DecoderStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.MimeType;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;
import mail2printgw.Certificate.CertificateCheck;

/**
 *
 * @author andre
 */
public class MailFetcher2 {
    private HashMap<Integer, ImapAcc> imapAccs
            = ConfigFileParser.getInstance().getImapAccs();
    private CertificateCheck cc = new CertificateCheck();
    
    public MailFetcher2() {
        
    }
    
    public ArrayList<PrintItem> getAllPrintableItems(){
        ArrayList<PrintItem> ret = new ArrayList<PrintItem>();
        
        Set<Integer> keys = imapAccs.keySet();
        for(Integer key: keys){
            System.out.println("----------------------------------------\n"
                             + "----------------------------------------");
            
            ImapAcc actAcc = (ImapAcc)imapAccs.get(key);
    
            //set mail properties
            Properties props = System.getProperties();
            props.put("mail.debug", "true");
            props.put("mail.host", actAcc.url);
            props.put("mail.user", actAcc.username);
            //props.put("mail.transport.protocol", "smtp");
            props.put("mail.store.protocol", actAcc.protocol);
            
            //Ports
            props.put("mail.protocol.port", actAcc.port);
            //props.put("mail.imap.port", actAcc.port);
            //props.put("mail.imap.socketFactory.port", actAcc.port);
            
            if(actAcc.protocol.equals("imap")){
                props.put("mail.imap.starttls.enable", true);
            }
            
            //props.put("mail.debug", "false");
            //props.put("mail.debug", "false");
            //props.put("mail.debug", "false");
            //props.put("mail.debug", "false");
            
            Session session = Session.getDefaultInstance(props, null);
            //session.setDebug(false);
            Store store = null;
            try{
                //store = session.getStore(actAcc.javaMailStoreName);
                store = session.getStore();
                //System.out.println("DEBUG Store: " + store);
            } catch(NoSuchProviderException ex){
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(1);
            }
            
            if(store == null){
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                        "unable to create a message store. the cause is a problem during retrieving the store from the session.");
                System.exit(1);
            }
            
            
            try{
                //System.out.println("try to connect to " + actAcc.username + "@"
                //        + actAcc.url + ":" + actAcc.port);
                store.connect(actAcc.url, actAcc.port, actAcc.username, actAcc.password);
            } catch(MessagingException ex){
                
                
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                        "May be there is a problem with certificates for the host.", ex);
                System.exit(1);
            }
            
            if(!store.isConnected()){
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                        "unable to connect to mail-server \"" + actAcc.url + "\".");
                System.exit(1);
            }
            
            try{
                //System.out.println("getDefaultFolder()");
                Folder f = store.getDefaultFolder();
                if(f != null && f.exists()){
                    Folder[] flds = f.list();
                    for(int i = 0; i<flds.length; i++){
                        if(flds[i] != null && flds[i].exists()){
                            ret.addAll(method1(flds[i]));
                        }
                    }
                } else {
                    Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, 
                            "Can't find folders in account: " +
                            actAcc.username + "@" + actAcc.url);
                    System.exit(1);
                }
            } catch(MessagingException ex){
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                        "problem during retrieving mails.", ex);
                System.exit(1);
            }
            
            try{
                store.close();
            } catch(MessagingException ex){
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                        "problem during logout.", ex);
                System.exit(1);
            }
        }
        return ret;
    }
    
    ArrayList<PrintItem> method1(Folder folder){
        ArrayList<PrintItem> ret = new ArrayList<PrintItem>();
        try{
            if(!folder.isOpen()){
                folder.open(Folder.READ_WRITE);
            }
            
            //System.out.println("actFolder open? " + folder.isOpen());
            //System.out.println("name of actFolder: -" + folder.getFullName() + "-");
            
            //System.out.println("number of messages: " + folder.getMessageCount());
            while(0 != folder.getMessageCount()){
                Message actMsg = folder.getMessage(1);
                
                
                //get from address
                Address[] from = actMsg.getFrom();
                ArrayList<String> fromAddr = new ArrayList<String>();
                if(from != null){
                    for(int i = 0; i<from.length; i++){
                        if(from[i] != null && !from[i].toString().equals("")){
                            fromAddr.add(from[i].toString());
                            System.out.println("from: " + from[i].toString());
                        }
                    }
                }
                
                if(fromAddr.isEmpty()){
                    fromAddr = null;
                }
                
                
                
                
                
                
                //get printable Attachments
                try{
                    if(actMsg.getContent() instanceof MimeMultipart){
                        MimeMultipart mm = (MimeMultipart)actMsg.getContent();
                        for(int i = 0; i<mm.getCount(); i++){
                            BodyPart bp = mm.getBodyPart(i);
                            if(checkPrintable(bp)){
                                printBodyPartInformation(bp);
                                System.out.println(bp.getContent());
                                if(bp.getContent() instanceof BASE64DecoderStream){
                                    BASE64DecoderStream b64ds = (BASE64DecoderStream)bp.getContent();
                                    int noAvail = b64ds.available();
                                    //i need to check a maximal filesize
                                    byte[] printableData = new byte[noAvail];
                                    b64ds.read(printableData, 0, noAvail);
                                    if(b64ds.available() != 0){
                                        Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                                            getMessageInformation(actMsg) + "\n" +
                                            "BodyPart: " + i  + " - a problem during decoding occure.");
                                        System.exit(1);
                                    } else {
                                        //decoding ok
                                        //go on prozessing, maybe safe to disk and put a fileitem in a list
                                        PrintItem pi = new PrintItem(fromAddr);
                                    }
                                } else {
                                    Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                                        getMessageInformation(actMsg) + "\n" +
                                        "BodyPart: " + i  + " - can't be decoded.");
                                    System.exit(1);
                                }
                            }
                        }
                    } else {
                        Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                             getMessageInformation(actMsg) + " - email data can't be parsed");
                        System.exit(1);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE,
                            getMessageInformation(actMsg) + " - email data can't be retrieved");
                    System.exit(1);
                }
                
                /*
                if(actMsg instanceof MimeMessage){
                    MimeMessage actMMsg = (MimeMessage) actMsg;
                    actMMsg.
                }
                */
                
                
                actMsg.setFlag(Flags.Flag.DELETED, true);
                folder.expunge();
            }
            
            
            Folder[] subFolders = folder.list();
            //System.out.println("number of subFolders: " + subFolders.length);
            for(int i=0; i<subFolders.length; i++){
                if(subFolders[i].exists()){
                    ret.addAll(method1(subFolders[i]));
                }
            }
        } catch(MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        return ret;
    }
    
    /**
     * Helperfunction that do, what the name suggests.
     * 
     * @param actFolder folder to start from
     */
    private void printFolderNameAndFolderNamesOfAllParents(Folder actFolder){
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
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }
    
    /**
     * Help function that print information about a bodypart.
     * 
     * @param bp 
     */
    private void printBodyPartInformation(BodyPart bp){
        try {
            System.out.println("contentType: -" + bp.getContentType() + "-");
            if(checkPrintable(bp)){
                System.out.println("printable");
            } else {
                System.out.println("not printable");
            }
                            
            System.out.println("description: -" + bp.getDescription() + "-");
            System.out.println("size:        -" + bp.getSize() + "-");
            System.out.println("content:\n" + bp.getContent());
        } catch (MessagingException ex) {
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        } catch (IOException ex){
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }
    
    
    /**
     * Helperfunction that check bodypart if it has a printable mimetype.
     * 
     * @param bp BodyPart to check.
     * @return true if it is printable - else false
     */
    private boolean checkPrintable(BodyPart bp){
        boolean printable = false;
        for(MimeType actMime : ConfigFileParser.getInstance().getPrintableMimes()){
            //System.out.println("mime: " + actMime.toString());
            try {
                if(bp.isMimeType(actMime.toString())){
                    printable = true;
                }
            } catch (MessagingException ex) {
                Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(1);
            }
        }
        return printable;
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
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        return ret;
    }
}
