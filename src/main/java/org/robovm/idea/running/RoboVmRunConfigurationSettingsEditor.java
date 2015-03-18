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

public class RoboVmRunConfigurationSettingsEditor extends SettingsEditor<RoboVmRunConfiguration> {
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

    @Override
    protected void resetEditorFrom(RoboVmRunConfiguration config) {
        populateControls(config);
    }

    @Override
    protected void applyEditorTo(RoboVmRunConfiguration config) throws ConfigurationException {
        config.setModule(((Module)module.getSelectedItem()));
        config.setDeviceConfiguration(attachedDeviceRadioButton.isSelected());
        config.setDeviceArch((Arch) deviceArch.getSelectedItem());
        config.setSigningIdentity(signingIdentity.getSelectedItem().toString());
        config.setProvisioningProfile(provisioningProfile.getSelectedItem().toString());
        config.setSimArch((Arch) simArch.getSelectedItem());
        config.setSimulatorName(((SimTypeWrapper)simType.getSelectedItem()).getType().getDeviceName());
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
            this.module.addItem(module);
            if(module.getName().equals(config.getConfigurationModule().getModuleName())) {
                this.module.setSelectedIndex(this.module.getItemCount() - 1);
            }
        }

        attachedDeviceRadioButton.setSelected(config.isDeviceConfiguration());

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
        for(DeviceType type: DeviceType.listDeviceTypes(RoboVmPlugin.getRoboVmHome())) {
            simType.addItem(new SimTypeWrapper(type));
            if(type.getDeviceName().equals(config.getSimulatorName())) {
                simType.setSelectedIndex(simType.getItemCount() - 1);
            }
        }

        // populate signing identities
        signingIdentity.removeAllItems();
        signingIdentity.addItem("Auto (matches 'iPhone Developer|iOS Development')");
        signingIdentity.addItem("Don't sign");
        for(SigningIdentity identity: SigningIdentity.list()) {
            signingIdentity.addItem(identity.getName());
            if(identity.getName().equals(config.getSigningIdentity())) {
                signingIdentity.setSelectedIndex(signingIdentity.getItemCount() - 1);
            }
        }

        // populate provisioning profiles
        provisioningProfile.removeAllItems();
        provisioningProfile.addItem("Auto");
        for(ProvisioningProfile profile: ProvisioningProfile.list()) {
            provisioningProfile.addItem(profile.getName());
            if(profile.getName().equals(config.getProvisioningProfile())) {
                provisioningProfile.setSelectedIndex(provisioningProfile.getItemCount() - 1);
            }
        }
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
