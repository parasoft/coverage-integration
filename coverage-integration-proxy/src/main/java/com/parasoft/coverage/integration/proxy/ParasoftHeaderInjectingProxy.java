/*
 * Copyright 2026 Parasoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.parasoft.coverage.integration.proxy;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.parasoft.coverage.integration.core.internal.CoverageExecutionContext;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP proxy that injects the current Parasoft coverage {@code Baggage} header
 * into proxied browser requests.
 */
public final class ParasoftHeaderInjectingProxy
        implements AutoCloseable
{
    public static final String BAGGAGE_HEADER_NAME = "Baggage";
    public static final String DEFAULT_BIND_HOST = "127.0.0.1";

    private static final Logger LOGGER = LoggerFactory.getLogger(ParasoftHeaderInjectingProxy.class);

    private final AtomicReference<String> baggageHeader;
    private final HttpProxyServer proxy;

    /**
     * Starts a proxy bound to {@value #DEFAULT_BIND_HOST} on an auto-assigned
     * port and initializes the injected header from the current test thread.
     */
    public ParasoftHeaderInjectingProxy()
    {
        this(DEFAULT_BIND_HOST, 0, getCurrentTestBaggageHeader());
    }

    /**
     * Starts a proxy bound to {@value #DEFAULT_BIND_HOST} on an auto-assigned
     * port with a shared baggage reference.
     *
     * @param baggageHeader baggage value to inject, such as
     *        {@code test-operator-id=userId+parallelId}
     */
    public ParasoftHeaderInjectingProxy(AtomicReference<String> baggageHeader)
    {
        this(DEFAULT_BIND_HOST, 0, baggageHeader);
    }

    /**
     * Starts a proxy on the requested host and port with a shared baggage
     * reference.
     *
     * @param bindHost host/interface to bind
     * @param port port to bind, or {@code 0} for an auto-assigned port
     * @param baggageHeader baggage value to inject, such as
     *        {@code test-operator-id=userId+parallelId}
     */
    public ParasoftHeaderInjectingProxy(String bindHost, int port, AtomicReference<String> baggageHeader)
    {
        this.baggageHeader = Objects.requireNonNull(baggageHeader, "baggageHeader must not be null");
        this.proxy = startProxy(bindHost, port);
    }

    /**
     * Starts a proxy on the requested host and port.
     *
     * @param bindHost host/interface to bind
     * @param port port to bind, or {@code 0} for an auto-assigned port
     * @param baggageHeader baggage value to inject, such as
     *        {@code test-operator-id=userId+parallelId}
     */
    public ParasoftHeaderInjectingProxy(String bindHost, int port, String baggageHeader)
    {
        this(bindHost, port, new AtomicReference<>(baggageHeader));
    }

    /**
     * Refreshes the injected baggage value from the current test thread.
     */
    public void useCurrentTestBaggageHeader()
    {
        baggageHeader.set(getCurrentTestBaggageHeader());
    }

    /**
     * Sets the injected baggage value directly.
     *
     * @param baggageHeader baggage value to inject, or {@code null} to stop
     *        injecting the header
     */
    public void setBaggageHeader(String baggageHeader)
    {
        this.baggageHeader.set(baggageHeader);
    }

    /**
     * @return the current baggage value that will be injected
     */
    public String getBaggageHeader()
    {
        return baggageHeader.get();
    }

    /**
     * @return the running LittleProxy server
     */
    public HttpProxyServer getProxy()
    {
        return proxy;
    }

    /**
     * @return the host this proxy is bound to
     */
    public String getHost()
    {
        return proxy.getListenAddress().getHostString();
    }

    /**
     * @return the port this proxy is listening on
     */
    public int getPort()
    {
        return proxy.getListenAddress().getPort();
    }

    @Override
    public void close()
    {
        proxy.stop();
    }

    private HttpProxyServer startProxy(String bindHost, int port)
    {
        HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap()
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx)
                    {
                        return new HttpFiltersAdapter(originalRequest) {
                            @Override
                            public HttpResponse clientToProxyRequest(HttpObject httpObject)
                            {
                                if (httpObject instanceof HttpRequest request) {
                                    injectBaggageHeader(request);
                                }

                                return null;
                            }
                        };
                    }
                });

        InetSocketAddress bindAddress = createBindAddress(bindHost, port);
        if (bindAddress == null) {
            bootstrap.withPort(port);
        }
        else {
            bootstrap.withAddress(bindAddress);
        }

        HttpProxyServer server = bootstrap.start();
        LOGGER.debug("Started Parasoft header injecting proxy on {}", server.getListenAddress());

        return server;
    }

    private void injectBaggageHeader(HttpRequest request)
    {
        String value = baggageHeader.get();
        if (value != null && !value.isBlank()) {
            request.headers().set(BAGGAGE_HEADER_NAME, value);
        }
    }

    private static InetSocketAddress createBindAddress(String bindHost, int port)
    {
        if (bindHost == null || bindHost.isBlank()) {
            return null;
        }

        try {
            return new InetSocketAddress(InetAddress.getByName(bindHost), port);
        }
        catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to resolve proxy bind host: " + bindHost, e);
        }
    }

    private static String getCurrentTestBaggageHeader()
    {
        return CoverageExecutionContext.getCurrentBaggageHeader();
    }
}
