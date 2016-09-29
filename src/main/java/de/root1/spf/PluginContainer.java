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

import de.root1.spf.PluginInterface;
import de.root1.spf.PluginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ACHR
 */
public class PluginContainer implements Comparable<PluginContainer> {

    /**
     * The logger used for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(PluginContainer.class);

    private PluginState state = PluginState.STOPPED;
    private final PluginInterface plugin;
    private final int priority;
    private final Archive archive;

    protected PluginContainer(Archive archive, PluginInterface plugin) {
        logger.debug("Creating ModuleContainer: archive={}, plugin.class={}", archive.getName(), plugin.getClass());
        this.plugin = plugin;
        this.archive = archive;
        /**
         * TODO fix priority
         */
        priority = 0;
    }


    public void start() {
        plugin.startPlugin();
        state = PluginState.STARTED;
    }

    public void stop() {
        plugin.stopPlugin();
        state = PluginState.STOPPED;
    }


    public PluginState getState() {
        return state;
    }

    public int getPriority() {
        return 0;
    }

    @Override
    public int compareTo(PluginContainer o) {

        // Wenn "this < argument" dann muss die Methode irgendetwas < 0 zurückgeben
        // Wenn "this = argument" dann muss die Methode 0 (irgendetwas = 0) zurückgeben
        // Wenn "this > argument" dann muss die Methode irgendetwas > 0 zurückgeben
        if (priority < o.priority) {
            return -1;
        }

        if (priority > o.priority) {
            return +1;
        }

        return 0;
    }

    public PluginInterface getPlugin() {
        return plugin;
    }

    public Archive getArchive() {
        return archive;
    }

    public String getName() {
        return plugin.getClass().getName();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("PluginContainer(").append(getName()).append("@prio=").append(getPriority()).append(")");
        return sb.toString();
    }

}
