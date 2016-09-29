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
package de.root1.spf.testplugin1;

import de.root1.spf.PluginInterface;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class TestPlugin1 implements PluginInterface {

    /**
     * The logger used for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(TestPlugin1.class);

    public void startPlugin() {
        System.out.println("TestPlugin1 started");
    }

    public void stopPlugin() {
        System.out.println("TestPlugin1 stopped");
    }
    
    public String getPluginId() {
        return getClass().getName();
    }
    
   


}
