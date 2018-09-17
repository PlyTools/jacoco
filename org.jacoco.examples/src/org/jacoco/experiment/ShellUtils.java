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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ShellUtils {
    public static String runShell(String cmd, int waitTime, boolean redirectIO) throws InterruptedException{
        ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
        if (redirectIO) {
            pb.inheritIO();
        }
        StringBuilder out = new StringBuilder();
        try {
            Process p = pb.start();

            if(waitTime == -1) {   //一直等待；
                p.waitFor();
            } else {
                p.waitFor(waitTime, TimeUnit.SECONDS);
            }
            // 获取标准输出
            BufferedReader readStdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = readStdout.readLine()) != null) {
                out.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw e;
        }
        return out.toString();
    }
}
