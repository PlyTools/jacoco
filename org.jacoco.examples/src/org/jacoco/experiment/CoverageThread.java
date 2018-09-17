/*******************************************************************************
 * Copyright (c) 2009, 2018 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Brock Janiczak - initial API and implementation
 *
 *******************************************************************************/

package org.jacoco.experiment;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CoverageThread extends Thread {
    private final static String TAG = CoverageThread.class.getSimpleName();
    private static HashMap<String, InputStream> originClasses = new HashMap<String, InputStream>();
    private static HashMap<String, Boolean> methodCov = new HashMap<String, Boolean>();
    private static int mTotal;
    private static CoverageThread singletonCT;
    private static int operateMode;
    private static Logger logger = Logger.getLogger(CoverageThread.class.getSimpleName());

    private OutputStream out;
    private InputStream in;

    @Override
    public void run() {
        super.run();
        if (operateMode == 0) {
            while (true) {
                long startTime = System.currentTimeMillis();
                try {
                    sendData("dump".getBytes());
                    byte[] covData = receiveData();
                    Thread.sleep(5000);
//                    InputStream in = new ByteArrayInputStream(covData);
//                    CovStatus covStatus = getCoverage(in);
//                    System.out.println("Coverage State: " + covStatus.getmCovered() + "/" + covStatus.getmTotal());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                long endTime = System.currentTimeMillis();
                logger.log(Level.INFO, "Cost Time: " + Long.toString(endTime - startTime));
            }
        }

    }

    public void sendData(byte[] data) {
        try {
            if (out != null) {
                out.write(ByteUtil.intToBytes(data.length));
                out.write(data);
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public byte[] receiveData() {
        logger.log(Level.INFO, "Receive execution data from sdk/file");

        byte[] datalength = recvBuffer(4);
        int length = ByteUtil.bytesToInt(datalength);

        byte[] data = null;
        if (length != 0) {
            data = recvBuffer(length);
        }
        logger.log(Level.FINE, "Success to receive execution data, data size: " + Integer.toString(length));
        return data;
    }

    public byte[] recvBuffer(int totalSize) {
        try {
            // prepare enough buffer size
            byte[] result = new byte[totalSize];

            // receive data
            int transferSize = 0;
            while (transferSize != totalSize)
                transferSize += in.read(result, transferSize, totalSize - transferSize);

            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public CovStatus getCoverage(InputStream in) {
        final ExecutionDataReader reader = new ExecutionDataReader(in);
        final ExecutionDataStore executionData = new ExecutionDataStore();
        final SessionInfoStore sessionInfos = new SessionInfoStore();
        reader.setSessionInfoVisitor(sessionInfos);
        reader.setExecutionDataVisitor(executionData);
        try {
            reader.read();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
        for (Map.Entry entry:originClasses.entrySet()) {
            try {
                String name = (String)entry.getKey();
                InputStream ins = (InputStream)entry.getValue();
                analyzer.analyzeClass(ins, name);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        // Let's dump some metrics and line coverage information:
        mTotal = 0;
        for (final IClassCoverage cc : coverageBuilder.getClasses()) {
            mTotal += cc.getMethods().size();
            for (final IMethodCoverage mc: cc.getMethods()) {
                if (mc.getLineCounter().getCoveredCount() > 0 && !methodCov.containsKey(mc.getName())) {
                    methodCov.put(mc.getName(), true);
                }
            }
        }

        return new CovStatus(methodCov.size(), mTotal);
    }


}

class CovStatus {
    private int mCovered;
    private int mTotal;

    public CovStatus(int mCovered, int mTotal) {
        this.mCovered = mCovered;
        this.mTotal = mTotal;
    }

    public int getmCovered() {
        return mCovered;
    }

    public int getmTotal() {
        return mTotal;
    }

    public void setmCovered(int mCovered) {
        this.mCovered = mCovered;
    }

    public void setmTotal(int mTotal) {
        this.mTotal = mTotal;
    }
}