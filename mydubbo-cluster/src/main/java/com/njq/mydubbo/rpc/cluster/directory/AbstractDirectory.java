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
package com.njq.mydubbo.rpc.cluster.directory;

import com.njq.mydubbo.common.URL;
import com.njq.mydubbo.common.logger.Logger;
import com.njq.mydubbo.common.logger.LoggerFactory;
import com.njq.mydubbo.common.utils.StringUtils;
import com.njq.mydubbo.rpc.api.Invocation;
import com.njq.mydubbo.rpc.api.Invoker;
import com.njq.mydubbo.rpc.api.RpcException;
import com.njq.mydubbo.rpc.cluster.Directory;
import com.njq.mydubbo.rpc.cluster.Router;
import com.njq.mydubbo.rpc.cluster.RouterChain;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.njq.mydubbo.common.constants.CommonConstants.MONITOR_KEY;
import static com.njq.mydubbo.common.constants.RegistryConstants.REGISTRY_PROTOCOL;
import static com.njq.mydubbo.rpc.cluster.Constants.REFER_KEY;

/**
 * Abstract implementation of Directory: Invoker list returned from this Directory's list method have been filtered by Routers
 */
public abstract class AbstractDirectory<T> implements Directory<T> {

    // logger
    private static final Logger logger = LoggerFactory.getLogger(AbstractDirectory.class);

    private final URL url;

    private volatile boolean destroyed = false;

    private volatile URL consumerUrl;

    protected RouterChain<T> routerChain;

    public AbstractDirectory(URL url) {
        this(url, null);
    }

    public AbstractDirectory(URL url, RouterChain<T> routerChain) {
        this(url, url, routerChain);
    }

    public AbstractDirectory(URL url, URL consumerUrl, RouterChain<T> routerChain) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }

        if (url.getProtocol().equals(REGISTRY_PROTOCOL)) {
            Map<String, String> queryMap = StringUtils.parseQueryString(url.getParameterAndDecoded(REFER_KEY));
            this.url = url.addParameters(queryMap).removeParameter(MONITOR_KEY);
        } else {
            this.url = url;
        }

        this.consumerUrl = consumerUrl;
        setRouterChain(routerChain);
    }

    @Override
    public List<Invoker<T>> list(Invocation invocation) throws RpcException {
        if (destroyed) {
            throw new RpcException("Directory already destroyed .url: " + getUrl());
        }

        return doList(invocation);
    }

    @Override
    public URL getUrl() {
        return url;
    }

    public RouterChain<T> getRouterChain() {
        return routerChain;
    }

    public void setRouterChain(RouterChain<T> routerChain) {
        this.routerChain = routerChain;
    }

    protected void addRouters(List<Router> routers) {
        routers = routers == null ? Collections.emptyList() : routers;
        routerChain.addRouters(routers);
    }

    public URL getConsumerUrl() {
        return consumerUrl;
    }

    public void setConsumerUrl(URL consumerUrl) {
        this.consumerUrl = consumerUrl;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public void destroy() {
        destroyed = true;
    }

    protected abstract List<Invoker<T>> doList(Invocation invocation) throws RpcException;

}
