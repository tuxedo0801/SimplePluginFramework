/**
 * This file is part of "Simple Plugin Framework".
 * 
 *  "Simple Plugin Framework" is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  "Simple Plugin Framework" is distributed in the hope that it will be useful,
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
package de.root1.spftest;

import de.root1.spf.PluginContainer;
import de.root1.spf.SimplePluginFramework;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class AppTest {

    @org.junit.Test
    public void testSpfStart() throws UnsupportedEncodingException {
        SimplePluginFramework spf = new SimplePluginFramework(new File ("./plugins"), 2000);
        spf.startLoading(true);
        List<PluginContainer> pluginContainerList = spf.getPluginContainerList();
        System.out.println(pluginContainerList);
        
//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(AppTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
}
