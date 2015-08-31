package org.robovm.idea.components.setupwizard;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.BuildNumber;
import com.intellij.openapi.util.Version;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.NameMapper;
import org.zeroturnaround.zip.ZipUtil;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import javax.swing.*;

public class AndroidSetupDialog extends JDialog {
    enum OS {
        MacOsX,
        Windows,
        Linux
    }

    private static final String ANDROID_SDK_URL_MACOSX = "http://dl.google.com/android/android-sdk_r24.3.4-macosx.zip";
    private static final String ANDROID_SDK_URL_WINDOWS = "http://dl.google.com/android/android-sdk_r24.3.4-windows.zip";
    private static final String ANDROID_SDK_URL_LINUX = "http://dl.google.com/android/android-sdk_r24.3.4-linux.tgz";

    private static OS os;
    private static String ANDROID_SDK_URL;

    static {
        if(System.getProperty("os.name").contains("Mac")) {
            ANDROID_SDK_URL = ANDROID_SDK_URL_MACOSX;
            os = OS.MacOsX;
        } else if(System.getProperty("os.name").contains("Windows")) {
            ANDROID_SDK_URL = ANDROID_SDK_URL_WINDOWS;
            os = OS.Windows;
        } else if(System.getProperty("os.name").contains("Linux")) {
            ANDROID_SDK_URL = ANDROID_SDK_URL_LINUX;
            os = OS.Linux;
        }
    }

    private JPanel panel;
    private JLabel infoText;
    private JButton nextButton;
    private JButton installAndroidSdkButton;
    private JButton browseButton;
    private JTextField sdkLocation;
    private JPanel installPanel;

    public AndroidSetupDialog() {
        setContentPane(panel);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("RoboVM Setup");
        infoText.setText("<html>Install the Android SDK if you want to develop for both iOS and Android.</br>");

        sdkLocation.setText(System.getProperty("user.home") + "/Library/Android/sdk");

        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
                    @Override
                    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                        return file.isDirectory();
                    }

                    @Override
                    public boolean isFileSelectable(VirtualFile file) {
                        return file.isDirectory();
                    }
                };
                FileChooserDialog fileChooser = FileChooserFactory.getInstance().createFileChooser(fileChooserDescriptor, null, panel);
                VirtualFile location = null;
                if (new File(sdkLocation.getText()).exists()) {
                    location = LocalFileSystem.getInstance().findFileByIoFile(new File(sdkLocation.getText()));
                }
                VirtualFile[] dir = fileChooser.choose(null, location);
                if (dir != null && dir.length > 0) {
                    sdkLocation.setText(dir[0].getCanonicalPath());
                    boolean validSdk = isAndroidSdkInstalled(sdkLocation.getText());
                    if(validSdk) {
                        installAndroidSdkButton.setVisible(false);
                        nextButton.setText("Next");
                    } else {
                        installAndroidSdkButton.setVisible(true);
                        nextButton.setText("Skip");
                    }
                }
            }
        });

        installAndroidSdkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                installAndroidSdk();
            }
        });

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(nextButton.getText().equals("Skip")) {
                    dispose();
                } else {
                    installAndroidSdk();
                }
            }
        });

        if(isAndroidSdkInstalled(sdkLocation.getText())) {
            nextButton.setText("Next");
            installAndroidSdkButton.setVisible(false);
        } else {
            nextButton.setText("Skip");
            installAndroidSdkButton.setVisible(true);
        }

        pack();
        setLocationRelativeTo(null);
    }

    private void installAndroidSdk() {
        sdkLocation.setEnabled(false);
        nextButton.setEnabled(false);
        browseButton.setEnabled(false);

        downloadAndroidSdk();
    }

    private void downloadAndroidSdk() {
        installPanel.removeAll();
        GridLayoutManager layout = new GridLayoutManager(2, 1);
        installPanel.setLayout(layout);

        final JLabel label = new JLabel("Downloading Android SDK ...");
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);

        GridConstraints labelConsts = new GridConstraints();
        labelConsts.setFill(GridConstraints.FILL_HORIZONTAL);
        GridConstraints progressConsts = new GridConstraints();
        progressConsts.setRow(1);
        progressConsts.setFill(GridConstraints.FILL_HORIZONTAL);

        installPanel.add(label, labelConsts);
        installPanel.add(progressBar, progressConsts);
        installPanel.revalidate();

        installAndroidSdkButton.setText("Cancel");
        for(ActionListener listener: installAndroidSdkButton.getActionListeners()) {
            installAndroidSdkButton.removeActionListener(listener);
        }
        final BooleanFlag cancel = new BooleanFlag();
        installAndroidSdkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel.setValue(true);
            }
        });

        final String sdkDir = sdkLocation.getText();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!isAndroidSdkInstalled(sdkDir)) {
                        downloadAndroidSdk(sdkDir, progressBar, label, cancel);
                    }

                    if (!areAndroidComponentsInstalled(sdkDir)) {
                        installAndroidSdkComponents(sdkDir, label);
                    }

                    // spawn the Sdk setup dialog
                    if( !isAndroidSdkSetup()) {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        String clazz = "com.android.tools.idea.sdk.DefaultSdks";
                                        String method = "createAndroidSdksForAllTargets";

                                        try {
                                            Class cls = AndroidSetupDialog.class.getClassLoader().loadClass(clazz);
                                            Method mtd = cls.getMethod(method, File.class);
                                            mtd.invoke(null, new File(sdkDir));
                                        } catch(Throwable t) {
                                            clazz = "com.android.tools.idea.sdk.IdeSdks";
                                            method = "createAndroidSdkPerAndroidTarget";

                                            try {
                                                Class cls = AndroidSetupDialog.class.getClassLoader().loadClass(clazz);
                                                Method mtd = cls.getMethod(method, File.class);
                                                mtd.invoke(null, new File(sdkDir));
                                            } catch(Throwable t2) {
                                                t2.printStackTrace();
                                                System.out.println("Couldn't create Android SDK");
                                            }
                                        }
                                    }
                                });
                            }
                        });
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            AndroidSetupDialog.this.dispose();
                        }
                    });
                } catch (final Throwable e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            label.setForeground(Color.red);
                            label.setText("Couldn't install Android SDK: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                }
            }
        }).start();
    }

    private void installAndroidSdkComponents(String sdkDir, final JLabel label) throws IOException, InterruptedException {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                label.setText("Installing SDK components");
            }
        });

        // done with unpacking, let's show the components
        // installer wizard
        Process process = new ProcessBuilder(new File(sdkDir, os == OS.Windows? "tools/android.bat": "tools/android").getAbsolutePath()).start();
        process.waitFor();
    }

    private void downloadAndroidSdk(final String sdkDir, final JProgressBar progressBar, final JLabel label, final BooleanFlag cancel) throws IOException {
        // download the SDK zip to a temporary location
        File destination = File.createTempFile("android-sdk", ".zip");
        destination.deleteOnExit();
        System.out.println(destination);
        URL url = new URL(ANDROID_SDK_URL);
        URLConnection con = url.openConnection();
        long length = con.getContentLengthLong();
        byte[] buffer = new byte[1024*100];
        try (InputStream in = con.getInputStream(); OutputStream out = new BufferedOutputStream(new FileOutputStream(destination))) {
            int read = in.read(buffer);
            int total = read;
            while(read != -1) {
                out.write(buffer, 0, read);
                read = in.read(buffer);
                total += read;
                final int percentage = (int)(((double)total / (double)length) * 100);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setValue(Math.min(100, percentage));
                    }
                });

                if(cancel.getValue()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            installAndroidSdkButton.setText("Install Android SDK");
                            for(ActionListener listener: installAndroidSdkButton.getActionListeners()) {
                                installAndroidSdkButton.removeActionListener(listener);
                            }
                            installAndroidSdkButton.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    installAndroidSdk();
                                }
                            });
                            installPanel.removeAll();
                            installPanel.revalidate();
                            browseButton.setEnabled(true);
                            nextButton.setEnabled(true);
                        }
                    });
                    return;
                }
            }
        }

        // unpack the SDK zip, then rename the extracted
        // folder
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                label.setText("Unpacking SDK to " + sdkDir);
                installAndroidSdkButton.setEnabled(false);
                nextButton.setEnabled(false);
            }
        });
        final File outputDir = new File(sdkDir);
        File tmpOutputDir = new File(outputDir.getParent());
        if(!tmpOutputDir.exists()) {
            if(!tmpOutputDir.mkdirs()) {
                throw new RuntimeException("Couldn't create output directory");
            }
        }

        if(ANDROID_SDK_URL.endsWith("zip")) {
            ZipUtil.unpack(destination, tmpOutputDir, new NameMapper() {
                @Override
                public String map(String s) {
                    int idx = s.indexOf("/");
                    s = outputDir.getName() + s.substring(idx);
                    final String file = s;
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            label.setText("Unpacking " + file.substring(0, Math.min(50, file.length() - 1)) + " ...");
                        }
                    });
                    return s;
                }
            });
        } else {
            TarArchiveInputStream in = null;
            try {
                in = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(destination)));
                ArchiveEntry entry = null;
                while ((entry = in.getNextEntry()) != null) {
                    File f = new File(tmpOutputDir, entry.getName());
                    if (entry.isDirectory()) {
                        f.mkdirs();
                    } else {
                        f.getParentFile().mkdirs();
                        OutputStream out = null;
                        try {
                            out = new FileOutputStream(f);
                            IOUtils.copy(in, out);
                        } finally {
                            IOUtils.closeQuietly(out);
                        }
                    }

                    final String fileName = entry.getName();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            label.setText("Unpacking " + fileName.substring(0, Math.min(50, fileName.length() - 1)) + " ...");
                        }
                    });
                }
            } catch (Throwable t) {
                // can't do anything here
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
        // ziputils doesn't preserve file permissions
        for(File file: new File(outputDir, "tools").listFiles()) {
            if(file.isFile()) {
                file.setExecutable(true);
            }
        }

        for(File file: new File(outputDir, "tools/proguard/bin").listFiles()) {
            if(file.isFile()) {
                file.setExecutable(true);
            }
        }
    }

    private static class BooleanFlag {
        volatile boolean value;

        public void setValue(boolean value) {
            this.value = value;
        }

        public boolean getValue() {
            return this.value;
        }
    }

    public static boolean isAndroidSdkInstalled(String sdkDir) {
        File sdk = new File(sdkDir, os == OS.Windows? "tools/android.bat": "tools/android");
        return sdk.exists();
    }

    public static boolean isAndroidSdkSetup() {
        for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
            if (sdk.getSdkType().getName().equals("Android SDK")) {
                return true;
            }
        }
        return false;
    }

    public static String getAndroidSdkLocation() {
        for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
            if (sdk.getSdkType().getName().equals("Android SDK")) {
                return sdk.getHomePath();
            }
        }
        return null;
    }

    public static boolean areAndroidComponentsInstalled(String sdkDir) {
        return new File(sdkDir, "platforms").list().length > 0;
    }

    private void createUIComponents() {}

    public static void main(String[] args) {
        AndroidSetupDialog dialog = new AndroidSetupDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
