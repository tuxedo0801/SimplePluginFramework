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


/**
 *
 * @author ACHR
 */
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class DelegatingArchiveClassLoader extends ClassLoader {

    /**
     * The logger used for this class
     */
    private final static Logger LOG = LoggerFactory.getLogger(DelegatingArchiveClassLoader.class);
    private final List<ArchiveClassLoader> archiveClassLoaders = new ArrayList<>();
    private final List<String> resolvingClassLoader = Collections.synchronizedList(new ArrayList<>());
    private final List<String> noInjectionRequired = Collections.synchronizedList(new ArrayList<>());
    final private Map<String, Class> cachedClazzes = new HashMap<>();

    public DelegatingArchiveClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        LOG.trace("begin: Trying to find class: {} ; this={}", new Object[]{name, this});

        if (resolvingClassLoader.contains(name)) {
            LOG.trace("resolvingClassLoader=true for {}, throw ClassNotFoundException.", name);
            throw new ClassNotFoundException("Class " + name + " not found.");
        }

        if (name.startsWith("de.roo1.spf.")) {
            LOG.info("Filtered class: {}. Delegating directly to parent.", name);
            return super.findClass(name);
        }

        synchronized (cachedClazzes) {
            if (cachedClazzes.containsKey(name)) {
                LOG.debug("returning cached class ...");
                return cachedClazzes.get(name);
            }
        }

        synchronized (archiveClassLoaders) {

            // first, parent CL
            LOG.trace("Trying to find via parent ...");
            Class<?> clazz = null;
            try {
                clazz = super.findClass(name);
                LOG.trace("Found in parent...");
            } catch (ClassNotFoundException ex) {
                LOG.trace("Not found in parent ...");
            }

            // if this nothing was found, try all childs CL...
            if (clazz == null) {
                LOG.trace("Trying to find via plugins ...");
                addResolvingCL(name);
                clazz = findClassInPlugins(name);
                removeResolvingCL(name);
                if (clazz != null) {
                    LOG.trace("Found in plugins...");
                }
            }

            if (clazz == null) {
                LOG.trace("Class {} not found. Throwing ClassNotFoundException.", name);
                throw new ClassNotFoundException("Class " + name + " not found.");
            }


            if (LOG.isTraceEnabled())
            for (Object object : clazz.getDeclaredAnnotations()){
                Annotation annotation = (Annotation) object;
                LOG.trace("Class {} is annotated with: {}", name, annotation.annotationType().getCanonicalName());
            }

            LOG.trace("end: Clazz {} found via {}", name, clazz.getClassLoader());
            synchronized (cachedClazzes) {
                cachedClazzes.put(name, clazz);
            }
            return clazz;
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        LOG.trace("Trying to get resources: {}", name);

        List<URL> urlList = new ArrayList<URL>();

        synchronized (archiveClassLoaders) {
            if (resolvingClassLoader.contains(name)) {
                LOG.trace("resolvingClassLoader=true, return null.");
                return null;
            }
            // add all parent resources
            LOG.trace("checking parent class loader ...");
            Enumeration<URL> urls = super.getResources(name);
            int i=0;
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                LOG.trace("-> found in parent: {} @ {}", url, super.toString());
                if (!urlList.contains(url)) {
                        urlList.add(url);
                } else {
                    LOG.trace("URL {} already in list!", url);
                }
                i++;
            }
            LOG.trace("found {} in parent classloader", i);


            // add all child resources
            for (ArchiveClassLoader classLoader : archiveClassLoaders) {
                LOG.trace("checking plugin-archive class loader {}...", classLoader);
                int ii=0;
                addResolvingCL(name);
                Enumeration<URL> childUrls = classLoader.getResources(name);
                removeResolvingCL(name);
                while (childUrls.hasMoreElements()) {
                    URL url = childUrls.nextElement();
                    LOG.trace("-> found in plugin: {} @ {}", url, classLoader);
                    if (!urlList.contains(url)) {
                        urlList.add(url);
                    } else {
                        LOG.trace("URL {} already in list!", url);
                    }
                    ii++;
                }
                LOG.trace("found {} in plugin-archive's classloader", i);
            }

        }
        LOG.trace("Resource [{}] found in: {}", name, urlList);
        return Collections.enumeration(urlList);
    }

    @Override
    protected URL findResource(String name) {
        LOG.trace("Trying to find resource: {}", name);
        synchronized (archiveClassLoaders) {
            if (resolvingClassLoader.contains(name)) {
                LOG.trace("resolvingClassLoader=true, return null.");
                return null;
            }
            URL url = super.findResource(name);
            if (url == null) {
                LOG.trace("Trying to find via plugins ...");
                addResolvingCL(name);
                url = findResourceInPlugins(name);
                removeResolvingCL(name);
                if (url != null) {
                    LOG.trace("Found in plugins...");
                }
            } else {
                LOG.trace("Found in parent...");
            }
            return url;
        }
    }

    private URL findResourceInPlugins(String name) {
        synchronized (archiveClassLoaders) {
            for (ClassLoader cl : archiveClassLoaders) {
                final URL url = cl.getResource(name);
                if (url != null) {
                    LOG.trace("Found in plugin, return it!");
                    return url;
                }
                LOG.trace("Nothing found in plugin, trying next");
            }
            LOG.trace("Nothing found in plugins, returning null");
            return null;
        }
    }

    private Class<?> findClassInPlugins(String name) {
        LOG.trace("begin: Searching in plugin-archives for {} ...", name);
        synchronized (archiveClassLoaders) {

            for (ArchiveClassLoader acl : archiveClassLoaders) {
                Class<?> clazz = null;
                LOG.trace("Searching in plugin-archive {} for {}",acl,name);
                try {
                    clazz = acl.loadClass(name);
                } catch (Throwable ex) {
                    LOG.trace("{}: {}. -> Nothing found in plugin-archive for {}, trying next", new Object[]{ex.getClass(), ex.getMessage(), name});
                }
                if (clazz != null) {
                    LOG.trace("end: Found {} in plugin-archive {}, return it!", name, acl);
                    return clazz;
                }
                LOG.trace("Nothing found in plugin-archive {} for {}, trying next", acl, name);

            }
            LOG.trace("end: Nothing found in plugin-archives for {}, returning null", name);
            return null;
        }
    }

    public void addArchiveClassLoader(ArchiveClassLoader cl) {
        LOG.debug("Adding ArchiveClassLoader: {}", cl);
        if (LOG.isTraceEnabled()) {
            if (cl!=null && cl.getURLs()!=null) {
                for (URL url : cl.getURLs()) {
                    LOG.trace("CL {} has URL: {}", cl, url.toString());                
                }
            }
        }
        synchronized (archiveClassLoaders) {
            if (!archiveClassLoaders.contains(cl)) {
                archiveClassLoaders.add(cl);
            }
        }
    }

    public void removeArchiveClassLoader(ArchiveClassLoader cl) {
        LOG.debug("Removing ArchiveClassLoader: {}", cl);
        synchronized (archiveClassLoaders) {
            archiveClassLoaders.remove(cl);
        }
        Iterator<String> iterator = cachedClazzes.keySet().iterator();
        synchronized(cachedClazzes) {
            List<String> clazzesToRemoveFromCache = new ArrayList<>();
            while (iterator.hasNext()) {
                String clazzName = iterator.next();
                if (cl==cachedClazzes.get(clazzName).getClassLoader()) {
                    clazzesToRemoveFromCache.add(clazzName);
                }
            }
            for (String clazzToRemove : clazzesToRemoveFromCache) {
                cachedClazzes.remove(clazzToRemove);
                noInjectionRequired.remove(clazzToRemove);
            }
        }
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        LOG.trace("begin: Trying to find resources: {}", name);

        List<URL> urlList = new ArrayList<>();

        synchronized (archiveClassLoaders) {
            if (resolvingClassLoader.contains(name)) {
                LOG.trace("end: resolvingClassLoader=true, return null.");
                return null;
            }
            // add all parent resources
            LOG.trace("checking parent class loader ...");
            Enumeration<URL> urls = super.findResources(name);
            int i=0;
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                LOG.trace("found: {} @ {}", url, super.toString());
                urlList.add(url);
                i++;
            }
            LOG.trace("found {} in parent classloader", i);


            // add all child resources
            for (ArchiveClassLoader classLoader : archiveClassLoaders) {
                LOG.trace("checking plugin class loader {}...", classLoader);
                int ii=0;
                addResolvingCL(name);
                Enumeration<URL> childUrls = classLoader.findResources(name);
                removeResolvingCL(name);
                while (childUrls.hasMoreElements()) {
                    URL url = childUrls.nextElement();
                    LOG.trace("found: {} @ {}", url, classLoader);
                    urlList.add(url);
                    ii++;
                }
                LOG.trace("found {} in plugin classloaders", i);
            }

        }
        LOG.trace("end: Found urls: {}", urlList);
        return Collections.enumeration(urlList);
    }

    private void addResolvingCL(String name) {
        LOG.trace(">>> adding '{}'", name);
        resolvingClassLoader.add(name);
        LOG.trace(">>>>> now contains: {}",resolvingClassLoader);
    }

    private void removeResolvingCL(String name) {
        LOG.trace(">>> removing '{}'", name);
        resolvingClassLoader.remove(name);
        LOG.trace(">>>>> now contains: {}",resolvingClassLoader);
    }

    /**
     * This method returns the ArchiveClassLoader which directly relates to the
     * given className.
     *
     * @param className the classname that the classloader must be able to load
     * @return the ArchiveClassLoader that is responsible for loading the given className as part of a single plugin
     */
    public ArchiveClassLoader findRelatedArchiveClassLoader(String className){

        LOG.trace("begin: Searching related classloader for class [{}]", className);
        String path = className.replace('.', '/').concat(".class");
        LOG.trace("Searching path: [{}]", path);
        for (ArchiveClassLoader archiveClassLoader : archiveClassLoaders) {

            LOG.trace("Searching in child {}",archiveClassLoader);

                URL[] urls = archiveClassLoader.getURLs();

                for (URL url : urls) {
                    try {
                        JarFile jar = new JarFile(url.getFile());
                        ZipEntry entry = jar.getEntry(path);
                        if (entry!=null) {
                            LOG.trace("end: Found it: {}", archiveClassLoader);
                            return archiveClassLoader;
                        } 

                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

        }
        
        LOG.trace("end: No related classloader found for class [{}]", className);
        return null;
    }

}
