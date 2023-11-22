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
public class NodeExporterCPU {

    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();
        String prometheusUrl = "http://133.186.215.103:9090";

        ZoneId seoulZoneId = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(seoulZoneId);
        ZonedDateTime oneDayAgo = now.minusDays(1);

        long endTime = now.toEpochSecond();
        long startTime = oneDayAgo.toEpochSecond();

        int interval = 60; // 1분 간격으로 데이터 수집
        int aggregationInterval = 3600; // 1시간 간격으로 집계

        while (startTime < endTime) {
            double[] cpuUtilization = calculateHourlyCPUUtilization(restTemplate, prometheusUrl, startTime);
            ZonedDateTime hour = ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(startTime), seoulZoneId);
            log.info("[{}] CPU Utilization: {}%", hour, cpuUtilization);

            startTime += aggregationInterval;
        }
    }

    private static double[] calculateHourlyCPUUtilization(RestTemplate restTemplate, String prometheusUrl, long startTime) {
        int numberOfCPUs = 4;
        double[] cpuUtilization = new double[numberOfCPUs];
        int interval = 3600;

        for (int cpu = 0; cpu < numberOfCPUs; cpu++) {
            String query = prometheusUrl + "/api/v1/query?" +
                    "query=(1 - avg(irate(node_cpu_seconds_total[60m])) by (instance)) * 100" +
                    "&start=" + startTime +
                    "&end=" + (startTime + interval);

            ResponseEntity<String> response = restTemplate.getForEntity(query, String.class);

            cpuUtilization[cpu] = extractValue(response);
        }

        return cpuUtilization;
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
                log.error("Error extracting CPU metric value: {}", e.getMessage());
            }
        } else {
            log.error("HTTP request failed: {}", response.getStatusCode());
        }
        return 0.0;
    }
}
