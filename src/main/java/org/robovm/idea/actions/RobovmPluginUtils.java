/*
 * Copyright (C) 2012 RoboVM AB
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

import java.io.StringWriter;

/**
 * Created by badlogic on 14/03/15.
 */
public class RobovmPluginUtils {
    // this is how we get the classpath of a module
    // ModuleRootManager.getInstance(ModuleManager.getInstance(project).getModules()[3]).orderEntries().classes().getRoots();

    // this is how we get the source paths
    // ModuleRootManager.getInstance(ModuleManager.getInstance(project).getModules()[3]).orderEntries().getSourcePathsList()

    // need to recursively get the dependencies as well, both classes and sources
    // should be simple

    // attach debugger https://devnet.jetbrains.com/message/5522503#5522503

//    Project project = e.getProject();
//    System.out.println(project);
    //GUI.main(new String[0]);

    public static void log(String tag, String format, Object ... objects) {
        String.format("[%s] " + format, tag, objects);
    }
}
