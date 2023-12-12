package openstack.eco_stack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import openstack.eco_stack.model.*;
import openstack.eco_stack.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class HypervisorMemoryCollector implements MetricCollector {

    private final CloudInstanceRepository cloudInstanceRepository;
    private final HypervisorInstanceMetricRepository hypervisorInstanceMetricRepository;
    private final CloudProjectRepository cloudProjectRepository;
    private final HypervisorRepository hypervisorRepository;
    private final CloudInstanceMetricRepository cloudInstanceMetricRepository;

    private final String metricType = "Memory Utilization";
    private final int NUMBER_OF_HYPERVISORS = 4;
    private final List<String> hypervisorIPs = Arrays.asList(
            "192.168.0.36:9100", "192.168.0.28:9100", "192.168.0.87:9100", "192.168.0.96:9100");

    @Scheduled(fixedRate = 5000)
    @Scheduled(cron = "0 0 0 * * *")
    public void collectMetric() {
        for (String ip : hypervisorIPs) {
            RestTemplate restTemplate = new RestTemplate();
            long endTime = ZonedDateTime.now().toEpochSecond();
            long startTime = ZonedDateTime.now().minusDays(1).toEpochSecond();
            MetricValues metricValues = MetricValues.builder().build();

            while (startTime < endTime) {
                double memoryUtilization = fetch(restTemplate, prometheusUrl, startTime, ip);
                ZonedDateTime hour = ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(startTime), ZoneId.systemDefault());
                log.info("[{}] Memory Utilization for Hypervisor IP {}: {}%", hour, ip, memoryUtilization);

                MetricValue metricValue = MetricValue.builder()
                        .dateTime(hour.toInstant())
                        .value(memoryUtilization)
                        .build();
                metricValues.add(metricValue);
                startTime += 3600;
            }
            saveMetric(metricValues);
        }
    }

    private double fetch(RestTemplate restTemplate, String prometheusUrl, long startTime, String instance) {
        String query, encodedQuery;
        URI uri;

        ResponseEntity<String> response;
        try {
            query = "avg_over_time(node_memory_MemFree_bytes{instance=\"" + instance + "\"}[60m])";
            encodedQuery = URLEncoder.encode(query, "UTF-8");

            try {
                uri = new URI(prometheusUrl + "/api/v1/query?query=" + encodedQuery + "&time=" + startTime);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return 0.0;
            }
            response = restTemplate.getForEntity(uri, String.class);
            double memFree = extract(response);


            query = "avg_over_time(node_memory_Cached_bytes{instance=\"" + instance + "\"}[60m])";
            encodedQuery = URLEncoder.encode(query, "UTF-8");
            try {
                uri = new URI(prometheusUrl + "/api/v1/query?query=" + encodedQuery + "&time=" + startTime);
                response = restTemplate.getForEntity(uri, String.class);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return 0.0;
            }
            double memCached = extract(response);


            query = "avg_over_time(node_memory_Buffers_bytes{instance=\"" + instance + "\"}[60m])";
            encodedQuery = URLEncoder.encode(query, "UTF-8");
            try {
                uri = new URI(prometheusUrl + "/api/v1/query?query=" + encodedQuery + "&time=" + startTime);
                response = restTemplate.getForEntity(uri, String.class);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return 0.0;
            }
            double memBuffers = extract(response);


            query = "avg_over_time(node_memory_MemTotal_bytes{instance=\"" + instance + "\"}[60m])";
            encodedQuery = URLEncoder.encode(query, "UTF-8");
            try {
                uri = new URI(prometheusUrl + "/api/v1/query?query=" + encodedQuery + "&time=" + startTime);
                response = restTemplate.getForEntity(uri, String.class);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return 0.0;
            }

            double memTotal = extract(response);

            double memoryUtilization = calculateMemoryUtilization(memFree, memCached, memBuffers, memTotal);

            return memoryUtilization;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    private double extract(ResponseEntity<String> response) {
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

    private double calculateMemoryUtilization(double memFree, double memCached, double memBuffers, double memTotal) {
        return 100 * (1 - ((memFree + memCached + memBuffers) / memTotal));
    }

    private void saveMetric(MetricValues metricValues) {
        //TODO: Save Metric
        HypervisorInstanceMetric instanceMetric = HypervisorInstanceMetric.builder()
                .name(metricType)
                .date(LocalDate.now(seoulZoneId))
                .metricValues(metricValues)
                .build();

        HypervisorInstanceMetric savedInstanceMetric = hypervisorInstanceMetricRepository.save(instanceMetric);

        CloudInstanceMetric cloudInstanceMetric = CloudInstanceMetric.builder()
                .name(metricType)
                .date(LocalDate.now(seoulZoneId))
                .metricValues(metricValues)
                .build();
        cloudInstanceMetric = cloudInstanceMetricRepository.save(cloudInstanceMetric);
        //TODO: Save Instance
        String cloudInstanceId = "Instance 1";
        CloudInstance cloudInstance = cloudInstanceRepository.findById(cloudInstanceId)
                .orElseGet(() -> CloudInstance.builder().id(cloudInstanceId).createdDate(LocalDate.now(seoulZoneId)).build());

        cloudInstance.addToHypervisorMemoryUtilizationMetricIds(savedInstanceMetric.getId());
        cloudInstance.addToMemoryUtilizationMetricIds(cloudInstanceMetric.getId());
        cloudInstance = cloudInstanceRepository.save(cloudInstance);

        //TODO: Save Project
        String cloudProjectId = "CloudProject 1";
        CloudProject cloudProject = cloudProjectRepository.findById(cloudProjectId)
                .orElseGet(() -> CloudProject.builder().id(cloudProjectId).createdDate(LocalDate.now(seoulZoneId)).build());

        cloudProject.addToCloudInstanceIds(cloudInstance.getId());
        cloudProjectRepository.save(cloudProject);

        //TODO: Save Hypervisor
        String hypervisorId = "Hypervisor 1";
        Hypervisor hypervisor = hypervisorRepository.findById(hypervisorId)
                .orElseGet(() -> Hypervisor.builder().id(hypervisorId).createdDate(LocalDate.now(seoulZoneId)).build());

        hypervisor.addToCloudInstanceIds(cloudInstance.getId());
        hypervisorRepository.save(hypervisor);

        log.info("Save Memory Metric");
    }
}
