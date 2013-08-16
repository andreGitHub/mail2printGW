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

package mail2printgw.Certificate;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

public class SavingTrustManager implements X509TrustManager

     {

          private final X509TrustManager tm;

          private X509Certificate[] chain;

          
          public X509Certificate[] getChain(){
              return chain;
          }
          

          SavingTrustManager(X509TrustManager tm)

          {

               this.tm = tm;

          }

          
          @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
            // throw new UnsupportedOperationException();
        }

          

          public void checkClientTrusted(X509Certificate[] chain, String authType)

          throws CertificateException

          {

               throw new UnsupportedOperationException();

          }

          

          public void checkServerTrusted(X509Certificate[] chain, String authType)

          throws CertificateException

          {

               this.chain = chain;

               tm.checkServerTrusted(chain, authType);

          }

     }