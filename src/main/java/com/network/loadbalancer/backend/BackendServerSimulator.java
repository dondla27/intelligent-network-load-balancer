
package com.network.loadbalancer.backend;

import com.sun.net.httpserver.HttpServer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
public class BackendServerSimulator {

    @Bean
    ApplicationRunner startBackendServers() {
        return args -> {
            startServer(8081, "Backend-1");
            startServer(8082, "Backend-2");
            startServer(8083, "Backend-3");
        };
    }

    private void startServer(int port, String name) throws Exception {

        HttpServer server = HttpServer.create(
                new InetSocketAddress(port), 0);

        AtomicBoolean healthy = new AtomicBoolean(true);

        server.createContext("/hello", exchange -> {

            if (!healthy.get()) {
                send(exchange, "Backend unavailable", 503);
                return;
            }

            String response = "{\"server\":\"" + name
                    + "\",\"message\":\"Hello from " + name
                    + "\",\"port\":" + port + "}";

            send(exchange, response, 200);
        });

        server.createContext("/health", exchange -> {

            if (healthy.get()) {
                send(exchange, "OK - " + name, 200);
            } else {
                send(exchange, "UNHEALTHY - " + name, 503);
            }
        });

        server.createContext("/simulate", exchange -> {

            String query = exchange.getRequestURI().getQuery();

            if (query != null && query.contains("healthy=false")) {
                healthy.set(false);
            } else if (query != null && query.contains("healthy=true")) {
                healthy.set(true);
            }

            send(exchange,
                    name + " healthy=" + healthy.get(),
                    200);
        });

        server.start();

        System.out.println(name + " started on port " + port);
    }

    private void send(
            com.sun.net.httpserver.HttpExchange exchange,
            String body,
            int status) throws java.io.IOException {

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain");

        exchange.sendResponseHeaders(status, bytes.length);

        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
