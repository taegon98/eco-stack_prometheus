package openstack.eco_stack.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Document(collection = "CloudInstance")
public class CloudInstance {

    @Id
    private String id;

    @Builder.Default
    private List<String> hypervisorCpuUtilizationMetricIds = new ArrayList<>();
    @Builder.Default
    private List<String> hypervisorMemoryUtilizationMetricIds = new ArrayList<>();
    private List<String> cpuUtilizationMetricIds;
    private List<String> memoryUtilizationMetricIds;
    private List<String> diskUtilizationMetricIds;

    public void addToHypervisorCpuUtilizationMetricIds(String metricId) {
        this.hypervisorCpuUtilizationMetricIds.add(metricId);
    }

    public void addToHypervisorMemoryUtilizationMetricIds(String metricId) {
        this.hypervisorMemoryUtilizationMetricIds.add(metricId);
    }
}
