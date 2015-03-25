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
package org.robovm.idea.compilation;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootsEnumerator;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Computable;
import org.apache.commons.io.FileUtils;
import org.robovm.compiler.AppCompiler;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.config.OS;
import org.robovm.compiler.target.ios.ProvisioningProfile;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.robovm.idea.RoboVmPlugin;
import org.robovm.idea.running.RoboVmRunConfiguration;
import org.robovm.idea.running.RoboVmIOSRunConfigurationSettingsEditor;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

/**
 * Registered by {@link org.robovm.idea.RoboVmPlugin} on startup. Responsible
 * for compiling an app in case there's a run configuration in the {@link com.intellij.openapi.compiler.CompileContext}
 * or if we perform an ad-hoc/IPA build from the RoboVM menu.
 */
public class RoboVmCompileTask implements CompileTask {
    @Override
    public boolean execute(CompileContext context) {
        RunConfiguration c = context.getCompileScope().getUserData(CompileStepBeforeRun.RUN_CONFIGURATION);
        if(c == null || !(c instanceof RoboVmRunConfiguration)) {
            return true;
        }
        final RoboVmRunConfiguration runConfig = (RoboVmRunConfiguration)c;

        try {
            ProgressIndicator progress = context.getProgressIndicator();
            context.getProgressIndicator().pushState();
            RoboVmPlugin.focusToolWindow();
            progress.setText("Compiling RoboVM app");

            Config.Builder builder = new Config.Builder();
            builder.logger(RoboVmPlugin.getLogger());

            // get the module we are about to compile
            ModuleManager moduleManager = ModuleManager.getInstance(runConfig.getProject());
            Module module = ApplicationManager.getApplication().runReadAction(new Computable<Module>() {
                @Override
                public Module compute() {
                    return ModuleManager.getInstance(runConfig.getProject()).findModuleByName(runConfig.getModuleName());
                }
            });
            if(module == null) {
                RoboVmPlugin.logBalloon(MessageType.ERROR, "Couldn't find Module '" + runConfig.getModuleName() + "'");
                return false;
            }
            File moduleBaseDir = new File(ModuleRootManager.getInstance(module).getContentRoots()[0].getPath());

            // load the robovm.xml file
            loadConfig(builder, moduleBaseDir, false);

            // set OS and arch
            OS os = null;
            Arch arch = null;
            if(runConfig.getTargetType() == RoboVmRunConfiguration.TargetType.Device) {
                os = OS.ios;
                arch = runConfig.getDeviceArch();
            } else if(runConfig.getTargetType() == RoboVmRunConfiguration.TargetType.Simulator) {
                os = OS.ios;
                arch = runConfig.getSimArch();
            } else {
                os = OS.getDefaultOS();
                arch = Arch.getDefaultArch();
            }
            builder.os(os);
            builder.arch(arch);

            // set build dir and install dir, pattern
            // project-basedir/robovm-build/tmp/module-name/runconfig-name/os/arch.
            // project-basedir/robovm-build/app/module-name/runconfig-name/os/arch.
            File buildDir = RoboVmPlugin.getBuildDir(module.getName(), runConfig.getName(), os, arch);
            builder.tmpDir(buildDir);
            builder.skipInstall(true);
            RoboVmPlugin.logInfo("Building executable in %s", buildDir.getAbsolutePath());
            RoboVmPlugin.logInfo("Installation of app in %s", buildDir.getAbsolutePath());

            // setup classpath entries, debug build parameters and target
            // parameters, e.g. signing identity etc.
            configureClassAndSourcepaths(context, module, builder, runConfig);
            configureTarget(builder, runConfig);

            // clean build dir
            RoboVmPlugin.logInfo("Cleaning output dir " + buildDir.getAbsolutePath());
            FileUtils.deleteDirectory(buildDir);
            buildDir.mkdirs();

            // Set the Home to be used, create the Config and AppCompiler
            Config.Home home = RoboVmPlugin.getRoboVmHome();
            if(home.isDev()) {
                builder.useDebugLibs(Boolean.getBoolean("robovm.useDebugLibs"));
                builder.dumpIntermediates(true);
            }
            builder.home(home);
            Config config = builder.build();
            AppCompiler compiler = new AppCompiler(config);
            if(progress.isCanceled()) {
                RoboVmPlugin.logInfo("Build canceled");
                return false;
            }
            progress.setFraction(0.5);

            // Start the build in a separate thread, check if
            // user canceled it.
            RoboVmCompilerThread thread = new RoboVmCompilerThread(compiler, progress);
            thread.compile();
            if(progress.isCanceled()) {
                RoboVmPlugin.logInfo("Build canceled");
                return false;
            }
            RoboVmPlugin.logInfo("Build done");

            // set the config and compiler on the run configuration so
            // it knows where to find things.
            runConfig.setConfig(config);
            runConfig.setCompiler(compiler);
        } catch(Throwable t) {
            RoboVmPlugin.logErrorThrowable("Couldn't compile app", t, false);
            return false;
        } finally {
            context.getProgressIndicator().popState();
        }
        return true;
    }

    private void configureClassAndSourcepaths(CompileContext context, Module module, Config.Builder builder, RoboVmRunConfiguration runConfig) {
        // gather the boot and user classpaths. RoboVM RT libs may be
        // specified in a Maven/Gradle build file, in which case they'll
        // turn up as order entries. We filter them out here.
        // FIXME junit needs to include test classes
        OrderEnumerator classes = ModuleRootManager.getInstance(module).orderEntries().recursively().withoutSdk().compileOnly().productionOnly();
        Set<File> classPaths = new HashSet<File>();
        Set<File> bootClassPaths = new HashSet<File>();
        for(String path: classes.getPathsList().getPathList()) {
            if(RoboVmPlugin.isSdkLibrary(path)) {
                bootClassPaths.add(new File(path));
            } else {
                classPaths.add(new File(path));
            }
        }

        // add the output dirs of all affected modules to the
        // classpath. IDEA will make the output directory
        // of a module an order entry after the first compile
        // so we add the path twice. Fixed by using a set.
        // FIXME junit needs to include test output directories
        for(Module mod: context.getCompileScope().getAffectedModules()) {
            File path = new File(CompilerPaths.getModuleOutputPath(mod, false));
            classPaths.add(path);
        }

        // set the user classpath entries
        for(File path: classPaths) {
            RoboVmPlugin.logInfo("classpath entry: %s", path.getAbsolutePath());
            builder.addClasspathEntry(path);
        }

        // check if we have a RoboVM RT jar in the classpath,
        // remove it from there and add it to the bootclasspath
        // otherwise we use the SDK assigned to the module
        builder.skipRuntimeLib(true);
        if(!bootClassPaths.isEmpty()) {
            for(File path: bootClassPaths) {
                RoboVmPlugin.logInfo("boot classpath entry: %s", path);
                if(RoboVmPlugin.isBootClasspathLibrary(path)) {
                    builder.addBootClasspathEntry(path);
                } else {
                    builder.addClasspathEntry(path);
                }
            }
        } else {
            RoboVmPlugin.logInfo("Using SDK boot classpath");
            for(File path: RoboVmPlugin.getSdkLibraries()) {
                if(RoboVmPlugin.isBootClasspathLibrary(path)) {
                    builder.addBootClasspathEntry(path);
                } else {
                    builder.addClasspathEntry(path);
                }
            }
        }

        // setup debug configuration if necessary
        if(runConfig.isDebug()) {
            // source paths of dependencies and modules
            OrderRootsEnumerator sources = ModuleRootManager.getInstance(module).orderEntries().recursively().withoutSdk().sources();
            Set<String> sourcesPaths = new HashSet<String>();
            for (String path : sources.getPathsList().getPathList()) {
                RoboVmPlugin.logInfo("source path entry: %s", path);
                sourcesPaths.add(path);
            }

            // SDK library source paths, only if not provided as a dependency
            if(bootClassPaths.isEmpty()) {
                for(File path: RoboVmPlugin.getSdkLibrarySources()) {
                    sourcesPaths.add(path.getAbsolutePath());
                }
            }

            StringBuilder b = new StringBuilder();
            for(String path: sourcesPaths) {
                b.append(path);
                b.append(":");
            }

            // set arguments for debug plugin
            runConfig.setDebugPort(findFreePort());
            builder.debug(true);
            builder.addPluginArgument("debug:sourcepath=" + b.toString());
            builder.addPluginArgument("debug:jdwpport=" + runConfig.getDebugPort());
        }
    }

    private void configureTarget(Config.Builder builder, RoboVmRunConfiguration runConfig) {
        if(runConfig.getTargetType() == RoboVmRunConfiguration.TargetType.Device) {
            // configure device build
            builder.targetType(Config.TargetType.ios);
            String signingId = runConfig.getSigningIdentity();
            String profile = runConfig.getProvisioningProfile();
            if (RoboVmIOSRunConfigurationSettingsEditor.SKIP_SIGNING.equals(signingId)) {
                builder.iosSkipSigning(true);
            } else {
                if (signingId != null && !RoboVmIOSRunConfigurationSettingsEditor.AUTO_SIGNING_IDENTITY.equals(signingId)) {
                    builder.iosSignIdentity(SigningIdentity.find(SigningIdentity.list(), signingId));
                }
                if (profile != null && !RoboVmIOSRunConfigurationSettingsEditor.AUTO_PROVISIONING_PROFILE.equals(profile)) {
                    builder.iosProvisioningProfile(ProvisioningProfile.find(ProvisioningProfile.list(), profile));
                }
            }
        } else if(runConfig.getTargetType() == RoboVmRunConfiguration.TargetType.Simulator) {
            builder.targetType(Config.TargetType.ios);
        } else if(runConfig.getTargetType() == RoboVmRunConfiguration.TargetType.Console) {
            builder.targetType(Config.TargetType.console);
        } else {
            throw new RuntimeException("Unsupported target type: " + runConfig.getTargetType());
        }
    }

    public static Config.Builder loadConfig(Config.Builder configBuilder, File projectRoot, boolean isTest) {
        try {
            configBuilder.readProjectProperties(projectRoot, isTest);
            configBuilder.readProjectConfig(projectRoot, isTest);
        } catch (IOException e) {
            RoboVmPlugin.logErrorThrowable("Couldn't load robovm.xml", e, true);
            throw new RuntimeException(e);
        }

        // Ignore classpath entries in config XML file.
        configBuilder.clearBootClasspathEntries();
        configBuilder.clearClasspathEntries();

        return configBuilder;
    }

    public int findFreePort()
    {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } catch (IOException localIOException2) {
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException localIOException4) {
                }
            }
        }
        return -1;
    }
}
