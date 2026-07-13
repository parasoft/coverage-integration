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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class ParasoftHeaderInjectingProxyTest
{
    @Test
    void forwardsRequestAndInjectsBaggageHeader() throws IOException
    {
        HttpServer target = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        target.createContext("/coverage", exchange -> {
            byte[] response = exchange.getRequestHeaders()
                    .getFirst(ParasoftHeaderInjectingProxy.BAGGAGE_HEADER_NAME)
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        target.start();

        try (ParasoftHeaderInjectingProxy coverageProxy =
                new ParasoftHeaderInjectingProxy("127.0.0.1", 0, "test-operator-id=user+parallel-1")) {
            Proxy javaProxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(coverageProxy.getHost(), coverageProxy.getPort()));
            URI targetUri = URI.create("http://127.0.0.1:" + target.getAddress().getPort() + "/coverage");
            HttpURLConnection connection = (HttpURLConnection) targetUri.toURL().openConnection(javaProxy);

            assertEquals(200, connection.getResponseCode());
            assertEquals("test-operator-id=user+parallel-1",
                    new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        }
        finally {
            target.stop(0);
        }
    }
}
