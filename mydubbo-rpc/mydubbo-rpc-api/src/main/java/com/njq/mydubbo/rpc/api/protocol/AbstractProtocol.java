/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.njq.mydubbo.rpc.api.protocol;

import com.njq.mydubbo.common.URL;
import com.njq.mydubbo.common.constants.CommonConstants;
import com.njq.mydubbo.common.logger.Logger;
import com.njq.mydubbo.common.logger.LoggerFactory;
import com.njq.mydubbo.common.utils.ConcurrentHashSet;
import com.njq.mydubbo.rpc.api.Exporter;
import com.njq.mydubbo.rpc.api.Invoker;
import com.njq.mydubbo.rpc.api.Protocol;
import com.njq.mydubbo.rpc.api.RpcException;
import com.njq.mydubbo.rpc.api.support.ProtocolUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.njq.mydubbo.remoting.api.Constants.BIND_PORT_KEY;

/**
 * abstract ProtocolSupport.
 */
public abstract class AbstractProtocol implements Protocol {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<String, Exporter<?>>();

    //TODO SoftReference
    protected final Set<Invoker<?>> invokers = new ConcurrentHashSet<>();

    protected static String serviceKey(URL url) {
        int port = url.getParameter(BIND_PORT_KEY, url.getPort());
        return serviceKey(port, url.getPath(), url.getParameter(CommonConstants.VERSION_KEY), url.getParameter(CommonConstants.GROUP_KEY));
    }

    protected static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        return ProtocolUtils.serviceKey(port, serviceName, serviceVersion, serviceGroup);
    }

    @Override
    public void destroy() {
        for (Invoker<?> invoker : invokers) {
            if (invoker != null) {
                invokers.remove(invoker);
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Destroy reference: " + invoker.getUrl());
                    }
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
        for (String key : new ArrayList<String>(exporterMap.keySet())) {
            Exporter<?> exporter = exporterMap.remove(key);
            if (exporter != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Unexport service: " + exporter.getInvoker().getUrl());
                    }
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        return new AsyncToSyncInvoker<>(protocolBindingRefer(type, url));
    }

    protected abstract <T> Invoker<T> protocolBindingRefer(Class<T> type, URL url) throws RpcException;
}
