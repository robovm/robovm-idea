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
package org.robovm.idea.running;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.target.ios.DeviceType;
import org.robovm.compiler.target.ios.ProvisioningProfile;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.robovm.idea.RoboVmPlugin;

import javax.swing.*;

public class RoboVmIOSRunConfigurationSettingsEditor extends SettingsEditor<RoboVmRunConfiguration> {
    public static final String SKIP_SIGNING = "Don't sign";
    public static final String AUTO_SIGNING_IDENTITY = "Auto (matches 'iPhone Developer|iOS Development')";
    public static final String AUTO_PROVISIONING_PROFILE = "Auto";
    private JPanel panel;
    private JTabbedPane tabbedPane1;
    private JComboBox module;
    private JRadioButton attachedDeviceRadioButton;
    private JRadioButton simulatorRadioButton;
    private JComboBox simType;
    private JComboBox signingIdentity;
    private JComboBox provisioningProfile;
    private JComboBox simArch;
    private JComboBox deviceArch;
    private JTextArea args;

    @Override
    protected void resetEditorFrom(RoboVmRunConfiguration config) {
        populateControls(config);
    }

    @Override
    protected void applyEditorTo(RoboVmRunConfiguration config) throws ConfigurationException {
        config.setModuleName(module.getSelectedItem().toString());
        config.setTargetType(attachedDeviceRadioButton.isSelected()? RoboVmRunConfiguration.TargetType.Device: RoboVmRunConfiguration.TargetType.Simulator);
        config.setDeviceArch((Arch) deviceArch.getSelectedItem());
        config.setSigningIdentity(signingIdentity.getSelectedItem().toString());
        config.setProvisioningProfile(provisioningProfile.getSelectedItem().toString());
        config.setSimArch((Arch) simArch.getSelectedItem());
        config.setSimulatorName(((SimTypeWrapper)simType.getSelectedItem()).getType().getDeviceName());
        config.setArguments(args.getText());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return panel;
    }

    private void populateControls(RoboVmRunConfiguration config) {
        // populate with RoboVM Sdk modules
        this.module.removeAllItems();
        for(Module module: RoboVmPlugin.getRoboVmModules(config.getProject())) {
            this.module.addItem(module.getName());
            if(module.getName().equals(config.getModuleName())) {
                this.module.setSelectedIndex(this.module.getItemCount() - 1);
            }
        }

        attachedDeviceRadioButton.setSelected(config.getTargetType() == RoboVmRunConfiguration.TargetType.Device);

        // populate archs
        simArch.removeAllItems();
        deviceArch.removeAllItems();
        for(Arch arch: Arch.values()) {
            if(arch.isArm()) {
                deviceArch.addItem(arch);
                if(arch == config.getDeviceArch()) {
                    deviceArch.setSelectedIndex(deviceArch.getItemCount() - 1);
                }
            } else {
                simArch.addItem(arch);
                if(arch == config.getSimArch()) {
                    simArch.setSelectedIndex(simArch.getItemCount() - 1);
                }
            }
        }

        // populate simulators
        simType.removeAllItems();
        for(DeviceType type: DeviceType.listDeviceTypes()) {
            simType.addItem(new SimTypeWrapper(type));
            if(type.getDeviceName().equals(config.getSimulatorName())) {
                simType.setSelectedIndex(simType.getItemCount() - 1);
            }
        }

        // populate signing identities
        signingIdentity.removeAllItems();
        signingIdentity.addItem(AUTO_SIGNING_IDENTITY);
        signingIdentity.addItem(SKIP_SIGNING);
        for(SigningIdentity identity: SigningIdentity.list()) {
            signingIdentity.addItem(identity.getName());
            if(identity.getName().equals(config.getSigningIdentity())) {
                signingIdentity.setSelectedIndex(signingIdentity.getItemCount() - 1);
            }
        }

        // populate provisioning profiles
        provisioningProfile.removeAllItems();
        provisioningProfile.addItem(AUTO_PROVISIONING_PROFILE);
        for(ProvisioningProfile profile: ProvisioningProfile.list()) {
            provisioningProfile.addItem(profile.getName());
            if(profile.getName().equals(config.getProvisioningProfile())) {
                provisioningProfile.setSelectedIndex(provisioningProfile.getItemCount() - 1);
            }
        }

        this.args.setText(config.getArguments());
    }

    private class SimTypeWrapper {
        private final DeviceType type;

        public SimTypeWrapper(DeviceType type) {
            this.type = type;
        }

        public DeviceType getType() {
            return type;
        }

        @Override
        public String toString() {
            return type.getSimpleDeviceName() + " - " + type.getSdk().getVersion();
        }
    }
}
