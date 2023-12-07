package openstack.eco_stack.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Builder
@Getter
@Document(collection = "CloudInstance")
public class CloudInstance {

    @Id
    private String id;
    @Builder.Default
    private LocalDate createdDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @Builder.Default
    private Set<String> hypervisorCpuUtilizationMetricIds = new LinkedHashSet<>();
    @Builder.Default
    private Set<String> hypervisorMemoryUtilizationMetricIds = new LinkedHashSet<>();
    @Builder.Default
    private Set<String> cpuUtilizationMetricIds = new LinkedHashSet<>();
    @Builder.Default
    private Set<String> memoryUtilizationMetricIds = new LinkedHashSet<>();
    @Builder.Default
    private Set<String> diskUtilizationMetricIds = new LinkedHashSet<>();

    public void addToHypervisorCpuUtilizationMetricIds(String metricId) {
        this.hypervisorCpuUtilizationMetricIds.add(metricId);
    }

    public void addToHypervisorMemoryUtilizationMetricIds(String metricId) {
        this.hypervisorMemoryUtilizationMetricIds.add(metricId);
    }

    public void addToCpuUtilizationMetricIds(String metricId) {
        this.cpuUtilizationMetricIds.add(metricId);
    }

    public void addToMemoryUtilizationMetricIds(String metricId) {
        this.memoryUtilizationMetricIds.add(metricId);
    }
}
