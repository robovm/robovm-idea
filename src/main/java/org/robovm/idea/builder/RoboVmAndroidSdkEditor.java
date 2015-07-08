package org.robovm.idea.builder;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.robovm.idea.components.setupwizard.AndroidSetupDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Created by badlogic on 08/07/15.
 */
public class RoboVmAndroidSdkEditor {
    public JPanel panel;
    private JLabel errorLabel;
    private JButton installSdkButton;

    public RoboVmAndroidSdkEditor() {
        installSdkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        new AndroidSetupDialog().show();
                        validate();
                    }
                });
            }
        });
        validate();
    }

    public void validate() {
        if(AndroidSetupDialog.isAndroidSdkSetup()) {
            errorLabel.setForeground(new Color(0, 200, 0));
            errorLabel.setText("Found valid Android SDK!");
            installSdkButton.setVisible(false);
        } else {
            errorLabel.setForeground(Color.RED);
            errorLabel.setText("Please install a valid Android SDK");
            installSdkButton.setVisible(true);
        }
    }
}
