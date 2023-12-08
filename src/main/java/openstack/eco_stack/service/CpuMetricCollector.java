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
import java.time.LocalDate;
import java.time.ZonedDateTime;

@RequiredArgsConstructor
@Component
@Slf4j
public class CpuMetricCollector implements MetricCollector{

    private final HypervisorInstanceMetricRepository hypervisorInstanceMetricRepository;
    private final CloudInstanceRepository cloudInstanceRepository;
    private final CloudProjectRepository cloudProjectRepository;
    private final HypervisorRepository hypervisorRepository;
    private final CloudInstanceMetricRepository cloudInstanceMetricRepository;

    private final String metricType = "CPU Utilization";
    private final int NUMBER_OF_CPU = 4;


//    @Scheduled(fixedRate = 5000)
    @Scheduled(cron = "0 0 0 * * *")
    public void collectMetric() throws UnsupportedEncodingException {
        RestTemplate restTemplate = new RestTemplate();
        long endTime = now.toEpochSecond();
        long startTime = oneDayAgo.toEpochSecond();
        MetricValues metricValues = MetricValues.builder().build();

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

            MetricValue metricValue = MetricValue.builder()
                    .dateTime(hour.toInstant())
                    .value(cpuUtilizationAvg)
                    .build();
            metricValues.add(metricValue);

            startTime += 3600;
        }

        saveMetric(metricValues);
    }

    private double fetchAndCalculateCPUUtilization(RestTemplate restTemplate, String prometheusUrl, long startTime, int cpu) throws UnsupportedEncodingException {
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

    private double extractCPUUtilization(ResponseEntity<String> response, int cpu) {
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

    private void saveMetric(MetricValues metricValues) {
        //TODO: Save Metric
        HypervisorInstanceMetric instanceMetric = HypervisorInstanceMetric.builder()
                .name(metricType)
                .date(LocalDate.now(seoulZoneId))
                .metricValues(metricValues)
                .build();

        HypervisorInstanceMetric savedInstanceMetric = hypervisorInstanceMetricRepository.save(instanceMetric);

//        CloudInstanceMetric cloudInstanceMetric = CloudInstanceMetric.builder()
//                .name(metricType)
//                .date(LocalDate.now(seoulZoneId))
//                .metricValues(metricValues)
//                .build();
//        cloudInstanceMetricRepository.save(cloudInstanceMetric);
//
//        //TODO: Save Instance
//        String cloudInstanceId = "Instance 1";
//        CloudInstance cloudInstance = cloudInstanceRepository.findById(cloudInstanceId)
//                .orElseGet(() -> CloudInstance.builder().id(cloudInstanceId).createdDate(LocalDate.now(seoulZoneId)).build());
//
//        cloudInstance.addToHypervisorCpuUtilizationMetricIds(savedInstanceMetric.getId());
//        cloudInstance.addToCpuUtilizationMetricIds(cloudInstanceMetric.getId());
//        cloudInstance = cloudInstanceRepository.save(cloudInstance);
//
//        //TODO: Save Project
//        String cloudProjectId = "CloudProject 1";
//        CloudProject cloudProject = cloudProjectRepository.findById(cloudProjectId)
//                        .orElseGet(() -> CloudProject.builder().id(cloudProjectId).createdDate(LocalDate.now(seoulZoneId)).build());
//
//        cloudProject.addToCloudInstanceIds(cloudInstance.getId());
//        cloudProjectRepository.save(cloudProject);
//
//        //TODO: Save Hypervisor
//        String hypervisorId = "Hypervisor 1";
//        Hypervisor hypervisor = hypervisorRepository.findById(hypervisorId)
//                        .orElseGet(() -> Hypervisor.builder().id(hypervisorId).createdDate(LocalDate.now(seoulZoneId)).build());
//
//        hypervisor.addToCloudInstanceIds(cloudInstance.getId());
//        hypervisorRepository.save(hypervisor);

        log.info("Save CPU Metric");
    }
}
