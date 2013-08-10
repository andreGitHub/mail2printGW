package trying_Out;

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