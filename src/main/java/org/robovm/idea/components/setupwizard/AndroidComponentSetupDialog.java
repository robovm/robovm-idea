package org.robovm.idea.components.setupwizard;

import org.jetbrains.android.sdk.AndroidSdkType;
import org.jetbrains.android.sdk.AndroidSdkUtils;

import java.io.File;

import javax.swing.*;

public class AndroidComponentSetupDialog extends JDialog {
    private static final String ANDROID_SDK_URL = "http://dl.google.com/android/android-sdk_r24.2-macosx.zip";
    private JPanel panel;
    private JLabel infoText;
    private JButton nextButton;
    private JButton installAndroidSdkButton;
    private JButton browseButton;
    private JTextField sdkLocation;
    private JPanel installPanel;

    public AndroidComponentSetupDialog(File sdkLocation) {
        setContentPane(panel);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("RoboVM Setup");
        infoText.setText("<html>Install or update Android SDK components.</br>");

        // AndroidSdkUtils.createNewAndroidPlatform(sdkLocation.getAbsolutePath(), true);

        pack();
        setLocationRelativeTo(null);
    }

    private void createUIComponents() {}

    public static void main(String[] args) {
        AndroidComponentSetupDialog dialog = new AndroidComponentSetupDialog(new File(System.getProperty("user.home") + "/Library/Android/sdk"));
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
