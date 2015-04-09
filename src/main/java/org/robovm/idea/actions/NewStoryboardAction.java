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

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.robovm.idea.RoboVmIcons;
import org.robovm.idea.RoboVmPlugin;
import org.robovm.idea.interfacebuilder.IBIntegratorManager;
import org.robovm.idea.interfacebuilder.IBIntegratorProxy;

import javax.sql.CommonDataSource;
import java.io.File;

/**
 * Created by badlogic on 08/04/15.
 */
public class NewStoryboardAction extends AnAction {
    private Module module;

    public NewStoryboardAction() {
        super("Storyboard", "An iOS Storyboard", RoboVmIcons.ROBOVM_SMALL);
    }

    public void actionPerformed(AnActionEvent e) {
        if(module == null) {
            return;
        }

        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        final NewStoryboardDialog dialog = new NewStoryboardDialog(e.getProject(), new File(file.getCanonicalPath()));
        dialog.show();
        if(dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            IBIntegratorProxy proxy = IBIntegratorManager.getInstance().getProxy(module);
            if(proxy == null) {
                RoboVmPlugin.logError("Couldn't get interface builder integrator for module %s", module.getName());
            } else {
                File resourceDir = new File(file.getCanonicalPath());
                proxy.newIOSStoryboard(dialog.getStoryboardName(), resourceDir);
                VirtualFile vsfFile = VfsUtil.findFileByIoFile(resourceDir, true);
                vsfFile.refresh(false, true);
            }
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = getTemplatePresentation();
        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        if(file == null || !file.isDirectory() || (module = RoboVmPlugin.isRoboVmModuleResourcePath(file)) == null) {
            presentation.setEnabled(false);
            presentation.setVisible(false);
            return;
        } else {
            presentation.setEnabled(true);
            presentation.setVisible(true);
        }

        System.out.println();
    }
}
