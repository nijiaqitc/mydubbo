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
package com.njq.mydubbo.rpc.dubbo;

import com.njq.mydubbo.common.URL;
import com.njq.mydubbo.common.config.ConfigurationUtils;
import com.njq.mydubbo.common.utils.AtomicPositiveInteger;
import com.njq.mydubbo.remoting.api.Constants;
import com.njq.mydubbo.remoting.api.RemotingException;
import com.njq.mydubbo.remoting.api.TimeoutException;
import com.njq.mydubbo.remoting.api.exchange.ExchangeClient;
import com.njq.mydubbo.rpc.api.AppResponse;
import com.njq.mydubbo.rpc.api.AsyncRpcResult;
import com.njq.mydubbo.rpc.api.Invocation;
import com.njq.mydubbo.rpc.api.Invoker;
import com.njq.mydubbo.rpc.api.Result;
import com.njq.mydubbo.rpc.api.RpcContext;
import com.njq.mydubbo.rpc.api.RpcException;
import com.njq.mydubbo.rpc.api.RpcInvocation;
import com.njq.mydubbo.rpc.api.protocol.AbstractInvoker;
import com.njq.mydubbo.rpc.api.support.RpcUtils;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import static com.njq.mydubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static com.njq.mydubbo.common.constants.CommonConstants.GROUP_KEY;
import static com.njq.mydubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static com.njq.mydubbo.common.constants.CommonConstants.PATH_KEY;
import static com.njq.mydubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static com.njq.mydubbo.common.constants.CommonConstants.VERSION_KEY;
import static com.njq.mydubbo.remoting.api.Constants.SENT_KEY;
import static com.njq.mydubbo.rpc.api.Constants.TOKEN_KEY;

/**
 * DubboInvoker
 */
public class DubboInvoker<T> extends AbstractInvoker<T> {

    private final ExchangeClient[] clients;

    private final AtomicPositiveInteger index = new AtomicPositiveInteger();

    private final String version;

    private final ReentrantLock destroyLock = new ReentrantLock();

    private final Set<Invoker<?>> invokers;

    public DubboInvoker(Class<T> serviceType, URL url, ExchangeClient[] clients) {
        this(serviceType, url, clients, null);
    }

    public DubboInvoker(Class<T> serviceType, URL url, ExchangeClient[] clients, Set<Invoker<?>> invokers) {
        super(serviceType, url, new String[]{INTERFACE_KEY, GROUP_KEY, TOKEN_KEY, TIMEOUT_KEY});
        this.clients = clients;
        // get version.
        this.version = url.getParameter(VERSION_KEY, "0.0.0");
        this.invokers = invokers;
    }

    @Override
    protected Result doInvoke(final Invocation invocation) throws Throwable {
        RpcInvocation inv = (RpcInvocation) invocation;
        final String methodName = RpcUtils.getMethodName(invocation);
        inv.setAttachment(PATH_KEY, getUrl().getPath());
        inv.setAttachment(VERSION_KEY, version);

        ExchangeClient currentClient;
        if (clients.length == 1) {
            currentClient = clients[0];
        } else {
            currentClient = clients[index.getAndIncrement() % clients.length];
        }
        try {
            boolean isOneway = RpcUtils.isOneway(getUrl(), invocation);
            int timeout = getUrl().getMethodParameter(methodName, TIMEOUT_KEY, DEFAULT_TIMEOUT);
            if (isOneway) {
                boolean isSent = getUrl().getMethodParameter(methodName, SENT_KEY, false);
                currentClient.send(inv, isSent);
                RpcContext.getContext().setFuture(null);
                return AsyncRpcResult.newDefaultAsyncResult(invocation);
            } else {
                AsyncRpcResult asyncRpcResult = new AsyncRpcResult(inv);
                CompletableFuture<Object> responseFuture = currentClient.request(inv, timeout);
                responseFuture.whenComplete((obj, t) -> {
                    if (t != null) {
                        asyncRpcResult.completeExceptionally(t);
                    } else {
                        asyncRpcResult.complete((AppResponse) obj);
                    }
                });
                RpcContext.getContext().setFuture(new FutureAdapter(asyncRpcResult));
                return asyncRpcResult;
            }
        } catch (TimeoutException e) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
        } catch (RemotingException e) {
            throw new RpcException(RpcException.NETWORK_EXCEPTION, "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        for (ExchangeClient client : clients) {
            if (client.isConnected() && !client.hasAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY)) {
                //cannot write == not Available ?
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        // in order to avoid closing a client multiple times, a counter is used in case of connection per jvm, every
        // time when client.close() is called, counter counts down once, and when counter reaches zero, client will be
        // closed.
        if (super.isDestroyed()) {
            return;
        } else {
            // double check to avoid dup close
            destroyLock.lock();
            try {
                if (super.isDestroyed()) {
                    return;
                }
                super.destroy();
                if (invokers != null) {
                    invokers.remove(this);
                }
                for (ExchangeClient client : clients) {
                    try {
                        client.close(ConfigurationUtils.getServerShutdownTimeout());
                    } catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                    }
                }

            } finally {
                destroyLock.unlock();
            }
        }
    }
}
