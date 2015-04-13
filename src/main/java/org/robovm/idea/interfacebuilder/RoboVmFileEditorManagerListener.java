package org.robovm.idea.interfacebuilder;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AppUIUtil;
import com.robovm.ibintegrator.IBIntegrator;
import org.robovm.idea.RoboVmPlugin;

import java.io.File;

/**
 * Created by badlogic on 08/04/15.
 */
public class RoboVmFileEditorManagerListener implements FileEditorManagerListener {
    final Project project;

    public RoboVmFileEditorManagerListener(Project project) {
        this.project = project;
    }

    @Override
    public void fileOpened(FileEditorManager source, final VirtualFile file) {
        if(!"storyboard".equals(file.getExtension())) {
            return;
        }
        RoboVmPlugin.logInfo("File opened: " + file.getCanonicalPath());
        Module module = null;
        for(Module m: ModuleManager.getInstance(project).getModules()) {
            if(ModuleRootManager.getInstance(m).getFileIndex().isInContent(file)) {
                module = m;
                break;
            }
        }
        if(module != null) {
            AppUIUtil.invokeOnEdt(new Runnable() {
                @Override
                public void run() {
                    FileEditorManager.getInstance(project).closeFile(file);
                }
            });

            final Module foundModule = module;
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    // this might be the first time someone opened a storyboard, do
                    // at least one compilation if the target folder doesn't exist!
                    final VirtualFile outputPath = CompilerPaths.getModuleOutputDirectory(foundModule, false);
                    if(outputPath == null || !outputPath.exists()) {
                        CompileScope scope = CompilerManager.getInstance(project).createModuleCompileScope(foundModule, true);
                        CompilerManager.getInstance(project).compile(scope, new CompileStatusNotification() {
                            @Override
                            public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
                                IBIntegratorManager.getInstance().moduleChanged(foundModule);
                                openXCodeProject(foundModule, file);
                            }
                        });
                    } else {
                        IBIntegratorManager.getInstance().moduleChanged(foundModule);
                        openXCodeProject(foundModule, file);
                    }
                }
            });
        }
    }

    private void openXCodeProject(Module module, final VirtualFile file) {
        IBIntegratorProxy proxy = IBIntegratorManager.getInstance().getProxy(module);
        if(proxy != null) {
            proxy.openProjectFile(file.getCanonicalPath());
        }
    }

    @Override
    public void fileClosed(FileEditorManager source, VirtualFile file) {
    }

    @Override
    public void selectionChanged(FileEditorManagerEvent event) {

    }
}
