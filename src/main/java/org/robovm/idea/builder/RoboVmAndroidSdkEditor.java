package org.robovm.idea.builder;

import org.robovm.idea.RoboVmPlugin;
import org.robovm.idea.components.setupwizard.AndroidBundledSetupDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
                        new AndroidBundledSetupDialog().show();
                        validate();
                    }
                });
            }
        });
        validate();
    }

    public void validate() {
        if(RoboVmPlugin.isAndroidSdkSetup()) {
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
