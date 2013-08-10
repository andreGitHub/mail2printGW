package mail2printgw.Tests;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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