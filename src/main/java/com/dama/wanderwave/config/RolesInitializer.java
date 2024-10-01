package com.dama.wanderwave.config;

import com.dama.wanderwave.role.Role;
import com.dama.wanderwave.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RolesInitializer implements CommandLineRunner {
	private final RoleRepository repository;

	private static final List<String> DEFAULT_ROLES = Arrays.asList("USER", "ADMIN");

	@Override
	@Transactional
	public void run(String... args) {
		Set<String> existingRoles = repository.findByNameIn(DEFAULT_ROLES)
				                            .stream()
				                            .map(Role::getName)
				                            .collect(Collectors.toSet());


		List<Role> newRoles = DEFAULT_ROLES.stream()
				                      .filter(role -> !existingRoles.contains(role))
				                      .map(role -> Role.builder().name(role).build())
				                      .toList();

		if (!newRoles.isEmpty()) {
			repository.saveAll(newRoles);
		}
	}




}
