
package com.network.loadbalancer;

import org.springframework.web.bind.annotation.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/network")
public class NetworkDiagnosticsController {

    @GetMapping("/dns")
    public Map<String, Object> dnsLookup(
            @RequestParam String domain) {

        Map<String, Object> result = new LinkedHashMap<>();

        // Remove leading and trailing spaces
        String cleanedDomain = domain == null
                ? ""
                : domain.trim();

        result.put("domain", cleanedDomain);

        // Validate domain format
        String domainPattern =
                "(?i)^(?=.{1,253}$)"
                + "([a-z0-9]"
                + "([a-z0-9-]{0,61}[a-z0-9])?\\.)+"
                + "[a-z]{2,63}\\.?$";

        if (cleanedDomain.isEmpty()
                || !cleanedDomain.matches(domainPattern)) {

            result.put("status", "Failed");
            result.put("message",
                    "Invalid domain name. Example: google.com");
            return result;
        }

        // Remove optional final dot
        if (cleanedDomain.endsWith(".")) {
            cleanedDomain = cleanedDomain.substring(
                    0, cleanedDomain.length() - 1);
        }

        try {
            InetAddress[] addresses =
                    InetAddress.getAllByName(cleanedDomain);

            String[] ipAddresses =
                    new String[addresses.length];

            for (int i = 0; i < addresses.length; i++) {
                ipAddresses[i] =
                        addresses[i].getHostAddress();
            }

            result.put("domain", cleanedDomain);
            result.put("ipAddresses", ipAddresses);
            result.put("status", "Success");

        } catch (UnknownHostException e) {

            result.put("status", "Failed");
            result.put("message",
                    "Domain could not be resolved");

        } catch (Exception e) {

            result.put("status", "Failed");
            result.put("message",
                    "DNS lookup failed: " + e.getMessage());
        }

        return result;
    }
}
