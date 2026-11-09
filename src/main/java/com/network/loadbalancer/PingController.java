
package com.network.loadbalancer;

import org.springframework.web.bind.annotation.*;
import java.net.InetAddress;
import java.net.URI;
import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/network")
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping(
            @RequestParam String host) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (host == null || !host.matches(
                "[a-zA-Z0-9.-]{1,253}")) {
            result.put("status", "Failed");
            result.put("message", "Invalid hostname");
            return result;
        }

        try {
            InetAddress address = InetAddress.getByName(host);

            URI uri = new URI("https", null, host,
                    -1, "/", null, null);

            HttpURLConnection connection =
                    (HttpURLConnection) uri.toURL().openConnection();

            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(true);

            long start = System.nanoTime();

            int statusCode = connection.getResponseCode();

            long end = System.nanoTime();

            double latency =
                    (end - start) / 1_000_000.0;

            result.put("host", host);
            result.put("ipAddress", address.getHostAddress());
            result.put("reachable", true);
            result.put("httpStatusCode", statusCode);
            result.put("responseTimeMs",
                    Math.round(latency * 100.0) / 100.0);
            result.put("status", "Success");

            connection.disconnect();

        } catch (Exception e) {

            result.put("host", host);
            result.put("reachable", false);
            result.put("status", "Failed");
            result.put("message", e.getMessage());
        }

        return result;
    }
}
