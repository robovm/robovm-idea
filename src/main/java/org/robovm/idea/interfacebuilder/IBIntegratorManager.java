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
package org.robovm.idea.interfacebuilder;

import org.robovm.idea.RoboVmPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by badlogic on 07/04/15.
 */
public class IBIntegratorManager {
    private static boolean hasIBIntegrator;
    private static IBIntegratorManager instance;

    private Map<String, IBIntegratorProxy> daemons = new HashMap<String, IBIntegratorProxy>();

    static {
        try {
            IBIntegratorProxy.getIBIntegratorClass();
            hasIBIntegrator = true;
        } catch (Throwable t) {
            hasIBIntegrator = false;
            RoboVmPlugin.logWarn(t.getMessage());
        }
    }

    public static IBIntegratorManager getInstance() {
        if (instance == null) {
            instance = new IBIntegratorManager();
        }
        return instance;
    }
}
