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

/**
 *
 * @author ACHR
 */
public interface PluginInterface {

    public static final int HIGHEST_PRIO = 0;
    public static final int HIGHER_PRIO = 1024;
    public static final int HIGH_PRIO = 2048;

    public static final int MEDIUM_PRIO = 3072;

    public static final int LOW_PRIO = 4096;
    public static final int LOWER_PRIO = 5120;
    public static final int LOWEST_PRIO = 6144;

    public static final int DEFAULT_PRIO = MEDIUM_PRIO;

    public void startPlugin();
    public void stopPlugin();
    public String getPluginId();
    

}
