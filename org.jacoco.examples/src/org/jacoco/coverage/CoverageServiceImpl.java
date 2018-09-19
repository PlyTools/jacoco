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
package org.jacoco.coverage;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CoverageServiceImpl implements CoverageService.Iface{
    private static Logger logger = Logger.getLogger(CoverageServiceImpl.class.getSimpleName());
    private static Map<String, HashMap<String, Boolean>> appCovRecord = new HashMap<>();

    public static void main(String[] args) {
        try {
            FileInputStream fis = new FileInputStream(new File("D:\\SourceCode\\Java\\backend\\covData.exec"));
            CoverageServiceImpl csi = new CoverageServiceImpl();
            CovStatus cs = csi.getCoverage("D:\\SourceCode\\Java\\InfoMe\\app\\tools\\origin\\debug.jar", fis, true);
            System.out.println(cs.getMCovered());
            System.out.println(cs.getMTotal());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public CovStatus getCoverage(String jarPath, InputStream in, boolean withHistory) {
        HashMap<String, Boolean> methodCov = getCovRecord(jarPath, withHistory);
        logger.info("Compute coverage by RPC");

        final ExecutionDataReader reader = new ExecutionDataReader(in);
        final ExecutionDataStore execData = new ExecutionDataStore();
        final SessionInfoStore sessionInfos = new SessionInfoStore();

        reader.setSessionInfoVisitor(sessionInfos);
        reader.setExecutionDataVisitor(execData);

        try {
            reader.read();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(execData, coverageBuilder);

        File jarFile = new File(jarPath);
        try {
            analyzer.analyzeAll(jarFile);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // Let's dump some metrics and line coverage information:
        int mTotal = 0;
        for (final IClassCoverage cc : coverageBuilder.getClasses()) {
            for (final IMethodCoverage mc: cc.getMethods()) {
                mTotal++;
                if (mc.getLineCounter().getCoveredCount() > 0 ) {
                    methodCov.put(cc.getName()+ "_" + mc.getName(), true);
                    System.out.println(cc.getName()+ "_" + mc.getName());
                }
            }
        }

        return new CovStatus(methodCov.size(), mTotal);
    }

    @Override
    public CovStatus getCoverage(String jarPath, ByteBuffer executionData, boolean withHistory) {
        HashMap<String, Boolean> methodCov = getCovRecord(jarPath, withHistory);
        logger.info("Compute coverage by RPC");

        InputStream in = new ByteArrayInputStream(executionData.array());
        final ExecutionDataReader reader = new ExecutionDataReader(in);
        final ExecutionDataStore execData = new ExecutionDataStore();
        final SessionInfoStore sessionInfos = new SessionInfoStore();

        reader.setSessionInfoVisitor(sessionInfos);
        reader.setExecutionDataVisitor(execData);

        try {
            reader.read();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(execData, coverageBuilder);

        File jarFile = new File(jarPath);
        try {
            analyzer.analyzeAll(jarFile);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // Let's dump some metrics and line coverage information:
        int mTotal = 0;
        for (final IClassCoverage cc : coverageBuilder.getClasses()) {
            for (final IMethodCoverage mc: cc.getMethods()) {
                mTotal++;
                if (mc.getLineCounter().getCoveredCount() > 0 ) {
                    methodCov.put(cc.getName()+ "_" + mc.getName(), true);
                    System.out.println(cc.getName()+ "_" + mc.getName());
                }
            }
        }

        return new CovStatus(methodCov.size(), mTotal);
    }

    public HashMap<String, Boolean> getCovRecord(String jarpath, boolean withHistory) {
        HashMap<String, Boolean> newRecord = new HashMap<>();
        if (withHistory) {
            if (appCovRecord.containsKey(jarpath)) {
                newRecord = appCovRecord.get(jarpath);
            } else {
                appCovRecord.put(jarpath, newRecord);
            }
        }
        return newRecord;
    }
}
