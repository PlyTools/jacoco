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

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Config {
    public String tcpPath;
    public String newMonkeyPath;
    public List<App> apps = new ArrayList<App>();

    public static Config getConfig() {
        Config config = new Config();
        File file = new File(".." + File.separatorChar + "config.json");
        try {
            String content= FileUtils.readFileToString(file,"UTF-8");
            JSONObject jsonObject=new JSONObject(content);
            config.tcpPath = jsonObject.getJSONObject("users").getJSONObject("Ren").getJSONObject("Windows").getString("path");
            config.newMonkeyPath = jsonObject.getJSONObject("users").getJSONObject("Ren").getJSONObject("Windows").getString("newMonkey");
            JSONArray apks = jsonObject.getJSONArray("apks");
            for (Object apk:apks) {
                config.apps.add(new App(((JSONObject)apk).getString("apk"), ((JSONObject)apk).getString("package"),config.tcpPath + File.separator + "test-pool" + File.separator + "decompiled" + File.separator + ((JSONObject)apk).getString("apk") + ".jar"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }
}
