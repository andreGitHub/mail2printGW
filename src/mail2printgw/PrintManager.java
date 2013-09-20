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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class converts files and issue print commands.
 * 
 * @author eddi
 */
public class PrintManager {
    private static PrintManager instance = null;
    
    /**
     * Only one PrintManager should exist in the program. 
     * @return instance of PrintManager
     */
    public static PrintManager getInstance(){
        if(instance == null){
            instance = new PrintManager();
            return instance;
        } else {
            return instance;
        }
    }
    
    private boolean debugMode = false;
    private ArrayList<String> pdfMimes = new ArrayList<String>();
    private ArrayList<String> unspecifiedMimes = new ArrayList<String>();
    
    private PrintManager() {
        pdfMimes.add("application/pdf");
        unspecifiedMimes.add("application/octet-stream");
    }
    
    public void printItem(PrintItem pi) {
        if(pi == null) {
            return;
        }
        LdapConnection ldap = LdapConnection.getInstance();
        boolean valid = false;
        for(String act : pi.getFrom()) {
            if(ldap.emailAddressInLdap(act)) {
                valid = true;
                break;
            }
        }
        if(!valid && !debugMode) {
            return;
        }
        
        String strSuffix = getSuffix(pi.getPathToAttachmentFile());
        boolean convert = false;
        if(pdfMimes.contains(pi.getMime().toString())) {
            //printitem is pdf
            convert = true;
        } else if(unspecifiedMimes.contains(pi.getMime().toString())) {
            //printitem is unspezified, take suffix
            if("pdf".equalsIgnoreCase(strSuffix)) {
                convert = true;
            }
        } else {
            //unsupportet mime
            Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE,
                "unsupportet mime: " + pi.getMime().getBaseType(), new Exception());
        }
        
        runConvertCommand(convert, pi);
        runPrintCommand(pi);
    }
    
    private void runConvertCommand(boolean convert, PrintItem pi) {
        //convert
        if(convert) {
            String convertCMD = ConfigFileParser.getInstance().getConvertCmd();
            if(convertCMD != null &&
                    convertCMD.contains("$inputPDF") &&
                    convertCMD.contains("$outputPS")) {
                
                String pathToPrintFile = getTmpPath(pi.getPathToAttachmentFile());
                String cmd = convertCMD.replace("$inputPDF", pi.getPathToAttachmentFile())
                        .replace("$outputPS", pathToPrintFile);
                
                try {
                    Process p = Runtime.getRuntime().exec(cmd);
                    p.waitFor();
                    if(0 == p.exitValue()) {
                        pi.setFilePathToPrintFile(pathToPrintFile);
                    }
                    
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                    String line = reader.readLine();
                    while (line != null) {
                        System.out.println(line);
                        line = reader.readLine();
                    }
                } catch (InterruptedException | IOException ex) {
                    Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE, 
                            "unable to run print-command.", ex);
                }
            } else {
                Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE,
                "unable to convert: " + pi.getPathToAttachmentFile(), new Exception());
            }
        } else {
            pi.attachedFileToPrintFile();
        }
    }
    
    private void runPrintCommand(PrintItem pi) {
        if(pi.getPathToPrintFile() == null) {
            System.out.println("error because there is no file to print.");
            return;
        }
        //print
        String printCMD = ConfigFileParser.getInstance().getPrintCmd();
        if(printCMD != null &&
                    printCMD.contains("$inputPS") &&
                    printCMD.contains("$printer")) {
            
            String cmd = printCMD.replace("$inputPS", pi.getPathToPrintFile());
            cmd = cmd.replace("$printer", pi.getPrinter());
            try {
                Process p = Runtime.getRuntime().exec(cmd);
                p.waitFor();
                if(0 != p.exitValue()) {
                    System.out.println("failure while running print command.");
                }
                
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                String line = reader.readLine();
                while (line != null) {
                    System.out.println(line);
                    line = reader.readLine();
                }
            } catch (InterruptedException | IOException ex) {
                Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE, 
                        "unable to run print-command.", ex);
            }
            
            //System.out.println("print:       " + cmd);
        } else {
            Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE,
                "unable to print: " + pi.getPathToPrintFile(), new Exception());
        }
    }
    
    private String getSuffix(String path) {
        String suffix = null;
        
        //System.out.println("mime: " + pi.getMime().getBaseType());
        String[] pathParts = path.split(File.separator);
        if(pathParts[pathParts.length-1].contains(".")){
            String fileName = pathParts[pathParts.length-1];
            //System.out.println("name:   " + fileName);
            String[] nameParts = fileName.split("\\.");
            //System.out.println("length: " + nameParts.length);
            //System.out.println("suffix: " + nameParts[nameParts.length-1]);
            suffix = nameParts[nameParts.length-1];
            return suffix;
        } else {
            //System.out.println("suffix: null");
            return null;
        }
        
        //System.out.println("suffix: " + suffix);
    }
    
    private String getTmpPath(String path) {
        if(path == null) {
            return null;
        }
        
        String[] pathItems = path.split(File.separator);
        if(pathItems.length == 0) {
            return null;
        }
        
        String fileName = pathItems[pathItems.length-1];
        String[] fileNameParts = fileName.split("\\.");
        
        String newFileName = new String();
        for(int i = 0; i<fileNameParts.length-1; i++) {
            newFileName += fileNameParts[i] + ".";
        }
        newFileName += "ps";
        
        String newPath = "/";
        for(int i = 0; i<pathItems.length-1; i++) {
            //pathItems[i].replace("/".charAt(0), " ".charAt(0));
            //pathItems[i].trim();
            if(null != pathItems[i] && pathItems[i].length()>0) {
                //System.out.println("pathItem: >" + pathItems[i] + "<");
                newPath += pathItems[i] + "/";
            }
        }
        newPath += newFileName;
        //System.out.println("newPath: " + newPath);
        return newPath;
    }
    
/*    
    public static void printItem(PrintItem pi) {
        System.out.println(pi.getInformation());
        
        /*
        String error = null;
        File f = new File("test/testPDFs/" + pi.getFileName());
        PDDocument pdd = null;
        try {
            pdd = PDDocument.load(f);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        /*
        
        boolean equal = true;
        if(pdd.isEncrypted() != pi.getDocument().isEncrypted()) {
            error = "dencryption";
            equal = false;
        }
        if(pdd.getNumberOfPages() != pi.getDocument().getNumberOfPages()) {
            error = "no of pages";
            equal = false;
        }
        if(pdd.toString().equals(pi.getDocument().toString())) {
            error = "document to string";
            equal = false;
        }
        if(pdd.getDocument().equals(pi.getDocument().getDocument())) {
            error = "documents";
            equal = false;
        }
        if(pdd.getDocumentCatalog().getAllPages().get(1)
                .equals(pi.getDocument().getDocumentCatalog().getAllPages().get(1))) {
            error = "first page";
            equal = false;
        }
        if(!checkEquality(
                pdd.getDocumentInformation().getAuthor(),
                pi.getDocument().getDocumentInformation().getAuthor())) {
            error = "author";
            equal = false;
        }
        try {
            if(!checkEquality(
                    pdd.getDocumentInformation().getCreationDate(),
                    pi.getDocument().getDocumentInformation().getCreationDate())) {
                error = "creation data";
                equal = false;
            }
        } catch (IOException ex) {
            Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
        if(checkEquality(pdd.getDocumentInformation().getCreator(),
                pi.getDocument().getDocumentInformation().getCreator())) {
            error = "creator";
            equal = false;
        }
        */
        
        /*
        if(!equal) {
            System.out.println("unequal " + error);
        }
        
        
        
        File fOut = new File("test/testPDFs/send/" + pi.getFileName());
        try {
            pi.getDocument().save(fOut);
            pi.getDocument().close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (COSVisitorException ex) {
            Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    */
    
    /*
    private boolean checkEquality(Object o1, Object o2){
        if(o1 == null) {
            if(o2 != null) {
                return false;
            }
        } else {
            if(!o1.equals(o2)) {
                return false;
            }
        }
        return true;
    }
    */
}
