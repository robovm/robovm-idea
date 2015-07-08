package org.robovm.idea.builder;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import org.robovm.idea.components.setupwizard.AndroidSetupDialog;

import javax.swing.*;

/**
 * Created by badlogic on 08/07/15.
 */
public class RoboVmAndroidModuleWizardStep extends ModuleWizardStep {
    private final RoboVmAndroidSdkEditor editor;

    public RoboVmAndroidModuleWizardStep(RoboVmModuleBuilder builder, WizardContext wizardContext, ModulesProvider modulesProvider) {
        super();
        this.editor = new RoboVmAndroidSdkEditor();
    }

    @Override
    public JComponent getComponent() {
        return editor.panel;
    }

    @Override
    public void updateDataModel() {
    }

    @Override
    public boolean validate() throws ConfigurationException {
        editor.validate();
        return AndroidSetupDialog.isAndroidSdkSetup();
    }
}
