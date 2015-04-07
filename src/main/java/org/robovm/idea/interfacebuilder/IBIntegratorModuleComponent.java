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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.robovm.idea.RoboVmPlugin;

public class IBIntegratorModuleComponent implements ModuleComponent {
    private final Module module;
    private final Project project;

    public IBIntegratorModuleComponent(Module module, Project project) {
        this.module = module;
        this.project = project;
    }

    @Override
    public void projectOpened() {

    }

    @Override
    public void projectClosed() {
        IBIntegratorManager.getInstance().removeAllDaemons();
    }

    @Override
    public void moduleAdded() {
        if(!RoboVmPlugin.isRoboVmModule(module)) {
            return;
        }

        IBIntegratorManager.getInstance().moduleChanged(module);
    }

    @Override
    public void initComponent() {

    }

    @Override
    public void disposeComponent() {

    }

    @NotNull
    @Override
    public String getComponentName() {
        return "org.robovm.idea.interfacebuilder.IBIntegratorModuleComponent";
    }
}
