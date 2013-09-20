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

package mail2printgw.Tests;

import mail2printgw.Tests.configFileParser.MyStreamHandler;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import mail2printgw.ConfigFileParser;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author eddi
 */
public abstract class Test {
    protected String defaultCupsPdfOutputDir = "/var/spool/cups";
    protected String[] filesInCupsPdfOutputDir = null;
    
    protected PrintStream origOut = System.out;
    protected PrintStream origErr = System.err;
    
    protected ByteArrayOutputStream baosOut = null;
    protected ByteArrayOutputStream baosErr = null;
    
    /*
    public Test() {
        origOut = System.out;
        origErr = System.err;
        
        baosOut = new ByteArrayOutputStream();
        baosErr = new ByteArrayOutputStream();
        
        PrintStream psOut = new PrintStream(baosOut);
        PrintStream psErr = new PrintStream(baosErr);
        
        System.setOut(psOut);
        System.setErr(psErr);
    }
    */
    
    @BeforeClass
    public static void setUpClass() {
        //System.out.println("-- t   - Test.setUpClass()");
        
        File sendFolder = new File("test/testPDFs/send/");
        if(!sendFolder.exists()) {
            sendFolder.mkdir();
        }
        
        File[] cont = sendFolder.listFiles();
        for(int i = cont.length - 1; i>=0; i--) {
            cont[i].delete();
        }
    }
    
    @Before
    public void setUp() {
        File printFolder = new File(defaultCupsPdfOutputDir);
        
        if(!printFolder.canRead()) {
            System.out.println("-- t   - unable to read folder " + defaultCupsPdfOutputDir);
            System.out.println("-- t   - change it to run the test sucessfully");
            System.exit(1);
        }
        String[] tmp = printFolder.list();
        if(tmp.length>0) {
            ArrayList<String> tmp2 = new ArrayList<String>();
            for(int i = 0; i<tmp.length; i++) {
                if(null != tmp[i]) {
                    tmp2.add(tmp[i]);
                }
            }
            filesInCupsPdfOutputDir = new String[tmp2.size()];
            int i = 0;
            for(String act : tmp2) {
                filesInCupsPdfOutputDir[i] = act;
                i++;
            }
            
            Arrays.sort(filesInCupsPdfOutputDir);
        } else {
            filesInCupsPdfOutputDir = tmp;
        }
            
        origOut = System.out;
        origErr = System.err;
        
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        origOut.println("-- tdm - test started at: " + dateFormat.format(date));
        //System.out.println("-- tdm - test started at: " + dateFormat.format(date));
        
        
        baosOut = new ByteArrayOutputStream();
        baosErr = new ByteArrayOutputStream();
        
        PrintStream psOut = new PrintStream(baosOut);
        PrintStream psErr = new PrintStream(baosErr);
        
        System.setOut(psOut);
        System.setErr(psErr);
        
        //LogManager.getLogManager().reset();
        //Handler[] hs = Logger.getLogger(ConfigFileParser.class.getName()).getHandlers();
        //for(int i = 0; i<hs.length; i++) {
        //    origOut.println("-- t - remove");
        //    Logger.getLogger(ConfigFileParser.class.getName()).removeHandler(hs[i]);
        //}
        
        Logger.getLogger(ConfigFileParser.class.getName())
                .addHandler(new MyStreamHandler(psErr));
        
        Logger.getLogger(ConfigFileParser.class.getName()).setUseParentHandlers(false);
    }
    
    @After
    public void tearDown() {
        //System.out.println("out: >" + origOut.toString() + "<");
        //System.out.println("err: >" + origErr.toString() + "<");
        
        
        System.out.close();
        System.err.close();
        
        System.setOut(origOut);
        System.setErr(origErr);
        
        try {
            baosOut.close();
            baosErr.close();
        } catch (IOException ex) {
            Logger.getLogger(TestDifferentMailAccs.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    @AfterClass
    public static void tearDownClass() {
        File sendFolder = new File("test/testPDFs/");
        FilenameFilter fnf = new FilenameFilter() {
            @Override
            public boolean accept(File file, String string) {
                String name = file.getName();
                if(name.equals("send")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        File[] content =  sendFolder.listFiles(fnf);
        for(int i = 0; i<content.length; i++) {
            content[i].delete();
        }
        
        //delete all stuff in tmp - folder
        File tmp = new File("tmp");
        System.out.println("path to clean: " + tmp.getAbsolutePath());
        File[] f = tmp.listFiles();
        for(int i = 0; i<f.length; i++){
            deleteFolder(f[i]);
        }
    }
    
    private static void deleteFolder(File f) {
        if(f.isDirectory()){
            File[] tmp = f.listFiles();
            for(int i = 0; i<tmp.length; i++) {
                deleteFolder(tmp[i]);
            }
            f.delete();
        } else {
            f.delete();
        }
    }
            
    
    protected ArrayList<String> getNewFilesInCupsPdfPrintFolder() {
        ArrayList<String> newFiles = new ArrayList<String>();
        File tmp = new File(defaultCupsPdfOutputDir);
        String[] actFilesInCupsPdfDefaultDir = tmp.list();
        Arrays.sort(actFilesInCupsPdfDefaultDir);
        
        //System.out.println("files length: " + actFilesInCupsPdfDefaultDir.length);
        
        int i = 0;
        int j = 0;
        //System.out.println("-- t   - oldL: " + filesInCupsPdfOutputDir.length);
        //System.out.println("-- t   - newL: " + actFilesInCupsPdfDefaultDir.length);
        while(i<filesInCupsPdfOutputDir.length) {
            if(j<actFilesInCupsPdfDefaultDir.length) {
                if(actFilesInCupsPdfDefaultDir[j] != null) {
                    if(filesInCupsPdfOutputDir[i].equals(actFilesInCupsPdfDefaultDir[j])) {
                        i++;
                        j++;
                    } else {
                        newFiles.add(actFilesInCupsPdfDefaultDir[j]);
                        //System.out.println("-- t   - " + actFilesInCupsPdfDefaultDir[i]);
                        j++;
                    }
                }
            } else {
                i = filesInCupsPdfOutputDir.length;
            }
        }
        for(int k = j; k<actFilesInCupsPdfDefaultDir.length; k++) {
            newFiles.add(actFilesInCupsPdfDefaultDir[k]);
        }
        
        filesInCupsPdfOutputDir = actFilesInCupsPdfDefaultDir;
        return newFiles;
    }

    protected void showOutput() {
        String strOut = baosOut.toString();
        baosOut.reset();
        String strErr =  baosErr.toString();
        baosErr.reset();
        
        origOut.println("stuff:\n>" + strOut + "<\n\n>" + strErr + "<");
    }
    
    protected void checkOutput() {
        ////////////////////////////////////
        String strOut = baosOut.toString();
        String strErr = baosErr.toString();
        
        if(strOut.length() != 0 || !strOut.isEmpty() || !strOut.equals("")) {
            origOut.println(">" + strOut + "<");
        }
        if(strErr.length() != 0 || !strErr.isEmpty() || !strErr.equals("")) {
            origOut.println(">" + strErr + "<");
        }
        assertTrue(strOut.length() == 0);
        assertTrue(strErr.length() == 0);
        assertTrue(strOut.isEmpty());
        assertTrue(strErr.isEmpty());
        assertTrue(strOut.equals(""));
        assertTrue(strErr.equals(""));
        
        baosOut.reset();
        baosErr.reset();
        ////////////////////////////////////
    }
}
