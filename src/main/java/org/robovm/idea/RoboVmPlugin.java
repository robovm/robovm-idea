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
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.robovm.compiler.Version;
import org.robovm.compiler.log.Logger;
import org.robovm.idea.sdk.RoboVmSdkType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
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

    public static void logInfo(String format, Object ... args) {
        log(ConsoleViewContentType.SYSTEM_OUTPUT, "[INFO] " + format, args);
    }

    public static void logError(String format, Object ... args) {
        log(ConsoleViewContentType.ERROR_OUTPUT, "[ERROR] " + format, args);
    }

    public static void logWarn(String format, Object ... args) {
        log(ConsoleViewContentType.ERROR_OUTPUT, "[WARNING] " + format, args);
    }

    public static void logDebug(String format, Object ... args) {
        log(ConsoleViewContentType.NORMAL_OUTPUT, "[DEBUG] " + format, args);
    }

    private static void log(final ConsoleViewContentType type, String format, Object ... args) {
        final String s = String.format(format, args) + "\n";
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if(consoleView != null) {
                    consoleView.print(s, type);
                } else {
                    if(type == ConsoleViewContentType.ERROR_OUTPUT) {
                        System.err.print(s);
                    } else {
                        System.out.print(s);
                    }
                    synchronized(RoboVmPlugin.class) {
                        FileWriter writer = null;
                        try {
                            writer = new FileWriter(new File(getSdkHome(), "log.txt"), true);
                            writer.write(s);
                            writer.flush();
                            writer.close();
                        } catch (IOException e) {
                        } finally {
                            IOUtils.closeQuietly(writer);
                        }
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
        if(consoleView != null) {
            consoleView.dispose();
        }
        consoleView = null;
        ToolWindowManager.getInstance(project).unregisterToolWindow(ROBOVM_TOOLWINDOW_ID);
    }

    public static void extractSdk() {
        File sdkHome = getSdkHomeBase();
        if(!sdkHome.exists()) {
            if (!sdkHome.mkdirs()) {
                logError("Couldn't create sdk dir in %s", sdkHome.getAbsolutePath());
                throw new RuntimeException("Couldn't create sdk dir in " + sdkHome.getAbsolutePath());
            }
            extractArchive("robovm-dist", sdkHome);
        } else {
            if(Version.getVersion().contains("SNAPSHOT")) {
                extractArchive("robovm-dist", sdkHome);
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
        } catch (Throwable t) {
            logError("Couldn't extract SDK to %s", dest.getAbsolutePath());
            throw new RuntimeException("Couldn't extract SDK to " + dest.getAbsolutePath(), t);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static List<File> getSdkLibraries() {
        List<File> libs = new ArrayList<File>();
        File libsDir = new File(getSdkHome(), "lib");
        for(File file: libsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        })) {
            libs.add(file);
        }
        return libs;
    }
}
