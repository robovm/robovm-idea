package org.robovm.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.Nullable;
import org.robovm.compiler.config.Arch;
import org.robovm.idea.RoboVmPlugin;

import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * Created by badlogic on 01/04/15.
 */
public class CreateIpaAction extends AnAction {
    public static final Key<IpaConfig> IPA_CONFIG_KEY = Key.create("IPA_CONFIG");

    public void actionPerformed(AnActionEvent e) {
        final CreateIpaDialog dialog = new CreateIpaDialog(e.getProject());
        dialog.show();
        if(dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            // create IPA
            IpaConfig ipaConfig = dialog.getIpaConfig();
            CompileScope scope = CompilerManager.getInstance(e.getProject()).createModuleCompileScope(ipaConfig.module, true);
            scope.putUserData(IPA_CONFIG_KEY, ipaConfig);
            CompilerManager.getInstance(e.getProject()).compile(scope, new CompileStatusNotification() {
                @Override
                public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
                    RoboVmPlugin.logInfo("IPA creation complete, %d errors, %d warnings", errors, warnings);
                }
            });
        }
    }

    public static class IpaConfig {
        private final Module module;
        private final String signingIdentity;
        private final String provisioningProfile;
        private final List<Arch> archs;
        private final File destinationDir;

        public IpaConfig(Module module, File destinationDir, String signingIdentity, String provisioningProile, List<Arch> archs) {
            this.module = module;
            this.destinationDir = destinationDir;
            this.signingIdentity = signingIdentity;
            this.provisioningProfile = provisioningProile;
            this.archs = archs;
        }

        public Module getModule() {
            return module;
        }

        public String getSigningIdentity() {
            return signingIdentity;
        }

        public String getProvisioningProfile() {
            return provisioningProfile;
        }

        public List<Arch> getArchs() {
            return archs;
        }

        public File getDestinationDir() {
            return destinationDir;
        }
    }
}
