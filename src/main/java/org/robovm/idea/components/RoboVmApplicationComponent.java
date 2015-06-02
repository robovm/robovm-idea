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
package org.robovm.idea.components;

import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;
import org.robovm.idea.RoboVmPlugin;
import org.robovm.idea.sdk.JdkSetupDialog;
import org.robovm.idea.sdk.RoboVmSdkType;

/**
 * Call on app startup, responsible for extracting/updating the
 * RoboVM SDK and setting up the SDK so its available in IDEA.
 */
public class RoboVmApplicationComponent implements ApplicationComponent {
    @Override
    public void initComponent() {
        RoboVmPlugin.extractSdk();
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "org.robovm.idea.components.RoboVmApplicationComponent";
    }
}
