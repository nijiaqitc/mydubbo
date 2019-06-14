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
package com.njq.mydubbo.config.api;

import com.njq.mydubbo.common.logger.Logger;
import com.njq.mydubbo.common.logger.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The shutdown hook thread to do the clean up stuff.
 * This is a singleton in order to ensure there is only one shutdown hook registered.
 * Because {@link ApplicationShutdownHooks} use {@link java.util.IdentityHashMap}
 * to store the shutdown hooks.
 */
public class DubboShutdownHook extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(DubboShutdownHook.class);

    private static final DubboShutdownHook DUBBO_SHUTDOWN_HOOK = new DubboShutdownHook("DubboShutdownHook");
    /**
     * Has it already been registered or not?
     */
    private final AtomicBoolean registered = new AtomicBoolean(false);
    /**
     * Has it already been destroyed or not?
     */
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    private DubboShutdownHook(String name) {
        super(name);
    }

    public static DubboShutdownHook getDubboShutdownHook() {
        return DUBBO_SHUTDOWN_HOOK;
    }

    @Override
    public void run() {
        if (logger.isInfoEnabled()) {
            logger.info("Run shutdown hook now.");
        }
        doDestroy();
    }

    /**
     * Register the ShutdownHook
     */
    public void register() {
        if (!registered.get() && registered.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(getDubboShutdownHook());
        }
    }

    /**
     * Unregister the ShutdownHook
     */
    public void unregister() {
        if (registered.get() && registered.compareAndSet(true, false)) {
            Runtime.getRuntime().removeShutdownHook(getDubboShutdownHook());
        }
    }

    /**
     * Destroy all the resources, including registries and protocols.
     */
    public void doDestroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        // destroy all the registries
        // destroy all the protocols
        destroyProtocols();
    }

    /**
     * Destroy all the protocols.
     */
    private void destroyProtocols() {

    }


}
