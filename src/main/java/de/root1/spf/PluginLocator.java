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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class PluginLocator {

    /**
     * The logger used for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(PluginLocator.class);


    private static final PluginLocator INSTANCE = new PluginLocator();

    private static final List<PluginContainer> plugins = Collections.synchronizedList(new ArrayList<PluginContainer>());

    /*
     * Ensures singleton pattern
     */
    private PluginLocator() {
    }

    /**
     * Returns the singleton instance of <code>PluginLocator</code>
     * @return
     */
    public static PluginLocator getInstance() {
        return INSTANCE;
    }




    /**
     * Registers a plugin with the locator. If available, the MarvinModuleCast annotation is considered.
     * 
     * @param container the container that contains the plugin to register
     */
    protected void registerPlugin(PluginContainer container){
        plugins.add(container);
        logger.trace("registered plugin [{}]", container.getName());
    }

    /**
     * Unregisters a plugin with the locator. Afer this call, the plugin is no longer available for lookups
     *
     * @param container the container that contains the plugin to unregister
     */
    protected void unregisterPlugin(PluginContainer container) {
        plugins.remove(container);
        logger.trace("unregistered plugin [{}]", container.getName());
    }

    /**
     * Returns all plugin instances which are currently known/deployed stored in a new list
     *
     * @return a list of plugins
     */
    public List<Object> getAllPlugins(){
        List<Object> moduleList = new ArrayList<Object>();

        // TODO FIXME

        return moduleList;
    }

}
