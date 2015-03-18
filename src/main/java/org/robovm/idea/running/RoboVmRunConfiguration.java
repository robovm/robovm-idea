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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.robovm.compiler.config.Arch;
import org.robovm.idea.RoboVmIcons;
import org.robovm.idea.RoboVmPlugin;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RoboVmRunConfiguration extends ModuleBasedConfiguration<RoboVmRunConfigurationSettings>  {
    private boolean deviceConfiguration;
    private Arch deviceArch;
    private String signingIdentity;
    private String provisioningProfile;
    private Arch simArch;
    private String simulatorName;

    public RoboVmRunConfiguration(String name, RoboVmRunConfigurationSettings configurationModule, ConfigurationFactory factory) {
        super(name, configurationModule, factory);
    }

    @Override
    public Collection<Module> getValidModules() {
        return RoboVmPlugin.getRoboVmModules(getConfigurationModule().getProject());
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new RoboVmRunConfigurationSettingsEditor();
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return null;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);

        deviceConfiguration = Boolean.parseBoolean(JDOMExternalizerUtil.readField(element, "isDeviceConfig"));
        String deviceArchStr = JDOMExternalizerUtil.readField(element, "deviceArch");
        deviceArch = deviceArchStr.length() == 0? null: Arch.valueOf(deviceArchStr);
        signingIdentity = JDOMExternalizerUtil.readField(element, "signingIdentity");
        provisioningProfile = JDOMExternalizerUtil.readField(element, "provisioningProfile");
        String simArchStr = JDOMExternalizerUtil.readField(element, "simArch");
        simArch = simArchStr.length() == 0? null: Arch.valueOf(simArchStr);
        simulatorName = JDOMExternalizerUtil.readField(element, "simulatorName");
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);

        JDOMExternalizerUtil.writeField(element, "isDeviceConfig", Boolean.toString(deviceConfiguration));
        JDOMExternalizerUtil.writeField(element, "deviceArch", deviceArch == null? null: deviceArch.toString());
        JDOMExternalizerUtil.writeField(element, "signingIdentity", signingIdentity);
        JDOMExternalizerUtil.writeField(element, "provisioningProfile", provisioningProfile);
        JDOMExternalizerUtil.writeField(element, "simArch", simArch == null? null: simArch.toString());
        JDOMExternalizerUtil.writeField(element, "simulatorName", simulatorName);
    }

    public void setDeviceConfiguration(boolean deviceConfiguration) {
        this.deviceConfiguration = deviceConfiguration;
    }

    public boolean isDeviceConfiguration() {
        return deviceConfiguration;
    }

    public void setDeviceArch(Arch deviceArch) {
        this.deviceArch = deviceArch;
    }

    public Arch getDeviceArch() {
        return deviceArch;
    }

    public void setSigningIdentity(String signingIdentity) {
        this.signingIdentity = signingIdentity;
    }

    public String getSigningIdentity() {
        return signingIdentity;
    }

    public void setProvisioningProfile(String provisioningProfile) {
        this.provisioningProfile = provisioningProfile;
    }

    public String getProvisioningProfile() {
        return provisioningProfile;
    }

    public void setSimArch(Arch simArch) {
        this.simArch = simArch;
    }

    public Arch getSimArch() {
        return simArch;
    }

    public void setSimulatorName(String simulatorName) {
        this.simulatorName = simulatorName;
    }

    public String getSimulatorName() {
        return simulatorName;
    }
}
