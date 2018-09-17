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
import java.util.Observer;

public abstract class AppTestTool extends Observable implements Runnable, Observer {
    protected String mode;
    protected App testApp;

    public AppTestTool(App testApp, String mode) {
        this.testApp = testApp;
        this.mode = mode;
    }

    public void doBusiness() {
        super.setChanged();
        notifyObservers();
    }

    public boolean isTimeArrive() {
        return System.currentTimeMillis() - Experiment.beforeCycle >= Experiment.testTime*3600*1000;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setTestApp(App testApp) {
        this.testApp = testApp;
    }

    abstract public void start(String packageName) throws InterruptedException;
    abstract public void kill();
    abstract public boolean isAlive();

}
