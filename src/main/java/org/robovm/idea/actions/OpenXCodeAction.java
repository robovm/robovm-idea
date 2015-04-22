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
package org.robovm.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.robovm.idea.RoboVmIcons;
import org.robovm.idea.RoboVmPlugin;
import org.robovm.idea.interfacebuilder.IBIntegratorManager;
import org.robovm.idea.interfacebuilder.IBIntegratorProxy;

import java.io.File;

/**
 * Created by badlogic on 14/04/15.
 */
public class OpenXCodeAction extends AnAction {
    private Module module;

    public OpenXCodeAction() {
        super("Open Xcode Project", "Opens the Xcode project of this RoboVM module", RoboVmIcons.ROBOVM_SMALL);
    }

    public void actionPerformed(AnActionEvent e) {
        if(module == null) {
            return;
        }

        IBIntegratorProxy proxy = IBIntegratorManager.getInstance().getProxy(module);
        if(proxy == null) {
            RoboVmPlugin.logError("Couldn't get interface builder integrator for module %s", module.getName());
        } else {
            proxy.openProject();
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = getTemplatePresentation();
        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        for(Module module: ModuleManager.getInstance(e.getProject()).getModules()) {
            if(ModuleRootManager.getInstance(module).getFileIndex().isInContent(file)) {
                this.module = module;
            }
        }
        if(module == null || !RoboVmPlugin.isRoboVmModule(module)) {
            presentation.setEnabled(false);
            presentation.setVisible(false);
            return;
        } else {
            presentation.setEnabled(true);
            presentation.setVisible(true);
        }
    }
}
