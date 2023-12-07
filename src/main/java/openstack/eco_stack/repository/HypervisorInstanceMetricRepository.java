package openstack.eco_stack.repository;

import openstack.eco_stack.model.HypervisorInstanceMetric;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HypervisorInstanceMetricRepository extends MongoRepository<HypervisorInstanceMetric, String> {
}
