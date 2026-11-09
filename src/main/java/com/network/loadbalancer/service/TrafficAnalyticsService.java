package com.network.loadbalancer.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Service
public class TrafficAnalyticsService {

    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();

    private final LongAdder totalResponseTime = new LongAdder();
    private final LongAdder responseTimeCount = new LongAdder();

    private final ConcurrentHashMap<String, BackendStats> backendStats =
            new ConcurrentHashMap<>();

    public void recordRequest() {
        totalRequests.increment();
    }

    public void recordBackendAttempt(String backendName) {
        backendStats.computeIfAbsent(backendName, name -> new BackendStats())
                .requests.increment();
    }

    public void recordBackendFailure(String backendName) {
        backendStats.computeIfAbsent(backendName, name -> new BackendStats())
                .failures.increment();
    }

    public void recordSuccess(String backendName, long responseTime) {
        successfulRequests.increment();
        recordResponseTime(responseTime);

        backendStats.computeIfAbsent(backendName, name -> new BackendStats())
                .successes.increment();
    }

    public void recordFailure(long responseTime) {
        failedRequests.increment();
        recordResponseTime(responseTime);
    }

    private void recordResponseTime(long responseTime) {
        totalResponseTime.add(responseTime);
        responseTimeCount.increment();
    }

    public Map<String, Object> getAnalytics() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("totalRequests", totalRequests.sum());
        result.put("successfulRequests", successfulRequests.sum());
        result.put("failedRequests", failedRequests.sum());

        long count = responseTimeCount.sum();

        result.put("averageResponseTimeMs",
                count == 0 ? 0 : totalResponseTime.sum() / count);

        Map<String, Object> backendResults = new LinkedHashMap<>();

        backendStats.forEach((name, stats) -> {
            Map<String, Object> details = new LinkedHashMap<>();

            details.put("totalRequests", stats.requests.sum());
            details.put("successfulRequests", stats.successes.sum());
            details.put("failedRequests", stats.failures.sum());

            backendResults.put(name, details);
        });

        result.put("backendStatistics", backendResults);

        return result;
    }

    public void resetAnalytics() {
        totalRequests.reset();
        successfulRequests.reset();
        failedRequests.reset();
        totalResponseTime.reset();
        responseTimeCount.reset();
        backendStats.clear();
    }

    private static class BackendStats {
        private final LongAdder requests = new LongAdder();
        private final LongAdder successes = new LongAdder();
        private final LongAdder failures = new LongAdder();
    }
}