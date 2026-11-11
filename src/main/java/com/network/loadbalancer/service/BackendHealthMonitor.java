
package com.network.loadbalancer.service;

import com.network.loadbalancer.model.BackendServer;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
public class BackendHealthMonitor {

    private final BackendServerService backendServerService;
    private final RestTemplate restTemplate;

    public BackendHealthMonitor(BackendServerService backendServerService) {
        this.backendServerService = backendServerService;

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(1500);
        factory.setReadTimeout(1500);

        this.restTemplate = new RestTemplate(factory);
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 2500)
    public void monitorBackends() {

        for (BackendServer server : backendServerService.getServers()) {

            long startTime = System.nanoTime();

            try {
                ResponseEntity<String> response =
                        restTemplate.getForEntity(
                                server.getUrl() + "/health",
                                String.class
                        );

                double responseTime =
                        (System.nanoTime() - startTime) / 1_000_000.0;

                boolean wasHealthy = server.isHealthy();
                boolean isHealthy = response.getStatusCode().is2xxSuccessful();

                server.setHealthy(isHealthy);
                server.setHealthResponseTimeMs(
                        Math.round(responseTime * 100.0) / 100.0
                );
                server.setLastHealthCheckAt(Instant.now().toString());

                if (wasHealthy != isHealthy) {
                    System.out.println(
                            "[HEALTH MONITOR] " + server.getName()
                                    + " status changed to "
                                    + (isHealthy ? "HEALTHY" : "UNHEALTHY")
                    );
                }

            } catch (Exception exception) {

                boolean wasHealthy = server.isHealthy();

                double responseTime =
                        (System.nanoTime() - startTime) / 1_000_000.0;

                server.setHealthy(false);
                server.setHealthResponseTimeMs(
                        Math.round(responseTime * 100.0) / 100.0
                );
                server.setLastHealthCheckAt(Instant.now().toString());

                if (wasHealthy) {
                    System.out.println(
                            "[HEALTH MONITOR] " + server.getName()
                                    + " is UNHEALTHY: "
                                    + exception.getMessage()
                    );
                }
            }
        }
    }
}
