/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import sun.security.krb5.Config;

/**
 *
 * @author andre
 */
public class MailFetcher2 {
    private HashMap<Integer, ImapAcc> imapAccs
            = ConfigFileParser.getInstance().getImapAccs();
    
    //Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE, null, ex);
    
    public MailFetcher2() {
        
    }
    
    public ArrayList<PrintItem> getAllPrintableItems(){
        ArrayList<PrintItem> ret = new ArrayList<PrintItem>();
        
        Set<Integer> keys = imapAccs.keySet();
        for(Integer key: keys){
            /*
            if(key>1){
                continue;
            }
            */
             
            System.out.println("----------------------------------------\n"
                             + "----------------------------------------");
            
            ImapAcc actAcc = (ImapAcc)imapAccs.get(key);
    
            Properties props = new Properties();
            //props.setProperty("mail.imap.starttls.enable", "true");
            //props.put("mail.imap.starttls.enable", "true");
        
            /*
            // Use SSL
            prop.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            prop.setProperty("mail.imap.socketFactory.fallback", "false");

            // Use port 143
            prop.setProperty("mail.imap.port", "143");
            prop.setProperty("mail.imap.socketFactory.port", "143");
            */
            
            
            Session session = Session.getDefaultInstance(props, null);
            session.setDebug(false);
            Store store = null;
            try{
                store = session.getStore(actAcc.javaMailStoreName);
            } catch(NoSuchProviderException ex){
                Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            try{
                System.out.println("try to connect to " + actAcc.username + "@" + actAcc.url);
                store.connect(actAcc.url, actAcc.username, actAcc.password);
                
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
                    Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE, 
                            "Can't find folders in account: " +
                            actAcc.username + "@" + actAcc.url);
                }
                
                store.close();
            } catch(MessagingException ex){
                Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE, null, ex);
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
                                        Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE,
                                            getMessageInformation(actMsg) + "\n" +
                                            "BodyPart: " + i  + " - a problem during decoding occure.");
                                    } else {
                                        //decoding ok
                                        //go on prozessing, maybe safe to disk and put a fileitem in a list
                                        PrintItem pi = new PrintItem(fromAddr);
                                    }
                                } else {
                                    Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE,
                                        getMessageInformation(actMsg) + "\n" +
                                        "BodyPart: " + i  + " - can't be decoded.");
                                }
                            }
                        }
                    } else {
                        Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE,
                             getMessageInformation(actMsg) + " - email data can't be parsed");
                    }
                } catch (IOException ex) {
                    Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE,
                            getMessageInformation(actMsg) + " - email data can't be retrieved");
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
            Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE, null, ex);
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
        } catch (IOException ex){
            Logger.getLogger(MailFetcher2.class.getName()).log(Level.SEVERE, null, ex);
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
        }
        return ret;
    }
}
