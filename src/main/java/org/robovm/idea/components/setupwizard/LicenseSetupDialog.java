package org.robovm.idea.components.setupwizard;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

import javax.swing.*;

import com.robovm.lm.LicenseManager;

public class LicenseSetupDialog extends JDialog {
    private JPanel header;
    private JPanel panel;
    private JLabel infoText;
    private JButton nextButton;
    private JButton signUpButton;
    private JButton activateKeyButton;
    private JTextField licenseKey;
    private JLabel licenseInfo;
    private Color defaultColor;

    public LicenseSetupDialog() {
        setContentPane(panel);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("RoboVM Setup");
        infoText.setText("<html>If you signed up for a RoboVM Trial license, or subscribed for a commercial license, enter the key below<br><br>" +
                "This will enable features such as the <strong>debugger</strong> or <strong>Interface Builder integration</strong>.<br><br>" +
                "You can use RoboVM without a license key as well. You can enter a license key at any time via the <i>RoboVM -> License Manager</i> menu.<br><br>");
        signUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://account.robovm.com/#/register"));
                } catch (Throwable t) {
                    // nothing to do here
                }
            }
        });
        licenseKey.setText(LicenseManager.getProductKey());
        try {
            if(LicenseManager.isActivated()) {
                licenseInfo.setText("Your license has been activated!");
                licenseInfo.setForeground(Color.green);
                activateKeyButton.setEnabled(false);
            }
        } catch (Throwable e) {
            licenseInfo.setText("Invalid license key");
            licenseInfo.setForeground(Color.red);
        }

        defaultColor = licenseInfo.getForeground();
        activateKeyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!activate(licenseKey.getText())) {
                    licenseInfo.setText("Invalid license key");
                    licenseInfo.setForeground(Color.red);
                } else {
                    licenseInfo.setText("Your license has been activated!");
                    licenseInfo.setForeground(Color.green);
                }
            }
        });

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    public static String getProductKey() {
        try {
            return LicenseManager.getProductKey();
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean activate(String productKey) {
        try {
            LicenseManager.activate(productKey);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
