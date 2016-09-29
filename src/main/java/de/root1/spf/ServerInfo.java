/**
 * This file is part of "Simple Plugin Framework".
 * 
 *  Foobar is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  Foobar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with "Simple Plugin Framework".  
 *  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  (c) 2016, Alexander Christian <info@root1.de>
 */
package de.root1.spf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ACHR
 */
public class ServerInfo {

    /**
     * The logger used for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(ServerInfo.class);

    private static final ServerInfo INSTANCE = new ServerInfo();

    private ServerInfo() {
    }



    public static ServerInfo getInstance() {
        return INSTANCE;
    }

    public void get() {
        logger.info("Java VM vendor: {}", System.getProperty("java.vm.vendor"));
        logger.info("Java VM version: {}", System.getProperty("java.vm.version"));
        logger.info("Java VM name: {}", System.getProperty("java.vm.name"));
        logger.info("Java runtime version: {}", System.getProperty("java.runtime.version"));
        logger.info("Java runtime name: {}", System.getProperty("java.runtime.name"));
        logger.info("OS-System: {} {}, {}", new Object[]{System.getProperty("os.name"),System.getProperty("os.version"), System.getProperty("os.arch")});

    }

    /**
     * Checks whether the server is running under linux or not ...
     * @return true, if running on linux, false if not
     */
    public boolean isLinux(){
        return System.getProperty("os.name").equalsIgnoreCase("Linux");
    }

}
