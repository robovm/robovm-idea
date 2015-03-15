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

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;

import javax.swing.*;
import java.awt.*;

/**
 * Custom project wizard step that lets the user specify
 * various attributes of the new project/module
 */
public class RoboVmModuleWizardStep extends ModuleWizardStep {
    private final JPanel panel;

    public RoboVmModuleWizardStep(RoboVmModuleBuilder builder, WizardContext wizardContext, ModulesProvider modulesProvider) {
        super();
        panel = new RoboVmNewModuleEditor().panel;
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void updateDataModel() {
        System.out.println("Nooo");
    }
}
