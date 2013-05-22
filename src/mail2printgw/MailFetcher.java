/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mail2printgw;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.net.imap.IMAPCommand;
import org.apache.commons.net.imap.IMAPSClient;

/**
 *
 * @author andre
 */
public class MailFetcher {
    private IMAPSClient myImapsClient = null;
    private HashMap<Integer, ImapAcc> imapAccs = ConfigFileParser.getInstance().getImapAccs();
    
    /**
     * Constructor initializes the the IMAPS-Client which connect to the server.
     */
    public MailFetcher(){
        myImapsClient = new IMAPSClient(IMAPSClient.DEFAULT_PROTOCOL, true);
    }
    
    /**
     * For this method "myImapsClient" should already connect to and logged into
     * and imaps server. In this method the whole mailaccount will be searched
     * for mails. If the mails have attachments, all of them will be returned. 
     * All mails will be deleted after this method run.
     * The whole task is divided in parts, which are processed by several methods.
     * This method handle the root element of the directory inside the mail
     * account. The method retrieve the name and status of every folder and call
     * the appropriate method to process the folder.
     * 
     * @return a list with all printable attachments.
     */
    private ArrayList<PrintItem> fetchAllMailAttachmentsFromMailbox(){
        ArrayList<PrintItem> ret = new ArrayList<PrintItem>();
        
        try {
            myImapsClient.doCommand(IMAPCommand.LIST, "\"\" %");
            String[] retLines = myImapsClient.getReplyStrings();
            //printOnScreen("List ", retLines);
            
            for(int i = 0;i<retLines.length; i++){
                //pat is a pattern, that matches a Folder given by the list command
                String pat = "^\\*\\sLIST\\s\\(\\\\[a-zA-Z]+\\)\\s\\\"\\/\\\"\\s\\D*";
                //System.out.println(pat);
                if(retLines[i].matches(pat)){
                    //System.out.println(retLines[i]);
                    //retrive state
                    String folderFileState = retLines[i]
                            .substring( retLines[i].lastIndexOf("(\\")+2,
                                        retLines[i].lastIndexOf(")".charAt(0)));
                    
                    //retrieve name
                    String folderFileName = retLines[i]
                            .substring(retLines[i].lastIndexOf("\"/\"")+4);
                    //folderFileName = folderFileName.replaceAll("\"", "");
                    //folderFileName = folderFileName.replaceAll("/", "");
                    //folderFileName = folderFileName.substring(1);
                    
                    //System.out.println("Name: -" + folderFileName + "- State: -" + folderFileState + "-");
                    
                    ret.addAll(fetchAllMailAttachmentsOfFolder(folderFileState, folderFileName));                    
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ret;
    }
    
    /**
     * This method should be called by the method "fetchAllMailAttachmentsFromMailbox()".
     * This method have the same preconditions like the mentioned method.
     * "myImapsClient" should already be connected and logged in. Task of the
     * method is to process (SELECT ... ) the file or directory with name "name"
     * take the right action. For example if the directory is a folder it select
     * this folder and searches for mails. The search for mails is handed over to
     * an further method.
     * 
     * @param state type (e.g. HasNoChildren or NoInferiors indicating that it
     *              is a mailbox/folder which can be selected and searched for
     *              mails)
     * @param name  name of the mailbox/folder to SELECT, ...
     * @return a ArrayList with all printable Items found in the mailbox
     */
    private ArrayList<PrintItem> fetchAllMailAttachmentsOfFolder(String state, String name){
        ArrayList<PrintItem> ret = new ArrayList<PrintItem>();
        if(state.equals("HasNoChildren") || state.equals("NoInferiors")){
            try{
                //liste alle mails des Ordners
                String args = name;
                myImapsClient.doCommand(IMAPCommand.SELECT, args);
                String[] retLines = myImapsClient.getReplyStrings();
                //printOnScreen("SELECT " + args, retLines);
                for(int i = 0; i<retLines.length; i++){
                    //find line where number of mails listed
                    if(retLines[i].matches("^\\*\\s\\d+\\sEXISTS$")){
                        //System.out.println(retLines[i]);
                        
                        int numMails = getNumberBetweenFirstTwoSpaces(retLines[i]);
                        //System.out.println("number of mails in folder " + name + ": -" + numMails + "-");
                        if(numMails > 0){
                            ret.addAll(fetchAllMailAttachmentsOfNonEmptyMailbox(name, numMails));
                            printOnScreen("CLOSE " + myImapsClient.close(),
                                    myImapsClient.getReplyStrings());
                            
                            return ret;
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE, "ERROR: unknown Directory-entry: " + state);
        }
        return ret;
    }
    
    /**
     * This method should be called by the method "fetchAllMailAttachmentsOfFolder()".
     * Preconditions for this method are the same like for the method mentioned 
     * above. Further the mailbox with name "name" have to be selected.
     * 
     * @param name name of the selected mailbox
     * @param num number of mails inside the mailbox
     * @return an ArrayList with all printable attachments
     */
    ArrayList<PrintItem> fetchAllMailAttachmentsOfNonEmptyMailbox(String name, int num){
        ArrayList<PrintItem> ret = new ArrayList<PrintItem>();
        try{
            boolean cmdOk = myImapsClient.fetch("1:*", "FLAGS");
            String[] cmdRet = myImapsClient.getReplyStrings();
            //printOnScreen("FETCH " + cmdRet, cmdRet);
            
            for(int i = 0; i<cmdRet.length; i++){
                int actMail = getNumberBetweenFirstTwoSpaces(cmdRet[i]);
                if(actMail>-1){
                    

                    ret.addAll(fetchAllMailAttachmentsOfAMail(actMail));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    
    /**
     * This method fetches a mail and process all attachments of the mail. The 
     * method has the same preconditions like the method
     * "fetchAllMailAttachmentsOfNonEmptyMailbox()" and should be called by this
     * method.
     * 
     * @param mailNo Number of mail in mail-account, that should be processed.
     * @return all printable attachments.
     */
    private ArrayList<PrintItem> fetchAllMailAttachmentsOfAMail(int mailNo){
        ArrayList<PrintItem> ret = new ArrayList<PrintItem>();
        
        try{
            //boolean cmdOk = myImapsClient.fetch("" + mailNo, "body[2]");
            boolean cmdOk = myImapsClient.fetch("" + mailNo, "full");
            printOnScreen("FETCH " + cmdOk, myImapsClient.getReplyStrings());
            
            cmdOk = myImapsClient.fetch("" + mailNo, "full");
            
            //hier muss der mailanhang verarbeitet werden
            
            
        
            //System.exit(0);
            
            //delete mail
            printOnScreen("FETCH flags " + myImapsClient.fetch("1:*", "FLAGS"), myImapsClient.getReplyStrings());
        
            printOnScreen("STORE delete " + myImapsClient.doCommand(IMAPCommand.STORE, mailNo + " +flags \\Deleted"), myImapsClient.getReplyStrings());
        
            printOnScreen("FETCH flags " + myImapsClient.fetch("1:*", "FLAGS"), myImapsClient.getReplyStrings());
        } catch(IOException ex){
            
        }
        return ret;
    }
    
    
    /**
     * This method use the information about imapAccs(from config-file) to
     * connect to a server and login. After calling a method to fetch all
     * Attachments, a logout is done and all found attachments are returned.
     * Before you call this method the variable myImapsClient have to be
     * proper initialized.
     * 
     * @param acc   account information to connect to.
     * @return      an ArrayList with all printable Items(mail attachments found in
     *              the account) 
     */
    private ArrayList<PrintItem> fetchMailsFromAcc(ImapAcc acc){
        //connect to imaps-Server
        ArrayList<PrintItem> ret = null;
        try {
            //establish connection to mailserver
            myImapsClient.connect(acc.url, acc.port);
            //System.out.println("connected");
            
            if(myImapsClient.isConnected()){
               if(myImapsClient.login(acc.username, acc.password)){
                   //System.out.println("successfully loged in to " + acc.url);
                   
                   printOnScreen("Login", myImapsClient.getReplyStrings());
                   
                   //printOnScreen("Capabilities " + myImapsClient.capability(), myImapsClient.getReplyStrings());
                   ret = fetchAllMailAttachmentsFromMailbox();
                   
                   
                   //printOnScreen("Logout " + myImapsClient.logout(), myImapsClient.getReplyStrings());
                   
                   //printOnScreen("CLOSE " + myImapsClient.close(), myImapsClient.getReplyStrings());
                   
                   myImapsClient.disconnect();
                   printOnScreen("Disconnect", myImapsClient.getReplyStrings());
               } else {
                   Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE,
                            "ERROR: unable to log into mail account \"" + acc.username + 
                            "\" on \"" + acc.url + "\"");
               }
            } else {
                Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE,
                        "can not establish connection to IMAP-Server " + acc.url);
            }
        } catch (SocketException ex) {
            Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MailFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    /**
     * Fetches printable items (printable mail attachments) from all
     * email-accounts listed in the config-file.
     * @return an ArrayList with all printable items
     */
    public ArrayList<PrintItem> fetchMails(){
        ArrayList<PrintItem> ret = new ArrayList<PrintItem>();
        for (ImapAcc acc : imapAccs.values()) {
            ret.addAll(fetchMailsFromAcc(acc));
        }
        return ret;
    }
    
    /**
     * Helper method that fetches an integer out of a string which look like this:
     * "<star><space><num><space><something>"
     * 
     * @param str string in the given format where a number is included
     * @return number of the string or -1 if no number can be extracted.
     */
    private int getNumberBetweenFirstTwoSpaces(String str){
        if(str.matches("^\\*\\s\\d+\\s\\D*")) {
            String strNum = str.substring(
                str.indexOf(" ".charAt(0))+1,
                str.indexOf(" ".charAt(0), str.indexOf(" ".charAt(0))+1)
                    );
            return Integer.parseInt(strNum);
        } else {
            return -1;
        }
        
    }
    
    /**
     * helper method for debugging purpose. This method print out (on console)
     * all given strings. It can be used to print out all strings returned by
     * the client after executing a imap - command.
     * 
     * @param heading heading, that should be printed
     * @param parm rows that should be printed
     */
    private void printOnScreen(String heading, String[] parm){
        String tmp = "--------------------------------------------------";
        StringBuilder sb = new StringBuilder();
        sb.append(tmp);
        sb.insert(3, " " + heading + " ");
        
        System.out.println(sb.subSequence(0, 50));
        for(int i = 0; i<parm.length; i++){
            System.out.println(parm[i]);
        }
        System.out.println(tmp);
    }
}
