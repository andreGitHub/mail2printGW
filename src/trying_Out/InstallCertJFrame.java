package trying_Out;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Vector;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/*
 * InstallCertJFrame.java
 *
 * Created on 22 September 2008, 13:21
 */

/**
 *
 * @author  AndreasSterbenz
 */
public class InstallCertJFrame {
    private String     host = "www.verisign.com";
    private int          port = 443;
    private String     passphrase = "changeit";
    private KeyStore     ks;
    private SavingTrustManager tm;
    private X509TrustManager      defaultTrustManager;
    
    /** Creates new form InstallCertJFrame */
    public InstallCertJFrame() {
        
    }

    void     connect()
    {
        try
        {
            File file = new File("jssecacerts");
            if (!file.isFile())
            {
                File dir = new File(new File(System.getProperty("java.home"), "lib"), "security");
                file = new File(dir, "jssecacerts");
                if (!file.isFile())
                {
                    file = new File(dir, "cacerts");
                }
            }
               
            InputStream in = new FileInputStream(file);
            this.ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(in, passphrase.toCharArray());
            in.close();
               
            SSLContext context = SSLContext.getInstance("TLS");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            this.defaultTrustManager = (X509TrustManager)tmf.getTrustManagers()[0];
            this.tm = new SavingTrustManager(defaultTrustManager);
            context.init(null, new TrustManager[] {tm}, null);
            SSLSocketFactory factory = context.getSocketFactory();
               
            SSLSocket socket = (SSLSocket)factory.createSocket(host, port);
            socket.setSoTimeout(10000);
            try
            {
                socket.startHandshake();
                socket.close();
            }
            catch (SSLException exc)
            {
                exc.printStackTrace();
            }

            X509Certificate[] chain = tm.chain;
            if (chain == null)
            {
                return;
            }
               
               MessageDigest sha1 = MessageDigest.getInstance("SHA1");
               MessageDigest md5 = MessageDigest.getInstance("MD5");
               for (int i = 0; i < chain.length; i++)
               {
                    X509Certificate cert = chain[i];

                    boolean     trusted = false;

                    for (int j = i; j >= 0; j--)

                    {

                         if (Arrays.asList(defaultTrustManager.getAcceptedIssuers()).contains(chain[j]))

                         {

                              trusted = true;

                              break;

                         }

                    }

                    StringBuilder     sb = new StringBuilder();

                    sha1.update(cert.getEncoded());

                    md5.update(cert.getEncoded());

                    Object[]     rowData =

                    {

                         Integer.valueOf(i+1),

                              trusted,

                              trusted,

                              cert.getSubjectDN(),

                              cert.getIssuerDN(),

                              toHexString(sha1.digest()),

                              toHexString(md5.digest()),

                    };

//                    System.out.println(Arrays.toString(rowData));

                    

               }

               

          }

          catch (Exception exc)

          {

               

          }

     }

     

     void     importCertificates()

     {




          try

          {


               OutputStream out = new BufferedOutputStream(new FileOutputStream("jssecacerts"));

               ks.store(out, passphrase.toCharArray());

               out.close();

          }

          catch (Exception exc)

          {

               

          }

          

          

     }

     

     /*

* This method picks good column sizes.

* If all column heads are wider than the column's cells'

* contents, then you can just use column.sizeWidthToFit().

*/



     

     private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

     

     private static String toHexString(byte[] bytes)

     {

          StringBuilder sb = new StringBuilder(bytes.length * 3);

          for (int b : bytes)

          {

               b &= 0xff;

               sb.append(HEXDIGITS[b >> 4]);

               sb.append(HEXDIGITS[b & 15]);

               sb.append(' ');

          }

          return sb.toString();

     }

     

     private static class SavingTrustManager implements X509TrustManager

     {

          private final X509TrustManager tm;

          private X509Certificate[] chain;

          

          SavingTrustManager(X509TrustManager tm)

          {

               this.tm = tm;

          }

          

          public X509Certificate[] getAcceptedIssuers()

          {

               throw new UnsupportedOperationException();

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

     

     

     /**

     * @param args the command line arguments

     */

     public static void main(String args[])

     {

          

     }
}