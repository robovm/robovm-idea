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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.target.ios.DeviceType;
import org.robovm.compiler.target.ios.ProvisioningProfile;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.robovm.idea.RoboVmPlugin;
import org.robovm.idea.running.RoboVmRunConfiguration;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by badlogic on 01/04/15.
 */
public class CreateIpaDialog extends DialogWrapper {
    private static final String ARCHS_ALL = "All - 32-bit (thumbv7) + 64-bit (arm64)";
    private static final String ARCHS_32BIT = "32-bit (thumbv7)";
    private static final Object ARCHS_64BIT = "64-bit (arm64)";
    private JPanel panel;
    private JComboBox archs;
    private JComboBox signingIdentity;
    private JComboBox provisioningProfile;
    private JButton browseButton;
    private JTextField destinationDir;
    private JComboBox module;
    private Project project;

    protected CreateIpaDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        init();
        setTitle("Create IPA");
        populateControls();
    }

    private void populateControls() {
        for(Module module: RoboVmPlugin.getRoboVmModules(project)) {
            this.module.addItem(module.getName());
        }

        // populate signing identities
        for(SigningIdentity identity: SigningIdentity.list()) {
            signingIdentity.addItem(identity.getName());
        }

        // populate provisioning profiles
        for(ProvisioningProfile profile: ProvisioningProfile.list()) {
            provisioningProfile.addItem(profile.getName());
        }

        // populate architectures
        archs.addItem(ARCHS_ALL);
        archs.addItem(ARCHS_32BIT);
        archs.addItem(ARCHS_64BIT);

        browseButton.addActionListener(new ActionListener() {
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
                VirtualFile[] dir = fileChooser.choose(project);
                if(dir != null && dir.length > 0) {
                    destinationDir.setText(dir[0].getCanonicalPath());
                }
            }
        });
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if(this.module.getSelectedIndex() == -1) {
            return new ValidationInfo("No RoboVM module selected");
        }
        if(this.destinationDir.getText() == null || this.destinationDir.getText().length() == 0) {
            return new ValidationInfo("Specify a destination directory");
        }
        File destDir = new File(this.destinationDir.getText());
        if(!destDir.exists()) {
            return new ValidationInfo("Destination directory does not exist");
        }
        if(!destDir.isDirectory()) {
            return new ValidationInfo("Destination is not a directory");
        }
        if(signingIdentity.getItemCount() == 0) {
            return new ValidationInfo("No signing identity found");
        }
        if(provisioningProfile.getItemCount() == 0) {
            return new ValidationInfo("No provisioning profile found");
        }
        return null;
    }

    public CreateIpaAction.IpaConfig getIpaConfig() {
        Module module = ModuleManager.getInstance(project).findModuleByName(this.module.getSelectedItem().toString());
        String signingIdentity = this.signingIdentity.getSelectedItem().toString();
        String provisioningProile = this.provisioningProfile.getSelectedItem().toString();
        List<Arch> archs = new ArrayList<Arch>();
        if(this.archs.getSelectedItem().toString().equals(ARCHS_ALL)) {
            archs.add(Arch.thumbv7);
            archs.add(Arch.arm64);
        } else if(this.archs.getSelectedItem().toString().equals(ARCHS_32BIT)) {
            archs.add(Arch.thumbv7);
        } else if(this.archs.getSelectedItem().toString().equals(ARCHS_ALL)) {
            archs.add(Arch.arm64);
        }
        return new CreateIpaAction.IpaConfig(module, new File(this.destinationDir.getText()), signingIdentity, provisioningProile, archs);
    }
}
