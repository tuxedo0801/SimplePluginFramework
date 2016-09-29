/**
 * Marvin Application Server
 * (c) Alexander Christian, 2010
 * This source is not public domain, nor open source, nor gpl, nor lgpl.
 */
package de.root1.spftest;

import de.root1.spf.PluginContainer;
import de.root1.spf.SimplePluginFramework;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unit test for simple App.
 */
public class AppTest {

    @org.junit.Test
    public void testSpfStart() throws UnsupportedEncodingException {
        SimplePluginFramework spf = new SimplePluginFramework(new File ("./plugins"));
        spf.startLoading(true);
        List<PluginContainer> pluginContainerList = spf.getPluginContainerList();
        System.out.println(pluginContainerList);
        
        try {
            Thread.sleep(20000);
        } catch (InterruptedException ex) {
            Logger.getLogger(AppTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
