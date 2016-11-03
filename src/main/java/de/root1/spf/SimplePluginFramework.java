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
    private DeploymentListener deploymentListener;

    public SimplePluginFramework(File pluginFolder) {
        if (!pluginFolder.exists()) {
            log.debug("Creating dir {}", pluginFolder.getAbsolutePath());
            pluginFolder.mkdirs();
        }

        // Starting deployer
        deployer = new Deployer(this, pluginFolder);

        deployerThread = new Thread(deployer);
        deployerThread.setName("PluginDeployer");
        deployerThread.setDaemon(true);
    }

    /**
     * Starting deployer thread
     * @param wait if true, wait until all plugins have been initially deployed
     */
    public void startLoading(boolean wait) {
        deployerThread.start();
        if (wait) {
            deployer.waitForInitialDeployment();
        }
    }

    public List<PluginContainer> getPluginContainerList() {
        deployer.waitForInitialDeployment();
        return deployer.getPlugins();
    }

    public void startPlugins() {
        for (PluginContainer plugin : deployer.getPlugins()) {
            try {
                log.info("Starting [{}]", plugin.getPlugin().getPluginId());
                doPreStart(plugin);
                plugin.start();
                doPostStart(plugin);
            } catch (Throwable t) {
                log.error("Cannot start plugin [" + plugin.getPlugin().getPluginId() + "]", t);
            }
        }
    }

    public void stopPlugins() {
        for (PluginContainer plugin : deployer.getPlugins()) {
            try {
                log.info("Stopping [{}]", plugin.getPlugin().getPluginId());
                doPreStop(plugin);
                plugin.stop();
                doPostStop(plugin);
            } catch (Throwable t) {
                log.error("Cannot stop plugin [" + plugin.getPlugin().getPluginId() + "]", t);
            }

        }
    }

    public void setDeploymentListener(DeploymentListener deploymentListener) {
        this.deploymentListener = deploymentListener;
    }

    DeploymentListener getDeploymentListener() {
        return deploymentListener;
    }

    
    
    void doPostStart(PluginContainer plugincontainer) {
        if (getDeploymentListener() != null) {
            try {
                getDeploymentListener().postStart(plugincontainer.getPlugin());
            } catch (Exception e) {
                log.error("Error in deploymentlistener", e);
            }
        }
    }

    void doPreStart(PluginContainer plugincontainer) {
        if (getDeploymentListener() != null) {
            try {
                getDeploymentListener().preStart(plugincontainer.getPlugin());
            } catch (Exception e) {
                log.error("Error in deploymentlistener", e);
            }
        }
    }
    
    void doPostStop(PluginContainer plugincontainer) {
        if (getDeploymentListener() != null) {
            try {
                getDeploymentListener().postStop(plugincontainer.getPlugin());
            } catch (Exception e) {
                log.error("Error in deploymentlistener", e);
            }
        }
    }

    void doPreStop(PluginContainer plugincontainer) {
        if (getDeploymentListener() != null) {
            try {
                getDeploymentListener().preStop(plugincontainer.getPlugin());
            } catch (Exception e) {
                log.error("Error in deploymentlistener", e);
            }
        }
    }

    void doLoaded(PluginContainer plugincontainer) {
        if (getDeploymentListener() != null) {
            try {
                getDeploymentListener().loaded(plugincontainer.getPlugin());
            } catch (Exception e) {
                log.error("Error in deploymentlistener", e);
            }
        }
    }
    
    

}
