package openstack.eco_stack.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
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
@ToString
@Document(collection = "CloudProject")
public class CloudProject {

    @Id
    private String id;
    private String name;
    @Builder.Default
    private LocalDate createdDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
    private String Owner;
    @Builder.Default
    private int lastCloudInstanceCnt = 0;
    @Builder.Default
    private Set<String> cloudInstanceIds = new HashSet<>();

    public void addToCloudInstanceIds(String cloudInstanceId) {
        cloudInstanceIds.add(cloudInstanceId);
    }
}
