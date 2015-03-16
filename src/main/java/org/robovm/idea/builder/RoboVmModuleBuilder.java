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
package org.robovm.idea.builder;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import org.jetbrains.annotations.Nullable;
import org.robovm.idea.RoboVmPlugin;
import org.robovm.templater.Templater;

import java.io.File;

/**
 * Creates all the files for a new project/module using the templater.
 * See https://android.googlesource.com/platform/tools/adt/idea/+/7ac63164a27e301d45d8640852a0bdaa6eabbad0/android/src/org/jetbrains/android/newProject/AndroidModuleBuilder.java
 */
public class RoboVmModuleBuilder extends JavaModuleBuilder {
    private String templateName;

    private String packageName;
    private String mainClassName;
    private String appName;
    private String appId;

    public RoboVmModuleBuilder(String templateName) {
        this.templateName = templateName;
    }

    public void setupRootModel(final ModifiableRootModel rootModel) throws ConfigurationException {
        // set the RoboVM SDK instead of a JDK
        for(Sdk sdk: ProjectJdkTable.getInstance().getAllJdks()) {
            if(sdk.getHomePath().equals(RoboVmPlugin.getSdkHome().getAbsolutePath())) {
                myJdk = sdk;
                break;
            }
        }
        rootModel.getModuleExtension(LanguageLevelModuleExtension.class).setLanguageLevel(LanguageLevel.HIGHEST);
        super.setupRootModel(rootModel);

        // extract the template files and setup the source
        // folders
        VirtualFile[] files = rootModel.getContentRoots();
        if (files.length > 0) {
            final VirtualFile contentRoot = files[0];
            final Project project = rootModel.getProject();
            Templater templater = new Templater(templateName);
            templater.appId(appId);
            templater.appName(appName);
            templater.executable(appName);
            templater.mainClass(mainClassName);
            templater.packageName(packageName);
            templater.buildProject(new File(contentRoot.getCanonicalPath()));
            contentRoot.refresh(false, true);

            for(ContentEntry entry: rootModel.getContentEntries()) {
                for(SourceFolder srcFolder: entry.getSourceFolders()) {
                    entry.removeSourceFolder(srcFolder);
                }
                entry.addSourceFolder(contentRoot.findFileByRelativePath("src/main/java"), false);
            }

            // FIXME add run configurations
        }
    }

//    private void addRunConfiguration(@NotNull AndroidFacet facet,
//                                     @NotNull TargetSelectionMode targetSelectionMode,
//                                     @Nullable String targetAvd) {
//        String activityClass;
//        if (isHelloAndroid()) {
//            activityClass = myPackageName + '.' + myActivityName;
//        }
//        else {
//            activityClass = null;
//        }
//        Module module = facet.getModule();
//        AndroidUtils.addRunConfiguration(module.getProject(), facet, activityClass, false, targetSelectionMode, targetAvd);
//    }

//    private static void addTestRunConfiguration(final AndroidFacet facet, @NotNull TargetSelectionMode mode, @Nullable String preferredAvd) {
//        Project project = facet.getModule().getProject();
//        RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
//        Module module = facet.getModule();
//        RunnerAndConfigurationSettings settings = runManager
//                .createRunConfiguration(module.getName(), AndroidTestRunConfigurationType.getInstance().getFactory());
//
//        AndroidTestRunConfiguration configuration = (AndroidTestRunConfiguration)settings.getConfiguration();
//        configuration.setModule(module);
//        configuration.setTargetSelectionMode(mode);
//        if (preferredAvd != null) {
//            configuration.PREFERRED_AVD = preferredAvd;
//        }
//
//        runManager.addConfiguration(settings, false);
//        runManager.setActiveConfiguration(settings);
//    }

    public void setApplicationId(String applicationId) {
        this.appId = applicationId;
    }

    public void setApplicationName(String applicationName) {
        this.appId = applicationName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setMainClassName(String mainClassName) {
        this.mainClassName = mainClassName;
    }

    public ModuleType getModuleType() {
        return StdModuleTypes.JAVA;
    }

    @Nullable
    @Override
    public String getBuilderId() {
        return this.getClass().getName();
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(WizardContext wizardContext, ModulesProvider modulesProvider) {
        return new ModuleWizardStep[] { new RoboVmModuleWizardStep(this, wizardContext, modulesProvider)};
    }
}
