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

import java.io.File;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class SimplePluginFramework {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Thread deployerThread;
    private final Deployer deployer;

    public SimplePluginFramework(File pluginFolder) {
        if (!pluginFolder.exists()) {
            System.out.println("Creating dir " + pluginFolder.getAbsolutePath());
            pluginFolder.mkdirs();
        }

        log.info("Testing SPF ...");
        long start = System.currentTimeMillis();
        log.debug("thread {}", Thread.currentThread());

        ServerInfo.getInstance().get();

        // Starting deployer
        deployer = new Deployer(pluginFolder);

        deployerThread = new Thread(deployer);
        deployerThread.setName("Deployer");
        deployerThread.setDaemon(true);
    }
    
    public void start(boolean wait) {
        deployerThread.start();
        if (wait) {
            deployer.waitForInitialDeployment();
        }
    }
    
    public List<PluginContainer> getPluginContainerList() {
        deployer.waitForInitialDeployment();
        return deployer.getPlugins();
    }

}