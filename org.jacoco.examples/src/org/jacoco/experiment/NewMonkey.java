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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Observable;

public class NewMonkey extends AppTestTool {

    public void run() {
        try {
            start(testApp.getPackageName());
            Thread t = new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        if(!isAlive()) {
                            try {
                                if (!isTimeArrive()) {
                                    System.out.println(getClass().getSimpleName() + " is not running, restart it");
                                    start(testApp.getPackageName());
                                    Experiment.restartTime += 1;
                                    System.out.println("Restart time in run: " + Integer.toString(Experiment.restartTime));
                                } else {
                                    System.out.println("test time arrives, finished");
                                    System.out.println("restart times: " + Integer.toString(Experiment.restartTime));
                                    break;
                                }
                            }catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            t.start();

            if (mode.equals("origin")) {
                weithtedAlgorithm();
            }else  if (mode.equals("methods-coverage")) {
                methodCoverageAlgorithm();
            }else if (mode.equals("views-coverage") || mode.equals("priority-set")) {
                viewsCoverageAlgorithm();
            }
        } catch (Exception e) {
            doBusiness();
        }
    }

    public NewMonkey(App testApp, String mode) {
        super(testApp, mode);
    }

    public void weithtedAlgorithm() {
        int label = -1;
        while (true){
            long time = System.currentTimeMillis() - Experiment.beforeCycle;
            if (time/120000 >= label) {
                label++;
                try {
                    float mcov = testApp.getMethodCoverage();
                    System.out.println("Time: " + Long.toString(time));
                    System.out.printf("coverage rate of methods: %f %n", mcov);
                    testApp.saveStatus(Experiment.tcpPath + "\\result\\" + testApp.getJarName() + "-"  + getClass().getSimpleName() + "-" + mode + ".txt", "Time: " + Long.toString(System.currentTimeMillis() - Experiment.beforeCycle) + "\r\ncoverage rate of methods: " + Float.toString(mcov) + "\r\n");
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            if( isTimeArrive()) {
                System.out.println("finished!");
                if (isAlive()) kill();
                break;
            }

        }
    }

    public void methodCoverageAlgorithm() {
        System.out.println("running method coverage algorithm");
        final int MONKEY_PORT = 6400;
        try {
            ServerSocket serverSocket = new ServerSocket(MONKEY_PORT);
            while (!isTimeArrive() && !serverSocket.isClosed() ) {
                try {
                    System.out.println("serverSocket is ready for action");
                    synchronized (serverSocket) {
                        Socket socket = serverSocket.accept();
//                        socket.setKeepAlive(true);
//                        socket.setSoTimeout(30*1000);
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                        while (true) {
                            long time = System.currentTimeMillis() - Experiment.beforeCycle;
                            if(isTimeArrive()) {
                                System.out.println("finished!");
                                if (isAlive()) kill();
                                socket.shutdownOutput();
                                socket.close();
                                break;
                            }
                            int i = in.read();
                            System.out.println("i = " + i);
                            if (i != -1) {
                                try {
                                    float mcov = testApp.getMethodCoverage();
                                    System.out.printf("coverage rate of methods: %f %n", mcov);

                                    testApp.saveStatus(Experiment.tcpPath + "\\result\\" + testApp.getJarName() + "-"  + getClass().getSimpleName() + "-" + mode + ".txt", "Time: " + Long.toString(time) + "\r\ncoverage rate of methods: " + Float.toString(mcov) + "\r\n");

                                    out.writeFloat(mcov);
                                    socket.shutdownOutput();
                                    socket.close();
                                    break;
                                } catch (Exception e) {
                                    System.out.println(e.getStackTrace());
                                    socket.close();
                                }
                            }
                        }
                    }
                } catch (final IOException e) {
                    // If the serverSocket is closed while accepting
                    // connections a SocketException is expected.
                    if (!serverSocket.isClosed()) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void viewsCoverageAlgorithm() {
        int label = -1;
        while (true){
            long time = System.currentTimeMillis() - Experiment.beforeCycle;
            if (time/120000 >= label) {
                label++;
                try {
                    float mcov = testApp.getMethodCoverage();
                    System.out.println("Time: " + Long.toString(time));
                    System.out.printf("coverage rate of methods: %f %n", mcov);
                    testApp.saveStatus(Experiment.tcpPath + "\\result\\" + testApp.getJarName() + "-" + getClass().getSimpleName() + "-"  + mode + ".txt", "Time: " + Long.toString(System.currentTimeMillis() - Experiment.beforeCycle) + "\r\ncoverage rate of methods: " + Float.toString(mcov) + "\r\n");
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            if( isTimeArrive()) {
                System.out.println("finished!");
                if (isAlive()) kill();
                break;
            }
        }
    }

    /**
     *  restart a new monkey thread after the observed thread terminate unexpectedly.
     * @param o
     * @param arg
     */
    public void update(Observable o, Object arg) {
        System.out.println(getClass().getSimpleName() + " thread exit");
        if (isTimeArrive()) {
            if (isAlive()) kill();
            System.out.println("test time arrives, finished");
        } else {
            Experiment.restartTime += 1;
            System.out.println("Restart time in update: " + Integer.toString(Experiment.restartTime));
            NewMonkey newMonkey = new NewMonkey(testApp, mode);
            newMonkey.addObserver(this);
            new Thread(newMonkey).start();
            System.out.println("did not reach the test time, " + getClass().getSimpleName() + " restart");
        }
    }

    @Override
    public void start(String packageName) throws InterruptedException {
        if (mode.equals("origin")) {
            ShellUtils.runShell("adb shell /data/local/tmp/monkey/monkey --ignore-security-exceptions --ignore-crashes --ignore-timeouts --kill-process-after-error -p " + packageName + " --clickviewmode 0 40000000", 3, true);
        } else if (mode.equals("methods-coverage")) {
            ShellUtils.runShell("adb shell /data/local/tmp/monkey/monkey --ignore-security-exceptions --ignore-crashes --ignore-timeouts --kill-process-after-error -p " + packageName + " --clickviewmode 3 40000000", 3, true);
        } else if (mode.equals("views-coverage")) {
            ShellUtils.runShell("adb shell /data/local/tmp/monkey/monkey --ignore-crashes --ignore-timeouts --kill-process-after-error -p " + packageName + " --clickviewmode 3 40000000", 3, true);
        } else if (mode.equals("priority-set")) {
            ShellUtils.runShell("adb shell /data/local/tmp/monkey/monkey --ignore-crashes --ignore-timeouts --kill-process-after-error -p " + packageName + " --clickviewmode 4 40000000", 3, true);
        }
    }

    @Override
    public void kill() {
        try {
            String out = ShellUtils.runShell("adb shell ps | grep monkey", -1, false);
            ShellUtils.runShell("adb shell kill " + out.replaceAll("\\s+", " ").split(" ")[1], -1, true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isAlive() {
        String out = "";
        try {
            out = ShellUtils.runShell("adb shell ps | grep monkey", -1, false);
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(out.contains("monkey")) {
            return true;
        } else {
            return false;
        }
    }
}
