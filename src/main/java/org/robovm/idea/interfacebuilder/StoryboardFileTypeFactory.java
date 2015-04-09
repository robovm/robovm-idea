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

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.robovm.idea.RoboVmIcons;

import javax.swing.*;

/**
 * Created by badlogic on 08/04/15.
 */
public class StoryboardFileTypeFactory extends FileTypeFactory {
    public static class StoryboardFileType implements FileType {
        public static final StoryboardFileType INSTANCE = new StoryboardFileType();

        @NotNull
        @Override
        public String getName() {
            return "iOS Storyboard";
        }

        @NotNull
        @Override
        public String getDescription() {
            return "An iOS Storyboard file";
        }

        @NotNull
        @Override
        public String getDefaultExtension() {
            return "storyboard";
        }

        @Nullable
        @Override
        public Icon getIcon() {
            return RoboVmIcons.ROBOVM_SMALL;
        }

        @Override
        public boolean isBinary() {
            return false;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Nullable
        @Override
        public String getCharset(@NotNull VirtualFile file, @NotNull byte[] content) {
            return "UTF-8";
        }
    }

    @Override
    public void createFileTypes(FileTypeConsumer consumer) {
        consumer.consume(StoryboardFileType.INSTANCE);
    }
}
