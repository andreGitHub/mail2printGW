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
import java.util.HashMap;
import javax.activation.MimeType;
import mail2printgw.ConfigFileParser;
import mail2printgw.ImapAcc;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author eddi
 */
public class TestFetchingMails {
    ConfigFileParser cfp = null;
    HashMap<Integer, ImapAcc> accs = null;
    ArrayList<MimeType> printables = null;
    
    public TestFetchingMails() {
        System.out.println("constructor");
    }
    
    @BeforeClass
    public static void setUpClass() {
        System.out.println("setUpClass");
    }
    
    @AfterClass
    public static void tearDownClass() {
           System.out.println("tearDownClass");
    }
    
    @Before
    public void setUp() {
        System.out.println("setUp");
        cfp = ConfigFileParser.getInstance();
        accs = cfp.getImapAccs();
        printables = cfp.getPrintableMimes();
    }
    
    @After
    public void tearDown() {
        System.out.println("tearDown");
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void FetchMailWithoutContentAndAttachment() {
        System.out.println("FetchMailWithoutContentAndAttachment");
    }
}