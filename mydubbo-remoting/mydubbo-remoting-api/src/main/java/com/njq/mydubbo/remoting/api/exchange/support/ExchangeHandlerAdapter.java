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
package com.njq.mydubbo.remoting.api.exchange.support;

import com.njq.mydubbo.remoting.api.RemotingException;
import com.njq.mydubbo.remoting.api.exchange.ExchangeChannel;
import com.njq.mydubbo.remoting.api.exchange.ExchangeHandler;
import com.njq.mydubbo.remoting.api.telnet.support.TelnetHandlerAdapter;

import java.util.concurrent.CompletableFuture;

/**
 * ExchangeHandlerAdapter
 */
public abstract class ExchangeHandlerAdapter extends TelnetHandlerAdapter implements ExchangeHandler {

    @Override
    public CompletableFuture<Object> reply(ExchangeChannel channel, Object msg) throws RemotingException {
        return null;
    }

}