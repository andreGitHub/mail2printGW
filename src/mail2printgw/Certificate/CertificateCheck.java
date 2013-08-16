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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
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
    private X509Certificate[] certChain = null;
    
    private void initCertificateCheck(){
        certificateStoreFile = null;
        ks = null;
        tm = null;
        sslSocketFactory = null;
        certChain = null;
        
        
        //initialize all needed stuff to retrieve certificates stored on this machine
        //find file which store certificates
        certificateStoreFile = getCertificateStoreFile();
        if(certificateStoreFile == null){
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "Unable to find file, where certificates of this maschine are"
                    + " stored", new Exception());
            certificateStoreFile = new File("temporaryCertificateFile");
        }
        
        ks = initKeystore();
        
        
        
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
    
    private KeyStore initKeystore(){
        //  create keystore
        KeyStore ret = null;
        try {
            ret = KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                    "Unable to load certificates of this maschine", ex);
        }
        
        
        
        if(certificateStoreFile.length()>0){
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
                ret.load(fis, passOfCertificates.toCharArray());
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
        } else {
            try {
                ret.load(null, passOfCertificates.toCharArray());
            } catch (IOException ex) {
                Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
            } catch (CertificateException ex) {
                Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return ret;
    }
    
    private File getCertificateStoreFile(){
        File ret = null;
        //System.setProperty("javax.net.ssl.keyStore", "falsch");
        //System.setProperty("javax.net.ssl.trustStore", "falsch");
        //check system first property
        if(System.getProperties().containsKey("javax.net.ssl.keyStore")){
            String path = (String)System.getProperties().get("javax.net.ssl.keyStore");
            //System.out.println("path of cert file: " + path);
            ret = new File(path);
        }
        //check second systemproperty if first one does not exist
        if( System.getProperties().containsKey("javax.net.ssl.trustStore")
            &&
            (
                ret == null
                ||
                (
                    ret != null
                    &&
                    !(
                        ret.exists() && ret.isFile() && ret.canRead()
                    )
                )
            )
          ){
            String path = (String)System.getProperties().get("javax.net.ssl.trustStore");
            //System.out.println("path of cert file: " + path);
            ret = new File(path);
        }
        //try to find file with certificates at default lication
        if( ret == null
            ||
            (
                ret != null && !(ret.exists() && ret.isFile() && ret.canRead())
            )
          ){
            
            //  find file where certificates are stored
            ret = new File("jssecacerts");
            if (!(ret.exists() && ret.isFile() && ret.canRead())) {
                final char SEP = File.separatorChar;
                final File dir = new File(System.getProperty("java.home")
                        + SEP + "lib" + SEP + "security");
                ret = new File(dir, "jssecacerts");
                if (!(ret.exists() && ret.isFile() && ret.canRead())) {
                    ret = new File(dir, "cacerts");
                    if(!(ret.exists() && ret.isFile() && ret.canRead())){
                        ret = null;
                    }
                }
            }
        }
        return ret;
    }
    
    public CertificateCheck(){
        initCertificateCheck();
        //System.out.println("path: " + certificateStoreFile.getAbsolutePath());
    }
    
    /**
     * This method try to establish a ssl/tls connection to the given server. If
     * a certificate for the server exist in the certificate store, the
     * connection can be established.
     * @param host host to connect to
     * @param port port to connect to
     * @return true if the connection can be established
     *         false if the connection can not be established
     */
    public boolean hasValidCertificate(String host, int port){
        certChain = null;
        if(hasValidCertificateByCommon(host, port)) {
            //System.out.println("has valid cert by common");
            return true;
        }
        if(certChain == null && hasValidCertificateBySTARTTLS(host, port)){
            //System.out.println("has valid cert by STARTTLS");
            return true;
        }
        //System.out.println("does not has valid cert");
        return false;
    }
    
    /**
     * method can be used to test if an ssl/tls connection can be established in
     * the common way.
     * 
     * @param host hostname, to connect to
     * @param port port number of host to connect to
     * @return X509Certificate-chain if it is possile to establish an ssl/tls -
     *              connection in the common way
     *         false otherwise
     */
    private boolean hasValidCertificateByCommon(String host, int port){
        return tryToEstablishSSLConnection(null, host, port);
    }
    
    /**
     * If an ssl connection can not be established in the common way, you can check
     * with this method if the imap-server is able to communicate using ssl/tls
     * by issuing imap "STARTTLS" command.
     * 
     * @param host hostname, to connect to
     * @param port port number of host to connect to
     * @return X509Certificate - chain if it is possile to establish an ssl/tls
     *              - connection in the common way
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
                    //System.out.println("server does not support STARTTLS");
                    this.certChain = null;
                    return false;
                }
            } else {
                //server does not respond to CAPABILITY command
                //System.out.println("server does not respond to CAPABILITY - message");
                this.certChain = null;
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
                if(!(tmp.contains("OK") || tmp.contains("ok"))){
                    //System.out.println("server not ready to start tls-session");
                    this.certChain = null;
                    return false;
                }
            }
            return tryToEstablishSSLConnection(sock, host, port);
        } catch (IOException ex) {
            //Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
            //        "IOException occured during direct communication with the"
            //        + " imap - server", ex);
            this.certChain = null;
            return false;
        }
    }
    
    /**
     * method, which try to establish a ssl/tls connection
     * @param sock socket, which is the basis for an ssl/tls connection
     *             null, if a new socket should be created
     * @param host hostname, to connect to
     * @param port port number of host to connect to
     * @return certificate-chain if it is possile to establish an ssl/tls -
     *              connection in the common way
     *         null otherwise
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
            this.certChain = null;
            return false;
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
            this.certChain = tm.getChain();
            return true;
        } catch (IOException ex) {
            //an ssl/tls handshake does not work
            //I dont have any glue how else to create an ssl/tls - connection
            //maybe it is simply not possible.
            
            //Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
            //        "problem during SSL/TLS handshake to \"" + host +
            //        ":" + port + "\" occure. " + getCertificateError(), ex);
            this.certChain = tm.getChain();
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
        X509Certificate[] chain = null;
        boolean connectSuccessful = hasValidCertificate(host, port);
        if(connectSuccessful){
            //a valid certificate for the given server:port exsisit in the keystore
            //a new certificate dont need to be imported - nothing else to do
            //System.out.println("1");
            return false;
        } else if(!connectSuccessful && this.certChain != null) {
            //a connection can not be established because a valid certificate does
            //not exist in certificate store. certificate need to be imported.
            //System.out.println("2");
            chain = this.certChain;
        } else {
            //a connection can not be established because a valid certificate does
            //not exist in certificate store. but there are no certificates which
            //can be imported in the certificate store.
            //System.out.println("3");
            return false;
        }
        
        
        
        //get right certificate
        X509Certificate certToStore = selectCert(chain, host);
        if(certToStore == null){
            System.out.println("4");
            return false;
        }
        //System.out.println("issuerDN: " + certToStore.getIssuerDN().getName());
        //System.out.println("subjectDN: " + certToStore.getSubjectDN().getName());
        
        
        
        //get certificate number which should be stored
        int k = 0;
        for(int j=0; k<chain.length; k++){
            if(certToStore.equals(chain[j])){
                k=j;
                break;
            }
        }
        
        
        
        final String alias = host + "-" + (k + 1);
        try {
            ks.setCertificateEntry(alias, certToStore);
            //System.out.println("path to file, where certificate is stored: " + certificateStoreFile);
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
        
        //System.out.println("Added certificate to keystore '" + certificateStoreFile.getName()
        //        + "' using alias '" + alias + "'");

        initCertificateCheck();
        return true;
    }
    
    
    
    /**
     * return a certificate, which is not already in the certificate store and fit
     * the host best
     * @param chain certificate retrieved from the host
     * @return certificate, which fit the host best
     */
    private X509Certificate selectCert(X509Certificate[] chain, String host){
        //filter certificates
        ArrayList<X509Certificate> unstoredCerts = new ArrayList<X509Certificate>();
        for(int i = 0; i<chain.length; i++){
            if(!isInCertStore(chain[i])){
                //System.out.println("new cert: " + chain[i].getIssuerDN().getName());
                unstoredCerts.add(chain[i]);
            }
        }
        
        
        
        //weight certs
        int[] weights = new int[unstoredCerts.size()];
        for(int i=0; i<unstoredCerts.size(); i++){
            weights[i] = 0;
            X509Certificate actC = unstoredCerts.get(i);
            String issuerDN = actC.getIssuerDN().getName();
            String[] issuerDNFields = issuerDN.split(",");
            String subjectDN = actC.getSubjectDN().getName();
            String[] subjectDNFields = subjectDN.split(",");
            
            //go through all issuerDN fields
            for(int j=0; j<issuerDNFields.length; j++){
                weights[i] = weights[i] + getWeight(host, issuerDNFields[j].trim().split("=")[1]);
            }
            
            //go through all subjectDN fields
            for(int j=0; j<subjectDNFields.length; j++){
                weights[i] = weights[i] + getWeight(host, subjectDNFields[j].trim().split("=")[1]);
            }
        }
        
        
        
        //search highest weight
        int high = 0;
        X509Certificate cert = null;
        for(int i=0; i<weights.length; i++){
            if(high<weights[i]){
                high = weights[i];
                cert = unstoredCerts.get(i);
            }
        }
        return cert;
    }
    
    
    
    /**
     * calculate if the value fits the hostname.
     * @param host name of host
     * @param value string which should fit the hostname
     * @return number of parts that fit
     */
    private int getWeight(String host, String value){
        if(value.contains("@")){
            String[] tmp = value.split("@");
            value = tmp[tmp.length-1];
        }
        int noOfCNDots = 0;
        int dotsInHostName = countChars(".".toCharArray()[0], host.toCharArray());
        int dotsInCN = countChars(".".toCharArray()[0], value.toCharArray());
            
        if(dotsInHostName<dotsInCN){
            noOfCNDots = dotsInHostName;
        } else {
            noOfCNDots = dotsInCN;
        }
        
        String[] partsHost = host.split("\\.");
        String[] partsValue = value.split("\\.");
        int j;
        for(j = 0; j<=noOfCNDots; j++){
            //System.out.println("host: " + host + "\nvalue: " + value + "\nnoDots: "
            //        + noOfCNDots + "\nj: " + j + "\npartsHost.length: " + partsHost.length
            //        + "\npartsValue.length: " + partsValue.length);
            if(!(partsHost[(partsHost.length-1)-j].equals(partsValue[(partsValue.length-1)-j]))){
                return j;
            }
        }
        return j;
    }
    
    
    
    /**
     * count number of character c in string str.
     * @param c character to count
     * @param str string where to count
     * @return times of the occurens of c in str
     */
    private int countChars(char c, char[] str){
        int ret = 0;
        for(int i = 0; i<str.length; i++){
            if(str[i] == c){
                ret++;
            }
        }
        return ret;
    }
    
    
    
    /**
     * method used to search a certificate store for a special
     * certificate manually.
     * 
     * @param cert certificate, which is searched in keystore.
     * @return true if the certificate is already stored in the certificate store.
     *         false otherwise
     */
    private boolean isInCertStore(X509Certificate cert){
        Enumeration<String> aliases = null;
        try {
            if(ks == null || (ks != null && ks.size() == 0)){
                //System.out.println("keystore empty");
                return false;
            }
            
            aliases = ks.aliases();
        } catch (KeyStoreException ex) {
            Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(aliases == null){
            //aliases can not be retrieved from keystore
            return false;
        }
        while(aliases.hasMoreElements()){
            String alias = aliases.nextElement();
            X509Certificate tmpCert = null;
            try {
                tmpCert = (X509Certificate)ks.getCertificate(alias);
            } catch (KeyStoreException ex) {
                Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE,
                        "certificate can not be retrieved from keystore", ex);
            }
            if(tmpCert != null &&
               tmpCert.getIssuerDN().getName().equals(cert.getIssuerDN().getName())) {
                return true;
            }
        }
        return false;
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
    
    
    
    //only for test purpose
    private static void createCertificateStoreBackup(){
        //  find file where certificates are stored
        //first location
        File certificateStoreFileORIGINAL = new File("jssecacerts");
        File certificateStoreFileBACKUP = new File("jssecacertsBACKUP");
        copyFromTo(certificateStoreFileORIGINAL, certificateStoreFileBACKUP);
        

        
        //second location
        final char SEP = File.separatorChar;
        final File dir = new File(System.getProperty("java.home")
                    + SEP + "lib" + SEP + "security");
        certificateStoreFileORIGINAL = new File(dir, "jssecacerts");
        certificateStoreFileBACKUP = new File(dir, "jssecacertsBACKUP");
        copyFromTo(certificateStoreFileORIGINAL, certificateStoreFileBACKUP);
        
        
        
        //third location
        certificateStoreFileORIGINAL = new File(dir, "cacerts");
        certificateStoreFileBACKUP = new File(dir, "cacertsBACKUP");
        copyFromTo(certificateStoreFileORIGINAL, certificateStoreFileBACKUP);
    }
    
    //only for test purpose
    private static void restoreCertificateStoreFromBackup(){
        //first location
        File certificateStoreFileBACKUP = new File("jssecacertsBACKUP");
        File certificateStoreFileORIGINAL = new File("jssecacerts");
        copyFromTo(certificateStoreFileBACKUP, certificateStoreFileORIGINAL);
     
        
        
        //second location
        final char SEP = File.separatorChar;
        final File dir = new File(System.getProperty("java.home")
                    + SEP + "lib" + SEP + "security");
        certificateStoreFileBACKUP = new File(dir, "jssecacertsBACKUP");
        certificateStoreFileORIGINAL = new File(dir, "jssecacerts");
        copyFromTo(certificateStoreFileBACKUP, certificateStoreFileORIGINAL);
        
        
        
        //third location
        certificateStoreFileBACKUP = new File(dir, "cacertsBACKUP");
        certificateStoreFileORIGINAL = new File(dir, "cacerts");
        copyFromTo(certificateStoreFileBACKUP, certificateStoreFileORIGINAL);
    }
    
    //only for test purpose
    private static boolean copyFromTo(File from, File to){
        if (from.exists() && from.isFile() && to.exists() && to.isFile() && to.canWrite()) {
            FileInputStream fis = null;
            FileOutputStream fos = null;
            try {
                fis = new FileInputStream(from);
                fos = new FileOutputStream(to);
                int len = fis.available();
                byte[] buf = new byte[len];
                fis.read(buf);
                fos.write(buf);
                fos.flush();
                
                fis.close();
                fos.close();
                return true;
            } catch (IOException ex) {
                Logger.getLogger(CertificateCheck.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        } else {
            if(from.exists() && from.isFile()){
                System.out.println("\"" + from.getAbsolutePath() + "\" can not be processed.");
                if(!to.exists() || !to.isFile()){
                    System.out.println("\"" + to.getAbsolutePath() + "\" does not "
                            + "exist. Thats the reason why the file can not be writtten. "
                            + "To solve the problem create the file and give write "
                            + "access to the user \"" + System.getProperty("user.name") + "\"");
                } else if(!to.canWrite()){
                    System.out.println("\"" + System.getProperty("user.name")
                            + "\" can not get write access to the file \""
                            + to.getAbsolutePath() + "\". Cange access writes "
                            + "to solve the problem.");
                }
            }
            return false;
        }
    }
    
    //only for test purpose
    public static void main(String args[]) {
        //createCertificateStoreBackup();
        //restoreCertificateStoreFromBackup();
        
        CertificateCheck cc = new CertificateCheck();
        
        String host = null;
        int port = -1;
        boolean ret;
        
        /*
        host = "www.google.com";
        port = 443;
        ret = cc.hasValidCertificate(host, port);
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
            while(!cc.hasValidCertificate(host, port)){
                ret = cc.importCertificate(host, port);
                if(!ret){
                    System.out.println("no certificate imported");
                } else {
                    System.out.println("certificate imported");
                }
                cc = new CertificateCheck();
            }
        }
        */
    }
}
