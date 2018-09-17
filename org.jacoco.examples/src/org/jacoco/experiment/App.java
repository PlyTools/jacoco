/*******************************************************************************
 * Copyright (c) 2009, 2018 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.experiment;

import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class App {
    private String jarName;
    private String packageName;
    private String jarPath;
    private String activityName;
    public int mTotalCount = 0;
    public float coverageRate;
    public List<String> mCovered = new ArrayList<String>();
    private List<String> classNames = new ArrayList<String>();


    public App(String jarName, String packageName, String jarPath) {
        this.jarName = jarName;
        this.packageName = packageName;
        this.jarPath = jarPath;
        this.activityName = "com.record.myLife.base.BottomActivity";//activity name is fixed for convenience, and we need to adjust the config.json later.
    }

    public String getJarName() {
        return jarName;
    }

    public String getJarPath() {
        return jarPath;
    }

    public float getCoverageRate() {
        return coverageRate;
    }

    public String getPackageName() {
        return packageName;
    }

    public void initInfo() {
        final String ADDRESS = "localhost";
        final int APK_PORT = 6300;
        ExecutionDataStore executionDataStore = new ExecutionDataStore();
        SessionInfoStore sessionInfos = new SessionInfoStore();

        try {
            System.out.println("connecting to the device");
            ShellUtils.runShell("adb shell am start " + packageName + "/" + activityName, 8, true);
            ShellUtils.runShell("adb devices", 5, true);
            // Open a socket to the coverage agent:
            Socket socket = new Socket(InetAddress.getByName(ADDRESS), APK_PORT);
            RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
            RemoteControlReader reader = new RemoteControlReader(socket.getInputStream());
            reader.setSessionInfoVisitor(sessionInfos);
            reader.setExecutionDataVisitor(executionDataStore);

            CoverageBuilder coverageBuilder = new CoverageBuilder();
            Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);

            // Send a dump command and read the response:
            writer.visitDumpCommand(true, false);
            if (!reader.read()) {
                throw new IOException("Socket closed unexpectedly.");
            }
            socket.close();
            // Initialize client and get the info of each class.

            for(String classname: PackageUtil.getClassNameByJar(jarPath)) {
                classname = classname.replace('.', '/');
                classNames.add(classname);
            }

            for(String classname:classNames) {
                InputStream original = getTargetClass(classname);
                if (original != null) {
                    analyzer.analyzeClass(original, classname);
                    original.close();
                } else {
                    System.out.println("Cannot find " + classname);
                    original.close();
                    continue;
                }
            }

            for (final IClassCoverage cc : coverageBuilder.getClasses()) {
                for (final IMethodCoverage mc : cc.getMethods()) {
                    ICounter icounter;
                    icounter = mc.getMethodCounter();
                    if(icounter.getCoveredCount() > 0) {
                        mCovered.add(cc.getName() + "-" + mc.getName());
                    }
                    mTotalCount++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void kill() {
        try {
            ShellUtils.runShell("adb shell am force-stop " + packageName,-1, true);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get different coverage rates.
     *
     * @return
     * @throws Exception
     */
    public float getMethodCoverage() throws Exception {
        final String ADDRESS = "localhost";
        final int APK_PORT = 6300;
        ExecutionDataStore executionDataStore = new ExecutionDataStore();
        final SessionInfoStore sessionInfos = new SessionInfoStore();

        // Open a socket to the coverage agent:
        final Socket socket = new Socket(InetAddress.getByName(ADDRESS), APK_PORT);
        final RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
        final RemoteControlReader reader = new RemoteControlReader(socket.getInputStream());
        reader.setSessionInfoVisitor(sessionInfos);
        reader.setExecutionDataVisitor(executionDataStore);

        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);

        // Send a dump command and read the response:
        writer.visitDumpCommand(true, false);
        if (!reader.read()) {
            throw new IOException("Socket closed unexpectedly.");
        }
        socket.close();
        for(String classname:classNames) {
            InputStream original = getTargetClass(classname);
            if (original != null) {
                analyzer.analyzeClass(original, classname);
                original.close();
            } else {
                System.out.println("Cannot find " + classname);
                original.close();
                continue;
            }
        }

        // Let's dump some metrics and line coverage information:
        for (final IClassCoverage cc : coverageBuilder.getClasses()) {
            for (final IMethodCoverage mc : cc.getMethods()) {
                ICounter icounter = mc.getMethodCounter();
                if(icounter.getCoveredCount() > 0 && !mCovered.contains(cc.getName() + "-" + mc.getName())) {
                    mCovered.add(cc.getName() + "-" + mc.getName());
                }
            }
        }
        if (!(mCovered.size()==0)) {
            System.out.println("Total methods: " + mTotalCount);
            coverageRate = (float) mCovered.size()/mTotalCount;
        } else {
            System.out.println("No method detected");
        }
        return coverageRate;
    }

    private static InputStream getTargetClass(final String name) {
        final String resource = '/' + name.replace('.', '/') + ".class";
        return Experiment.class.getResourceAsStream(resource);
    }

    /**
     * append the string to the last of the file
     * @param filename
     * @param content
     */
    public static void saveStatus(String filename, String content) {
        FileOutputStream fos = null;
        File file;
        try{
            file = new File(filename);
            fos = new FileOutputStream(file,true);
            fos.write(content.getBytes());
        } catch (IOException e) {
        } finally {
            try {
                if (null != fos) fos.close();
            } catch (IOException e) {
            }
        }
    }
}
