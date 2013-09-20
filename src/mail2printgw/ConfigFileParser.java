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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

/**
 *
 * @author andre
 */
public class ConfigFileParser {
    private static String pathToConfig = null;
    private static ConfigFileParser instance = null;
    
    public static void setPathToConfigFile(String path) {
        if(pathToConfig == null) {
            pathToConfig = path;
        }
    }
    
    /**
     * Only one ConfigFileParser should exist in the program. A config-File
     * should be parsed only once at start time.
     * @return 
     */
    public static ConfigFileParser getInstance(){
        if(instance == null){
            instance = new ConfigFileParser();
            return instance;
        } else {
            return instance;
        }
    }
    
    /**
     * Hashmap containing all imaps-accounts.
     */
    private HashMap<Integer,MailAcc> imapAccs = new HashMap<Integer,MailAcc>();
    public HashMap<Integer,MailAcc> getImapAccs(){
        if(imapAccs.isEmpty()) {
            return null;
        } else {
            return imapAccs;
        }
    }
    
    private ArrayList<MimeType> printableMimes = new ArrayList<MimeType>();
    public ArrayList<MimeType> getPrintableMimes(){
        if(printableMimes.isEmpty()) {
            return null;
        } else {
            return printableMimes;
        }
    }
    
    private String cacertFilePath = null;
    public String getPathToCertFile(){
        return cacertFilePath;
    }
    
    private String urlToLdapServer = null;
    public String getUrlToLdapServer() {
        return urlToLdapServer;
    }
    
    private String tmpDirForPDFs = null;
    public String getTmpDirForPDFs() {
        return tmpDirForPDFs;
    }
    
    private String printCmd = null;
    public String getPrintCmd() {
        return printCmd;
    }
    
    private String convertCmd = null;
    public String getConvertCmd() {
        return convertCmd;
    }
    
    
    
    //private static String configFilePath = "./gw.conf";
    
    /**
     * Config File is read/parsed at creation time of this class.
     * Method print errors if config-file does not exist.
     */
    private ConfigFileParser() {
        //init
        instance = null;
        imapAccs = new HashMap<Integer,MailAcc>();
        printableMimes = new ArrayList<MimeType>();
        cacertFilePath = null;
        urlToLdapServer = null;
        tmpDirForPDFs = null;
        printCmd = null;
        convertCmd = null;
        
        File config = null;
        if(pathToConfig != null) {
            config = new File(pathToConfig);
        } else {
            config = new File("gw.conf");
        }
        
        if(config.exists() && config.isFile()){
            processConfigFile(config);
        } else {
            try {
                Logger.getLogger(ConfigFileParser.class.getName()).log(Level.WARNING,
                        "can't find configfile at path \"" + config.getCanonicalPath() + "\"");
            } catch (IOException ex) {
                Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        Handler[] hs = Logger.getLogger(ConfigFileParser.class.getName()).getHandlers();
        for(int i = 0; i<hs.length; i++) {
            hs[i].flush();
        }
    }
    
    /**
     * This method read and parse the config file.
     * Filter for comments and empty lines
     * 
     * @param config File that point to an existing config-file.
     */
    private void processConfigFile(File config){
        //System.out.println("-- cfp - process config-file");
        
        BufferedReader br = null;
        try {
            
            br = new BufferedReader(new InputStreamReader(new FileInputStream(config)));
            String line = br.readLine();
            
            while (line != null) {
                //Remove comments
                
                if(line.contains("#")) {
                    line = line.substring(0, line.indexOf("#".charAt(0)));
                }
                if(line.contains("'")) {
                    line = line.substring(0, line.indexOf("'".charAt(0)));
                }
                
                line = line.trim();
                //only parse not-empty lines
                
                //System.out.println("line >" + line + "<");
                
                if(line.length()>0){
                    //System.out.println("line to parse: \"" + line + "\"");
                    parseLine(line);
                }
                
                line = br.readLine();
            }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Variable to save state of the parser. If the parser enter a [Section] in
     * the config-file the state of the parser changes.
     */
    private ArrayList<String> stateSectionName = new ArrayList<String>();
    private int stateSectionKey = 0;
    private enum SectionType {none, imapAcc, unknown, faulty};
    private SectionType stateSectionType = SectionType.none;
    
    
    /**
     * Parse one line of the config-file.
     * Parse section names and change state. Check for duplicated section names.
     * Read lines of the config and store information depending on the state.
     * 
     * @param line one line of the config-file.
     */
    private void parseLine(String line){
        //System.out.println("parseLine >" + line + "<");
        if(line.matches("^\\[[a-zA-Z_]+\\d+\\]$")) {
            //System.out.println("if");
            processSectionName(line);
        } else if(line.matches("^[a-zA-Z0-9_:;.\\-\\/]+\\s*=\\s*[a-zA-Z0-9_:;.\\$\\-\\/\\s\\p{Punct}]+$")){
            //System.out.println("else if"); 
            switch(stateSectionType){
                case none:{
                    processGeneralOptionLine(line);
                }
                break;
                    
                case imapAcc:{
                    processImapAccOptionLine(line);
                }
                break;
                    
                case unknown:{
                    Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                            "skip option of unknown section");
                    return;
                }
                    
                case faulty:{
                    Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                            "skip option of faulty section");
                    return;
                }
                    
                default:{
                    Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                            "ERROR: sectiontype not implemented jet");
                    return;
                }
            }
        } else {
            //System.out.println("else");
            Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                            "Parser does not recognize line type:\n" + line);
            stateSectionType = SectionType.faulty;
        }
    }
    
    /**
     * If the parser recognize a line belong to the section of a mail account,
     * the parser call this method to process the line.
     * 
     * @param line 
     */
    
    
    private void processImapAccOptionLine(String line) {
        //System.out.println("processImapAccOptionLine\n>" + line + "<");
        String optionName = line.substring(0, line.indexOf("="));
        String optionValue = line.substring(line.indexOf("=")+1);
        optionName = optionName.trim();
        optionValue = optionValue.trim();
        
        //System.out.println("\"" + optionName + "\" \"" + optionValue + "\"");
        
        MailAcc tmpAcc = null;
        if(imapAccs.containsKey(stateSectionKey)){
            tmpAcc = imapAccs.get(stateSectionKey);
        } else {
            tmpAcc = new MailAcc();
        }
        
        if(optionName.equalsIgnoreCase("url")){
            tmpAcc.url = optionValue;
        } else if(optionName.equalsIgnoreCase("port")){
            tmpAcc.port = Integer.parseInt(optionValue);
        } else if(optionName.equalsIgnoreCase("importCert")) {
            if(optionValue.equalsIgnoreCase("true")){
                tmpAcc.importCert = true;
            } else if(optionValue.equalsIgnoreCase("false")){
                tmpAcc.importCert = false;
            } else {
                Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                    "ERROR: wrong Syntax in config-File. \"" + optionValue +
                        "\" is not a valid option for useSTARTTLS");
            }
        } else if(optionName.equalsIgnoreCase("protocol")){
            tmpAcc.protocol = optionValue;
        } else if(optionName.equalsIgnoreCase("useSTARTTLS")){
            if(optionValue.equalsIgnoreCase("true")){
                tmpAcc.useSTARTTLS = true;
            } else if(optionValue.equalsIgnoreCase("false")){
                tmpAcc.useSTARTTLS = false;
            } else {
                Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                    "ERROR: wrong Syntax in config-File. \"" + optionValue +
                        "\" is not a valid option for useSTARTTLS");
            }
        } else if(optionName.equalsIgnoreCase("username")){
            tmpAcc.username = optionValue;
        } else if(optionName.equalsIgnoreCase("password")){
            tmpAcc.password = optionValue;
        } else if(optionName.equalsIgnoreCase("printer")){
            tmpAcc.printer = optionValue;
        } else {
            Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                "ERROR: wrong Syntax in config-File. unknown option for imapAcc");
        }
        
        if(!imapAccs.containsKey(stateSectionKey)){
            imapAccs.put(stateSectionKey, tmpAcc);
        }
    }
    
    /**
     * If the parser recognize a line which of the general part of the config
     * file, it call this method to process the line.
     * 
     * @param line  line of the config file, which should be processed
     */
    private void processGeneralOptionLine(String line) {
        //System.out.println("processGeneralOptionLine\n>" + line + "<");
        String optionName = line.substring(0, line.indexOf("="));
        String optionValue = line.substring(line.indexOf("=")+1);
        optionName = optionName.trim();
        optionValue = optionValue.trim();
        
        if(optionName.equalsIgnoreCase("printableMimes")){
            processMimeTypes(optionValue);
        } else if(optionName.equalsIgnoreCase("cacertFilePath")) {
            cacertFilePath = new String(optionValue);
        } else if(optionName.equalsIgnoreCase("ldapURL")) {
            urlToLdapServer = new String(optionValue);
        } else if(optionName.equalsIgnoreCase("tempDir")) {
            tmpDirForPDFs = new String(optionValue);
        } else if(optionName.equalsIgnoreCase("printCmd")) {
            printCmd = new String(optionValue);
        } else if(optionName.equalsIgnoreCase("convertCmd")) {
            convertCmd = new String(optionValue);
        } else {
            Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                "skip unknown option: " + line);
        }
    }
    
    /**
     * method process mime-types
     * 
     * @param optionValue   part of line of config-file which represent mime-types
     */
    private void processMimeTypes(String optionValue) {
        if(optionValue.contains(";")) {
            if(isValidSemicolonSeperatedString(optionValue)) {
               String[] mimesStr = optionValue.split(";");
                for(int i = 0; i<mimesStr.length; i++) {
                    try {
                        printableMimes.add(new MimeType(mimesStr[i]));
                    } catch (MimeTypeParseException ex) {
                        Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                            "mime type " + mimesStr[i] + " not valid.", ex);
                    }
                } 
            } else {
                Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                            "mimetypes are not specified correctly.");
                //System.err.println("mimetypes are not specified correctly.");
            }
        } else {
            try {
                //optionvalues with only one mime-type specified
                //System.err.println("\n\nmimeOptionValue: " + optionValue + "\n\n");
                printableMimes.add(new MimeType(optionValue));
            } catch (MimeTypeParseException ex) {
                Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
    }
    
    /**
     * If the parser recognize a line which specify a section name, it call this
     * method to process the line.
     * 
     * @param line  line of the config file, which should be processed
     */
    private void processSectionName(String line) {
        //System.out.println("processSectionName\n>" + line + "<");
        //line is a sectionName
        line = line.substring(line.indexOf("[")+1, line.indexOf("]"));
        
        if(stateSectionName.contains(line)){
            Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                "ERROR: wrong Syntax in config-File. Section-Name \""+
                    line +"\" allready exists.");
            stateSectionType = SectionType.faulty;
            return;
        } else {
            stateSectionName.add(line);
            //System.out.println("Section-Name added: \"" + line + "\"");
        }
        
        String accNum = stateSectionName.get(stateSectionName.size()-1).replaceAll("[a-zA-Z_]+", "");
        stateSectionKey = Integer.parseInt(accNum);
        String name = stateSectionName.get(stateSectionName.size()-1).replaceAll("\\d", "");
        if(name.equalsIgnoreCase("imapacc")){
            stateSectionType = SectionType.imapAcc;
        } else {
            stateSectionType = SectionType.unknown;
            Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                "ERROR: wrong Syntax in config-File. Unknown Section-Name("+
                    name +") found in config file");
        }
    }
    
    /**
     * method check if the value string of an option like mime-types is in a
     * valid format.
     * 
     * @param   str string with the option values.
     * @return  true ... if options are in a valid format
     *          false ... otherwise
     */
    private boolean isValidSemicolonSeperatedString(String str) {
        if(str == null) {
            return false;
        }
        
        str.trim();
        if(str.equals("")) {
            return false;
        }
        
        if(!str.contains(";")) {
            if(str.length()>0) {
                return false;
            } else {
                return false;
            }
        } else {
            String[] vals = str.split(";");
            for(int i = 0; i<vals.length; i++) {
                if(null == vals[i]) {
                    return false;
                }
                
                vals[i].trim();
                if("".equals(vals[i])){
                    return false;
                }
            }
        }
        return true;
    }
}
