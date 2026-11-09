
package com.network.loadbalancer;

import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/network")
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping(
            @RequestParam String host) {

        Map<String, Object> result = new LinkedHashMap<>();

        // Validate the hostname or IP address
        if (host == null || !host.matches(
                "[a-zA-Z0-9.-]{1,253}")) {
            result.put("status", "Failed");
            result.put("message", "Invalid hostname");
            return result;
        }

        try {
            InetAddress address = InetAddress.getByName(host);

            boolean windows = System.getProperty("os.name")
                    .toLowerCase().contains("win");

            List<String> command = windows
                    ? Arrays.asList("ping", "-n", "4",
                        "-w", "2000", address.getHostAddress())
                    : Arrays.asList("ping", "-c", "4",
                        "-W", "2", address.getHostAddress());

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            StringBuilder output = new StringBuilder();

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(
                            process.getInputStream()))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(
                    12, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                result.put("status", "Timeout");
                result.put("message", "Ping timed out");
                return result;
            }

            result.put("host", host);
            result.put("ipAddress", address.getHostAddress());
            result.put("pingOutput", output.toString());
            result.put("status",
                    process.exitValue() == 0
                            ? "Success" : "Unreachable");

        } catch (Exception e) {
            result.put("host", host);
            result.put("status", "Failed");
            result.put("message", e.getMessage());
        }

        return result;
    }
}
