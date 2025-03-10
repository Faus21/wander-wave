package com.dama.wanderwave.role;


import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleRepository extends CrudRepository<Role, String > {
	Optional<Role> findByName(String name);

	List<Role> findByNameIn(List<String> names);
}
