package openstack.eco_stack.repository;

import openstack.eco_stack.model.CloudInstanceMetric;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CloudInstanceMetricRepository extends MongoRepository<CloudInstanceMetric, String> {
}
