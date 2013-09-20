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


import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

/**
 * This class is used for Ldap lookups. It is used to check, if a given email
 * address is inside the ldap directory. It can be used to authorize the sender
 * of an email.
 * 
 * @author eddi
 */
public class LdapConnection {
    private static LdapConnection instance = null;
    
    /**
     * Only one LdapConnection should exist in the program. A config-File
     * should be parsed only once at start time.
     * @return 
     */
    public static LdapConnection getInstance(){
        if(instance == null){
            return new LdapConnection();
        } else {
            return instance;
        }
    }
    
    private Hashtable<String, String> environment = null;
    
    private LdapConnection() {
        //ssh -L 636:intserv-new.net.t-labs.tu-berlin.de:636 -l aschuetze roadrunner.net.t-labs.tu-berlin.de
        ConfigFileParser cfp = ConfigFileParser.getInstance();
        String ldapURL = cfp.getUrlToLdapServer().trim();
        
        
        //System.out.println("\n\n\n\nldap " + ldapURL + "\n\n\n\n");
        
        
        while(ldapURL.endsWith("/")) {
            ldapURL = ldapURL.substring(0, ldapURL.length()-2);
        }
        
        environment = new Hashtable<String, String>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL,
                ldapURL + "/ou=People,ou=net,dc=t-labs,dc=tu-berlin,dc=de");
        environment.put(Context.SECURITY_AUTHENTICATION, "none");
    }
    
    public boolean emailAddressInLdap(String email) {
        if(null == email || "*".equalsIgnoreCase(email)) {
            return false;
        }
        try {
            DirContext ctx = new InitialDirContext(environment);
            
            SearchControls sc = new SearchControls();  
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            
            LdapName name = new LdapName("");
            
            
            NamingEnumeration<SearchResult> neMail = ctx.search(name, "(mail=" +
                    email+ ")", sc);
            if(neMail.hasMore()) {
                return true;
            }
            
            NamingEnumeration<SearchResult> neMailAlias = ctx.search(name,
                    "(mailalias=" + email + ")", sc);
            while(neMailAlias.hasMore()){
                return true;
            }
        } catch (NamingException ex) {
            Logger.getLogger(LdapConnection.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
        return false;
    }
}
