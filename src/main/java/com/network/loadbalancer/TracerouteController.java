
package com.network.loadbalancer;

import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/network")
public class TracerouteController {

    @GetMapping("/traceroute")
    public Map<String, Object> traceroute(
            @RequestParam String host) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (host == null || host.isBlank()) {
            result.put("status", "Invalid hostname");
            result.put("message", "Please enter a hostname.");
            return result;
        }

        host = host.trim();
        result.put("host", host);

        if (!host.matches("[a-zA-Z0-9.-]+")
                || host.startsWith(".")
                || host.endsWith(".")
                || host.contains("..")) {

            result.put("status", "Invalid hostname");
            result.put("message", "Enter a valid hostname.");
            return result;
        }

        Process process = null;

        try {
            String os = System.getProperty("os.name")
                    .toLowerCase(Locale.ROOT);

            ProcessBuilder builder;

            if (os.contains("win")) {
                builder = new ProcessBuilder(
                        "tracert", "-d", "-h", "12",
                        "-w", "1000", host
                );
            } else {
                builder = new ProcessBuilder(
                        "traceroute", "-n", "-m", "12",
                        "-w", "1", host
                );
            }

            builder.redirectErrorStream(true);

            long startTime = System.currentTimeMillis();

            process = builder.start();

            boolean completed = process.waitFor(
                    20, TimeUnit.SECONDS
            );

            if (!completed) {
                process.destroyForcibly();

                result.put("status", "Traceroute timed out");
                result.put("message",
                        "The traceroute operation exceeded 20 seconds.");
                return result;
            }

            List<String> hops = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            process.getInputStream()))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    hops.add(line);
                }
            }

            result.put("hops", hops);

            result.put("responseTimeMs",
                    System.currentTimeMillis() - startTime);

            result.put("status",
                    process.exitValue() == 0
                            ? "Success"
                            : "Completed with warnings");

        } catch (java.io.IOException e) {

            result.put("status", "Traceroute unavailable");
            result.put("message",
                    "Traceroute utility is not installed or accessible "
                    + "on this server.");

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            if (process != null) {
                process.destroyForcibly();
            }

            result.put("status", "Traceroute interrupted");

        } catch (Exception e) {

            if (process != null) {
                process.destroyForcibly();
            }

            result.put("status", "Traceroute failed");
            result.put("message", e.getMessage());
        }

        return result;
    }
}
