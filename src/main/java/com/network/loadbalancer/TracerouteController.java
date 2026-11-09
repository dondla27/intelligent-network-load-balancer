
package com.network.loadbalancer;

import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@RestController
@RequestMapping("/api/network")
public class TracerouteController {

    @GetMapping("/traceroute")
    public Map<String, Object> traceroute(
            @RequestParam String host) {

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("host", host);

        // Basic hostname validation
        if (host == null || host.isBlank()
                || !host.matches("[a-zA-Z0-9.-]+")) {
            result.put("status", "Invalid hostname");
            return result;
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();

            ProcessBuilder builder;

            if (os.contains("win")) {
                builder = new ProcessBuilder(
                    "tracert", "-d", "-h", "12", "-w", "1000", host
                );
            } else {
                builder = new ProcessBuilder(
                    "traceroute", "-n", "-m", "12", "-w", "1", host
                );
            }

            builder.redirectErrorStream(true);

            Process process = builder.start();

            List<String> hops = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    hops.add(line);
                }
            }

            boolean completed = process.waitFor(
                20, java.util.concurrent.TimeUnit.SECONDS
            );

            if (!completed) {
                process.destroyForcibly();
                result.put("status", "Traceroute timed out");
                result.put("hops", hops);
                return result;
            }

            result.put("hops", hops);
            result.put("status",
                process.exitValue() == 0
                    ? "Success"
                    : "Completed with warnings");

        } catch (Exception e) {
            result.put("status", "Traceroute failed");
            result.put("message", e.getMessage());
        }

        return result;
    }
}

