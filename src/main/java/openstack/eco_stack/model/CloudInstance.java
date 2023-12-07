package openstack.eco_stack.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Builder
@Getter
@Document(collection = "CloudInstance")
public class CloudInstance {

    @Id
    private String id;
    @Builder.Default
    private LocalDate createdDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @Builder.Default
    private Set<String> hypervisorCpuUtilizationMetricIds = new HashSet<>();
    @Builder.Default
    private Set<String> hypervisorMemoryUtilizationMetricIds = new HashSet<>();
    @Builder.Default
    private Set<String> cpuUtilizationMetricIds = new HashSet<>();;
    @Builder.Default
    private Set<String> memoryUtilizationMetricIds = new HashSet<>();;
    @Builder.Default
    private Set<String> diskUtilizationMetricIds = new HashSet<>();;

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
