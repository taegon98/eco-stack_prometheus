package openstack.eco_stack.repository;

import openstack.eco_stack.model.CloudInstance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CloudInstanceRepository extends MongoRepository<CloudInstance, String> {
}
