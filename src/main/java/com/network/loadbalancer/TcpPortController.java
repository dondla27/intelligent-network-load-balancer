package com.network.loadbalancer;

import org.springframework.web.bind.annotation.*;
import java.net.Socket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/network")
public class TcpPortController {

    @GetMapping("/tcp")
    public Map<String, Object> checkTcpPort(
            @RequestParam String host,
            @RequestParam int port) {

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        result.put("host", host);
        result.put("port", port);

        if (port < 1 || port > 65535) {
            result.put("status", "Invalid port");
            result.put("reachable", false);
            return result;
        }

        try (Socket socket = new Socket()) {

            InetAddress address = InetAddress.getByName(host);

            socket.connect(
                new java.net.InetSocketAddress(address, port),
                5000
            );

            long responseTime =
                    System.currentTimeMillis() - startTime;

            result.put("ipAddress", address.getHostAddress());
            result.put("reachable", true);
            result.put("responseTimeMs", responseTime);
            result.put("status", "Success");

        } catch (Exception e) {

            result.put("reachable", false);
            result.put("responseTimeMs",
                    System.currentTimeMillis() - startTime);
            result.put("status", "Connection failed");
            result.put("message", e.getMessage());
        }

        return result;
    }
}