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
package de.root1.spf.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * @author achristian
 */
public class ServiceFinder {

    private final String path = "META-INF/services/";
    private final ClassLoader classLoader;
    private URL url;

    public ServiceFinder(ClassLoader classLoader, File f) {
        if (f == null) {
            throw new IllegalArgumentException("file may not be null");
        }

        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        this.classLoader = classLoader;

        try {
            this.url = new URL("jar", "", -1, f.toURI().toURL().toString() + "!/");
        } catch (MalformedURLException e) {
        }
    }

    /**
     * Gets a list of classes that implement the given interface
     * 
     * @param interfaceClass
     * @return
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public List<Class> getServiceImplementations(Class interfaceClass) throws IOException, ClassNotFoundException {
        List<Class> implementations = new ArrayList<>();
        List<String> strings = findAllStrings(interfaceClass.getName());
        for (String className : strings) {
            Class impl = classLoader.loadClass(className);
            if (!interfaceClass.isAssignableFrom(impl)) {
                throw new ClassCastException("Class not of type: " + interfaceClass.getName());
            }
            implementations.add(impl);
        }
        return implementations;
    }
    
    private List<String> findAllStrings(String uri) throws IOException {
        String fulluri = path + uri;

        List<String> strings = new ArrayList<String>();

        List<URL> resources = getResources(fulluri);
        for (URL url : resources) {
            String string = readContents(url);
            strings.add(string);
        }
        return strings;
    }

    private String readContents(URL resource) throws IOException {
        InputStream in = resource.openStream();
        BufferedInputStream reader = null;
        StringBuffer sb = new StringBuffer();

        try {
            reader = new BufferedInputStream(in);

            int b = reader.read();
            while (b != -1) {
                sb.append((char) b);
                b = reader.read();
            }

            return sb.toString().trim();
        } finally {
            try {
                in.close();
                reader.close();
            } catch (Exception e) {
            }
        }
    }

    private List<URL> getResources(String fulluri) throws IOException {
        List<URL> resources = new ArrayList<>();
        URL resource = findResource(fulluri, url);
        if (resource != null) {
            resources.add(resource);
        }
        return resources;
    }

    private URL findResource(String resourceName, URL searchURL) {

        try {
            URL jarURL = ((JarURLConnection) searchURL.openConnection()).getJarFileURL();
            JarFile jarFile;
            JarURLConnection juc;
            try {
                juc = (JarURLConnection) new URL("jar", "", jarURL.toExternalForm() + "!/").openConnection();
                jarFile = juc.getJarFile();
            } catch (IOException e) {
                throw e;
            }

            try {
                juc = (JarURLConnection) new URL("jar", "", jarURL.toExternalForm() + "!/").openConnection();
                jarFile = juc.getJarFile();
                String entryName;
                if (searchURL.getFile().endsWith("!/")) {
                    entryName = resourceName;
                } else {
                    String file = searchURL.getFile();
                    int sepIdx = file.lastIndexOf("!/");
                    if (sepIdx == -1) {
                        // Invalid URL
                        return null;
                    }
                    sepIdx += 2;
                    StringBuffer sb = new StringBuffer(file.length() - sepIdx + resourceName.length());
                    sb.append(file.substring(sepIdx));
                    sb.append(resourceName);
                    entryName = sb.toString();
                }
                if (entryName.equals("META-INF/") && jarFile.getEntry("META-INF/MANIFEST.MF") != null) {
                    return createResourceURL(searchURL, "META-INF/MANIFEST.MF");
                }
                if (jarFile.getEntry(entryName) != null) {
                    return createResourceURL(searchURL, resourceName);
                }
            } finally {
                if (!juc.getUseCaches()) {
                    try {
                        jarFile.close();
                    } catch (Exception e) {
                    }
                }
            }

        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (SecurityException e) {
        }
        return null;
    }

    private URL createResourceURL(URL base, String name) throws MalformedURLException {
        StringBuffer sb = new StringBuffer(base.getFile().length() + name.length());
        sb.append(base.getFile());
        sb.append(name);
        String file = sb.toString();
        return new URL(base.getProtocol(), base.getHost(), base.getPort(), file, null);
    }
}
