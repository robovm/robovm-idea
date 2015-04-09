package org.robovm.idea.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;

public class NewStoryboardDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField storyboardName;

    private final File resourceDir;

    public NewStoryboardDialog(Project project, File resourceDir) {
        super(project);
        init();
        setTitle("New iOS Storyboard");
        this.resourceDir = resourceDir;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if(storyboardName.getText() == null || storyboardName.getText().trim().isEmpty()) {
            return new ValidationInfo("Please specify a valid file name for the storyboard");
        }

        if(new File(resourceDir, storyboardName.getText() + ".storyboard").exists()) {
            return new ValidationInfo("A storyboard with that name already exists");
        }
        return null;
    }

    public String getStoryboardName() {
        String text = storyboardName.getText();
        if(text != null) {
            text.trim();
            if(text.isEmpty()) return null;
            else return text;
        }
        return null;
    }
}
