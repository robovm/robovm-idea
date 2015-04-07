/*
 * Copyright (C) 2015 RoboVM AB
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html>.
 */
package org.robovm.idea;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.robovm.compiler.Version;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.config.OS;
import org.robovm.compiler.config.Resource;
import org.robovm.compiler.log.Logger;
import org.robovm.idea.compilation.RoboVmCompileTask;
import org.robovm.idea.sdk.RoboVmSdkType;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Provides util for the other components of the plugin such
 * as logging.
 */
public class RoboVmPlugin {
    private static final String ROBOVM_TOOLWINDOW_ID = "RoboVM";
    static volatile Project project;
    static volatile ConsoleView consoleView;
    static volatile ToolWindow toolWindow;
    static final List<UnprintedMessage> unprintedMessages = new ArrayList<UnprintedMessage>();

    static class UnprintedMessage {
        final String string;
        final ConsoleViewContentType type;

        public UnprintedMessage(String string, ConsoleViewContentType type) {
            this.string = string;
            this.type = type;
        }
    }

    public static void logBalloon(final MessageType messageType, final String message) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if (project != null) {
                    ToolWindowManager.getInstance(project).notifyByBalloon(ROBOVM_TOOLWINDOW_ID, MessageType.ERROR, message);
                }
            }
        });
    }

    public static void logInfo(String format, Object... args) {
        log(ConsoleViewContentType.SYSTEM_OUTPUT, "[INFO] " + format, args);
    }

    public static void logError(String format, Object... args) {
        log(ConsoleViewContentType.ERROR_OUTPUT, "[ERROR] " + format, args);
    }

    public static void logErrorThrowable(String s, Throwable t, boolean showBalloon) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        t.printStackTrace(writer);
        log(ConsoleViewContentType.ERROR_OUTPUT, "[ERROR] %s\n%s", s, stringWriter.toString());
        logBalloon(MessageType.ERROR, s);
    }

    public static void logWarn(String format, Object... args) {
        log(ConsoleViewContentType.ERROR_OUTPUT, "[WARNING] " + format, args);
    }

    public static void logDebug(String format, Object... args) {
        log(ConsoleViewContentType.NORMAL_OUTPUT, "[DEBUG] " + format, args);
    }

    private static void log(final ConsoleViewContentType type, String format, Object... args) {
        final String s = String.format(format, args) + "\n";
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if (consoleView != null) {
                    for (UnprintedMessage unprinted : unprintedMessages) {
                        consoleView.print(unprinted.string, unprinted.type);
                    }
                    unprintedMessages.clear();
                    consoleView.print(s, type);
                } else {
                    unprintedMessages.add(new UnprintedMessage(s, type));
                    if (type == ConsoleViewContentType.ERROR_OUTPUT) {
                        System.err.print(s);
                    } else {
                        System.out.print(s);
                    }
                }
            }
        });
    }

    public static Logger getLogger() {
        return new Logger() {
            @Override
            public void debug(String s, Object... objects) {
                logDebug(s, objects);
            }

            @Override
            public void info(String s, Object... objects) {
                logInfo(s, objects);
            }

            @Override
            public void warn(String s, Object... objects) {
                logWarn(s, objects);
            }

            @Override
            public void error(String s, Object... objects) {
                logError(s, objects);
            }
        };
    }

    public static void initializeProject(final Project project) {
        // store the project, we may need it later
        RoboVmPlugin.project = project;

        // setup a compile task if there isn't one yet
        boolean found = false;
        for (CompileTask task : CompilerManager.getInstance(project).getAfterTasks()) {
            if (task instanceof RoboVmCompileTask) {
                found = true;
                break;
            }
        }
        if (!found) {
            CompilerManager.getInstance(project).addAfterTask(new RoboVmCompileTask());
        }

        // initialize our tool window to which we
        // log all messages
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if (project.isDisposed()) {
                    return;
                }
                toolWindow = ToolWindowManager.getInstance(project).registerToolWindow(ROBOVM_TOOLWINDOW_ID, false, ToolWindowAnchor.BOTTOM, project, true);
                consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
                Content content = toolWindow.getContentManager().getFactory().createContent(consoleView.getComponent(), "RoboVM Console", true);
                toolWindow.getContentManager().addContent(content);
                toolWindow.setIcon(RoboVmIcons.ROBOVM_SMALL);
                logInfo("RoboVM plugin initialized");
            }
        });
    }

    public static void unregisterProject(Project project) {
        if (consoleView != null) {
            consoleView.dispose();
        }
        consoleView = null;
        ToolWindowManager.getInstance(project).unregisterToolWindow(ROBOVM_TOOLWINDOW_ID);
    }

    public static void extractSdk() {
        File sdkHome = getSdkHomeBase();
        if (!sdkHome.exists()) {
            if (!sdkHome.mkdirs()) {
                logError("Couldn't create sdk dir in %s", sdkHome.getAbsolutePath());
                throw new RuntimeException("Couldn't create sdk dir in " + sdkHome.getAbsolutePath());
            }
            extractArchive("robovm-dist", sdkHome);
        } else {
            if (Version.getVersion().contains("SNAPSHOT")) {
                // extractArchive("robovm-dist", sdkHome);
            }
        }

        // create an SDK if it doesn't exist yet
        RoboVmSdkType.createSdkIfNotExists();
    }

    public static File getSdkHome() {
        File sdkHome = new File(getSdkHomeBase(), "robovm-" + Version.getVersion());
        return sdkHome;
    }

    public static File getSdkHomeBase() {
        return new File(System.getProperty("user.home"), ".robovm-sdks");
    }

    private static void extractArchive(String archive, File dest) {
        archive = "/" + archive;
        TarArchiveInputStream in = null;
        try {
            in = new TarArchiveInputStream(new GZIPInputStream(RoboVmPlugin.class.getResourceAsStream(archive)));
            ArchiveEntry entry = null;
            while ((entry = in.getNextEntry()) != null) {
                File f = new File(dest, entry.getName());
                if (entry.isDirectory()) {
                    f.mkdirs();
                } else {
                    f.getParentFile().mkdirs();
                    OutputStream out = null;
                    try {
                        out = new FileOutputStream(f);
                        IOUtils.copy(in, out);
                    } finally {
                        IOUtils.closeQuietly(out);
                    }
                }
            }
            logInfo("Installed RoboVM SDK %s to %s", Version.getVersion(), dest.getAbsolutePath());

            // make all files in bin executable
            for (File file : new File(getSdkHome(), "bin").listFiles()) {
                file.setExecutable(true);
            }
        } catch (Throwable t) {
            logError("Couldn't extract SDK to %s", dest.getAbsolutePath());
            throw new RuntimeException("Couldn't extract SDK to " + dest.getAbsolutePath(), t);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * @return all sdk runtime libraries and their source jars
     */
    public static List<File> getSdkLibraries() {
        List<File> libs = new ArrayList<File>();
        File libsDir = new File(getSdkHome(), "lib");
        for (File file : libsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        })) {
            libs.add(file);
        }
        return libs;
    }

    /**
     * @return the source jars of all runtime libraries
     */
    public static List<File> getSdkLibrariesWithoutSources() {
        List<File> libs = getSdkLibraries();
        Iterator<File> iter = libs.iterator();
        while(iter.hasNext()) {
            File file = iter.next();
            if(file.getName().endsWith("-sources.jar")) {
                iter.remove();
            }
        }
        return libs;
    }

    /**
     * @return the source jars of all runtime libraries
     */
    public static List<File> getSdkLibrarySources() {
        List<File> libs = getSdkLibraries();
        Iterator<File> iter = libs.iterator();
        while(iter.hasNext()) {
            File file = iter.next();
            if(!file.getName().endsWith("-sources.jar")) {
                iter.remove();
            }
        }
        return libs;
    }

    public static Config.Home getRoboVmHome() {
        try {
            return Config.Home.find();
        } catch(Throwable t) {
            return new Config.Home(getSdkHome());
        }
    }

    public static Collection<Module> getRoboVmModules(Project project) {
        List<Module> validModules = new ArrayList<Module>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (isRoboVmModule(module)) {
                validModules.add(module);
            }
        }
        return validModules;
    }

    public static boolean isRoboVmModule(Module module) {
        // HACK! to identify if the module uses a robovm sdk
        if (ModuleRootManager.getInstance(module).getSdk().getSdkType().getName().toLowerCase().contains("robovm")) {
            return true;
        }

        // check if there's any RoboVM RT libs in the classpath
        OrderEnumerator classes = ModuleRootManager.getInstance(module).orderEntries().recursively().withoutSdk().compileOnly();
        for (String path : classes.getPathsList().getPathList()) {
            if (isSdkLibrary(path)) {
                return true;
            }
        }

        return false;
    }

    public static void focusToolWindow() {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                toolWindow.show(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        });
    }

    public static File getBuildDir(String name, String moduleName, OS os, Arch arch) {
        if (project != null) {
            File buildDir = new File(project.getBasePath(), "robovm-build/tmp/" + moduleName + "/" + os + "/" + arch);
            if (!buildDir.exists()) {
                if (!buildDir.mkdirs()) {
                    throw new RuntimeException("Couldn't create build dir '" + buildDir.getAbsolutePath() + "'");
                }
            }
            return buildDir;
        } else {
            throw new RuntimeException("No project opened");
        }
    }

    public static File getInstallDir(String name, String moduleName, OS os, Arch arch) {
        if (project != null) {
            File buildDir = new File(project.getBasePath(), "robovm-build/app/" + moduleName + "/" + os + "/" + arch);
            if (!buildDir.exists()) {
                if (!buildDir.mkdirs()) {
                    throw new RuntimeException("Couldn't create build dir '" + buildDir.getAbsolutePath() + "'");
                }
            }
            return buildDir;
        } else {
            throw new RuntimeException("No project opened");
        }
    }

    public static File getLogDir(Module module) {
        if (project != null) {
            File logDir = new File(project.getBasePath(), "robovm-build/logs/");
            if (!logDir.exists()) {
                if (!logDir.mkdirs()) {
                    throw new RuntimeException("Couldn't create log dir '" + logDir.getAbsolutePath() + "'");
                }
            }
            return logDir;
        } else {
            throw new RuntimeException("No project opened");
        }
    }

    public static File getModuleBuildDir(Module module) {
        if (module.getProject() != null) {
            File buildDir = new File(module.getProject().getBasePath(), "robovm-build/tmp/" + module.getName());
            if (!buildDir.exists()) {
                if (!buildDir.mkdirs()) {
                    throw new RuntimeException("Couldn't create build dir '" + buildDir.getAbsolutePath() + "'");
                }
            }
            return buildDir;
        } else {
            throw new RuntimeException("No project opened");
        }
    }

    public static Set<File> getModuleResourcePaths(Module module) {
        try {
            File projectRoot = new File(module.getModuleFilePath());
            Config.Builder configBuilder = new Config.Builder();
            configBuilder.home(RoboVmPlugin.getRoboVmHome());
            configBuilder.addClasspathEntry(new File(".")); // Fake a classpath to make Config happy
            configBuilder.skipLinking(true);
            RoboVmCompileTask.loadConfig(configBuilder, projectRoot, false);
            Config config = configBuilder.build();
            Set<File> paths = new HashSet<>();
            for (Resource r : config.getResources()) {
                if (r.getPath() != null) {
                    if (r.getPath().exists() && r.getPath().isDirectory()) {
                        paths.add(r.getPath());
                    }
                } else if (r.getDirectory() != null) {
                    if (r.getDirectory().exists() && r.getDirectory().isDirectory()) {
                        paths.add(r.getDirectory());
                    }
                }
            }
            return paths;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isSdkLibrary(String path) {
        String name = new File(path).getName();

        return name.startsWith("robovm-rt") ||
                name.startsWith("robovm-objc") ||
                name.startsWith("robovm-cocoatouch") ||
                name.startsWith("robovm-cacerts");
    }

    public static boolean isBootClasspathLibrary(File path) {
        return path.getName().startsWith("robovm-rt");
    }
}
