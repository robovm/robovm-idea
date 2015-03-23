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
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.robovm.compiler.AppCompiler;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.target.LaunchParameters;
import org.robovm.compiler.util.io.Fifos;
import org.robovm.compiler.util.io.OpenOnReadFileInputStream;
import org.robovm.idea.RoboVmPlugin;
import org.robovm.idea.compilation.RoboVmCompileTask;

import java.io.*;

public class RoboVmRunner extends GenericProgramRunner {

    public static final String DEBUG_EXECUTOR = "Debug";
    public static final String RUN_EXECUTOR = "Run";

    @NotNull
    @Override
    public String getRunnerId() {
        return "com.robovm.idea.running.RoboVmRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return (executorId.equals(DEBUG_EXECUTOR) || executorId.equals(RUN_EXECUTOR)) && profile instanceof RoboVmRunConfiguration;
    }

    @Override
    protected void execute(@NotNull ExecutionEnvironment environment, @Nullable Callback callback, @NotNull RunProfileState state) throws ExecutionException {
        // we need to pass the run profile info to the compiler so
        // we can decide if this is a debug or release build
        RoboVmRunConfiguration runConfig = (RoboVmRunConfiguration)environment.getRunnerAndConfigurationSettings().getConfiguration();
        runConfig.setDebug(DEBUG_EXECUTOR.equals(environment.getExecutor().getId()));
        super.execute(environment, callback, state);
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        ExecutionResult executionResult = state.execute(environment.getExecutor(), this);
        if (executionResult == null) {
            return null;
        }
        return new RunContentBuilder(executionResult, environment).showRunContent(environment.getContentToReuse());
    }
}
