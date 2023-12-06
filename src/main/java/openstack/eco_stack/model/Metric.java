package openstack.eco_stack.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.ZonedDateTime;

@ToString
@Builder
@Getter
@Document(collection = "Metric")
public class Metric {

    @Id
    private String id;
    private String name;
    private Instant dateTime;
    private Double value;
}
