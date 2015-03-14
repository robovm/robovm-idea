package org.robovm.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.robovm.lm.LicenseManager;
import com.robovm.lm.ui.GUI;
import org.robovm.compiler.AppCompiler;

import java.io.IOException;

/**
 * Created by badlogic on 12/03/15.
 */
public class OpenLicenseManagerAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        // this is how we get the classpath of a module
        // ModuleRootManager.getInstance(ModuleManager.getInstance(project).getModules()[3]).orderEntries().classes().getRoots();

        // this is how we get the source paths
        // ModuleRootManager.getInstance(ModuleManager.getInstance(project).getModules()[3]).orderEntries().getSourcePathsList()

        // need to recursively get the dependencies as well, both classes and sources
        // should be simple

        // attach debugger https://devnet.jetbrains.com/message/5522503#5522503

        Project project = e.getProject();
        System.out.println(project);
        //GUI.main(new String[0]);

        try {
            LicenseManager.forkUI();
        } catch (IOException e1) {

        }
    }
}
