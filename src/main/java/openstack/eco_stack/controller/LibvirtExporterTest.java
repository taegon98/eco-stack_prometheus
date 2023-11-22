package openstack.eco_stack.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LibvirtExporterTest {

    public static void main(String[] args) {
        String prometheusUrl = "http://133.186.215.103:9090";
        String promQLQueryCpuWithTime = "libvirt_domain_info_cpu_time_seconds_total";
        String promQLQueryMemoryWithTime = "libvirt_domain_info_memory_usage_bytes";
        String promQLQueryVirtualCpuWithTime = "libvirt_domain_info_virtual_cpus";

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 3600000;

        RestTemplate restTemplate = new RestTemplate();

        String prometheusQueryURLCpu = prometheusUrl + "/api/v1/query_range?query=" + promQLQueryCpuWithTime +
                "&start=" + startTime / 1000 + "&end=" + endTime / 1000 + "&step=60";
        String prometheusQueryURLMemory = prometheusUrl + "/api/v1/query_range?query=" + promQLQueryMemoryWithTime +
                "&start=" + startTime / 1000 + "&end=" + endTime / 1000 + "&step=60";
        String prometheusQueryURLVirtualCpu = prometheusUrl + "/api/v1/query_range?query=" + promQLQueryVirtualCpuWithTime +
                "&start=" + startTime / 1000 + "&end=" + endTime / 1000 + "&step=60";

        ResponseEntity<String> responseCpu = restTemplate.getForEntity(prometheusQueryURLCpu, String.class);
        ResponseEntity<String> responseMemory = restTemplate.getForEntity(prometheusQueryURLMemory, String.class);
        ResponseEntity<String> responseVirtualCpu = restTemplate.getForEntity(prometheusQueryURLVirtualCpu, String.class);

        List<String> resultLines = new ArrayList<>();

        if (responseCpu.getStatusCode().is2xxSuccessful() && responseMemory.getStatusCode().is2xxSuccessful() && responseVirtualCpu.getStatusCode().is2xxSuccessful()) {
            String resultCpu = responseCpu.getBody();
            String resultMemory = responseMemory.getBody();
            String resultVirtualCpu = responseVirtualCpu.getBody();

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode rootNodeCpu = objectMapper.readTree(resultCpu).get("data").get("result");
                JsonNode rootNodeMemory = objectMapper.readTree(resultMemory).get("data").get("result");
                JsonNode rootNodeVirtualCpu = objectMapper.readTree(resultVirtualCpu).get("data").get("result");

                for (int i = 0; i < rootNodeCpu.size(); i++) {
                    String projectIdCpu = rootNodeCpu.get(i).get("metric").get("projectId").asText();
                    String projectIdMemory = rootNodeMemory.get(i).get("metric").get("projectId").asText();
                    String projectIdVirtualCpu = rootNodeVirtualCpu.get(i).get("metric").get("projectId").asText();

                    StringBuilder message = new StringBuilder();
                    message.append("Project ID - CPU: ").append(projectIdCpu).append("\n");

                    // Extract timestamp
                    long timestamp = rootNodeCpu.get(i).get("values").get(0).get(0).asLong() * 1000; // Assuming timestamp is in milliseconds

                    // Format timestamp to human-readable date/time
                    String formattedTime = java.time.format.DateTimeFormatter.ISO_INSTANT
                            .format(java.time.Instant.ofEpochMilli(timestamp));

                    message.append("Data Collection Time - CPU: ").append(formattedTime).append("\n");

                    JsonNode valuesNodeCpu = rootNodeCpu.get(i).get("values");
                    JsonNode valuesNodeMemory = rootNodeMemory.get(i).get("values");
                    JsonNode valuesNodeVirtualCpu = rootNodeVirtualCpu.get(i).get("values");

                    message.append("CPU TIME: ");
                    for (JsonNode valueNode : valuesNodeCpu) {
                        double cpuTime = valueNode.get(1).asDouble();
                        message.append(cpuTime).append(", ");
                    }
                    message.append("\n");

                    message.append("VIRTUAL CPU: ");
                    for (JsonNode valueNode : valuesNodeVirtualCpu) {
                        double virtualCpu = valueNode.get(1).asDouble();
                        message.append(virtualCpu).append(", ");
                    }
                    message.append("\n");

                    message.append("MEMORY USAGE: ");
                    for (JsonNode valueNode : valuesNodeMemory) {
                        double memoryUsage = valueNode.get(1).asDouble();
                        message.append(memoryUsage).append(", ");
                    }
                    message.append("\n");

                    // 로깅 - 각 매트릭의 수집 시간
                    log.info("Project ID - CPU: {}, Data Collection Time - CPU: {}", projectIdCpu, formattedTime);
                    log.info("Project ID - Virtual CPU: {}, Data Collection Time - Virtual CPU: {}", projectIdVirtualCpu, formattedTime);
                    log.info("Project ID - Memory: {}, Data Collection Time - Memory: {}", projectIdMemory, formattedTime);

                    resultLines.add(message.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("HTTP 요청 실패: " + responseCpu.getStatusCode() + ", " + responseMemory.getStatusCode() + ", " + responseVirtualCpu.getStatusCode());
        }

        for (String line : resultLines) {
            log.info(line);
        }
    }
}
