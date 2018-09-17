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

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;

import java.util.logging.Logger;

public class CoverageServer {
    
    private static final Logger logger = Logger.getLogger(CoverageServer.class.getName());

    public static void main(String[] args) {
        try {

            TServerSocket serverTransport = new TServerSocket(9090);
            TBinaryProtocol.Factory proFactory = new TBinaryProtocol.Factory();

            /**
             * 关联处理器与CoverageService服务实现
             */
            TProcessor processor = new CoverageService.Processor(new CoverageServiceImpl());

            TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
            serverArgs.processor(processor);
            serverArgs.protocolFactory(proFactory);
            TServer server = new TThreadPoolServer(serverArgs);
            logger.info("Start server on port 9090...");

            server.serve();
        } catch (TTransportException e) {
            e.printStackTrace();
        }
    }
}
