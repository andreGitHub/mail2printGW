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

import mail2printgw.Tests.configFileParser.MyOutputStream;
import java.io.OutputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.StreamHandler;

/**
 *
 * @author eddi
 */
public class MyStreamHandler extends StreamHandler{
    public MyStreamHandler(OutputStream out) {
        MyOutputStream mos = new MyOutputStream();
        mos.setOutputStream(out);
        setOutputStream(mos);
    }
}
