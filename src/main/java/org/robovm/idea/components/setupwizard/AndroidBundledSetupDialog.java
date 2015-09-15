package org.robovm.idea.components.setupwizard;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import javax.swing.*;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.NameMapper;
import org.zeroturnaround.zip.ZipUtil;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

public class AndroidBundledSetupDialog extends JDialog {
    private String sdkDir;

    enum OS {
        MacOsX,
        Windows,
        Linux
    }

    private static final String ANDROID_SDK_URL_MACOSX = "http://download.robovm.org/android-sdks/android-sdk-macosx-23-rvm-1.8.tar.gz";
    private static final String ANDROID_SDK_URL_WINDOWS = "http://download.robovm.org/android-sdks/android-sdk-windows-23-rvm-1.8.zip";
    private static final String ANDROID_SDK_URL_LINUX = "http://download.robovm.org/android-sdks/android-sdk-linux-23-rvm-1.8.tar.gz";

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
    private JPanel installPanel;

    public AndroidBundledSetupDialog() {
        setContentPane(panel);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("RoboVM Setup");
        infoText.setText("<html>Install the Android SDK if you want to develop for both iOS and Android.</br>");

        installAndroidSdkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                installAndroidSdk();
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

    private void installAndroidSdk() {
        nextButton.setEnabled(false);
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
        for (ActionListener listener : installAndroidSdkButton.getActionListeners()) {
            installAndroidSdkButton.removeActionListener(listener);
        }
        final BooleanFlag cancel = new BooleanFlag();
        installAndroidSdkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel.setValue(true);
            }
        });

        final String sdkDir = getSdkDir();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Download our bundle
                    downloadAndroidSdk(sdkDir, progressBar, label, cancel);

                    // Setup the Android SDK for our installation dir
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                @Override
                                public void run() {
                                    String clazz = "com.android.tools.idea.sdk.DefaultSdks";
                                    String method = "createAndroidSdksForAllTargets";

                                    try {
                                        Class cls = AndroidBundledSetupDialog.class.getClassLoader().loadClass(clazz);
                                        Method mtd = cls.getMethod(method, File.class);
                                        mtd.invoke(null, new File(sdkDir));
                                    } catch (Throwable t) {
                                        clazz = "com.android.tools.idea.sdk.IdeSdks";
                                        method = "createAndroidSdkPerAndroidTarget";

                                        try {
                                            Class cls = AndroidBundledSetupDialog.class.getClassLoader().loadClass(clazz);
                                            Method mtd = cls.getMethod(method, File.class);
                                            mtd.invoke(null, new File(sdkDir));
                                        } catch (Throwable t2) {
                                            t2.printStackTrace();
                                            System.out.println("Couldn't create Android SDK");
                                        }
                                    }
                                }
                            });
                            }
                    });

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            AndroidBundledSetupDialog.this.dispose();
                        }
                    });
                } catch (final Throwable e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            label.setForeground(Color.red);
                            String msg = e.getMessage().substring(0, Math.min(50, e.getMessage().length() - 1)) + " ...";
                            label.setText("Couldn't install Android SDK: " + msg);
                            installAndroidSdkButton.setEnabled(false);
                            nextButton.setEnabled(true);
                        }
                    });
                }
            }
        }).start();
    }

    private void downloadAndroidSdk(final String sdkDir, final JProgressBar progressBar, final JLabel label, final BooleanFlag cancel) throws IOException {
        // download the SDK zip to a temporary location
        File destination = File.createTempFile("android-sdk", ".zip");
        destination.deleteOnExit();

        URL url = new URL(ANDROID_SDK_URL);
        URLConnection con = url.openConnection();
        final long length = con.getContentLengthLong();
        byte[] buffer = new byte[1024*100];
        try (InputStream in = con.getInputStream(); OutputStream out = new BufferedOutputStream(new FileOutputStream(destination))) {
            int read = in.read(buffer);
            long total = read;
            while(read != -1) {
                out.write(buffer, 0, read);
                read = in.read(buffer);
                total += read;
                final int percentage = (int)(((double)total / (double)length) * 100);
                final long totalRead = total;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setValue(Math.min(100, percentage));
                        label.setText("Downloading Android SDK (" + (totalRead / 1024 / 1024) + "/" + (length  / 1024 / 1024) + "MB)");
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
                TarArchiveEntry entry = null;
                while ((entry = (TarArchiveEntry)in.getNextEntry()) != null) {
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

                        if((entry.getMode() & 0100) != 0) {
                            f.setExecutable(true);
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

        for(File file: new File(outputDir, "platform-tools").listFiles()) {
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

    public static String getSdkDir() {
        return new File(new File(System.getProperty("user.home")), "/Library/RoboVM/android-sdk").getAbsolutePath();
    }

    private void createUIComponents() {}
}
