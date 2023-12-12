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
public class InstanceMetricCollector implements MetricCollector{
    static String promQLQueryCpuWithTime = "avg_over_time(libvirt_domain_info_cpu_time_seconds_total[1h])";
    static String promQLQueryMemoryWithTime = "avg_over_time(libvirt_domain_info_memory_usage_bytes[1h])";
    static String promQLQueryVirtualCpuWithTime = "avg_over_time(libvirt_domain_info_virtual_cpus[1h])";

    static long endTime = now.toEpochSecond();
    static long startTime = oneDayAgo.toEpochSecond();

    public void collectMetric() {
        RestTemplate restTemplate = new RestTemplate();

        ZonedDateTime currentCollectionTime = ZonedDateTime.now().minusDays(1);

        for (int hour = 1; hour <= 24; hour++) {
            long endTime = currentCollectionTime.toEpochSecond();
            long startTime = currentCollectionTime.minusHours(1).toEpochSecond();

            fetch(restTemplate, prometheusUrl, startTime, endTime, hour);

            currentCollectionTime = currentCollectionTime.plusHours(1);
        }
    }

    private void fetch(RestTemplate restTemplate, String prometheusUrl, long startTime, long endTime, int hour) {
        String prometheusQueryURLCpu = prometheusUrl + "/api/v1/query_range?query=" + promQLQueryCpuWithTime +
                "&start=" + startTime + "&end=" + endTime + "&step=3600";
        String prometheusQueryURLMemory = prometheusUrl + "/api/v1/query_range?query=" + promQLQueryMemoryWithTime +
                "&start=" + startTime + "&end=" + endTime + "&step=3600";
        String prometheusQueryURLVirtualCpu = prometheusUrl + "/api/v1/query_range?query=" + promQLQueryVirtualCpuWithTime +
                "&start=" + startTime + "&end=" + endTime + "&step=3600";

        ResponseEntity<String> responseCpu = restTemplate.getForEntity(prometheusQueryURLCpu, String.class);
        ResponseEntity<String> responseMemory = restTemplate.getForEntity(prometheusQueryURLMemory, String.class);
        ResponseEntity<String> responseVirtualCpu = restTemplate.getForEntity(prometheusQueryURLVirtualCpu, String.class);

        if (responseCpu.getStatusCode().is2xxSuccessful() &&
                responseMemory.getStatusCode().is2xxSuccessful() &&
                responseVirtualCpu.getStatusCode().is2xxSuccessful()) {

            String resultCpu = responseCpu.getBody();
            String resultMemory = responseMemory.getBody();
            String resultVirtualCpu = responseVirtualCpu.getBody();

            ObjectMapper objectMapper = new ObjectMapper();

            try {
                JsonNode rootNodeCpu = objectMapper.readTree(resultCpu).get("data").get("result");
                JsonNode rootNodeMemory = objectMapper.readTree(resultMemory).get("data").get("result");
                JsonNode rootNodeVirtualCpu = objectMapper.readTree(resultVirtualCpu).get("data").get("result");

                for (int i = 0; i < rootNodeCpu.size(); i++) {
                    JsonNode valuesNodeCpu = rootNodeCpu.get(i).get("values");
                    JsonNode valuesNodeMemory = rootNodeMemory.get(i).get("values");
                    JsonNode valuesNodeVirtualCpu = rootNodeVirtualCpu.get(i).get("values");

                    long timestamp = valuesNodeCpu.get(0).get(0).asLong();
                    ZonedDateTime dataCollectionTime = ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(timestamp), ZoneId.of("UTC"));
                    String formattedTime = dataCollectionTime.withZoneSameInstant(ZoneId.of("Asia/Seoul")).toString();


                    double cpuUtilization = calculateCPUUtilization(valuesNodeCpu);
                    double memoryUsageInMB = calculateMemoryUsage(valuesNodeMemory);
                    double virtualCpuValue = calculateVirtualCpu(valuesNodeVirtualCpu);

                    List<String> info = extract(resultCpu);

                    StringBuilder message = new StringBuilder();
                    message.append("Instance ").append(i).append("\n");
                    message.append("instanceId: ").append(info.get(0)).append("\n");
                    message.append("projectId: ").append(info.get(1)).append("\n");
                    message.append("Hypervisor IP address: ").append(info.get(2)).append("\n");
                    message.append("Timestamp: ").append(formattedTime).append("\n");
                    message.append("CPU Utilization: ").append(cpuUtilization).append("%\n");
                    message.append("Memory Usage: ").append(memoryUsageInMB).append(" MB\n");
                    message.append("Virtual CPU: ").append(virtualCpuValue).append("\n");

                    log.info(message.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private double calculateMemoryUsage(JsonNode valuesNodeMemory) {
        double totalMemoryUsageBytes = 0.0;

        for (int i = 0; i < valuesNodeMemory.size(); i++) {
            JsonNode valueNode = valuesNodeMemory.get(i);
            if (valueNode != null && valueNode.isArray() && valueNode.size() > 1) {
                double memoryUsageInBytes = valueNode.get(1).asDouble();
                totalMemoryUsageBytes += memoryUsageInBytes;
            }
        }

        return totalMemoryUsageBytes / (1024 * 1024);
    }

    private double calculateVirtualCpu(JsonNode valuesNodeVirtualCpu) {
        double totalVirtualCpu = 0.0;

        for (int i = 0; i < valuesNodeVirtualCpu.size(); i++) {
            JsonNode valueNode = valuesNodeVirtualCpu.get(i);
            if (valueNode != null && valueNode.isArray() && valueNode.size() > 1) {
                double virtualCpuValue = valueNode.get(1).asDouble();
                totalVirtualCpu += virtualCpuValue;
            }
        }
        return totalVirtualCpu;
    }

    private double calculateCPUUtilization(JsonNode valuesNodeCpu) {
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


    private List<String> extract(String metricData) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"instanceId\":\"([^\"]+)\",.*\"instance\":\"([^\"]+)\",.*\"projectId\":\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(metricData);

        String instanceId = null;
        String projectId = null;
        String instanceIp = null;

        if (matcher.find()) {
            instanceId = matcher.group(1);
            projectId = matcher.group(2);
            instanceIp = matcher.group(3);
        } else {
            log.info("Matcher not found for: " + metricData);
        }
        result.add(instanceId);
        result.add(instanceIp);
        result.add(projectId);

        return result;
    }
}
