package openstack.eco_stack.repository;

import openstack.eco_stack.model.Hypervisor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HypervisorRepository extends MongoRepository<Hypervisor, String> {
}
