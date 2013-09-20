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

import javax.mail.Store;

/**
 *
 * @author andre
 */
public class MailAcc {
    //attributes filled from config
    public String url = null;
    public int    port = 0;
    public boolean importCert = false;
    public String protocol = null;
    public boolean useSTARTTLS = false;
    public String username = null;
    public String password = null;
    public String printer = null;
    
    //attributes filled by MailFetcher
    public Store mailStore = null;
}
