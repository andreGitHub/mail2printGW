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
import mail2printgw.MailAcc;
import org.junit.Test;

/**
 *
 * @author eddi
 */
public class TestTestToolbox extends mail2printgw.Tests.Test{
    @Test
    public void testSendMailWithYahooAcc() {
        //System.out.println("putOneMailInAccount");
        MailAcc ma = TestToolbox.getMailAcc_myImap();

        ArrayList<String> fts = new ArrayList<String>();
        fts.add("test/testPDFs/pdf2.pdf");
        
        String to = TestToolbox.getCorrectMailAddress(ma.username, ma.url);
        //origOut.println("to: " + to);
        TestToolbox.sendMessageOverYahooDe(to, fts);
        checkOutput();
    }
    
    @Test
    public void testSendMailWithWebAcc() {
        //System.out.println("putOneMailInAccount");
        MailAcc ma = TestToolbox.getMailAcc_myImap();

        ArrayList<String> fts = new ArrayList<String>();
        fts.add("test/testPDFs/pdf2.pdf");
        
        
        TestToolbox.sendMessageOverWebDe(
                TestToolbox.getCorrectMailAddress(ma.username, ma.url), fts);
        checkOutput();
    }
}
