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

import java.util.ArrayList;
import mail2printgw.PrintItem;
import mail2printgw.PrintManager;
import static org.junit.Assert.*;


/**
 *
 * @author eddi
 */
public class TestPrintManager extends Test{
    private String tmpDir = "/home/eddi/NetBeansProjects/mail2printGW/tmp/dirToTestPrintManager/";
    
    public TestPrintManager() {
        
    }
    
    @org.junit.Test
    public void testPrintManagerForNull() {
        getNewFilesInCupsPdfPrintFolder();
        PrintManager.getInstance().printItem(null);
        ArrayList<String> newFiles = getNewFilesInCupsPdfPrintFolder();
        assertTrue(0 == newFiles.size());
    }
    
    @org.junit.Test
    public void testPrintManagerWithOneMailWithOneValidAttachment() {
        PrintItem pi = new PrintItem();
        ArrayList<String> from = new ArrayList<String>();
        //from.add
        //pi.setFrom(from);
        
        getNewFilesInCupsPdfPrintFolder();
        PrintManager.getInstance().printItem(null);
        ArrayList<String> newFiles = getNewFilesInCupsPdfPrintFolder();
        assertTrue(0 == newFiles.size());
    }
}
