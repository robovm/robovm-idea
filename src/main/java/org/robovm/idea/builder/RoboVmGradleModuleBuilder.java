package org.robovm.idea.builder;

import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.projectWizard.ProjectSettingsStep;
import com.intellij.ide.util.EditorHelper;
import com.intellij.ide.util.newProjectWizard.AddSupportForFrameworksPanel;
import com.intellij.ide.util.newProjectWizard.impl.FrameworkSupportModelBase;
import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.project.ProjectId;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder;
import com.intellij.openapi.externalSystem.service.project.wizard.ExternalModuleSettingsStep;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.containers.ContainerUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder;
import org.jetbrains.plugins.gradle.frameworkSupport.GradleFrameworkSupportProvider;
import org.jetbrains.plugins.gradle.frameworkSupport.GradleJavaFrameworkSupportProvider;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleFrameworksWizardStep;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleModuleBuilder;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleModuleWizardStep;
import org.jetbrains.plugins.gradle.service.settings.GradleProjectSettingsControl;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.robovm.compiler.Version;
import org.robovm.idea.RoboVmPlugin;
import org.robovm.idea.interfacebuilder.IBIntegratorManager;
import org.robovm.idea.sdk.RoboVmSdkType;
import org.robovm.templater.Templater;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by badlogic on 17/06/15.
 */
public class RoboVmGradleModuleBuilder extends AbstractExternalModuleBuilder<GradleProjectSettings> {
    public static final String ROBOVM_VERSION_PLACEHOLDER = "__robovmVersion__";
    public static final String PACKAGE_NAME_PLACEHOLDER = "__packageName__";
    public static final String APP_NAME_PLACEHOLDER = "__appName__";

    private String templateName;
    private String packageName;
    private String mainClassName;
    private String appName;
    private String appId;

    private static final Logger LOG = Logger.getInstance(RoboVmGradleModuleBuilder.class);

    private static final String TEMPLATE_GRADLE_SETTINGS = "Gradle Settings.gradle";
    private static final String TEMPLATE_GRADLE_SETTINGS_MERGE = "Gradle Settings merge.gradle";
    private static final String TEMPLATE_GRADLE_BUILD_WITH_WRAPPER = "Gradle Build Script with wrapper.gradle";
    private static final String DEFAULT_TEMPLATE_GRADLE_BUILD = "Gradle Build Script.gradle";

    private static final String TEMPLATE_ATTRIBUTE_PROJECT_NAME = "PROJECT_NAME";
    private static final String TEMPLATE_ATTRIBUTE_MODULE_PATH = "MODULE_PATH";
    private static final String TEMPLATE_ATTRIBUTE_MODULE_FLAT_DIR = "MODULE_FLAT_DIR";
    private static final String TEMPLATE_ATTRIBUTE_MODULE_NAME = "MODULE_NAME";
    private static final String TEMPLATE_ATTRIBUTE_MODULE_GROUP = "MODULE_GROUP";
    private static final String TEMPLATE_ATTRIBUTE_MODULE_VERSION = "MODULE_VERSION";
    private static final String TEMPLATE_ATTRIBUTE_GRADLE_VERSION = "GRADLE_VERSION";
    private static final Key<BuildScriptDataBuilder> BUILD_SCRIPT_DATA =
            Key.create("gradle.module.buildScriptData");

    private WizardContext myWizardContext;

    @Nullable
    private ProjectData myParentProject;
    private boolean myInheritGroupId;
    private boolean myInheritVersion;
    private ProjectId myProjectId;
    private String rootProjectPath;

    public RoboVmGradleModuleBuilder(String templateName) {
        super(GradleConstants.SYSTEM_ID, new GradleProjectSettings());
        this.templateName = templateName;
    }

    @Override
    public void setupRootModel(final ModifiableRootModel modifiableRootModel) throws ConfigurationException {
        String contentEntryPath = getContentEntryPath();
        if (StringUtil.isEmpty(contentEntryPath)) {
            return;
        }
        File contentRootDir = new File(contentEntryPath);
        FileUtilRt.createDirectory(contentRootDir);
        LocalFileSystem fileSystem = LocalFileSystem.getInstance();
        VirtualFile modelContentRootDir = fileSystem.refreshAndFindFileByIoFile(contentRootDir);
        if (modelContentRootDir == null) {
            return;
        }

        modifiableRootModel.addContentEntry(modelContentRootDir);
        // todo this should be moved to generic ModuleBuilder
        if (myJdk != null){
            modifiableRootModel.setSdk(myJdk);
        } else {
            modifiableRootModel.inheritSdk();
        }

        final Project project = modifiableRootModel.getProject();
        if (myParentProject != null) {
            rootProjectPath = myParentProject.getLinkedExternalProjectPath();
        }
        else {
            rootProjectPath =
                    FileUtil.toCanonicalPath(myWizardContext.isCreatingNewProject() ? project.getBasePath() : modelContentRootDir.getPath());
        }
        assert rootProjectPath != null;

        // we set the compiler output path to be inside the robovm-build dir
        File outputDir = RoboVmPlugin.getModuleClassesDir(getContentEntryPath());

        myJdk = RoboVmPlugin.getSdk();
        if(myJdk == null) {
            myJdk = RoboVmSdkType.findBestJdk();
        }

        // set a project jdk if none is set
        ProjectRootManager manager = ProjectRootManager.getInstance(modifiableRootModel.getProject());
        if(manager.getProjectSdk() == null) {
            manager.setProjectSdk(RoboVmSdkType.findBestJdk());
        }

        // extract the template files and setup the source
        // folders
        final VirtualFile contentRoot = LocalFileSystem.getInstance().findFileByIoFile(new File(getContentEntryPath()));

        Templater templater = new Templater(templateName);
        templater.appId(appId);
        templater.appName(appName);
        templater.executable(appName);
        templater.mainClass(packageName + "." + mainClassName);
        templater.packageName(packageName);
        templater.buildProject(new File(contentRoot.getCanonicalPath()));
        RoboVmPlugin.logInfo(modifiableRootModel.getProject(), "Project created in %s", contentRoot.getCanonicalPath());

        File buildFile = null;
        try {
            String template = IOUtils.toString(RoboVmModuleBuilder.class.getResource("/build.gradle"), "UTF-8");
            template = template.replaceAll(ROBOVM_VERSION_PLACEHOLDER, Version.getVersion());
            buildFile = new File(contentRoot.getCanonicalPath() + "/build.gradle");
            FileUtils.write(buildFile, template);
        } catch(Throwable t) {
            throw new ConfigurationException("Couldn't create build.gradle file");
        }

        if (buildFile != null) {
            modifiableRootModel.getModule().putUserData(
                    BUILD_SCRIPT_DATA, new BuildScriptDataBuilder(LocalFileSystem.getInstance().findFileByIoFile(buildFile)));
        }
    }

    @Override
    protected void setupModule(Module module) throws ConfigurationException {
        super.setupModule(module);
        assert rootProjectPath != null;

        VirtualFile buildScriptFile = null;
        final BuildScriptDataBuilder buildScriptDataBuilder = getBuildScriptData(module);
        try {
            if (buildScriptDataBuilder != null) {
                buildScriptFile = buildScriptDataBuilder.getBuildScriptFile();
                final String text = buildScriptDataBuilder.build();
                appendToFile(buildScriptFile, "\n" + text);
            }
        }
        catch (IOException e) {
            LOG.warn("Unexpected exception on applying frameworks templates", e);
        }

        final Project project = module.getProject();
        if (myWizardContext.isCreatingNewProject()) {
            getExternalProjectSettings().setExternalProjectPath(rootProjectPath);
            AbstractExternalSystemSettings settings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID);
            project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, Boolean.TRUE);
            //noinspection unchecked
            settings.linkProject(getExternalProjectSettings());
        }
        else {
            FileDocumentManager.getInstance().saveAllDocuments();
            final GradleProjectSettings gradleProjectSettings = getExternalProjectSettings();
            final VirtualFile finalBuildScriptFile = buildScriptFile;
            Runnable runnable = new Runnable() {
                public void run() {
                    if (myParentProject == null) {
                        gradleProjectSettings.setExternalProjectPath(rootProjectPath);
                        AbstractExternalSystemSettings settings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID);
                        //noinspection unchecked
                        settings.linkProject(gradleProjectSettings);
                    }

                    ExternalSystemUtil.refreshProject(
                            project, GradleConstants.SYSTEM_ID, rootProjectPath, false,
                            ProgressExecutionMode.IN_BACKGROUND_ASYNC);

                    final PsiFile psiFile;
                    if (finalBuildScriptFile != null) {
                        psiFile = PsiManager.getInstance(project).findFile(finalBuildScriptFile);
                        if (psiFile != null) {
                            EditorHelper.openInEditor(psiFile);
                        }
                    }
                }
            };

            // execute when current dialog is closed
            ExternalSystemUtil.invokeLater(project, ModalityState.NON_MODAL, runnable);
        }
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
        myWizardContext = wizardContext;
        return new ModuleWizardStep[]{
                new RoboVmGradleModuleWizardStep(this, wizardContext, modulesProvider),
                new ExternalModuleSettingsStep<GradleProjectSettings>(
                        wizardContext, this, new GradleProjectSettingsControl(getExternalProjectSettings()))
        };
    }

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        final GradleFrameworksWizardStep step = new GradleFrameworksWizardStep(context, this);
        Disposer.register(parentDisposable, step);
        return step;
    }

    @Override
    public boolean isSuitableSdkType(SdkTypeId sdk) {
        return sdk instanceof JavaSdkType;
    }

    @Override
    public String getParentGroup() {
        return JavaModuleType.BUILD_TOOLS_GROUP;
    }

    @Override
    public int getWeight() {
        return JavaModuleBuilder.BUILD_SYSTEM_WEIGHT;
    }

    @Override
    public ModuleType getModuleType() {
        return StdModuleTypes.JAVA;
    }

    @Nullable
    private static VirtualFile getOrCreateExternalProjectConfigFile(@NotNull String parent, @NotNull String fileName) {
        File file = new File(parent, fileName);
        FileUtilRt.createIfNotExists(file);
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
    }

    public void setParentProject(@Nullable ProjectData parentProject) {
        myParentProject = parentProject;
    }

    public boolean isInheritGroupId() {
        return myInheritGroupId;
    }

    public void setInheritGroupId(boolean inheritGroupId) {
        myInheritGroupId = inheritGroupId;
    }

    public boolean isInheritVersion() {
        return myInheritVersion;
    }

    public void setInheritVersion(boolean inheritVersion) {
        myInheritVersion = inheritVersion;
    }

    public ProjectId getProjectId() {
        return myProjectId;
    }

    public void setProjectId(@NotNull ProjectId projectId) {
        myProjectId = projectId;
    }

    @Nullable
    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        if (settingsStep instanceof ProjectSettingsStep) {
            final ProjectSettingsStep projectSettingsStep = (ProjectSettingsStep)settingsStep;
            if (myProjectId != null) {
                final JTextField moduleNameField = settingsStep.getModuleNameField();
                if (moduleNameField != null) {
                    moduleNameField.setText(myProjectId.getArtifactId());
                }
                projectSettingsStep.setModuleName(myProjectId.getArtifactId());
            }
            projectSettingsStep.bindModuleSettings();
        }
        return super.modifySettingsStep(settingsStep);
    }

    public static void appendToFile(@NotNull VirtualFile file, @NotNull String text) throws IOException {
        String lineSeparator = LoadTextUtil.detectLineSeparator(file, true);
        if (lineSeparator == null) {
            lineSeparator = CodeStyleSettingsManager.getSettings(ProjectManagerEx.getInstanceEx().getDefaultProject()).getLineSeparator();
        }
        final String existingText = StringUtil.trimTrailing(VfsUtilCore.loadText(file));
        String content = (StringUtil.isNotEmpty(existingText) ? existingText + lineSeparator : "") +
                StringUtil.convertLineSeparators(text, lineSeparator);
        VfsUtil.saveText(file, content);
    }

    @Nullable
    public static BuildScriptDataBuilder getBuildScriptData(@Nullable Module module) {
        return module == null ? null : module.getUserData(BUILD_SCRIPT_DATA);
    }

    public void setApplicationId(String applicationId) {
        this.appId = applicationId;
    }

    public void setApplicationName(String applicationName) {
        this.appName = applicationName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setMainClassName(String mainClassName) {
        this.mainClassName = mainClassName;
    }

    public class RoboVmGradleModuleWizardStep extends ModuleWizardStep {
        private final RoboVmNewModuleEditor editor;
        private final RoboVmGradleModuleBuilder builder;

        public RoboVmGradleModuleWizardStep(RoboVmGradleModuleBuilder builder, WizardContext wizardContext, ModulesProvider modulesProvider) {
            super();
            this.editor = new RoboVmNewModuleEditor();
            this.builder = builder;
        }

        @Override
        public JComponent getComponent() {
            return editor.panel;
        }

        @Override
        public void updateDataModel() {
            builder.setApplicationId(editor.appId.getText());
            builder.setApplicationName(editor.appName.getText());
            builder.setMainClassName(editor.mainClassName.getText());
            builder.setPackageName(editor.packageName.getText());
        }
    }

    public class GradleFrameworksWizardStep extends ModuleWizardStep implements Disposable {

        private JPanel myPanel;
        private final AddSupportForFrameworksPanel myFrameworksPanel;
        private JPanel myFrameworksPanelPlaceholder;
        private JPanel myOptionsPanel;
        @SuppressWarnings("unused") private JBLabel myFrameworksLabel;

        public GradleFrameworksWizardStep(WizardContext context, final RoboVmGradleModuleBuilder builder) {

            Project project = context.getProject();
            final LibrariesContainer container = LibrariesContainerFactory.createContainer(context.getProject());
            FrameworkSupportModelBase model = new FrameworkSupportModelBase(project, null, container) {
                @NotNull
                @Override
                public String getBaseDirectoryForLibrariesPath() {
                    return StringUtil.notNullize(builder.getContentEntryPath());
                }
            };

            myFrameworksPanel =
                    new AddSupportForFrameworksPanel(Collections.<FrameworkSupportInModuleProvider>emptyList(), model, true, null);

            List<FrameworkSupportInModuleProvider> providers = ContainerUtil.newArrayList();
            Collections.addAll(providers, GradleFrameworkSupportProvider.EP_NAME.getExtensions());

            myFrameworksPanel.setProviders(providers, Collections.<String>emptySet(), Collections.singleton(GradleJavaFrameworkSupportProvider.ID));
            Disposer.register(this, myFrameworksPanel);
            myFrameworksPanelPlaceholder.add(myFrameworksPanel.getMainPanel());

            ModuleBuilder.ModuleConfigurationUpdater configurationUpdater = new ModuleBuilder.ModuleConfigurationUpdater() {
                @Override
                public void update(@NotNull Module module, @NotNull ModifiableRootModel rootModel) {
                    myFrameworksPanel.addSupport(module, rootModel);
                }
            };
            builder.addModuleConfigurationUpdater(configurationUpdater);

            ((CardLayout)myOptionsPanel.getLayout()).show(myOptionsPanel, "frameworks card");
        }

        @Override
        public JComponent getComponent() {
            return myPanel;
        }

        @Override
        public void updateDataModel() {
        }

        @Override
        public void dispose() {
        }

        @Override
        public void disposeUIResources() {
            Disposer.dispose(this);
        }
    }

}
