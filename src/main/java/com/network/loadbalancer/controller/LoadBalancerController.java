package com.network.loadbalancer.controller;

import com.network.loadbalancer.model.BackendServer;
import com.network.loadbalancer.service.BackendServerService;
import com.network.loadbalancer.service.TrafficAnalyticsService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/lb")
public class LoadBalancerController {

    private final BackendServerService service;
    private final TrafficAnalyticsService analytics;
    private final RestTemplate restTemplate = new RestTemplate();

    public LoadBalancerController(
            BackendServerService service,
            TrafficAnalyticsService analytics) {
        this.service = service;
        this.analytics = analytics;
    }

    @GetMapping("/hello")
    public ResponseEntity<?> forward(
            @RequestParam(defaultValue = "round-robin") String strategy) {

        long startTime = System.currentTimeMillis();
        analytics.recordRequest();

        List<String> attempted = new ArrayList<>();

        for (int i = 0; i < service.getServers().size(); i++) {

            BackendServer server = service.nextServer(strategy, attempted);

            if (server == null) {
                long responseTime = System.currentTimeMillis() - startTime;
                analytics.recordFailure(responseTime);

                return ResponseEntity.status(503)
                        .body(Map.of("error", "No healthy backend servers"));
            }

            attempted.add(server.getName());

            analytics.recordBackendAttempt(server.getName());

            server.beginConnection();

            try {
                server.incrementRequests();

                ResponseEntity<String> response =
                        restTemplate.getForEntity(
                                server.getUrl() + "/hello",
                                String.class);

                long responseTime = System.currentTimeMillis() - startTime;

                analytics.recordSuccess(server.getName(), responseTime);

                return ResponseEntity.status(response.getStatusCode())
                        .header("X-Backend-Server", server.getName())
                        .header("X-LB-Strategy", strategy)
                        .body(response.getBody());

            } catch (Exception e) {

                server.setHealthy(false);
                analytics.recordBackendFailure(server.getName());

            } finally {
                server.endConnection();
            }
        }

        long responseTime = System.currentTimeMillis() - startTime;
        analytics.recordFailure(responseTime);

        return ResponseEntity.status(502)
                .body(Map.of(
                        "error", "All backends failed",
                        "attempted", attempted));
    }

    @GetMapping("/servers")
    public List<Map<String, Object>> servers() {

        return service.getServers().stream().map(server -> {
            Map<String, Object> result = new LinkedHashMap<>();

            result.put("name", server.getName());
            result.put("url", server.getUrl());
            result.put("healthy", server.isHealthy());
            result.put("requestCount", server.getRequestCount());
            result.put("activeConnections", server.getActiveConnections());

            return result;
        }).toList();
    }

    @GetMapping("/health-check")
    public List<Map<String, Object>> healthCheck() {

        return service.getServers().stream().map(server -> {
            boolean healthy;

            try {
                restTemplate.getForEntity(
                        server.getUrl() + "/health",
                        String.class);
                healthy = true;

            } catch (Exception e) {
                healthy = false;
            }

            server.setHealthy(healthy);

            return Map.<String, Object>of(
                    "server", server.getName(),
                    "healthy", healthy);
        }).toList();
    }

    @PostMapping("/servers/{name}/simulate")
    public ResponseEntity<?> simulate(
            @PathVariable String name,
            @RequestParam boolean healthy) {

        BackendServer server = service.findByName(name);

        if (server == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String url = server.getUrl()
                    + "/simulate?healthy=" + healthy;

            restTemplate.getForEntity(url, String.class);

            server.setHealthy(healthy);

            return ResponseEntity.ok(Map.of(
                    "server", server.getName(),
                    "healthy", server.isHealthy(),
                    "message", healthy
                            ? "Backend restored successfully"
                            : "Backend failure simulated successfully"));

        } catch (Exception e) {

            server.setHealthy(false);

            return ResponseEntity.status(502).body(Map.of(
                    "server", server.getName(),
                    "healthy", false,
                    "error", "Unable to connect to backend simulator"));
        }
    }

    // Module 11: Traffic Analytics
    @GetMapping("/analytics")
    public Map<String, Object> getAnalytics() {
        return analytics.getAnalytics();
    }
}