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

import java.util.Observable;

public class Monkey extends AppTestTool{

    @Override
    public void run() {
        try {
            if (!isTimeArrive()) {
                start(testApp.getPackageName());
                viewsCoverageAlgorithm();
            } else {
                System.out.println("test time arrives, finished");
            }
        } catch (InterruptedException e) {
            doBusiness();
        }
    }

    public Monkey(App testApp) {
        super(testApp, "origin");
    }

    public void viewsCoverageAlgorithm() {
        int label = -1;
        while (true){
            long time = System.currentTimeMillis() - Experiment.beforeCycle;
            if(!isAlive()) {
                try {
                    System.out.println(getClass().getSimpleName() + " is not running, restart it");
                    start(testApp.getPackageName());
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (time/12000 >= label) {
                label++;
                try {
                    float mcov = testApp.getMethodCoverage();
                    System.out.println("Time: " + Long.toString(time));
                    System.out.printf("coverage rate of methods: %f %n", mcov);
                    testApp.saveStatus(Experiment.tcpPath + "\\result\\" + testApp.getJarName() + "-" + getClass().getSimpleName() + "-" + mode + ".txt", "Time: " + Long.toString(System.currentTimeMillis() - Experiment.beforeCycle) + "\r\ncoverage rate of methods: " + Float.toString(mcov) + "\r\n");
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            if( isTimeArrive()) {
                System.out.println("finished!");
                break;
            }

        }
    }

    /**
     *  restart a new monkey thread after the observed thread terminate unexpectedly.
     * @param o
     * @param arg
     */
    @Override
    public void update(Observable o, Object arg) {
        System.out.println(getClass().getSimpleName() + " thread exit");
        if (isTimeArrive()) {
            if (isAlive()) kill();
            System.out.println("test time arrives, finished");
        } else {
            Experiment.restartTime += 1;
            System.out.println("Restart time in update: " + Integer.toString(Experiment.restartTime));
            Monkey monkey = new Monkey(testApp);
            monkey.addObserver(this);
            new Thread(monkey).start();
            System.out.println("did not reach the test time, " + getClass().getSimpleName() + " restart");
        }
    }

    @Override
    public void start(String packageName) throws InterruptedException {
        ShellUtils.runShell("adb shell monkey --ignore-security-exceptions --ignore-timeouts --ignore-crashes --throttle 250 -p " + packageName +" 40000000", 3, true);
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
