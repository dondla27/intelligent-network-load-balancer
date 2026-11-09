
package com.network.loadbalancer;

import org.springframework.web.bind.annotation.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/network")
public class NetworkDiagnosticsController {

    @GetMapping("/dns")
    public Map<String, Object> dnsLookup(
            @RequestParam String domain) {

        Map<String, Object> result = new HashMap<>();

        try {
            InetAddress[] addresses =
                    InetAddress.getAllByName(domain);

            String[] ipAddresses = new String[addresses.length];

            for (int i = 0; i < addresses.length; i++) {
                ipAddresses[i] = addresses[i].getHostAddress();
            }

            result.put("domain", domain);
            result.put("ipAddresses", ipAddresses);
            result.put("status", "Success");

        } catch (UnknownHostException e) {
            result.put("domain", domain);
            result.put("status", "Failed");
            result.put("message", "Domain could not be resolved");
        }

        return result;
    }
}
