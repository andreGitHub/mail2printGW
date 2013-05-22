/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mail2printgw;

import java.util.ArrayList;
import javax.activation.MimeType;

/**
 *
 * @author andre
 */
public class PrintItem {
    private ArrayList<String> senders = null;
    
    public PrintItem(ArrayList<String> senders){
        this.senders = senders;
    }
    
    public static boolean mimePrintable(MimeType mime){
        for(MimeType actMime : ConfigFileParser.getInstance().getPrintableMimes()){
            if(actMime.match(mime)){
                return true;
            }
        }
        return false;
    }
}
