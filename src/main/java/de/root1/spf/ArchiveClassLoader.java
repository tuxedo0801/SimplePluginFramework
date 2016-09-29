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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ClassLoader for temp archive files
 * @author achristian
 */
public class ArchiveClassLoader extends URLClassLoader {
    
    private String name;
    private final static Pattern archiveTmpFilePattern = Pattern.compile("ARCHIVE_.+_\\d+?\\.deploytmp\\.jar");
    private final File f;
    
    public ArchiveClassLoader(File f, ClassLoader parent) throws MalformedURLException {
        super(new URL[]{f.toURI().toURL()}, parent);
        this.f = f;
        
        name = f.getName();
        
        Matcher m = archiveTmpFilePattern.matcher(name);
        if (!m.matches()) {
            name = f.toString();
        }
    }

    @Override
    public String toString() {
        return "ArchiveClassLoader{" + "archive=" + name + '}';
    }
    
    
    
   
}
