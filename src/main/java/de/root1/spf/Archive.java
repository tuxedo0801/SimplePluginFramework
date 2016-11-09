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

import de.root1.spf.Deployer;
import de.root1.spf.DelegatingArchiveClassLoader;
import de.root1.spf.ArchiveClassLoader;
import de.root1.spf.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.root1.spf.PluginInterface;
import java.util.HashSet;
import java.util.ServiceLoader;
import de.root1.spf.utils.ServiceFinder;

/**
 *
 * @author ACHR
 */
public class Archive {

    /**
     * The log used for this class
     */
    private final static Logger log = LoggerFactory.getLogger(Archive.class);

    private static final String PLUGIN_ARCHIVE_EXTENSION = "JAR";

    private File file;
    private long lastModified;
    private long length;
    private final List<PluginContainer> pluginContainerList = new ArrayList<>();
    private Throwable lastDeployError;

    private File tmpDeployFile;

    private final DelegatingArchiveClassLoader delegatingModuleClassLoader = Deployer.getDelegatingPluginClassLoader();
    private ArchiveClassLoader archiveClassLoader;
    private final Deployer deployer;
    
    public static Set<Class> loadedPluginClasses = new HashSet<>();

    public Archive(Deployer deployer, File file) {
        
        this.deployer = deployer;
        
        if (!accepted(file)) {
            throw new IllegalArgumentException("File is invalid");
        }

        this.file = file;
        lastModified = file.lastModified();
        length = file.length();

    }

    /**
     * Loads and creates an instance of the plugins. Creating the instance is
     * only done once (singleton-approach). After the first call, all other
     * calls will return the same instance. So it's not possible to load plugins
     * of an archive-instance more than one time.
     *
     * @return the singelton instance list of this archive's plugins
     * @throws ModuleInstantiationException in case of problems while creating
     * plugin instances
     */
    public synchronized List<PluginContainer> getPluginContainerList() throws ModuleInstantiationException {

        if (!pluginContainerList.isEmpty()) {
            return pluginContainerList;
        }

        // make temporary file that that is used to load the plugin so that the
        // original file can be deleted to trigger undeploy
        try {
            tmpDeployFile = File.createTempFile("ARCHIVE_" + file.getName() + "_", ".deploytmp.jar", deployer.getPluginTempPath());
            tmpDeployFile.deleteOnExit();
            log.debug("Copying [{}] to deploy temp [{}]", file, tmpDeployFile);
            Utils.copyFile(file, tmpDeployFile);
        } catch (IOException ex) {
            pluginContainerList.clear();
            throw new ModuleInstantiationException("Can't create temp file for deployment due to IOException. Error was: " + ex.getMessage());
        }

        log.trace("Loading archive via file [{}]", tmpDeployFile.getName());
        // We need to create a new classloader, and the URLClassLoader is very convinient.
        // Load the class that was specified

        String currentProcessedClass = "<not yet started to process>";
        try {

            archiveClassLoader = new ArchiveClassLoader(tmpDeployFile, delegatingModuleClassLoader);

            log.debug("ArchiveClassLoader for archive [{}]: {}", file.getName(), archiveClassLoader);
            delegatingModuleClassLoader.addArchiveClassLoader(archiveClassLoader);

            ServiceFinder finder = new ServiceFinder(archiveClassLoader, file);
            List<Class> serviceImplementations = finder.getServiceImplementations(de.root1.spf.PluginInterface.class);
            for (Class pluginImplClass : serviceImplementations) {
                pluginContainerList.add(new PluginContainer(this, (PluginInterface) pluginImplClass.newInstance()));
                log.info("Added: {}", pluginImplClass);
            }
            
            return pluginContainerList;

        } catch (NoClassDefFoundError ex) {
            pluginContainerList.clear();
            if (log.isTraceEnabled()) {
                ex.printStackTrace();
            }
            tmpDeployFile.delete();
            throw new ModuleInstantiationException("Can't load plugin class [" + currentProcessedClass + "] due to NoClassDefFoundError: " + ex.getMessage(), ex);
        } catch (MalformedURLException ex) {
            pluginContainerList.clear();
            tmpDeployFile.delete();
            throw new ModuleInstantiationException("Can't load plugin class [" + currentProcessedClass + "] due to MalformedURLException: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            pluginContainerList.clear();
            tmpDeployFile.delete();
            throw new ModuleInstantiationException("Can't load plugin class [" + currentProcessedClass + "] due to unknown exception: " + ex.getMessage(), ex);
        }
    }

    /**
     * TODO document me
     */
    public void undeployed() {
        delegatingModuleClassLoader.removeArchiveClassLoader(archiveClassLoader);
    }

    /**
     * Utility method to check if an archive would be accepted
     *
     * @param file the file to test
     * @return true, if accepted, false if not
     */
    public static boolean accepted(File file) {
        log.debug("Checking file: {}", file.getName());

        int pluginCount = 0;
        // get plugin count from archive
        try {
            ArchiveClassLoader acl = new ArchiveClassLoader(file, Deployer.getDelegatingPluginClassLoader());
            ServiceFinder finder = new ServiceFinder(acl, file);
            List<Class> serviceImplementations = finder.getServiceImplementations(de.root1.spf.PluginInterface.class);
            for (Class clazz : serviceImplementations) {
                log.info("detected: {}", clazz);
                pluginCount++;
            }
        } catch (MalformedURLException ex) {
            log.warn("Problem checking file ["+file.getAbsolutePath()+"].", ex);
            return false;
        } catch (java.util.ServiceConfigurationError err) {
            log.warn("Error checking file ["+file.getAbsolutePath()+"].", err);
            return false;
        } catch (IOException | ClassNotFoundException ex) {
            log.warn("Problem checking file ["+file.getAbsolutePath()+"].", ex);
            return false;
        }

        boolean extensionValid = file.getName().toUpperCase().endsWith("."+PLUGIN_ARCHIVE_EXTENSION);

        if (pluginCount > 0 && extensionValid) {
            return true;
        }

        return false;
    }

    /**
     * Returns the underlying <code>File</code> object of this archive
     *
     * @return the file
     */
    public File getArchiveFile() {
        return file;
    }

    /**
     * Compares the this with the provided object.
     *
     * Comparison bases on:<br>
     * <ul>
     * <li>archive's filename</li>
     * <li>last modified timestamp of archive file</li>
     * <li>size of archive file</li>
     * </ul>
     *
     * @param obj the object to compare with
     * @return true, if this equals <code>obj</code>, false if not
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Archive)) {
            return false;
        }

        Archive other = (Archive) obj;
        log.debug("Comparing: \n{}\n with\n{}", this, other);

        if (file.getName().equals(other.file.getName())
                && // do not compare plugin list, as this information is not directly available after instantiation of Archive class
                //                pluginList.size() == other.pluginList.size() && 
                lastModified == other.lastModified
                && length == other.length) {
            return true;
        }

        return false;
    }

    /**
     * Provides a hash-code for this instance. Hash code is based on:<br>
     * <ul>
     * <li>archive's filename</li>
     * <li>last modified timestamp of archive file</li>
     * <li>size of archive file</li>
     * </ul>
     *
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.file != null ? this.file.getName().hashCode() : 0);
        hash = 29 * hash + (int) (this.lastModified ^ (this.lastModified >>> 32));
        hash = 29 * hash + (int) (this.length ^ (this.length >>> 32));
        return hash;
    }

    /**
     * Returns a string representation of this object.<br>
     * String will contain:
     *
     * <ul>
     * <li>archive's filename</li>
     * <li>last modified timestamp of archive file</li>
     * <li>size of archive file</li>
     * </ul>
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Archive {\n");
        sb.append("   filename: ").append(file.getName()).append("\n");
        sb.append("   lastmodified: ").append(new Date(lastModified)).append(" (").append(lastModified).append(")\n");
        sb.append("   size: ").append(length).append(" bytes\n");
        sb.append("   lastDeployError: ").append(Utils.getStackTraceAsString(lastDeployError)).append("\n");
        sb.append("}");

        return sb.toString();
    }

    /**
     * Returns the archives filename
     *
     * @return filename of archive
     */
    public String getName() {
        return file.getName();
    }

    /**
     * @return the lastDeployError
     */
    public Throwable getLastDeployError() {
        return lastDeployError;
    }

    /**
     * @param lastDeployError the lastDeployError to set
     */
    public void setLastDeployError(Throwable lastDeployError) {
        this.lastDeployError = lastDeployError;
    }

}
