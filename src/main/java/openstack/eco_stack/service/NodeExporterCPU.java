package openstack.eco_stack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
public class NodeExporterCPU {

    public static void main(String[] args) throws UnsupportedEncodingException {
        RestTemplate restTemplate = new RestTemplate();

        String prometheusUrl = "http://133.186.215.103:9090";
        String region = "Asia/Seoul";
        ZoneId seoulZoneId = ZoneId.of(region);
        ZonedDateTime now = ZonedDateTime.now(seoulZoneId);
        ZonedDateTime oneHourAgo = now.minusHours(24);

        long endTime = now.toEpochSecond();
        long startTime = oneHourAgo.toEpochSecond();
        final int NUMBER_OF_CPU = 4;

        while (startTime < endTime) {
            double cpuUtilizationAvg = 0;
            ZonedDateTime hour = null;
            for (int cpuNumber = 0; cpuNumber < NUMBER_OF_CPU; cpuNumber++) {
                double cpuUtilization = fetchAndCalculateCPUUtilization(restTemplate, prometheusUrl, startTime, cpuNumber);
                hour = ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(startTime), seoulZoneId);
                cpuUtilizationAvg += cpuUtilization;
//                log.info("[{}] CPU {} Utilization: {}%", hour, cpuNumber, cpuUtilization); //[hour: 수집 시각, cpuNUmber: cpuNUmber 번호, cpuUtilization: CPU 사용률]
            }
            cpuUtilizationAvg /= NUMBER_OF_CPU;

            log.info("[{}] {} : {} %", hour, "CPU Utilization Average", cpuUtilizationAvg);

            startTime += 3600;
        }

    }

    private static double fetchAndCalculateCPUUtilization(RestTemplate restTemplate, String prometheusUrl, long startTime, int cpu) throws UnsupportedEncodingException {
        String query = "avg without (mode,cpu) (1 - rate(node_cpu_seconds_total{cpu=\"" + cpu + "\", mode=\"idle\"}[1h]))";
        String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        URI uri;

        try {
            uri = new URI(prometheusUrl + "/api/v1/query?query=" + encodedQuery + "&time=" + startTime);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return 0.0;
        }
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        return extractCPUUtilization(response, cpu);
    }

    private static double extractCPUUtilization(ResponseEntity<String> response, int cpu) {
        if (response.getStatusCode().is2xxSuccessful()) {
            String responseBody = response.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode jsonNode = objectMapper.readTree(responseBody);
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
                e.printStackTrace();
            }
        } else {
            log.error("Error fetching data for CPU {}. Response code: {}", cpu, response.getStatusCode());
        }
        return 0.0;
    }
}
