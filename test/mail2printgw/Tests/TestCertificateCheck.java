package mail2printgw.Tests;

import mail2printgw.Certificate.CertificateCheck;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andre
 */
public class TestCertificateCheck {
    public TestCertificateCheck() {
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
    }
    
    @After
    public void tearDown() {
        System.out.println("tearDown");
    }
    
    @Test
    public void connectToHostWithExsistingCertificate() {
        System.out.println("ConnectToHostWithExsistingCertificate");
        CertificateCheck cc = new CertificateCheck();
        assertTrue(cc.hasValidCertificate("www.google.de", 443));
    }
}
