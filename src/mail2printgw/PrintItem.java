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
import java.util.ArrayList;
import javax.activation.MimeType;
//import org.apache.pdfbox.pdmodel.PDDocument;

/**
 *
 * @author andre
 */
public class PrintItem {
    private ArrayList<String> from = null;
    private String printer = null;
    private String pathToAttachmentFile = null;
    private String pathToPrintFile = null;
    private MimeType mime = null;
    /*
    private int itemNr = -1;
    private String fileName = null;
    private PDDocument doc = null;
    */

    public void setFrom(ArrayList<String> parm) {
        from = parm;
    }
    public ArrayList<String> getFrom() {
        return from;
    }
    
    public void setFilePath(String path) {
        this.pathToAttachmentFile = path;
    }
    public void setFilePathToPrintFile(String path) {
        this.pathToPrintFile = path;
    }
    public void attachedFileToPrintFile() {
        pathToPrintFile = pathToAttachmentFile;
    }
    public String getPathToPrintFile() {
        return pathToPrintFile;
    }
    public String getPathToAttachmentFile() {
        return pathToAttachmentFile;
    }
    
    public void setPrinter(String pr) {
        printer = pr;
    }
    public String getPrinter() {
        return printer;
    }
    
    public void setMime(MimeType m) {
        mime = m;
    }
    public MimeType getMime() {
        return mime;
    }
    
    public static boolean mimePrintable(MimeType mime){
        ArrayList<MimeType> mimes = ConfigFileParser.getInstance().getPrintableMimes();
        if(mimes == null) {
            return false;
        }
        for(MimeType actMime : mimes){
            if(actMime.match(mime)){
                return true;
            }
        }
        return false;
    }
    
    public String getInformation() {
        String ret = "from: ";
        for(String f : from) {
            ret += f + "; ";
        }
        
        ret += "\nprinter: " + printer;
        
        ret += "\npath: " + pathToAttachmentFile;
        
        File f = new File(pathToAttachmentFile);
        ret += "\nfile-length: " + f.length();
        
        return ret;
    }
}
