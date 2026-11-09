
package com.network.loadbalancer;

import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/network")
public class SubnetCalculatorController {

    @GetMapping("/subnet")
    public Map<String, Object> calculateSubnet(
            @RequestParam String ip,
            @RequestParam int cidr) {

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // Validate CIDR
            if (cidr < 0 || cidr > 32) {
                throw new IllegalArgumentException(
                        "CIDR must be between 0 and 32");
            }

            // Validate IPv4 address
            String[] parts = ip.split("\\.", -1);

            if (parts.length != 4) {
                throw new IllegalArgumentException(
                        "Invalid IPv4 address");
            }

            long ipNumber = 0;

            for (String part : parts) {
                if (part.isEmpty() || !part.matches("\\d{1,3}")) {
                    throw new IllegalArgumentException(
                            "Invalid IPv4 address");
                }

                int octet = Integer.parseInt(part);

                if (octet < 0 || octet > 255) {
                    throw new IllegalArgumentException(
                            "IPv4 octets must be between 0 and 255");
                }

                ipNumber = (ipNumber << 8) | octet;
            }

            // Calculate subnet mask
            long mask = cidr == 0
                    ? 0
                    : (0xFFFFFFFFL << (32 - cidr))
                    & 0xFFFFFFFFL;

            // Calculate network and broadcast addresses
            long network = ipNumber & mask;

            long broadcast = network
                    | (~mask & 0xFFFFFFFFL);

            // Calculate address counts
            long totalAddresses = 1L << (32 - cidr);

            long usableHosts;

            if (cidr == 32) {
                usableHosts = 1;
            } else if (cidr == 31) {
                usableHosts = 2;
            } else {
                usableHosts = totalAddresses - 2;
            }

            result.put("success", true);
            result.put("ipAddress", ip);
            result.put("cidr", "/" + cidr);
            result.put("subnetMask", toIp(mask));
            result.put("networkAddress", toIp(network));
            result.put("broadcastAddress", toIp(broadcast));
            result.put("totalAddresses", totalAddresses);
            result.put("usableHostAddresses", usableHosts);

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    private String toIp(long number) {
        return ((number >> 24) & 255) + "."
                + ((number >> 16) & 255) + "."
                + ((number >> 8) & 255) + "."
                + (number & 255);
    }
}

