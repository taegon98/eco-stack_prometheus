package openstack.eco_stack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class InstanceMetricCollector {
    public static void main(String[] args) {

        String prometheusUrl = "http://180.210.80.14:9090";
        String promQLQueryCpuWithTime = "avg_over_time(libvirt_domain_info_cpu_time_seconds_total[1h])";
        String promQLQueryMemoryWithTime = "avg_over_time(libvirt_domain_info_memory_usage_bytes[1h])";
        String promQLQueryVirtualCpuWithTime = "avg_over_time(libvirt_domain_info_virtual_cpus[1h])";

        ZoneId seoulZoneId = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(seoulZoneId);
        ZonedDateTime ago = now.minusDays(1);

        long endTime = now.toEpochSecond();
        long startTime = ago.toEpochSecond();

        RestTemplate restTemplate = new RestTemplate();

        String prometheusQueryURLCpu = prometheusUrl + "/api/v1/query_range?query=" + promQLQueryCpuWithTime +
                "&start=" + startTime + "&end=" + endTime + "&step=3600";
        String prometheusQueryURLMemory = prometheusUrl + "/api/v1/query_range?query=" + promQLQueryMemoryWithTime +
                "&start=" + startTime + "&end=" + endTime + "&step=3600";
        String prometheusQueryURLVirtualCpu = prometheusUrl + "/api/v1/query_range?query=" + promQLQueryVirtualCpuWithTime +
                "&start=" + startTime + "&end=" + endTime + "&step=3600";

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
                    String metricData = fetchMetricsData();

                    Pattern pattern = Pattern.compile("instanceId=\"([^\"]+)\".*projectId=\"([^\"]+)\"");
                    Matcher matcher = pattern.matcher(metricData);

                    String instanceId = null;
                    String projectId = null;

                    if (matcher.find()) {
                        instanceId = matcher.group(1);
                        projectId = matcher.group(2);

                        log.info("Matcher found: instanceId=" + instanceId + ", projectId=" + projectId);
                    } else {
                        log.info("Matcher not found for: " + metricData);
                    }

                    JsonNode valuesNodeCpu = rootNodeCpu.get(i).get("values");
                    JsonNode valuesNodeMemory = rootNodeMemory.get(i).get("values");
                    JsonNode valuesNodeVirtualCpu = rootNodeVirtualCpu.get(i).get("values");


                    long timestamp = valuesNodeCpu.get(0).get(0).asLong();
                    ZonedDateTime dataCollectionTime = ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(timestamp), ZoneId.of("UTC"));
                    String formattedTime = dataCollectionTime.withZoneSameInstant(seoulZoneId).toString();
                    double cpuUtilization = calculateCPUUtilization(valuesNodeCpu);

                    StringBuilder message = new StringBuilder();
                    message.append(formattedTime).append("\n");
                    message.append("CPU UTILIZATION: ").append(cpuUtilization).append("%\n");

                    message.append("VIRTUAL CPU: ");
                    appendValues(valuesNodeVirtualCpu, message);
                    message.append("\n");

                    message.append("MEMORY USAGE: ");
                    appendValues(valuesNodeMemory, message);
                    message.append("\n");

                    resultLines.add(message.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (String line : resultLines) {
            log.info(line);
        }
    }

    private static void appendValues(JsonNode valuesNode, StringBuilder message) {
        for (JsonNode valueNode : valuesNode) {
            double value = valueNode.get(1).asDouble();
            message.append(value).append(", ");
        }
    }


    private static double calculateCPUUtilization(JsonNode valuesNodeCpu) {
        double totalCPUTime = 0.0;
        double prevCPUTime = 0.0;
        int validValues = 0;

        for (int i = 0; i < valuesNodeCpu.size(); i++) {
            JsonNode valueNode = valuesNodeCpu.get(i);
            if (valueNode != null && valueNode.isArray() && valueNode.size() > 1) {
                double currentCPUTime = valueNode.get(1).asDouble();
                if (i > 0) {
                    totalCPUTime += (currentCPUTime - prevCPUTime);
                    validValues++;
                }
                prevCPUTime = currentCPUTime;
            }
        }

        return (validValues > 0) ? ((totalCPUTime / validValues) / prevCPUTime) * 100 : 0.0;
    }


    private static String fetchMetricsData() {
        String prometheusMetricsURL = "http://133.186.215.103:9000/metrics";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(prometheusMetricsURL, String.class);
        return response.getBody();
    }
}
