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
package org.robovm.idea.interfacebuilder;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import org.robovm.idea.RoboVmPlugin;

import java.io.File;
import java.util.*;

/**
 * Created by badlogic on 07/04/15.
 */
public class IBIntegratorManager {
    private static boolean hasIBIntegrator;
    private static IBIntegratorManager instance;

    private Map<String, IBIntegratorProxy> daemons = new HashMap<String, IBIntegratorProxy>();

    static {
        try {
            IBIntegratorProxy.getIBIntegratorClass();
            hasIBIntegrator = true;
        } catch (Throwable t) {
            hasIBIntegrator = false;
            RoboVmPlugin.logWarn(t.getMessage());
        }
    }

    public static IBIntegratorManager getInstance() {
        if (instance == null) {
            instance = new IBIntegratorManager();
        }
        return instance;
    }

    public void moduleChanged(Module module) {
        if (!hasIBIntegrator || !System.getProperty("os.name").toLowerCase().contains("mac os x")) {
            return;
        }

        IBIntegratorProxy proxy = daemons.get(module.getName());
        if(proxy == null) {
            try {
                File buildDir = RoboVmPlugin.getModuleBuildDir(module);
                RoboVmPlugin.logInfo("Starting Interface Builder integrator daemon for module %s", module.getName());
                proxy = new IBIntegratorProxy(RoboVmPlugin.getRoboVmHome(), RoboVmPlugin.getLogger(), module.getName(), buildDir);
                proxy.start();
                daemons.put(module.getName(), proxy);
            } catch (Throwable e) {
                RoboVmPlugin.logWarn("Failed to start Interface Builder integrator for module " + module.getName() + ": " + e.getMessage());
            }
        }

        if(proxy != null) {
            // set the classpath, excluding module output paths
            OrderEnumerator classes = ModuleRootManager.getInstance(module).orderEntries().recursively().withoutSdk().compileOnly().productionOnly();
            List<File> classPaths = new ArrayList<File>();
            for(String path: classes.getPathsList().getPathList()) {
                classPaths.add(new File(path));
            }
            proxy.setClasspath(classPaths);

            // set the source paths
            Set<File> moduleOutputPaths = new HashSet<File>();
            for(Module dep: ModuleRootManager.getInstance(module).getDependencies(false)) {
                moduleOutputPaths.add(new File(CompilerPaths.getModuleOutputPath(dep, false)));
            }
            moduleOutputPaths.add(new File(CompilerPaths.getModuleOutputPath(module, false)));
            proxy.setSourceFolders(moduleOutputPaths);

            proxy.setResourceFolders(RoboVmPlugin.getModuleResourcePaths(module));
        }
    }

    public void removeDaemon(Module module) {
        if (!hasIBIntegrator || !System.getProperty("os.name").toLowerCase().contains("mac os x")) {
            return;
        }
        RoboVmPlugin.logInfo("Stopping Interface Builder integrator daemon for module %s", module.getName());
    }

    public void removeAllDaemons() {
        if (!hasIBIntegrator || !System.getProperty("os.name").toLowerCase().contains("mac os x")) {
            return;
        }
        for(IBIntegratorProxy daemon: daemons.values()) {
            daemon.shutDown();
        }
        RoboVmPlugin.logInfo("Stopping all Interface Builder integrator daemons");
    }

    public IBIntegratorProxy getProxy(Module module) {
        return daemons.get(module.getName());
    }
}
