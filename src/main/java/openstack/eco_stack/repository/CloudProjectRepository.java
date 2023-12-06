package openstack.eco_stack.repository;

import openstack.eco_stack.model.CloudProject;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CloudProjectRepository extends MongoRepository<CloudProject, String> {
}
