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

import java.util.List;

/**
 * This example connects to a coverage agent that run in output mode
 * <code>tcpserver</code> and requests execution data. The collected data is
 * dumped to a local file.
 */
public final class Experiment {

	public static final String tcpPath = Config.getConfig().tcpPath;
	public static final String newMonkeyPath = Config.getConfig().newMonkeyPath;
	private final static List<App> apps = Config.getConfig().apps;

	public static App testApp;
	public static double testTime = 18;  						// hours
	final static String mode = "origin";
	public static long beforeCycle;
	public static int restartTime = -1;

	public static void main(final String[] args) {
		try {
			System.out.println("push automator into the device");
			ShellUtils.runShell("adb connect 127.0.0.1:62025", -1, true);
			ShellUtils.runShell("adb forward tcp:6300 tcp:6300", -1, true);
			ShellUtils.runShell("adb push " + newMonkeyPath + "\\bin\\monkey.jar /data/local/tmp", -1, true);
			ShellUtils.runShell("adb push " + newMonkeyPath + "\\monkey\\MonkeyScript.jar /data/local/tmp", -1, true);
			ShellUtils.runShell("adb push " + newMonkeyPath + "\\monkey /data/local/tmp", -1, true);
			ShellUtils.runShell("adb shell \"chmod 777 /data/local/tmp/monkey/monkey\"", -1, true);
			ShellUtils.runShell("adb shell \"chown root:root /data/local/tmp/monkey.jar\"", -1, true);
			ShellUtils.runShell("adb shell \"chown root:root /data/local/tmp/monkey/\"", -1, true);
			System.out.println("pushed");
		} catch (InterruptedException e) {
			System.err.println("push error");
		}

	    for(App app:apps) {
	    	if(app.getJarName().equals("itoday")) {
				testApp = app;
			}
		}

		testApp.initInfo();
		beforeCycle = System.currentTimeMillis();
		Monkey Monkey = new Monkey(testApp);
		Monkey.addObserver(Monkey);
		new Thread(Monkey).start();

//		Monkey monkey = new Monkey(testApp);
//		monkey.addObserver(monkey);
//		new Thread(monkey).start();null

	}

}

