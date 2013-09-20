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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import mail2printgw.Certificate.CertificateCheck;


/**
 *
 * @author andre
 */
public class Mail2printGW {
    //private static String pathOfLogFile = null;
    private static String pathToDirTheRunningJarIsLocated = null;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        pathToDirTheRunningJarIsLocated = getPathToDirTheJarRunsIn();
        
        //distTest();
        
        setLogging(pathToDirTheRunningJarIsLocated + "mail2PrintGW_error.log");
        
        ConfigFileParser.setPathToConfigFile(pathToDirTheRunningJarIsLocated + "gw.conf");
        String path = ConfigFileParser.getInstance().getPathToCertFile();
        if(path != null && !path.equals("")) {
            System.setProperty("javax.net.ssl.keyStore", path);
            System.setProperty("javax.net.ssl.trustStore", path);
        }
        
        Logger.getLogger(Mail2printGW.class.getName()).log(Level.SEVERE,"start to print.");
        MailFetcher2 fetcher = new MailFetcher2();
        fetcher.printAllAttachmentsOfAllMailsOfAllAccs(ConfigFileParser.getInstance().getImapAccs());
        
    }
    
    private static void setLogging(String pathToLogFile) {
        Logger rootLogger = Logger.getLogger("");
        
        
        //File logFile = new File(pathOfLogFile);
        FileHandler logHandler = null;
        try {
            logHandler = new FileHandler(pathToLogFile, true);
        } catch (IOException ex) {
            Logger.getLogger(Mail2printGW.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(Mail2printGW.class.getName()).log(Level.SEVERE, null, ex);
        }
        logHandler.setFormatter(new SimpleFormatter()); 
        logHandler.setLevel(Level.INFO); 
        rootLogger.removeHandler(rootLogger.getHandlers()[0]); 
        rootLogger.setLevel(Level.INFO); 
        rootLogger.addHandler(logHandler); 
    }
    
    private static void distTest() {
        System.out.println("dist test");
        File tf = new File("hallo.txt");
        File jar = new File(Mail2printGW.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        System.out.println("path of ./hallo.txt dir: " + tf.getAbsolutePath());
        
        File inf = new File("/home/eddi/NetBeansProjects/mail2printGW/dist/info.txt");
        if(!inf.exists()) {
            try {
                inf.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(Mail2printGW.class.getName()).log(Level.SEVERE,
                        "cant create file", ex);
            }
        }
        
        FileWriter fw = null;
        try {
            fw = new FileWriter(inf);
            fw.write("home dir: " + tf.getAbsolutePath() + "\n");
            fw.write("operating dir: " + jar.getAbsolutePath() + "\n");
            fw.write("path to dir the jar runs in: " + pathToDirTheRunningJarIsLocated + "\n");
            fw.flush();
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(Mail2printGW.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static String getPathToDirTheJarRunsIn() {
        File jar = new File(Mail2printGW.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String path = jar.getAbsolutePath();
        int end = path.indexOf("mail2printGW.jar");
        path = path.substring(0, end);
        return path;
    }
}
