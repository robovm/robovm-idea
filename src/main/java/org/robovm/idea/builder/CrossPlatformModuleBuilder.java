package org.robovm.idea.builder;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FileUtils;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.robovm.compiler.Version;
import org.robovm.idea.components.setupwizard.AndroidSetupDialog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


/**
 * Configures the module builder for the cross platform wizard.
 */
public class CrossPlatformModuleBuilder extends RoboVmModuleBuilder {
    public CrossPlatformModuleBuilder(String templateName) {
        super(templateName, "ios");
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(WizardContext wizardContext, ModulesProvider modulesProvider) {
        RoboVmModuleWizardStep wizardStep = new RoboVmModuleWizardStep(this, wizardContext, modulesProvider);
        wizardStep.disableBuildSystem();
        RoboVmAndroidModuleWizardStep androidStep = new RoboVmAndroidModuleWizardStep(this, wizardContext, modulesProvider);
        return new ModuleWizardStep[] { androidStep, wizardStep };
    }

    @Override
    protected void applyBuildSystem(final Project project, final ModifiableRootModel model, final VirtualFile contentRoot) {
        try {
            final File buildFile = new File(contentRoot.getCanonicalPath() + "/" + this.robovmDir + "/build.gradle");
            String template = FileUtils.readFileToString(buildFile, StandardCharsets.UTF_8);
            template = template.replaceAll(ROBOVM_VERSION_PLACEHOLDER, Version.getVersion());
            FileUtils.write(buildFile, template);

            final File localProps = new File(contentRoot.getCanonicalPath() + "/local.properties");
            try (FileWriter writer = new FileWriter(localProps)) {
                writer.write("sdk.dir=" + AndroidSetupDialog.getAndroidSdkLocation());
            }

            GradleProjectSettings gradleSettings = new GradleProjectSettings();
            gradleSettings.setDistributionType(DistributionType.DEFAULT_WRAPPED);
            gradleSettings.setExternalProjectPath(this.getContentEntryPath() + "/" + this.robovmDir);
            AbstractExternalSystemSettings settings = ExternalSystemApiUtil.getSettings(model.getProject(), GradleConstants.SYSTEM_ID);
            project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, Boolean.TRUE);
            settings.linkProject(gradleSettings);

            FileDocumentManager.getInstance().saveAllDocuments();
            ImportSpecBuilder builder = new ImportSpecBuilder(model.getProject(), GradleConstants.SYSTEM_ID);
            builder.forceWhenUptodate(true);
            ExternalSystemUtil.refreshProjects(builder);
        } catch (IOException e) {
            // nothing to do here, can't log or throw an exception
        }
    }
}
