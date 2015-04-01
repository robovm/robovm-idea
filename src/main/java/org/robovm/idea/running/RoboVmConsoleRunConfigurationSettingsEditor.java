package org.robovm.idea.running;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.target.ios.DeviceType;
import org.robovm.compiler.target.ios.ProvisioningProfile;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.robovm.idea.RoboVmPlugin;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by badlogic on 25/03/15.
 */
public class RoboVmConsoleRunConfigurationSettingsEditor extends SettingsEditor<RoboVmRunConfiguration> {
    private JComboBox module;
    private JPanel panel;
    private JTextArea args;
    private JTextField workingDir;
    private JButton browseButton;

    @Override
    protected void resetEditorFrom(RoboVmRunConfiguration config) {
        populateControls(config);
    }

    @Override
    protected void applyEditorTo(RoboVmRunConfiguration config) throws ConfigurationException {
        config.setModuleName(module.getSelectedItem().toString());
        config.setTargetType(RoboVmRunConfiguration.TargetType.Console);
        config.setArguments(args.getText());
        config.setWorkingDir(workingDir.getText());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return panel;
    }

    private void populateControls(final RoboVmRunConfiguration config) {
        // populate with RoboVM Sdk modules
        this.module.removeAllItems();
        for(Module module: RoboVmPlugin.getRoboVmModules(config.getProject())) {
            this.module.addItem(module.getName());
            if(module.getName().equals(config.getModuleName())) {
                this.module.setSelectedIndex(this.module.getItemCount() - 1);
            }
        }

        this.args.setText(config.getArguments());
        String dir = config.getWorkingDir();
        if(dir == null || dir.trim().isEmpty()) {
            dir = config.getProject().getBasePath();
        }
        this.workingDir.setText(dir);
        this.browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileChooserDialog fileChooser = FileChooserFactory.getInstance()
                        .createFileChooser(new FileChooserDescriptor(true, false, false, false, false, false) {
                            @Override
                            public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                                return file.isDirectory();
                            }
                            @Override
                            public boolean isFileSelectable(VirtualFile file) {
                                return file.isDirectory();
                            }
                        }, null, panel);
                VirtualFile[] dir = fileChooser.choose(config.getProject());
                if(dir != null && dir.length > 0) {
                    workingDir.setText(dir[0].getCanonicalPath());
                }
            }
        });
    }
}
