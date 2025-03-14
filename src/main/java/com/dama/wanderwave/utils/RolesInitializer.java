package com.dama.wanderwave.utils;

import com.dama.wanderwave.role.Role;
import com.dama.wanderwave.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RolesInitializer implements CommandLineRunner {
	private final RoleRepository repository;

	@Override
	public void run(String... args) {
		createRoleIfNotExists("ROLE_USER");
		createRoleIfNotExists("ROLE_ADMIN");
	}

	private void createRoleIfNotExists(String roleName) {
		repository.findByName(roleName).ifPresentOrElse(
				existingRole -> {},
				() -> {
					Role role = Role.builder().name(roleName).build();
					repository.save(role);
				}
		);
	}

}
