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
import java.io.File;
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
    private final static Logger logger = LoggerFactory.getLogger(DelegatingArchiveClassLoader.class);
    private final List<ArchiveClassLoader> archiveClassLoaders = new ArrayList<ArchiveClassLoader>();
    private final List<String> resolvingClassLoader = Collections.synchronizedList(new ArrayList<String>());
    private List<String> noInjectionRequired = Collections.synchronizedList(new ArrayList<String>());
    final private Map<String, Class> cachedClazzes = new HashMap<String, Class>();

    public DelegatingArchiveClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        logger.trace("begin: Trying to find class: {} ; this={}", new Object[]{name, this});

        if (resolvingClassLoader.contains(name)) {
            logger.trace("resolvingClassLoader=true for {}, throw ClassNotFoundException.", name);
            throw new ClassNotFoundException("Class " + name + " not found.");
        }

        if (name.startsWith("de.roo1.mas.")) {
            logger.info("Filtered class: {}. Delegating directly to parent.", name);
            return super.findClass(name);
        }

        synchronized (cachedClazzes) {
            if (cachedClazzes.containsKey(name)) {
                logger.debug("returning cached class ...");
                return cachedClazzes.get(name);
            }
        }

        synchronized (archiveClassLoaders) {

            // first, parent CL
            logger.trace("Trying to find via parent ...");
            Class<?> clazz = null;
            try {
                clazz = super.findClass(name);
                logger.trace("Found in parent...");
            } catch (ClassNotFoundException ex) {
                logger.trace("Not found in parent ...");
            }

            // if this nothing was found, try all childs CL...
            if (clazz == null) {
                logger.trace("Trying to find via childs ...");
                addResolvingCL(name);
                clazz = findClassInModule(name);
                removeResolvingCL(name);
                if (clazz != null) {
                    logger.trace("Found in child...");
                }
            }

            if (clazz == null) {
                logger.trace("Class {} not found. Throwing ClassNotFoundException.", name);
                throw new ClassNotFoundException("Class " + name + " not found.");
            }


            if (logger.isTraceEnabled())
            for (Object object : clazz.getDeclaredAnnotations()){
                Annotation annotation = (Annotation) object;
                logger.trace("Class {} is annotated with: {}", name, annotation.annotationType().getCanonicalName());
            }

            logger.trace("end: Clazz {} found via {}", name, clazz.getClassLoader());
            synchronized (cachedClazzes) {
                cachedClazzes.put(name, clazz);
            }
            return clazz;
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        logger.trace("Trying to get resources: {}", name);

        List<URL> urlList = new ArrayList<URL>();

        synchronized (archiveClassLoaders) {
            if (resolvingClassLoader.contains(name)) {
                logger.trace("resolvingClassLoader=true, return null.");
                return null;
            }
            // add all parent resources
            logger.trace("checking parent class loader ...");
            Enumeration<URL> urls = super.getResources(name);
            int i=0;
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                logger.trace("-> found in parent: {} @ {}", url, super.toString());
                if (!urlList.contains(url)) {
                        urlList.add(url);
                } else {
                    logger.trace("URL {} already in list!", url);
                }
                i++;
            }
            logger.trace("found {} in parent classloader", i);


            // add all child resources
            for (ArchiveClassLoader classLoader : archiveClassLoaders) {
                logger.trace("checking child class loader {}...", classLoader);
                int ii=0;
                addResolvingCL(name);
                Enumeration<URL> childUrls = classLoader.getResources(name);
                removeResolvingCL(name);
                while (childUrls.hasMoreElements()) {
                    URL url = childUrls.nextElement();
                    logger.trace("-> found in child: {} @ {}", url, classLoader);
                    if (!urlList.contains(url)) {
                        urlList.add(url);
                    } else {
                        logger.trace("URL {} already in list!", url);
                    }
                    ii++;
                }
                logger.trace("found {} in child classloader", i);
            }

        }
        logger.trace("Resource [{}] found in: {}", name, urlList);
        return Collections.enumeration(urlList);
    }

    @Override
    protected URL findResource(String name) {
        logger.trace("Trying to find resource: {}", name);
        synchronized (archiveClassLoaders) {
            if (resolvingClassLoader.contains(name)) {
                logger.trace("resolvingClassLoader=true, return null.");
                return null;
            }
            URL url = super.findResource(name);
            if (url == null) {
                logger.trace("Trying to find via childs ...");
                addResolvingCL(name);
                url = findModuleResource(name);
                removeResolvingCL(name);
                if (url != null) {
                    logger.trace("Found in child...");
                }
            } else {
                logger.trace("Found in parent...");
            }
            return url;
        }
    }

    private URL findModuleResource(String name) {
        synchronized (archiveClassLoaders) {
            for (ClassLoader cl : archiveClassLoaders) {
                final URL url = cl.getResource(name);
                if (url != null) {
                    logger.trace("Found in child, return it!");
                    return url;
                }
                logger.trace("Nothing found in child, trying next");
            }
            logger.trace("Nothing found in childs, returning null");
            return null;
        }
    }

    private Class<?> findClassInModule(String name) {
        logger.trace("begin: Searching in childs for {} ...", name);
        synchronized (archiveClassLoaders) {

            for (ArchiveClassLoader ucl : archiveClassLoaders) {
                Class<?> clazz = null;
                logger.trace("Searching in child {} for {}",ucl,name);
                try {
                    clazz = ucl.loadClass(name);
                } catch (Throwable ex) {
                    logger.trace("{}: {}. -> Nothing found in child for {}, trying next", new Object[]{ex.getClass(), ex.getMessage(), name});
                }
                if (clazz != null) {
                    logger.trace("end: Found {} in child {}, return it!", name, ucl);
                    return clazz;
                }
                logger.trace("Nothing found in child {} for {}, trying next", ucl, name);

            }
            logger.trace("end: Nothing found in childs for {}, returning null", name);
            return null;
        }
    }

    public void addArchiveClassLoader(ArchiveClassLoader cl) {
        logger.debug("Adding ArchiveClassLoader: {}", cl);
        if (logger.isTraceEnabled()) {
            if (cl!=null && cl.getURLs()!=null) {
                for (URL url : cl.getURLs()) {
                    logger.trace("CL {} has URL: {}", cl, url.toString());                
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
        logger.debug("Removing ArchiveClassLoader: {}", cl);
        synchronized (archiveClassLoaders) {
            archiveClassLoaders.remove(cl);
        }
        Iterator<String> iterator = cachedClazzes.keySet().iterator();
        synchronized(cachedClazzes) {
            List<String> clazzesToRemoveFromCache = new ArrayList<String>();
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
        logger.trace("begin: Trying to find resources: {}", name);

        List<URL> urlList = new ArrayList<URL>();

        synchronized (archiveClassLoaders) {
            if (resolvingClassLoader.contains(name)) {
                logger.trace("end: resolvingClassLoader=true, return null.");
                return null;
            }
            // add all parent resources
            logger.trace("checking parent class loader ...");
            Enumeration<URL> urls = super.findResources(name);
            int i=0;
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                logger.trace("found: {} @ {}", url, super.toString());
                urlList.add(url);
                i++;
            }
            logger.trace("found {} in parent classloader", i);


            // add all child resources
            for (ArchiveClassLoader classLoader : archiveClassLoaders) {
                logger.trace("checking child class loader {}...", classLoader);
                int ii=0;
                addResolvingCL(name);
                Enumeration<URL> childUrls = classLoader.findResources(name);
                removeResolvingCL(name);
                while (childUrls.hasMoreElements()) {
                    URL url = childUrls.nextElement();
                    logger.trace("found: {} @ {}", url, classLoader);
                    urlList.add(url);
                    ii++;
                }
                logger.trace("found {} in child classloaders", i);
            }

        }
        logger.trace("end: Found urls: {}", urlList);
        return Collections.enumeration(urlList);
    }

    private void addResolvingCL(String name) {
        logger.trace(">>> adding '{}'", name);
        resolvingClassLoader.add(name);
        logger.trace(">>> contains: {}",resolvingClassLoader);
    }

    private void removeResolvingCL(String name) {
        logger.trace(">>> removing '{}'", name);
        resolvingClassLoader.remove(name);
        logger.trace(">>> contains: {}",resolvingClassLoader);
    }

    /**
     * This method returns the ArchiveClassLoader which directly relates to the
     * given className.
     *
     * @param className the classname that the classloader must be able to load
     * @return the ArchiveClassLoader that is responsible for loading the given className as part of a single module
     */
    public ArchiveClassLoader findRelatedArchiveClassLoader(String className){

        logger.trace("begin: Searching related classloader for class [{}]", className);
        String path = className.replace('.', '/').concat(".class");
        logger.trace("Searching path: [{}]", path);
        for (ArchiveClassLoader archiveClassLoader : archiveClassLoaders) {

            logger.trace("Searching in child {}",archiveClassLoader);

                URL[] urls = archiveClassLoader.getURLs();

                for (URL url : urls) {
                    try {
                        JarFile jar = new JarFile(url.getFile());
                        ZipEntry entry = jar.getEntry(path);
                        if (entry!=null) {
                            logger.trace("end: Found it: {}", archiveClassLoader);
                            return archiveClassLoader;
                        } 

                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

        }
        
        logger.trace("end: No related classloader found for class [{}]", className);
        return null;
    }

}
