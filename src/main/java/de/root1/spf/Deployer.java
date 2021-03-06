/**
 * This file is part of "Simple Plugin Framework".
 *
 * "Simple Plugin Framework" is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * "Simple Plugin Framework" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * "Simple Plugin Framework". If not, see <http://www.gnu.org/licenses/>.
 *
 * (c) 2016, Alexander Christian <info@root1.de>
 */
package de.root1.spf;

import com.google.common.collect.ArrayListMultimap;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ACHR
 */
class Deployer implements Runnable {

    /**
     * The logger used for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(Deployer.class);

    /**
     * Path to look into for plugins to deploy/undeploy
     */
    private final File pluginFolder;

    /**
     * Path where temp files are stored
     */
    private final File tempPluginFolder;

    /**
     * Flag for stopping the deployer run() method
     */
    private boolean stopped = false;

    /**
     * Holds the list of files since last
     */
    private FileList oldFileList;

    /**
     * List of known archives and currently loaded plugins. If an archive is
     * deployed, it's added to this map. If it's undeployed, it's removed again.
     * This list also keeps the relation between the archive and it's plugins
     */
    private final ArrayListMultimap<Archive, PluginContainer> archivePluginList = ArrayListMultimap.create();

    /**
     * This classloader acts as a parent classloader for all deployed plugins.
     * All non plugin is delegated to System CL
     */
    private static final DelegatingArchiveClassLoader delegatingPluginClassLoader = new DelegatingArchiveClassLoader(ClassLoader.getSystemClassLoader());

    /**
     * flag that is set to true when initial deployment is done
     */
    private boolean initialDeploymentDone = false;

    /**
     * Monitor object, used with "waitForInitialDeploymentDone"
     */
    private final Object MONITOR = new Object();
    private final SimplePluginFramework spf;

    /*
     * Package private constructor 
     */
    Deployer(SimplePluginFramework spf, File pluginFolder) {

        this.spf = spf;
        this.pluginFolder = pluginFolder;
        this.tempPluginFolder = new File(pluginFolder, "tmp");

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.error("Uncaught exception happened during deploy in thread ["+t.getName()+"]", e);
            }
        });

        logger.info("Deployer created with deployment path: {}", pluginFolder.getAbsolutePath());
        logger.info("Cleanup temp folder: {}", tempPluginFolder.getAbsolutePath());
        boolean pathsOkay = true;
        if (!pluginFolder.exists()) {
            logger.info("Deployment folder does not exist ... ");
            try {
                pluginFolder.mkdirs();
                logger.info("... created");
            } catch (Throwable t) {
                logger.error("Error while creating deployment folder.", t);
                pathsOkay = false;
            }
        }
        if (!tempPluginFolder.exists()) {
            logger.info("Deployment temp folder does not exist ... ");
            try {
                tempPluginFolder.mkdirs();
                logger.info("... created");
            } catch (Throwable t) {
                logger.error("Error whle creating deployment temp folder.", t);
                pathsOkay = false;
            }
        }

        if (!pathsOkay) {
            logger.error("Exiting due to path problems...");
            System.exit(1);
        }
        File[] tempFiles = tempPluginFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isFile() && f.getName().endsWith(".jar");
            }
        });
        int filesDeleted = 0;
        int fileCount = 0;
        for (File file : tempFiles) {
            logger.trace("Removing: {}", file.getName());
            fileCount++;
            boolean deleted = file.delete();
            logger.trace("Successfull? -> {}", deleted);
            if (deleted) {
                filesDeleted++;
            }
        }
        logger.info("Removed {} of {} temp files.", filesDeleted, fileCount);

    }

    /**
     * Returns the central plugin-class-loader parent
     *
     * @return
     */
    public static DelegatingArchiveClassLoader getDelegatingPluginClassLoader() {
        return delegatingPluginClassLoader;
    }

    @Override
    public void run() {
        logger.debug("Deployer is running");
        while (!stopped) {

            File[] fileList = pluginFolder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isFile() && f.getName().endsWith(".jar");
                }
            });
            FileList newFileList = new FileList(fileList);

            boolean needToDeploy = false;

            if (oldFileList == null) {
                needToDeploy = true;
            } else if (!oldFileList.equals(newFileList)) {
                needToDeploy = true;
            }

            if (needToDeploy) {

                logger.info("Change in deploy folder detected!");
                logger.info("\\/------STARTING-DEPLOY-PROCESS------\\/");
                List<Archive> archivesToUndeploy = new ArrayList<Archive>();
                List<Archive> archivesToDeploy = new ArrayList<Archive>();

                logger.debug("Checking known archives. archiveModuleList.keySet.size: {}", archivePluginList.keySet().size());

                // creating a duplicate list so that modifying list while
                // iterating through it is not a problem
                List<Archive> clonedKnownArchives = new ArrayList<Archive>(archivePluginList.keySet());

                for (Archive knownArchive : clonedKnownArchives) {

                    logger.debug("Checking known archive: {}", knownArchive);

                    /*
                     * check for plugins needed to unload:
                     * Unload each plugin whose file isnt available in deploy-folder
                     *
                     */
                    if (!knownArchive.getArchiveFile().exists()) {
                        logger.debug("Undeploy for removed archive triggered: [{}]", knownArchive.getArchiveFile().getName());
                        archivesToUndeploy.add(knownArchive);
                    } else {
                        /*
                         * check already loaded plugins:
                         * is there any change in deploy folder that
                         * require the plugin to re-load?
                         *
                         * if loaded plugin and plugin in deploy folder
                         * have different parameters (last modified, size, author, version, ...)
                         * a reload is required
                         */
                        Archive deployFolderArchive = new Archive(this, knownArchive.getArchiveFile());
                        if (!deployFolderArchive.equals(knownArchive)) {
                            logger.debug("Redeploy for changed archive triggered. New archive: [{}]", deployFolderArchive.getArchiveFile().getName());
                            archivesToUndeploy.add(knownArchive);
                            archivesToDeploy.add(deployFolderArchive);
                        }
                    }
                }


                /*
                 * Check new plugins:
                 * Load all plugins found in deploy folder that are not already
                 * in knownModules list
                 */
                if (fileList != null) {
                    for (File file : fileList) {

                        if (file.isFile()) {
                            logger.debug("Checking file for possible deployment: [{}]", file.getName());

                            if (Archive.accepted(file)) {

                                Archive possibleNewArchive = new Archive(this, file);
                                if (!archivePluginList.containsKey(possibleNewArchive) && !archivesToDeploy.contains(possibleNewArchive)) {
                                    logger.info("Deploy for archive registered: [{}]", file.getName());
                                    archivesToDeploy.add(possibleNewArchive);
                                }

                            }
                        }

                    }
                }

                logger.debug("Archives to undeploy: {}", archivesToUndeploy);
                logger.debug("Archives to deploy: {}", archivesToDeploy);

                boolean modulesStopped = false;
                for (Archive archive : archivesToUndeploy) {
                    if (!modulesStopped) {
                        logger.info("\\/----------STOPPING-MODULES-STARTED------\\/");
                        modulesStopped = true;
                    }
                    undeployArchive(archive);
                }
                if (modulesStopped) {
                    logger.info("/\\----------STOPPING-MODULES-FINISHED------/\\");
                }

                /**
                 * Load all the archives to resolve dependencies etc. At this
                 * stage, it is unknown in which order the plugins must be
                 * created and started
                 */
                int toDeployCount = archivesToDeploy.size();
                int maxLoops = (int) (archivesToDeploy.size() * ((archivesToDeploy.size() / 2D) + 0.5D));
                logger.debug("Trying to deploy {} archives with max. {} loops. Archive-List: \n{}", new Object[]{archivesToDeploy.size(), maxLoops, archivesToDeploy});
                int loop = 0;
                Archive archive;
                while (!archivesToDeploy.isEmpty()) {
                    loop++;
                    if (loop > maxLoops) {
                        logger.error("Failed to load archives: {}", archivesToDeploy);
                        break;
                    }
                    archive = archivesToDeploy.remove(0);
                    logger.debug("Trying to deploy archive [{}]. Loop={}", archive.getName(), loop);
                    try {
                        List<PluginContainer> pluginContainerFromArchive = archive.getPluginContainerList();
                        for (PluginContainer plugincontainer : pluginContainerFromArchive) {
                            archivePluginList.put(archive, plugincontainer);
                            spf.doLoaded(plugincontainer);
                        }
                        logger.info("Loading archive [{}] done. Loaded {} plugins: {}", new Object[]{archive.getName(), pluginContainerFromArchive.size(), pluginContainerFromArchive});
                    } catch (Exception ex) {
                        if (logger.isDebugEnabled()) {
                            ex.printStackTrace();
                        }
                        logger.debug("Loading plugin from archive [" + archive.getName() + "] failed. Reordering list and trying again later. Error details: " + ex.getClass().getName() + ": " + ex.getMessage());
                        archive.setLastDeployError(ex);
                        archive.undeployed();
                        archivesToDeploy.add(archive);
                    }
                }

                if (loop <= maxLoops) {
                    if (toDeployCount > 0) {
                        logger.info("All archives loaded successfully.");
                    }
                } else {
                    logger.error("***** One or more plugins failed to load. *****");
                }

                logger.info("/\\------FINISHED-DEPLOY-PROCESS------/\\");

                if (!initialDeploymentDone) {
                    initialDeploymentDone = true;
                    synchronized (MONITOR) {
                        MONITOR.notifyAll();
                    }
                }

            } else {
//                logger.trace("No change in deploy folder detected");
            }

            oldFileList = newFileList;

            // loop sleep time
            try {
                Thread.sleep(spf.getDeployDelay());
            } catch (InterruptedException ex) {
            }

        }

        logger.info("Deployer stop has been signaled");

        // undeploy all plugins
        Iterator<Archive> archiveIter = archivePluginList.keySet().iterator();
        while (archiveIter.hasNext()) {
            Archive archive = archiveIter.next();
            undeployArchive(archive);
        }

        logger.info("Deployer has been stopped.");
    }

    /**
     * Undeploys a given archive. On the plugin, first invokeStopLifecycle() is
     * called, followed by invokeDestroyLifecycle(). Afterwards the GC will do
     * the rest.
     *
     * @param archive the archive to undeploy
     */
    private void undeployArchive(final Archive archive) {
        try {

            List<PluginContainer> pluginContainerList = archive.getPluginContainerList();
            logger.info("Undeploying: [{}], {} plugins ...", archive.getArchiveFile().getName(), pluginContainerList.size());
            for (final PluginContainer pluginContainer : pluginContainerList) {
                logger.info("Undeploy plugin [{}]", pluginContainer.getName());
                archivePluginList.remove(archive, pluginContainer);
                logger.debug("Undeploying: [{}@{}] invoking stop() ... ", pluginContainer.getClass().getName(), archive.getArchiveFile().getName());
                spf.doPreStop(pluginContainer);
                pluginContainer.stop();
                spf.doPostStop(pluginContainer);
                logger.debug("Undeploying: [{}@{}] invoking stop() ... *done*", pluginContainer.getClass().getName(), archive.getArchiveFile().getName());

                logger.debug("Undeploy plugin [{}] *done*", pluginContainer.getName());
            }
            archive.undeployed();
        } catch (ModuleInstantiationException ex) {
            logger.error("Can deploy archive [{}]. Error was: {}", archive.getArchiveFile().getName(), ex.getMessage());
        }

    }

    /**
     * Shutdown deployer: stops and destroyes all so far loaded plugins
     */
    void shutdown() {

        stopped = true;

    }

    void waitForInitialDeployment() {
        while (!initialDeploymentDone) {
            synchronized (MONITOR) {
                try {
                    MONITOR.wait(100);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    List<PluginContainer> getPlugins() {
        Collection<PluginContainer> values = archivePluginList.values();
        List<PluginContainer> pc = new ArrayList<>();
        pc.addAll(values);
        return pc;
    }

    File getPluginTempPath() {
        return tempPluginFolder;
    }

}
