package com.njq.mydubbo.config.api;

import com.njq.mydubbo.config.api.support.Parameter;

import java.util.Map;

/**
 * @author: nijiaqi
 * @date: 2019/6/10
 */
public abstract class AbstractMethodConfig extends AbstractConfig{
    /**
     * The timeout for remote invocation in milliseconds
     */
    protected Integer timeout;

    /**
     * The retry times
     */
    protected Integer retries;

    /**
     * max concurrent invocations
     */
    protected Integer actives;

    /**
     * The load balance
     */
    protected String loadbalance;

    /**
     * Whether to async
     * note that: it is an unreliable asynchronism that ignores return values and does not block threads.
     */
    protected Boolean async;

    /**
     * Whether to ack async-sent
     */
    protected Boolean sent;

    /**
     * The name of mock class which gets called when a service fails to execute
     *
     * note that: the mock doesn't support on the provider side，and the mock is executed when a non-business exception
     * occurs after a remote service call
     */
    protected String mock;

    /**
     * Merger
     */
    protected String merger;

    /**
     * Cache the return result with the call parameter as key, the following options are available: lru, threadlocal,
     * jcache, etc.
     */
    protected String cache;

    /**
     * Whether JSR303 standard annotation validation is enabled or not, if enabled, annotations on method parameters will
     * be validated
     */
    protected String validation;

    /**
     * The customized parameters
     */
    protected Map<String, String> parameters;

    /**
     * Forks for forking cluster
     */
    protected Integer forks;

    public Integer getForks() {
        return forks;
    }

    public void setForks(Integer forks) {
        this.forks = forks;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public String getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
    }

    public Boolean isAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public Integer getActives() {
        return actives;
    }

    public void setActives(Integer actives) {
        this.actives = actives;
    }

    public Boolean getSent() {
        return sent;
    }

    public void setSent(Boolean sent) {
        this.sent = sent;
    }

    @Parameter(escaped = true)
    public String getMock() {
        return mock;
    }

    public void setMock(String mock) {
        if (mock == null) {
            return;
        }


        this.mock = mock;
    }

    public void setMock(Boolean mock) {
        if (mock == null) {
            setMock((String) null);
        } else {
            setMock(mock.toString());
        }
    }

    public String getMerger() {
        return merger;
    }

    public void setMerger(String merger) {
        this.merger = merger;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        checkParameterName(parameters);
        this.parameters = parameters;
    }
}
