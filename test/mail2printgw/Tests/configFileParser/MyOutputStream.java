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


package mail2printgw.Tests.configFileParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eddi
 */
public class MyOutputStream extends OutputStream{
    private OutputStream stream = null;
    
    public MyOutputStream() {
        super();
    }
    
    public void setOutputStream(OutputStream os) {
        stream = os;
    }
    
    @Override
    public void write(byte[] bytes) throws IOException {
        //printError("wBytes.txt", new String(bytes) + "\n");
        if(stream!= null) {
            //printError("wBytes.txt", "x\n");
            stream.write(bytes);
            stream.flush();
        }
    }
    
    @Override
    public void write(byte[] bytes, int i, int i1) throws IOException {
        byte[] buf = Arrays.copyOfRange(bytes, i, i1);
        //printError("wBytes_I_J.txt", new String(buf) + "\n");
        if(stream!= null) {
            //printError("wBytes_I_J.txt", "x\n");
            stream.write(bytes, i, i1);
            stream.flush();
        }
    }
    
    @Override
    public void write(int i) throws IOException {
        //printError("wInt.txt", i + "\n");
        if(stream!= null) {
            //printError("wInt.txt", "x\n");
            stream.write(i);
            stream.flush();
        }
    }
    
    @Override
    public void flush() throws IOException {
        if(stream != null) {
            stream.flush();
        }
    }
    
    @Override
    public void close() {
        
    }
    
    /*
    private void printError(String fileName, String write) {
        File f = new File(fileName);
        try {
            FileWriter fw = new FileWriter(f, true);
            fw.write(write);
            fw.flush();
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(MyOutputStream.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    */
}
