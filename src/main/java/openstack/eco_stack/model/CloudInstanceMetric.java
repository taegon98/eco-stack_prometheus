package openstack.eco_stack.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@ToString
@Builder
@Getter
@Document(collection = "CloudInstanceMetric")
public class CloudInstanceMetric {

    @Id
    private String id;
    private String name;
    private LocalDate date;
    private MetricValues metricValues;
}
