package trying_Out;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * This class checks if the certificate is valid. To start an SSL/TLS connection
 * the server send a certificate to the client. The client have to check the certificate
 * and have to decide, if the server is the one the client want to connect to. This
 * is done implicite during the establishment of every SSL/TLS connection. To get
 * the cause of a failure during SSL/TLS connection establishment the validation
 * of the certificate is done manually here.
 * 
 * @author andré
 */
public class CertificateCheck {
    private String passOfCertificates = "changeit";
    private File certificateStoreFile = null;
    private KeyStore ks = null;
    private SavingTrustManager tm = null;
    private SSLSocketFactory sslSocketFactory = null;
    
    public CertificateCheck(){
        //initialize all needed stuff to retrieve certificates stored on this machine
        //  create keystore
        try {
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "Unable to load certificates of this maschine", ex);
        }
 
        //  find file where certificates are stored
        certificateStoreFile = new File("jssecacerts");
        if (certificateStoreFile.isFile() == false) {
            final char SEP = File.separatorChar;
            final File dir = new File(System.getProperty("java.home")
                    + SEP + "lib" + SEP + "security");
            certificateStoreFile = new File(dir, "jssecacerts");
            if (certificateStoreFile.isFile() == false) {
                certificateStoreFile = new File(dir, "cacerts");
            }
        }
        if(!certificateStoreFile.isFile()){
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "Unable to find file, where certificates of this maschine are"
                    + " stored", new Exception());
        }
        
        //  read file, which store certificates
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(certificateStoreFile);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "unable to create a file input stream to file at the location \""
                    + certificateStoreFile.getAbsolutePath() + "\" which store certificates.", ex);
        }
        try {
            ks.load(fis, passOfCertificates.toCharArray());
        } catch (IOException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "unable to read file \"" + certificateStoreFile.getAbsolutePath() + "\"", ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "unable to decrypt certificates stored in file \"" +
                    certificateStoreFile.getAbsolutePath() + "\"", ex);
        } catch (CertificateException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "unable to load certificates from file. maybe another password"
                    + " than the default ("+ passOfCertificates + ") password is"
                    + " used.", ex);
        }
        try {
            fis.close();
        } catch (IOException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "unable to close the file input stream to the file at the location \""
                    + certificateStoreFile.getAbsolutePath() + "\" which store certificates.", ex);
        }
        
        
        
        
        
        //initialise sslSocketFactory
        //1. create trustManagerFactory to get savingTrustManager
        TrustManagerFactory tmf = null;
        try {
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "unable to create TrustedManagerFactory with default algorthm", ex);
        }
        try {
            tmf.init(ks);
        } catch (KeyStoreException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "unable to initialize trusted manager factory", ex);
        }
        //2. create savingTrustManager to create sslContext
        X509TrustManager dtm = (X509TrustManager)tmf.getTrustManagers()[0];
        tm = new SavingTrustManager(dtm);
        //3. create sslContext to create sslSocketFactory
        SSLContext context = null;
        try {
            context = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            context.init(null, new TrustManager[]{tm}, null);
        } catch (KeyManagementException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
        //4. initialise sslSocketFactory
        sslSocketFactory = context.getSocketFactory();
    }
    
    /**
     * method can be used to test if an ssl/tls connection can be established in
     * the common way.
     * 
     * @param host hostname, to connect to
     * @param port port number of host to connect to
     * @return true if it is possile to establish an ssl/tls - connection in the
     *              common way
     *         false otherwise
     */
    public boolean hasValidCertificate(String host, int port){
        return tryToEstablishSSLConnection(null, host, port);
    }
    
    /**
     * If an ssl connection can not be established in the common way, you can check
     * with this method if the imap-server is able to communicate using ssl/tls
     * by issuing imap "STARTTLS" command.
     * 
     * @param host hostname, to connect to
     * @param port port number of host to connect to
     * @return true if it is possile to establish an ssl/tls - connection in the
     *              common way
     *         false otherwise
     */
    private boolean hasValidCertificateBySTARTTLS(String host, int port){
        Socket sock = null;
        try {
            sock = new Socket(host, port);
        } catch (UnknownHostException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            //initialize read/write streams to server
            InputStream is = sock.getInputStream();
            OutputStream os = sock.getOutputStream();
            //wait till server send something after connecting
            this.sleep(is, 1000);
            //skip message
            is.skip(is.available());
            
            
            
            //send imap CAPABILITY-command to server
            os.write("A0001 CAPABILITY\n".getBytes());
            os.flush();
            //wait till server respond to the command
            this.sleep(is, 1000);
            //read from socket and decide if it is possible to start an ssl/tls
            //connection. It is only possible if the server respond to the capability
            //command and the answer include STARTTLS.
            int toRead = is.available();
            if(toRead > 0) {
                byte[] read = new byte[toRead];
                is.read(read);
                String tmp = new String(read);
                if(!tmp.contains("STARTTLS") && !tmp.contains("starttls")){
                    //server do not except STARTTLS - command
                    return false;
                }
            } else {
                //server does not respond to CAPABILITY command
                return false;
            }
            
            
            
            //send imap STARTTLS-command to server to start a ssl/tls connection
            os.write("A0002 STARTTLS\n".getBytes());
            os.flush();
            //wait till server respond to the command
            this.sleep(is, 1000);
            //read from socket and decide if it is possible to start an ssl/tls
            //connection. It is only possible if the server is silent or the
            //server send OK.
            toRead = is.available();
            if(toRead > 0) {
                byte[] read = new byte[toRead];
                is.read(read);
                String tmp = new String(read);
                if(!tmp.contains("OK") && !tmp.contains("ok")){
                    return false;
                }
            }
            return tryToEstablishSSLConnection(sock, host, port);
        } catch (IOException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "IOException occured during direct communication with the"
                    + " imap - server", ex);
            return false;
        }
    }
    
    /**
     * method, which try to establish a ssl/tls connection
     * @param sock socket, which is the basis for an ssl/tls connection
     *             null, if a new socket should be created
     * @param host hostname, to connect to
     * @param port port number of host to connect to
     * @return true if it is possile to establish an ssl/tls - connection in the
     *              common way
     *         false otherwise
     */
    private boolean tryToEstablishSSLConnection(Socket sock, String host, int port){
        SSLSocket sslSocket = null;
        try {
            if(sock == null) {
                sslSocket = (SSLSocket)sslSocketFactory.createSocket(host, port);
            } else {
                sslSocket = (SSLSocket)sslSocketFactory.createSocket(sock, host, port, true);
            }
        } catch (IOException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "unable to create ssl/tls socket", ex);
        }
        try {
            sslSocket.setSoTimeout(10000);
        } catch (SocketException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "unable to set ssl/tls socket timeout", ex);
        }
        try {
            sslSocket.startHandshake();
            sslSocket.close();
            
            //ssl/tls connection can be established
            return true;
        } catch (IOException ex) {
            //an ssl/tls handshake does not work
            //I dont have any glue how else to create an ssl/tls - connection
            //maybe it is simply not possible.
            
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "problem during SSL/TLS handshake to \"" + host +
                    ":" + port + "\" occure. " + getCertificateError(), ex);
            return false;
        }
    }
    
    /**
     * connect to the server, try to retrieve certificate and save it to certificate
     * store.
     * 
     * @param host hostname, to connect to
     * @param port port number of host to connect to
     * @return true if a new certificate was stored in the certificate store
     *         false otherwise (valid certificate already exist or no certificate can be retrieved from the server)
     */
    public boolean importCertificate(String host, int port){
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        if(hasValidCertificate(host, port)){
            return false;
        }
        
        read this method to figure out if it is right
        
        X509Certificate[] chain = tm.getChain();
        System.out.println();
        System.out.println("Server sent " + chain.length + " certificate(s):");
        System.out.println();
        MessageDigest sha1 = null;
        MessageDigest md5 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for (int i = 0; i < chain.length; i++) {
            final X509Certificate cert = chain[i];
            System.out.println(" " + (i + 1) + " Subject " + cert.getSubjectDN());
            System.out.println("   Issuer  " + cert.getIssuerDN());
            try {
                sha1.update(cert.getEncoded());
                System.out.println("   sha1    " + toHexString(sha1.digest()));
                md5.update(cert.getEncoded());
                System.out.println("   md5     " + toHexString(md5.digest()));
                System.out.println();
            } catch (CertificateEncodingException ex) {
                Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
 
        System.out.println("Enter certificate to add to trusted keystore"
                + " or 'q' to quit: [1]");
        String line = null;
        try {
            line = reader.readLine().trim();
        } catch (IOException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        int k;
        try {
            k = (line.length() == 0) ? 0 : Integer.parseInt(line) - 1;
        } catch (final NumberFormatException e) {
            System.out.println("KeyStore not changed");
            return false;
        }
 
        final X509Certificate cert = chain[k];
        final String alias = host + "-" + (k + 1);
        try {
            ks.setCertificateEntry(alias, cert);
            OutputStream out = new FileOutputStream(certificateStoreFile);
            ks.store(out, passOfCertificates.toCharArray());
            out.close();
        }catch (CertificateException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeyStoreException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex){
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println();
        System.out.println(cert);
        System.out.println();
        System.out.println(
                "Added certificate to keystore 'cacerts' using alias '"
                        + alias + "'");
    }
    
    /**
     * Simply return a long error-message needed more than once.
     * @return error message
     */
    private String getCertificateError(){
        return "It is likely"
                    + " that on this machine there is not the right certificate"
                    + " to validate the identity of the server. Maybe the "
                    + "certificate store of this machine (imap-client) is empty"
                    + " or the server is signed by an uncommon certification "
                    + "authority CA.";
    }
    
    /**
     * This method wait till there are available data in the inputstream or the
     * timeout is reached.
     * @param is 
     * @param millisTimeout 
     */
    private void sleep(InputStream is, int millisTimeout){
        try {
            //wait till server respond to the command
            for(int i = 0; is.available() < 1 && i<millisTimeout; i++){
                this.sleep(1);
            }
        } catch (IOException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * This method simply run the Thread.sleep() - method. Only for test purpose.
     * @param milli milliseconds to sleep.
     */
    private void sleep(int milli){
        try {
            Thread.sleep(milli);
        } catch (InterruptedException ex1) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex1);
        }
    }

    private static String toHexString(byte[] bytes) {
        final char[] HEXDIGITS = "0123456789abcdef".toCharArray();
        
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (int b : bytes) {
            b &= 0xff;
            sb.append(HEXDIGITS[b >> 4]);
            sb.append(HEXDIGITS[b & 15]);
            sb.append(' ');
        }
        return sb.toString();
    }

    /*
    /**
     * untested code. method used to search a certificate store for a special
     * certificate manually.
     */
    /*
    private void searchCertificateStore(){
        /*
        X509Certificate[] chain = tm.getChain();
        if (chain == null) {
            System.out.println("Could not obtain server certificate chain");
            return false;
        }
        
        boolean anyValidCertificate = false;
        for(int i = 0; i < chain.length; i++){
            Enumeration<String> aliases = null;
            try {
                aliases = ks.aliases();
            } catch (KeyStoreException ex) {
                Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            while(aliases.hasMoreElements()){
                String alias = aliases.nextElement();
                X509Certificate cert = null;
                try {
                    cert = (X509Certificate)ks.getCertificate(alias);
                } catch (KeyStoreException ex) {
                    Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
                }
                if(cert != null){
                    if(cert.getIssuerDN().getName().equals(chain[i].getIssuerDN().getName())) {
                        anyValidCertificate = true;
                    }
                } else {
                    Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                            "problem with retriefing cert from keystore");
                }
            }
        }
        
        if(anyValidCertificate == true){
            return true;
        } else {
            return false;
        }
    }
    */
    
    //only for test purpose
    private void createCertificateStoreBackup(){
        //  find file where certificates are stored
        //first location
        File certificateStoreFileORIGINAL = new File("jssecacerts");
        if (certificateStoreFileORIGINAL.isFile()) {
            File certificateStoreFileBACKUP = new File("jssecacertsBACKUP");
            copyFromTo(certificateStoreFileORIGINAL, certificateStoreFileBACKUP);
        }
        
        
        
        //second location
        final char SEP = File.separatorChar;
        final File dir = new File(System.getProperty("java.home")
                    + SEP + "lib" + SEP + "security");
        certificateStoreFileORIGINAL = new File(dir, "jssecacerts");
        if (certificateStoreFileORIGINAL.isFile()) {
            File certificateStoreFileBACKUP = new File(dir, "jssecacertsBACKUP");
            copyFromTo(certificateStoreFileORIGINAL, certificateStoreFileBACKUP);
        }
        
        
        
        //third location
        certificateStoreFileORIGINAL = new File(dir, "cacerts");
        if (certificateStoreFileORIGINAL.isFile()) {
            File certificateStoreFileBACKUP = new File(dir, "cacertsBACKUP");
            copyFromTo(certificateStoreFileORIGINAL, certificateStoreFileBACKUP);
        }
    }
    
    //only for test purpose
    private boolean copyFromTo(File from, File to){
        if(to.exists()){
            System.out.println("\" " + to.getAbsolutePath() + "\" already exists."
                    + " Unable to create backup.");
            return false;
        } else {
            System.out.println("create backup of file \"" + from.getAbsolutePath() + "\"");
        }
        try {
            to.createNewFile();
                
            FileInputStream fis = new FileInputStream(from);
            FileOutputStream fos = new FileOutputStream(to);
                
            byte[] buf = new byte[fis.available()];
            fis.read(buf);
            fos.write(buf);
                
            fis.close();
            fos.close();
        } catch (IOException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }
    
    //only for test purpose
    public static void main(String args[]) {
        CertificateCheck cc = new CertificateCheck();
        
        String host = null;
        int port = -1;
        boolean ret;
        /*
        String host = "www.google.com";
        int port = 443;
        boolean ret = cc.hasValidCertificate(host, port);
        if(ret){
            System.out.println("a valid ssl/tls connection to \"" + host + ":" + port + "\" can be created.");
        } else {
            System.out.println("There is no certificate for \"" + host + ":" + port + "\".");
        }
        */
        /*
        host = "mail.net.t-labs.tu-berlin.de";
        port = 143;
        ret = cc.hasValidCertificate(host, port);
        if(ret){
            System.out.println("a valid ssl/tls connection to \"" + host + ":" + port + "\" can be created.");
        } else {
            System.out.println("There is no certificate for \"" + host + ":" + port + "\".");
        }
        */
        
        host = "mail.net.t-labs.tu-berlin.de";
        port = 143;
        cc.hasValidCertificateBySTARTTLS(host, port);
        
    }
    
    /*
    private void storeNewCertificates(){
        System.out.println("print aliases");
        Enumeration<String> aliases = null;
        try {
            aliases = ks.aliases();
        } catch (KeyStoreException ex) {
            Logger.getLogger(Certificate_check.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        while(aliases.hasMoreElements()){
            String alias = aliases.nextElement();
            System.out.println("Next aliases: " + alias);
        }
        System.out.println("end aliases");
        */
        
        
        
        
        /*
                final BufferedReader reader =
                new BufferedReader(new InputStreamReader(System.in));
 
        System.out.println();
        System.out.println("Server sent " + chain.length + " certificate(s):");
        System.out.println();
        final MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        for (int i = 0; i < chain.length; i++) {
            final X509Certificate cert = chain[i];
            System.out.println(" " + (i + 1) + " Subject "
                    + cert.getSubjectDN());
            System.out.println("   Issuer  " + cert.getIssuerDN());
            sha1.update(cert.getEncoded());
            System.out.println("   sha1    " + toHexString(sha1.digest()));
            md5.update(cert.getEncoded());
            System.out.println("   md5     " + toHexString(md5.digest()));
            System.out.println();
        }
 
        System.out.println("Enter certificate to add to trusted keystore"
                + " or 'q' to quit: [1]");
        final String line = reader.readLine().trim();
        int k;
        try {
            k = (line.length() == 0) ? 0 : Integer.parseInt(line) - 1;
        } catch (final NumberFormatException e) {
            System.out.println("KeyStore not changed");
            return;
        }
 
        final X509Certificate cert = chain[k];
        final String alias = host + "-" + (k + 1);
        ks.setCertificateEntry(alias, cert);
 
        final OutputStream out = new FileOutputStream(file);
        ks.store(out, passphrase);
        out.close();
 
        System.out.println();
        System.out.println(cert);
        System.out.println();
        System.out.println(
                "Added certificate to keystore 'cacerts' using alias '"
                        + alias + "'");
    }
    */
}
