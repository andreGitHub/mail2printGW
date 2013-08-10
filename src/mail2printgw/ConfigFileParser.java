/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

/**
 *
 * @author andre
 */
public class ConfigFileParser {
    
    private static ConfigFileParser instance = null;
    
    /**
     * Only one ConfigFileParser should exist in the program. A config-File
     * should be parsed only once at start time.
     * @return 
     */
    public static ConfigFileParser getInstance(){
        if(instance == null){
            return new ConfigFileParser();
        } else {
            return instance;
        }
    }
    
    /**
     * Hashmap containing all imaps-accounts.
     */
    private HashMap<Integer,ImapAcc> imapAccs = new HashMap<Integer,ImapAcc>();
    public HashMap<Integer,ImapAcc> getImapAccs(){
        return imapAccs;
    }
    
    private ArrayList<MimeType> printableMimes = new ArrayList<MimeType>();
    
    public ArrayList<MimeType> getPrintableMimes(){
        return printableMimes;
    }
    
    private String configFilePath = "./gw.conf";
    
    /**
     * Config File is read/parsed at creation time of this class.
     * Method print errors if config-file does not exist.
     */
    private ConfigFileParser() {
        File config = new File(configFilePath);
        
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
    }
    
    /**
     * This method read and parse the config file.
     * Filter for comments and empty lines
     * 
     * @param config File that point to an existing config-file.
     */
    private void processConfigFile(File config){
        BufferedReader br = null;
        try {
            
            br = new BufferedReader(new InputStreamReader(new FileInputStream(config)));
            String line = br.readLine();
            
            while (line != null) {
                //Remove comments
                int comNo = -1;
                if(line.indexOf("#") >= line.indexOf("'")){
                    comNo = line.indexOf("#");
                } else {
                    comNo = line.indexOf("'");
                }
                
                if(comNo != -1){
                    line = line.substring(0,comNo);
                }
                
                //only parse not-empty lines
                if(line.length()>0){
                    line = line.trim();
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
        if(line.startsWith("#") || line.startsWith("'")){
            //System.out.println("parser recognize a comment");
            //line is a comment => skip
        } else if(line.matches("^\\[[a-zA-Z_]+\\d*\\]")) {
            //System.out.println("parser recognize a sectionname");
            //line is a sectionName
            line = line.substring(line.indexOf("[")+1, line.indexOf("]"));
            
            if(stateSectionName.contains(line)){
                Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                        "ERROR: wrong Syntax in config-File. Section-Name \""+ line +"\" allready exists.");
                stateSectionType = SectionType.faulty;
                return;
            } else {
                stateSectionName.add(line);
                //System.out.println("Section-Name added: \"" + line + "\"");
            }
            
            stateSectionKey = Integer.parseInt(stateSectionName.get(stateSectionName.size()-1).replaceAll("[a-zA-Z_]+", ""));
            
            String name = stateSectionName.get(stateSectionName.size()-1).replaceAll("\\d", "");
            if(name.equalsIgnoreCase("imapacc")){
                stateSectionType = SectionType.imapAcc;
            } else {
                stateSectionType = SectionType.unknown;
                Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                        "ERROR: wrong Syntax in config-File. Unknown Section-Name("+ name +") found in config file");
            }
        } else {
            //System.out.println("parser recognize an option");
            
            switch(stateSectionType){
                case none:{
                    String optionName = line.substring(0, line.indexOf("="));
                    String optionValue = line.substring(line.indexOf("=")+1);
                    optionName = optionName.trim();
                    optionValue = optionValue.trim();
                    
                    if(optionName.equals("printableMimes")){
                        String[] mimesStr = optionValue.split(";");
                        for(int i = 0; i<mimesStr.length; i++){
                            try {
                                printableMimes.add(new MimeType(mimesStr[i]));
                            } catch (MimeTypeParseException ex) {
                                Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                                        "mime type " + mimesStr[i] + " not valid.", ex);
                            }
                        }
                    } else {
                        Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                            "skip unknown option: " + line);
                    }
                }
                break;
                    
                case imapAcc:{
                    String optionName = line.substring(0, line.indexOf("="));
                    String optionValue = line.substring(line.indexOf("=")+1);
                    optionName = optionName.trim();
                    optionValue = optionValue.trim();
                
                    //System.out.println("\"" + optionName + "\" \"" + optionValue + "\"");
                    
                    ImapAcc tmpAcc = null;
                    if(imapAccs.containsKey(stateSectionKey)){
                        tmpAcc = imapAccs.get(stateSectionKey);
                    } else {
                        tmpAcc = new ImapAcc();
                    }
                    
                    if(optionName.equalsIgnoreCase("url")){
                        tmpAcc.url = optionValue;
                    } else if(optionName.equalsIgnoreCase("port")){
                        tmpAcc.port = Integer.parseInt(optionValue);
                    } else if(optionName.equalsIgnoreCase("protocol")){
                        tmpAcc.protocol = optionValue;
                    } else if(optionName.equalsIgnoreCase("useSTARTTLS")){
                        if(optionValue.equalsIgnoreCase("true")){
                            tmpAcc.useSTARTTLS = true;
                        } else if(optionValue.equalsIgnoreCase("false")){
                            tmpAcc.useSTARTTLS = false;
                        } else {
                            Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                                "ERROR: wrong Syntax in config-File. \"" + optionValue + "\" is not a valid option for useSTARTTLS");
                        }
                    } else if(optionName.equalsIgnoreCase("username")){
                        tmpAcc.username = optionValue;
                    } else if(optionName.equalsIgnoreCase("password")){
                        tmpAcc.password = optionValue;
                    } else {
                        Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE,
                                "ERROR: wrong Syntax in config-File. unknown option for imapAcc");
                    }
                    
                    if(!imapAccs.containsKey(stateSectionKey)){
                        imapAccs.put(stateSectionKey, tmpAcc);
                    }
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
        }
    }
}
