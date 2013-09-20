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

import mail2printgw.LdapConnection;
import static org.junit.Assert.*;

/**
 * This is a test class to test the class which is used to establish a
 * connection to a ldap server and 
 * 
 * @author eddi
 */
public class TestLdapConnection extends Test{
    @org.junit.Test
    public void testMailNameInLdap() {
        origOut.println("-- tlc - testMailNameInLdap");
        LdapConnection ldap = LdapConnection.getInstance();
        
        //ldap.emailAddressInLdap("rainer@net.t-labs.tu-berlin.de");
        
        assertTrue(ldap.emailAddressInLdap("rainer@net.t-labs.tu-berlin.de"));
        checkOutput();
    }
    
    @org.junit.Test
    public void testMailAliasInLdap() {
        origOut.println("-- tlc - testMailAliasInLdap");
        LdapConnection ldap = LdapConnection.getInstance();
        assertTrue(ldap.emailAddressInLdap("florian@inet.tu-berlin.de"));
        checkOutput();
    }
    
    @org.junit.Test
    public void testNullInLdap() {
        origOut.println("-- tlc - testNullInLdap");
        LdapConnection ldap = LdapConnection.getInstance();
        assertFalse(ldap.emailAddressInLdap(null));
        checkOutput();
    }
    
    @org.junit.Test
    public void testWildcardInLdap() {
        origOut.println("-- tlc - testWildcardInLdap");
        LdapConnection ldap = LdapConnection.getInstance();
        assertFalse(ldap.emailAddressInLdap("*"));
        checkOutput();
    }
    
    @org.junit.Test
    public void testMailNameNotInLdap() {
        origOut.println("-- tlc - testMailNameNotInLdap");
        LdapConnection ldap = LdapConnection.getInstance();
        assertFalse(ldap.emailAddressInLdap("iregenwer@net.t-labs.tu-berlin.de"));
        checkOutput();
    }
    
    @org.junit.Test
    public void testSeveralLdapQueries() {
        origOut.println("-- tlc - testMailAliasInLdap");
        LdapConnection ldap = LdapConnection.getInstance();
        assertTrue(ldap.emailAddressInLdap("florian@inet.tu-berlin.de"));
        assertTrue(ldap.emailAddressInLdap("rainer@net.t-labs.tu-berlin.de"));
        assertTrue(ldap.emailAddressInLdap("rainer@inet.tu-berlin.de"));
        assertTrue(ldap.emailAddressInLdap("cmack@inet.tu-berlin.de"));
        checkOutput();
    }
}
