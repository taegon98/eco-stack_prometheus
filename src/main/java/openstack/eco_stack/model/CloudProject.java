package openstack.eco_stack.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Builder
@Getter
@ToString
@Document(collection = "CloudProject")
public class CloudProject {

    @Id
    private String id;
    private String name;
    private LocalDate createdDate;
    private String Owner;
    private List<String> cloudInstanceIds;
}
