package org.robovm.idea.running;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import org.jetbrains.annotations.NotNull;
import org.robovm.idea.RoboVmIcons;

import javax.swing.*;

/**
 * Created by badlogic on 25/03/15.
 */
public class RoboVmConsoleConfigurationType implements ConfigurationType {
    @Override
    public String getDisplayName() {
        return "RoboVM Console";
    }

    @Override
    public String getConfigurationTypeDescription() {
        return "A run configuration to test your console app";
    }

    @Override
    public Icon getIcon() {
        return RoboVmIcons.ROBOVM_SMALL;
    }

    @NotNull
    @Override
    public String getId() {
        return "com.robovm.idea.running.RoboVmConsoleConfigurationType";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[] {
                new RoboVmConfigurationFactory(this)
        };
    }
}