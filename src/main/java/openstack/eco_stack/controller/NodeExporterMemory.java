package openstack.eco_stack.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
public class NodeExporterMemory {

    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();
        String prometheusUrl = "http://133.186.215.103:9090";

        ZoneId seoulZoneId = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(seoulZoneId);
        ZonedDateTime oneHourAgo = now.minusHours(24);

        long endTime = now.toEpochSecond();
        long startTime = oneHourAgo.toEpochSecond();

        while (startTime < endTime) {
            double memoryUtilization = calculateHourlyMemoryUtilization(restTemplate, prometheusUrl, startTime);
            ZonedDateTime hour = ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(startTime), seoulZoneId);
            log.info("[{}] Memory Utilization: {}%", hour, memoryUtilization); //[hour: 수집 시각, memoryUtilization: memory 사용률]

            startTime += 3600;
        }
    }

    private static double calculateHourlyMemoryUtilization(RestTemplate restTemplate, String prometheusUrl, long startTime) {
        String memFreeQuery = prometheusUrl + "/api/v1/query?" +
                "query=avg_over_time(node_memory_MemFree_bytes[60m])" +
                "&time=" + startTime;
        String memCachedQuery = prometheusUrl + "/api/v1/query?" +
                "query=avg_over_time(node_memory_Cached_bytes[60m])" +
                "&time=" + startTime;
        String memBuffersQuery = prometheusUrl + "/api/v1/query?" +
                "query=avg_over_time(node_memory_Buffers_bytes[60m])" +
                "&time=" + startTime;
        String memTotalQuery = prometheusUrl + "/api/v1/query?" +
                "query=avg_over_time(node_memory_MemTotal_bytes[60m])" +
                "&time=" + startTime;

        double memFree = extractValue(restTemplate.getForEntity(memFreeQuery, String.class));
        double memCached = extractValue(restTemplate.getForEntity(memCachedQuery, String.class));
        double memBuffers = extractValue(restTemplate.getForEntity(memBuffersQuery, String.class));
        double memTotal = extractValue(restTemplate.getForEntity(memTotalQuery, String.class));

        double memoryUtilization = calculateMemoryUtilization(memFree, memCached, memBuffers, memTotal);

        log.info("Memory Free: {}, Memory Cached: {}, Memory Buffers: {}, Memory Total: {}", memFree, memCached, memBuffers, memTotal);

        return memoryUtilization;
    }

    private static double extractValue(ResponseEntity<String> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            String result = response.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode jsonNode = objectMapper.readTree(result);
                if (jsonNode.has("data") && jsonNode.get("data").has("result")) {
                    JsonNode resultNode = jsonNode.get("data").get("result");
                    if (resultNode.isArray() && resultNode.size() > 0) {
                        JsonNode valueNode = resultNode.get(0).get("value");
                        if (valueNode.isArray() && valueNode.size() == 2) {
                            return valueNode.get(1).asDouble();
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Error extracting metric value: {}", e.getMessage());
            }
        } else {
            log.error("HTTP request failed: {}", response.getStatusCode());
        }
        return 0;
    }

    private static double calculateMemoryUtilization(double memFree, double memCached, double memBuffers, double memTotal) {
        return 100 * (1 - ((memFree + memCached + memBuffers) / memTotal));
    }
}
