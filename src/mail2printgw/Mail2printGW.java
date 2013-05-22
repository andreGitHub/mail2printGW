/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mail2printgw;


/**
 *
 * @author andre
 */
public class Mail2printGW {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //MailFetcher fetcher = new MailFetcher();
        //fetcher.fetchMails();
        MailFetcher2 fetcher = new MailFetcher2();
        fetcher.getAllPrintableItems();
    }
}
